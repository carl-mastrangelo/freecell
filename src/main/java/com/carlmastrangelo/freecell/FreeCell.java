package com.carlmastrangelo.freecell;

import javax.annotation.Nullable;

public sealed interface FreeCell permits ForkFreeCell, MutableFreeCell {

  boolean gameWon();

  FreeCell moveToHomeCellFromTableau(int tableauCol);

  boolean canMoveToHomeCellFromTableau(int tableauCol);

  FreeCell moveToHomeCellFromFreeCell(int freeCol);

  boolean canMoveToHomeCellFromFreeCell(int freeCol);

  FreeCell moveToFreeCellFromTableau(int tableauCol);

  boolean canMoveToFreeCellFromTableau(int tableauCol);

  @Nullable
  Card peekTableau(int tableauCol);

  @Nullable
  Card peekFreeCell(int freeCol);

  FreeCell moveToTableauFromTableau(int dstTableauCol, int srcTableauCol);

  boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol);

  FreeCell moveToTableauFromFreeCell(int dstTableauCol, int freeCol);

  boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol);

  @Nullable
  Card topHomeCell(Card.Suit suit);
}
