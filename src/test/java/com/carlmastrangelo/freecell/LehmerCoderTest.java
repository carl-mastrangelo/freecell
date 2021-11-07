package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.CARDS_BY_ORD;
import static com.carlmastrangelo.freecell.Card.CARD_COUNT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LehmerCoderTest {
  @Test
  public void shuffleCards() {
    var cards = new ArrayList<>(CARDS_BY_ORD);
    var rand = new Random(1);
    var coder = new LehmerCoder(CARD_COUNT);
    for (int i = 0; i < 10000; i++) {
      Collections.shuffle(cards, rand);

      int[] permdexes = coder.permdexes(new ArrayList<>(cards), CARDS_BY_ORD::get);
      BigInteger encoded = coder.encode(permdexes);


      var altPermdexes = coder.altPermdexes(cards, Card::ordinal);
      var c2 = new ArrayList<>();
      coder.altPermute(c2, altPermdexes, CARDS_BY_ORD::get);
      int mismatch = -1;
      for (int k = 0; k < cards.size(); k++) {
        if (cards.get(k) != c2.get(k)) {
          mismatch = k;
          break;
        }
      }

      var c3 = new ArrayList<>();
      coder.altPermute(c3, altPermdexes, CARDS_BY_ORD::get);

      int[] newPermdexes = coder.decode(CARD_COUNT, encoded);
      assertArrayEquals(permdexes, newPermdexes);

      List<Card> newCards = coder.permute(newPermdexes, CARDS_BY_ORD::get);
      assertEquals(cards, newCards);
    }
  }
}
