import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Bonus {
    public enum BonusType {
        HEALTH,
        NEW_SOLDIER
    }

    private Vector2D position;
    private Vector2D velocity;
    private double radius = 8;
    private BonusType type;
    private Color color;
    private boolean isActive = true;
    private double floatOffset = 0;

    public Bonus(double x, double y) {
        this.position = new Vector2D(x, y);

        // Случайный тип бонуса
        if (Math.random() < 0.3) {
            type = BonusType.HEALTH;
            color = new Color(0, 255, 100); // Зеленый для здоровья
        } else {
            type = BonusType.NEW_SOLDIER;
            color = new Color(255, 200, 0); // Желтый для нового солдата
        }

        // Небольшая начальная скорость
        velocity = new Vector2D(
                (Math.random() - 0.5) * 3,
                (Math.random() - 0.5) * 3
        );
    }

    public void update() {
        if (!isActive) return;

        position.x += velocity.x;
        position.y += velocity.y;

        // Замедление
        velocity.x *= 0.98;
        velocity.y *= 0.98;

        // Плавное плавание
        floatOffset += 0.1;
        position.y += Math.sin(floatOffset) * 0.5;
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isActive) return;

        // Основной круг
        Ellipse2D.Double circle = new Ellipse2D.Double(
                position.x - radius - camera.getOffsetX(),
                position.y - radius - camera.getOffsetY(),
                radius * 2,
                radius * 2
        );

        g2d.setColor(color);
        g2d.fill(circle);

        // Внутренний круг
        g2d.setColor(Color.WHITE);
        g2d.fill(new Ellipse2D.Double(
                position.x - radius/2 - camera.getOffsetX(),
                position.y - radius/2 - camera.getOffsetY(),
                radius,
                radius
        ));

        // Символ бонуса
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String symbol = type == BonusType.HEALTH ? "+" : "S";
        g2d.drawString(symbol,
                (int)(position.x - 3 - camera.getOffsetX()),
                (int)(position.y + 4 - camera.getOffsetY())
        );
    }

    public boolean checkCollision(Vector2D playerPos, double playerRadius) {
        if (!isActive) return false;

        double distance = position.distanceTo(playerPos);
        if (distance < radius + playerRadius) {
            isActive = false;
            return true;
        }
        return false;
    }

    public BonusType getType() {
        return type;
    }

    public boolean isActive() {
        return isActive;
    }
}