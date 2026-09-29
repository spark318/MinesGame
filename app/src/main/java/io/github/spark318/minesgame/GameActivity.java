package io.github.spark318.minesgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.os.BundleCompat;

/**
 * Hosts the board. All rules live in {@link MinesweeperGame}; this class only renders game state
 * and forwards input to it.
 */
public class GameActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 10;
    private static final int NUMBER_OF_MINES = 12;

    private static final String KEY_GAME = "game";
    private static final String KEY_DIG_MODE = "dig_mode";
    private static final String KEY_ELAPSED_MS = "elapsed_ms";

    // Classic Minesweeper number colors, indexed by adjacent mine count.
    private static final int[] NUMBER_COLORS = {
            0, R.color.number_1, R.color.number_2, R.color.number_3, R.color.number_4,
            R.color.number_5, R.color.number_6, R.color.number_7, R.color.number_8
    };

    private MinesweeperGame game;
    private TextView[][] cellViews;

    private TextView mineCountText;
    private TextView timerText;
    private Button modeButton;
    private GridLayout boardGrid;

    private boolean isDigMode = true;

    // Elapsed time is accumulated across pauses so time spent in the background doesn't count.
    private long elapsedBeforeResumeMs = 0;
    private long resumedAtMs = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerText();
            timerHandler.postDelayed(this, 1000);
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
            if (game.isOver()) {
                showResult();
            } else {
                isDigMode = !isDigMode;
                updateStatusUI();
            }
        });
        findViewById(R.id.restart_button).setOnClickListener(v -> startNewGame());

        if (savedInstanceState != null) {
            game = BundleCompat.getSerializable(savedInstanceState, KEY_GAME, MinesweeperGame.class);
            isDigMode = savedInstanceState.getBoolean(KEY_DIG_MODE, true);
            elapsedBeforeResumeMs = savedInstanceState.getLong(KEY_ELAPSED_MS, 0);
        }
        if (game == null) {
            game = new MinesweeperGame(BOARD_SIZE, BOARD_SIZE, NUMBER_OF_MINES);
        }
        buildBoard();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumedAtMs = SystemClock.elapsedRealtime();
        if (game.getState() == MinesweeperGame.State.PLAYING) {
            timerHandler.post(timerRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (game.getState() == MinesweeperGame.State.PLAYING) {
            elapsedBeforeResumeMs = getElapsedMs();
            resumedAtMs = SystemClock.elapsedRealtime();
        }
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_GAME, game);
        outState.putBoolean(KEY_DIG_MODE, isDigMode);
        outState.putLong(KEY_ELAPSED_MS, getElapsedMs());
    }

    private void startNewGame() {
        timerHandler.removeCallbacks(timerRunnable);
        game = new MinesweeperGame(BOARD_SIZE, BOARD_SIZE, NUMBER_OF_MINES);
        isDigMode = true;
        elapsedBeforeResumeMs = 0;
        render();
    }

    private void buildBoard() {
        int rows = game.getRows();
        int cols = game.getCols();
        cellViews = new TextView[rows][cols];
        boardGrid.removeAllViews();
        boardGrid.setRowCount(rows);
        boardGrid.setColumnCount(cols);

        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1.5f, getResources().getDisplayMetrics());

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                TextView cellView = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(r, 1f),
                        GridLayout.spec(c, 1f)
                );
                params.width = 0;
                params.height = 0;
                params.setMargins(margin, margin, margin, margin);
                cellView.setLayoutParams(params);
                cellView.setGravity(Gravity.CENTER);
                cellView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                cellView.setTypeface(cellView.getTypeface(), android.graphics.Typeface.BOLD);

                final int finalR = r;
                final int finalC = c;
                cellView.setOnClickListener(v -> onCellTap(finalR, finalC));
                cellView.setOnLongClickListener(v -> onCellLongPress(finalR, finalC));

                boardGrid.addView(cellView);
                cellViews[r][c] = cellView;
            }
        }
    }

    private void onCellTap(int r, int c) {
        if (game.isOver()) {
            showResult();
            return;
        }
        MinesweeperGame.State before = game.getState();
        boolean changed = isDigMode ? game.reveal(r, c) : game.toggleFlag(r, c);
        if (changed) {
            onMoveMade(before);
        }
    }

    /** Long-press always flags, whichever mode is selected. */
    private boolean onCellLongPress(int r, int c) {
        MinesweeperGame.State before = game.getState();
        if (game.toggleFlag(r, c)) {
            onMoveMade(before);
            return true;
        }
        return false;
    }

    private void onMoveMade(MinesweeperGame.State before) {
        if (before == MinesweeperGame.State.NOT_STARTED) {
            resumedAtMs = SystemClock.elapsedRealtime();
            timerHandler.post(timerRunnable);
        }
        if (game.isOver()) {
            // getElapsedMs() stops counting once the game is over, so fold in the running segment here.
            if (before == MinesweeperGame.State.PLAYING) {
                elapsedBeforeResumeMs += SystemClock.elapsedRealtime() - resumedAtMs;
            }
            timerHandler.removeCallbacks(timerRunnable);
            boolean won = game.getState() == MinesweeperGame.State.WON;
            Toast.makeText(this, won ? R.string.toast_won : R.string.toast_lost, Toast.LENGTH_SHORT).show();
        }
        render();
    }

    private void showResult() {
        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra(GameResultActivity.EXTRA_WON, game.getState() == MinesweeperGame.State.WON);
        intent.putExtra(GameResultActivity.EXTRA_TIME_SECONDS, elapsedBeforeResumeMs / 1000);
        startActivity(intent);
        finish();
    }

    private long getElapsedMs() {
        if (game.getState() != MinesweeperGame.State.PLAYING) {
            return elapsedBeforeResumeMs;
        }
        return elapsedBeforeResumeMs + SystemClock.elapsedRealtime() - resumedAtMs;
    }

    private void render() {
        updateStatusUI();
        updateTimerText();
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                renderCell(game.getCell(r, c), cellViews[r][c]);
            }
        }
    }

    private void renderCell(Cell cell, TextView view) {
        if (cell.isRevealed() && cell.isMine()) {
            view.setText(R.string.cell_mine);
            view.setBackgroundResource(R.color.cell_mine);
        } else if (cell.isRevealed()) {
            int count = cell.getAdjacentMines();
            view.setText(count > 0 ? String.valueOf(count) : "");
            if (count > 0) {
                view.setTextColor(ContextCompat.getColor(this, NUMBER_COLORS[count]));
            }
            view.setBackgroundResource(R.color.cell_revealed);
        } else if (cell.isFlagged()) {
            view.setText(R.string.cell_flag);
            view.setBackgroundResource(R.color.cell_hidden);
        } else {
            view.setText("");
            view.setBackgroundResource(R.color.cell_hidden);
        }
    }

    private void updateStatusUI() {
        mineCountText.setText(getString(R.string.mines_remaining, game.getRemainingMineCount()));
        if (game.isOver()) {
            modeButton.setText(R.string.see_results);
        } else {
            modeButton.setText(isDigMode ? R.string.mode_dig : R.string.mode_flag);
        }
    }

    private void updateTimerText() {
        timerText.setText(getString(R.string.timer, getElapsedMs() / 1000));
    }
}
