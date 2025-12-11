import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Enemy {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 15;
    private Color color;
    private int health = 100;
    private boolean isAlive = true;
    private double speed;

    public Enemy(double x, double y, Vector2D target) {
        this.position = new Vector2D(x, y);
        this.color = new Color(200, 50, 50);
        this.speed = 1 + Math.random() * 2;

        // Направление к цели
        velocity = new Vector2D(target.x - x, target.y - y);
        velocity.normalize();
        velocity.multiply(speed);
    }

    public void update(double deltaTime, Vector2D target) {
        if (!isAlive) return;

        // Обновление направления к цели
        Vector2D direction = new Vector2D(target.x - position.x, target.y - position.y);
        direction.normalize();

        // Плавное изменение направления
        velocity.x += (direction.x * speed - velocity.x) * 0.05;
        velocity.y += (direction.y * speed - velocity.y) * 0.05;

        position.x += velocity.x;
        position.y += velocity.y;
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

        g2d.setColor(color);
        g2d.fill(circle);

        // Индикатор здоровья
        double healthRatio = health / 100.0;
        g2d.setColor(new Color(
                (int)(255 * (1 - healthRatio)),
                (int)(255 * healthRatio),
                0
        ));
        g2d.fill(new Ellipse2D.Double(
                position.x - radius * 0.7 - camera.getOffsetX(),
                position.y - radius * 0.7 - camera.getOffsetY(),
                radius * 1.4 * healthRatio,
                radius * 1.4 * healthRatio
        ));
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