package com.carlmastrangelo.freecell.player;

import com.carlmastrangelo.freecell.FreeCell;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

public final class GamePlayer {


  public static void main(String [] args) {
    new GamePlayer().play();
  }

  private void play() {
    FreeCell freeCell = new FreeCell();
    RandomGenerator rng = new SplittableRandom(1);
    freeCell.deal(rng);


  }
}
