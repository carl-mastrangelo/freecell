package com.carlmastrangelo.freecell.coder;

import static com.carlmastrangelo.freecell.Card.CARDS_BY_ORD;
import static com.carlmastrangelo.freecell.Card.CARD_COUNT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.carlmastrangelo.freecell.Card;
import com.carlmastrangelo.freecell.coder.LehmerCoder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
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

      //int[] permdexes = coder.permdexes(new ArrayList<>(cards), CARDS_BY_ORD::get);

      var altPermdexes = coder.altPermdexes(cards, Card::ordinal);
      BigInteger encoded = coder.encode(altPermdexes);

      int[] newPermdexes = coder.decode(CARD_COUNT, encoded);
      assertArrayEquals(altPermdexes, newPermdexes);

      //List<Card> newCards = coder.permute(newPermdexes, CARDS_BY_ORD::get);
      var newCards = new ArrayList<>(CARD_COUNT);
      coder.altPermute(newCards, altPermdexes, CARDS_BY_ORD::get);
      assertEquals(cards, newCards);
    }
  }
}
