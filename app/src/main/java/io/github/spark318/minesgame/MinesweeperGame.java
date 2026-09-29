package io.github.spark318.minesgame;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for Minesweeper, with no Android dependencies so it can be unit tested on the JVM.
 *
 * <p>Mines are placed lazily on the first reveal so the first tap (and its neighbors, when the
 * board has room) is always safe. Revealing an empty cell flood-fills iteratively rather than
 * recursively, so large boards can't overflow the stack.
 */
public class MinesweeperGame implements Serializable {

    public enum State { NOT_STARTED, PLAYING, WON, LOST }

    private final int rows;
    private final int cols;
    private final int mineCount;
    private final Cell[][] board;
    private final Random random;

    private State state = State.NOT_STARTED;
    private int flagsPlaced = 0;
    private int revealedCount = 0;

    public MinesweeperGame(int rows, int cols, int mineCount) {
        this(rows, cols, mineCount, new Random());
    }

    MinesweeperGame(int rows, int cols, int mineCount, Random random) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board must be at least 1x1");
        }
        if (mineCount <= 0 || mineCount >= rows * cols) {
            throw new IllegalArgumentException("Mine count must be between 1 and " + (rows * cols - 1));
        }
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.random = random;
        this.board = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board[r][c] = new Cell();
            }
        }
    }

    /** Creates a game with mines at fixed positions, skipping random placement. Used by tests. */
    static MinesweeperGame withMines(int rows, int cols, int[][] minePositions) {
        MinesweeperGame game = new MinesweeperGame(rows, cols, minePositions.length);
        for (int[] pos : minePositions) {
            game.board[pos[0]][pos[1]].setMine(true);
        }
        game.calculateAdjacentMines();
        game.state = State.PLAYING;
        return game;
    }

    /**
     * Reveals a cell. The first reveal of a game places the mines. Hidden flagged cells are
     * ignored, as are all moves once the game is over.
     *
     * @return true if the board changed
     */
    public boolean reveal(int r, int c) {
        if (isOver() || !inBounds(r, c)) {
            return false;
        }
        if (state == State.NOT_STARTED) {
            placeMines(r, c);
            calculateAdjacentMines();
            state = State.PLAYING;
        }

        Cell cell = board[r][c];
        if (cell.isRevealed() || cell.isFlagged()) {
            return false;
        }

        if (cell.isMine()) {
            cell.setRevealed(true);
            state = State.LOST;
            revealAllMines();
            return true;
        }

        floodReveal(r, c);
        if (revealedCount == rows * cols - mineCount) {
            state = State.WON;
            flagAllMines();
        }
        return true;
    }

    /**
     * Toggles a flag on a hidden cell. Flags can't be placed before the first reveal, since the
     * mines don't exist yet.
     *
     * @return true if the board changed
     */
    public boolean toggleFlag(int r, int c) {
        if (state != State.PLAYING || !inBounds(r, c)) {
            return false;
        }
        Cell cell = board[r][c];
        if (cell.isRevealed()) {
            return false;
        }
        cell.setFlagged(!cell.isFlagged());
        flagsPlaced += cell.isFlagged() ? 1 : -1;
        return true;
    }

    private void placeMines(int safeR, int safeC) {
        // Keep the 3x3 block around the first tap clear when there's room, so the opening move
        // always reveals an area instead of a single number. Fall back to only the tapped cell.
        boolean protectNeighbors = rows * cols - 9 >= mineCount;

        List<Integer> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean isSafe = protectNeighbors
                        ? Math.abs(r - safeR) <= 1 && Math.abs(c - safeC) <= 1
                        : r == safeR && c == safeC;
                if (!isSafe) {
                    candidates.add(r * cols + c);
                }
            }
        }

        // Shuffling and taking a prefix gives a uniform placement without the retry loop that
        // rejection sampling needs as the board fills up.
        Collections.shuffle(candidates, random);
        for (int i = 0; i < mineCount; i++) {
            int index = candidates.get(i);
            board[index / cols][index % cols].setMine(true);
        }
    }

    private void calculateAdjacentMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (inBounds(r + dr, c + dc) && board[r + dr][c + dc].isMine()) {
                            count++;
                        }
                    }
                }
                board[r][c].setAdjacentMines(count);
            }
        }
    }

    private void floodReveal(int startR, int startC) {
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startR, startC});
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            Cell cell = board[pos[0]][pos[1]];
            if (cell.isRevealed() || cell.isFlagged() || cell.isMine()) {
                continue;
            }
            cell.setRevealed(true);
            revealedCount++;

            if (cell.getAdjacentMines() == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = pos[0] + dr;
                        int nc = pos[1] + dc;
                        if (inBounds(nr, nc) && !board[nr][nc].isRevealed()) {
                            queue.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
    }

    private void revealAllMines() {
        for (Cell[] row : board) {
            for (Cell cell : row) {
                if (cell.isMine() && !cell.isFlagged()) {
                    cell.setRevealed(true);
                }
            }
        }
    }

    private void flagAllMines() {
        for (Cell[] row : board) {
            for (Cell cell : row) {
                if (cell.isMine() && !cell.isFlagged()) {
                    cell.setFlagged(true);
                    flagsPlaced++;
                }
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public Cell getCell(int r, int c) {
        return board[r][c];
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMineCount() {
        return mineCount;
    }

    /** Mines minus flags placed. Can go negative, as in classic Minesweeper. */
    public int getRemainingMineCount() {
        return mineCount - flagsPlaced;
    }

    public State getState() {
        return state;
    }

    public boolean isOver() {
        return state == State.WON || state == State.LOST;
    }
}
