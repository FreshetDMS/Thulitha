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
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.pathirage.thulitha.utils.SizeUtility;
import org.pathirage.thulitha.workloads.WorkloadGenerator;
import org.pathirage.thulitha.workloads.WorkloadGeneratorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Evaluator {
  private static final Logger log = LoggerFactory.getLogger(Evaluator.class);

  @Parameter(names = {"-e", "--evaluation"}, required = true)
  String evaluation;

  @Parameter(names = {"-T", "--instance-types"})
  String instanceTypes;

  @Parameter(names = {"-lb"})
  boolean startWithLowerBound = false;

  @Parameter(names = {"-u", "--upper-bound"})
  int upperBound = 2000;

  @Parameter(names = {"-i", "--iterations"})
  int iterations = 5;

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
    } else if (evaluation.equals("bl")) {
      List<CCInstanceType> instanceTypes = getInstanceTypes();
      Map<Integer, Map<CCInstanceType, Stat>> stats = new HashMap<>();
      List<Replica> replicas = new WorkloadGenerator(new WorkloadGeneratorConfig(null)).run(upperBound);
      for (int p = 0; p < 3; p++) {
        Map<CCInstanceType, Stat> typeStat = new HashMap<>();
        for (CCInstanceType t : instanceTypes) {
          typeStat.put(t, getWorkloadDistributionStats(t, new ArrayList<>(replicas), p));
        }
        stats.put(p, typeStat);
      }

      for (Map.Entry<Integer, Map<CCInstanceType, Stat>> stat : stats.entrySet()) {
        System.out.println("Planner: " + stat.getKey());
        System.out.println("Instance Type\t\tMean Size\t\tStd Dev\t\tBrokers\t\tNeg. Remaining");
        for (Map.Entry<CCInstanceType, Stat> entry : stat.getValue().entrySet()) {
          System.out.println(String.format("%s\t\t%s\t\t%s\t\t%s\t\t%s", entry.getKey(), entry.getValue().mean, entry.getValue().std, entry.getValue().brokers,entry.getValue().minusRemaining));
        }
      }
    }
  }

  private Stat computeDistributionStats(List<Broker> brokers, int planner) {
    DescriptiveStatistics statistics = new DescriptiveStatistics();
    List<Replica> replicas = new ArrayList<>();

    for (Broker b : brokers) {
      replicas.addAll(b.getReplicas());
    }

    long[] totalSizeOfItems = SizeUtility.computeTotalSizeOfItems(replicas);

    for (Broker b : brokers) {
      double totalSizeOfAssignedItems = 0;
      for (Replica r : b.getReplicas()) {
        for (int d = 0; d < totalSizeOfItems.length; d++) {
          totalSizeOfAssignedItems += (double) r.getDimension(d) / totalSizeOfItems[d];
        }
      }

      statistics.addValue(totalSizeOfAssignedItems);
    }

    int mr = 0;

    for (Broker b : brokers) {
      for (int d = 0; d < totalSizeOfItems.length; d++) {
        if (b.getRemainingCapacity(d) < 0) {
          if (planner == 0) {
            log.warn("Dimension " + d + " capacity minus.");
          }
          mr++;
        }
      }
    }


    return new Stat(statistics.getMean(), statistics.getStandardDeviation(), statistics.getMin(), statistics.getMax(), mr,replicas.size(), brokers.size());
  }

  private Stat getWorkloadDistributionStats(CCInstanceType instanceType, List<Replica> replicas, int planner) {

    if (planner == 0) {
      BFDCapacityPlanner capacityPlanner = new BFDCapacityPlanner(replicas, instanceType, getVolumeType(instanceType), true, startWithLowerBound);
      List<Broker> solution = capacityPlanner.solve();
      return computeDistributionStats(solution, planner);
    } else if (planner == 1) {
      List<Replica> replicasForBFDCP = new ArrayList<>(replicas);
      BFDCapacityPlanner capacityPlanner = new BFDCapacityPlanner(replicasForBFDCP, instanceType, getVolumeType(instanceType), true, startWithLowerBound);
      int brokerCount = capacityPlanner.solve().size();

      RandomCapacityPlanner randomCP = new RandomCapacityPlanner(replicas, instanceType, getVolumeType(instanceType), true, brokerCount);
      List<Broker> solution = randomCP.solve();

      return computeDistributionStats(solution, planner);
    } else if (planner == 2) {
      List<Replica> replicasForBFDCP = new ArrayList<>(replicas);
      BFDCapacityPlanner capacityPlanner = new BFDCapacityPlanner(replicasForBFDCP, instanceType, getVolumeType(instanceType), true, startWithLowerBound);
      int brokerCount = capacityPlanner.solve().size();

      RandomBalancingCapacityPlanner randomCP = new RandomBalancingCapacityPlanner(replicas, instanceType, getVolumeType(instanceType), true, brokerCount);
      List<Broker> solution = randomCP.solve();

      return computeDistributionStats(solution, planner);
    }

    throw new RuntimeException("Unsupported planner type " + planner);
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
