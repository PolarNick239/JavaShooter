import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class Enemy {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 15;
    private Color color;
    private int health = 100;
    private boolean isAlive = true;
    private double speed;
    private Vector2D avoidanceForce = new Vector2D();
    private double avoidanceTimer = 0;

    public Enemy(double x, double y, Vector2D target) {
        this.position = new Vector2D(x, y);
        this.color = new Color(200, 50, 50);
        this.speed = 1 + Math.random() * 2;

        // Начальное направление к цели
        velocity = new Vector2D(target.x - x, target.y - y);
        velocity.normalize();
        velocity.multiply(speed);
    }

    public void update(double deltaTime, Vector2D target, List<Obstacle> obstacles) {
        if (!isAlive) return;

        // Основное направление к цели
        Vector2D desiredDirection = new Vector2D(target.x - position.x, target.y - position.y);
        desiredDirection.normalize();

        // Избегание препятствий
        avoidanceForce = new Vector2D();
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isActive()) continue;

            Vector2D toObstacle = new Vector2D(
                    obstacle.getPosition().x - position.x,
                    obstacle.getPosition().y - position.y
            );
            double distance = toObstacle.distanceTo(new Vector2D(0, 0));

            // Если препятствие слишком близко
            if (distance < 60) {
                toObstacle.normalize();
                // Сила отталкивания обратно пропорциональна расстоянию
                double force = 60 / (distance + 1);
                avoidanceForce.x -= toObstacle.x * force;
                avoidanceForce.y -= toObstacle.y * force;
            }
        }

        // Комбинируем направление к цели и силу избегания
        Vector2D finalDirection = new Vector2D(
                desiredDirection.x * 0.7 + avoidanceForce.x * 0.3,
                desiredDirection.y * 0.7 + avoidanceForce.y * 0.3
        );
        finalDirection.normalize();

        // Плавное изменение скорости
        velocity.x += (finalDirection.x * speed - velocity.x) * 0.05;
        velocity.y += (finalDirection.y * speed - velocity.y) * 0.05;

        // Проверяем столкновения с препятствиями перед движением
        double newX = position.x + velocity.x;
        double newY = position.y + velocity.y;

        boolean collision = false;
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isActive()) continue;

            if (obstacle.collidesWithCircle(newX, newY, radius)) {
                collision = true;

                // Отталкивание от препятствия
                Vector2D push = new Vector2D(position.x - obstacle.getPosition().x,
                        position.y - obstacle.getPosition().y);
                push.normalize();
                velocity.x = push.x * speed * 0.5;
                velocity.y = push.y * speed * 0.5;
                break;
            }
        }

        if (!collision) {
            position.x += velocity.x;
            position.y += velocity.y;
        }

        // Обновляем таймер для анимации избегания
        avoidanceTimer += deltaTime;
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isAlive) return;

        // Основной круг
        Ellipse2D.Double circle = new Ellipse2D.Double(
                position.x - radius - camera.getOffsetX(),
                position.y - radius - camera.getOffsetY(),
                radius * 2,
                radius * 2
        );

        // Изменение цвета при избегании препятствий
        Color currentColor = color;
        if (avoidanceForce.distanceTo(new Vector2D(0, 0)) > 0.5) {
            currentColor = new Color(255, 100, 100);
        }

        g2d.setColor(currentColor);
        g2d.fill(circle);

        // Индикатор здоровья
        double healthRatio = health / 100.0;
        g2d.setColor(new Color(
                (int)(255 * (1 - healthRatio)),
                (int)(255 * healthRatio),
                0
        ));
        g2d.fill(new Ellipse2D.Double(
                position.x - radius * 0.7 * healthRatio - camera.getOffsetX(),
                position.y - radius * 0.7 * healthRatio - camera.getOffsetY(),
                radius * 1.4 * healthRatio,
                radius * 1.4 * healthRatio
        ));

        // Глаза для лучшей видимости
        g2d.setColor(Color.BLACK);
        g2d.fillOval(
                (int)(position.x - radius * 0.3 - camera.getOffsetX()),
                (int)(position.y - radius * 0.3 - camera.getOffsetY()),
                (int)(radius * 0.6),
                (int)(radius * 0.6)
        );
    }

    public boolean takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            isAlive = false;
            return true; // Враг убит
        }
        return false;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public int getHealth() {
        return health;
    }
}