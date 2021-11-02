package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.ALL_CARDS;
import static com.carlmastrangelo.freecell.Card.ALL_SUITS;
import static com.carlmastrangelo.freecell.Card.SUITS;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class MutableFreeCell implements FreeCell {

  public static final int NO_FREE_CELL_IDX = FreeCells.NO_FREE_CELL;
  public static final int FREE_CELL_COLUMNS = FreeCells.COLS;
  public static final int TABLEAU_COLUMNS = Tableau.COLS;

  private static final Card[] EMPTY_FREE_CELLS = new Card[0];
  private static final Card[] EMPTY_HOME_CELLS = new Card[SUITS];

  private final Card[] homeCells;
  private final FreeCells freeCells;
  private final Tableau tableau;

  public MutableFreeCell() {
    this(new Card[Card.SUITS], new FreeCells(), new Tableau());
  }

  private MutableFreeCell(Card[] homeCells, FreeCells freeCells, Tableau tableau) {
    this.homeCells = homeCells;
    this.freeCells = freeCells;
    this.tableau = tableau;
  }

  public MutableFreeCell copy() {
    return new MutableFreeCell(homeCells.clone(), freeCells.copy(), tableau.copy());
  }

  public void deal() {
    deal(new Random());
  }

  public void deal(RandomGenerator rng) {
    Card[] cards = ALL_CARDS.clone();
    shuffle(cards, rng);
    deal(cards, EMPTY_FREE_CELLS, EMPTY_HOME_CELLS);
  }

  public void deal(List<Card> tableauCards, List<Card> freeCellCards, List<Card> homeCellCards) {
    Set<Card> allCardsSet = new LinkedHashSet<>(Arrays.asList(ALL_CARDS));
    for (Card card : tableauCards) {
      if (card == null) {
        continue;
      }
      if (!allCardsSet.remove(card)) {
        throw new IllegalArgumentException("Duplicate tableau card " + card);
      }
    }
    for (Card card : freeCellCards) {
      if (card == null) {
        continue;
      }
      if (!allCardsSet.remove(card)) {
        throw new IllegalArgumentException("Duplicate tableau card " + card);
      }
    }
    Card[] homeCells = new Card[SUITS];
    for (Card card : homeCellCards) {
      if (card == null) {
        continue;
      }
      homeCells[card.suit().ordinal()] = card;
      do {
        if (!allCardsSet.remove(card)) {
          throw new IllegalArgumentException("Duplicate tableau card " + card);
        }
        card = card.lowerRank();
      } while (card != null);
    }
    if (!allCardsSet.isEmpty()) {
      throw new IllegalArgumentException("Not all cards used " + allCardsSet);
    }

    deal(tableauCards.toArray(new Card[0]), freeCellCards.toArray(new Card[0]), homeCells);
  }

  private void deal(Card[] tableauCards, Card[] freeCellCards, Card[] homeCells) {
    int col = 0;
    for (Card card : tableauCards) {
      int dstCol = col++;
      if (col == Tableau.COLS) {
        col = 0;
      }
      if (card == null) {
        continue;
      }
      tableau.push(card, dstCol);
    }

    for (Card card : freeCellCards) {
      freeCells.push(card);
    }

    assert homeCells.length == ALL_SUITS.length;
    for (Card.Suit suit : ALL_SUITS) {
      assert this.homeCells[suit.ordinal()] == null : this.homeCells[suit.ordinal()];
      this.homeCells[suit.ordinal()] = homeCells[suit.ordinal()];
    }
  }

  @Override
  public boolean gameWon() {
    for (Card card : homeCells) {
      if (card == null || card.rank() != Card.Rank.KING) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void moveToHomeCellFromTableau(int tableauCol) {
    if (!canMoveToHomeCellFromTableau(tableauCol)) {
      throw new IllegalArgumentException("Can't move card");
    }
    Card card = tableau.pop(tableauCol);
    homeCells[card.suit().ordinal()] = card;
  }

  @Override
  public boolean canMoveToHomeCellFromTableau(int tableauCol) {
    return canMoveToHome(tableau.peek(tableauCol));
  }

  @Override
  public void moveToHomeCellFromFreeCell(int freeCol) {
    if (!canMoveToHomeCellFromFreeCell(freeCol)) {
      throw new IllegalArgumentException("Can't move card");
    }
    Card card = freeCells.pop(freeCol);
    homeCells[card.suit().ordinal()] = card;
  }

  @Override
  public boolean canMoveToHomeCellFromFreeCell(int freeCol) {
    return canMoveToHome(freeCells.peek(freeCol));
  }

  private boolean canMoveToHome(Card card) {
    Card homeCell = homeCells[card.suit().ordinal()];
    if (homeCell == null) {
      return card.rank() == Card.Rank.ACE;
    }
    return homeCell.rank().num() == card.rank().num() - 1;
  }

  /**
   * Returns if the given freecell column is empty.
   */
  @Override
  public boolean canMoveToFreeCell() {
    return freeCells.freeCell();
  }

  @Override
  public void moveToFreeCellFromTableau(int tableauCol) {
    if (!canMoveToFreeCell()) {
      throw new IllegalArgumentException();
    }

    Card card = tableau.pop(tableauCol);
    freeCells.push(card);
  }

  /**
   * Returns the top most card on the tableau column, or {@code null} if there is none.
   */
  @Override
  public Card peekTableau(int tableauCol) {
    return tableau.peek(tableauCol);
  }

  /**
   * Returns the card on the freecell column, or {@code null} if there is none.
   */
  @Override
  public Card peekFreeCell(int freeCol) {
    return freeCells.peek(freeCol);
  }

  @Override
  public void moveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    if (!canMoveToTableauFromTableau(dstTableauCol, srcTableauCol)) {
      throw new IllegalArgumentException();
    }
    tableau.push(tableau.pop(srcTableauCol), dstTableauCol);
  }

  @Override
  public boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    Card src = tableau.peek(srcTableauCol);
    return canMoveToTableau(dstTableauCol, src);
  }

  @Override
  public void moveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    if (!canMoveToTableauFromFreeCell(dstTableauCol, freeCol)) {
      throw new IllegalArgumentException();
    }
    tableau.push(freeCells.pop(freeCol), dstTableauCol);
  }

  public boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    Card src = freeCells.peek(freeCol);
    return canMoveToTableau(dstTableauCol, src);
  }

  public Card topHomeCell(Card.Suit suit) {
    return homeCells[suit.ordinal()];
  }

  public int score() {
    int sum = 0;
    for (Card card : homeCells) {
      if (card != null) {
        sum += card.rank().num();
      }
    }
    return sum;
  }

  private boolean canMoveToTableau(int dstTableauCol, Card src) {
    Card dst = tableau.peek(dstTableauCol);
    if (dst == null) {
      return true;
    }
    return dst.suit().color() != src.suit().color() && dst.rank().num() == src.rank().num() + 1;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Card card : homeCells) {
      sb.append("  ");
      if (card != null) {
        sb.append(card);
      } else {
        sb.append("  ");
      }
    }
    sb.append("||");
    freeCells.toString(sb);
    sb.append("\n\n");
    tableau.toString(sb);
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MutableFreeCell freeCell = (MutableFreeCell) o;
    return Arrays.equals(homeCells, freeCell.homeCells)
        && Objects.equals(freeCells, freeCell.freeCells)
        && Objects.equals(tableau, freeCell.tableau);
  }

  @Override
  public int hashCode() {
    int result = freeCells.hashCode();
    result = 31 * result + Arrays.hashCode(homeCells);
    return result;
  }

  private static void shuffle(Card[] cards, RandomGenerator rng) {
    for (int i = cards.length; i > 1; i--) {
      Card c1 = cards[i - 1];
      int pos = rng.nextInt(i);
      Card c2 = cards[pos];

      cards[i - 1] = c2;
      cards[pos] = c1;
    }
  }
}
