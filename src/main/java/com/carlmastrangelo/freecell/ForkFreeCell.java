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

public final class ForkFreeCell implements FreeCell {

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

  @Nullable
  @Override
  public Card topHomeCell(Card.Suit suit) {
    byte cardId = cardIds[suit.ordinal()];
    if (cardId == EMPTY) {
      return null;
    }
    return ALL_CARDS_ID[cardId];
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
  private static final int[] SUIT_COLOR = new int[]{0, 1, 1, 0};

  static {
    assert ALL_SUITS.length == 4;
    assert ALL_RANKS.length == 13;
    // TODO: assert on SUIT_COLOR matching actual values
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
    assert canMoveToHomeCellFromTableau(tableauCol);
    int srcTabPos = tabTop(tableauCol);
    byte cardId = checkCardNotEmpty(cardIds[srcTabPos]);
    byte[] newCardIds = copyAndRemoveAt(cardIds, srcTabPos);
    newCardIds[suitOrd(cardId)] = cardId;
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = tableauCol + 1; col < newTableauRoot.length; col++) {
      tableauRoot[col]--;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToHomeCellFromTableau(int tableauCol) {
    assert tableauCol >= 0 && tableauCol < Tableau.COLS;
    // TODO: test if this can be called with empty
    int srcTabPos = tabTop(tableauCol);
    byte cardId = cardIds[srcTabPos];
    if (cardId == EMPTY) {
      return false;
    }
    return canMoveHome(cardId);
  }

  @Override
  public ForkFreeCell moveToHomeCellFromFreeCell(int freeCol) {
    assert canMoveToHomeCellFromFreeCell(freeCol);
    int srcFreePos = SUITS + freeCol;
    byte cardId = cardIds[srcFreePos];
    byte[] newCardIds = copyAndRemoveAt(cardIds, srcFreePos);
    newCardIds[suitOrd(cardId)] = cardId;
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = 0; col < newTableauRoot.length; col++) {
      newTableauRoot[col]--;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToHomeCellFromFreeCell(int freeCol) {
    assert freeCol >= 0 && freeCol < FreeCells.COLS;
    if (freeCol >= freeCellsUsed()) {
      return false;
    }
    byte cardId = cardIds[SUITS + freeCol];
    assert cardId != EMPTY;
    return canMoveHome(cardId);
  }

  private boolean canMoveHome(byte cardId) {
    byte homeCardId = cardIds[suitOrd(cardId)];
    if (homeCardId == EMPTY && rankOrd(cardId) == Card.Rank.ACE_ORD) {
      return true;
    }
    return homeCardId + 1 == cardId;
  }

  @Override
  public ForkFreeCell moveToFreeCellFromTableau(int tableauCol) {
    assert canMoveToFreeCellFromTableau(tableauCol);
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

  @Override
  public boolean canMoveToFreeCellFromTableau(int tableauCol) {
    assert tableauCol >= 0 && tableauCol < Tableau.COLS;
    int srcTabPos = tabTop(tableauCol);
    byte cardId = cardIds[srcTabPos];
    if (cardId == EMPTY) {
      return false;
    }
    return freeCellsUsed() < FreeCells.COLS;
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

  public boolean tableauEmpty(int tableauCol) {
    assert tabTop(tableauCol) >= tabRoot(tableauCol);
    return tabTop(tableauCol) == tabRoot(tableauCol);
  }

  @Override
  @Nullable
  public Card peekTableau(int tableauCol) {
    // TODO: test
    assert tableauCol >= 0 && tableauCol < Tableau.COLS;
    byte cardId = cardIds[tabTop(tableauCol)];
    if (cardId == EMPTY) {
      return null;
    }
    return ALL_CARDS_ID[cardId];
  }

  @Override
  @Nullable
  public Card peekFreeCell(int freeCol) {
    // TODO: test
    assert freeCol >= 0 && freeCol < FreeCells.COLS;
    if (freeCol >= freeCellsUsed()) {
      return null;
    }
    return ALL_CARDS_ID[cardIds[SUITS + freeCol]];
  }

  @Override
  public ForkFreeCell moveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    assert canMoveToTableauFromTableau(dstTableauCol, srcTableauCol);
    int dstPos = tabTop(dstTableauCol);
    byte dstCardId = cardIds[dstPos];
    int srcPos = tabTop(srcTableauCol);
    byte srcCardId = checkCardNotEmpty(cardIds[srcPos]);
    assert dstCardId == EMPTY
         || (rankOrd(dstCardId) - 1 == rankOrd(srcCardId) && colorOrd(dstCardId) != colorOrd(srcCardId));
    byte[] newCardIds = new byte[cardIds.length];
    int[] newTableauRoot = tableauRoot.clone();
    /*
      Two cases:
      [ a b c d S e f g D h k]
      [ a b c d e f g D S h k]
                |     | + ---


      [ a b c d D e f g S h k]
      [ a b c d D S e f g h k]
                  + |   | ---
     */
    if (dstPos > srcPos) {
      System.arraycopy(cardIds, 0, newCardIds, 0, srcPos);
      System.arraycopy(cardIds, srcPos + 1, newCardIds, srcPos, dstPos - srcPos);
      newCardIds[dstPos] = srcCardId;
      System.arraycopy(cardIds, dstPos + 1, newCardIds, dstPos + 1, newCardIds.length - dstPos - 1);
      for (int col = srcTableauCol + 1; col <= dstTableauCol; col++) {
        newTableauRoot[col]--;
      }
    } else {
      System.arraycopy(cardIds, 0, newCardIds, 0, dstPos + 1);
      newCardIds[dstPos + 1] = srcCardId;
      System.arraycopy(cardIds, dstPos + 1, newCardIds, dstPos + 2, srcPos - dstPos - 1);
      System.arraycopy(cardIds, srcPos + 1, newCardIds, srcPos + 1, newCardIds.length - srcPos - 1);
      for (int col = dstTableauCol + 1; col <= srcTableauCol; col++) {
        newTableauRoot[col]++;
      }
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    assert dstTableauCol >= 0 && dstTableauCol < Tableau.COLS;
    assert srcTableauCol >= 0 && srcTableauCol < Tableau.COLS;
    if (srcTableauCol == dstTableauCol) {
      return false;
    }
    int srcPos = tabTop(srcTableauCol);
    byte srcCardId = cardIds[srcPos];
    if (srcCardId == EMPTY) {
      return false;
    }
    int dstPos = tabTop(dstTableauCol);
    byte dstCardId = cardIds[dstPos];
    if (dstCardId == EMPTY) {
      return true;
    }
    return rankOrd(dstCardId) - 1 == rankOrd(srcCardId) && colorOrd(dstCardId) != colorOrd(srcCardId);
  }

  @Override
  public ForkFreeCell moveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    assert canMoveToTableauFromFreeCell(dstTableauCol, freeCol);
    int dstTabPos = tabTop(dstTableauCol);
    byte dstCardId = checkCardNotEmpty(cardIds[dstTabPos]);
    int srcFreePos = SUITS + freeCol;
    byte srcCardId = cardIds[srcFreePos];

    byte[] newCardIds = new byte[cardIds.length];
    System.arraycopy(cardIds, 0, newCardIds, 0, srcFreePos);
    System.arraycopy(cardIds, srcFreePos, newCardIds, srcFreePos -  1, dstTabPos - srcFreePos);
    newCardIds[dstTabPos] = srcCardId;
    System.arraycopy(cardIds, dstTabPos + 1, newCardIds, dstTabPos + 1, newCardIds.length - dstTabPos - 1);
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = 0; col <= dstTableauCol; col++) {
      newTableauRoot[col]++;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    assert dstTableauCol >= 0 && dstTableauCol < Tableau.COLS;
    assert freeCol >= 0 && freeCol < FreeCells.COLS;
    if (freeCol >= freeCellsUsed()) {
      return false;
    }
    int srcFreePos = SUITS + freeCol;
    byte srcCardId = cardIds[srcFreePos];
    assert srcCardId != EMPTY;

    int dstTabPos = tabTop(dstTableauCol);
    byte dstCardId = cardIds[dstTabPos];
    if (dstCardId == EMPTY) {
      return true;
    }
    return rankOrd(dstCardId) - 1 == rankOrd(srcCardId) && colorOrd(dstCardId) != colorOrd(srcCardId);
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

  private static int colorOrd(byte cardId) {
    return SUIT_COLOR[suitOrd(cardId)];
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

  private int freeCellsUsed() {
    return tabRoot(0) - SUITS;
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

  // TODO: test
  @VisibleForTesting
  static byte[] copyAndRemoveAt(byte[] arr, int pos) {
    byte[] newArr = new byte[arr.length - 1];
    System.arraycopy(arr, 0, newArr, 0, pos);
    System.arraycopy(arr, pos + 1, newArr, pos, arr.length - pos - 1);
    return newArr;
  }
}
