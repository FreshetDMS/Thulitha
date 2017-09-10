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
package org.pathirage.thulitha.experiments.storagemodel;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WekaDecisionTrees {

  private final String dataFile;

  public WekaDecisionTrees(String dataFile) {
    this.dataFile = dataFile;
  }

  private static BufferedReader readDataFile(String fileName) throws FileNotFoundException {
    ClassLoader classLoader = WekaDecisionTrees.class.getClassLoader();
    return new BufferedReader(new FileReader(classLoader.getResource(fileName).getFile()));
  }

  private Instances[][] crossValidationSplit(Instances data, int numberOfFolds) {
    Instances[][] split = new Instances[2][numberOfFolds];

    for (int i = 0; i < numberOfFolds; i++) {
      split[0][i] = data.trainCV(numberOfFolds, i);
      split[1][i] = data.testCV(numberOfFolds, i);
    }

    return split;
  }

  private Evaluation classify(Classifier model,
                              Instances trainingSet, Instances testingSet) throws Exception {
    Evaluation evaluation = new Evaluation(trainingSet);

    model.buildClassifier(trainingSet);
//    Instance i = new DenseInstance(4);
////    FastVector vals = new ArrayList<>();
////    vals.add("128k");
////    vals.add("64k");
////    i.setValue(new Attribute("id"), 40);
////    i.setValue(new Attribute("blocksize", vals), "128k");
////    i.setValue(new Attribute("writepct"), 35);
////    i.setValue(new Attribute("leaders"), 2);
////    i.setValue(new Attribute("follower"), 4);
////    System.out.println(model.classifyInstance(i));
    evaluation.evaluateModel(model, testingSet);

    return evaluation;
  }

  public void crossValidate() throws Exception {
    BufferedReader dataReader = readDataFile(this.dataFile);

    Instances data = new Instances(dataReader);
    data.setClassIndex(data.numAttributes() - 1);
    data.randomize(new Random(System.currentTimeMillis()));

    Instances[][] split = crossValidationSplit(data, 20);

    // Separate split into training and testing arrays
    Instances[] trainingSplits = split[0];
    Instances[] testingSplits = split[1];

    REPTree repTree = new REPTree();
    repTree.setMaxDepth(8);
    Classifier[] models = {
        repTree,
        new RandomForest(),
        new RandomTree()
    };

    for (int j = 0; j < models.length; j++) {
      List predictions = new ArrayList();
      System.out.println("\n Model: " + models[j].getClass().getSimpleName());
      double rsquared = 0;
      double mae = 0;
      double rmse = 0;

      for (int i = 0; i < trainingSplits.length; i++) {
        Evaluation validation = classify(models[j], trainingSplits[i], testingSplits[i]);
        rsquared += validation.correlationCoefficient();
        mae += validation.meanAbsoluteError();
        rmse += validation.rootMeanSquaredError();
      }

      System.out.println("R^2: " + rsquared / testingSplits.length + " MAE: " + mae / trainingSplits.length +
          " RMSE: " + rmse / trainingSplits.length);
    }
  }

  public static void buildAndPersistDTModel(String trainingDataPath, String modelOutput, int treeMaxDepth) throws Exception {
    BufferedReader dataReader = readDataFile(trainingDataPath);

    Instances data = new Instances(dataReader);
    data.setClassIndex(data.numAttributes() - 1);
    data.randomize(new Random(System.currentTimeMillis()));

    REPTree repTree = new REPTree();
    repTree.setMaxDepth(treeMaxDepth);

    repTree.buildClassifier(data);

    SerializationHelper.write(modelOutput, repTree);
  }

  public static void verifyModel(String modelPath) throws Exception {
    Classifier cls = (Classifier) weka.core.SerializationHelper.read(modelPath);

    Attribute writePct = new Attribute("writepct");
    Attribute leaders = new Attribute("leaders");
    Attribute followers = new Attribute("followers");
    Attribute iops = new Attribute("iops");

    ArrayList<Attribute> attributes = new ArrayList<>();
    attributes.add(writePct);
    attributes.add(leaders);
    attributes.add(followers);
    attributes.add(iops);

    Instances dataset = new Instances("test", attributes, 0);
    dataset.setClassIndex(attributes.size() - 1);
    double[] values = new double[attributes.size()];
    values[0] = 35;
    values[1] = 2;
    values[2] = 6;
    values[3] = Double.NaN;

    dataset.add(new DenseInstance(1.0, values));

    System.out.println(cls.classifyInstance(dataset.firstInstance()));

  }

  public static void main(String[] args) throws Exception {
//    WekaDecisionTrees w = new WekaDecisionTrees("hdd.arff");
//    w.crossValidate();
//    buildAndPersistDTModel("hdd.arff", "/Users/mpathira/Workspace/PhD/FreshetDMS/Thulitha/src/main/resources/models/hdd.model", 8);
    verifyModel( "/Users/mpathira/Workspace/PhD/FreshetDMS/Thulitha/src/main/resources/models/hdd.model");
  }

}
