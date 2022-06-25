package com.carlmastrangelo.freecell.coder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;

final class ArithmeticCoder {

  <T> BitSet arithmeticEncode(List<T> symbols, SortedMap<T, ? extends Number> probabilities) {
    List<SymbolProbability<T, ? extends Number>> symbolProbabilities =
        probabilities.entrySet().stream().map(e -> new SymbolProbability<>(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

    var symbolRanges = buildSymbolRanges(symbolProbabilities);

    List<BigDecimal> lows = new ArrayList<>();
    List<BigDecimal> highs = new ArrayList<>();
    lows.add(BigDecimal.ZERO);
    highs.add(BigDecimal.ONE);

    return new BitSet();
  }

  record SymbolRanges<T>(List<SymbolRange<T>> symbolProbabilities) {
    SymbolRanges {
      symbolProbabilities = validate(symbolProbabilities);
    }

    static <T> List<SymbolRange<T>> validate(List<SymbolRange<T>> symbolProbabilities) {
      // performs null checks implicitly
      symbolProbabilities = List.copyOf(symbolProbabilities);
      if (symbolProbabilities.isEmpty()) {
        throw new IllegalArgumentException("Empty Probabilities");
      }
      if (symbolProbabilities.get(0).probabilityRange().low().compareTo(BigDecimal.ZERO) != 0) {
        throw new IllegalArgumentException("Minimum probability not 0");
      }
      var lastRange = symbolProbabilities.get(symbolProbabilities.size() - 1);
      if (lastRange.probabilityRange().high().compareTo(BigDecimal.ONE) != 0) {
        throw new IllegalArgumentException("Cumulative Probability not 1");
      }
      for (int i = 0; i < symbolProbabilities.size() - 1; i++) {
        if (symbolProbabilities.get(i).probabilityRange().high()
            .compareTo(symbolProbabilities.get(i + 1).probabilityRange().low()) != 0) {
          throw new IllegalArgumentException("Probability orderings don't properly overlap");
        }
      }
      if (symbolProbabilities.stream().map(SymbolRange::symbol).collect(Collectors.toSet()).size()
          != symbolProbabilities.size()) {
        throw new IllegalArgumentException("Non-unique symbols in probability list");
      }
      return symbolProbabilities;
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
