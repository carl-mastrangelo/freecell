package com.carlmastrangelo.freecell;

import java.math.BigInteger;
import java.util.BitSet;

final class BoardCoder {

  private static final int[] FIBS = new int[45];
  static {
    FIBS[0] = 1;
    FIBS[1] = 2;
    for (int i = 2; i < FIBS.length; i++) {
      FIBS[i] = Math.addExact(FIBS[i - 1], FIBS[i - 2]);
    }
  }

  BigInteger encode(FreeCell freeCell) {
    int[] homeRanks = new int[Suit.SUIT_COUNT];
    for (Suit s : Suit.SUITS_BY_ORD) {
      var top = freeCell.topHomeCell(s);
      if (top != null) {
        homeRanks[s.num() - 1] = top.rank().num();
      }
    }

    var bs = fibonacciEncode(homeRanks[0]);

    return null;

  }

  static int encodeZigZag(int num) {
    return  (num << 1) ^ (num >> 31);
  }

  static int decodeZigZag(int num) {
    return (num >>> 1) ^ -(num & 1);
  }

  static int decodeFibonacci(BitSet bits) {
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

  static BitSet fibonacciEncode(int value) {
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
