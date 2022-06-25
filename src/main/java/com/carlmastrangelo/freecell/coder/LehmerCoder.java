package com.carlmastrangelo.freecell.coder;

import static com.carlmastrangelo.freecell.Card.CARDS_BY_ORD;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

final class LehmerCoder {
  private final List<BigInteger> factorials;
  private final List<BigInteger> numbers;

  LehmerCoder(int maxElements) {
    this.factorials = new ArrayList<>(maxElements);
    this.numbers = new ArrayList<>(maxElements);
    var product = BigInteger.ONE;
    numbers.add(BigInteger.ZERO);
    for (int i = 1; i <= maxElements + 1; i++) {
      var bigI = BigInteger.valueOf(i);
      numbers.add(bigI);
      factorials.add(product);
      product = product.multiply(bigI);
    }
  }

  <T> List<T> permute(int[] permdexes, IntFunction<? extends T> ordFn) {
    var elems = new ArrayList<T>(permdexes.length);
    permute(elems, permdexes, ordFn);
    return elems;
  }

  /**
   * Given a permutation index and ordinal function fills a permutation List with the values.
   * e.g. [0 1 1 0] becomes [0 3 1 2].
   */
  <T> void permute(List<? super T> permutation, int[] permdexes, IntFunction<? extends T> ordFn) {
    assert permutation.isEmpty();
    for (int i = 0; i < permdexes.length; i++) {
      permutation.add(permdexes[permdexes.length - i - 1], ordFn.apply(permdexes.length - i - 1));
    }
  }

  /**
   * This destroys elems!  Returns the permutation indexes for each element in elems.
   * e.g. [0 3 1 2] becomes [0 1 1 0]
   */
  <T> int[] permdexes(List<? extends T> elems, IntFunction<? extends T> ordFn) {
    int[] permdexes = new int[elems.size()];
    for (int i = 0; i < permdexes.length; i++) {
      int idx = elems.indexOf(ordFn.apply(i));
      elems.remove(idx);
      permdexes[i] = idx;
    }
    return permdexes;
  }

  <T> int[] altPermdexes(List<? extends T> elems, ToIntFunction<? super T> ordFn) {
    int[] permdexes = new int[elems.size()];
    int i = 0;
    long bitset = 0;
    for (T elem : elems) {
      int ord = ordFn.applyAsInt(elem);
      long bit = 1L << ord;
      permdexes[i++] = ord - Long.bitCount(bitset & (bit - 1));
      bitset |= bit;
    }
    return permdexes;
  }

  <T> void altPermute(List<? super T> permutation, int[] permdexes, IntFunction<? extends T> ordFn) {
    long bitset = 0;
    for (int permdex : permdexes) {
      int lower;
      int nextlower = belowOrEqual(bitset, permdex);
      do {
        lower = nextlower;
        nextlower = belowOrEqual(bitset, permdex + lower);
      } while (lower != nextlower);
      permdex += lower;
      permutation.add(ordFn.apply(permdex));
      bitset |= 1L << permdex;
    }
  }

  private static int belowOrEqual(long bitset, int ord) {
    if (ord >= 64) {
      throw new IllegalArgumentException();
    }
    long mask = ord == 63 ? -1 : ((1L << (ord + 1)) - 1);
    return Long.bitCount(bitset & mask);
  }

  BigInteger encode(int[] radixes) {
    assert radixes[radixes.length - 1] == 0;
    BigInteger sum = BigInteger.ZERO;
    for (int i = 0; i < radixes.length; i++) {
      var fact = factorials.get(radixes.length - i - 1);
      var radix = numbers.get(radixes[i]);
      sum = sum.add(fact.multiply(radix));
    }
    return sum;
  }

  /**
   * Decodes a factoradix number into an array of radixes.
   */
  int[] decode(BigInteger factoradix) {
    // TODO: make this a binary search.
    for (int i = 0; ; i++) {
      if (factoradix.compareTo(factorials.get(i)) > 0) {
        continue;
      }
      return decode(i, factoradix);
    }
  }

  /**
   * Decodes a factoradix number into an array of radixes.
   */
  int[] decode(int radixCount, BigInteger factoradix) {
    int[] radixes = new int[radixCount];
    decode(radixes, factoradix);
    return radixes;
  }

  /**
   * Decodes a factoradix number into an array of radixes.
   */
  void decode(int[] radixes, BigInteger factoradix) {
    // normally this would be k >= 0, but since the final element is always 0, it doesn't matter.
    for (int i = 0, k = radixes.length - 1; k > 0; k--, i++) {
      BigInteger[] divmod = factoradix.divideAndRemainder(factorials.get(k));
      radixes[i] = divmod[0].intValueExact();
      factoradix = divmod[1];
    }
    assert factoradix.equals(BigInteger.ZERO);
  }
}
