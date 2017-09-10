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

public class Broker implements Comparable<Broker> {
  private final int[] capacity;
  private final int[] remainingCapacity;
  private final List<Replica> replicas;
  private double size = 0;

  public Broker(int[] capacity) {
    this.capacity = capacity;
    this.remainingCapacity = capacity;
    this.replicas = new ArrayList<>();
  }

  public int[] getCapacity() {
    return capacity;
  }

  public int[] getRemainingCapacity() {
    return remainingCapacity;
  }

  public List<Replica> getReplicas() {
    return replicas;
  }

  public double getSize() {
    return size;
  }

  public void setSize(double size) {
    this.size = size;
  }

  public int getDimensionCount(){
    return capacity.length;
  }

  @Override
  public int compareTo(Broker o) {
    return Double.compare(this.size, o.size);
  }
}
