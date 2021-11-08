package com.carlmastrangelo.freecell;

import java.util.Collection;
import javax.annotation.Nullable;

public sealed interface FreeCell permits ForkFreeCell {

  int FREE_CELLS = 4;
  int TABLEAU_COLS = 8;

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

  void readTableau(Collection<? super Card> column, int tableauCol);

  @Nullable
  Card topHomeCell(Suit suit);
}
