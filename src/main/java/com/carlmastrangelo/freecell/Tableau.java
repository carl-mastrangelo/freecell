package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.CARDS;
import static com.carlmastrangelo.freecell.Card.RANKS;

import java.util.Arrays;

final class Tableau {
  static final int COLS = 8;
  static final int ROWS = CARDS / COLS + (CARDS % COLS > 0 ? 1 : 0) + (RANKS - 1);

  private final Card[][] cols;
  /**
   * Points to the top card in a column, or else -1.
   */
  private final int[] colTopIdx;

  Tableau() {
    this.cols = new Card[COLS][];
    for (int colIdx = 0; colIdx < cols.length; colIdx++) {
      cols[colIdx] = new Card[ROWS];
    }
    this.colTopIdx = new int[COLS];
    Arrays.fill(colTopIdx, -1);
  }

  void push(Card card, int col) {
    assert card != null;
    cols[col][++colTopIdx[col]] = card;
  }

  Card pop(int col) {
    Card card = peek(col);
    cols[col][colTopIdx[col]--] = null;
    return card;
  }

  Card peek(int col) {
    return cols[col][cardsInCol(col) - 1];
  }

  /**
   * Returns the number of cards in a column, or {@code 0} if the column is empty.
   */
  int cardsInCol(int col) {
    return colTopIdx[col] + 1;
  }

  void toString(StringBuilder sb) {
    for (int i = 0; i < ROWS; i++) {
      boolean cardOnRow = false;
      for (int col = 0; col < COLS; col++) {
        sb.append("  ");
        if (i <= colTopIdx[col]) {
          sb.append(cols[col][i]);
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
  }
}
