package com.example.minesgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView timeUsedText = findViewById(R.id.time_used_text);
        TextView resultText = findViewById(R.id.result_text);
        Button playAgainButton = findViewById(R.id.play_again_button);

        Intent intent = getIntent();
        boolean won = intent.getBooleanExtra("won", false);
        long timeUsed = intent.getLongExtra("time", 0);

        timeUsedText.setText("Time: " + timeUsed + "s");
        resultText.setText(won ? "You Won!" : "You Lost!");

        playAgainButton.setOnClickListener(v -> {
            Intent backToBoard = new Intent(ResultActivity.this, BoardActivity.class);
            startActivity(backToBoard);
            finish();
        });
    }
} 