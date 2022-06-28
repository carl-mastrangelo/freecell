package com.carlmastrangelo.freecell.coder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ArithmeticCoderTest {

  @Test
  public void buildSymbolProbabilities_doubles() {
    var probs = ArithmeticCoder.buildSymbolRanges(
        List.of(
            new ArithmeticCoder.SymbolProbability<>("a", 0.5),
            new ArithmeticCoder.SymbolProbability<>("b", 0.4),
            new ArithmeticCoder.SymbolProbability<>("c", 0.1)));
  }

  @Test
  public void arithmeticEncode_workflow() {
    var symbolRanges =
        ArithmeticCoder.convertProbabilityMap(new TreeMap<>(Map.of("a", .5, "b", .45, "c", .05)));

    var seed = new SplittableRandom().nextLong();
    //seed = 364570638067155961L;
    System.out.println(seed);
    int expected = 7;
    RandomGenerator rng = new SplittableRandom(seed);
    List<String> vals = new ArrayList<>();
    for (int i = 0; i < expected; i++) {
      var rand = rng.nextDouble();
      if (rand < .5) {
        vals.add("a");
      } else if (rand < .95) {
        vals.add("b");
      } else {
        vals.add("c");
      }
    }

    var code = ArithmeticCoder.arithmeticEncode(vals, symbolRanges);

    var decoder = new ArithmeticCoder.Decoder<>(symbolRanges);

    var decode = new ArrayList<String>();
    int top = code.bitsUsed();
    for (int pos = 0; pos <= top; pos++) {
      decoder.acceptBit(decode::add, code.bs().get(pos));
    }
    if (decode.size() < expected) {
      fail();
    }
    while (decode.size() > expected) {
      decode.remove(decode.size() - 1);
    }
    assertEquals(vals, decode);
  }

  @Test
  public void countPossible() {
    record NumPair(int remaining, int divs) {}

  }
}
