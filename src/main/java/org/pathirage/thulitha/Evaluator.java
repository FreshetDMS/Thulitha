/**
 * Copyright 2017 Milinda Pathirage
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pathirage.thulitha;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.pathirage.thulitha.workloads.WorkloadGenerator;
import org.pathirage.thulitha.workloads.WorkloadGeneratorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Evaluator {
  private static final Logger log = LoggerFactory.getLogger(Evaluator.class);

  @Parameter(names = {"-e", "--evaluation"}, required = true)
  private String evaluation;

  @Parameter(names = {"-T", "--instance-types"})
  private String instanceTypes;

  @Parameter(names = {"-lb"})
  private boolean startWithLowerBound = false;

  @Parameter(names = {"-u", "--upper-bound"})
  private int upperBound = 2000;

  @Parameter(names = {"-i", "--iterations"})
  private int iterations = 5;

  public static void main(String[] args) {
    Evaluator evaluator = new Evaluator();
    JCommander.newBuilder()
        .addObject(evaluator)
        .build()
        .parse(args);

    evaluator.run();
  }

  public void run() {
    if (evaluation.equals("cr")) {
      List<CCInstanceType> instanceTypes = getInstanceTypes();
      for (CCInstanceType t : instanceTypes) {
        List<Double> competitiveRatios = new ArrayList<>();
        for (int p = 0; p < iterations; p++) {
          log.info(String.format("Iteration %s of competitive ration calculation for instance type %s", p, t));
          List<Double> iterationsCompetitiveRatios = new ArrayList<>();
          for (int i = 500; i < upperBound; i += 400) {
            iterationsCompetitiveRatios.add(computeCompetitiveRatio(t, i));
          }

          competitiveRatios.add(Collections.max(iterationsCompetitiveRatios));
        }

        log.info(String.format("Competitive Ratio for instance %s = %s", t, Collections.max(competitiveRatios)));
      }

    } else if (evaluation.equals("et")) {
      List<CCInstanceType> instanceTypes = getInstanceTypes();
      Map<String, Double> executionTimes = new HashMap<>();
      for (CCInstanceType t : instanceTypes) {
        for (int r = 500; r < upperBound; r += 400) {
          List<Double> executionTime = new ArrayList<>();
          for (int i = 0; i < iterations; i++) {
            executionTime.add(measureExecutionTime(t, r));
          }

          executionTimes.put(String.format("%s-%s", t, r), calculateAverage(executionTime));
        }
      }

      log.info("Execution times: " + Arrays.toString(executionTimes.entrySet().toArray()));
    }
  }

  private double calculateAverage(List<Double> values) {
    double sum = 0;
    if (!values.isEmpty()) {
      for (Double mark : values) {
        sum += mark;
      }
      return sum / values.size();
    }
    return sum;
  }

  public double measureExecutionTime(CCInstanceType instanceType, int replicaCount) {
    List<Replica> replicas = getReplicas(replicaCount);

    StorageVolumeType storageVolumeType = getVolumeType(instanceType);

    BFDCapacityPlanner capacityPlanner = new BFDCapacityPlanner(replicas, instanceType, storageVolumeType, true, startWithLowerBound);
    long start = System.nanoTime();

    long solutionSize = capacityPlanner.solve().size();
    if (log.isDebugEnabled()) {
      log.debug("Solution size: " + solutionSize);
    }
    return (System.nanoTime() - start) / 1000000;
  }

  private List<CCInstanceType> getInstanceTypes() {
    if (instanceTypes == null || instanceTypes.isEmpty()) {
      throw new RuntimeException("Instance type argument is missing.");
    }

    String[] specifiedTypes = instanceTypes.split(";");
    List<CCInstanceType> instanceTypes = new ArrayList<>();

    for (String t : specifiedTypes) {
      instanceTypes.add(CCInstanceType.valueOf(t));
    }

    return instanceTypes;
  }

  private StorageVolumeType getVolumeType(CCInstanceType instanceType) {
    if (instanceType == CCInstanceType.D2_2X || instanceType == CCInstanceType.D2_4X || instanceType == CCInstanceType.D2_8X) {
      return StorageVolumeType.D2HDD;
    }
    return StorageVolumeType.ST1;
  }

  private List<Replica> getReplicas(int count) {
    return new WorkloadGenerator(new WorkloadGeneratorConfig(null)).run(count);
  }

  private double computeCompetitiveRatio(CCInstanceType instanceType, int replicaCount) {
    List<Replica> replicas = getReplicas(replicaCount);
    StorageVolumeType storageVolumeType = getVolumeType(instanceType);

    BFDCapacityPlanner capacityPlanner = new BFDCapacityPlanner(replicas, instanceType, storageVolumeType, true, startWithLowerBound);
    long optimalBrokers = capacityPlanner.lowestPossibleBrokersRequired();
    if (log.isDebugEnabled()) {
      log.debug("Max values for each dimension: " + Arrays.toString(capacityPlanner.getMaxRequirements()));
    }
    long solutionSize = capacityPlanner.solve().size();

    // TODO: Move to cost.

    return (double) solutionSize / optimalBrokers;
  }
}
