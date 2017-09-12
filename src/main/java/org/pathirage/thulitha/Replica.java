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
 * Requirement dimensions
 *  - ram : 0
 *  - storage : 1
 *  - storage bw : 2
 *  - network in : 3
 *  - network out : 4
 */
public class Replica implements Comparable<Replica> {
  private final String topic;
  private final int partition;
  private final int id;
  private final int[] requirements;
  private final int readPercentage;
  private double size = -1;

  public Replica(String topic, int partition, int id, int ramRequirement, int storageRequirement, int storageBandwidthRequirement, int networkInRequirement, int networkOutRequirement, int readPercentage) {
    this.topic = topic;
    this.partition = partition;
    this.id = id;
    this.requirements = new int[]{ramRequirement, storageRequirement, storageBandwidthRequirement, networkInRequirement, networkOutRequirement};
    this.readPercentage = readPercentage;

  }

  public int getReadPercentage() {
    return readPercentage;
  }

  /**
   * Get replica's resource requirements
   * @return resource requirements over all dimensions
   */
  public int[] getRequirements() {
    return requirements;
  }

  /**
   * Get the topic this replica belongs to
   * @return topic
   */
  public String getTopic() {
    return topic;
  }

  /**
   * Get the partition this replica belongs to
   * @return partition id
   */
  public int getPartition() {
    return partition;
  }

  /**
   * Get the replica identifier
   * @return replica identifier
   */
  public int getId() {
    return id;
  }

  /**
   * Return number of dimensions in the resource requirements
   * @return number of dimensions
   */
  public int getDimensionCount() {
    return requirements.length;
  }

  /**
   * Get resource requirement along a specific dimension
   * @param dimension dimension id
   * @return resource requirement
   */
  public int getDimension(int dimension) {
    return requirements[dimension];
  }

  /**
   * Get the scalar size of the replica
   * @return scalar size
   */
  public double getSize() {
    return size;
  }

  /**
   * Set the scalar size
   * @param size scalar size
   */
  public void setSize(double size) {
    this.size = size;
  }

  @Override
  public int compareTo(Replica o) {
    return Double.compare(this.size, o.size);
  }
}
