package com.carlmastrangelo.freecell;

final class FreeCells {

  static final int FREE_CELLS = 4;

  private final Card[] cells = new Card[FREE_CELLS];

  void push(Card card, int col) {
    assert card != null;
    if (cells[col] != null) {
      throw new IllegalArgumentException();
    }
    cells[col] = card;
  }

  Card pop(int col) {
    Card card = cells[col];
    cells[col] = null;
    return card;
  }

  Card peek(int col) {
    Card card = cells[col];
    if (card == null) {
      throw new IllegalArgumentException();
    }
    return card;
  }

  void toString(StringBuilder sb) {
    for (Card card : cells) {
      sb.append("  ");
      if (card != null) {
        sb.append(card);
      } else {
        sb.append("  ");
      }
    }
  }
}
