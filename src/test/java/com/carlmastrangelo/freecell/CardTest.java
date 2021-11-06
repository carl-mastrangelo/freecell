package com.carlmastrangelo.freecell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CardTest {

  @Test
  public void rankLower() {
    assertNull(Rank.ACE.lower());
    assertEquals(Rank.ACE, Rank.TWO.lower());
    assertEquals(Rank.QUEEN, Rank.KING.lower());
  }

  @Test
  public void rankUpper() {
    assertNull(Rank.KING.upper());
    assertEquals(Rank.KING, Rank.QUEEN.upper());
    assertEquals(Rank.TWO, Rank.ACE.upper());
  }

  @Test
  public void suitLower() {
    assertNull(Suit.CLUBS.lower());
    assertEquals(Suit.CLUBS, Suit.DIAMONDS.lower());
    assertEquals(Suit.HEARTS, Suit.SPADES.lower());
  }

  @Test
  public void suitUpper() {
    assertNull(Suit.SPADES.upper());
    assertEquals(Suit.SPADES, Suit.HEARTS.upper());
    assertEquals(Suit.DIAMONDS, Suit.CLUBS.upper());
  }

  @Test
  public void suitUpper1() {
    final int nums = 52;
    var s = new ArrayList<Integer>();
    for (int i = 0; i < nums; i++) {
      s.add(i);
    }
    Collections.shuffle(s);

    var order = new ArrayList<Integer>();
    for (int i = 0; i < nums; i++) {
      int idx = s.indexOf(i);
      s.remove(idx);
      order.add(idx);
    }
    var code = encode(order);
    System.out.println(code);

    var newOrds = decode(52, code);
    if (!Arrays.stream(newOrds).boxed().collect(Collectors.toList()).equals(order)) {
      throw new AssertionError();
    }

    System.out.println(code.toByteArray().length);
  }

  private static final List<BigInteger> factorials =
      new ArrayList<>();

  static {
    var product = BigInteger.ONE;
    for (int i = 1; i <= 52 + 1; i++) {
      factorials.add(product);
      product = product.multiply(BigInteger.valueOf(i));
    }
  }

  private static BigInteger encode(List<Integer> ords) {
    int off = ords.size();
    BigInteger sum = BigInteger.ZERO;
    for (int i = 0; i < ords.size(); i++) {
      var fact = factorials.get(off - i - 1);
      var ord = BigInteger.valueOf(ords.get(i));
      sum = sum.add(fact.multiply(ord));
    }
    return sum;
  }

  private int[] decode(BigInteger factoradix) {
    for (int i = 0; ; i++) {
      if (factoradix.compareTo(factorials.get(i)) > 0) {
        continue;
      }
      return decode(i, factoradix);
    }
  }

  private int[] decode(int ordCount, BigInteger factoradix) {
    int[] ords = new int[ordCount];
    decode(ords, factoradix);
    return ords;
  }

  private void decode(int[] ords, BigInteger factoradix) {
    // normally this would be k >= 0, but since the final element is always 0, it doesn't matter.
    for (int i = 0, k = ords.length - 1; k > 0; k--, i++) {
      BigInteger[] divmod = factoradix.divideAndRemainder(factorials.get(k));
      ords[i] = divmod[0].intValueExact();
      factoradix = divmod[1];
    }
  }
}
