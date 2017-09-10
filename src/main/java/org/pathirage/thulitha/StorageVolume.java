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

public class StorageVolume {
  private final String brokerId;
  private final StorageVolumeType type;
  private final CCInstanceType instanceType;
  private final int iopSizeKB;
  private int numberOfLogs = 0;
  private int numberOfLeaders = 0;
  private int totalLeaderIO = 0;
  private int leaderReadIO = 0;
  private int[] capacity = new int[2];
  private int[] remaining = new int[2];
  private int[] totalItemSize = new int[2];

  public StorageVolume(String brokerId, StorageVolumeType type, CCInstanceType instanceType, int iopSizeKB) {
    this.brokerId = brokerId;
    this.type = type;
    this.instanceType = instanceType;
    this.iopSizeKB = iopSizeKB;
    this.capacity[0] = type.getSizeMB();
    this.capacity[1] = type.getIOPS(iopSizeKB, instanceType.getStorageBWMB()); // IOPS
    this.remaining[0] = this.capacity[0];
    this.remaining[1] = this.capacity[1]; // IOPS
    this.totalItemSize[0] = 0;
    this.totalItemSize[1] = 0; // IOPS
  }

  public boolean addReplica(Replica replica, int readPercentage, boolean leader) {
    // TODO: Hope considering total IOPS requirement not read IOPS and write IOPS is correct
    if (isFeasible(replica)) {
      numberOfLogs += 1;
      if (leader) {
        numberOfLeaders += 1;
      }

      if (readPercentage > 0) {
        totalLeaderIO += replica.getDimension(1); // TODO: Make sure dimensions are correct
        leaderReadIO += replica.getDimension(1) * (readPercentage / 100); // TODO: Make sure calculation is correct
      }

      totalItemSize[0] += replica.getDimension(2); // TODO: Verify for correct dimensions
      totalItemSize[1] += replica.getDimension(1);

      int effectiveIOPS = computeEffectiveIOPS();
      remaining[0] = type.getSizeMB() - totalItemSize[0];
      remaining[1] = effectiveIOPS - totalItemSize[1];

      capacity[1] = effectiveIOPS;

      return true;
    }

    return false;
  }

  public boolean isFeasible(Replica replica) {
    return replica.getDimension(1) < remaining[1] && replica.getDimension(2) < remaining[0];
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

  private int effectiveThroughput() {
    return computeEffectiveIOPS() * iopSizeKB;
  }

  private double hourlyCost() {
    return type.getHourlyCost(totalItemSize[0] / (1024 * 1024), totalItemSize[0], iopSizeKB);
  }
}
