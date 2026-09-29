package io.github.spark318.minesgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameResultActivity extends AppCompatActivity {

    static final String EXTRA_WON = "won";
    static final String EXTRA_TIME_SECONDS = "time_seconds";

    private static final String PREFS_NAME = "stats";
    private static final String KEY_BEST_TIME = "best_time_seconds";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView resultText = findViewById(R.id.result_text);
        TextView timeUsedText = findViewById(R.id.time_used_text);
        TextView bestTimeText = findViewById(R.id.best_time_text);
        Button playAgainButton = findViewById(R.id.play_again_button);

        Intent intent = getIntent();
        boolean won = intent.getBooleanExtra(EXTRA_WON, false);
        long timeUsed = intent.getLongExtra(EXTRA_TIME_SECONDS, 0);

        resultText.setText(won ? R.string.result_won : R.string.result_lost);
        timeUsedText.setText(getString(R.string.result_time, timeUsed));

        long bestTime = recordBestTime(won, timeUsed);
        if (bestTime >= 0) {
            bestTimeText.setText(getString(R.string.result_best_time, bestTime));
        }

        playAgainButton.setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });
    }

    /**
     * Saves the time if it's a new best win. Safe to call again on rotation since an equal time
     * isn't a new best.
     *
     * @return the best winning time in seconds, or -1 if the player has never won
     */
    private long recordBestTime(boolean won, long timeUsed) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long best = prefs.getLong(KEY_BEST_TIME, -1);
        if (won && (best < 0 || timeUsed < best)) {
            best = timeUsed;
            prefs.edit().putLong(KEY_BEST_TIME, best).apply();
        }
        return best;
    }
}
