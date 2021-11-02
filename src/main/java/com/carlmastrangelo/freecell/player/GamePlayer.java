package com.carlmastrangelo.freecell.player;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.FreeCell;
import com.carlmastrangelo.freecell.MutableFreeCell;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

public final class GamePlayer {

  public static void main(String [] args) {
    new GamePlayer().play();
  }

  private final AtomicLong gamesSeen = new AtomicLong(1);
  private final AtomicReference<FreeCell> lastGame = new AtomicReference<>();

  private void play() {
    ForkJoinPool.commonPool().submit(new Runnable() {
      long lastRun = System.nanoTime();
      long lastCount = 0;

      @Override
      public void run() {
        while (true) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          long now = System.nanoTime();
          long currentCount = gamesSeen.get();

          long gameDiff = currentCount - lastCount;
          lastCount = currentCount;
          long timeDiff = now - lastRun;
          lastRun = now;
          System.out.println((1_000_000_000.0 * gameDiff / timeDiff) + " games per second");
          System.out.println(lastGame.get());
        }
      }
    });


    MutableFreeCell game = new MutableFreeCell();

    RandomGenerator rng = new SplittableRandom(10);
    game.deal(rng);
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

    record GameMoves(FreeCell game, List<Move> moves){}
    Deque<GameMoves> gameMoves = new ArrayDeque<>();
    gameMoves.addLast(new GameMoves(game, moves(game)));

    int top = 0;
    while (!gameMoves.isEmpty()) {
      GameMoves gm = gameMoves.pollFirst();
      lastGame.set(gm.game());
      List<Move> moves = gm.moves();
      for (Move move : moves) {
        FreeCell newGame;// = gm.game().copy();
        newGame = move.play(gm.game());
        boolean better = false;
        /*
        if (newGame.score() > top) {
          top = newGame.score();
          better = true;
          System.out.println(newGame);
        }
        */

        if (newGame.gameWon()) {
          throw new RuntimeException("Success!");
        }
        if (seen.add(newGame)) {
          gamesSeen.incrementAndGet();

          GameMoves newGm = new GameMoves(newGame, moves(newGame));
          if (better) {
            gameMoves.addFirst(newGm);
          } else {
            gameMoves.addLast(newGm);
          }
        }
      }
    }
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
      for (int dstTableauCol = srcTableauCol + 1; dstTableauCol < MutableFreeCell.TABLEAU_COLUMNS; dstTableauCol++) {
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
}
