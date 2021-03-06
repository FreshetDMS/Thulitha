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

  int maxMessageSize = 500;

  int minMessageSize = 300;

  int readPercentage = 0;

  void run() {
    if (!storageType.toUpperCase().equals("ST1") && !storageType.toUpperCase().equals("D2")) {
      throw new RuntimeException("Unknown storage type " + storageType);
    }

    List<Replica> replicas;

    if (readWriteWorkload) {
      replicas = getReadWriteWorkload(minMessageSize, maxMessageSize, readPercentage);
    } else {
      replicas = getWriteOnlyWorkload(minMessageSize, maxMessageSize);
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


    int i = 1;
    for (Replica r : result.getFirst()) {
      System.out.println("replayt" + i + " {");
      System.out.println("\tpartitions = 1");
      System.out.println("\treplication-factor = 1");
      System.out.println("\tmsg-size {");
      System.out.println("\t\tmean = " + r.getAvgMessageSize());
      System.out.println("\t\tstd = 40");
      System.out.println("\t\tdist = \"normal\"");
      System.out.println("\t}");
      System.out.println("\tproducers {");
      System.out.println("\t\tproducer-group-" + i + " {");
      System.out.println("\t\t\tuse-all-partitions = true");
      System.out.println("\t\t\ttasks = 1");
      System.out.println("\t\t\trate = " + r.getProduceRate());
      System.out.println("\t\t}");
      System.out.println("\t}");
      System.out.println("}");
      i++;
    }

    if (readWriteWorkload) {
      int j = 1;
      for (Replica r : result.getFirst()) {
        long msgProcTime = 1000000000 / r.getReplayRates()[0];
        System.out.println("replayt" + j + " {");
        System.out.println("\tpartitions = 1");
        System.out.println("\treplication-factor = 1");
        System.out.println("\tconsumers {");
        System.out.println("\t\tconsumer-group-" + j + " {");
        System.out.println("\t\t\ttasks = 1");
        System.out.println("\t\t\tdelay= 1");
        System.out.println("\t\t\tmsg-processing {");
        System.out.println("\t\t\t\ttype = \"constant\"");
        System.out.println("\t\t\t\tmean = " + msgProcTime);
        System.out.println("\t\t\t\tstddev = 20");
        System.out.println("\t\t\t}");
        System.out.println("\t\t}");
        System.out.println("\t}");
        System.out.println("}");
        j++;
      }
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

  List<Replica> getWriteOnlyWorkload(int minMessageSize, int maxMessageSize) {
    WorkloadGeneratorConfig workloadGeneratorConfig = new WorkloadGeneratorConfig(null);
    workloadGeneratorConfig.setMaxReplays(0);
    workloadGeneratorConfig.setMaxRetentionHours(2);
    workloadGeneratorConfig.setMinRetentionHours(1);
    workloadGeneratorConfig.setMinMessageSize(minMessageSize);
    workloadGeneratorConfig.setMaxMessageSize(maxMessageSize);


    List<Replica> replicas = new WorkloadGenerator(workloadGeneratorConfig).run(500);
    Collections.shuffle(replicas);
    return replicas.subList(0, 300);
  }

  List<Replica> getReadWriteWorkload(int minMessageSize, int maxMessageSize, int readPercentage) {
    WorkloadGeneratorConfig workloadGeneratorConfig = new WorkloadGeneratorConfig(null);
    workloadGeneratorConfig.setMaxReplays(-1);
    workloadGeneratorConfig.setMaxRetentionHours(2);
    workloadGeneratorConfig.setMinRetentionHours(1);
    workloadGeneratorConfig.setMinMessageSize(minMessageSize);
    workloadGeneratorConfig.setMaxMessageSize(maxMessageSize);
    workloadGeneratorConfig.setReadPercentage(readPercentage);

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
