package org.pathirage.thulitha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CapacityPlanner {
  private static final Logger log = LoggerFactory.getLogger(CapacityPlanner.class);

  private static final int IO_OP_SIZE_128KB = 128;
  private final List<Replica> replicas;
  private final CCInstanceType instanceType;
  private final StorageVolumeType storageVolumeType;
  private boolean dynamic = true;

  public CapacityPlanner(List<Replica> replicas,
                         CCInstanceType instanceType,
                         StorageVolumeType storageVolumeType,
                         boolean dynamic) {
    this.replicas = replicas;
    this.instanceType = instanceType;
    this.storageVolumeType = storageVolumeType;
    this.dynamic = dynamic;
  }

  public List<Broker> solve() {
    List<Broker> brokers = createBrokers(replicas.size());

    log.info(String.format("Solving capacity planning for %s replicas with initial brokers %s", replicas.size(), brokers.size()));

    SizeUtility.updateBrokerSize(brokers);
    SizeUtility.updateReplicaSize(replicas, brokers);
    Collections.sort(brokers);

    while(!replicas.isEmpty()) {
      Replica largestReplica = Collections.max(replicas);

      if (dynamic) {
        Collections.sort(brokers);
      }

      boolean packed = false;
      for (Broker b : brokers) {
        if (b.add(largestReplica)) {
          packed = true;
          break;
        }
      }

      if (!packed) {
        throw  new RuntimeException("Could not pack replica " + largestReplica);
      }

      replicas.remove(largestReplica);
    }

    List<Broker> emptyBrokers = new ArrayList<>();
    for (Broker b : brokers) {
      if (b.isEmpty()) {
        emptyBrokers.add(b);
      }
    }

    log.info("Empty brokers " + emptyBrokers.size());

    for (Broker emptyBroker : emptyBrokers) {
      brokers.remove(emptyBroker);
    }

    return brokers;
  }

  private List<Broker> createBrokers(int binCount) {
    List<Broker> brokers = new ArrayList<>();
    for (int i = 0; i < binCount; i++) {
      brokers.add(new Broker(instanceType, storageVolumeType, IO_OP_SIZE_128KB));
    }

    return brokers;
  }
}
