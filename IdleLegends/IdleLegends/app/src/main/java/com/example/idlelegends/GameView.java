package com.example.idlelegends;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Core battlefield rendering + auto-battle simulation loop.
 * Visual style: bright green grass arena, rounded cartoon units,
 * bold outlined numbers for damage/gold, similar mood to typical
 * idle-RPG "auto battler" mobile ads.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread gameThread;
    private volatile boolean running = false;

    private final List<Unit> units = new ArrayList<>();
    private final List<FloatingEffect> effects = new ArrayList<>();
    private final Random rng = new Random();

    private long totalGold = 0;
    private int wave = 1;

    // Paints (created once, reused every frame for performance)
    private final Paint grassPaint = new Paint();
    private final Paint pathPaint = new Paint();
    private final Paint heroArcherPaint = new Paint();
    private final Paint heroTankPaint = new Paint();
    private final Paint goblinPaint = new Paint();
    private final Paint impPaint = new Paint();
    private final Paint bossPaint = new Paint();
    private final Paint outlinePaint = new Paint();
    private final Paint hpBackPaint = new Paint();
    private final Paint hpFrontPaint = new Paint();
    private final Paint textFillPaint = new Paint();
    private final Paint textStrokePaint = new Paint();
    private final Paint coinPaint = new Paint();
    private final Paint hitFlashPaint = new Paint();
    private final Paint uiPaint = new Paint();

    private GameCallback callback;

    public interface GameCallback {
        void onGoldChanged(long totalGold);
        void onWaveChanged(int wave);
    }

    public void setCallback(GameCallback cb) {
        this.callback = cb;
    }

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setupPaints();
    }

    private void setupPaints() {
        grassPaint.setColor(Color.parseColor("#5CC23A"));

        pathPaint.setColor(Color.parseColor("#D9B872"));

        heroArcherPaint.setColor(Color.parseColor("#F2C14E"));
        heroTankPaint.setColor(Color.parseColor("#3E8EDE"));
        goblinPaint.setColor(Color.parseColor("#8FD14F"));
        impPaint.setColor(Color.parseColor("#E0473C"));
        bossPaint.setColor(Color.parseColor("#7A4A2B"));

        outlinePaint.setColor(Color.parseColor("#2B2B2B"));
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(4f);
        outlinePaint.setAntiAlias(true);

        hpBackPaint.setColor(Color.parseColor("#444444"));
        hpFrontPaint.setColor(Color.parseColor("#4CD964"));

        textFillPaint.setColor(Color.WHITE);
        textFillPaint.setAntiAlias(true);
        textFillPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        textFillPaint.setTextAlign(Paint.Align.CENTER);

        textStrokePaint.setColor(Color.parseColor("#2B2B2B"));
        textStrokePaint.setStyle(Paint.Style.STROKE);
        textStrokePaint.setStrokeWidth(6f);
        textStrokePaint.setAntiAlias(true);
        textStrokePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        textStrokePaint.setTextAlign(Paint.Align.CENTER);

        coinPaint.setColor(Color.parseColor("#FFD23F"));
        coinPaint.setAntiAlias(true);

        hitFlashPaint.setColor(Color.parseColor("#FFFFFF"));

        uiPaint.setColor(Color.WHITE);
        uiPaint.setAntiAlias(true);
        uiPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        uiPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        spawnWave();
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException ignored) {}
    }

    // --------------------------------------------------------------
    // Wave / unit setup
    // --------------------------------------------------------------

    private void spawnWave() {
        units.clear();
        int w = getWidth() > 0 ? getWidth() : 1080;
        int h = getHeight() > 0 ? getHeight() : 1920;

        // Heroes (player party) bottom-center
        units.add(new Unit(Unit.Type.HERO_ARCHER, w * 0.40f, h * 0.55f, 320, 18, 420f, 0.8f));
        units.add(new Unit(Unit.Type.HERO_TANK,   w * 0.25f, h * 0.62f, 600, 12, 110f, 1.1f));

        // Enemy goblins, scale slightly with wave
        int goblinCount = 3 + Math.min(wave - 1, 3);
        int goblinHp = 80 + wave * 15;
        int goblinAtk = 6 + wave;

        for (int i = 0; i < goblinCount; i++) {
            float gx = w * (0.55f + 0.12f * (i % 3));
            float gy = h * (0.30f + 0.10f * (i / 3));
            units.add(new Unit(Unit.Type.ENEMY_GOBLIN, gx, gy, goblinHp, goblinAtk, 100f, 1.0f));
        }

        // An imp on early waves
        if (wave % 2 == 1) {
            units.add(new Unit(Unit.Type.ENEMY_IMP, w * 0.30f, h * 0.32f, 60 + wave * 10, 9 + wave, 90f, 0.9f));
        }

        // Boss every 5th wave
        if (wave % 5 == 0) {
            units.add(new Unit(Unit.Type.ENEMY_BOSS, w * 0.65f, h * 0.45f, 1200 + wave * 100, 25 + wave * 2, 130f, 1.3f));
        }
    }

    // --------------------------------------------------------------
    // Game loop
    // --------------------------------------------------------------

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastTime) / 1_000_000_000f;
            dt = Math.min(dt, 0.05f); // clamp for stability
            lastTime = now;

            update(dt);

            Canvas canvas = null;
            SurfaceHolder holder = getHolder();
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // Target ~60fps
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {}
        }
    }

    private void update(float dt) {
        List<Unit> heroes = new ArrayList<>();
        List<Unit> enemies = new ArrayList<>();
        for (Unit u : units) {
            if (!u.alive) continue;
            if (u.isHero()) heroes.add(u); else enemies.add(u);
        }

        // Heroes act
        for (Unit hero : heroes) {
            updateUnit(hero, enemies, dt);
        }
        // Enemies act
        for (Unit enemy : enemies) {
            updateUnit(enemy, heroes, dt);
        }

        // Update floating effects
        Iterator<FloatingEffect> it = effects.iterator();
        while (it.hasNext()) {
            FloatingEffect fx = it.next();
            if (!fx.update(dt)) it.remove();
        }

        // Decay hit flash
        for (Unit u : units) {
            if (u.hitFlashTimer > 0) u.hitFlashTimer -= dt;
        }

        // Check wave completion
        boolean enemiesAlive = false;
        for (Unit u : units) {
            if (!u.isHero() && u.alive) enemiesAlive = true;
        }
        if (!enemiesAlive) {
            wave++;
            // small delay handled implicitly by next frame spawning
            spawnWave();
            if (callback != null) callback.onWaveChanged(wave);
        }

        // Remove dead enemies after a short period (instant for simplicity)
        units.removeIf(u -> !u.alive && !u.isHero());
    }

    private Unit findNearestTarget(Unit self, List<Unit> targets) {
        Unit nearest = null;
        float best = Float.MAX_VALUE;
        for (Unit t : targets) {
            if (!t.alive) continue;
            float d = self.distanceTo(t);
            if (d < best) {
                best = d;
                nearest = t;
            }
        }
        return nearest;
    }

    private void updateUnit(Unit self, List<Unit> targets, float dt) {
        Unit target = findNearestTarget(self, targets);
        if (target == null) return;

        float dist = self.distanceTo(target);

        if (dist > self.attackRange) {
            // Move toward target
            float dx = target.x - self.x;
            float dy = target.y - self.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float speed = 90f; // px/sec
            if (len > 1f) {
                self.x += (dx / len) * speed * dt;
                self.y += (dy / len) * speed * dt;
            }
        } else {
            // In range: attack on cooldown
            self.cooldownTimer -= dt;
            if (self.cooldownTimer <= 0f) {
                self.cooldownTimer = self.attackCooldown;
                performAttack(self, target);
            }
        }
    }

    private void performAttack(Unit attacker, Unit target) {
        // 12% miss chance
        boolean miss = rng.nextFloat() < 0.12f;
        if (miss) {
            effects.add(new FloatingEffect(FloatingEffect.Kind.MISS, target.x, target.y - 60, "MISS", 0.8f));
            return;
        }

        boolean crit = rng.nextFloat() < 0.15f;
        int dmg = attacker.attack;
        if (crit) dmg = (int) (dmg * 1.8f);

        target.takeDamage(dmg);

        FloatingEffect.Kind kind = crit ? FloatingEffect.Kind.CRIT : FloatingEffect.Kind.DAMAGE;
        String txt = "-" + dmg;
        effects.add(new FloatingEffect(kind, target.x, target.y - 60, txt, 0.8f));

        if (!target.alive && !target.isHero()) {
            long reward = 5L + (long) (target.maxHp / 4);
            totalGold += reward;
            effects.add(new FloatingEffect(FloatingEffect.Kind.COIN, target.x, target.y - 20, "+" + reward, 1.0f));
            if (callback != null) callback.onGoldChanged(totalGold);
        }
    }

    // --------------------------------------------------------------
    // Drawing
    // --------------------------------------------------------------

    private void draw(Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        drawBackground(canvas, w, h);

        for (Unit u : units) {
            drawUnit(canvas, u);
        }

        for (FloatingEffect fx : effects) {
            drawEffect(canvas, fx);
        }

        drawHud(canvas, w, h);
    }

    private void drawBackground(Canvas canvas, int w, int h) {
        // Grass gradient for depth
        Shader sky = new LinearGradient(0, 0, 0, h, Color.parseColor("#79D94C"), Color.parseColor("#4FAE2E"), Shader.TileMode.CLAMP);
        grassPaint.setShader(sky);
        canvas.drawRect(0, 0, w, h, grassPaint);
        grassPaint.setShader(null);

        // Diagonal dirt path
        canvas.save();
        canvas.rotate(-18f, w * 0.1f, h * 0.5f);
        canvas.drawRect(-w, h * 0.45f, w * 2, h * 0.62f, pathPaint);
        canvas.restore();

        // Subtle grass texture circles
        Paint texPaint = new Paint();
        texPaint.setColor(Color.parseColor("#66B83A"));
        texPaint.setAlpha(80);
        for (int i = 0; i < 18; i++) {
            float cx = (i * 137) % w;
            float cy = (i * 251) % h;
            canvas.drawCircle(cx, cy, 14, texPaint);
        }
    }

    private void drawUnit(Canvas canvas, Unit u) {
        float r = 50f * u.scale;

        Paint body;
        switch (u.type) {
            case HERO_ARCHER: body = heroArcherPaint; break;
            case HERO_TANK: body = heroTankPaint; break;
            case ENEMY_IMP: body = impPaint; break;
            case ENEMY_BOSS: body = bossPaint; r *= 1.6f; break;
            default: body = goblinPaint; break;
        }

        // Shadow
        Paint shadow = new Paint();
        shadow.setColor(Color.parseColor("#33000000"));
        canvas.drawOval(u.x - r * 0.9f, u.y + r * 0.6f, u.x + r * 0.9f, u.y + r * 1.1f, shadow);

        // Body circle
        canvas.drawCircle(u.x, u.y, r, body);
        canvas.drawCircle(u.x, u.y, r, outlinePaint);

        // Hit flash overlay
        if (u.hitFlashTimer > 0) {
            hitFlashPaint.setAlpha((int) (200 * (u.hitFlashTimer / 0.12f)));
            canvas.drawCircle(u.x, u.y, r, hitFlashPaint);
        }

        // Simple "face" - eyes
        Paint eyePaint = new Paint();
        eyePaint.setColor(Color.WHITE);
        eyePaint.setAntiAlias(true);
        float eyeOffset = r * 0.35f;
        float eyeR = r * 0.18f;
        canvas.drawCircle(u.x - eyeOffset, u.y - r * 0.1f, eyeR, eyePaint);
        canvas.drawCircle(u.x + eyeOffset, u.y - r * 0.1f, eyeR, eyePaint);

        Paint pupilPaint = new Paint();
        pupilPaint.setColor(Color.BLACK);
        pupilPaint.setAntiAlias(true);
        float pupilR = eyeR * 0.5f;
        canvas.drawCircle(u.x - eyeOffset, u.y - r * 0.1f, pupilR, pupilPaint);
        canvas.drawCircle(u.x + eyeOffset, u.y - r * 0.1f, pupilR, pupilPaint);

        // HP bar
        float barW = r * 2.2f;
        float barH = 10f;
        float bx = u.x - barW / 2f;
        float by = u.y - r - 24f;
        canvas.drawRoundRect(bx, by, bx + barW, by + barH, 6, 6, hpBackPaint);
        float pct = Math.max(0f, (float) u.hp / u.maxHp);
        canvas.drawRoundRect(bx, by, bx + barW * pct, by + barH, 6, 6, hpFrontPaint);
        canvas.drawRoundRect(bx, by, bx + barW, by + barH, 6, 6, outlinePaintThin());
    }

    private Paint outlinePaintThin() {
        Paint p = new Paint(outlinePaint);
        p.setStrokeWidth(2f);
        return p;
    }

    private void drawEffect(Canvas canvas, FloatingEffect fx) {
        int alpha = (int) (255 * fx.alpha());
        textFillPaint.setAlpha(alpha);
        textStrokePaint.setAlpha(alpha);

        float textSize;
        int fillColor;
        switch (fx.kind) {
            case CRIT:
                textSize = 64f;
                fillColor = Color.parseColor("#FFD23F");
                break;
            case COIN:
                textSize = 44f;
                fillColor = Color.parseColor("#FFE27A");
                break;
            case MISS:
                textSize = 44f;
                fillColor = Color.parseColor("#CCCCCC");
                break;
            default:
                textSize = 50f;
                fillColor = Color.WHITE;
        }

        textFillPaint.setColor(fillColor);
        textFillPaint.setAlpha(alpha);
        textFillPaint.setTextSize(textSize);
        textStrokePaint.setTextSize(textSize);

        // Coin icon for coin popups
        if (fx.kind == FloatingEffect.Kind.COIN) {
            coinPaint.setAlpha(alpha);
            canvas.drawCircle(fx.x - 38, fx.y, 14, coinPaint);
            canvas.drawCircle(fx.x - 38, fx.y, 14, outlinePaintThin());
        }

        canvas.drawText(fx.text, fx.x, fx.y, textStrokePaint);
        canvas.drawText(fx.text, fx.x, fx.y, textFillPaint);
    }

    private void drawHud(Canvas canvas, int w, int h) {
        uiPaint.setTextSize(56f);
        uiPaint.setColor(Color.WHITE);

        // Gold counter top-left with coin icon
        canvas.drawCircle(48, 70, 22, coinPaint);
        canvas.drawCircle(48, 70, 22, outlinePaintThin());
        canvas.drawText(formatGold(totalGold), 90, 88, uiPaint);

        // Wave counter top-right
        String waveTxt = "Wave " + wave;
        float waveWidth = uiPaint.measureText(waveTxt);
        canvas.drawText(waveTxt, w - waveWidth - 30, 88, uiPaint);
    }

    private String formatGold(long gold) {
        if (gold >= 1_000_000) {
            return String.format("%.2fM", gold / 1_000_000.0);
        } else if (gold >= 1_000) {
            return String.format("%.1fK", gold / 1_000.0);
        }
        return String.valueOf(gold);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Future: tap to trigger hero special ability
        return true;
    }
}
