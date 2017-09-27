package org.pathirage.thulitha;

import java.util.ArrayList;
import java.util.List;

public abstract class CapacityPlanner {
  static final int IO_OP_SIZE_128KB = 128;
  final List<Replica> replicas;
  final CCInstanceType instanceType;
  final StorageVolumeType storageVolumeType;
  boolean dynamic = true;

  CapacityPlanner(List<Replica> replicas,
                  CCInstanceType instanceType,
                  StorageVolumeType storageVolumeType,
                  boolean dynamic) {
    this.replicas = replicas;
    this.instanceType = instanceType;
    this.storageVolumeType = storageVolumeType;
    this.dynamic = dynamic;
  }

  public abstract List<Broker> solve();

  List<Broker> createBrokers(int binCount) {
    List<Broker> brokers = new ArrayList<>();
    for (int i = 0; i < binCount; i++) {
      brokers.add(new Broker(instanceType, storageVolumeType, IO_OP_SIZE_128KB));
    }

    return brokers;
  }

  public long lowestPossibleBrokersRequired() {
    long numberOfBrokers = 0;
    long[] totalSizeOfItems = computeTotalSizeOfReplicas();
    long[] brokerCapacity = instanceType.capacity();


    for(int d = 0; d < 5; d++) {
      long nb = totalSizeOfItems[d]/ brokerCapacity[d];
      if (totalSizeOfItems[d] % brokerCapacity[d] > 0) {
        nb += 1;
      }

      numberOfBrokers = Math.max(numberOfBrokers, nb);
    }

    return numberOfBrokers;
  }

  long computeLowestBinCount() {
    long numberOfBrokers = 0;
    long[] totalSizeOfItems = computeTotalSizeOfReplicas();
    long[] brokerCapacity = instanceType.capacity();


    for(int d = 0; d < 5; d++) {
      long nb = totalSizeOfItems[d]/ brokerCapacity[d];
      if (totalSizeOfItems[d] % brokerCapacity[d] > 0) {
        nb += 1;
      }

      numberOfBrokers = Math.max(numberOfBrokers, nb);
    }

    return numberOfBrokers;
  }

  long[] computeTotalSizeOfReplicas() {
    long[] totalItemSize = new long[]{0, 0, 0, 0, 0};

    for (Replica r : replicas) {
      for (int d = 0; d < 5; d++) {
        totalItemSize[d] += r.getDimension(d);
      }
    }

    return totalItemSize;
  }

  public static class CapacityPlanningException extends Exception {

    public CapacityPlanningException() {
      super();
    }

    public CapacityPlanningException(String message) {
      super(message);
    }

    public CapacityPlanningException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
