package com.carlmastrangelo.freecell;

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
  static final Card[] ALL_CARDS = Card.values();

  static final int RANKS = 13;
  static final int SUITS = 4;
  static final int CARDS = 52;

  static {
    assert RANKS == ALL_RANKS.length;
    assert SUITS == ALL_SUITS.length;
    assert CARDS == ALL_CARDS.length;
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
  }

  enum Suit {

    CLUBS('\u2667', Color.BLACK),
    DIAMONDS('\u2662', Color.RED),
    HEARTS('\u2661', Color.RED),
    SPADES('\u2664', Color.BLACK),
    ;

    private final String symbol;
    private final Color color;

    Suit(char symbol, Color color) {
      this.symbol = String.valueOf(symbol);
      this.color = color;
    }

    public String symbol() {
      return symbol;
    }

    public Color color() {
      return color;
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
    this.symbol = switch (suit) {
      case CLUBS -> Character.toString(0x1F0D0 + rank.num());
      case DIAMONDS -> Character.toString(0x1F0C0 + rank.num());
      case HEARTS -> Character.toString(0x1F0B0 + rank.num());
      case SPADES -> Character.toString(0x1F0A0 + rank.num());
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
}
