package org.pathirage.thulitha;

import java.util.List;

public class SizeUtility {
  public static void updateBrokerSize(List<Broker> brokers) {
    int[] totalRemaining = computeTotalRemaining(brokers);
    float[] normalizationFactor = new float[totalRemaining.length];

    for (int i = 0; i < totalRemaining.length; i++) {
      if (totalRemaining[i] < 0) {
        throw new IllegalStateException(String.format("Total remaining capacity of dimension %s is negative [%s]", i, totalRemaining[i]));
      }

      normalizationFactor[i] = totalRemaining[i] == 0 ? 0.0f : 1f / totalRemaining[i];
    }

    for (Broker b : brokers) {
      double size = 0;

      for (int i = 0; i < totalRemaining.length; i++){
        size += normalizationFactor[i] * b.getRemainingCapacity(i);
      }

      b.setSize(size);
    }
  }

  public static void updateReplicaSize(List<Replica> replicas, List<Broker> brokers) {
    int[] totalRemaining = computeTotalRemaining(brokers);
    float[] normalizationFactor = new float[totalRemaining.length];

    for (int i = 0; i < totalRemaining.length; i++) {
      if (totalRemaining[i] < 0) {
        throw new IllegalStateException(String.format("Total remaining capacity of dimension %s is negative [%s]", i, totalRemaining[i]));
      }

      normalizationFactor[i] = totalRemaining[i] == 0 ? 0.0f : 1f / totalRemaining[i];
    }

    for (Replica r : replicas) {
      double size = 0;

      for (int i = 0; i < totalRemaining.length; i++) {
        size += normalizationFactor[i] * r.getDimension(i);
      }

      r.setSize(size);
    }
  }

  private static int[] computeTotalRemaining(List<Broker> brokers) {
    if (brokers == null || brokers.isEmpty()) {
      throw new RuntimeException("No brokers");
    }

    int dimensions = brokers.get(0).getDimensionCount();
    int[] totalRemaining = new int[dimensions];

    for (int d = 0; d < dimensions; d++) {
      totalRemaining[d] = 0;
    }

    for (Broker b : brokers) {
      for (int d = 0; d < dimensions; d++) {
        totalRemaining[d] += b.getRemainingCapacity(d);
      }
    }

    return totalRemaining;
  }
}
