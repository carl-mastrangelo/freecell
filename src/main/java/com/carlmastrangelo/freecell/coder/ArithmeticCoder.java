package com.carlmastrangelo.freecell.coder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;

final class ArithmeticCoder {




  static <T> BitString arithmeticEncode(List<T> symbols, SortedMap<T, ? extends Number> probabilities) {
    List<SymbolProbability<T, ? extends Number>> symbolProbabilities =
        probabilities.entrySet().stream().map(e -> new SymbolProbability<>(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

    var symbolRanges = buildSymbolRanges(symbolProbabilities);

    // Core Encoding loop
    BigDecimal low = BigDecimal.ZERO;
    BigDecimal high = BigDecimal.ONE;
    for (T symbol : symbols) {
      BigDecimal oldRange = high.subtract(low);
      ProbabilityRange symRange = symbolRanges.rangeFor(symbol);
      BigDecimal newLow = low.add(oldRange.multiply(symRange.low()));
      BigDecimal newHigh = low.add(oldRange.multiply(symRange.high()));
      low = newLow;
      high = newHigh;
    }

    return encodeArithmetic(low, high);
  }


  static BitString encodeArithmetic(BigDecimal low, BigDecimal high) {
    var bs = new BitSet();
    int wpos = 0; // where in the bitset are we writing to next

    var lowReader = new BitReader(low);
    var highReader = new BitReader(high);

    // This code proceeds in 4 steps.
    // A.  Copy over the bits that are the same.
    // B.  Copy over the very first bit that is different.
    // C.  Copy over each bit that is a consecutive 1
    // D.  Copy over a 1 at the first 0.
    // Example:
    // Low:  1 0 1 1 0 1 1 0 1 1 0 1 0 1 1
    // High: 1 0 1 1 0 1 1 1 0 1 0 1 1 0 1
    //       A A A A A A A B C C D _ _ _ _
    // Res:  1 0 1 1 0 1 1 0 1 1 1 _ _ _ _

    while (true) {
      var lowBit = lowReader.readBit();
      var highBit = highReader.readBit();
      if (lowBit) {
        bs.set(wpos);
      }
      wpos++;
      if (lowBit != highBit) {
        break;
      }
    }

    while (true) {
      var lowBit = lowReader.readBit();
      bs.set(wpos++);
      if (!lowBit) {
        break;
      }
    }

    return new BitString(bs, wpos);
  }


  private static final class BitReader {
    private static final int BITS_PER_CHUNK = 3;
    private static final BigDecimal CHUNK_FACTOR = new BigDecimal("2").pow(BITS_PER_CHUNK);

    private BigDecimal num;
    private final boolean numIsOne;
    private long chunk;
    private int chunkBitsLeft;

    BitReader(BigDecimal num) {
      this.num = Objects.requireNonNull(num);
      int cmp = num.compareTo(BigDecimal.ONE);
      numIsOne = cmp == 0;
      if (cmp > 0) {
        throw new IllegalArgumentException();
      }
      if (num.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException();
      }
    }

    /**
     * Reads a single big Endian Bit from this number.
     */
    boolean readBit() {
      // If ever there was a useful end to the 1.0 != 0.999999999999999 argument, this is it.
      if (numIsOne) {
        return true;
      }
      if (chunkBitsLeft == 0) {
        BigDecimal[] chunkAndNum = num.multiply(CHUNK_FACTOR).divideAndRemainder(BigDecimal.ONE);
        num = chunkAndNum[1];
        chunk = chunkAndNum[0].longValueExact();
        chunkBitsLeft = BITS_PER_CHUNK;
      }
      long mask = 1L << (--chunkBitsLeft);
      return (chunk & mask) != 0;
    }
  }

  record BitString(BitSet bs, int bitsUsed) {}

  record SymbolRanges<T>(List<SymbolRange<T>> symbolRanges) {
    SymbolRanges {
      symbolRanges = validate(symbolRanges);
    }

    ProbabilityRange rangeFor(T symbol) {
      Objects.requireNonNull(symbol);
      // Haha computer go BRRRR
      for (var symRange : symbolRanges) {
        if (symbol.equals(symRange.symbol())) {
          return symRange.probabilityRange();
        }
      }
      throw new NoSuchElementException();
    }

    static <T> List<SymbolRange<T>> validate(List<SymbolRange<T>> symbolRanges) {
      // performs null checks implicitly
      symbolRanges = List.copyOf(symbolRanges);
      if (symbolRanges.isEmpty()) {
        throw new IllegalArgumentException("Empty Probabilities");
      }
      if (symbolRanges.get(0).probabilityRange().low().compareTo(BigDecimal.ZERO) != 0) {
        throw new IllegalArgumentException("Minimum probability not 0");
      }
      var lastRange = symbolRanges.get(symbolRanges.size() - 1);
      if (lastRange.probabilityRange().high().compareTo(BigDecimal.ONE) != 0) {
        throw new IllegalArgumentException("Cumulative Probability not 1");
      }
      for (int i = 0; i < symbolRanges.size() - 1; i++) {
        if (symbolRanges.get(i).probabilityRange().high()
            .compareTo(symbolRanges.get(i + 1).probabilityRange().low()) != 0) {
          throw new IllegalArgumentException("Probability orderings don't properly overlap");
        }
      }
      if (symbolRanges.stream().map(SymbolRange::symbol).collect(Collectors.toSet()).size() != symbolRanges.size()) {
        throw new IllegalArgumentException("Non-unique symbols in probability list");
      }
      return symbolRanges;
    }
  }

  record SymbolRange<T>(T symbol, ProbabilityRange probabilityRange) {
    SymbolRange {
      Objects.requireNonNull(symbol);
      Objects.requireNonNull(probabilityRange);
    }
  }

  record SymbolProbability<T, N extends Number>(T symbol, N probability) {
    SymbolProbability {
      Objects.requireNonNull(symbol);
      Objects.requireNonNull(probability);
    }
  }

  /**
   * @param low the inclusive lower bound for the probability
   * @param high the exclusive upper bound for the probability
   */
  record ProbabilityRange(BigDecimal low, BigDecimal high) {
    ProbabilityRange {
      Objects.requireNonNull(low);
      Objects.requireNonNull(high);
      if (low.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException();
      }
      if (high.compareTo(BigDecimal.ONE) > 0) {
        throw new IllegalArgumentException();
      }
      if (low.compareTo(high) > 0) {
        throw new IllegalArgumentException();
      }
    }
  }

  static <T> SymbolRanges<T> buildSymbolRanges(
      List<SymbolProbability<T, ? extends Number>> probabilities) {
    if (probabilities.isEmpty()) {
      throw new IllegalArgumentException("Empty Probability Set");
    }
    List<BigDecimal> cumProbabilities = new ArrayList<>(probabilities.size() + 1);
    cumProbabilities.add(BigDecimal.ZERO);

    for (int i = 0; i < probabilities.size(); i++) {
      Number prob = probabilities.get(i).probability();
      BigDecimal bigProb;
      if (prob instanceof BigDecimal bd) {
        bigProb = bd;
      } else {
        bigProb = BigDecimal.valueOf(prob.doubleValue());
      }
      if (bigProb.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Negative probabilities not allowed " + prob);
      }
      cumProbabilities.add(cumProbabilities.get(i).add(bigProb));
    }

    BigDecimal divisor;
    if ((divisor = cumProbabilities.get(cumProbabilities.size() - 1)).compareTo(BigDecimal.ONE) != 0) {
      if (divisor.compareTo(BigDecimal.ZERO) == 0) {
        throw new IllegalArgumentException("All probabilities zero");
      }
      for (int i = 1; i < cumProbabilities.size(); i++) {
        cumProbabilities.set(i, cumProbabilities.get(i).divide(divisor, MathContext.DECIMAL128));
      }
    }

    List<SymbolRange<T>> symbolRanges = new ArrayList<>(probabilities.size());
    for (int i = 0; i < probabilities.size(); i++) {
      symbolRanges.add(
          new SymbolRange<>(
              probabilities.get(i).symbol(),
              new ProbabilityRange(cumProbabilities.get(i), cumProbabilities.get(i + 1))));
    }

    return new SymbolRanges<>(symbolRanges);
  }

  private ArithmeticCoder() {}
}
