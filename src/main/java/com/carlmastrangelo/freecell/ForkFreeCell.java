package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.ALL_CARDS_ORD;
import static com.carlmastrangelo.freecell.Card.ALL_RANKS;
import static com.carlmastrangelo.freecell.Card.ALL_SUITS;
import static com.carlmastrangelo.freecell.Card.CARDS;
import static com.carlmastrangelo.freecell.Card.RANKS;
import static com.carlmastrangelo.freecell.Card.SUITS;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class ForkFreeCell implements FreeCell<ForkFreeCell> {

  public static ForkFreeCell dealDeck() {
    return dealDeck(RandomGenerator.of("L64X256MixRandom"));
  }

  public static ForkFreeCell dealDeck(RandomGenerator rng) {
    Objects.requireNonNull(rng);
    List<Card> cards = Arrays.asList(ALL_CARDS_ORD.clone());
    shuffle(cards, rng);
    return dealDeck(List.of(), List.of(), cards);
  }

  public static ForkFreeCell dealDeck(List<Card> homeCells, List<Card> freeCells, List<Card> tableauCards) {
    List<List<Card>> columns = new ArrayList<>();
    for (int col = 0; col < Tableau.COLS; col++) {
      columns.add(new ArrayList<>());
    }
    int col = 0;
    for (Card card : tableauCards) {
      int dstCol = col++;
      if (col == Tableau.COLS) {
        col = 0;
      }
      if (card == null) {
        continue;
      }
      columns.get(dstCol).add(card);
    }
    return dealColumns(homeCells, freeCells, columns);
  }

  public static ForkFreeCell dealColumns(
      List<Card> homeCells, List<Card> freeCells, List<? extends List<Card>> tableauColumns) {
    Set<Card> allCards = EnumSet.allOf(Card.class);
    int homeCards = 0;
    for (Card card : homeCells) {
      do {
        checkRemove(allCards, card);
        homeCards++;
        card = card.lowerRank();
      } while (card != null);
    }
    byte[] cardIds = new byte[CARDS - homeCards + SUITS + Tableau.COLS];
    Arrays.fill(cardIds, EMPTY);
    for (Card card : homeCells) {
      byte cardId = cardId(card);
      cardIds[suitOrd(cardId)] = cardId;
    }
    int pos = SUITS;
    freeCells = freeCells.stream().sorted().collect(Collectors.toList());
    for (Card card : freeCells) {
      checkRemove(allCards, card);
      cardIds[pos++] = cardId(card);
    }

    int[] tableauRoot = new int[Tableau.COLS];
    int col = 0;
    for (List<Card> column : tableauColumns) {
      tableauRoot[col++] = pos++;
      for (Card card : column) {
        checkRemove(allCards, card);
        cardIds[pos++] = cardId(card);
      }
    }
    if (!allCards.isEmpty()) {
      throw new IllegalArgumentException("Not all cards used " + allCards);
    }
    return new ForkFreeCell(cardIds, tableauRoot);
  }



  /*
  * [1 2 3 4 X 5 3 X 6]
  *        A     B   C
  * [1 2 3 4 X 5 X 6 3]
  *        A   B     C
  * [1 2 3 4 X X 6 5 3]
  *        A B       C
  *
  *
  * */

  @VisibleForTesting
  static final byte EMPTY = -1;
  private static final Card[] ALL_CARDS_ID;

  static {
    assert ALL_SUITS.length == 4;
    assert ALL_RANKS.length == 13;
    ALL_CARDS_ID = new Card[64];
    for (Card card : ALL_CARDS_ORD) {
      ALL_CARDS_ID[cardId(card)] = card;
    }
  }

  private final byte[] cardIds;
  private final int[] tableauRoot;

  private ForkFreeCell(byte[] cardIds, int[] tableauRoot) {
    this.cardIds = cardIds;
    this.tableauRoot = tableauRoot;
  }

  @Override
  public boolean gameWon() {
    for (int i = 0; i < SUITS; i++) {
      if (cardIds[i] == EMPTY || rankOrd(cardIds[i]) != Card.Rank.KING_ORD) {
        return false;
      }
    }
    return true;
  }

  @Override
  public ForkFreeCell moveToHomeCellFromTableau(int tableauCol) {
    int pos = tabTop(tableauCol);
    byte cardId = checkCardNotEmpty(cardIds[pos]);
    if (!canMoveHome(cardId)) {
      throw new IllegalArgumentException();
    }
    byte[] newCardIds = copyAndRemoveAt(cardIds, pos);
    newCardIds[suitOrd(cardId)] = cardId;
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = tableauCol + 1; col < newTableauRoot.length; col++) {
      tableauRoot[col]--;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  private boolean canMoveHome(byte cardId) {
    byte homeCardId;
    if ((homeCardId = cardIds[suitOrd(cardId)]) == EMPTY && rankOrd(cardId) == Card.Rank.ACE_ORD) {
      return true;
    }
    return homeCardId + 1 == cardId;
  }

  @Override
  public boolean canMoveToHomeCellFromTableau(int tableauCol) {
    // TODO: test if this can be called with empty
    int pos = tabTop(tableauCol);
    byte cardId = cardIds[pos];
    if (cardId == EMPTY) {
      return false;
    }
    return canMoveHome(cardId);
  }

  @Override
  public ForkFreeCell moveToHomeCellFromFreeCell(int freeCol) {
    assert canMoveToHomeCellFromFreeCell(freeCol);
    int pos = SUITS + freeCol;
    byte cardId = cardIds[pos];
    if (!canMoveHome(cardId)) {
      throw new IllegalArgumentException();
    }
    byte[] newCardIds = copyAndRemoveAt(cardIds, pos);
    newCardIds[suitOrd(cardId)] = cardId;
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = 0; col < newTableauRoot.length; col++) {
      newTableauRoot[col]--;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToHomeCellFromFreeCell(int freeCol) {
    if (freeCol >= tabRoot(0) || freeCol < 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return false;
  }

  @Override
  public ForkFreeCell moveToFreeCellFromTableau(int tableauCol) {
    assert !tableauEmpty(tableauCol);
    int topTabCardPos = tabTop(tableauCol);
    byte cardId = checkCardNotEmpty(cardIds[topTabCardPos]);
    byte[] newCardIds = new byte[cardIds.length];
    int newLastFreePos = tabRoot(0);
    // In the array [H H H H F F R], R is the tableau root.  Add 1 to the length
    System.arraycopy(cardIds, 0, newCardIds, 0, newLastFreePos + 1);
    insertFreeCard(newCardIds, cardId);
    assert newCardIds[newLastFreePos] != EMPTY;
    // The final argument would normally be (topTabCardPos - newLastFreePos + 1), but we will overwrite the top tab
    // card in the next call.
    System.arraycopy(cardIds, newLastFreePos, newCardIds, newLastFreePos + 1, topTabCardPos - newLastFreePos);
    System.arraycopy(cardIds, topTabCardPos + 1, newCardIds, topTabCardPos + 1, newCardIds.length - topTabCardPos - 1);
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = 0; col <= tableauCol; col++) {
      newTableauRoot[col]++;
    }
    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  private static byte checkCardNotEmpty(byte cardId) {
    if (cardId == EMPTY) {
      throw new IllegalArgumentException("empty card");
    }
    return cardId;
  }

  private static void checkRemove(Set<Card> cards, Card card) {
    if (!cards.remove(Objects.requireNonNull(card))) {
      throw new IllegalArgumentException("Card already used " + card);
    }
  }

  /**
   * Inserts the card in the free cell.  This overwrites the first tableau root.
   */
  @VisibleForTesting
  static void insertFreeCard(byte[] cardIds, byte cardId) {
    byte freeCardId;
    int pos = SUITS;
    for (; (freeCardId = cardIds[pos]) != EMPTY; pos++) {
      assert freeCardId != cardId;
      if (freeCardId < cardId) {
        cardIds[pos] = cardId;
        cardId = freeCardId;
      }
    }
    assert freeCardId == EMPTY;
    cardIds[pos] = cardId;
  }

  @Override
  public boolean canMoveToFreeCell() {
    return tabRoot(0) < SUITS + FreeCells.COLS;
  }

  public boolean tableauEmpty(int tableauCol) {
    assert tabTop(tableauCol) >= tabRoot(tableauCol);
    return tabTop(tableauCol) == tabRoot(tableauCol);
  }

  @Override
  @Nullable
  public Card peekTableau(int tableauCol) {
    byte cardId = cardIds[tabTop(tableauCol)];
    if (cardId == EMPTY) {
      return null;
    }
    return ALL_CARDS_ID[cardId];
  }

  @Override
  @Nullable
  public Card peekFreeCell(int freeCol) {
    return null;
  }

  @Override
  public ForkFreeCell moveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    return null;
  }

  @Override
  public boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    return false;
  }

  @Override
  public ForkFreeCell moveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    return null;
  }

  @Override
  public boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    byte cardId;
    for (;pos < SUITS; pos++) {
      sb.append("  ").append((cardId = cardIds[pos]) == EMPTY ? "  " : ALL_CARDS_ID[cardId]);
    }
    sb.append("||");
    while((cardId = cardIds[pos]) != EMPTY) {
      sb.append("  ").append(ALL_CARDS_ID[cardId]);
      pos++;
    }

    List<Deque<Card>> cols = new ArrayList<>();
    for (int col = 0; col < Tableau.COLS; col++) {
      cols.add(getCol(col));
    }
    boolean output;
    do {
      output = false;
      sb.append("\n");
      for (int col = 0; col < Tableau.COLS; col++) {
        sb.append("  ");
        Card card;
        if ((card = cols.get(col).pollLast()) == null) {
          sb.append("  ");
        } else {
          output = true;
          sb.append(card);
        }
      }
    } while(output);

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ForkFreeCell that = (ForkFreeCell) o;
    return Arrays.equals(cardIds, that.cardIds);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(cardIds);
  }

  private static byte cardId(Card card) {
    return (byte) ((card.suit().ordinal() << 4) + card.rank().ordinal());
  }

  private static int cardOrd(byte cardId) {
    return suitOrd(cardId) * RANKS + rankOrd(cardId);
  }

  private static int suitOrd(byte cardId) {
    assert (cardId >> 4) < SUITS;
    assert (cardId >> 4) >= 0;
    return cardId >> 4;
  }

  private static int rankOrd(byte cardId) {
    assert (cardId & 0xF) < RANKS;
    return cardId & 0xF;
  }

  private static void shuffle(List<Card> cards, RandomGenerator rng) {
    for (int i = cards.size(); i > 1; i--) {
      Card c1 = cards.get(i - 1);
      int pos = rng.nextInt(i);
      Card c2 = cards.get(pos);

      cards.set(i - 1, c2);
      cards.set(pos, c1);
    }
  }

  private Deque<Card> getCol(int col) {
    Deque<Card> cards = new ArrayDeque<>();
    int pos = tabTop(col);
    byte cardId;
    while ((cardId = cardIds[pos--]) != EMPTY) {
      cards.addLast(ALL_CARDS_ORD[cardOrd(cardId)]);
    }
    return cards;
  }

  private int tabTop(int col) {
    if (col + 1 == Tableau.COLS) {
      return cardIds.length - 1;
    }
    return tabRoot(col + 1) - 1;
  }

  private int tabRoot(int col) {
    return tableauRoot[col];
  }

  // TODO: test
  @VisibleForTesting
  static byte[] copyAndRemoveAt(byte[] arr, int pos) {
    byte[] newArr = new byte[arr.length - 1];
    System.arraycopy(arr, 0, newArr, 0, pos);
    System.arraycopy(arr, pos + 1, newArr, pos, arr.length - pos - 1);
    return newArr;
  }
}
