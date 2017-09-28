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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Evaluator {

  @Parameter(names = {"-e", "--evaluation"}, required = true)
  private String evaluation;

  @Parameter(names = {"-T", "--instance-types"})
  private String instanceTypes;

  public static void main(String[] args) {
    Evaluator evaluator = new Evaluator();
    JCommander.newBuilder()
        .addObject(evaluator)
        .build()
        .parse(args);

    evaluator.run();
  }

  public void run(){
    if (evaluation.equals("cr")) {
      if (instanceTypes == null || instanceTypes.isEmpty()) {
        throw new RuntimeException("Instance type argument is missing.");
      }

      String[] specifiedTypes = instanceTypes.split(";");
      List<Double> competitiveRatios = new ArrayList<>();
      for(String t : specifiedTypes) {
        CCInstanceType instanceType = CCInstanceType.valueOf(t.trim().toUpperCase());
        for(int i = 500; i < 2000; i += 100) {
          competitiveRatios.add(computeCompetitiveRatio(instanceType, i));
        }
      }

      System.out.println("Competitive Ratio = " + Collections.max(competitiveRatios));
    }
  }

  private StorageVolumeType getVolumeType(CCInstanceType instanceType) {
    return StorageVolumeType.ST1;
  }

  private List<Replica> getReplicas(int count) {
    return Collections.emptyList();
  }

  private double computeCompetitiveRatio(CCInstanceType instanceType, int replicaCount) {
    List<Replica> replicas = getReplicas(replicaCount);
    StorageVolumeType storageVolumeType = getVolumeType(instanceType);

    BFDCapacityPlanner capacityPlanner = new BFDCapacityPlanner(replicas, instanceType, storageVolumeType, true, false);
    long optimalBrokers = capacityPlanner.lowestPossibleBrokersRequired();
    long solutionSize = capacityPlanner.solve().size();
    return (double)solutionSize/optimalBrokers;
  }
}
