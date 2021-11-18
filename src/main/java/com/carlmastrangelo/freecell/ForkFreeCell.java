package com.carlmastrangelo.freecell;

import static com.carlmastrangelo.freecell.Card.CARDS_BY_ORD;
import static com.carlmastrangelo.freecell.Card.CARD_COUNT;
import static com.carlmastrangelo.freecell.Rank.ACE;
import static com.carlmastrangelo.freecell.Rank.RANK_COUNT;
import static com.carlmastrangelo.freecell.Suit.SUIT_COUNT;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class ForkFreeCell implements FreeCell {

  public static ForkFreeCell dealDeck() {
    return dealDeck(RandomGenerator.of("L64X256MixRandom"));
  }

  public static ForkFreeCell dealDeck(RandomGenerator rng) {
    Objects.requireNonNull(rng);
    List<Card> cards = new ArrayList<>(Card.CARDS_BY_ORD);
    shuffle(cards, rng);
    return dealDeck(List.of(), List.of(), cards);
  }

  public static ForkFreeCell dealDeck(List<Card> homeCells, List<Card> freeCells, List<Card> tableauCards) {
    List<List<Card>> columns = new ArrayList<>();
    for (int col = 0; col < TABLEAU_COLS; col++) {
      columns.add(new ArrayList<>());
    }
    int col = 0;
    for (Card card : tableauCards) {
      int dstCol = col++;
      if (col == TABLEAU_COLS) {
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
    byte[] cardIds = new byte[CARD_COUNT - homeCards + HOME_CELLS + TABLEAU_COLS];
    Arrays.fill(cardIds, EMPTY);
    for (Card card : homeCells) {
      byte cardId = cardId(card);
      cardIds[suitOrd(cardId)] = cardId;
    }
    int pos = HOME_CELLS;
    freeCells = freeCells.stream().sorted().collect(Collectors.toList());
    for (Card card : freeCells) {
      checkRemove(allCards, card);
      cardIds[pos++] = cardId(card);
    }

    int[] tableauRoot = new int[TABLEAU_COLS];
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
  public Card topHomeCell(Suit suit) {
    byte cardId = cardIds[suit.ordinal()];
    if (isEmpty(cardId)) {
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

  private static final int HOME_CELLS = SUIT_COUNT;
  @VisibleForTesting
  static final byte EMPTY = -1;
  private static final Card[] ALL_CARDS_ID;
  private static final int[] SUIT_COLOR = new int[]{0, 1, 1, 0};

  static {
    assert SUIT_COUNT == 4;
    assert RANK_COUNT == 13;
    // TODO: assert on SUIT_COLOR matching actual values
    ALL_CARDS_ID = new Card[64];
    for (Card card : CARDS_BY_ORD) {
      ALL_CARDS_ID[cardId(card)] = card;
    }
  }

  private final byte[] cardIds;
  private final int[] tableauRoot;

  private ForkFreeCell(byte[] cardIds, int[] tableauRoot) {
    assert validateGame(cardIds, tableauRoot);
    this.cardIds = cardIds;
    this.tableauRoot = tableauRoot;
    if (false && !isSorted()) {
      //System.out.println(this);
      sort();
      assert isSorted();
      //System.out.println(isSorted());
      //.out.println(this);
      //System.out.println();
    }
  }

  @Override
  public boolean gameWon() {
    for (int i = 0; i < HOME_CELLS; i++) {
      if (isEmpty(cardIds[i]) || rankOrd(cardIds[i]) != Rank.KING_ORD) {
        return false;
      }
    }
    assert minMovesToWin() == 0;
    return true;
  }

  public int minMovesToWin() {
    int sum = 0;
    for (int i = 0; i < HOME_CELLS; i++) {
      if (isEmpty(cardIds[i])) {
        sum += RANK_COUNT;
      } else {
        sum += Rank.KING_ORD - rankOrd(cardIds[i]);
      }
    }
    return sum;
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
      newTableauRoot[col]--;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToHomeCellFromTableau(int tableauCol) {
    assert tableauCol >= 0 && tableauCol < TABLEAU_COLS;
    // TODO: test if this can be called with empty
    int srcTabPos = tabTop(tableauCol);
    byte cardId = cardIds[srcTabPos];
    if (isEmpty(cardId)) {
      return false;
    }
    return canMoveHome(cardId);
  }

  @Override
  public ForkFreeCell moveToHomeCellFromFreeCell(int freeCol) {
    assert canMoveToHomeCellFromFreeCell(freeCol);
    int srcFreePos = HOME_CELLS + freeCol;
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
    assert freeCol >= 0 && freeCol < FREE_CELLS;
    if (freeCol >= freeCellsUsed()) {
      return false;
    }
    byte cardId = cardIds[HOME_CELLS + freeCol];
    assert !isEmpty(cardId);
    return canMoveHome(cardId);
  }

  private boolean canMoveHome(byte cardId) {
    byte homeCardId = cardIds[suitOrd(cardId)];
    if (isEmpty(homeCardId) && rankOrd(cardId) == Rank.ACE_ORD) {
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
    assert !isEmpty(newCardIds[newLastFreePos]);
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
    assert tableauCol >= 0 && tableauCol < TABLEAU_COLS;
    int srcTabPos = tabTop(tableauCol);
    byte cardId = cardIds[srcTabPos];
    if (isEmpty(cardId)) {
      return false;
    }
    return freeCellsUsed() < FREE_CELLS;
  }

  /**
   * Inserts the card in the free cell.  This overwrites the first tableau root.
   */
  @VisibleForTesting
  static void insertFreeCard(byte[] cardIds, byte cardId) {
    byte freeCardId;
    int pos = HOME_CELLS;
    for (; !isEmpty(freeCardId = cardIds[pos]); pos++) {
      assert freeCardId != cardId;
      if (freeCardId < cardId) {
        cardIds[pos] = cardId;
        cardId = freeCardId;
      }
    }
    assert isEmpty(freeCardId);
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
    assert tableauCol >= 0 && tableauCol < TABLEAU_COLS;
    byte cardId = cardIds[tabTop(tableauCol)];
    if (isEmpty(cardId)) {
      return null;
    }
    return ALL_CARDS_ID[cardId];
  }

  @Override
  @Nullable
  public Card peekFreeCell(int freeCol) {
    // TODO: test
    assert freeCol >= 0 && freeCol < FREE_CELLS;
    if (freeCol >= freeCellsUsed()) {
      return null;
    }
    return ALL_CARDS_ID[cardIds[HOME_CELLS + freeCol]];
  }

  @Override
  public ForkFreeCell moveToTableauFromTableau(int dstTableauCol, int srcTableauCol, int count) {
    assert canMoveToTableauFromTableau(dstTableauCol, srcTableauCol, count);
    int dstPos = tabTop(dstTableauCol);
    byte dstCardId = cardIds[dstPos];
    int srcPos = tabTop(srcTableauCol) - count + 1;
    byte srcCardId = checkCardNotEmpty(cardIds[srcPos]);
    assert isEmpty(dstCardId)
         || (rankOrd(dstCardId) - 1 == rankOrd(srcCardId) && colorOrd(dstCardId) != colorOrd(srcCardId));
    byte[] newCardIds = new byte[cardIds.length];
    int[] newTableauRoot = tableauRoot.clone();
    /*
      Two cases:
      [ a b c d S S e f g D h k]
      [ a b c d e f g D S S h k]
                |     | + ---


      [ a b c d D e f g S S h k]
      [ a b c d D S S e f g h k]
                  + |   | ---
     */
    if (dstPos > srcPos) {
      System.arraycopy(cardIds, 0, newCardIds, 0, srcPos);
      System.arraycopy(cardIds, srcPos + count, newCardIds, srcPos, dstPos - srcPos - count + 1);
      System.arraycopy(cardIds, srcPos, newCardIds, dstPos - count + 1, count);
      System.arraycopy(cardIds, dstPos + 1, newCardIds, dstPos + 1, newCardIds.length - dstPos - 1);
      for (int col = srcTableauCol + 1; col <= dstTableauCol; col++) {
        newTableauRoot[col] -= count;
      }
    } else {
      System.arraycopy(cardIds, 0, newCardIds, 0, dstPos + 1);
      System.arraycopy(cardIds, srcPos, newCardIds, dstPos + 1, count);
      System.arraycopy(cardIds, dstPos + 1, newCardIds, dstPos + count + 1, srcPos - dstPos - 1);
      System.arraycopy(cardIds, srcPos + count, newCardIds, srcPos + count, newCardIds.length - (srcPos + count));
      for (int col = dstTableauCol + 1; col <= srcTableauCol; col++) {
        newTableauRoot[col] += count;
      }
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol) {
    assert dstTableauCol >= 0 && dstTableauCol < TABLEAU_COLS;
    assert srcTableauCol >= 0 && srcTableauCol < TABLEAU_COLS;
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
  public boolean canMoveToTableauFromTableau(int dstTableauCol, int srcTableauCol, int count) {
    assert dstTableauCol >= 0 && dstTableauCol < TABLEAU_COLS;
    assert srcTableauCol >= 0 && srcTableauCol < TABLEAU_COLS;
    assert count > 0 && count <= RANK_COUNT;
    if (srcTableauCol == dstTableauCol) {
      return false;
    }

    int srcTabTop = tabTop(srcTableauCol);
    int srcTabRoot = tabRoot(srcTableauCol);
    int srcStackSize = stackSize(srcTabRoot, srcTabTop);
    if (srcStackSize < count) {
      return false;
    }
    int srcPos = srcTabTop - count + 1;
    byte srcCardId = cardIds[srcPos];
    assert !isEmpty(srcCardId);

    int emptyUsableCols = 0;
    for (int col = 0; col < TABLEAU_COLS; col++) {
      if (col == dstTableauCol || col == srcTableauCol) {
        continue;
      }
      if (tabTop(col) == tabRoot(col)) {
        emptyUsableCols++;
      }
    }
    int freeCellsAvailable = FREE_CELLS - freeCellsUsed();
    int movableCards = freeCellsAvailable + 1;
    while (emptyUsableCols != 0) {
      movableCards *= 2;
      emptyUsableCols--;
    }
    if (count > movableCards) {
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
  public int stackSize(int tableauCol) {
    int tabTop = tabTop(tableauCol);
    int tabRoot = tabRoot(tableauCol);
    return stackSize(tabRoot, tabTop);
  }

  private int stackSize(int tabRoot, int tabTop) {
    int columnSize = tabTop - tabRoot;
    if (columnSize == 0 || columnSize == 1) {
      return columnSize;
    }
    int count = 1;
    for (int pos = tabTop; pos > tabRoot + 1; pos--) {
      byte cardId = cardIds[pos];
      byte underCardId = cardIds[pos - 1];
      if (colorOrd(cardId) == colorOrd(underCardId) || rankOrd(cardId) != rankOrd(underCardId) - 1) {
        break;
      }
      count++;
    }
    return count;
  }

  @Override
  public Spliterator<Card> readTableau(int tableauCol) {
    var top = tabTop(tableauCol);
    var root = tabRoot(tableauCol);

    return new Spliterators.AbstractSpliterator<>(top - root,
        Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.SUBSIZED) {
      @Override
      public boolean tryAdvance(Consumer<? super Card> action) {
        return false;
      }

      @Override
      public Spliterator<Card> trySplit() {
        return super.trySplit();
      }
    };
  }

  @Override
  public ForkFreeCell moveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    assert canMoveToTableauFromFreeCell(dstTableauCol, freeCol);
    int dstTabPos = tabTop(dstTableauCol);
    int srcFreePos = HOME_CELLS + freeCol;
    byte srcCardId = cardIds[srcFreePos];

    byte[] newCardIds = new byte[cardIds.length];
    System.arraycopy(cardIds, 0, newCardIds, 0, srcFreePos);
    System.arraycopy(cardIds, srcFreePos + 1, newCardIds, srcFreePos, dstTabPos - srcFreePos);
    newCardIds[dstTabPos] = srcCardId;
    System.arraycopy(cardIds, dstTabPos + 1, newCardIds, dstTabPos + 1, newCardIds.length - dstTabPos - 1);
    int[] newTableauRoot = tableauRoot.clone();
    for (int col = 0; col <= dstTableauCol; col++) {
      newTableauRoot[col]--;
    }

    return new ForkFreeCell(newCardIds, newTableauRoot);
  }

  @Override
  public boolean canMoveToTableauFromFreeCell(int dstTableauCol, int freeCol) {
    assert dstTableauCol >= 0 && dstTableauCol < TABLEAU_COLS;
    assert freeCol >= 0 && freeCol < FREE_CELLS;
    if (freeCol >= freeCellsUsed()) {
      return false;
    }
    int srcFreePos = HOME_CELLS + freeCol;
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
  public void readTableau(Collection<? super Card> column, int tableauCol) {
    assert tableauCol >= 0 && tableauCol < TABLEAU_COLS;
    int colRoot = tabRoot(tableauCol);
    byte cardId;
    while (++colRoot != cardIds.length && (cardId = cardIds[colRoot]) != EMPTY) {
      column.add(ALL_CARDS_ID[cardId]);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    byte cardId;
    for (;pos < HOME_CELLS; pos++) {
      sb.append("  ").append((cardId = cardIds[pos]) == EMPTY ? "  " : ALL_CARDS_ID[cardId]);
    }
    sb.append("||");
    while((cardId = cardIds[pos]) != EMPTY) {
      sb.append("  ").append(ALL_CARDS_ID[cardId]);
      pos++;
    }

    List<Deque<Card>> cols = new ArrayList<>();
    for (int col = 0; col < TABLEAU_COLS; col++) {
      cols.add(getCol(col));
    }
    boolean output;
    do {
      output = false;
      sb.append("\n");
      for (int col = 0; col < TABLEAU_COLS; col++) {
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
    sb.delete(sb.lastIndexOf("\n"), sb.length() -1);

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
    return suitOrd(cardId) * RANK_COUNT + rankOrd(cardId);
  }

  private static int suitOrd(byte cardId) {
    assert (cardId >>> 4) < SUIT_COUNT;
    assert (cardId >>> 4) >= 0;
    return cardId >> 4;
  }

  private static int rankOrd(byte cardId) {
    assert (cardId & 0xF) < RANK_COUNT;
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
      cards.addLast(CARDS_BY_ORD.get(cardOrd(cardId)));
    }
    return cards;
  }

  /**
   * Returns the index of top most card of this column, or the tab root if there are no cards in the column.
   */
  private int tabTop(int col) {
    if (col + 1 == TABLEAU_COLS) {
      return cardIds.length - 1;
    }
    return tabRoot(col + 1) - 1;
  }

  /**
   * Returns the index of the {@link #EMPTY} card root of this column.
   */
  private int tabRoot(int col) {
    return tableauRoot[col];
  }

  private int freeCellsUsed() {
    return tabRoot(0) - HOME_CELLS;
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

  private void sort() {
    record SortOrder(int col, byte bottom) {}
    List<SortOrder> order = new ArrayList<>(TABLEAU_COLS);

    for (int col = 0; col < tableauRoot.length; col++) {
      byte cardId = EMPTY;
      if (tableauRoot[col] != cardIds.length - 1) {
        cardId = cardIds[tableauRoot[col] + 1];
      }
      order.add(new SortOrder(col, cardId));
    }
    order.sort(Comparator.comparing(SortOrder::bottom));
    byte[] oldCardIds = cardIds.clone();
    int[] oldTableauRoot = Arrays.copyOf(tableauRoot, tableauRoot.length + 1);
    oldTableauRoot[tableauRoot.length] = cardIds.length;
    int pos = tabRoot(0);
    int col = 0;
    for (SortOrder so : order) {
      int bytes = oldTableauRoot[so.col + 1] - oldTableauRoot[so.col];
      System.arraycopy(oldCardIds, oldTableauRoot[so.col], cardIds, pos, bytes);

      tableauRoot[col++] = pos;
      pos += bytes;
    }
  }

  private boolean isSorted() {
    for (int col = 0; col < tableauRoot.length - 1; col++) {
      byte cardIdLeft = cardIds[tableauRoot[col] + 1];
      byte cardIdRight = EMPTY;
      if (tableauRoot[col + 1] != cardIds.length - 1) {
        cardIdRight = cardIds[tableauRoot[col + 1] + 1];
      }
      if (cardIdRight < cardIdLeft) {
        return false;
      }
    }
    return true;
  }

  private static boolean isEmpty(byte cardId) {
    return cardId == EMPTY;
  }

  private static boolean validateGame(byte[] cardIds, int[] tableauRoot) {
    Objects.requireNonNull(cardIds);
    BitSet cards = new BitSet();
    cards.set(0, 52);
    for (int i = 0; i < HOME_CELLS; i++) {
      if (isEmpty(cardIds[i])) {
        continue;
      }
      for (byte home = cardIds[i]; ; home--) {
        Card card = validateCardId(home);
        clear(cards, home);
        if (card.rank() == ACE) {
          break;
        }
      }
    }
    int empties = 0;
    for (int i = HOME_CELLS; i < cardIds.length; i++) {
      if (isEmpty(cardIds[i])) {
        empties++;
      } else {
        clear(cards, cardIds[i]);
      }
    }
    if (empties != TABLEAU_COLS) {
      throw new IllegalArgumentException("bad number of column roots " + empties);
    }
    if (!cards.isEmpty()) {
      throw new IllegalArgumentException("missing cards "
          + cards.stream().mapToObj(CARDS_BY_ORD::get).collect(Collectors.toList()));
    }
    Objects.requireNonNull(tableauRoot);
    BitSet roots = new BitSet();
    roots.set(0, TABLEAU_COLS);
    if (tableauRoot.length != TABLEAU_COLS) {
      throw new IllegalArgumentException("bad number of tableau column roots " + tableauRoot.length);
    }
    for (int i = 0; i < tableauRoot.length; i++) {
      int root = tableauRoot[i];
      if (root < HOME_CELLS || root >= cardIds.length) {
        throw new ArrayIndexOutOfBoundsException("column root " + root + " out of bounds ");
      }
      if (!isEmpty(cardIds[root])) {
        throw new IllegalArgumentException("column root " + root + "points to non-empty card " + cardIds[root]);
      }
      if (!roots.get(i)) {
        throw new IllegalArgumentException("duplicate column root " + root);
      }
      roots.clear(i);
    }
    if (tableauRoot[0] - HOME_CELLS > FREE_CELLS) {
      throw new IllegalArgumentException("too many free cells");
    }
    return roots.isEmpty();
  }

  private static void clear(BitSet set, byte cardId) {
    if (isEmpty(cardId)) {
      throw new IllegalArgumentException("empty card " + cardId);
    }
    Card card = validateCardId(cardId);
    if (!set.get(card.ordinal())) {
      throw new IllegalArgumentException("card already used (" + cardId + ") " + card);
    }
    set.clear(card.ordinal());
  }

  private static Card validateCardId(byte cardId) {
    if (cardId < 0 || cardId >= ALL_CARDS_ID.length) {
      throw new IllegalArgumentException("bad card " + cardId);
    }
    Card card = ALL_CARDS_ID[cardId];
    if (card == null) {
      throw new IllegalArgumentException("bad card " + cardId);
    }
    return card;
  }
}
