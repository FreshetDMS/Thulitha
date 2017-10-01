package org.pathirage.thulitha;

public class Stat {
  double mean;
  double std;
  double min;
  double max;
  int minusRemaining;
  int replicas;
  int brokers;

  public Stat(double mean, double std, double min, double max, int minusRemaining, int replicas, int brokers) {
    this.mean = mean;
    this.std = std;
    this.min = min;
    this.max = max;
    this.minusRemaining = minusRemaining;
    this.replicas = replicas;
    this.brokers = brokers;
  }

  @Override
  public String toString() {
    return "Stat{" +
        "mean=" + mean +
        ", std=" + std +
        ", min=" + min +
        ", max=" + max +
        ", minusRemaining=" + minusRemaining +
        ", replicas=" + replicas +
        ", brokers=" + brokers +
        '}';
  }
}
