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

import org.apache.commons.math3.util.Pair;
import org.pathirage.thulitha.Replica;
import org.pathirage.thulitha.utils.Topic;

import java.util.*;

/**
 * Basic algorithm:
 * - We are getting number of replicas needed
 * - Need to decide how many topics we are going to use
 * - Then decide replication factors for each topic
 * - Then decide message rates and sizes
 * - We need to decide number of consumers
 * - We need to decide replay rates
 * - How to decide retention period
 */
public class WorkloadGenerator {
  private final WorkloadGeneratorConfig config;

  public WorkloadGenerator(WorkloadGeneratorConfig config) {
    this.config = config;
  }

  private Pair<Integer, Integer[]> getReplays(int replayRate, int replayConsumers) {
    Integer[] replays = new Integer[replayConsumers];
    int perConsumerReplayRate = replayRate / replayConsumers;
    for (int r = 0; r < replayConsumers; r++) {
      replays[r] = perConsumerReplayRate;
    }

    return new Pair<Integer, Integer[]>(replayConsumers, replays);
  }

  public List<Replica> run(int replicaCount) {
    int generated = 0;
    int t = 0;
    Map<String, Integer> maxCounts = new HashMap<>();
    maxCounts.put("PublishRate", 0);
    maxCounts.put("MessageSize", 0);
    maxCounts.put("replays", 0);
    maxCounts.put("consumers", 0);
    maxCounts.put("retention", 0);
    maxCounts.put("partitions", 0);

    List<Topic> topics = new ArrayList<>();
    List<Replica> replicas = new ArrayList<>();

    while (generated < replicaCount) {
      Pair<Integer, Integer> replicationFactorAndPartitionCount = config.getNextReplicationFactorAndPartitionCount();
      generated += replicationFactorAndPartitionCount.getFirst() * replicationFactorAndPartitionCount.getSecond();

      int publishRateMb = config.getNextPerPartitionPublishRate();
      int averageMessageSize = config.getNextAverageMessageSize();
      int publishRate = (int) Math.ceil(((double) publishRateMb * 1024 * 1024) / averageMessageSize) * replicationFactorAndPartitionCount.getSecond();

      Pair<Integer, Integer[]> replays = null;
      if (config.getMaxReplays() <= 0 && config.getReadPercentage() > 0) {
        double replayRate = publishRate * (config.getReadPercentage() / 100.0);

        System.out.println("Replay rate: " + replayRate + " publish rate: " + publishRate);

        replays = new Pair<Integer, Integer[]>(1, new Integer[]{(int) replayRate});
      } else {
        replays = config.getNextReplayConfiguration();
      }
      int[] replayRates = new int[replays.getFirst()];

      if (config.getReadPercentage() > 0 && config.getMaxReplays() <= 0) {
        for (int j = 0; j < replays.getFirst(); j++) {
          replayRates[j] = replays.getSecond()[j];
        }
      } else {
        for (int j = 0; j < replays.getFirst(); j++) {
          replayRates[j] = replays.getSecond()[j] * publishRate;
        }
      }

      int consumerCount = config.getNextConsumerCount();
      int retentionPeriod = config.getNextRetentionHours();

      if (maxCounts.get("partitions") < replicationFactorAndPartitionCount.getSecond()) {
        maxCounts.put("partitions", replicationFactorAndPartitionCount.getSecond());
      }

      if (maxCounts.get("PublishRate") < publishRateMb) {
        maxCounts.put("PublishRate", publishRateMb);
      }

      if (maxCounts.get("MessageSize") < averageMessageSize) {
        maxCounts.put("MessageSize", averageMessageSize);
      }

      if (maxCounts.get("replays") < replays.getFirst()) {
        maxCounts.put("replays", replays.getFirst());
      }

      if (maxCounts.get("consumers") < consumerCount) {
        maxCounts.put("consumers", consumerCount);
      }

      topics.add(new Topic(String.format("t%s", t), publishRate, averageMessageSize,
          replicationFactorAndPartitionCount.getSecond(),
          replicationFactorAndPartitionCount.getFirst(),
          consumerCount, replays.getFirst(), replayRates, config.getNextConsumerLagSeconds(),
          retentionPeriod, config.isAllocateReadCapacityForFollowers()));
      t++;
    }

    System.out.println(Arrays.toString(maxCounts.entrySet().toArray()));

    for (Topic topic : topics) {
      replicas.addAll(topic.getReplicas());
    }

    return replicas;
  }
}
