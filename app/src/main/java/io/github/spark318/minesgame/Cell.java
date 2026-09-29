package io.github.spark318.minesgame;

import java.io.Serializable;

/**
 * A single square on the board. Setters are package-private so only {@link MinesweeperGame}
 * can change a cell; the UI reads cells but never modifies them.
 */
public class Cell implements Serializable {

    private boolean isMine;
    private boolean isRevealed;
    private boolean isFlagged;
    private int adjacentMines;

    public boolean isMine() {
        return isMine;
    }

    void setMine(boolean mine) {
        isMine = mine;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    void setRevealed(boolean revealed) {
        isRevealed = revealed;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }

    public int getAdjacentMines() {
        return adjacentMines;
    }

    void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }
}
