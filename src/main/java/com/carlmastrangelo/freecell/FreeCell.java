package com.carlmastrangelo.freecell;

import java.util.ArrayList;
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

  default FreeCell moveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    return moveToTableauFromTableau(dstTableauCol, srcTableauCol, 1);
  }

  FreeCell moveToTableauFromTableau(int dstTableauCol, int srcTableauCol, int count);

  default boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    return canMoveToTableauFromTableau(dstTableauCol, srcTableauCol, 1);
  }

  boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol, int count);

  FreeCell moveToTableauFromFreeCell(int dstTableauCol, int freeCol);

  boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol);

  /**
   * Returns the size of the alternating-color, increasing-rank run of cards on the column.
   */
  default int stackSize(int tableauCol) {
    // TODO: test
    var cards = new ArrayList<Card>();
    readTableau(cards, tableauCol);
    if (cards.size() == 0 || cards.size() == 1) {
      return cards.size();
    }
    int count = 1;
    for (int i = cards.size() - 1; i >= 1; i--) {
      Card card = cards.get(i);
      Card underCard = cards.get(i - 1);
      if (card.suit().color() == underCard.suit().color() || card.rank().num() != underCard.rank().num() - 1) {
        break;
      }
      count++;
    }
    return count;
  }

  void readTableau(Collection<? super Card> column, int tableauCol);

  @Nullable
  Card topHomeCell(Suit suit);
}
