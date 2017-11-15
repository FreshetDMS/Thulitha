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
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

public enum StorageVolumeType {
  IO1,
  GP2,
  ST1,
  D2HDD,
  ST1STATIC,
  D2HDDSTATIC;

  private static final String[] MODEL_IDENTIFIERS = {"io1", "gp2", "st1", "d2hdd", "st1.static", "d2hdd.static"};
  private static final float[] SIZES = {16 * 1024.0f * 1024.0f, 16 * 1024.0f * 1024.0f, 16 * 1024.0f * 1024.0f, 2 * 1024.0f * 1024.0f,
      16 * 1024.0f * 1024.0f, 2 * 1024.0f * 1024.0f};
  private static final float HOURLY_COST_FACTOR = 1.0f / (24 * 30);

  private static final Classifier hddModel = StoragePerfModel.hddPerfModel();
  private static final Classifier st1Model = StoragePerfModel.st1PerfModel();
  private static ArrayList<Attribute> attributes = new ArrayList<>();

  static {
    Attribute writePct = new Attribute("writepct");
    Attribute leaders = new Attribute("leaders");
    Attribute followers = new Attribute("followers");
    Attribute iops = new Attribute("iops");

    attributes.add(writePct);
    attributes.add(leaders);
    attributes.add(followers);
    attributes.add(iops);
  }

  public String getVolumeTypeIdentifier() {
    return MODEL_IDENTIFIERS[ordinal()];
  }

  public int getSizeMB() {
    return new Float(SIZES[ordinal()]).intValue();
  }

  public int getIOPS(int iopSizeKB, int storageBWMB) {
    if (this == ST1) {
      try {
        return new Double(Math.min(st1Model.classifyInstance(createRegressionRequest(50, 1, 0)), (storageBWMB * 1024.0) / iopSizeKB)).intValue();
      } catch (Exception e) {
        throw new RuntimeException("ST1 prediction failed.", e);
      }
    } else if (this == ST1STATIC) {
      return 2000;
    } else if (this == D2HDD || this == D2HDDSTATIC) {
      try {
        return new Double(Math.min(hddModel.classifyInstance(createRegressionRequest(50, 1, 0)), (storageBWMB * 1024.0) / iopSizeKB)).intValue();
      } catch (Exception e) {
        throw new RuntimeException("HDD prediction failed.", e);
      }
    } else {
      throw new UnsupportedOperationException("Storage volume type " + this + " not supported yet.");
    }
  }

  public int effectiveIOPS(int iopSizeKB, int storageBWMB, int writePct, int leaders, int followers) {
    if (this == ST1) {
      try {
        return new Double(Math.min(st1Model.classifyInstance(createRegressionRequest(writePct, leaders, followers)), (storageBWMB * 1024.0) / iopSizeKB)).intValue();
      } catch (Exception e) {
        throw new RuntimeException("ST1 prediction failed.", e);
      }
    } else if (this == D2HDD) {
      try {
        return new Double(Math.min(hddModel.classifyInstance(createRegressionRequest(writePct, leaders, followers)), (storageBWMB * 1024.0) / iopSizeKB)).intValue();
      } catch (Exception e) {
        throw new RuntimeException("HDD prediction failed.", e);
      }
    } else if (this == ST1STATIC || this == D2HDD) {
      return getIOPS(iopSizeKB, storageBWMB);
    } else {
      throw new UnsupportedOperationException("Storage volume type " + this + " not supported yet.");
    }
  }

  public double getHourlyCost(long size, long provisionedIOPS, int iopSizeKB) {
    double dr = (iopSizeKB * provisionedIOPS) / 1024.0;
    double requiredSize = 0;

    if (this == ST1 || this == ST1STATIC) {
      if (dr < 20) {
        requiredSize = 0.5 * 1000;
      } else if (dr < 40) {
        requiredSize = 1 * 1000;
      } else if (dr < 80) {
        requiredSize = 2 * 1000;
      } else if (dr < 120) {
        requiredSize = 3 * 1000;
      } else if (dr < 160) {
        requiredSize = 4 * 1000;
      } else if (dr < 200) {
        requiredSize = 5 * 1000;
      } else if (dr < 240) {
        requiredSize = 6 * 1000;
      } else if (dr < 280) {
        requiredSize = 7 * 1000;
      } else if (dr < 320) {
        requiredSize = 8 * 1000;
      } else if (dr < 360) {
        requiredSize = 9 * 1000;
      } else if (dr < 400) {
        requiredSize = 10 * 1000;
      } else if (dr < 440) {
        requiredSize = 11 * 1000;
      } else if (dr < 480) {
        requiredSize = 12 * 1000;
      } else {
        requiredSize = 12.5 * 1000;
      }
    }

    if (size < requiredSize) {
      size = new Double(requiredSize).intValue();
    }

    return HOURLY_COST_FACTOR * costFunction(size, provisionedIOPS);
  }

  private double costFunction(long size, long provisionedIOPS) {
    if (this == IO1) {
      return 0.125 * size + 0.065 * provisionedIOPS;
    } else if (this == GP2) {
      return 0.1 * size;
    } else if (this == ST1 || this == ST1STATIC) {
      return 0.045 * size;
    } else if (this == D2HDD || this == D2HDDSTATIC) {
      return 0;
    } else {
      throw new UnsupportedOperationException("Unsupported volume type " + this);
    }
  }

  private Instance createRegressionRequest(int writePct, int leaders, int followers) {
    Instances dataset = new Instances("test", attributes, 0);
    dataset.setClassIndex(attributes.size() - 1);

    double[] values = new double[4];
    values[0] = writePct;
    values[1] = leaders;
    values[2] = followers;
    values[3] = Double.NaN;

    dataset.add(new DenseInstance(1.0, values));

    return dataset.firstInstance();
  }
}
