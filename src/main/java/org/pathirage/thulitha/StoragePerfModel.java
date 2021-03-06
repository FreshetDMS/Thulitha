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

import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

class StoragePerfModel {
  static Classifier hddPerfModel() {
    return loadModelFromClassPath("models/hdd.model");
  }

  static Classifier st1PerfModel() {
    return loadModelFromClassPath("models/st1.model");
  }

  private static Classifier loadModelFromClassPath(String modelName) {
    ClassLoader classLoader = StoragePerfModel.class.getClassLoader();
    try {
      return (Classifier) SerializationHelper.read(classLoader.getResourceAsStream(modelName));
    } catch (Exception e) {
      throw new RuntimeException("Could not load weka model.", e);
    }
  }
}
