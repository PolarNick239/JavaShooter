import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class BossProjectile {
    public enum Kind {
        BULLET,
        SHELL
    }

    private Vector2D position;
    private Vector2D velocity;
    private double radius;
    private int damage;
    private double ttl;
    private boolean explosive;
    private double explosionRadius;
    private boolean exploded = false;
    private Kind kind;

    public static BossProjectile createBullet(double startX, double startY, double targetX, double targetY,
                                              double speed, int damage, double ttl) {
        Vector2D dir = new Vector2D(targetX - startX, targetY - startY);
        dir.normalize();
        return new BossProjectile(Kind.BULLET, startX, startY, dir, speed, 3.5, damage, ttl, false, 0);
    }

    public static BossProjectile createShell(double startX, double startY, double targetX, double targetY,
                                             double speed, int damage, double ttl, double explosionRadius) {
        Vector2D dir = new Vector2D(targetX - startX, targetY - startY);
        dir.normalize();
        return new BossProjectile(Kind.SHELL, startX, startY, dir, speed, 6.0, damage, ttl, true, explosionRadius);
    }

    private BossProjectile(Kind kind, double startX, double startY, Vector2D direction, double speed,
                           double radius, int damage, double ttl, boolean explosive, double explosionRadius) {
        this.kind = kind;
        this.position = new Vector2D(startX, startY);
        this.velocity = new Vector2D(direction.x * speed, direction.y * speed);
        this.radius = radius;
        this.damage = damage;
        this.ttl = ttl;
        this.explosive = explosive;
        this.explosionRadius = explosionRadius;
    }

    public boolean update(double deltaTime, List<Obstacle> obstacles) {
        if (exploded) return true;

        position.x += velocity.x;
        position.y += velocity.y;
        ttl -= deltaTime;

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isActive()) continue;
            if (obstacle.collidesWithCircle(position.x, position.y, radius)) {
                if (explosive) {
                    exploded = true;
                }
                return true;
            }
        }

        if (ttl <= 0) {
            if (explosive) {
                exploded = true;
            }
            return true;
        }

        return false;
    }

    public void draw(Graphics2D g2d, Camera camera) {
        double drawX = position.x - radius - camera.getOffsetX();
        double drawY = position.y - radius - camera.getOffsetY();

        Color color = kind == Kind.SHELL ? new Color(240, 170, 60) : new Color(255, 80, 80);
        g2d.setColor(color);
        g2d.fill(new Ellipse2D.Double(drawX, drawY, radius * 2, radius * 2));
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public int getDamage() {
        return damage;
    }

    public boolean shouldExplode() {
        return exploded && explosive;
    }

    public double getExplosionRadius() {
        return explosionRadius;
    }

    public Kind getKind() {
        return kind;
    }
}
