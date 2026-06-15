# Idle Legends — Android Auto-Battler RPG

A starter Android Studio project for an idle/auto-battle RPG, in the style of
"auto-battler" mobile ads: bright green grass arena, cartoon round units,
floating damage numbers (DAMAGE / MISS / CRIT), gold coin pickups, and
wave-based enemy spawns. All art is drawn programmatically with Canvas
(circles, gradients, simple faces) — no copyrighted assets used.

## How to open
1. Open Android Studio (any recent version, e.g. 2023.x or later).
2. Choose "Open" and select the `IdleLegends` folder.
3. Let Gradle sync (it will download the Gradle 8.2 wrapper automatically).
4. Run on an emulator or device (minSdk 24 / Android 7.0+).

## Project structure
```
IdleLegends/
├── build.gradle              (project-level)
├── settings.gradle
└── app/
    ├── build.gradle           (app module — namespace, sdk versions)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/idlelegends/
        │   ├── MainActivity.java     – hosts the fullscreen GameView
        │   ├── GameView.java         – render + game loop (SurfaceView/Thread)
        │   ├── Unit.java             – hero/enemy stats & state
        │   └── FloatingEffect.java   – damage/coin/miss popups
        └── res/
            ├── values/styles.xml
            ├── values/strings.xml
            └── mipmap-mdpi/ic_launcher.png
```

## How the gameplay works
- Two heroes (an Archer and a Tank) auto-fight waves of goblins/imps.
- Every 5th wave spawns a tougher boss with more HP/attack.
- Units walk toward the nearest enemy, attack on a cooldown, and have a
  12% miss chance + 15% critical-hit chance (1.8x damage).
- Defeating an enemy drops gold (shown as a floating "+N" with a coin icon),
  which accumulates in the top-left HUD counter (formatted as K/M like "2.41M").
- Clearing all enemies on screen advances to the next wave automatically.

## Extending it
- **New units**: add entries to `Unit.Type` and a spawn line in
  `GameView.spawnWave()`.
- **Abilities/specials**: `GameView.onTouchEvent()` is wired up and ready —
  add a tap-to-cast special attack for the player's heroes there.
- **Persistence**: hook `MainActivity.onPause()`/`onResume()` to save/load
  `totalGold` and `wave` (e.g. via SharedPreferences) for true idle/offline
  progress.
- **Real art**: swap the `Paint`-based circle rendering in
  `GameView.drawUnit()` for `Bitmap`/sprite-sheet drawing once you have
  character art (PNG sprites in `res/drawable-nodpi/`).
- **Sound/music**: add `SoundPool` calls in `performAttack()` for hit/coin SFX.

## Notes
- This is a clean, original implementation — it does not use any assets,
  code, or branding from "Let's Go Legends!" or any other existing game.
  Visual style (green arena, round cartoon units, bold outlined damage
  numbers, coin pickups) is a common genre convention for idle/auto-battler
  mobile games, recreated here from scratch.
