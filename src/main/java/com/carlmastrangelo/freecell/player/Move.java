package com.carlmastrangelo.freecell.player;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.MutableFreeCell;

sealed interface Move
    permits Move.MoveToHomeCellFromTableau, Move.MoveToFreeCellFromTableau, Move.MoveToTableauFromTableau,
    Move.MoveToHomeCellFromFreeCell, Move.MoveToTableauFromFreeCell {
  void play(MutableFreeCell game);

  /**
   * May be called before {@link #play(MutableFreeCell)}, but not after.  Describes the move about to be done.
   */
  void describe(StringBuilder sb, MutableFreeCell game);


  record MoveToHomeCellFromTableau(int tableauCol) implements Move {
    @Override
    public void play(MutableFreeCell game) {
      game.moveToHomeCellFromTableau(tableauCol);
    }

    @Override
    public void describe(StringBuilder sb, MutableFreeCell game) {
      Card card = game.peekTableau(tableauCol);
      sb.append("Move ").append(card.name()).append(" to home");
    }
  }

  record MoveToFreeCellFromTableau(int tableauCol) implements Move {
    @Override
    public void play(MutableFreeCell game) {
      game.moveToFreeCellFromTableau(tableauCol);
    }

    @Override
    public void describe(StringBuilder sb, MutableFreeCell game) {
      Card card = game.peekTableau(tableauCol);
      sb.append("Move ")
          .append(card.name())
          .append(" to free cell");
    }
  }

  record MoveToTableauFromTableau(int dstTableauCol, int srcTableauCol) implements Move {
    @Override
    public void play(MutableFreeCell game) {
      game.moveToTableauFromTableau(dstTableauCol, srcTableauCol);
    }

    @Override
    public void describe(StringBuilder sb, MutableFreeCell game) {
      Card srcCard = game.peekTableau(srcTableauCol);
      Card dstCard = game.peekTableau(dstTableauCol);
      sb.append("Move ").append(srcCard.name());
      if (dstCard == null) {
        sb.append(" onto empty tableau col ").append(dstTableauCol);
      } else {
        sb.append(" onto ").append(dstCard.name());
      }
    }
  }

  record MoveToHomeCellFromFreeCell(int freeCol) implements Move {
    @Override
    public void play(MutableFreeCell game) {
      game.moveToHomeCellFromFreeCell(freeCol);
    }

    @Override
    public void describe(StringBuilder sb, MutableFreeCell game) {
      Card card = game.peekFreeCell(freeCol);
      sb.append("Move ").append(card.name()).append(" to home");
    }
  }

  record MoveToTableauFromFreeCell(int dstTableauCol, int freeCol) implements Move {
    @Override
    public void play(MutableFreeCell game) {
      game.moveToTableauFromFreeCell(dstTableauCol, freeCol);
    }

    @Override
    public void describe(StringBuilder sb, MutableFreeCell game) {
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
