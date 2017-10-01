/**
 * Copyright 2017 Milinda Pathirage
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pathirage.thulitha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Capacity dimensions
 * - ram : 0
 * - storage : 1
 * - storage iops : 2 (item's storage bw requirements get converted to IOPS requirements)
 * - network in : 3
 * - network out : 4
 */
public class Broker implements Comparable<Broker> {
  private static final Logger log = LoggerFactory.getLogger(Broker.class);

  private static final int MBS_TO_KB = 1024;
  private final long[] capacity; // {ram, storage size, storage iops, network in, network out}
  private final long[] remainingCapacity;
  private final long[] totalSizeOfItems;
  private final List<Replica> replicas;
  private double size = 0;
  private final String id;
  private final CCInstanceType instanceType;
  private final StorageVolumeType storageVolumeType;
  private final int iopSizeKB;
  private int maxStorageVolumes = -1;
  private final List<StorageVolume> storageVolumes = new ArrayList<>();
  private final Set<String> partitions = new HashSet<>();
  private final boolean dumb;

  public Broker(CCInstanceType instanceType, StorageVolumeType storageVolumeType, int iopSizeKB) {
    this(instanceType, storageVolumeType, iopSizeKB, false);
  }

  public Broker(CCInstanceType instanceType, StorageVolumeType storageVolumeType, int iopSizeKB, boolean dumb) {
    this.instanceType = instanceType;
    this.storageVolumeType = storageVolumeType;
    this.iopSizeKB = iopSizeKB;
    this.dumb = dumb;
    this.capacity = computeInitialCapacity();
    this.remainingCapacity = this.capacity.clone();
    this.replicas = new ArrayList<>();
    this.id = UUID.randomUUID().toString();
    this.maxStorageVolumes = computeMaxVolumeCount();
    this.totalSizeOfItems = new long[]{0, 0, 0, 0, 0};
    initializeStorageVolumes();
  }

  public boolean isEmpty() {
    return replicas.isEmpty();
  }

  public boolean add(Replica replica) {
    return isFeasible(replica) && insert(replica);
  }

  private boolean insert(Replica replica) {
    boolean leader = replica.getId() == 0; // Replica with id 0 is always the leader

//    if (dumb) {
//      StorageVolume storageVolume = selectStorageVolume(replica);
//      storageVolume.addReplica(replica, leader);
//      replicas.add(replica);
//      partitions.add(replica.getTopicPartition());
//      return true;
//    }

    StorageVolume storageVolume = selectStorageVolume(replica);
    if (storageVolume == null) {
      return false;
    }

    storageVolume.addReplica(replica, leader);

    if (!dumb) {
      allocateMoreStorageBinsIfNecessary();
    }

    for (int i = 0; i < 5; i++) {
      totalSizeOfItems[i] += replica.getRequirements()[i];
    }

    // Updating broker remaining capacity except storage related remaining capacity
    remainingCapacity[0] -= replica.getRequirements()[0];
    remainingCapacity[3] -= replica.getRequirements()[3];
    remainingCapacity[4] -= replica.getRequirements()[4];

    // We should not expose all available storage capacity as remaining since replica's does not share storage volumes.
    // So always use the remaining capacity of storage volume with largest remaining capacity
    StorageVolume maxSV = getStorageVolumeWithMaxRemainingCapacity();
    remainingCapacity[1] = maxSV.getRemaining()[0];
    remainingCapacity[2] = Math.min(maxSV.getRemaining()[1], ((instanceType.getStorageBWMB() * MBS_TO_KB) / iopSizeKB) - ((totalSizeOfItems[2] * MBS_TO_KB) / iopSizeKB));
    capacity[1] = maxSV.getRemaining()[0];
    capacity[2] = maxSV.getRemaining()[1];

    replicas.add(replica);
    partitions.add(replica.getTopicPartition());

    return true;
  }

  public StorageVolume getStorageVolumeWithMaxRemainingCapacity() {
    Collections.sort(storageVolumes);
    // sort storage volume in ascending order of remaining capacity and get the last one.
    StorageVolume maxSv = storageVolumes.get(storageVolumes.size() - 1);

    if (storageVolumes.size() < maxStorageVolumes) {
      return new StorageVolume(id, storageVolumeType, instanceType, iopSizeKB);
    } else {
      return maxSv;
    }
  }

  private boolean isFeasible(Replica replica) {
    if (partitions.contains(replica.getTopicPartition())) {
      return false;
    }

    if (dumb) {
      return true;
    }

    for (int i = 0; i < replica.getDimensionCount(); i++) {
      if (i != 2) {
        if (replica.getDimension(i) > remainingCapacity[i]) {
          return false;
        }
      } else {
        if (((replica.getDimension(i) * MBS_TO_KB) / iopSizeKB) > remainingCapacity[i]) {
          return false;
        }
      }
    }
    return true;
  }

  private StorageVolume selectStorageVolume(Replica replica) {
    if (dumb) {
      Collections.shuffle(storageVolumes);
      return storageVolumes.get(0);
    }

    Collections.sort(storageVolumes);
    for (StorageVolume volume : storageVolumes) {
      if (volume.isFeasible(replica)) {
        return volume;
      }
    }

    if (storageVolumes.size() < computeMaxVolumeCount()) {
      storageVolumes.add(new StorageVolume(id, storageVolumeType, instanceType, iopSizeKB));
    } else {
      return null;
    }

    return selectStorageVolume(replica);
  }

  public void allocateMoreStorageBinsIfNecessary() {
    if (!dumb && (storageVolumeType != StorageVolumeType.D2HDD) && (storageVolumeType != StorageVolumeType.D2HDDSTATIC) &&
        (storageVolumes.size() == maxStorageVolumes)) {
      int effectiveThroughput = 0;
      for (StorageVolume storageVolume : storageVolumes) {
        effectiveThroughput += storageVolume.effectiveThroughput();
      }

      if ((effectiveThroughput / 1024.0) < instanceType.getStorageBWMB()) {
        storageVolumes.add(new StorageVolume(id, storageVolumeType, instanceType, iopSizeKB));
        maxStorageVolumes += 1;
      }
    }
  }

  public long[] getCapacity() {
    return capacity;
  }

  public int numberOfStorageVolumes() {
    if (instanceType != CCInstanceType.D2_2X && instanceType != CCInstanceType.D2_4X && instanceType != CCInstanceType.D2_8X) {
      int numVols = 0;
      for (StorageVolume storageVolume : storageVolumes) {
        if (storageVolume.getNumberOfLogs() > 0) {
          numVols += 1;
        }
      }

      return numVols;
    }

    return storageVolumes.size();
  }

  public int getMaxStorageVolumes() {
    return maxStorageVolumes;
  }

  public CCInstanceType getInstanceType() {
    return instanceType;
  }

  public StorageVolumeType getStorageVolumeType() {
    return storageVolumeType;
  }

  public int getIopSizeKB() {
    return iopSizeKB;
  }

  public long[] getRemainingCapacity() {
    return remainingCapacity;
  }

  public long getRemainingCapacity(int d) {
    return remainingCapacity[d];
  }

  public long getTotalSizeOfItems(int d) {
    return totalSizeOfItems[d];
  }

  public List<Replica> getReplicas() {
    return replicas;
  }

  public double getSize() {
    return size;
  }

  public void setSize(double size) {
    this.size = size;
  }

  public int getDimensionCount() {
    return capacity.length;
  }

  public String getId() {
    return id;
  }

  @Override
  public int compareTo(Broker o) {
    return Double.compare(this.size, o.size);
  }

  private void initializeStorageVolumes() {
    for (int i = 0; i < computeVolumeCount(); i++) {
      storageVolumes.add(new StorageVolume(id, storageVolumeType, instanceType, iopSizeKB, dumb));
    }
  }

  public int getStorageVolumeCount() {
    return storageVolumes.size();
  }

  public double getHourlyCost() {
    double storageCost = 0;
    for (StorageVolume sv : storageVolumes) {
      if (sv.getNumberOfLogs() >= 1) {
        storageCost += sv.hourlyCost();
      }
    }
    return instanceType.getHourlyCost() + storageCost;
  }

  private long[] computeInitialCapacity() {
    int maxIOPS;
    if (instanceType == CCInstanceType.D2_2X || instanceType == CCInstanceType.D2_4X ||
        instanceType == CCInstanceType.D2_8X) {
      maxIOPS = Math.min(
          storageVolumeType.getIOPS(iopSizeKB, instanceType.getStorageBWMB()) * instanceType.getLocalDiskCount(), // TODO: Do we need to consider duplex bandwidth
          (instanceType.getStorageBWMB() * MBS_TO_KB) / iopSizeKB);
    } else {
      maxIOPS = (instanceType.getStorageBWMB() * MBS_TO_KB) / iopSizeKB;
    }

    return new long[]{instanceType.getRAMMB(), storageVolumeType.getSizeMB() * computeVolumeCount(), maxIOPS,
        instanceType.getNetworkBWMB(), instanceType.getNetworkBWMB()}; // Assumes duplex network card.
  }

  private int computeVolumeCount() {
    if (storageVolumeType == StorageVolumeType.D2HDD || storageVolumeType == StorageVolumeType.D2HDDSTATIC) {
      return instanceType.getLocalDiskCount();
    } else if (dumb) {
      return (int) Math.ceil(((instanceType.getStorageBWMB() * 1024.0) / iopSizeKB) / storageVolumeType.getIOPS(iopSizeKB, instanceType.getStorageBWMB()));
    } else {
      return 1; // Initial volume count
    }
  }

  private int computeMaxVolumeCount() {
    if (storageVolumeType == StorageVolumeType.D2HDD || storageVolumeType == StorageVolumeType.D2HDDSTATIC) {
      return instanceType.getLocalDiskCount();
    } else {
      return new Double(Math.ceil(((instanceType.getStorageBWMB() * MBS_TO_KB) / iopSizeKB) /
          storageVolumeType.getIOPS(iopSizeKB, instanceType.getStorageBWMB()))).intValue();
    }
  }

  public double getStorageBWUtilization() {
    return new Double(totalSizeOfItems[2]) / ((instanceType.getStorageBWMB() * 1024.0) / iopSizeKB);
  }

  public double getNetworkInBWUtilization() {
    return new Double(totalSizeOfItems[3]) / instanceType.getNetworkBWMB();
  }

  public double getNetworkOutBWUtilization() {
    return new Double(totalSizeOfItems[4]) / instanceType.getNetworkBWMB();
  }

  public int getReplicaCount() {
    return replicas.size();
  }
}
