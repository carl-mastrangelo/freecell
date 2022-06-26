package com.carlmastrangelo.freecell.player;

import static com.carlmastrangelo.freecell.FreeCell.FREE_CELLS;
import static com.carlmastrangelo.freecell.FreeCell.TABLEAU_COLS;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.ForkFreeCell;
import com.carlmastrangelo.freecell.FreeCell;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public final class GameProfiler {

  public static void main(String [] args) {
    var rngf = RandomGeneratorFactory.<RandomGenerator.SplittableGenerator>of("L64X256MixRandom");
    List<int[]> columnDiffs = new ArrayList<>();
    int gameCount = 10000;
    int moveCount = 100;
    int bias = 6;
    List<Move> moves = new ArrayList<>();
    for (int i = 0; i < gameCount; i++) {
      Deque<FreeCell> games = new ArrayDeque<>();
      games.addFirst(ForkFreeCell.dealDeck(rngf.create()));
      Set<FreeCell> seen = new HashSet<>();
      for (int k = 0; k < moveCount; k++) {
        FreeCell game = games.pollFirst();
        if (game == null) {
          break;
        }
        if (!seen.add(game)) {
          continue;
        }
        columnDiffs.add(findTableauDiffs(bias, game));
        moves.clear();
        findMovesNoHome(moves, game);
        for (Move move : moves) {
          games.offerLast(move.play(game));
        }
      }
    }
    long[] diffTotals = new long[18];
    long total = 0;
    long total2 = 0;
    for (int[] columnDiff : columnDiffs) {
      for (int diff : columnDiff) {
        total += diff;
        total2 += (long) diff *diff;
        diffTotals[diff+bias]++;
      }
    }

    System.out.println(columnDiffs.size());
    System.out.println(Arrays.toString(diffTotals));
    System.out.println(total);
    System.out.println(total2);
  }

  static int[] findTableauDiffs(int start, FreeCell game) {
    int[] sizes = new int[TABLEAU_COLS];
    for (int i = 0; i < TABLEAU_COLS; i++) {
      sizes[i] = Math.toIntExact(game.tableauColStream(i).count());
    }
    Arrays.sort(sizes);
    int[] diffs = new int[TABLEAU_COLS];
    for (int i = 0; i < TABLEAU_COLS; i++) {
      int less = (i == 0) ? start : sizes[i - 1];
      diffs[i] = sizes[i] - less;
    }
    return diffs;
  }



  private static void findMovesNoHome(List<? super Move> moves, FreeCell game) {
    for (int srcTableauCol = 0; srcTableauCol < TABLEAU_COLS; srcTableauCol++) {
      Card srcCard = game.peekTableau(srcTableauCol);
      if (srcCard == null) {
        continue;
      }
      if (game.canMoveToFreeCellFromTableau(srcTableauCol)) {
        moves.add(new Move.MoveToFreeCellFromTableau(srcTableauCol));
      }
      int stack = game.stackSize(srcTableauCol);
      for (int dstTableauCol = 0; dstTableauCol < TABLEAU_COLS; dstTableauCol++) {
        for (int i = 1; i <= stack; i++) {
          if (game.canMoveToTableauFromTableau(dstTableauCol, srcTableauCol, i)) {
            moves.add(new Move.MoveToTableauFromTableau(dstTableauCol, srcTableauCol, i));
          }
        }
      }
    }
    for (int freeCol = 0; freeCol < FREE_CELLS; freeCol++) {
      Card srcCard = game.peekFreeCell(freeCol);
      if (srcCard == null) {
        continue;
      }
      for (int dstTableauCol = 0; dstTableauCol < TABLEAU_COLS; dstTableauCol++) {
        if (game.canMoveToTableauFromFreeCell(dstTableauCol, freeCol)) {
          moves.add(new Move.MoveToTableauFromFreeCell(dstTableauCol, freeCol));
        }
      }
    }
  }
}
