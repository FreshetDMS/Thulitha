package org.pathirage.thulitha;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.math3.util.Pair;
import org.pathirage.thulitha.workloads.WorkloadGenerator;
import org.pathirage.thulitha.workloads.WorkloadGeneratorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoragePerfModelEvaluator {
  private static final Logger log = LoggerFactory.getLogger(StoragePerfModelEvaluator.class);

  @Parameter(names = {"-s", "--storage-type"}, required = true)
  String storageType;

  @Parameter(names = {"-rw"})
  boolean readWriteWorkload = false;

  void run() {
    if (!storageType.toUpperCase().equals("ST1") && !storageType.toUpperCase().equals("D2")) {
      throw new RuntimeException("Unknown storage type " + storageType);
    }

    List<Replica> replicas;

    if (readWriteWorkload) {
      replicas = getReadWriteWorkload();
    } else {
      replicas = getWriteOnlyWorkload();
    }

    if (storageType.toUpperCase().equals("ST1")) {
      printAssignment(getAssignment(new ArrayList<>(replicas), StorageVolumeType.ST1), StorageVolumeType.ST1);
      printAssignment(getAssignment(new ArrayList<>(replicas), StorageVolumeType.ST1STATIC), StorageVolumeType.ST1STATIC);
    } else if (storageType.toUpperCase().equals("D2")) {
      printAssignment(getAssignment(new ArrayList<>(replicas), StorageVolumeType.D2HDD), StorageVolumeType.D2HDD);
      printAssignment(getAssignment(new ArrayList<>(replicas), StorageVolumeType.D2HDDSTATIC), StorageVolumeType.D2HDDSTATIC);
    }
  }

  void printAssignment(Pair<List<Replica>, Integer> result, StorageVolumeType storageVolumeType) {
    System.out.println("Storage Volume Type: " + storageVolumeType);
    System.out.println("Effective IOPS: " + result.getSecond());
    System.out.println("Size: " + storageVolumeType.getSizeMB());
    System.out.println("Num Replicas: " + result.getFirst().size());

    int totlaIOPS = 0;
    int totalSize = 0;
    for (Replica r : result.getFirst()) {
      totlaIOPS += ((r.getDimension(2) * 1024.0) / 128);
      totalSize += r.getDimension(1);
    }

    System.out.println("Total IOPS: " + totlaIOPS);
    System.out.println("Total Size: " + totalSize);
    for (Replica r : result.getFirst()) {
      System.out.println(r.toString());
    }
    System.out.println("");
    System.out.println("");
  }

  private Pair<List<Replica>, Integer> getAssignment(List<Replica> replicas, StorageVolumeType storageVolumeType) {
    CCInstanceType instanceType;
    if (storageVolumeType == StorageVolumeType.ST1 || storageVolumeType == StorageVolumeType.ST1STATIC) {
      instanceType = CCInstanceType.M4_10X;
    } else {
      instanceType = CCInstanceType.D2_4X;
    }
    StorageVolume sv = new StorageVolume("b1", storageVolumeType, instanceType, 128);

    for (Replica r : replicas) {
      if (sv.isFeasible(r)) {
        sv.addReplica(r, true);
      } else {
        log.warn("Replica " + r + " does not fit.");
        break;
      }
    }

    return new Pair<List<Replica>, Integer>(sv.getReplicas(), sv.effectiveIOPS());
  }

  List<Replica> getWriteOnlyWorkload() {
    WorkloadGeneratorConfig workloadGeneratorConfig = new WorkloadGeneratorConfig(null);
    workloadGeneratorConfig.setMaxReplays(0);
    workloadGeneratorConfig.setMaxRetentionHours(2);
    workloadGeneratorConfig.setMinRetentionHours(1);

    List<Replica> replicas = new WorkloadGenerator(workloadGeneratorConfig).run(500);
    Collections.shuffle(replicas);
    return replicas.subList(0, 300);
  }

  List<Replica> getReadWriteWorkload() {
    WorkloadGeneratorConfig workloadGeneratorConfig = new WorkloadGeneratorConfig(null);
    workloadGeneratorConfig.setMaxReplays(3);

    List<Replica> replicas = new WorkloadGenerator(workloadGeneratorConfig).run(500);
    Collections.shuffle(replicas);
    return replicas.subList(0, 300);
  }

  public static void main(String[] args) {
    StoragePerfModelEvaluator storagePerfModelEvaluator = new StoragePerfModelEvaluator();
    JCommander.newBuilder()
        .addObject(storagePerfModelEvaluator)
        .build()
        .parse(args);

    storagePerfModelEvaluator.run();
  }
}
