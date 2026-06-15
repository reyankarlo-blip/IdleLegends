package com.example.idlelegends;

/**
 * Represents a single combat unit (hero or enemy) on the battlefield.
 */
public class Unit {

    public enum Type { HERO_ARCHER, HERO_TANK, ENEMY_GOBLIN, ENEMY_IMP, ENEMY_BOSS }

    public float x, y;          // position on battlefield
    public float targetX, targetY;
    public int hp, maxHp;
    public int attack;
    public float attackRange;
    public float attackCooldown;   // seconds between attacks
    public float cooldownTimer;
    public boolean alive = true;
    public Type type;
    public float hitFlashTimer = 0f; // visual flash when hit
    public float scale = 1f;

    public Unit(Type type, float x, float y, int hp, int attack, float range, float cooldown) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.hp = hp;
        this.maxHp = hp;
        this.attack = attack;
        this.attackRange = range;
        this.attackCooldown = cooldown;
        this.cooldownTimer = 0f;
    }

    public boolean isHero() {
        return type == Type.HERO_ARCHER || type == Type.HERO_TANK;
    }

    public float distanceTo(Unit other) {
        float dx = other.x - x;
        float dy = other.y - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void takeDamage(int dmg) {
        hp -= dmg;
        hitFlashTimer = 0.12f;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }
}
