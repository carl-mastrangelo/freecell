package com.carlmastrangelo.freecell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CardTest {

  @Test
  public void rankLower() {
    assertNull(Card.Rank.ACE.lower());
    assertEquals(Card.Rank.ACE, Card.Rank.TWO.lower());
    assertEquals(Card.Rank.QUEEN, Card.Rank.KING.lower());
  }

  @Test
  public void rankUpper() {
    assertNull(Card.Rank.KING.upper());
    assertEquals(Card.Rank.KING, Card.Rank.QUEEN.upper());
    assertEquals(Card.Rank.TWO, Card.Rank.ACE.upper());
  }

  @Test
  public void suitLower() {
    assertNull(Card.Suit.CLUBS.lower());
    assertEquals(Card.Suit.CLUBS, Card.Suit.DIAMONDS.lower());
    assertEquals(Card.Suit.HEARTS, Card.Suit.SPADES.lower());
  }

  @Test
  public void suitUpper() {
    assertNull(Card.Suit.SPADES.upper());
    assertEquals(Card.Suit.SPADES, Card.Suit.HEARTS.upper());
    assertEquals(Card.Suit.DIAMONDS, Card.Suit.CLUBS.upper());
  }
}
