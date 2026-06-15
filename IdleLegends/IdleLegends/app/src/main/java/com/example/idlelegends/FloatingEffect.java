package com.example.idlelegends;

/**
 * A short-lived floating popup: damage number, "MISS" text, or a coin reward.
 */
public class FloatingEffect {

    public enum Kind { DAMAGE, MISS, COIN, CRIT }

    public Kind kind;
    public float x, y;
    public float vy = -60f;     // upward drift speed (px/sec)
    public float life;          // remaining lifetime in seconds
    public float maxLife;
    public String text;

    public FloatingEffect(Kind kind, float x, float y, String text, float life) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.text = text;
        this.life = life;
        this.maxLife = life;
    }

    /** Returns false once the effect should be removed. */
    public boolean update(float dt) {
        y += vy * dt;
        life -= dt;
        return life > 0;
    }

    public float alpha() {
        return Math.max(0f, life / maxLife);
    }
}
