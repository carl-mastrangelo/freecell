package com.carlmastrangelo.freecell;

public sealed interface FreeCell permits MutableFreeCell {

  boolean gameWon();

  void moveToHomeCellFromTableau(int tableauCol);

  boolean canMoveToHomeCellFromTableau(int tableauCol);

  void moveToHomeCellFromFreeCell(int freeCol);

  boolean canMoveToHomeCellFromFreeCell(int freeCol);

  void moveToFreeCellFromTableau(int tableauCol);

  boolean canMoveToFreeCell();

  Card peekTableau(int tableauCol);

  Card peekFreeCell(int freeCol);

  void moveToTableauFromTableau(int dstTableauCol, int srcTableauCol);

  boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol);

  void moveToTableauFromFreeCell(int dstTableauCol, int freeCol);

  boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol);
}
