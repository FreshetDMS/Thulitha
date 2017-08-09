/**
 * Copyright 2016 Milinda Pathirage
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

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;

@Data
public class Travel {

  private ArrayList<City> travel = new ArrayList<>();
  private ArrayList<City> previousTravel = new ArrayList<>();

  private final int numberOfCities;

  public Travel(int numberOfCities) {
    this.numberOfCities = numberOfCities;

    for (int i = 0; i < numberOfCities; i++){
      travel.add(new City());
    }
  }

  public void genInitialTravel() {
    if (travel.isEmpty()) {
      for (int i = 0; i < numberOfCities; i++){
        travel.add(new City());
      }
    }

    Collections.shuffle(travel);
  }

  public void swapCities() {
    int a = generateRandomIndex();
    int b = generateRandomIndex();
    previousTravel = travel;
    City x = travel.get(a);
    City y = travel.get(b);

    travel.set(a, y);
    travel.set(b, x);
  }

  public void revertSwap() {
    travel = previousTravel;
  }

  private int generateRandomIndex() {
    return (int) (Math.random() * travel.size());
  }

  public City getCity(int index) {
    return travel.get(index);
  }

  public int getDistance() {
    int distance = 0;
    for (int index = 0; index < travel.size(); index++) {
      City starting = getCity(index);
      City destination;
      if (index + 1 < travel.size()) {
        destination = getCity(index + 1);
      } else {
        destination = getCity(0);
      }
      distance += starting.distanceTo(destination);
    }
    return distance;
  }

}
