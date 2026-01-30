import java.awt.*;
import java.awt.geom.Ellipse2D;

public class TankBoss implements Boss {
    private Vector2D position;
    private Vector2D velocity = new Vector2D();
    private double radius = 36;
    private int health;
    private int maxHealth;
    private double speed;

    private double shellCooldown = 2.6;
    private double shellTimer = 0;
    private double chargeTimer = 0;
    private boolean charging = false;

    public TankBoss(double x, double y, int maxHealth, double speed) {
        this.position = new Vector2D(x, y);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
    }

    @Override
    public void update(double deltaTime, Vector2D playerPos, GameManager game) {
        if (!isAlive()) return;

        shellTimer -= deltaTime;
        if (!charging && shellTimer <= 0) {
            charging = true;
            chargeTimer = 0.6;
        }

        if (charging) {
            chargeTimer -= deltaTime;
            if (chargeTimer <= 0) {
                charging = false;
                shellTimer = shellCooldown;
                fireShell(playerPos, game);
            }
        } else {
            moveToward(playerPos);
        }
    }

    private void moveToward(Vector2D playerPos) {
        Vector2D dir = new Vector2D(playerPos.x - position.x, playerPos.y - position.y);
        double dist = dir.distanceTo(new Vector2D(0, 0));
        if (dist > 1) {
            dir.normalize();
            velocity.x = dir.x * speed;
            velocity.y = dir.y * speed;
            position.x += velocity.x;
            position.y += velocity.y;
        }
    }

    private void fireShell(Vector2D playerPos, GameManager game) {
        double speed = 4.0;
        int damage = 60;
        double ttl = 2.5;
        double radius = 110;
        BossProjectile shell = BossProjectile.createShell(
                position.x, position.y,
                playerPos.x, playerPos.y,
                speed, damage, ttl, radius
        );
        game.addBossProjectile(shell);
        SoundManager.playBossShot();
    }

    @Override
    public void draw(Graphics2D g2d, Camera camera) {
        if (!isAlive()) return;

        double ox = camera.getOffsetX();
        double oy = camera.getOffsetY();

        double bodyW = radius * 2.4;
        double bodyH = radius * 1.4;
        double bodyX = position.x - bodyW / 2 - ox;
        double bodyY = position.y - bodyH / 2 - oy;

        double treadW = bodyW * 1.05;
        double treadH = bodyH * 0.35;
        double treadX = position.x - treadW / 2 - ox;
        double treadY = position.y + bodyH / 2 - treadH / 2 - oy;

        double turretW = bodyW * 0.45;
        double turretH = bodyH * 0.5;
        double turretX = position.x - turretW / 2 - ox;
        double turretY = position.y - bodyH * 0.45 - oy;

        double barrelW = bodyW * 0.6;
        double barrelH = bodyH * 0.15;
        double barrelX = position.x + turretW / 2 - ox;
        double barrelY = position.y - bodyH * 0.2 - barrelH / 2 - oy;

        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRoundRect((int) treadX, (int) treadY, (int) treadW, (int) treadH, 10, 10);

        g2d.setColor(new Color(120, 120, 120));
        g2d.fillRoundRect((int) bodyX, (int) bodyY, (int) bodyW, (int) bodyH, 12, 12);

        g2d.setColor(charging ? new Color(180, 90, 60) : new Color(90, 90, 90));
        g2d.fillRoundRect((int) turretX, (int) turretY, (int) turretW, (int) turretH, 10, 10);

        g2d.setColor(new Color(70, 70, 70));
        g2d.fillRoundRect((int) barrelX, (int) barrelY, (int) barrelW, (int) barrelH, 6, 6);

        if (charging) {
            g2d.setColor(new Color(255, 120, 80, 120));
            g2d.fillOval((int) (turretX - 6), (int) (turretY - 6),
                    (int) (turretW + 12), (int) (turretH + 12));
        }

        drawHealthBar(g2d, camera);
    }

    private void drawHealthBar(Graphics2D g2d, Camera camera) {
        double barWidth = 80;
        double barHeight = 8;
        double healthRatio = maxHealth > 0 ? (health / (double) maxHealth) : 0;
        double x = position.x - barWidth / 2 - camera.getOffsetX();
        double y = position.y - radius - 16 - camera.getOffsetY();

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect((int) x, (int) y, (int) barWidth, (int) barHeight);
        g2d.setColor(new Color(200, 60, 60));
        g2d.fillRect((int) x, (int) y, (int) (barWidth * healthRatio), (int) barHeight);
    }

    @Override
    public boolean isAlive() {
        return health > 0;
    }

    @Override
    public Vector2D getPosition() {
        return position;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    @Override
    public String getName() {
        return "Tank";
    }
}
