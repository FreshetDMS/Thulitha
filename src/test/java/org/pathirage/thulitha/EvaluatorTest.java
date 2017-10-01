package org.pathirage.thulitha;

import org.junit.Test;

public class EvaluatorTest {
  @Test
  public void testItemDistributionEvaluation() {
    Evaluator evaluator = new Evaluator();
    evaluator.evaluation = "bl";
    evaluator.instanceTypes = "M4_2X";
    evaluator.upperBound = 1000;
    evaluator.run();
  }
}
