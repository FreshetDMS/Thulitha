package org.pathirage.thulitha;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.pathirage.thulitha.utils.SizeUtility;

import java.util.ArrayList;
import java.util.List;

public class ClusterBalancingTool {

  private final List<Broker> brokers;
  private final List<Replica> replicas;
  private final CCInstanceType instanceType;
  private final StorageVolumeType storageVolumeType;
  private final int iopSizeKB;

  public ClusterBalancingTool(List<Broker> brokers) {
    this.brokers = brokers;
    this.replicas = getAllReplicas(brokers);
    this.instanceType = brokers.get(0).getInstanceType();
    this.storageVolumeType = brokers.get(0).getStorageVolumeType();
    this.iopSizeKB = brokers.get(0).getIopSizeKB();
  }

  public List<Broker> balance(){
    // Run the capacity planner to get near optimal assignment
    List<Broker> nearOptimalAssignment =
        new BFDCapacityPlanner(replicas, instanceType, storageVolumeType, true).solve();

    // Add more brokers if needed based capacity planner output
    if (nearOptimalAssignment.size() > brokers.size()) {
      int brokersNeeded = nearOptimalAssignment.size() - brokers.size();
      for (int i = 0; i < brokersNeeded; i++) {
        brokers.add(new Broker(instanceType, storageVolumeType, iopSizeKB));
      }
    }

    // Note: This size measure cannot be use with bin packing
    SizeUtility.updateBrokerSizeBasedOnTotalSizeOfItems(brokers, replicas);
    while(!isTerminationConditionMet()) {
      // Perform re-assignment
    }

    return brokers;
  }

  private DescriptiveStatistics computeBrokerSizeStats() {
    DescriptiveStatistics statistics = new DescriptiveStatistics();

    for (Broker b : brokers) {
      statistics.addValue(b.getSize());
    }

    return statistics;
  }

  private boolean isTerminationConditionMet(){
    // TODO: When calculating stats we need to use total size of items assigned to a broker or total (used) resources assigned to a broker
    return false;
  }

  private List<Replica> getAllReplicas(List<Broker> brokers) {
    List<Replica> replicas = new ArrayList<>();

    for (Broker b : brokers) {
      replicas.addAll(b.getReplicas());
    }

    return replicas;
  }
}
