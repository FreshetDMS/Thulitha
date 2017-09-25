package org.pathirage.thulitha;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CapacityPlannerTest extends BaseTest {
  @Test
  public void testPlanner() {
    List<Replica> replicas = getHighNetworkOutReplicas();

    CapacityPlanner cp = new BFDCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true);
    List<Broker> solution = cp.solve();

    Assert.assertEquals(112, solution.size());

    for (Broker b : solution) {
      System.out.println("Utilization [NIn, NOut, Storage]: " + b.getNetworkInBWUtilization() + ", " + b.getNetworkOutBWUtilization() + ", " + b.getStorageBWUtilization());
    }
  }
}
