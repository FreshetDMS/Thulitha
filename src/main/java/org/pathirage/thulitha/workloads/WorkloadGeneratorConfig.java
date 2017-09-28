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
package org.pathirage.thulitha.workloads;

import com.typesafe.config.Config;
import org.apache.commons.math3.util.Pair;

import java.util.Random;

public class WorkloadGeneratorConfig {
  private int maxReplicationFactor;
  private boolean allocateReadCapacityForFollowers;
  private int perTopicPartitionCountMin;
  private int perTopicPartitionCountMax;
  private int minMessageSize;
  private int maxMessageSize;
  private int minRetentionHours;
  private int maxRetentionHours;
  private boolean clusterWideRetentionConfiguration;
  private int minConsumers;
  private int maxConsumers;
  private int maxPerPartitionPublishRate;
  private int minPerPartitionPublishRate;

  private final Config config;
  private final Random random = new Random(System.currentTimeMillis());

  public WorkloadGeneratorConfig(Config config) {
    this.config = config;
    initFromConfig();
  }

  private void initFromConfig() {
    // Initialize this from config object.
  }

  public Pair<Integer, Integer> getNextPartitionCountAndReplicationFactor() {
    int replicationFactor = random.nextInt(maxReplicationFactor);
    int partitionCount = perTopicPartitionCountMin + random.nextInt(perTopicPartitionCountMax - perTopicPartitionCountMin);

    return new Pair<Integer, Integer>(replicationFactor, partitionCount);
  }

  public int getNextPerPartitionPublishRate() {
    return minPerPartitionPublishRate + random.nextInt(maxPerPartitionPublishRate - minPerPartitionPublishRate);
  }

  public int getNextAverageMessageSize() {
    return minMessageSize + random.nextInt(maxMessageSize - minMessageSize);
  }

  public void setMaxReplicationFactor(int maxReplicationFactor) {
    this.maxReplicationFactor = maxReplicationFactor;
  }

  public void setAllocateReadCapacityForFollowers(boolean allocateReadCapacityForFollowers) {
    this.allocateReadCapacityForFollowers = allocateReadCapacityForFollowers;
  }

  public void setPerTopicPartitionCountMin(int perTopicPartitionCountMin) {
    this.perTopicPartitionCountMin = perTopicPartitionCountMin;
  }

  public void setPerTopicPartitionCountMax(int perTopicPartitionCountMax) {
    this.perTopicPartitionCountMax = perTopicPartitionCountMax;
  }

  public void setMinMessageSize(int minMessageSize) {
    this.minMessageSize = minMessageSize;
  }

  public void setMaxMessageSize(int maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  public void setMinRetentionHours(int minRetentionHours) {
    this.minRetentionHours = minRetentionHours;
  }

  public void setMaxRetentionHours(int maxRetentionHours) {
    this.maxRetentionHours = maxRetentionHours;
  }

  public void setClusterWideRetentionConfiguration(boolean clusterWideRetentionConfiguration) {
    this.clusterWideRetentionConfiguration = clusterWideRetentionConfiguration;
  }

  public void setMinConsumers(int minConsumers) {
    this.minConsumers = minConsumers;
  }

  public void setMaxConsumers(int maxConsumers) {
    this.maxConsumers = maxConsumers;
  }
}
