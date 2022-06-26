package com.carlmastrangelo.freecell.coder;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    var code = ArithmeticCoder.arithmeticEncode(
        List.of("a", "a", "b", "a", "c"),
        new TreeMap<>(Map.of("a", 0.5, "b", 0.375, "c", .125)));
  }
}
