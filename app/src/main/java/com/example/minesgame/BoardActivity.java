package com.example.minesgame;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class BoardActivity extends AppCompatActivity {

    private final int BOARD_SIZE = 10;
    private final int NUMBER_OF_MINES = 5;

    private MineCell[][] board = new MineCell[BOARD_SIZE][BOARD_SIZE];
    private Button[][] cellButtons = new Button[BOARD_SIZE][BOARD_SIZE];

    private TextView mineCountText;
    private TextView timerText;
    private Button modeButton;
    private GridLayout boardGrid;

    private boolean isDigMode = true;
    private int minesToFlag;
    private boolean isGameOver = false;
    private boolean isGameWon = false;
    private boolean isFirstClick = true;

    private long startTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isGameOver) {
                long millis = System.currentTimeMillis() - startTime;
                long seconds = millis / 1000;
                timerText.setText("Time: " + String.format("%03d", seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        mineCountText = findViewById(R.id.mine_count_text);
        timerText = findViewById(R.id.timer_text);
        modeButton = findViewById(R.id.mode_button);
        boardGrid = findViewById(R.id.board_grid);

        modeButton.setOnClickListener(v -> {
            isDigMode = !isDigMode;
            modeButton.setText(isDigMode ? "Dig" : "Flag");
        });

        startNewGame();
    }

    private void startNewGame() {
        isGameOver = false;
        isGameWon = false;
        isFirstClick = true;
        isDigMode = true;
        minesToFlag = NUMBER_OF_MINES;
        modeButton.setText("Dig");
        timerText.setText("Time: 000");
        updateMineCount();

        boardGrid.removeAllViews();
        boardGrid.setColumnCount(BOARD_SIZE);
        boardGrid.setRowCount(BOARD_SIZE);

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                board[r][c] = new MineCell();
                Button button = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r, 1f),
                        GridLayout.spec(c, 1f)
                );
                params.width = 0;
                params.height = 0;
                params.setMargins(2, 2, 2, 2);
                button.setLayoutParams(params);
                button.setBackgroundColor(Color.parseColor("#4CAF50")); // Green color
                
                final int finalR = r;
                final int finalC = c;
                button.setOnClickListener(v -> onCellClick(finalR, finalC));

                boardGrid.addView(button);
                cellButtons[r][c] = button;
            }
        }
    }

    private void onCellClick(int r, int c) {
        if (isGameOver) {
            long timeUsed = (System.currentTimeMillis() - startTime) / 1000;
            Intent intent = new Intent(BoardActivity.this, ResultActivity.class);
            intent.putExtra("won", isGameWon);
            intent.putExtra("time", timeUsed);
            startActivity(intent);
            finish();
            return;
        }

        if (isFirstClick) {
            isFirstClick = false;
            placeMines(r, c);
            calculateAdjacentMines();
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
        }

        if (isDigMode) {
            revealCell(r, c);
        } else {
            toggleFlag(r, c);
        }

        updateBoardUI();
        checkWinCondition();
    }
    
    private void placeMines(int firstClickR, int firstClickC) {
        Random random = new Random();
        int minesPlaced = 0;
        while (minesPlaced < NUMBER_OF_MINES) {
            int r = random.nextInt(BOARD_SIZE);
            int c = random.nextInt(BOARD_SIZE);
            if ((r == firstClickR && c == firstClickC) || board[r][c].isMine()) {
                continue;
            }
            board[r][c].setMine(true);
            minesPlaced++;
        }
    }

    private void calculateAdjacentMines() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (!board[r][c].isMine()) {
                    int count = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = r + dr;
                            int nc = c + dc;
                            if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE && board[nr][nc].isMine()) {
                                count++;
                            }
                        }
                    }
                    board[r][c].setAdjacentMines(count);
                }
            }
        }
    }

    private void revealCell(int r, int c) {
        MineCell cell = board[r][c];
        if (cell.isRevealed() || cell.isFlagged()) {
            return;
        }

        cell.setRevealed(true);

        if (cell.isMine()) {
            endGame(false);
            return;
        }

        if (cell.getAdjacentMines() == 0) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE) {
                        revealCell(nr, nc);
                    }
                }
            }
        }
    }

    private void toggleFlag(int r, int c) {
        MineCell cell = board[r][c];
        if (cell.isRevealed()) {
            return;
        }
        if (cell.isFlagged()) {
            cell.setFlagged(false);
            minesToFlag++;
        } else {
            cell.setFlagged(true);
            minesToFlag--;
        }
        updateMineCount();
    }

    private void updateMineCount() {
        mineCountText.setText("Mines: " + minesToFlag);
    }

    private void checkWinCondition() {
        if (isGameOver) return;
        
        int unrevealedCount = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (!board[r][c].isRevealed()) {
                    unrevealedCount++;
                }
            }
        }
        if (unrevealedCount == NUMBER_OF_MINES) {
            endGame(true);
        }
    }

    private void updateBoardUI() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                MineCell cell = board[r][c];
                Button button = cellButtons[r][c];

                if (cell.isRevealed()) {
                    // Only disable the button if the game is NOT over.
                    if (!isGameOver) {
                        button.setEnabled(false);
                    }
                    
                    if (cell.isMine()) {
                        button.setText("M");
                        button.setBackgroundColor(Color.RED);
                    } else {
                        button.setText(cell.getAdjacentMines() > 0 ? String.valueOf(cell.getAdjacentMines()) : "");
                        button.setBackgroundColor(Color.LTGRAY);
                    }
                } else if (cell.isFlagged()) {
                    button.setText("F");
                } else {
                    button.setText("");
                    button.setEnabled(true); 
                }
            }
        }
    }
    
    private void endGame(boolean won) {
        isGameOver = true;
        isGameWon = won;
        timerHandler.removeCallbacks(timerRunnable);
        
        // Reveal all mines
        for(int r = 0; r < BOARD_SIZE; r++){
            for(int c = 0; c < BOARD_SIZE; c++){
                if(board[r][c].isMine()){
                    board[r][c].setRevealed(true);
                }
            }
        }
        updateBoardUI();

        Toast.makeText(this, won ? "You won! Click any cell to see results." : "You lost! Click any cell to see results.", Toast.LENGTH_LONG).show();
    }
}