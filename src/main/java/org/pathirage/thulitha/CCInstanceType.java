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

public enum CCInstanceType {
  M4_2X,
  M4_4X,
  M4_10X,
  M4_16X,
  D2_2X,
  D2_4X,
  D2_8X;

  private static final String[] TYPE_IDENTIFIERS = {"m4.2xlarge", "m4.4xlarge", "m4.10xlarge", "m4.16xlarge",
      "d2.2xlarge", "d2.4xlarge", "d2.8xlarge"};
  private static final int[] VCPUS = {8, 16, 40, 64, 8, 16, 36};
  private static final int[] RAM = {32 * 1024, 64 * 1024, 160 * 1024, 256 * 1024, 61 * 1024, 122 * 1024, 244 * 1024};
  private static final int[] NETWORK_BW = {118, 237, 1250, 2500, 118, 237, 1250};
  private static final int[] STORAGE_BW = {125, 250, 500, 1250, 550, 550, 550};
  private static final float[] HOURLY_COST = {0.296f, 0.592f, 1.480f, 2.369f, 0.804f, 1.608f, 3.216f};
  private static final int[] LOCAL_DISK_COUNT = {0, 0, 0, 0, 6, 12, 24};
  private static final int[] STORAGE_VOLUME_COUNT = {1, 1, 2, 5, 6, 12, 24};
  private static final long[] SIZES = {16 * 1024 * 1024, 16 * 1024 * 1024, 16 * 1024 * 1024, 16 * 1024 * 1024,
      2 * 1024 * 1024, 2 * 1024 * 1024, 2 * 1024 * 1024};

  public String getInstanceTypeIdentifier() {
    return TYPE_IDENTIFIERS[ordinal()];
  }

  public int getVCPUs() {
    return VCPUS[ordinal()];
  }

  public int getRAMMB() {
    return RAM[ordinal()];
  }

  public int getNetworkBWMB() {
    return NETWORK_BW[ordinal()];
  }

  public int getStorageBWMB() {
    return STORAGE_BW[ordinal()];
  }

  public float getHourlyCost() {
    return HOURLY_COST[ordinal()];
  }

  public int getLocalDiskCount() {
    return LOCAL_DISK_COUNT[ordinal()];
  }

  public long[] capacity() {
    long[] dimensions = new long[]{0,0,0,0,0};

    dimensions[0] = RAM[ordinal()];
    dimensions[1] = STORAGE_VOLUME_COUNT[ordinal()] * SIZES[ordinal()];
    dimensions[2] = (STORAGE_BW[ordinal()] * 1024) / 128;
    dimensions[3] = NETWORK_BW[ordinal()];
    dimensions[4] = NETWORK_BW[ordinal()];

    return dimensions;
  }
}
