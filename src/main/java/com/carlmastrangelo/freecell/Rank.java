package com.carlmastrangelo.freecell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public enum Rank {
  ACE(1, "A"),
  TWO(2, "2"),
  THREE(3, "3"),
  FOUR(4, "4"),
  FIVE(5, "5"),
  SIX(6, "6"),
  SEVEN(7, "7"),
  EIGHT(8, "8"),
  NINE(9, "9"),
  TEN(10, "T"),
  JACK(11, "J"),
  QUEEN(12, "Q"),
  KING(13, "K"),
  ;

  public static final int ACE_ORD = 0;
  public static final int KING_ORD = 12;
  public static final int RANK_COUNT = 13;

  private static final Rank[] RANKS_BY_ORD;
  private static final Map<String, Rank> RANKS_BY_SYMBOL;

  static {
    assert ACE.ordinal() == ACE_ORD;
    assert KING.ordinal() == KING_ORD;
    RANKS_BY_ORD = Rank.values();
    assert RANK_COUNT == RANKS_BY_ORD.length;
    Map<String, Rank> rankMap = new LinkedHashMap<>(32);
    for (Rank rank : RANKS_BY_ORD) {
      rankMap.put(rank.symbol, rank);
    }
    RANKS_BY_SYMBOL = Collections.unmodifiableMap(rankMap);
  }

  private final int num;
  private final String symbol;

  Rank(int num, String symbol) {
    this.num = num;
    this.symbol = symbol;
  }

  public int num() {
    return num;
  }

  public String symbol() {
    return symbol;
  }

  public Rank ofSymbol(String symbol) {
    Objects.requireNonNull(symbol);
    var rank = RANKS_BY_SYMBOL.get(symbol);
    if (rank == null) {
      throw new IllegalArgumentException("Unknown rank " + symbol);
    }
    return rank;
  }

  public Rank ofOrd(int ordinal) {
    return RANKS_BY_ORD[ordinal];
  }

  @Nullable
  public Rank lower() {
    if (this == ACE) {
      return null;
    }
    return RANKS_BY_ORD[ordinal() - 1];
  }

  @Nullable
  public Rank upper() {
    if (this == KING) {
      return null;
    }
    return RANKS_BY_ORD[ordinal() + 1];
  }
}
