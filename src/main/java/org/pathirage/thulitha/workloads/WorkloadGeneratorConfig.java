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
  private int maxReplicationFactor = 2;
  private boolean allocateReadCapacityForFollowers = false;
  private int perTopicPartitionCountMin = 50;
  private int perTopicPartitionCountMax = 100;
  private int minMessageSize = 124;
  private int maxMessageSize = 356;
  private int minRetentionHours = 12;
  private int maxRetentionHours = 48;
  private boolean clusterWideRetentionConfiguration = true;
  private int minConsumers = 1;
  private int maxConsumers = 3;
  private int maxPerPartitionPublishRate = 8;
  private int minPerPartitionPublishRate = 3;
  private int minReplayFactor = 1;
  private int maxReplayFactor = 3;
  private int maxReplays = 2;
  private int minReplays = 0;
  private int minConsumerLag = 10;
  private int maxConsumerLag = 120;
  private int previousRetentionPeriod = -1;
  private boolean startWithLowestPossible = false;
  private int readPercentage = 0;

  private final Config config;
  private final Random random = new Random(System.currentTimeMillis());

  public WorkloadGeneratorConfig(Config config) {
    this.config = config;
    initFromConfig();
  }

  private void initFromConfig() {
    // Initialize this from config object.
  }

  public int getReadPercentage() {
    return readPercentage;
  }

  public void setReadPercentage(int readPercentage) {
    this.readPercentage = readPercentage;
  }

  public int getMaxReplays() {
    return maxReplays;
  }

  public boolean isAllocateReadCapacityForFollowers() {
    return allocateReadCapacityForFollowers;
  }

  public Pair<Integer, Integer> getNextReplicationFactorAndPartitionCount() {
    int replicationFactor = random.nextInt(maxReplicationFactor);
    if (maxReplicationFactor == 1 || replicationFactor == 0) {
      replicationFactor = 1;
    }
    int partitionCount = perTopicPartitionCountMin + random.nextInt(perTopicPartitionCountMax - perTopicPartitionCountMin);

    return new Pair<Integer, Integer>(replicationFactor, partitionCount);
  }

  public int getNextPerPartitionPublishRate() {
    return minPerPartitionPublishRate + random.nextInt(maxPerPartitionPublishRate - minPerPartitionPublishRate);
  }

  public int getNextAverageMessageSize() {
    return minMessageSize + random.nextInt(maxMessageSize - minMessageSize);
  }

  public int getNextConsumerCount() {
    return minConsumers + random.nextInt(maxConsumers - minConsumers);
  }

  public int getNextConsumerLagSeconds() {
    return minConsumerLag + random.nextInt(maxConsumerLag - minConsumerLag);
  }

  public Pair<Integer, Integer[]> getNextReplayConfiguration() {
    if (maxReplays == 0) {
      return new Pair<Integer, Integer[]>(0, null);
    }

    int replays = minReplays + random.nextInt(maxReplays);

    Integer[] replayRates = new Integer[replays];

    for (int i = 0; i < replays; i++) {
      replayRates[i] = minReplayFactor + random.nextInt(maxReplayFactor);
    }

    return new Pair<Integer, Integer[]>(replays, replayRates);
  }

  public int getNextRetentionHours() {
    if (clusterWideRetentionConfiguration) {
      if (previousRetentionPeriod == -1) {
        previousRetentionPeriod = minRetentionHours + random.nextInt(maxRetentionHours - minRetentionHours);
      }

      return previousRetentionPeriod;
    }

    return minRetentionHours + random.nextInt(maxRetentionHours - minRetentionHours);
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

  public void setMaxPerPartitionPublishRate(int maxPerPartitionPublishRate) {
    this.maxPerPartitionPublishRate = maxPerPartitionPublishRate;
  }

  public void setMinPerPartitionPublishRate(int minPerPartitionPublishRate) {
    this.minPerPartitionPublishRate = minPerPartitionPublishRate;
  }

  public void setMinReplayFactor(int minReplayFactor) {
    this.minReplayFactor = minReplayFactor;
  }

  public void setMaxReplayFactor(int maxReplayFactor) {
    this.maxReplayFactor = maxReplayFactor;
  }

  public void setMaxReplays(int maxReplays) {
    this.maxReplays = maxReplays;
  }

  public void setMinReplays(int minReplays) {
    this.minReplays = minReplays;
  }

  public void setMinConsumerLag(int minConsumerLag) {
    this.minConsumerLag = minConsumerLag;
  }

  public void setMaxConsumerLag(int maxConsumerLag) {
    this.maxConsumerLag = maxConsumerLag;
  }
}
