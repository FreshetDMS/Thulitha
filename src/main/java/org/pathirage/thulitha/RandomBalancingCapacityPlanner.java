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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomBalancingCapacityPlanner extends CapacityPlanner {
  private final int binCount;

  private final Random randomizer = new Random(System.currentTimeMillis());

  RandomBalancingCapacityPlanner(List<Replica> replicas, CCInstanceType instanceType, StorageVolumeType storageVolumeType, boolean dynamic, int binCount) {
    super(replicas, instanceType, storageVolumeType, dynamic);
    this.binCount = binCount;
  }

  @Override
  public List<Broker> solve() {
    List<Broker> brokers = createBrokers(binCount);
    while (!replicas.isEmpty()) {
      Replica r = replicas.get(randomizer.nextInt(replicas.size()));

      boolean packed = false;
      Broker lastSelectedBroker = null;
      for (Broker b : brokers) {
        if (b.add(r)) {
          packed = true;
          lastSelectedBroker = b;
          break;
        }
      }

      if (!packed) {
        throw new RuntimeException("Could not pack replica " + r);
      }

      brokers.remove(lastSelectedBroker);
      brokers.add(lastSelectedBroker);

      replicas.remove(r);
    }

    return brokers;
  }



  List<Broker> createBrokers(int binCount) {
    List<Broker> brokers = new ArrayList<>();
    for (int i = 0; i < binCount; i++) {
      brokers.add(new Broker(instanceType, storageVolumeType, IO_OP_SIZE_128KB, true));
    }

    return brokers;
  }
}
