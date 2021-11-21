package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.ForkFreeCell.EMPTY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ForkFreeCellTest {
  @Test
  public void dealDeck() {
    ForkFreeCell game = ForkFreeCell.dealDeck();
    assertFalse(game.gameWon());
  }

  @Test
  public void dealDeck_won() {
    ForkFreeCell game =
        ForkFreeCell.dealDeck(
            List.of(Card.KING_CLUBS, Card.KING_SPADES, Card.KING_DIAMONDS, Card.KING_HEARTS),
            List.of(),
            List.of());
    assertTrue(game.gameWon());
  }

  @Test
  public void dealDeck_homeContainsSameDuplicates() {
    IllegalArgumentException failure = Assert.assertThrows(IllegalArgumentException.class, () -> ForkFreeCell.dealDeck(
        List.of(Card.KING_CLUBS, Card.KING_CLUBS, Card.KING_DIAMONDS, Card.KING_HEARTS),
        List.of(),
        List.of()));
    Truth.assertThat(failure).hasMessageThat().contains("Card already used");
  }

  @Test
  public void dealDeck_homeContainsDiffDuplicates() {
    IllegalArgumentException failure = Assert.assertThrows(IllegalArgumentException.class, () -> ForkFreeCell.dealDeck(
        List.of(Card.KING_CLUBS, Card.ACE_CLUBS, Card.KING_DIAMONDS, Card.KING_HEARTS),
        List.of(),
        List.of()));
    Truth.assertThat(failure).hasMessageThat().contains("Card already used");
  }

  @Test
  public void dealDeck_homeAndFreeContainsDiffDuplicates() {
    IllegalArgumentException failure = Assert.assertThrows(IllegalArgumentException.class, () -> ForkFreeCell.dealDeck(
        List.of(Card.KING_CLUBS, Card.KING_DIAMONDS, Card.KING_HEARTS),
        List.of(Card.ACE_CLUBS),
        List.of()));
    Truth.assertThat(failure).hasMessageThat().contains("Card already used");
  }

  @Test
  public void dealDeck_homeAndFreeContainsSameDuplicates() {
    IllegalArgumentException failure = Assert.assertThrows(IllegalArgumentException.class, () -> ForkFreeCell.dealDeck(
        List.of(Card.KING_CLUBS, Card.KING_DIAMONDS, Card.KING_HEARTS),
        List.of(Card.KING_CLUBS),
        List.of()));
    Truth.assertThat(failure).hasMessageThat().contains("Card already used");
  }

  @Test
  public void game_toString() {
    ForkFreeCell game = ForkFreeCell.dealDeck(new SplittableRandom(1));
    System.out.println(game.toString());
    game.hashCode();
  }

  @Test
  public void canMoveToHomeCellFromTableau() {
    ForkFreeCell game = ForkFreeCell.dealDeck(new SplittableRandom(3));
    System.out.println(game.toString());
    assertTrue(game.canMoveToHomeCellFromTableau(0));
    game = game.moveToHomeCellFromTableau(0);
    System.out.println(game.toString());
  }

  @Test
  public void moveToFreeCellFromTableau() {
    ForkFreeCell game = ForkFreeCell.dealDeck(new SplittableRandom(3));
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);
    System.out.println(game.toString());

    assertFalse(game.canMoveToFreeCellFromTableau(0));
  }

  @Test
  public void moveToTableauCellFromTableau() {
    ForkFreeCell game = ForkFreeCell.dealDeck(new SplittableRandom(3));
    System.out.println(game.toString());

    var out = StreamSupport.stream(game.tableauColSpliterator(6), false).collect(Collectors.toList());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(0, 1, 2);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(0, 1, 1);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(1, 0, 1);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(1, 0, 1);
    System.out.println(game.toString());

    assertFalse(game.canMoveToFreeCellFromTableau(0));
  }

  @Test
  public void moveToTableauCellFromFree() {
    ForkFreeCell game = ForkFreeCell.parse("""
                          |                 \s
        TS   TD   6D   6H   5S   3D   8S   KD
        AS   9H   QS   4H   4S   4D   AH   JS
        JD   JH   5C   QD   9D   AC   7C   KS
        2S   3S   5H   6C   2D   TH   AD   7S
        6S   QC   5D   4C   8C   QH   7D   KC
        3H   JC   2H   8D   7H   2C   KH   TC
        9C   8H   3C   9S                   \s
        """);

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToFreeCellFromTableau(0);

    assertFalse(game.canMoveToFreeCellFromTableau(0));
  }

  @Test
  public void insertFreeCard_smaller() {
    byte[] cardIds = new byte[]{EMPTY, EMPTY, EMPTY, EMPTY, 4, EMPTY};
    ForkFreeCell.insertFreeCard(cardIds, (byte) 2);
    assertArrayEquals(new byte[]{EMPTY, EMPTY, EMPTY, EMPTY, 4, 2}, cardIds);
  }

  @Test
  public void insertFreeCard_bigger() {
    byte[] cardIds = new byte[]{EMPTY, EMPTY, EMPTY, EMPTY, 1, EMPTY};
    ForkFreeCell.insertFreeCard(cardIds, (byte) 2);
    assertArrayEquals(new byte[]{EMPTY, EMPTY, EMPTY, EMPTY, 2, 1}, cardIds);
  }

  @Test
  public void insertFreeCard_empty() {
    byte[] cardIds = new byte[]{EMPTY, EMPTY, EMPTY, EMPTY, EMPTY};
    ForkFreeCell.insertFreeCard(cardIds, (byte) 2);
    assertArrayEquals(new byte[]{EMPTY, EMPTY, EMPTY, EMPTY, 2}, cardIds);
  }

  @Test
  public void parse() {
    String board = """
      9C   9D   8H   9S | QS   9H   KD
                KH        JH   QH   KS   KC
                QC        TC   JS        QD
                JD             TH        JC
                TS                       TD
                  """;
    var parsed = ForkFreeCell.parse(board);

    var reparsed = ForkFreeCell.parse(parsed.toString());
    assertEquals(parsed, reparsed);
  }
}
