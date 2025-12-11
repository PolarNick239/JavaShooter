import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Bullet {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 4;
    private Color color;
    private int damage = 25;
    private boolean isActive = true;

    public Bullet(double startX, double startY, double targetX, double targetY, Color color) {
        this.position = new Vector2D(startX, startY);
        this.color = color;

        // Направление к цели
        velocity = new Vector2D(targetX - startX, targetY - startY);
        velocity.normalize();
        velocity.multiply(10); // Скорость пули
    }

    public void update() {
        position.x += velocity.x;
        position.y += velocity.y;
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isActive) return;

        Ellipse2D.Double circle = new Ellipse2D.Double(
                position.x - radius - camera.getOffsetX(),
                position.y - radius - camera.getOffsetY(),
                radius * 2,
                radius * 2
        );

        g2d.setColor(color);
        g2d.fill(circle);

        // Эффект свечения
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        g2d.fill(new Ellipse2D.Double(
                position.x - radius * 1.5 - camera.getOffsetX(),
                position.y - radius * 1.5 - camera.getOffsetY(),
                radius * 3,
                radius * 3
        ));
    }

    public boolean checkCollision(Enemy enemy) {
        if (!isActive) return false;

        double distance = position.distanceTo(enemy.getPosition());
        if (distance < radius + enemy.getRadius()) {
            isActive = false;
            return true;
        }
        return false;
    }

    public boolean isActive() {
        return isActive;
    }

    public Vector2D getPosition() {
        return position;
    }

    public int getDamage() {
        return damage;
    }
}