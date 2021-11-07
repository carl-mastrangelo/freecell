package com.carlmastrangelo.freecell;


import static com.carlmastrangelo.freecell.Card.CARDS_BY_ORD;
import static com.carlmastrangelo.freecell.Card.CARD_COUNT;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class LehmerBenchmark {

  private final LehmerCoder coder = new LehmerCoder(CARD_COUNT);
  private List<Card> cards;

  private int[] permdexes;
  private int[] altPermdexes;
  private BigInteger encode;

  @Setup
  public void setUp() {
    cards = new ArrayList<>(CARDS_BY_ORD);
    Collections.shuffle(cards);

    altPermdexes = coder.altPermdexes(cards, Card::ordinal);
    permdexes = coder.permdexes(new ArrayList<>(cards), CARDS_BY_ORD::get);
    encode = coder.encode(permdexes);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public int[] altPermdexes() {
    return coder.altPermdexes(cards, Card::ordinal);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public List<Card> altPermute() {
    List<Card> card = new ArrayList<>(CARD_COUNT);
    coder.altPermute(card, altPermdexes, CARDS_BY_ORD::get);
    return card;
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public int[] permdexes() {
    return coder.permdexes(new ArrayList<>(cards), CARDS_BY_ORD::get);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public List<Card> permute() {
    List<Card> card = new ArrayList<>(CARD_COUNT);
    coder.permute(card, permdexes, CARDS_BY_ORD::get);
    return card;
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public BigInteger encode() {
    return coder.encode(permdexes);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public int[] decode() {
    return coder.decode(encode);
  }

}
