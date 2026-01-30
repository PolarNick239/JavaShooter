import java.awt.*;
import java.awt.geom.Ellipse2D;

public class HelicopterBoss implements Boss {
    private enum State {
        HOVER,
        DASH
    }

    private Vector2D position;
    private double radius = 28;
    private int health;
    private int maxHealth;
    private State state = State.HOVER;
    private double hoverTime = 0;
    private double dashCooldown = 6.0;
    private double dashTimer = 0;
    private int dashDirection = 1;
    private double dashSpeed = 12.0;
    private double dropTimer = 0;
    private int dropsLeft = 0;

    private double burstCooldown = 1.6;
    private double burstTimer = 0;
    private double fireTimer = 0;
    private boolean bursting = false;

    public HelicopterBoss(double x, double y, int maxHealth) {
        this.position = new Vector2D(x, y);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    @Override
    public void update(double deltaTime, Vector2D playerPos, GameManager game) {
        if (!isAlive()) return;

        hoverTime += deltaTime;

        if (state == State.HOVER) {
            double baseY = 90 + Math.sin(hoverTime * 1.2) * 10;
            double baseX = game.getWorldWidth() / 2.0 + Math.sin(hoverTime * 0.6) * (game.getWorldWidth() * 0.3);
            position.x = baseX;
            position.y = baseY;

            updateBurst(deltaTime, playerPos, game);

            dashCooldown -= deltaTime;
            if (dashCooldown <= 0) {
                startDash(game);
            }
        } else if (state == State.DASH) {
            position.x += dashSpeed * dashDirection;
            dropTimer -= deltaTime;
            if (dropTimer <= 0 && dropsLeft > 0) {
                dropTimer = 0.3;
                dropsLeft--;
                game.spawnEnemyAt(position.x, position.y + 30);
                SoundManager.playDrop();
            }

            if ((dashDirection > 0 && position.x > game.getWorldWidth() + 80) ||
                    (dashDirection < 0 && position.x < -80)) {
                state = State.HOVER;
                dashCooldown = 6.0;
            }
        }
    }

    private void updateBurst(double deltaTime, Vector2D playerPos, GameManager game) {
        if (bursting) {
            fireTimer -= deltaTime;
            if (fireTimer <= 0) {
                fireTimer = 0.18;
                fireAt(playerPos, game);
            }
            burstTimer -= deltaTime;
            if (burstTimer <= 0) {
                bursting = false;
                burstCooldown = 1.6;
            }
        } else {
            burstCooldown -= deltaTime;
            if (burstCooldown <= 0) {
                bursting = true;
                burstTimer = 1.0;
                fireTimer = 0;
            }
        }
    }

    private void fireAt(Vector2D playerPos, GameManager game) {
        double spread = (Math.random() - 0.5) * 30;
        double targetX = playerPos.x + spread;
        double targetY = playerPos.y + (Math.random() - 0.5) * 20;
        BossProjectile bullet = BossProjectile.createBullet(
                position.x, position.y,
                targetX, targetY,
                8.5, 8, 2.5
        );
        game.addBossProjectile(bullet);
        SoundManager.playBossShot();
    }

    private void startDash(GameManager game) {
        state = State.DASH;
        dashDirection = Math.random() < 0.5 ? 1 : -1;
        position.x = dashDirection > 0 ? -80 : game.getWorldWidth() + 80;
        position.y = 70 + Math.random() * 60;
        dropsLeft = 3 + (int) Math.floor(Math.random() * 3);
        dropTimer = 0.2;
        SoundManager.playBossDash();
    }

    @Override
    public void draw(Graphics2D g2d, Camera camera) {
        if (!isAlive()) return;

        double ox = camera.getOffsetX();
        double oy = camera.getOffsetY();

        double bodyW = radius * 2.2;
        double bodyH = radius * 1.2;
        double bodyX = position.x - bodyW / 2 - ox;
        double bodyY = position.y - bodyH / 2 - oy;

        double cockpitW = bodyW * 0.55;
        double cockpitH = bodyH * 0.75;
        double cockpitX = bodyX + bodyW * 0.15;
        double cockpitY = bodyY + bodyH * 0.05;

        double tailW = bodyW * 1.4;
        double tailH = bodyH * 0.25;
        double tailX = position.x + bodyW / 2 - ox;
        double tailY = position.y - tailH / 2 - oy;

        g2d.setColor(new Color(70, 110, 170));
        g2d.fillRoundRect((int) bodyX, (int) bodyY, (int) bodyW, (int) bodyH, 16, 16);

        g2d.setColor(new Color(110, 160, 220));
        g2d.fillOval((int) cockpitX, (int) cockpitY, (int) cockpitW, (int) cockpitH);

        g2d.setColor(new Color(50, 80, 130));
        g2d.fillRoundRect((int) tailX, (int) tailY, (int) tailW, (int) tailH, 8, 8);

        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(40, 60, 120));
        int rotorY = (int) (position.y - bodyH / 2 - 8 - oy);
        g2d.drawLine((int) (position.x - bodyW * 0.9 - ox), rotorY,
                (int) (position.x + bodyW * 0.9 - ox), rotorY);
        g2d.drawLine((int) (position.x - ox), rotorY - 6,
                (int) (position.x - ox), rotorY + 6);

        int tailRotorX = (int) (tailX + tailW - 2);
        int tailRotorY = (int) (tailY + tailH / 2);
        g2d.drawLine(tailRotorX - 6, tailRotorY, tailRotorX + 6, tailRotorY);
        g2d.drawLine(tailRotorX, tailRotorY - 6, tailRotorX, tailRotorY + 6);

        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine((int) (position.x - bodyW * 0.5 - ox), (int) (position.y + bodyH / 2 - oy),
                (int) (position.x - bodyW * 0.2 - ox), (int) (position.y + bodyH / 2 + 8 - oy));
        g2d.drawLine((int) (position.x + bodyW * 0.2 - ox), (int) (position.y + bodyH / 2 - oy),
                (int) (position.x + bodyW * 0.5 - ox), (int) (position.y + bodyH / 2 + 8 - oy));

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
        g2d.setColor(new Color(80, 180, 220));
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
        return "Helicopter";
    }
}
