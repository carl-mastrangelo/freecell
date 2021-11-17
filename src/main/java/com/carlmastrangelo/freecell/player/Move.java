package com.carlmastrangelo.freecell.player;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.FreeCell;

sealed interface Move
    permits Move.MoveToHomeCellFromTableau, Move.MoveToFreeCellFromTableau, Move.MoveToTableauFromTableau,
    Move.MoveToHomeCellFromFreeCell, Move.MoveToTableauFromFreeCell {
  FreeCell play(FreeCell game);

  /**
   * May be called before {@link #play(FreeCell)}, but not after.  Describes the move about to be done.
   */
  void describe(StringBuilder sb, FreeCell game);

  record MoveToHomeCellFromTableau(int tableauCol) implements Move {
    @Override
    public FreeCell play(FreeCell game) {
      return game.moveToHomeCellFromTableau(tableauCol);
    }

    @Override
    public void describe(StringBuilder sb, FreeCell game) {
      Card card = game.peekTableau(tableauCol);
      sb.append("Move ").append(card.name()).append(" to home");
    }
  }

  record MoveToFreeCellFromTableau(int tableauCol) implements Move {
    @Override
    public FreeCell play(FreeCell game) {
      return game.moveToFreeCellFromTableau(tableauCol);
    }

    @Override
    public void describe(StringBuilder sb, FreeCell game) {
      Card card = game.peekTableau(tableauCol);
      sb.append("Move ")
          .append(card.name())
          .append(" to free cell");
    }
  }

  record MoveToTableauFromTableau(int dstTableauCol, int srcTableauCol, int count) implements Move {
    @Override
    public FreeCell play(FreeCell game) {
      return game.moveToTableauFromTableau(dstTableauCol, srcTableauCol, count);
    }

    @Override
    public void describe(StringBuilder sb, FreeCell game) {
      Card srcCard = game.peekTableau(srcTableauCol);
      Card dstCard = game.peekTableau(dstTableauCol);
      sb.append("Move() ").append(srcCard.name());
      if (dstCard == null) {
        sb.append(" onto empty tableau col ").append(dstTableauCol);
      } else {
        sb.append(" onto ").append(dstCard.name());
      }
    }
  }

  record MoveToHomeCellFromFreeCell(int freeCol) implements Move {
    @Override
    public FreeCell play(FreeCell game) {
      return game.moveToHomeCellFromFreeCell(freeCol);
    }

    @Override
    public void describe(StringBuilder sb, FreeCell game) {
      Card card = game.peekFreeCell(freeCol);
      sb.append("Move ").append(card.name()).append(" to home");
    }
  }

  record MoveToTableauFromFreeCell(int dstTableauCol, int freeCol) implements Move {
    @Override
    public FreeCell play(FreeCell game) {
      return game.moveToTableauFromFreeCell(dstTableauCol, freeCol);
    }

    @Override
    public void describe(StringBuilder sb, FreeCell game) {
      Card srcCard = game.peekFreeCell(freeCol);
      Card dstCard = game.peekTableau(dstTableauCol);
      sb.append("Move ").append(srcCard.name());
      if (dstCard == null) {
        sb.append(" onto empty tableau col ").append(dstTableauCol);
      } else {
        sb.append(" onto ").append(dstCard.name());
      }
    }
  }
}
