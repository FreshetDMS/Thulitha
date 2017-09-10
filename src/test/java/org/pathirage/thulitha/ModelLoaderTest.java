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

import org.junit.Assert;
import org.junit.Test;
import weka.classifiers.Classifier;
import weka.classifiers.trees.REPTree;

public class ModelLoaderTest {
  @Test
  public void testLoadHDDModel() {
    Classifier classifier = StoragePerfModel.hddPerfModel();
    Assert.assertNotNull(classifier);
    Assert.assertEquals(REPTree.class, classifier.getClass());
  }

  @Test
  public void testLoadST1Model() {
    Classifier classifier = StoragePerfModel.st1PerfModel();
    Assert.assertNotNull(classifier);
    Assert.assertEquals(REPTree.class, classifier.getClass());
  }
}
