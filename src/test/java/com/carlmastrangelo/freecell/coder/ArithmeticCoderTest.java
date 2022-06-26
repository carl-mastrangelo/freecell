package com.carlmastrangelo.freecell.coder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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
        ArithmeticCoder.convertProbabilityMap(new TreeMap<>(Map.of("a", 0.5, "b", 0.375, "c", .125)));

    int expected = 100;
    RandomGenerator rng = new SplittableRandom();
    List<String> vals = new ArrayList<>();
    for (int i = 0; i < expected; i++) {
      var rand = rng.nextDouble();
      if (rand < .5) {
        vals.add("a");
      } else if (rand < .875) {
        vals.add("b");
      } else {
        vals.add("c");
      }
    }

    var code = ArithmeticCoder.arithmeticEncode(vals, symbolRanges);

    var decoder = new ArithmeticCoder.Decoder<>(symbolRanges);

    var decode = new ArrayList<String>();
    int top = code.bs().previousSetBit(Integer.MAX_VALUE);
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
}
