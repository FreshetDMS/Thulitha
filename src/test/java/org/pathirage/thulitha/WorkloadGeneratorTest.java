package org.pathirage.thulitha;

import org.junit.Assert;
import org.junit.Test;
import org.pathirage.thulitha.workloads.WorkloadGenerator;
import org.pathirage.thulitha.workloads.WorkloadGeneratorConfig;

import java.util.List;

public class WorkloadGeneratorTest {

  @Test
  public void testWorkloadGeneratorSimple() {
    WorkloadGenerator workloadGenerator = new WorkloadGenerator(new WorkloadGeneratorConfig(null));

    List<Replica> replicaList = workloadGenerator.run(1000);
    Assert.assertTrue(replicaList.size() >= 500);
  }
}
