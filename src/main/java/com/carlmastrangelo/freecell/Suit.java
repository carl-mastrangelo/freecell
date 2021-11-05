package com.carlmastrangelo.freecell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public enum Suit {
  CLUBS("\u2667", "C", Color.BLACK),
  DIAMONDS("\u2662", "D", Color.RED),
  HEARTS("\u2661", "H", Color.RED),
  SPADES("\u2664", "S", Color.BLACK),
  ;

  public static final int SUIT_COUNT = 4;
  public static final List<Suit> SUITS_BY_ORD;
  private static final Map<String, Suit> SUITS_BY_SYMBOL;

  static {
    SUITS_BY_ORD = List.of(Suit.values());
    assert SUIT_COUNT == SUITS_BY_ORD.size();
    Map<String, Suit> suitMap = new LinkedHashMap<>();
    for (Suit suit : SUITS_BY_ORD) {
      suitMap.put(suit.symbol, suit);
      suitMap.put(suit.asciiSymbol, suit);
    }
    SUITS_BY_SYMBOL = Collections.unmodifiableMap(suitMap);
  }

  private final String symbol;
  private final String asciiSymbol;
  private final Color color;

  Suit(String symbol, String altSymbol, Color color) {
    this.symbol = symbol;
    this.asciiSymbol = altSymbol;
    this.color = color;
  }

  public static Suit ofSymbol(String symbol) {
    Objects.requireNonNull(symbol);
    Suit suit = SUITS_BY_SYMBOL.get(symbol);
    if (suit == null) {
      throw new IllegalArgumentException("Unknown suit " + symbol);
    }
    return suit;
  }

  public static Suit ofOrd(int ordinal) {
    return SUITS_BY_ORD.get(ordinal);
  }

  public String symbol() {
    return symbol;
  }

  public String asciiSymbol() {
    return asciiSymbol;
  }

  public Color color() {
    return color;
  }

  @Nullable
  public Suit lower() {
    if (this == CLUBS) {
      return null;
    }
    return SUITS_BY_ORD.get(ordinal() - 1);
  }

  @Nullable
  public Suit upper() {
    if (this == SPADES) {
      return null;
    }
    return SUITS_BY_ORD.get(ordinal() + 1);
  }

  public enum Color {
    BLACK,
    RED;

    public Color flip() {
      return this == BLACK ? RED : BLACK;
    }
  }
}
