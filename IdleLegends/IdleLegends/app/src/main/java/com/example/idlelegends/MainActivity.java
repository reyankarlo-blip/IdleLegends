package com.example.idlelegends;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity implements GameView.GameCallback {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive battlefield
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        gameView = new GameView(this);
        gameView.setCallback(this);

        FrameLayout root = new FrameLayout(this);
        root.addView(gameView);
        setContentView(root);
    }

    @Override
    public void onGoldChanged(long totalGold) {
        // HUD is drawn directly by GameView; hook left for future UI (toasts, save, etc.)
    }

    @Override
    public void onWaveChanged(int wave) {
        // Could trigger sounds/animations on wave clear
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
