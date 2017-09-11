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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Broker implements Comparable<Broker> {
  private static final int MBS_TO_KB = 1024;
  private final int[] capacity; // {ram, storage size, storage iops, network in, network out}
  private final int[] remainingCapacity;
  private final List<Replica> replicas;
  private double size = 0;
  private final String id;
  private final CCInstanceType instanceType;
  private final StorageVolumeType storageVolumeType;
  private final int iopSizeKB;
  private final List<StorageVolume> storageVolumes = new ArrayList<>();

  public Broker(CCInstanceType instanceType, StorageVolumeType storageVolumeType, int iopSizeKB) {
    this.instanceType = instanceType;
    this.storageVolumeType = storageVolumeType;
    this.iopSizeKB = iopSizeKB;
    this.capacity = computeInitialCapacity();
    this.remainingCapacity = this.capacity;
    this.replicas = new ArrayList<>();
    this.id = UUID.randomUUID().toString();
    initializeStorageVolumes();
  }

  public boolean add(Replica replica) {
    return isFeasible(replica) && insert(replica);
  }

  private boolean insert(Replica replica) {
    return false; // TODO: Fill this
  }

  private boolean isFeasible(Replica replica) {
    return false; // TODO: Fill this
  }

  private StorageVolume selectStorageVolume(int sizeRequirement, int iopsRequirements) {

  }

  public int[] getCapacity() {
    return capacity;
  }

  public int[] getRemainingCapacity() {
    return remainingCapacity;
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
      storageVolumes.add(new StorageVolume(id, storageVolumeType, instanceType, iopSizeKB));
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

  private int[] computeInitialCapacity() {
    int maxIOPS;
    if (instanceType == CCInstanceType.D2_2X || instanceType == CCInstanceType.D2_4X ||
        instanceType == CCInstanceType.D2_8X) {
      maxIOPS = Math.min(
          storageVolumeType.getIOPS(iopSizeKB, instanceType.getStorageBWMB()) * instanceType.getLocalDiskCount(), // TODO: Do we need to consider duplex bandwidth
          (instanceType.getStorageBWMB() * MBS_TO_KB) / iopSizeKB);
    } else {
      maxIOPS = (instanceType.getStorageBWMB() * MBS_TO_KB) / iopSizeKB;
    }

    return new int[]{instanceType.getRAMMB(), storageVolumeType.getSizeMB() * computeVolumeCount(), maxIOPS,
        instanceType.getNetworkBWMB(), instanceType.getNetworkBWMB()}; // Assumes duplex network card.
  }

  private int computeVolumeCount() {
    if (storageVolumeType == StorageVolumeType.D2HDD || storageVolumeType == StorageVolumeType.D2HDDSTATIC) {
      return instanceType.getLocalDiskCount();
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
}
