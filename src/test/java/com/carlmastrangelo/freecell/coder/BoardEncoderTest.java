package com.carlmastrangelo.freecell.coder;

import static com.carlmastrangelo.freecell.coder.BoardCoder.decodeFibonacci;
import static com.carlmastrangelo.freecell.coder.BoardCoder.decodeZigZag;
import static com.carlmastrangelo.freecell.coder.BoardCoder.fibonacciEncode;
import static com.carlmastrangelo.freecell.coder.BoardCoder.encodeZigZag;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BoardEncoderTest {

  @Test
  public void zeroWorks() {
    assertEquals(0, decodeFibonacci(fibonacciEncode(0)));
  }

  @Test
  public void smallWorks() {
    for (int i = 0; i < 256; i++) {
      assertEquals(i, decodeFibonacci(fibonacciEncode(i)));
    }
  }

  @Test
  public void randomWorks() {
    long seed = new SplittableRandom().nextLong();
    RandomGenerator rand = new SplittableRandom(seed);
    for (int i = 0; i < 256; i++) {
      int value = rand.nextInt(Integer.MAX_VALUE);
      assertEquals("failed with seed " + seed, value, decodeFibonacci(fibonacciEncode(value)));
    }
  }

  @Test
  public void maxValueWorks() {
    assertEquals(Integer.MAX_VALUE, decodeFibonacci(fibonacciEncode(Integer.MAX_VALUE)));
  }

  @Test
  public void zigZag() {
    assertEquals(0, encodeZigZag(0));
    assertEquals(2, encodeZigZag(1));
    assertEquals(0xFF_FF_FF_FE, encodeZigZag(Integer.MAX_VALUE));
    assertEquals(0xFF_FF_FF_FF, encodeZigZag(Integer.MIN_VALUE));
  }

  @Test
  public void zigZagAndBack() {
    long seed = new SplittableRandom().nextLong();
    RandomGenerator rand = new SplittableRandom(seed);
    for (int i = 0; i < 256; i++) {
      int value = rand.nextInt();
      assertEquals("failed with seed " + seed, value, decodeZigZag(encodeZigZag(value)));
    }
  }

  @Test
  public void buildSymbolProbabilities_doubles() {
    var probs = BoardCoder.buildSymbolRanges(
        List.of(
            new BoardCoder.SymbolProbability<>("a", 0.5),
            new BoardCoder.SymbolProbability<>("b", 0.4),
            new BoardCoder.SymbolProbability<>("c", 0.1)));
    
  }


}
