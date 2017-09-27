package org.pathirage.thulitha;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CapacityPlannerTest extends BaseTest {
  private static final Logger log = LoggerFactory.getLogger(CapacityPlannerTest.class);

  @Test
  public void testPlanner() {
    List<Replica> replicas = getHighNetworkOutReplicas();

    CapacityPlanner cp = new BFDCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true, false);
    log.info("Absolute minimum of brokers: " + cp.lowestPossibleBrokersRequired());
    List<Broker> solution = cp.solve();

    Assert.assertEquals(115, solution.size());
    log.info("Solution size: " + solution.size());
  }

  @Test
  public void testNoReplayNoReplicationLowConsumers() {
    List<Replica> replicas = getNoReplayAndSingleConsumer();


      for (int d = 0; d < 5; d++) {
        log.info("replica in, out:" + replicas.get(0).getDimension(3) + ", " + replicas.get(0).getDimension(4));
      }


    CapacityPlanner cp = new BFDCapacityPlanner(replicas, CCInstanceType.M4_4X, StorageVolumeType.ST1, true, false);
    log.info("Absolute minimum of brokers: " + cp.lowestPossibleBrokersRequired());
    List<Broker> solution = cp.solve();

//    Assert.assertEquals(73, solution.size());
    log.info("Solution size: " + solution.size());
    for (Broker b : solution) {
      log.info("Utilization [NIn, NOut, Storage, Vols, Replicas]: " + b.getNetworkInBWUtilization() + ", " + b.getNetworkOutBWUtilization() + ", " + b.getStorageBWUtilization() + ", " + b.numberOfStorageVolumes() + ", " + b.getReplicas().size());
    }

  }
}
