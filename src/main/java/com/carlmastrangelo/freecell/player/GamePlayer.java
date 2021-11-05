package com.carlmastrangelo.freecell.player;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.ForkFreeCell;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.Suit;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import javax.annotation.Nullable;

public final class GamePlayer {

  public static void main(String [] args) {
    var rngf = RandomGeneratorFactory.<RandomGenerator.SplittableGenerator>of("L64X256MixRandom");
    RandomGenerator.SplittableGenerator rng = rngf.create(1);
    GamePlayer gp = new GamePlayer(ForkFreeCell.dealDeck(rng));
    gp.reportProgress(Executors.newSingleThreadScheduledExecutor(), Duration.ofSeconds(5));
    var game = ForkFreeCell.dealDeck(rng);
    var res = new GamePlay(
        new GameProgress(game, 0, null),
        10_000_000,
        GameComparator.INSTANCE,
        GamePlayer::score,
        Integer.MAX_VALUE,
        rng,
        new GamePlay.ProgressReporter() {
          @Override
          public void movePlayed() {
            gp.gamesPlayed.increment();
          }

          @Override
          public void gameSeen() {
            gp.gamesSeen.increment();
          }
        }
    ).play();
    System.out.println(res);
  }

  private final ForkJoinPool pool = ForkJoinPool.commonPool();

  private final LongAdder gamesPlayed = new LongAdder();
  private final LongAdder gamesSeen = new LongAdder();

  private final AtomicReference<GameProgress> bestGameMoves = new AtomicReference<>();

  GamePlayer(FreeCell startGame) {
  Objects.requireNonNull(startGame);
  }

  ScheduledFuture<?> reportProgress(ScheduledExecutorService scheduler, Duration frequency) {
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
            "Seen " + seen + " (" + seenPerSecond + "/s) Played " + played + " (" + playedPerSecond + "/s)");
                //+ " Pending " + nextGames.size() + " Cached: " + visitedGames.size());

        //visitedGames.clear();
        //GameProgress next = nextGames.peek();
        //System.out.println("Next to try: " + next + "\n");
        GameProgress next = bestGameMoves.get();
        if (next != null) {
          System.out.println("Best: " + next.moves().totalMoves());
        }
      }
    }
    return scheduler.scheduleAtFixedRate(new ProgressReporter(), frequency.toNanos(), frequency.toNanos(), NANOSECONDS);
  }

  private static final class GameComparator implements Comparator<GameProgress> {
    static GameComparator INSTANCE = new GameComparator();

    @Override
    public int compare(GameProgress o1, GameProgress o2) {
      int cmp = Double.compare(o1.score, o2.score);
      if (cmp != 0) {
        return cmp;
      }
      var thisMoves = o1.moves().totalMoves();
      var thatMoves = o2.moves().totalMoves();
      if (Math.abs(thisMoves - thatMoves) < 20) {
        return 0;
      }
      return Integer.compare(thatMoves, thisMoves);
    }
  }
/*
  private final class PlayMovesTask implements Runnable {

    private void updateBestGame(GameProgress nextGame) {
      boolean first;
      GameProgress currentBest;
      do {
        first = false;
        currentBest = bestGameMoves.get();
        if (currentBest != null) {
          System.err.println("SUCCESS " + currentBest.moves().prevMoves());
          currentBestMoves = Math.min(currentBestMoves, currentBest.moves().prevMoves());
          if (currentBest.moves().prevMoves() < nextGame.moves().prevMoves()) {
            break;
          }
        } else {
          first = true;
        }
      } while(!bestGameMoves.compareAndSet(currentBest, nextGame));
      done.set(true);
      playOnce();
    }
  }*/


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


    throw new RuntimeException("no moves left");
  }





  private static double score(FreeCell game)  {
    double sum = 0;
    int[] parts = new int[4];
    for (Suit suit : Suit.values()) {
      Card card = game.topHomeCell(suit);
      parts[suit.ordinal()] = card != null ? card.rank().num() : 0;
      sum +=parts[suit.ordinal()];
    }
    double var = 0;
    for (int i =0; i < 4; i++) {
      var += Math.pow(parts[i] - sum/4, 2);
    }

    return sum - Math.sqrt(var);
  }

  private static List<Card> parse(String ... symbols) {
    List<Card> cards = new ArrayList<>(symbols.length);
    for (String symbol : symbols) {
      if (symbol == null) {
        cards.add(null);
        continue;
      }
      cards.add(Card.ofSymbol(symbol));
    }
    return Collections.unmodifiableList(cards);
  }

  record GameProgress(FreeCell game, double score, @Nullable MoveList moves) {
    GameProgress {
      Objects.requireNonNull(game);
      if (!Double.isFinite(score)) {
        throw new IllegalArgumentException();
      }
    }

    @Override
    public String toString() {
      return "GameProgress{\n" +
          "game=" + game +
          "\n\nscore=" + score +
          ", moves=" + (moves == null ? "null" : moves.totalMoves()) +
          '}';
    }
  }

  record MoveList(@Nullable MoveList lastMove, int totalMoves, Move move) {

    MoveList {
      if (lastMove == null && totalMoves != 1) {
        throw new IllegalArgumentException();
      } else if (lastMove != null && lastMove.totalMoves != totalMoves - 1) {
        throw new IllegalArgumentException();
      }
      Objects.requireNonNull(move);
    }

    MoveList branch(Move move) {
      return new MoveList(this, totalMoves + 1, move);
    }

    @Override
    public String toString() {
      return "MoveList(" + totalMoves + ')';
    }
  }
}
