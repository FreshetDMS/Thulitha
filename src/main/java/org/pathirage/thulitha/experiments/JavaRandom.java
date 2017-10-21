package org.pathirage.thulitha.experiments;

import java.util.Random;

public class JavaRandom {
  public static void main(String[] args) {
    Random random = new Random(System.currentTimeMillis());

    for (int i = 0; i < 20; i++ ) {
      System.out.println(random.nextInt(1));
    }
  }
}
