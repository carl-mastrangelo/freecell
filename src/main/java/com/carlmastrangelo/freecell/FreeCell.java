package com.carlmastrangelo.freecell;

import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGenerator;

public class FreeCell {

  private enum Color {
    BLACK,
    RED;
  }

  private enum Suit {
    CLUBS('\u2667', Color.BLACK),
    DIAMONDS('\u2662', Color.RED),
    HEARTS('\u2661', Color.RED),
    SPADES('\u2664', Color.BLACK);

    final char symbol;
    final Color color;

    Suit(char symbol, Color color) {
      this.symbol = symbol;
      this.color = color;
    }
  }

  private enum Rank {
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

    final int num;
    final char letter;

    Rank(int num, char letter) {
      this.num = num;
      this.letter = letter;
    }
  }

  record Card(Suit suit, Rank rank) {
    @Override
    public String toString() {
      return "" + suit.symbol + rank.letter;
    }
  }

  static final int RANKS = 13;
  static final int SUITS = 4;
  static final int CARDS = 52;
  static final int COLS = 8;
  private static final int ROWS = CARDS / COLS + (CARDS % COLS > 0 ? 1 : 0) + (RANKS - 1);

  private static final Suit[] ALL_SUITS = Suit.values();
  private static final Rank[] ALL_RANKS = Rank.values();
  private static final Card[] ALL_CARDS = new Card[RANKS * SUITS];

  static {
    assert RANKS == ALL_RANKS.length;
    assert SUITS == ALL_SUITS.length;
    int i = 0;
    for (Suit suit : ALL_SUITS) {
      for (Rank rank : ALL_RANKS) {
        ALL_CARDS[i++] = new Card(suit, rank);
      }
    }
  }

  private final Card[][] tableau;
  private final int[] tableauIdx;

  private final Card[] homeCells = new Card[SUITS];
  private final Card[] freeCells = new Card[4];

  private FreeCell() {
    this.tableau = new Card[8][];
    this.tableauIdx = new int[8];
    Arrays.fill(tableauIdx, -1);
  }

  void init(RandomGenerator rng) {
    Card[] cards = Arrays.copyOf(ALL_CARDS, ALL_CARDS.length);
    shuffle(cards, rng);
    for (int i = 0; i < tableau.length; i++) {
      tableau[i] = new Card[ROWS];
    }
    int col = 0;
    for (Card card : cards) {
      push(card, col++);
      if (col == COLS) {
        col = 0;
      }
    }
  }

  private void push(Card card, int col) {
    tableau[col][++tableauIdx[col]] = card;
  }

  private Card pop(int col) {
    Card card = peek(col);
    tableau[col][tableauIdx[col]--] = null;
    return card;
  }

  private Card peek(int col) {
    return tableau[col][tableauIdx[col]];
  }

  void moveHome(int tableauCol) {
    if (!canMoveHome(tableauCol)) {
      throw new IllegalArgumentException("Can't move card");
    }
    Card card = pop(tableauCol);
    homeCells[card.suit().ordinal()] = card;
  }

  boolean canMoveHome(int tableauCol) {
    Card card = peek(tableauCol);
    Card homeCell = homeCells[card.suit().ordinal()];
    if (homeCell == null) {
      return card.rank() == Rank.ACE;
    }
    return homeCell.rank().num == card.rank().num - 1;
  }

  boolean canMoveFree(int freeCol) {
    return freeCells[freeCol] == null;
  }

  void moveFree(int tableauCol, int freeCol) {
    if (!canMoveFree(freeCol)) {
      throw new IllegalArgumentException();
    }

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Card card : homeCells) {
      sb.append("  ");
      if (card != null) {
        sb.append(card);
      } else {
        sb.append("  ");
      }
    }
    sb.append("||");
    for (Card card : freeCells) {
      sb.append("  ");
      if (card != null) {
        sb.append(card);
      } else {
        sb.append("  ");
      }
    }
    sb.append("\n\n");
    for (int i = 0; i < ROWS; i++) {
      boolean cardOnRow = false;
      for (int col = 0; col < tableau.length; col++) {
        sb.append("  ");
        if (i <= tableauIdx[col]) {
          sb.append(tableau[col][i]);
          cardOnRow = true;
        } else {
          sb.append("  ");
        }
      }
      if (!cardOnRow) {
        sb.delete(sb.lastIndexOf("\n"), sb.length());
        break;
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  private static void shuffle(Card[] cards, RandomGenerator rng) {
    for (int i = cards.length; i > 1; i--) {
      Card c1 = cards[i - 1];
      int pos = rng.nextInt(i);
      Card c2 = cards[pos];

      cards[i - 1] = c2;
      cards[pos] = c1;
    }
  }

  public static void main(String [] args) {
    FreeCell freecell = new FreeCell();
    freecell.init(new Random());
    System.out.println(freecell);

    boolean done;
    do {
      done = true;
      for (int i = 0; i < FreeCell.COLS; i++) {
        if (freecell.canMoveHome(i)) {
          done = false;
          freecell.moveHome(i);
          System.out.println(freecell);
        }
      }
    } while (!done);

  }

}
