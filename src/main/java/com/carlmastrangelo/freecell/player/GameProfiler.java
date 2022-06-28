package com.carlmastrangelo.freecell.player;

import static com.carlmastrangelo.freecell.FreeCell.FREE_CELLS;
import static com.carlmastrangelo.freecell.FreeCell.TABLEAU_COLS;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.ForkFreeCell;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.coder.ArithmeticCoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;

public final class GameProfiler {

  public static void main(String [] args) {
    var rngf = RandomGeneratorFactory.<RandomGenerator.SplittableGenerator>of("L64X256MixRandom");
    List<int[]> columnDiffs = new ArrayList<>();
    int gameCount = 10000;
    int moveCount = 1000;
    int bias = 5;
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


    SortedMap<Integer, Double> probabilities = new TreeMap<>();
    double den = 5366163.0;
    probabilities.put(-5, 1/den);
    probabilities.put(-4, 1/den);
    probabilities.put(-3, 26/den);
    probabilities.put(-2, 679/den);
    probabilities.put(-1, 48556/den);
    probabilities.put(0, 3866788/den);
    probabilities.put(1, 1395384/den);
    probabilities.put(2, 53784/den);
    probabilities.put(3, 902/den);
    probabilities.put(4, 33/den);
    probabilities.put(5, 1/den);
    probabilities.put(6, 1/den);
    probabilities.put(7, 1/den);
    probabilities.put(8, 1/den);
    probabilities.put(9, 1/den);
    probabilities.put(10, 1/den);
    probabilities.put(11, 1/den);
    probabilities.put(12, 1/den);
    probabilities.put(13, 1/den);
    probabilities.put(14, 1/den);

    Map<Integer, AtomicLong> counts = new ConcurrentSkipListMap<>();
    columnDiffs.parallelStream().forEach(cd -> {
      cd[0] = 0;
      var enc =
          ArithmeticCoder.arithmeticEncode(
              Arrays.stream(cd).boxed().collect(Collectors.toList()), probabilities);
      if (enc.bitsUsed() > 45) {
        System.out.println("woa");
      }
      counts.computeIfAbsent(enc.bitsUsed(), k -> new AtomicLong()).incrementAndGet();
    });


    System.out.println(counts);
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
