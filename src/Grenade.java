import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class Grenade {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 6;
    private double fuseTime = 1.2;
    private boolean exploded = false;

    public Grenade(double startX, double startY, double targetX, double targetY) {
        this.position = new Vector2D(startX, startY);
        Vector2D direction = new Vector2D(targetX - startX, targetY - startY);
        direction.normalize();
        this.velocity = new Vector2D(direction.x * 6, direction.y * 6);
    }

    public boolean update(double deltaTime, List<Obstacle> obstacles) {
        if (exploded) {
            return true;
        }

        position.x += velocity.x;
        position.y += velocity.y;

        velocity.x *= 0.98;
        velocity.y *= 0.98;

        fuseTime -= deltaTime;

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isActive()) continue;
            if (obstacle.collidesWithCircle(position.x, position.y, radius)) {
                fuseTime = 0;
                break;
            }
        }

        if (fuseTime <= 0) {
            exploded = true;
            return true;
        }

        return false;
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (exploded) return;

        double drawX = position.x - radius - camera.getOffsetX();
        double drawY = position.y - radius - camera.getOffsetY();

        g2d.setColor(new Color(80, 80, 80));
        g2d.fill(new Ellipse2D.Double(drawX, drawY, radius * 2, radius * 2));

        g2d.setColor(new Color(200, 200, 50));
        g2d.fill(new Ellipse2D.Double(drawX + radius * 0.5, drawY - radius * 0.2,
                radius * 0.6, radius * 0.6));
    }

    public Vector2D getPosition() {
        return position;
    }
}
