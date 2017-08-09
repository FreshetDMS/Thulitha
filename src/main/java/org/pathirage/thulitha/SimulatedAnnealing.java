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

public class SimulatedAnnealing {
  public static double solve(int numberOfCities, double startingTemp, int numberOfIteration, double coolingRate) {
    Travel travel = new Travel(numberOfCities);
    System.out.println("Starting temp: " + startingTemp + " # of iterations: " + numberOfIteration + " and cooling rate: " + coolingRate);
    double t = startingTemp;
    travel.genInitialTravel();
    double  bestDistance = travel.getDistance();
    System.out.println("Initial distance of travel: " + bestDistance);
    Travel bestSolution = travel;
    Travel currentSolution = bestSolution;

    for (int i = 0; i < numberOfCities; i++) {
      if ( t > 0.1) {
        currentSolution.swapCities();
        double currentDistance = currentSolution.getDistance();
        if (currentDistance < bestDistance) {
          bestDistance = currentDistance;
        } else if (Math.exp((bestDistance - currentDistance)/t) < Math.random()) {
          currentSolution.revertSwap();
        }

        t *= coolingRate;
      } else {
        
      }
    }
  }
}
