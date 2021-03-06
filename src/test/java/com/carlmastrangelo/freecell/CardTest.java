package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.CARDS_BY_ORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CardTest {

  @Test
  public void rankLower() {
    assertNull(Rank.ACE.lower());
    assertEquals(Rank.ACE, Rank.TWO.lower());
    assertEquals(Rank.QUEEN, Rank.KING.lower());
  }

  @Test
  public void rankUpper() {
    assertNull(Rank.KING.upper());
    assertEquals(Rank.KING, Rank.QUEEN.upper());
    assertEquals(Rank.TWO, Rank.ACE.upper());
  }

  @Test
  public void suitLower() {
    assertNull(Suit.CLUBS.lower());
    assertEquals(Suit.CLUBS, Suit.DIAMONDS.lower());
    assertEquals(Suit.HEARTS, Suit.SPADES.lower());
  }

  @Test
  public void suitUpper() {
    assertNull(Suit.SPADES.upper());
    assertEquals(Suit.SPADES, Suit.HEARTS.upper());
    assertEquals(Suit.DIAMONDS, Suit.CLUBS.upper());
  }
}
