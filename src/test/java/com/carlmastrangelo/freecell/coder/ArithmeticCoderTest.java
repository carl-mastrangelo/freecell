package com.carlmastrangelo.freecell.coder;

import java.util.List;
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
}
