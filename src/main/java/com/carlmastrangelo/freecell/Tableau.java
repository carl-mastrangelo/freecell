package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.ALL_CARDS_ORD;
import static com.carlmastrangelo.freecell.Card.CARDS;
import static com.carlmastrangelo.freecell.Card.RANKS;

import java.util.Arrays;

final class Tableau {
  static final int COLS = 8;
  static final int ROWS = CARDS / COLS + (CARDS % COLS > 0 ? 1 : 0) + (RANKS - 1);

  private final byte[][] cols;

  /**
   * Points to the top card in a column, or else -1.
   */
  private final int[] colTopIdx;

  Tableau() {
    this(initialCols(COLS, ROWS), initialColTopIdx(COLS));
  }

  private Tableau(byte[][] cols, int[] colTopIdx) {
    this.cols = cols;
    this.colTopIdx = colTopIdx;
  }

  void reset() {
    for (byte[] col : cols) {
      Arrays.fill(col, (byte) -1);
    }
    Arrays.fill(colTopIdx, -1);
  }

  Tableau copy() {
    byte[][] newCols = cols.clone();
    for (int i = 0; i < newCols.length; i++) {
      newCols[i] = newCols[i].clone();
    }
    int[] newColTopIdx = colTopIdx.clone();
    return new Tableau(newCols, newColTopIdx);
  }

  void push(Card card, int col) {
    assert card != null;
    cols[col][++colTopIdx[col]] = (byte)card.ordinal();
  }

  Card pop(int col) {
    Card card = peek(col);
    cols[col][colTopIdx[col]--] = -1;
    return card;
  }

  /**
   * Returns the top most card in the column, or else {@code null}.
   */
  Card peek(int col) {
    if (colTopIdx[col] == -1) {
      return null;
    }
    return Card.ALL_CARDS_ORD[cols[col][colTopIdx[col]]];
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
          sb.append(ALL_CARDS_ORD[cols[col][i]]);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Tableau tableau = (Tableau) o;
    return Arrays.deepEquals(cols, tableau.cols) && Arrays.equals(colTopIdx, tableau.colTopIdx);
  }

  @Override
  public int hashCode() {
    int result = Arrays.deepHashCode(cols);
    result = 31 * result + Arrays.hashCode(colTopIdx);
    return result;
  }

  private static byte[][] initialCols(int colCount, int rowCount) {
    byte[][] cols = new byte[colCount][];
    for (int colIdx = 0; colIdx < cols.length; colIdx++) {
      cols[colIdx] = new byte[rowCount];
    }
    return cols;
  }

  private static int[] initialColTopIdx(int colCount) {
    int[] colTopIdx = new int[colCount];
    Arrays.fill(colTopIdx, -1);
    return colTopIdx;
  }
}
