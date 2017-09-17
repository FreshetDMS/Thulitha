package org.pathirage.thulitha;

import org.junit.Test;
import org.pathirage.thulitha.utils.Topic;

import java.util.List;

public class CapacityPlannerTest {
  @Test
  public void testPlanner() {
    Topic t = new Topic("t", 1000000, 234, 10, 2, 4, 2,
        new int[]{3000000, 4000000}, 20, 7, true);

    CapacityPlanner cp = new CapacityPlanner(t.getReplicas(), CCInstanceType.M4_4X, StorageVolumeType.ST1, true);
    List<Broker> solution = cp.solve();
    System.out.println(solution.size());
  }
}
