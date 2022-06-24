package com.carlmastrangelo.freecell;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BoardEncoderTest {
  private static final int[] FIBS = new int[45];
  static {
    FIBS[0] = 1;
    FIBS[1] = 2;
    for (int i = 2; i < FIBS.length; i++) {
      FIBS[i] = Math.addExact(FIBS[i - 1], FIBS[i - 2]);
    }
  }

  @Test
  public void zeroWorks() {
    assertEquals(0, decode(encode(0)));
  }

  @Test
  public void smallWorks() {
    for (int i = 0; i < 256; i++) {
      assertEquals(i, decode(encode(i)));
    }
  }

  @Test
  public void randomWorks() {
    long seed = new SplittableRandom().nextLong();
    RandomGenerator rand = new SplittableRandom(seed);
    for (int i = 0; i < 256; i++) {
      int value = rand.nextInt(Integer.MAX_VALUE);
      assertEquals("failed with seed " + seed, value, decode(encode(value)));
    }
  }

  @Test
  public void maxValueWorks() {
    assertEquals(Integer.MAX_VALUE, decode(encode(Integer.MAX_VALUE)));
  }

  private static int decode(BitSet bits) {
    int top = bits.previousSetBit(Integer.MAX_VALUE);
    if (!bits.get(top - 1)) {
      throw new IllegalArgumentException("Not Fibonacci coded");
    }
    int value = 0;
    for (int i = 0; i < top; i++) {
      if (bits.get(i)) {
        value += FIBS[i];
      }
    }
    return value - 1;
  }

  private static BitSet encode(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Can't encode negative");
    }
    value += 1;
    BitSet bs = new BitSet();
    int i = FIBS.length - 1;
    while (true) {
      for (; i >= 0; i--) {
        if (FIBS[i] <= value || value == Integer.MIN_VALUE) {
          value -= FIBS[i];
          bs.set(i);
          break;
        }
      }
      if (value == 0) {
        break;
      }
    }
    bs.set(bs.previousSetBit(Integer.MAX_VALUE) + 1);
    return bs;
  }
}
