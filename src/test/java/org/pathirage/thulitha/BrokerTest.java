package org.pathirage.thulitha;

import org.junit.Assert;
import org.junit.Test;

public class BrokerTest {

  @Test
  public void testFeasibleAssignment() {
    Broker b = new Broker(CCInstanceType.M4_2X, StorageVolumeType.ST1, 128);
    Replica r = new Replica("t", 0, 0, 256, 1000000, 30, 10, 40, new Double((20.0 / 30.0) * 100).intValue());
    Assert.assertTrue(b.add(r));
    Assert.assertEquals(10, b.getCapacity()[3] - b.getRemainingCapacity(3));
    Assert.assertEquals(40, b.getCapacity()[4] - b.getRemainingCapacity(4));

    int remainingIOPS = ((CCInstanceType.M4_2X.getStorageBWMB() * 1024) / 128) - ((30 * 1024) / 128);
    Assert.assertEquals(remainingIOPS, b.getRemainingCapacity(2));
  }

  @Test
  public void testUnfeasibleAssignment() {
    Broker b = new Broker(CCInstanceType.M4_2X, StorageVolumeType.ST1, 128);
    Replica r = new Replica("t", 0, 0, 256, 1000000, 150, 30, 180, new Double((120.0 / 150.0) * 100).intValue());
    Assert.assertTrue(!b.add(r));
    Assert.assertEquals(0, b.getCapacity()[0] - b.getRemainingCapacity(0));
    Assert.assertEquals(0, b.getCapacity()[1] - b.getRemainingCapacity(1));
    Assert.assertEquals(0, b.getCapacity()[2] - b.getRemainingCapacity(2));
    Assert.assertEquals(0, b.getCapacity()[3] - b.getRemainingCapacity(3));
  }
}
