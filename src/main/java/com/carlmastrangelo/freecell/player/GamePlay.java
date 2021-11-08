package com.carlmastrangelo.freecell.player;

import static com.carlmastrangelo.freecell.FreeCell.FREE_CELLS;
import static com.carlmastrangelo.freecell.FreeCell.TABLEAU_COLS;
import static com.carlmastrangelo.freecell.Rank.RANK_COUNT;
import static com.carlmastrangelo.freecell.Suit.SUITS_BY_ORD;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.Rank;
import com.carlmastrangelo.freecell.Suit;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;

final class GamePlay {

  private final long maxMoves;
  private final ToDoubleBiFunction<? super FreeCell, ? super GamePlayer.MoveList> scorer;
  private final Reference<Map<FreeCell, Integer>> visitedGames = new SoftReference<>(new HashMap<>());
  private final Queue<GamePlayer.GameProgress> nextGames;
  private final int bestMoveCount;
  @Nullable
  private final RandomGenerator moveShuffler;
  private final ProgressReporter reporter;

  private final List<Move> movesCache = new ArrayList<>();

  GamePlay(
      GamePlayer.GameProgress initialGameProgress, long maxMoves, Comparator<GamePlayer.GameProgress> comparator,
      ToDoubleBiFunction<? super FreeCell, ? super GamePlayer.MoveList> scorer, int bestMoveCount,
      @Nullable RandomGenerator moveShuffler, @Nullable ProgressReporter reporter) {
    this.nextGames = new PriorityQueue<>(1000, comparator.reversed());
    this.nextGames.add(Objects.requireNonNull(initialGameProgress));
    this.maxMoves = maxMoves;
    this.scorer = Objects.requireNonNull(scorer);
    this.bestMoveCount = bestMoveCount;
    this.moveShuffler = moveShuffler;
    this.reporter = reporter != null ? reporter : new ProgressReporter() {
      @Override
      public void movePlayed() {}

      @Override
      public void gameSeen() {}
    };
  }

  interface ProgressReporter {
    void movePlayed();
    void gameSeen();
  }

  @Nullable
  GameResult play() {
    try {
      return playInternal();
    } finally {
      //var set = visitedGames.get();
      visitedGames.clear();
      //if (set != null) {
      //  set.clear();
      //}
      //nextGames.clear();
      //movesCache.clear();
    }
  }

  record GameResult(Status status, GamePlayer.GameProgress gameProgress) {}

  enum Status {
    SUCCESS,
    UNWINNABLE,
    INTERRUPTED,
    MAX_PLAYS,
  }

  private GameResult playInternal() {
    GamePlayer.GameProgress preProgress;
    int movesPlayed = 0;
    while ((preProgress = nextGames.poll()) != null) {
      movesCache.clear();
      findMoves(movesCache, preProgress.game());
      if (moveShuffler != null) {
        shuffle(movesCache, moveShuffler);
      }

      for (Move move : movesCache) {
        if (movesPlayed++ == maxMoves) {
          return new GameResult(Status.MAX_PLAYS, null);
        }
        if ((movesPlayed & 0xFFFFF) == 0xFFFFF && Thread.currentThread().isInterrupted()) {
          return new GameResult(Status.INTERRUPTED, null);
        }

        FreeCell postGame = move.play(preProgress.game());
        reporter.movePlayed();
        GamePlayer.MoveList preMoves = preProgress.moves();
        if (!visitGame(postGame, preMoves.totalMoves() + 1)) {
          continue;
        }
        reporter.gameSeen();
        var postMoves = preMoves.branch(move);
        double score = scorer.applyAsDouble(postGame, postMoves);

        GamePlayer.GameProgress nextGameProgress =
            new GamePlayer.GameProgress(postGame, score, postMoves);
        if (postGame.gameWon()) {
          return new GameResult(Status.SUCCESS, nextGameProgress);
        } else if (couldBeatBestMoves(nextGameProgress)) {
          nextGames.add(nextGameProgress);
        }
      }
    }
    return new GameResult(Status.UNWINNABLE, null);
  }



  private boolean visitGame(FreeCell game, int depth) {
    var map = visitedGames.get();
    if (map == null) {
      return true;
    }
    Integer depthBox  = depth;
    Integer old = map.putIfAbsent(game, depthBox);
    if (old != null && old <= depth) {
      return false;
    }
    map.put(game, depthBox);
    return true;
  }

  private boolean couldBeatBestMoves(GamePlayer.GameProgress gameProgress) {
    int minMovesToWin = 0;
    FreeCell game = gameProgress.game();
    for (Suit suit : SUITS_BY_ORD) {
      Card card = game.topHomeCell(suit);
      if (card == null) {
        minMovesToWin += RANK_COUNT;
      } else {
        minMovesToWin += Rank.KING_ORD - card.rank().ordinal();
      }
    }
    if (gameProgress.moves() == null) {
      return true;
    }
    return gameProgress.moves().totalMoves() + minMovesToWin <= bestMoveCount;
  }

  private static void findMoves(List<? super Move> moves, FreeCell game) {
    for (int srcTableauCol = 0; srcTableauCol < TABLEAU_COLS; srcTableauCol++) {
      Card srcCard = game.peekTableau(srcTableauCol);
      if (srcCard == null) {
        continue;
      }
      if (game.canMoveToHomeCellFromTableau(srcTableauCol)) {
        Move moveHome = new Move.MoveToHomeCellFromTableau(srcTableauCol);
        if (shouldMoveHome(srcCard, game)) {
          moves.clear();
          moves.add(moveHome);
          return;
        }
      }
      if (game.canMoveToFreeCellFromTableau(srcTableauCol)) {
        moves.add(new Move.MoveToFreeCellFromTableau(srcTableauCol));
      }
      for (int dstTableauCol = 0; dstTableauCol < TABLEAU_COLS; dstTableauCol++) {
        if (game.canMoveToTableauFromTableau(dstTableauCol, srcTableauCol)) {
          moves.add(new Move.MoveToTableauFromTableau(dstTableauCol, srcTableauCol));
        }
      }
    }
    for (int freeCol = 0; freeCol < FREE_CELLS; freeCol++) {
      Card srcCard = game.peekFreeCell(freeCol);
      if (srcCard == null) {
        continue;
      }
      if (game.canMoveToHomeCellFromFreeCell(freeCol)) {
        Move moveHome = new Move.MoveToHomeCellFromFreeCell(freeCol);
        if (shouldMoveHome(srcCard, game)) {
          moves.clear();
          moves.add(moveHome);
          return;
        }
      }
      for (int dstTableauCol = 0; dstTableauCol < TABLEAU_COLS; dstTableauCol++) {
        if (game.canMoveToTableauFromFreeCell(dstTableauCol, freeCol)) {
          moves.add(new Move.MoveToTableauFromFreeCell(dstTableauCol, freeCol));
        }
      }
    }
  }

  private static boolean shouldMoveHome(Card card, FreeCell game) {
    if (card.rank() == Rank.ACE || card.rank() == Rank.TWO) {
      return true;
    }
    return switch (card.suit().color()) {
      case BLACK -> {
        Card topDiamond = game.topHomeCell(Suit.DIAMONDS);
        Card topHeart = game.topHomeCell(Suit.HEARTS);
        yield topDiamond != null && topDiamond.rank().num() >= card.rank().num() - 1
            && topHeart != null && topHeart.rank().num() >= card.rank().num() - 1;
      }
      case RED -> {
        Card topClub = game.topHomeCell(Suit.CLUBS);
        Card topSpade = game.topHomeCell(Suit.SPADES);
        yield topClub != null && topClub.rank().num() >= card.rank().num() - 1
            && topSpade != null && topSpade.rank().num() >= card.rank().num() - 1;
      }
    };
  }

  private static <T> void shuffle(List<T> elems, RandomGenerator rng) {
    for (int i = elems.size(); i > 1; i--) {
      T e1 = elems.get(i - 1);
      int pos = rng.nextInt(i);
      T e2 = elems.get(pos);

      elems.set(i - 1, e2);
      elems.set(pos, e1);
    }
  }
}
