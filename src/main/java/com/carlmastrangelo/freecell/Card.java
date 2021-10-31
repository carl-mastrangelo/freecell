package com.carlmastrangelo.freecell;

import java.util.Arrays;
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

  static final Suit[] ALL_SUITS = Suit.values();
  static final Rank[] ALL_RANKS = Rank.values();
  static final Card[] ALL_CARDS_ORD = Card.values();

  private static final Card[][] SUITS_RANKS;
  private static final Card[][] RANKS_SUITS;

  static final int RANKS = 13;
  static final int SUITS = 4;
  static final int CARDS = 52;

  static {
    assert RANKS == ALL_RANKS.length;
    assert SUITS == ALL_SUITS.length;
    assert CARDS == ALL_CARDS_ORD.length;
    SUITS_RANKS = new Card[SUITS][];
    RANKS_SUITS = new Card[RANKS][];

    for (Rank rank : ALL_RANKS) {
      RANKS_SUITS[rank.ordinal()] = new Card[SUITS];
    }
    for (Suit suit : ALL_SUITS) {
      SUITS_RANKS[suit.ordinal()] = new Card[RANKS];
      for (Rank rank : ALL_RANKS) {
        int cardOrdinal = suit.ordinal() * RANKS + rank.ordinal();
        Card card = ALL_CARDS_ORD[cardOrdinal];
        assert card.rank() == rank;
        assert card.suit() == suit;
        assert card.ordinal() == cardOrdinal;
        SUITS_RANKS[suit.ordinal()][rank.ordinal()] = card;
        RANKS_SUITS[rank.ordinal()][suit.ordinal()] = card;
      }
    }
  }

  public enum Rank {
    ACE(1, 'A'),
    TWO(2, '2'),
    THREE(3, '3'),
    FOUR(4, '4'),
    FIVE(5, '5'),
    SIX(6, '6'),
    SEVEN(7, '7'),
    EIGHT(8, '8'),
    NINE(9, '9'),
    TEN(10, 'X'),
    JACK(11, 'J'),
    QUEEN(12, 'Q'),
    KING(13, 'K'),
    ;

    public static final int ACE_ORD = 0;
    public static final int KING_ORD = 12;

    static {
      assert ACE.ordinal() == ACE_ORD;
      assert KING.ordinal() == KING_ORD;
    }

    private final int num;
    private final String symbol;

    Rank(int num, char symbol) {
      this.num = num;
      this.symbol = String.valueOf(symbol);
    }

    public int num() {
      return num;
    }

    public String symbol() {
      return symbol;
    }

    @Nullable
    public Rank lower() {
      if (this == ACE) {
        return null;
      }
      return ALL_RANKS[ordinal() - 1];
    }

    @Nullable
    public Rank upper() {
      if (this == KING) {
        return null;
      }
      return ALL_RANKS[ordinal() + 1];
    }
  }

  public enum Suit {

    CLUBS("\u2667", "C", Color.BLACK),
    DIAMONDS("\u2662", "D", Color.RED),
    HEARTS("\u2661", "H", Color.RED),
    SPADES("\u2664", "S", Color.BLACK),
    ;

    private final String symbol;
    private final String altSymbol;
    private final Color color;

    Suit(String symbol, String altSymbol, Color color) {
      this.symbol = symbol;
      this.altSymbol = altSymbol;
      this.color = color;
    }

    public String symbol() {
      return symbol;
    }

    public Color color() {
      return color;
    }

    @Nullable
    public Suit lower() {
      if (this == CLUBS) {
        return null;
      }
      return ALL_SUITS[ordinal() - 1];
    }

    @Nullable
    public Suit upper() {
      if (this == SPADES) {
        return null;
      }
      return ALL_SUITS[ordinal() + 1];
    }
  }

  public enum Color {
    BLACK,
    RED;
  }
  
  private final Rank rank;
  private final Suit suit;
  private final String symbol;

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

  @Nullable
  public Card upperRank() {
    if (rank().upper() == null) {
      return null;
    }
    return RANKS_SUITS[rank().ordinal() + 1][suit().ordinal()];
  }

  @Nullable
  public Card lowerRank() {
    if (rank().lower() == null) {
      return null;
    }
    return RANKS_SUITS[rank().ordinal() - 1][suit().ordinal()];
  }

  @Nullable
  public Card upperSuit() {
    if (suit().upper() == null) {
      return null;
    }
    return RANKS_SUITS[rank().ordinal()][suit().ordinal() + 1];
  }

  @Nullable
  public Card lowerSuit() {
    if (suit().lower() == null) {
      return null;
    }
    return RANKS_SUITS[rank().ordinal()][suit().ordinal() - 1];
  }

  public static Card parse(String symbol) {

    Map<String, Card> cards =
        Arrays.stream(ALL_CARDS_ORD).collect(Collectors.toMap(c -> c.rank().symbol() + c.suit().altSymbol, c -> c));
    Card card = cards.get(symbol.toUpperCase(Locale.ROOT));
    if (card != null) {
      return card;
    }
    // Hacky, but not called often.
    Map<String, Card> revCards =
        Arrays.stream(ALL_CARDS_ORD).collect(Collectors.toMap(c -> c.suit().altSymbol + c.rank().symbol() , c -> c));
    card = revCards.get(symbol.toUpperCase(Locale.ROOT));
    if (card != null) {
      return card;
    }
    throw new IllegalArgumentException("Unknown cards " + card);
  }
}
