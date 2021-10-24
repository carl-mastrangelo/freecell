package com.carlmastrangelo.freecell;

import java.util.Arrays;
import java.util.Objects;

final class FreeCells {

  static final int COLS = 4;

  private final Card[] cells;
  private int free;

  FreeCells() {
    this(new Card[COLS], COLS);
  }

  private FreeCells(Card[] cells, int free) {
    this.cells = cells;
    this.free = free;
  }

  void push(Card card, int col) {
    assert card != null;
    if (cells[col] != null) {
      throw new IllegalStateException();
    }
    cells[col] = card;
    free--;
  }

  Card pop(int col) {
    Card card = cells[col];
    if (card == null) {
      throw new IllegalStateException();
    }
    cells[col] = null;
    free++;
    return card;
  }

  Card peek(int col) {
    return cells[col];
  }

  /**
   * Returns a free cell column, or {@code -1} if there are none.
   */
  int freeCell() {
    for (int i = 0; i < cells.length; i++) {
      if (cells[i] == null) {
        return i;
      }
    }
    return -1;
  }

  void reset() {
    Arrays.fill(cells, null);
    free = cells.length;
  }

  FreeCells copy() {
    return new FreeCells(cells.clone(), free);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FreeCells freeCells = (FreeCells) o;
    return Arrays.equals(cells, freeCells.cells);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(cells);
  }

  void toString(StringBuilder sb) {
    for (Card card : cells) {
      if (card != null) {
        sb.append(card);
      } else {
        sb.append("  ");
      }
      sb.append("  ");
    }
    sb.delete(sb.length() - 2, sb.length());
  }
}
