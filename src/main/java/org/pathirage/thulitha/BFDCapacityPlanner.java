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

import org.pathirage.thulitha.utils.SizeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BFDCapacityPlanner extends CapacityPlanner {
  private static final Logger log = LoggerFactory.getLogger(BFDCapacityPlanner.class);

  private final boolean startWithLowestPossible;

  public BFDCapacityPlanner(List<Replica> replicas, CCInstanceType instanceType, StorageVolumeType storageVolumeType, boolean dynamic, boolean startWithLowestPossible) {
    super(replicas, instanceType, storageVolumeType, dynamic);
    this.startWithLowestPossible = startWithLowestPossible;
  }

  @Override
  public List<Broker> solve() {
    int lowerBound = (int)computeLowestBinCount();
    List<Broker> solution;
    int i = 0;
    while(true) {
      try {
        solution = solve(lowerBound, new ArrayList<>(replicas));
        if (solution != null && solution.size() > 0) {
          break;
        }
      } catch (CapacityPlanningException e) {
        lowerBound += 1;
      }
      i++;
    }

    log.info("Number of iterations: " + i);

    return solution;
  }

  public List<Broker> solve(int binCount, List<Replica> replicas) throws CapacityPlanningException {
    if (binCount > replicas.size()) {
      throw new CapacityPlanningException("Could not find a solution.");
    }

    List<Broker> brokers = createBrokers(binCount);

    log.info(String.format("Solving capacity planning for %s replicas with initial brokers %s", replicas.size(), brokers.size()));

    SizeUtility.updateBrokerSize(brokers);
    SizeUtility.updateReplicaSize(replicas, brokers);
    Collections.sort(brokers);

    // TODO: Do we need to sort replicas first?
    while (!replicas.isEmpty()) {
      Replica largestReplica = Collections.max(replicas);

      if (dynamic) {
        Collections.sort(brokers);
      }

      boolean packed = false;
      for (Broker b : brokers) {
        if (b.add(largestReplica)) {
          packed = true;
          break;
        }
      }

      if (!packed) {
        throw new CapacityPlanningException("Could not pack replica " + largestReplica);
      }

      replicas.remove(largestReplica);
    }

    List<Broker> emptyBrokers = new ArrayList<>();
    for (Broker b : brokers) {
      if (b.isEmpty()) {
        emptyBrokers.add(b);
      }
    }

    log.info("Empty brokers " + emptyBrokers.size());

    brokers.removeAll(emptyBrokers);

    return brokers;
  }

  @Override
  long computeLowestBinCount() {
    if (startWithLowestPossible) {
      return super.computeLowestBinCount();
    }

    return replicas.size();
  }
}
