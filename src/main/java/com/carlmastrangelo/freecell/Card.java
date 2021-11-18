package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Rank.RANK_COUNT;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public enum Card {
  ACE_CLUBS(Rank.ACE, Suit.CLUBS),
  TWO_CLUBS(Rank.TWO, Suit.CLUBS),
  THREE_CLUBS(Rank.THREE, Suit.CLUBS),
  FOUR_CLUBS(Rank.FOUR, Suit.CLUBS),
  FIVE_CLUBS(Rank.FIVE, Suit.CLUBS),
  SIX_CLUBS(Rank.SIX, Suit.CLUBS),
  SEVEN_CLUBS(Rank.SEVEN, Suit.CLUBS),
  EIGHT_CLUBS(Rank.EIGHT, Suit.CLUBS),
  NINE_CLUBS(Rank.NINE, Suit.CLUBS),
  TEN_CLUBS(Rank.TEN, Suit.CLUBS),
  JACK_CLUBS(Rank.JACK, Suit.CLUBS),
  QUEEN_CLUBS(Rank.QUEEN, Suit.CLUBS),
  KING_CLUBS(Rank.KING, Suit.CLUBS),

  ACE_DIAMONDS(Rank.ACE, Suit.DIAMONDS),
  TWO_DIAMONDS(Rank.TWO, Suit.DIAMONDS),
  THREE_DIAMONDS(Rank.THREE, Suit.DIAMONDS),
  FOUR_DIAMONDS(Rank.FOUR, Suit.DIAMONDS),
  FIVE_DIAMONDS(Rank.FIVE, Suit.DIAMONDS),
  SIX_DIAMONDS(Rank.SIX, Suit.DIAMONDS),
  SEVEN_DIAMONDS(Rank.SEVEN, Suit.DIAMONDS),
  EIGHT_DIAMONDS(Rank.EIGHT, Suit.DIAMONDS),
  NINE_DIAMONDS(Rank.NINE, Suit.DIAMONDS),
  TEN_DIAMONDS(Rank.TEN, Suit.DIAMONDS),
  JACK_DIAMONDS(Rank.JACK, Suit.DIAMONDS),
  QUEEN_DIAMONDS(Rank.QUEEN, Suit.DIAMONDS),
  KING_DIAMONDS(Rank.KING, Suit.DIAMONDS),

  ACE_HEARTS(Rank.ACE, Suit.HEARTS),
  TWO_HEARTS(Rank.TWO, Suit.HEARTS),
  THREE_HEARTS(Rank.THREE, Suit.HEARTS),
  FOUR_HEARTS(Rank.FOUR, Suit.HEARTS),
  FIVE_HEARTS(Rank.FIVE, Suit.HEARTS),
  SIX_HEARTS(Rank.SIX, Suit.HEARTS),
  SEVEN_HEARTS(Rank.SEVEN, Suit.HEARTS),
  EIGHT_HEARTS(Rank.EIGHT, Suit.HEARTS),
  NINE_HEARTS(Rank.NINE, Suit.HEARTS),
  TEN_HEARTS(Rank.TEN, Suit.HEARTS),
  JACK_HEARTS(Rank.JACK, Suit.HEARTS),
  QUEEN_HEARTS(Rank.QUEEN, Suit.HEARTS),
  KING_HEARTS(Rank.KING, Suit.HEARTS),

  ACE_SPADES(Rank.ACE, Suit.SPADES),
  TWO_SPADES(Rank.TWO, Suit.SPADES),
  THREE_SPADES(Rank.THREE, Suit.SPADES),
  FOUR_SPADES(Rank.FOUR, Suit.SPADES),
  FIVE_SPADES(Rank.FIVE, Suit.SPADES),
  SIX_SPADES(Rank.SIX, Suit.SPADES),
  SEVEN_SPADES(Rank.SEVEN, Suit.SPADES),
  EIGHT_SPADES(Rank.EIGHT, Suit.SPADES),
  NINE_SPADES(Rank.NINE, Suit.SPADES),
  TEN_SPADES(Rank.TEN, Suit.SPADES),
  JACK_SPADES(Rank.JACK, Suit.SPADES),
  QUEEN_SPADES(Rank.QUEEN, Suit.SPADES),
  KING_SPADES(Rank.KING, Suit.SPADES),
  ;

  public static final int CARD_COUNT = 52;
  public static final List<Card> CARDS_BY_ORD;

  private static final Map<String, Card> CARDS_BY_SYMBOL;

  static {
    CARDS_BY_ORD = List.of(Card.values());
    assert CARD_COUNT == CARDS_BY_ORD.size();

    Map<String, Card> cardsBySymbol = new HashMap<>();
    for (Card  card : CARDS_BY_ORD) {
      cardsBySymbol.put(card.symbol, card);
      cardsBySymbol.put(card.asciiSymbol, card);
    }
    CARDS_BY_SYMBOL = Map.copyOf(cardsBySymbol);
  }

  private final Rank rank;
  private final Suit suit;
  private final String symbol;
  private final String asciiSymbol;

  Card(Rank rank, Suit suit) {
    this.rank = rank;
    this.suit = suit;
    int rankNum = rank.num() + (rank.num() >= 0xC ? 1 : 0);
    this.symbol = switch (suit) {
      case CLUBS -> Character.toString(0x1F0D0 + rankNum);
      case DIAMONDS -> Character.toString(0x1F0C0 + rankNum);
      case HEARTS -> Character.toString(0x1F0B0 + rankNum);
      case SPADES -> Character.toString(0x1F0A0 + rankNum);
    };
    this.asciiSymbol = rank.symbol() + suit.asciiSymbol();
  }

  public Rank rank() {
    return rank;
  }

  public Suit suit() {
    return suit;
  }

  @Override
  public String toString() {
    return symbol;
  }

  public String asciiSymbol() {
    return asciiSymbol;
  }

  @Nullable
  public Card upperRank() {
    if (rank().upper() == null) {
      return null;
    }
    return CARDS_BY_ORD.get(suit().ordinal() * RANK_COUNT  + rank().ordinal() + 1);
  }

  @Nullable
  public Card lowerRank() {
    if (rank().lower() == null) {
      return null;
    }
    return CARDS_BY_ORD.get(suit().ordinal() * RANK_COUNT  + rank().ordinal() - 1);
  }

  @Nullable
  public Card upperSuit() {
    if (suit().upper() == null) {
      return null;
    }
    return CARDS_BY_ORD.get((suit().ordinal() + 1) * RANK_COUNT  + rank().ordinal());
  }

  @Nullable
  public Card lowerSuit() {
    if (suit().lower() == null) {
      return null;
    }
    return CARDS_BY_ORD.get((suit().ordinal() - 1) * RANK_COUNT  + rank().ordinal());
  }

  public static Card ofSymbol(String symbol) {
    Card card = CARDS_BY_SYMBOL.get(symbol.toUpperCase(Locale.ROOT));
    if (card == null) {
      throw new IllegalArgumentException("Unknown card " + symbol);
    }
    return card;
  }
}
