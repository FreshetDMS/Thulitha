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
}
