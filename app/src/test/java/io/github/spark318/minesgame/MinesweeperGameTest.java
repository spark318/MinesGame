package io.github.spark318.minesgame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public class MinesweeperGameTest {

    // ---- Mine placement ----

    @Test
    public void firstRevealPlacesExactMineCount() {
        for (long seed = 0; seed < 50; seed++) {
            MinesweeperGame game = new MinesweeperGame(10, 10, 12, new Random(seed));
            game.reveal(5, 5);
            assertEquals(12, countMines(game));
        }
    }

    @Test
    public void firstRevealAndItsNeighborsAreNeverMines() {
        for (long seed = 0; seed < 200; seed++) {
            MinesweeperGame game = new MinesweeperGame(10, 10, 30, new Random(seed));
            game.reveal(0, 0);
            assertFalse(game.getState() == MinesweeperGame.State.LOST);
            for (int r = 0; r <= 1; r++) {
                for (int c = 0; c <= 1; c++) {
                    assertFalse("seed " + seed, game.getCell(r, c).isMine());
                }
            }
        }
    }

    @Test
    public void denseBoardStillProtectsFirstCell() {
        // 3x3 with 8 mines leaves no room to protect the neighbors, only the tapped cell.
        MinesweeperGame game = new MinesweeperGame(3, 3, 8, new Random(1));
        game.reveal(1, 1);
        assertFalse(game.getCell(1, 1).isMine());
        assertEquals(8, countMines(game));
        assertEquals(MinesweeperGame.State.WON, game.getState());
    }

    @Test
    public void rejectsInvalidMineCounts() {
        assertThrows(IllegalArgumentException.class, () -> new MinesweeperGame(3, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> new MinesweeperGame(3, 3, 9));
        assertThrows(IllegalArgumentException.class, () -> new MinesweeperGame(0, 3, 1));
    }

    // ---- Adjacent counts ----

    @Test
    public void adjacentCountsIncludeDiagonals() {
        // M . .
        // . . .
        // . . M
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}, {2, 2}});
        assertEquals(2, game.getCell(1, 1).getAdjacentMines());
        assertEquals(1, game.getCell(0, 1).getAdjacentMines());
        assertEquals(0, game.getCell(0, 2).getAdjacentMines());
    }

    // ---- Revealing ----

    @Test
    public void revealingNumberedCellRevealsOnlyThatCell() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}});
        game.reveal(1, 1);
        assertTrue(game.getCell(1, 1).isRevealed());
        assertEquals(1, countRevealed(game));
    }

    @Test
    public void revealingEmptyCellFloodFillsUpToNumberBorder() {
        // Mine in the corner of a 5x5: revealing the far corner opens everything except the mine.
        MinesweeperGame game = MinesweeperGame.withMines(5, 5, new int[][]{{0, 0}});
        game.reveal(4, 4);
        assertEquals(24, countRevealed(game));
        assertFalse(game.getCell(0, 0).isRevealed());
    }

    @Test
    public void floodFillStopsAtFlaggedCells() {
        MinesweeperGame game = MinesweeperGame.withMines(5, 5, new int[][]{{0, 0}});
        game.toggleFlag(2, 2);
        game.reveal(4, 4);
        assertFalse(game.getCell(2, 2).isRevealed());
    }

    @Test
    public void floodFillHandlesLargeBoardsWithoutStackOverflow() {
        MinesweeperGame game = MinesweeperGame.withMines(500, 500, new int[][]{{0, 0}});
        game.reveal(499, 499);
        assertEquals(MinesweeperGame.State.WON, game.getState());
    }

    @Test
    public void revealingMineLosesAndShowsAllMines() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}, {2, 2}});
        game.reveal(0, 0);
        assertEquals(MinesweeperGame.State.LOST, game.getState());
        assertTrue(game.getCell(2, 2).isRevealed());
    }

    @Test
    public void flaggedCellCannotBeRevealed() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}});
        game.toggleFlag(0, 0);
        assertFalse(game.reveal(0, 0));
        assertEquals(MinesweeperGame.State.PLAYING, game.getState());
    }

    @Test
    public void outOfBoundsMovesAreIgnored() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}});
        assertFalse(game.reveal(-1, 0));
        assertFalse(game.reveal(0, 3));
        assertFalse(game.toggleFlag(3, 0));
    }

    // ---- Winning ----

    @Test
    public void revealingAllSafeCellsWinsAndFlagsMines() {
        MinesweeperGame game = MinesweeperGame.withMines(2, 2, new int[][]{{0, 0}});
        game.reveal(0, 1);
        game.reveal(1, 0);
        assertEquals(MinesweeperGame.State.PLAYING, game.getState());
        game.reveal(1, 1);
        assertEquals(MinesweeperGame.State.WON, game.getState());
        assertTrue(game.getCell(0, 0).isFlagged());
        assertEquals(0, game.getRemainingMineCount());
    }

    @Test
    public void flaggingEveryMineDoesNotWinOnItsOwn() {
        MinesweeperGame game = MinesweeperGame.withMines(2, 2, new int[][]{{0, 0}});
        game.toggleFlag(0, 0);
        assertEquals(MinesweeperGame.State.PLAYING, game.getState());
    }

    @Test
    public void noMovesAcceptedAfterGameOver() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}});
        game.reveal(0, 0);
        assertFalse(game.reveal(2, 2));
        assertFalse(game.toggleFlag(2, 2));
        assertFalse(game.getCell(2, 2).isRevealed());
    }

    // ---- Flags ----

    @Test
    public void flagsCannotBePlacedBeforeFirstReveal() {
        MinesweeperGame game = new MinesweeperGame(5, 5, 3);
        assertFalse(game.toggleFlag(0, 0));
        assertEquals(MinesweeperGame.State.NOT_STARTED, game.getState());
    }

    @Test
    public void togglingFlagUpdatesRemainingCount() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}});
        game.toggleFlag(1, 1);
        assertEquals(0, game.getRemainingMineCount());
        game.toggleFlag(2, 2);
        assertEquals(-1, game.getRemainingMineCount());
        game.toggleFlag(1, 1);
        assertEquals(0, game.getRemainingMineCount());
    }

    @Test
    public void revealedCellCannotBeFlagged() {
        MinesweeperGame game = MinesweeperGame.withMines(3, 3, new int[][]{{0, 0}});
        game.reveal(1, 1);
        assertFalse(game.toggleFlag(1, 1));
    }

    private static int countMines(MinesweeperGame game) {
        int count = 0;
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.getCell(r, c).isMine()) count++;
            }
        }
        return count;
    }

    private static int countRevealed(MinesweeperGame game) {
        int count = 0;
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.getCell(r, c).isRevealed()) count++;
            }
        }
        return count;
    }
}
