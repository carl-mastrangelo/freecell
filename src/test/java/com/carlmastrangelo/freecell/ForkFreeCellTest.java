package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.ForkFreeCell.EMPTY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.truth.Truth;
import java.util.List;
import java.util.SplittableRandom;
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

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(1, 0);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(0, 1);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(1, 0);
    System.out.println(game.toString());

    assertTrue(game.canMoveToFreeCellFromTableau(0));
    game = game.moveToTableauFromTableau(1, 0);
    System.out.println(game.toString());

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
}
