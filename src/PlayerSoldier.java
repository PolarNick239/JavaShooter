import java.awt.*;
import java.awt.geom.Ellipse2D;

public class PlayerSoldier {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 10;
    private Color color;
    private boolean isMain; // Главный солдат (управляется игроком)
    private double followDistance; // Дистанция следования за главным солдатом
    private int index; // Индекс в отряде

    public PlayerSoldier(double x, double y, Color color, boolean isMain, int index) {
        this.position = new Vector2D(x, y);
        this.velocity = new Vector2D();
        this.color = color;
        this.isMain = isMain;
        this.index = index;
        this.followDistance = 30 + index * 15; // Солдаты следуют с разным отступом
    }

    public void update(double deltaTime, Vector2D mainPosition, Vector2D target) {
        if (isMain) {
            // Главный солдат следует за мышкой с инерцией
            Vector2D direction = new Vector2D(
                    target.x - position.x,
                    target.y - position.y
            );
            direction.normalize();
            direction.multiply(0.1);

            velocity.x += direction.x;
            velocity.y += direction.y;

            // Ограничение скорости
            double speed = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
            if (speed > 5) {
                velocity.x = (velocity.x / speed) * 5;
                velocity.y = (velocity.y / speed) * 5;
            }

            // Трение
            velocity.x *= 0.9;
            velocity.y *= 0.9;

            position.x += velocity.x;
            position.y += velocity.y;
        } else {
            // Ведомые солдаты следуют за главным
            // Позиция в круге вокруг главного солдата
            double angle = (index * 2 * Math.PI / 6); // Распределение по кругу
            double targetX = mainPosition.x + Math.cos(angle) * followDistance;
            double targetY = mainPosition.y + Math.sin(angle) * followDistance;

            // Плавное движение к целевой позиции
            position.x += (targetX - position.x) * 0.1;
            position.y += (targetY - position.y) * 0.1;
        }
    }

    public void draw(Graphics2D g2d, Camera camera) {
        Ellipse2D.Double circle = new Ellipse2D.Double(
                position.x - radius - camera.getOffsetX(),
                position.y - radius - camera.getOffsetY(),
                radius * 2,
                radius * 2
        );

        g2d.setColor(color);
        g2d.fill(circle);

        // Обводка для главного солдата
        if (isMain) {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(circle);
        }
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public void setVelocity(double vx, double vy) {
        this.velocity.x = vx;
        this.velocity.y = vy;
    }
}