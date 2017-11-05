package org.pathirage.thulitha;

import org.junit.Test;

public class StoragePerfModelEvaluatorTest {

  @Test
  public void testWriteOnlyWorkload() {
    StoragePerfModelEvaluator storagePerfModelEvaluator = new StoragePerfModelEvaluator();
    for (Replica r : storagePerfModelEvaluator.getWriteOnlyWorkload(300, 500)) {
      System.out.println(r);
    }
  }

  @Test
  public void testAll() {
    StoragePerfModelEvaluator storagePerfModelEvaluator = new StoragePerfModelEvaluator();
    storagePerfModelEvaluator.readWriteWorkload = true;
    storagePerfModelEvaluator.storageType= "ST1";
    storagePerfModelEvaluator.readPercentage = 30;
    storagePerfModelEvaluator.run();
  }
}
