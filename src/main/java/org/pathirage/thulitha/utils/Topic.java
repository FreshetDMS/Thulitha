package org.pathirage.thulitha.utils;

import org.pathirage.thulitha.Replica;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Topic {
  private static final double MILLION = 1000000.0;
  private static final int SECONDS_TO_HOURS = 60 * 60;
  private static final int FLUSH_DELAY_SECS = 30;

  private final String name;
  private final int produceRate;
  private final int avgMessageSizeBytes;
  private final int partitions;
  private final int replicationFactor;
  private final int numConsumers;
  private final int numReplays;
  private final int[] replayRates;
  private final int maxConsumerLagSeconds;
  private final int retentionPeriodHours;
  private final boolean readCapacityForFollowers;

  public Topic(String name, int produceRate, int avgMessageSizeBytes, int partitions, int replicationFactor,
               int numConsumers, int numReplays, int[] replayRates, int maxConsumerLagSeconds, int retentionPeriodHours,
               boolean readCapacityForFollowers) {
    this.name = name;
    this.produceRate = produceRate;
    this.avgMessageSizeBytes = avgMessageSizeBytes;
    this.partitions = partitions;
    this.replicationFactor = replicationFactor;
    this.numConsumers = numConsumers;
    this.numReplays = numReplays;
    this.replayRates = replayRates;
    this.maxConsumerLagSeconds = maxConsumerLagSeconds;
    this.retentionPeriodHours = retentionPeriodHours;
    this.readCapacityForFollowers = readCapacityForFollowers;

    if (numReplays!= 0 && numReplays != replayRates.length) {
      throw new RuntimeException("Number of replays and replay rate does not match.");
    }
  }

  public List<Replica> getReplicas() {
    List<Replica> replicas = new ArrayList<>();

    int perPartitionProduceRateMB = (int) Math.ceil(((produceRate * avgMessageSizeBytes) / partitions) / MILLION);
    int storageRequirement = perPartitionProduceRateMB * retentionPeriodHours * SECONDS_TO_HOURS;
    int overallReplayRate = IntStream.of(replayRates).sum();
    int perPartitionReplayRate = overallReplayRate / partitions;
    int perPartitionReplayRateMB = (int) Math.ceil((perPartitionReplayRate * avgMessageSizeBytes) / MILLION);
    if (numReplays == 0) {
      perPartitionReplayRateMB = 0;
    }
    int perPartitionMemRequirement = Math.max(maxConsumerLagSeconds * perPartitionProduceRateMB,
        FLUSH_DELAY_SECS * perPartitionProduceRateMB + perPartitionReplayRateMB);
    int storageBWRequirement = perPartitionProduceRateMB + perPartitionReplayRateMB;
    int readPercentage = (int) Math.ceil(new Float(perPartitionReplayRateMB) / storageBWRequirement);
    int networkInRequirement = perPartitionProduceRateMB;
    int networkOutRequirement = (numConsumers + (replicationFactor - 1)) * perPartitionProduceRateMB + perPartitionReplayRateMB;
    int[] perPartitionReplayRates = null;
    if (numReplays > 0) {
      perPartitionReplayRates = new int[numReplays];
      for (int i = 0; i < numReplays; i++) {
        double replayPerPartition = replayRates[i]/(double)partitions;
        if (replayPerPartition < 0 ){
          System.out.println("Negative replay rate: " + replayRates[i] + " partitions: " + partitions + " produce rate: " + produceRate/partitions);
        }
        perPartitionReplayRates[i] = (int)replayPerPartition;
      }
    }
    for (int p = 0; p < partitions; p++) {
      replicas.add(new Replica(name, p, 0, perPartitionMemRequirement, storageRequirement, storageBWRequirement,
          networkInRequirement, networkOutRequirement, readPercentage, avgMessageSizeBytes, produceRate / partitions, perPartitionReplayRates));
      for (int r = 1; r < replicationFactor; r++) {
        replicas.add(new Replica(name, p, r, perPartitionMemRequirement, storageRequirement,
            readCapacityForFollowers ? storageBWRequirement : perPartitionProduceRateMB, networkInRequirement,
            readCapacityForFollowers ? networkOutRequirement : 0,
            readCapacityForFollowers ? readPercentage : 0, avgMessageSizeBytes, produceRate /partitions, perPartitionReplayRates));
      }
    }

    return replicas;
  }
}
