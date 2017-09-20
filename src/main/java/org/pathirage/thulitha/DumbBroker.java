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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DumbBroker implements IBroker {
  private final List<Replica> replicas = new ArrayList<>();
  private final Set<String> partitions = new HashSet<>();

  @Override
  public List<Replica> getReplicas() {
    return replicas;
  }

  public boolean add(Replica replica) {
    if (partitions.contains(replica.getTopicPartition())) {
      return false;
    }

    replicas.add(replica);
    partitions.add(replica.getTopicPartition());

    return true;
  }



}
