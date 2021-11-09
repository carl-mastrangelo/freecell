package com.carlmastrangelo.freecell.player;

import static com.carlmastrangelo.freecell.FreeCell.FREE_CELLS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.ForkFreeCell;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.Suit;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import javax.annotation.Nullable;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;

public final class GamePlayer {

  public static void main(String [] args) throws Exception {
    var rngf = RandomGeneratorFactory.<RandomGenerator.SplittableGenerator>of("L64X256MixRandom");

    ForkFreeCell game = ForkFreeCell.dealDeck(parse("AS", "AH"), parse(), parse(
        "6C", "7D", "QC", "AD", "9D", "5D", "9H", "9C",
        "3S", "XS", "AC", "8H", "6H", "8C", "7C", "QD",
        "4S", "XC", "8S", "5H", "7H", "3D", "JC", "KS",
        "4D", "JS", "QS", "QH", "4H", "3C", "2C", "XH",
        "KH", "JD", "2S", "5S", "JH", "9S", null, "4C",
        "8D", "6D", "2H", "5C", "KC", "2D", null, "7S",
        "3H", "KD", "XD", "6S", null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null));


    GamePlayer gp = new GamePlayer(game, Executors.newSingleThreadScheduledExecutor());

    gp.start();
  }

  private static String describeGame(FreeCell game, GameProgress gameState) {
    var sb = new StringBuilder();

    sb.append(game);
    var deque = new ArrayDeque<Move>();
    MoveList list = gameState.moves();
    while (list != null  && list.totalMoves != 0) {
      deque.addFirst(list.move());
      list = list.lastMove();
    }
    for (Move move : deque) {
      sb.append("\n");
      move.describe(sb, game);
      sb.append("\n\n");
      game = move.play(game);
      sb.append(game);
    }
    sb.append("\n").append(gameState.moves.totalMoves).append(" moves to win\n");
    return sb.toString();
  }

  private final ScheduledExecutorService scheduler;
  private final ForkJoinPool pool = ForkJoinPool.commonPool();
  private final BlockingQueue<GamePlayArgs> nextGames = new LinkedBlockingQueue<>();

  private final ProgressReporter progressReporter = new ProgressReporter();
  private final FreeCell startGame;

  private final RandomGeneratorFactory<RandomGenerator.SplittableGenerator> randomFactory =
      RandomGeneratorFactory.of("L64X256MixRandom");

  GamePlayer(FreeCell startGame, ScheduledExecutorService scheduler) {
    this.startGame = Objects.requireNonNull(startGame);
    this.scheduler = Objects.requireNonNull(scheduler);
  }

  void start() throws Exception {
    var frequency = Duration.ofSeconds(5);
    scheduler.scheduleAtFixedRate(progressReporter, frequency.toNanos(), frequency.toNanos(), NANOSECONDS);

    var rng = randomFactory.create(6);
    int bestMovesCount = Integer.MAX_VALUE;
    int maxMoves = 5_000_000;
    BlockingQueue<PlayTask> tasks = new LinkedBlockingQueue<>();
    for (int i = 0; i < Runtime.getRuntime().availableProcessors() * 10; i++) {
      var gameGameArgs = new GamePlayArgs(
          new GameProgress(startGame, Double.MIN_VALUE, new MoveList(null, 0, null)),
          maxMoves,
          bestMovesCount,
          rng.split());
      PlayTask res = (PlayTask) pool.submit(new PlayTask(gameGameArgs, 1));
      tasks.add(res);
    }

    while (true) {
      var task = tasks.take();
      GamePlay.GameResult gameResult = task.get();
      switch (gameResult.status()) {
        case SUCCESS -> {
          var bestProgress = gameResult.gameProgress();
          if (bestProgress.moves().totalMoves() <= bestMovesCount) {
            bestMovesCount = bestProgress.moves().totalMoves();
            maxMoves /= 2;
          }

          System.out.println(describeGame(startGame, bestProgress));
        }
        case UNWINNABLE -> {
          maxMoves = task.args.maxPlays() * 5 / 4;
        }
        case INTERRUPTED -> throw new InterruptedException();
        case MAX_PLAYS -> {
          maxMoves = task.args.maxPlays() * 5 / 4;
        }
      };
      var gameGameArgs = new GamePlayArgs(task.args.gameProgress(), maxMoves, bestMovesCount, rng.split());
      PlayTask res = (PlayTask) pool.submit(new PlayTask(gameGameArgs, 1));
      tasks.add(res);
    }
  }

  private final class ProgressReporter implements Runnable, GamePlay.ProgressReporter {

    private final LongAdder gamesPlayed = new LongAdder();
    private final LongAdder gamesSeen = new LongAdder();

    private final LongAdder roundsStarted = new LongAdder();
    private final LongAdder roundsFinished = new LongAdder();

    private long lastRun = System.nanoTime();

    private long lastMovesPlayed;
    private long lastGamesSeen;

    private final Histogram moveHistogram = new ConcurrentHistogram(4000, 3);

    @Override
    public void movePlayed() {
      gamesPlayed.increment();
    }

    @Override
    public void gameSeen() {
      gamesSeen.increment();
    }

    @Override
    public void run() {
      long now = System.nanoTime();
      long played = gamesPlayed.sum();
      long seen = gamesSeen.sum();
      long started = roundsStarted.sum();
      long ended = roundsFinished.sum();

      long playedDiff = played - lastMovesPlayed;
      lastMovesPlayed = played;
      long seenDiff = seen - lastGamesSeen;
      lastGamesSeen = seen;
      double nanosDiff = now - lastRun;
      lastRun = now;

      long playedPerSecond = (long)(playedDiff / nanosDiff * SECONDS.toNanos(1));
      long seenPerSecond = (long)(seenDiff / nanosDiff * SECONDS.toNanos(1));

      System.out.println(
          "Seen " + seen + " (" + seenPerSecond + "/s) Played " + played + " (" + playedPerSecond + "/s)");
      System.out.println(
          "Started " + started + " ended " + ended + " (" + (started - ended) + ")");
      //System.out.println(moveHistogram);
    }
  }

  private record GamePlayArgs(
      GameProgress gameProgress, int maxPlays, int bestMovesCount, RandomGenerator.SplittableGenerator rng) {
    GamePlayArgs {
      if (maxPlays <= 0) {
        throw new IllegalArgumentException();
      }
      if (bestMovesCount <= 0) {
        throw new IllegalArgumentException();
      }
    }

    GamePlayArgs split() {
      return new GamePlayArgs(gameProgress, maxPlays, bestMovesCount, rng.split());
    }
  }

  private final class PlayTask extends RecursiveTask<GamePlay.GameResult> {

    private final GamePlayArgs args;
    private final int depth;

    PlayTask(GamePlayArgs args, int depth) {
      this.args = Objects.requireNonNull(args);
      this.depth = depth;
    }

    @Override
    protected GamePlay.GameResult compute() {
      try {
        progressReporter.roundsStarted.add(1);
        return computeInternal();
      } finally{
        progressReporter.roundsFinished.add(1);
      }
    }

    private GamePlay.GameResult computeInternal() {

      GameProgress start = args.gameProgress();
      GamePlay gamePlay = new GamePlay(
          start,
          args.maxPlays(),
          GameComparator.INSTANCE,
          GamePlayer::score,
          args.bestMovesCount(),
          args.rng(),
          progressReporter,
          progressReporter.moveHistogram);
      GamePlay.GameResult result = gamePlay.play();
      return switch (result.status()) {
        case SUCCESS -> {
          var gameProgress = result.gameProgress();
          if (gameProgress.moves().totalMoves() >= args.bestMovesCount()) {
            yield result;
          }
          List<GameProgress> samples = sampleGames(args.gameProgress(), gameProgress);
          List<PlayTask> forks = new ArrayList<>(samples.size());
          for (GameProgress sample : samples) {
            var task =
                new PlayTask(
                    new GamePlayArgs(
                        sample,
                        args.maxPlays() / (1 + sample.moves().totalMoves()),
                        gameProgress.moves().totalMoves() - 1,
                        args.rng().split()),
                    depth + 1);
            task.fork();
            forks.add(task);
          }
          GamePlay.GameResult best = result;
          for (PlayTask task : forks) {
            var gameProgressChild = task.join();
            if (gameProgressChild.status() == GamePlay.Status.SUCCESS) {
              if (gameProgressChild.gameProgress().moves().totalMoves() < best.gameProgress().moves().totalMoves()) {
                best = gameProgressChild;
              }
            }
          }
          yield best;
        }
        case INTERRUPTED -> result;
        case MAX_PLAYS -> result;
        case UNWINNABLE -> result;
      };
    }
  }

  private static List<GameProgress> sampleGames(GameProgress initialGame, GameProgress destinationGame) {
    Deque<MoveList> moveLists = new ArrayDeque<>();

    for (MoveList fullMovesList = destinationGame.moves();
         fullMovesList.totalMoves() != initialGame.moves().totalMoves(); fullMovesList = fullMovesList.lastMove()) {
      moveLists.addFirst(fullMovesList);
    }
    int i = 0;
    List<GameProgress> samples = new ArrayList<>();
    GameProgress gameProgress = initialGame;
    for (MoveList moveList : moveLists) {
      Move move = moveList.move();
      FreeCell game = move.play(gameProgress.game());
      gameProgress = new GameProgress(game, Double.MIN_VALUE, moveList);
      i++;
      if ((i & (i-1)) == 0) {
        samples.add(gameProgress);
      }
    }
    return samples;
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
      if (Math.abs(thisMoves - thatMoves) < 2) {
        return 0;
      }
      return Integer.compare(thatMoves, thisMoves);
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


    throw new RuntimeException("no moves left");
  }
  
  private static double score(FreeCell game, MoveList moves)  {
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
    Deque<Card> column = new ArrayDeque<>(24);
    int diff = 0;
    for (int i = 0; i < FREE_CELLS; i++) {
      if (game.peekFreeCell(i) == null) {
        sum += 0.05;
      }
    }
    for (int col = 0; col < FreeCell.TABLEAU_COLS; col++) {
      game.readTableau(column, col);
      if (column.isEmpty()) {
        sum += 0.05;
      }
      while (!column.isEmpty()) {
        Card card = column.removeFirst();
        if (parts[card.suit().ordinal()] + 1 == card.rank().num()) {
          diff += Math.pow(column.size(), 2);
          column.clear();
        }
      }
    }


    return (sum - Math.sqrt(var) - Math.sqrt(diff) / 4) / moves.totalMoves();
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

  record GameProgress(FreeCell game, double score, MoveList moves) {
    GameProgress {
      Objects.requireNonNull(game);
      Objects.requireNonNull(moves);
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

  record MoveList(@Nullable MoveList lastMove, int totalMoves, @Nullable Move move) {

    MoveList {
      if (lastMove == null || totalMoves == 0 || move == null) {
        if (lastMove != null || totalMoves != 0 || move != null) {
          throw new IllegalArgumentException();
        }
      } else {
        if (lastMove.totalMoves != totalMoves - 1) {
          throw new IllegalArgumentException();
        }
      }
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
