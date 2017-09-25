package org.pathirage.thulitha;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class RandomPlannerTest extends BaseTest {
  @Test
  public void testRandomCapacityPlanner() {
    List<Replica> replicas = getHighNetworkOutReplicas();

    CapacityPlanner cp = new BFDCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true);
    List<Broker> solution = cp.solve();

    replicas = getHighNetworkOutReplicas();

    RandomCapacityPlanner randomCP = new RandomCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true, solution.size());
    List<Broker> randomSolution = randomCP.solve();

    Assert.assertEquals(112, randomSolution.size());

    for (Broker b : randomSolution) {
      System.out.println("Utilization [NIn, NOut, Storage]: " + b.getNetworkInBWUtilization() + ", " + b.getNetworkOutBWUtilization() + ", " + b.getStorageBWUtilization());
    }
  }

  @Test
  public void testRandomBalancingCapacityPlanner() {
    List<Replica> replicas = getHighNetworkOutReplicas();

    CapacityPlanner cp = new BFDCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true);
    List<Broker> solution = cp.solve();

    replicas = getHighNetworkOutReplicas();

    RandomBalancingCapacityPlanner randomCP = new RandomBalancingCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true, solution.size());
    List<Broker> randomSolution = randomCP.solve();

    Assert.assertEquals(112, randomSolution.size());

    for (Broker b : randomSolution) {
      System.out.println("Utilization [NIn, NOut, Storage]: " + b.getNetworkInBWUtilization() + ", " + b.getNetworkOutBWUtilization() + ", " + b.getStorageBWUtilization());
    }
  }
}
