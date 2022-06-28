package com.carlmastrangelo.freecell.coder;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.Rank;
import com.carlmastrangelo.freecell.Suit;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;

final class BoardCoder {

  private static final int[] FIBS = new int[45];

  private static final BigInteger[][] COLUMN_DIV_COUNTS;

  static {
    FIBS[0] = 1;
    FIBS[1] = 2;
    for (int i = 2; i < FIBS.length; i++) {
      FIBS[i] = Math.addExact(FIBS[i - 1], FIBS[i - 2]);
    }

    COLUMN_DIV_COUNTS = fillColumnDivCounts(Card.CARD_COUNT, FreeCell.TABLEAU_COLS);
  }

  private static BigInteger[][] fillColumnDivCounts(int cardCount, int columns) {
    BigInteger[][] columnDivCounts = new BigInteger[cardCount + 1][];
    int maxColumnSize = 19;
    // assert maxColumnSize == Math.ceil(1.0 * cardCount / columns) + Rank.RANK_COUNT - 2;
    countColumnDiv(columnDivCounts, cardCount, columns, maxColumnSize);
    return columnDivCounts;
  }

  private static BigInteger countColumnDiv(
      final BigInteger[][] memo, final int elements, final int divisions, final int maxElementsPerColumn) {
    if (memo[elements] == null) {
      memo[elements] = new BigInteger[0];
    }
    if (memo[elements].length < divisions + 1) {
      memo[elements] = Arrays.copyOf(memo[elements], divisions + 1);
    }
    if (memo[elements][divisions] != null) {
      return memo[elements][divisions];
    }
    if (elements > Math.multiplyExact(maxElementsPerColumn, Math.addExact(divisions, 1))) {
      return (memo[elements][divisions] = BigInteger.ZERO);
    }
    if (divisions == 0) {
      return (memo[elements][divisions] = BigInteger.ONE);
    }
    BigInteger combos = BigInteger.ZERO;
    int maxElementsToKeep = Math.min(elements, maxElementsPerColumn);
    for (int elementsToKeep = 0; elementsToKeep <= maxElementsToKeep; elementsToKeep++) {
      combos = combos.add(countColumnDiv(memo, elements - elementsToKeep, divisions - 1, maxElementsToKeep));
    }
    return (memo[elements][divisions] = combos);
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
