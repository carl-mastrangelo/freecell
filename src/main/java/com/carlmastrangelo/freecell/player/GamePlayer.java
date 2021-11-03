package com.carlmastrangelo.freecell.player;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.ForkFreeCell;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.MutableFreeCell;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;

public final class GamePlayer {

  public static void main(String [] args) {
    RandomGenerator rng = new SplittableRandom(2000);
    GamePlayer gp = new GamePlayer(ForkFreeCell.dealDeck(rng));
    gp.reportProgress(Executors.newSingleThreadScheduledExecutor(), Duration.ofSeconds(5));
    // TODO: cancel this on complete
    gp.playOnce();
  }

  private final ForkJoinPool pool = ForkJoinPool.commonPool();
  private final FreeCell startGame;

  private final LongAdder gamesPlayed = new LongAdder();
  private final LongAdder gamesSeen = new LongAdder();

  private final Set<FreeCell> visitedGames = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Queue<GameProgress> nextGames = new PriorityBlockingQueue<>(1000, Comparator.reverseOrder());
  private final AtomicReference<GameProgress> bestGameMoves = new AtomicReference<>();

  GamePlayer(FreeCell startGame) {
    this.startGame = Objects.requireNonNull(startGame);
  }

  Future<?> reportProgress(ScheduledExecutorService scheduler, Duration frequency) {
    final class ProgressReporter implements Runnable {
      private long lastRun = System.nanoTime();

      private long lastGamesPlayed;
      private long lastGamesSeen;

      @Override
      public synchronized void run() {
        long now = System.nanoTime();
        long played = gamesPlayed.sum();
        long seen = gamesSeen.sum();

        long playedDiff = played - lastGamesPlayed;
        lastGamesPlayed = played;
        long seenDiff = seen - lastGamesSeen;
        lastGamesSeen = seen;
        double nanosDiff = now - lastRun;
        lastRun = now;

        long playedPerSecond = (long)(playedDiff / nanosDiff * SECONDS.toNanos(1));
        long seenPerSecond = (long)(seenDiff / nanosDiff * SECONDS.toNanos(1));
        System.out.println(
            "Seen " + seen + " (" + seenPerSecond + "/s) Played " + played + " (" + playedPerSecond + "/s)"
                + " Pending " + nextGames.size() + " Cached: " + visitedGames.size());
        GameProgress next = nextGames.peek();
        System.out.println("Next to try: " + next + "\n");
        next = bestGameMoves.get();
        if (next != null) {
          System.out.println("Best: " + next.moves().prevMoves());
        }
      }
    }
    return scheduler.scheduleAtFixedRate(new ProgressReporter(), frequency.toNanos(), frequency.toNanos(), NANOSECONDS);
  }

  private void playOnce() {
    assert !startGame.gameWon();
    visitedGames.add(startGame);
    gamesSeen.add(1);
    for (Move move : moves(startGame)) {
      FreeCell game = move.play(startGame);
      gamesPlayed.add(1);
      if (!visitedGames.add(game)) {
        continue;
      }
      gamesSeen.add(1);
      double score = score(game);
      nextGames.add(new GameProgress(game, score, new MoveList(null, 0, move)));
    }

    try {
      pool.submit(new PlayMovesTask(new AtomicBoolean())).get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
  }

  private final class PlayMovesTask implements Runnable {

    private final AtomicBoolean done;

    private int currentBestMoves = Integer.MAX_VALUE;

    PlayMovesTask(AtomicBoolean done) {
      this.done = Objects.requireNonNull(done);
    }

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted() && !done.get()) {
        var prevGame = nextGames.poll();
        if (prevGame == null) {
          Thread.yield();
          continue;
        }
        if (currentBestMoves <= prevGame.moves().prevMoves() + 1 + ((ForkFreeCell)prevGame.game()).minMovesToWin()) {
          continue;
        }

        List<Move> moves = moves(prevGame.game());
        for (Move move : moves) {
          FreeCell game = move.play(prevGame.game());
          gamesPlayed.add(1);
          if (!visitedGames.add(game)) {
            continue;
          }
          gamesSeen.add(1);
          double score = score(game);
          GameProgress nextGame = new GameProgress(game, score, prevGame.moves().branch(move));
          if (game.gameWon()) {
            updateBestGame(nextGame);
          } else {
            nextGames.add(nextGame);
          }
        }
      }
    }

    private void updateBestGame(GameProgress nextGame) {
      boolean first;
      GameProgress currentBest;
      do {
        first = false;
        currentBest = bestGameMoves.get();
        if (currentBest != null) {
          currentBestMoves = Math.min(currentBestMoves, currentBest.moves().prevMoves());
          if (currentBest.moves().prevMoves() < nextGame.moves().prevMoves()) {
            break;
          }
        } else {
          first = true;
        }
      } while(!bestGameMoves.compareAndSet(currentBest, nextGame));
      if (first) {
        System.out.println("First Success!");
        visitedGames.clear();
      }
    }
  }


  private void play() {
    RandomGenerator rng = new SplittableRandom(2000);
    /*
    MutableFreeCell game = new MutableFreeCell();


    game.deal(rng);

     */
    FreeCell game = ForkFreeCell.dealDeck(rng);
    /*
    game.deal(parse(
            "JD", "KD", "7D", "4C", "KH", "QH", "4S", "AC",
            "9S", "3D", "5H", "9D", "QC", "JH", "8C", "9C",
            "AS", "2S", null, "5C", null, "4H", "QS", "8D",
            "2C", null, null, "3H", null, "6S", "XH", "4D",
            "KC", null, null, "6D", null, "7C", "XC", "3S",
            "5S", null, null, "6H", null, "8H", "3C", null,
            "XS", null, null, "KS", null, "7S", null, null,
            "9H", null, null, "QD", null, null, null, null,
            "8S", null, null, "JC", null, null, null, null,
            "7H", null, null, "XD", null, null, null, null,
            "6C", null, null, null, null, null, null, null,
            "5D", null, null, null, null, null, null, null),
        parse("JS"),
        parse("2D", "2H"));*/
    System.out.println(game);

    Set<FreeCell> seen = Collections.newSetFromMap(new HashMap<>());
    seen.add(game);

/*
    double top = 0;
    while (!gameMoves.isEmpty()) {
      GameMoves gm = gameMoves.poll();
      lastGame.set(gm.game());
      List<Move> moves = gm.moves();
      for (Move move : moves) {
        FreeCell newGame;// = gm.game().copy();
        newGame = move.play(gm.game());
        gamePlayed.incrementAndGet();
        boolean better = false;
        double score = score(newGame);
        if (score> top) {
          top = score;
          better = true;
          System.out.println("better " + top);
          System.out.println(newGame);
        }

        if (newGame.gameWon()) {
          Deque<MoveNode> moveNodes = new ArrayDeque<>();
          MoveNode it = gm.move();
          while (it != null) {
            moveNodes.addFirst(it);
            it = it.prev;
          }
          StringBuilder sb = new StringBuilder();

          for (MoveNode mn : moveNodes) {
            sb.append((mn.game()));
            sb.append("\n");
            mn.move().describe(sb, mn.game());
            sb.append("\n\n");
          }
          sb.append("\n");
          move.describe(sb, gm.game());
          throw new RuntimeException("Success!" + sb);
        }
        if (seen.add(newGame)) {
          gamesSeen.incrementAndGet();

          GameMoves newGm = new GameMoves(new MoveNode(gm.move(), move, gm.game()), newGame, moves(newGame), score);
          gameMoves.add(newGm);
        }
      }
    }*/
    throw new RuntimeException("no moves left");
  }

  private static List<Move> moves(FreeCell game) {
    List<Move> moves = new ArrayList<>();
    for (int srcTableauCol = 0; srcTableauCol < MutableFreeCell.TABLEAU_COLUMNS; srcTableauCol++) {
      Card srcCard = game.peekTableau(srcTableauCol);
      if (srcCard == null) {
        continue;
      }
      if (game.canMoveToHomeCellFromTableau(srcTableauCol)) {
        moves.add(new Move.MoveToHomeCellFromTableau(srcTableauCol));
        switch (srcCard.suit().color()) {
          case BLACK -> {
            Card topDiamond = game.topHomeCell(Card.Suit.DIAMONDS);
            Card topHeart = game.topHomeCell(Card.Suit.HEARTS);
            if (topDiamond != null && topDiamond.rank().num() >= srcCard.rank().num()
               && topHeart != null && topHeart.rank().num() >= srcCard.rank().num()) {
              return List.of(moves.get(moves.size() - 1));
            }
          }
          case RED -> {
            Card topClub = game.topHomeCell(Card.Suit.CLUBS);
            Card topSpade = game.topHomeCell(Card.Suit.SPADES);
            if (topClub != null && topClub.rank().num() >= srcCard.rank().num()
                && topSpade != null && topSpade.rank().num() >= srcCard.rank().num()) {
              return List.of(moves.get(moves.size() - 1));
            }
          }
        }
        if (srcCard.rank() == Card.Rank.ACE || srcCard.rank() == Card.Rank.TWO)  {
          return List.of(moves.get(moves.size() - 1));
        }
      }
      if (game.canMoveToFreeCellFromTableau(srcTableauCol)) {
        moves.add(new Move.MoveToFreeCellFromTableau(srcTableauCol));
      }
      for (int dstTableauCol = 0; dstTableauCol < MutableFreeCell.TABLEAU_COLUMNS; dstTableauCol++) {
        if (game.canMoveToTableauFromTableau(dstTableauCol, srcTableauCol)) {
          moves.add(new Move.MoveToTableauFromTableau(dstTableauCol, srcTableauCol));
        }
      }
    }
    for (int freeCol = 0; freeCol < MutableFreeCell.FREE_CELL_COLUMNS; freeCol++) {
      if (game.peekFreeCell(freeCol) == null) {
        continue;
      }
      if (game.canMoveToHomeCellFromFreeCell(freeCol)) {
        Card srcCard = game.peekFreeCell(freeCol);
        moves.add(new Move.MoveToHomeCellFromFreeCell(freeCol));
        switch (srcCard.suit().color()) {
          case BLACK -> {
            Card topDiamond = game.topHomeCell(Card.Suit.DIAMONDS);
            Card topHeart = game.topHomeCell(Card.Suit.HEARTS);
            if (topDiamond != null && topDiamond.rank().num() >= srcCard.rank().num()
                && topHeart != null && topHeart.rank().num() >= srcCard.rank().num()) {
              return List.of(moves.get(moves.size() - 1));
            }
          }
          case RED -> {
            Card topClub = game.topHomeCell(Card.Suit.CLUBS);
            Card topSpade = game.topHomeCell(Card.Suit.SPADES);
            if (topClub != null && topClub.rank().num() >= srcCard.rank().num()
                && topSpade != null && topSpade.rank().num() >= srcCard.rank().num()) {
              return List.of(moves.get(moves.size() - 1));
            }
          }
        }
        if (srcCard.rank() == Card.Rank.ACE || srcCard.rank() == Card.Rank.TWO) {
          return List.of(moves.get(moves.size() - 1));
        }
      }
      for (int dstTableauCol = 0; dstTableauCol < MutableFreeCell.TABLEAU_COLUMNS; dstTableauCol++) {
        if (game.canMoveToTableauFromFreeCell(dstTableauCol, freeCol)) {
          moves.add(new Move.MoveToTableauFromFreeCell(dstTableauCol, freeCol));
        }
      }
    }
    return moves;
  }

  double score(FreeCell game)  {
    double sum = 0;
    int[] parts = new int[4];
    for (Card.Suit suit : Card.Suit.values()) {
      Card card = game.topHomeCell(suit);
      parts[suit.ordinal()] = card != null ? card.rank().num() : 0;
      sum +=parts[suit.ordinal()];
    }
    double var = 0;
    for (int i =0; i < 4; i++) {
      var += Math.pow(parts[i] - sum/4, 2);
    }


    return sum - var;
  }

  private static List<Card> parse(String ... symbols) {
    List<Card> cards = new ArrayList<>(symbols.length);
    for (String symbol : symbols) {
      if (symbol == null) {
        cards.add(null);
        continue;
      }
      cards.add(Card.parse(symbol));
    }
    return Collections.unmodifiableList(cards);
  }

  private record GameProgress(FreeCell game, double score, MoveList moves) implements Comparable<GameProgress> {
    GameProgress {
      Objects.requireNonNull(game);
      if (!Double.isFinite(score)) {
        throw new IllegalArgumentException();
      }
      Objects.requireNonNull(moves);
    }

    @Override
    public int compareTo(GameProgress that) {
      return Double.compare(this.score, that.score);
    }

    @Override
    public String toString() {
      return "GameProgress{\n" +
          "game=" + game +
          "\n\nscore=" + score +
          ", moves=" + moves.prevMoves() +
          '}';
    }
  }

  private record MoveList(@Nullable MoveList prev, int prevMoves, Move nextMove) {

    MoveList {
      if (prev == null && prevMoves != 0) {
        throw new IllegalArgumentException();
      } else if (prev != null && prev.prevMoves != prevMoves - 1) {
        throw new IllegalArgumentException();
      }
      Objects.requireNonNull(nextMove);
    }

    MoveList branch(Move move) {
      return new MoveList(this, prevMoves + 1, move);
    }

    @Override
    public String toString() {
      return "MoveList(" + prevMoves + ')';
    }
  }
}
