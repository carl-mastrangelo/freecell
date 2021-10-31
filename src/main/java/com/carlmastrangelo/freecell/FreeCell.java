package com.carlmastrangelo.freecell;

public sealed interface FreeCell<T extends FreeCell<T>> permits ForkFreeCell, MutableFreeCell {

  boolean gameWon();

  T moveToHomeCellFromTableau(int tableauCol);

  boolean canMoveToHomeCellFromTableau(int tableauCol);

  T moveToHomeCellFromFreeCell(int freeCol);

  boolean canMoveToHomeCellFromFreeCell(int freeCol);

  T moveToFreeCellFromTableau(int tableauCol);

  boolean canMoveToFreeCell();

  Card peekTableau(int tableauCol);

  Card peekFreeCell(int freeCol);

  T moveToTableauFromTableau(int dstTableauCol, int srcTableauCol);

  boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol);

  T moveToTableauFromFreeCell(int dstTableauCol, int freeCol);

  boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol);
}
