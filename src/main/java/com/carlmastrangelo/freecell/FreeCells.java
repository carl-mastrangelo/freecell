package com.carlmastrangelo.freecell;

import java.util.Arrays;
import java.util.Objects;

final class FreeCells {

  static final int NO_FREE_CELL = -1;
  static final int COLS = 8;

  private final Card[] cells;

  FreeCells() {
    this(new Card[COLS]);
  }

  private FreeCells(Card[] cells) {
    this.cells = cells;
  }

  void push(Card card) {
    assert card != null;
    if (!freeCell()) {
      throw new IllegalStateException();
    }
    for (int i = 0; i < cells.length; i++) {
      if (cells[i] == null) {
        cells[i] = card;
        return;
      }
      if (cells[i].ordinal() < card.ordinal()) {
        System.arraycopy(cells, i, cells, i + 1, cells.length - i - 1);
        cells[i] = card;
        return;
      }
    }
  }

  Card pop(int col) {
    Card card = cells[col];
    if (card == null) {
      throw new IllegalStateException();
    }
    System.arraycopy(cells, col + 1, cells, col, cells.length - col - 1);
    cells[cells.length - 1] = null;
    return card;
  }

  Card peek(int col) {
    return cells[col];
  }

  boolean freeCell() {
    return cells[COLS - 1] == null;
  }

  void reset() {
    Arrays.fill(cells, null);
  }

  FreeCells copy() {
    return new FreeCells(cells.clone());
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
