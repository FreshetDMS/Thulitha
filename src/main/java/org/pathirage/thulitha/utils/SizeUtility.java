package org.pathirage.thulitha.utils;

import org.pathirage.thulitha.Broker;
import org.pathirage.thulitha.Replica;

import java.util.List;

public class SizeUtility {
  public static void updateBrokerSizeBasedOnTotalSizeOfItems(List<Broker> brokers, List<Replica> replicas) {
    long[] totalSizeOfItems = computeTotalSizeOfItems(replicas);
    float[] normalizationFactor = new float[totalSizeOfItems.length];

    for (int i = 0; i < totalSizeOfItems.length; i++) {
      if (totalSizeOfItems[i] < 0) {
        throw new IllegalStateException(String.format("Total item size in dimension %s is negative [%s]", i, totalSizeOfItems[i]));
      }

      normalizationFactor[i] = totalSizeOfItems[i] == 0 ? 0.0f : 1f / totalSizeOfItems[i];
    }

    for (Broker b : brokers) {
      double size = 0;

      for (int d = 0; d < totalSizeOfItems.length; d++) {
        size += normalizationFactor[d] * b.getTotalSizeOfItems(d);
      }

      b.setSize(size);
    }
  }

  public static long[] computeTotalSizeOfItems(List<Replica> replicas) {
    if (replicas == null || replicas.isEmpty()) {
      throw new RuntimeException("No replicas");
    }

    int dimensions = replicas.get(0).getDimensionCount();
    long[] totalSize = new long[dimensions];

    for (int d = 0; d < dimensions; d++) {
      totalSize[d] = 0;
    }

    for (Replica r : replicas) {
      for (int d = 0; d < dimensions; d++) {
        totalSize[d] += r.getDimension(d);
      }
    }

    return totalSize;
  }

  public static void updateBrokerSize(List<Broker> brokers) {
    long[] totalRemaining = computeTotalRemaining(brokers);
    float[] normalizationFactor = new float[totalRemaining.length];

    for (int i = 0; i < totalRemaining.length; i++) {
      if (totalRemaining[i] < 0) {
        throw new IllegalStateException(String.format("Total remaining capacity of dimension %s is negative [%s]", i, totalRemaining[i]));
      }

      normalizationFactor[i] = totalRemaining[i] == 0 ? 0.0f : 1f / totalRemaining[i];
    }

    for (Broker b : brokers) {
      double size = 0;

      for (int i = 0; i < totalRemaining.length; i++) {
        size += normalizationFactor[i] * b.getRemainingCapacity(i);
      }

      b.setSize(size);
    }
  }

  public static void updateReplicaSize(List<Replica> replicas, List<Broker> brokers) {
    long[] totalRemaining = computeTotalRemaining(brokers);
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

  private static long[] computeTotalRemaining(List<Broker> brokers) {
    if (brokers == null || brokers.isEmpty()) {
      throw new RuntimeException("No brokers");
    }

    int dimensions = brokers.get(0).getDimensionCount();
    long[] totalRemaining = new long[dimensions];

    for (int d = 0; d < dimensions; d++) {
      totalRemaining[d] = 0;
    }

    for (Broker b : brokers) {
      for (int d = 0; d < dimensions; d++) {
        if (b.getRemainingCapacity(d) < 0) {
          throw new RuntimeException(String.format("Remaining capacity over dimension %s cannot be negative.", d));
        }
        totalRemaining[d] += b.getRemainingCapacity(d);
      }
    }

    return totalRemaining;
  }
}
