package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.ALL_CARDS;

import java.util.Random;
import java.util.random.RandomGenerator;

public final class FreeCell {

  private final Card[] homeCells = new Card[Card.SUITS];
  private final FreeCells freeCells = new FreeCells();
  private final Tableau tableau = new Tableau();

  private FreeCell() {
  }

  void init(RandomGenerator rng) {
    Card[] cards = ALL_CARDS.clone();
    shuffle(cards, rng);
    int col = 0;
    for (Card card : cards) {
      tableau.push(card, col++);
      if (col == Tableau.COLS) {
        col = 0;
      }
    }
  }



  void moveHome(int tableauCol) {
    if (!canMoveHome(tableauCol)) {
      throw new IllegalArgumentException("Can't move card");
    }
    Card card = tableau.pop(tableauCol);
    homeCells[card.suit().ordinal()] = card;
  }

  boolean canMoveHome(int tableauCol) {
    Card card = tableau.peek(tableauCol);
    Card homeCell = homeCells[card.suit().ordinal()];
    if (homeCell == null) {
      return card.rank() == Card.Rank.ACE;
    }
    return homeCell.rank().num() == card.rank().num() - 1;
  }

  boolean canMoveFree(int freeCol) {
    return freeCells.peek(freeCol) != null;
  }

  void moveToFree(int tableauCol, int freeCol) {
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
    freeCells.toString(sb);
    sb.append("\n\n");
    tableau.toString(sb);
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
      for (int i = 0; i < Tableau.COLS; i++) {
        if (freecell.canMoveHome(i)) {
          done = false;
          freecell.moveHome(i);
          System.out.println(freecell);
        }
      }
    } while (!done);

  }

}
