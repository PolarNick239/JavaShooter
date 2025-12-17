import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

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

    public void update(double deltaTime, Vector2D mainPosition, Vector2D target, List<Obstacle> obstacles) {
        Vector2D oldPosition = position.copy();

        if (isMain) {
            // Главный солдат получает движение через GameManager
            // Здесь мы просто обновляем позицию на основе velocity
            position.x += velocity.x;
            position.y += velocity.y;

            // Проверка столкновений с препятствиями
            for (Obstacle obstacle : obstacles) {
                if (!obstacle.isActive()) continue;

                if (obstacle.collidesWithCircle(position.x, position.y, radius)) {
                    // Возвращаем на старую позицию
                    position = oldPosition;

                    // И отталкиваемся от препятствия
                    Vector2D push = new Vector2D(position.x - obstacle.getPosition().x,
                            position.y - obstacle.getPosition().y);
                    push.normalize();
                    velocity.x = push.x * 2;
                    velocity.y = push.y * 2;
                    break;
                }
            }

            // Трение
            velocity.x *= 0.9;
            velocity.y *= 0.9;
        } else {
            // Ведомые солдаты следуют за главным
            double angle = (index * 2 * Math.PI / 6);
            double targetX = mainPosition.x + Math.cos(angle) * followDistance;
            double targetY = mainPosition.y + Math.sin(angle) * followDistance;

            // Плавное движение к целевой позиции
            position.x += (targetX - position.x) * 0.1;
            position.y += (targetY - position.y) * 0.1;

            // Проверка столкновений с препятствиями для ведомых солдат
            for (Obstacle obstacle : obstacles) {
                if (!obstacle.isActive()) continue;

                if (obstacle.collidesWithCircle(position.x, position.y, radius)) {
                    // Отталкиваемся от препятствия
                    Vector2D push = new Vector2D(position.x - obstacle.getPosition().x,
                            position.y - obstacle.getPosition().y);
                    push.normalize();
                    position.x += push.x * 2;
                    position.y += push.y * 2;
                    break;
                }
            }
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

        // Маленький глаз для ориентации
        g2d.setColor(Color.BLACK);
        g2d.fillOval(
                (int)(position.x + radius * 0.5 - camera.getOffsetX()),
                (int)(position.y - radius * 0.3 - camera.getOffsetY()),
                (int)(radius * 0.6),
                (int)(radius * 0.6)
        );
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