package org.pathirage.thulitha;

import org.junit.Assert;
import org.junit.Test;
import org.pathirage.thulitha.utils.Topic;

import java.util.ArrayList;
import java.util.List;

public class CapacityPlannerTest {
  @Test
  public void testPlanner() {
    Topic t1 = new Topic("t1", 1000000, 234, 70, 2, 4, 2,
        new int[]{3000000, 4000000}, 20, 2, false);
    Topic t2 = new Topic("t2", 2000000, 234, 80, 2, 4, 2,
        new int[]{4000000, 5000000}, 20, 2, false);
    Topic t3 = new Topic("t3", 3000000, 234, 100, 2, 4, 2,
        new int[]{5000000, 6000000}, 20, 1, false);
    Topic t4 = new Topic("t4", 2000000, 234, 80, 2, 4, 3,
        new int[]{3000000, 4000000, 6000000}, 20, 2, false);
    Topic t5 = new Topic("t5", 1500000, 234, 70, 2, 4, 2,
        new int[]{3000000, 4000000}, 20, 1, false);

    List<Replica> replicas = new ArrayList<>();

    replicas.addAll(t1.getReplicas());
    replicas.addAll(t2.getReplicas());
    replicas.addAll(t3.getReplicas());
    replicas.addAll(t4.getReplicas());
    replicas.addAll(t5.getReplicas());

    CapacityPlanner cp = new CapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true);
    List<Broker> solution = cp.solve();
    
    Assert.assertEquals(112, solution.size());
  }
}
