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

/**
 * Dimensions
 *  - size : 0
 *  - iops : 1
 */
public class StorageVolume implements Comparable<StorageVolume>{
  private final String brokerId;
  private final StorageVolumeType type;
  private final CCInstanceType instanceType;
  private final int iopSizeKB;
  private int numberOfLogs = 0;
  private int numberOfLeaders = 0;
  private int totalLeaderIO = 0;
  private int leaderReadIO = 0;
  private long[] capacity = new long[2];
  private long[] remaining = new long[2];
  private long[] totalItemSize = new long[2];
  private double size = 0;

  public StorageVolume(String brokerId, StorageVolumeType type, CCInstanceType instanceType, int iopSizeKB) {
    this.brokerId = brokerId;
    this.type = type;
    this.instanceType = instanceType;
    this.iopSizeKB = iopSizeKB;
    this.capacity[0] = type.getSizeMB();
    this.capacity[1] = type.getIOPS(iopSizeKB, instanceType.getStorageBWMB()); // IOPS
    this.remaining = this.capacity.clone();
    this.totalItemSize[0] = 0;
    this.totalItemSize[1] = 0; // IOPS
  }

  public boolean addReplica(Replica replica, boolean leader) {
    // We consider sum of storage bandwidth in and out since cloud has no concept of in IOPS vs out IOPS when
    // allocating EBS like volumes
    if (isFeasible(replica)) {
      numberOfLogs += 1;
      if (leader) {
        numberOfLeaders += 1;
      }

      if (replica.getReadPercentage() > 0) {
        totalLeaderIO += replica.getDimension(2);
        leaderReadIO += replica.getDimension(2) * (replica.getReadPercentage() / 100);
      }

      totalItemSize[0] += replica.getDimension(1);
      totalItemSize[1] += Math.ceil(new Float(replica.getDimension(2)) / iopSizeKB); // TODO: figure out a  way to get rid of BW to IOPS conversion

      int effectiveIOPS = computeEffectiveIOPS();
      remaining[0] = type.getSizeMB() - totalItemSize[0];
      remaining[1] = effectiveIOPS - totalItemSize[1]; // Since we are converting to IOPS 4 lines above this is correct

      capacity[1] = effectiveIOPS;

      return true;
    }

    return false;
  }

  public boolean isFeasible(Replica replica) {
    return replica.getDimension(1) < remaining[0] && replica.getDimension(2) < remaining[1];
  }

  private int computeEffectiveIOPS() {
    Float leaderWritePercentage = 0.0f;
    if (numberOfLeaders >= 1 && leaderReadIO > 0) {
      leaderWritePercentage = new Float(totalLeaderIO - leaderReadIO) / totalLeaderIO;
    } else if (leaderReadIO == 0) {
      leaderWritePercentage = 100.0f;
    } else {
      leaderWritePercentage = 0.0f;
    }

    return type.effectiveIOPS(iopSizeKB, instanceType.getStorageBWMB(),
        new Float(leaderWritePercentage * 100).intValue(), numberOfLeaders,
        numberOfLogs - numberOfLeaders);
  }

  public int effectiveThroughput() {
    return computeEffectiveIOPS() * iopSizeKB;
  }

  public double hourlyCost() {
    return type.getHourlyCost(totalItemSize[0] / (1024 * 1024), totalItemSize[0], iopSizeKB);
  }

  public long[] getRemaining() {
    return remaining;
  }

  private long[] getCapacity() {
    return capacity;
  }

  public int getNumberOfLogs() {
    return numberOfLogs;
  }

  public void setSize(double size) {
    this.size = size;
  }

  @Override
  public int compareTo(StorageVolume o) {
    return Double.compare(this.size, o.size);
  }
}
