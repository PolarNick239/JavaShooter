import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Bonus {
    public enum BonusType {
        HEALTH,
        NEW_SOLDIER,
        FIRE_RATE,
        MOVE_SPEED,
        DAMAGE,
        EXPLOSION_RADIUS,
        EXPLOSIVE_SHOTS
    }

    private Vector2D position;
    private Vector2D velocity;
    private double radius = 8;
    private BonusType type;
    private Color color;
    private boolean isActive = true;
    private double ttl = -1;
    private double floatOffset = 0;

    public Bonus(double x, double y) {
        this.position = new Vector2D(x, y);
        this.type = rollType();

        switch (type) {
            case HEALTH:
                color = new Color(0, 255, 100);
                break;
            case NEW_SOLDIER:
                color = new Color(255, 200, 0);
                break;
            case FIRE_RATE:
                color = new Color(0, 200, 255);
                break;
            case MOVE_SPEED:
                color = new Color(100, 150, 255);
                break;
            case DAMAGE:
                color = new Color(255, 120, 60);
                break;
            case EXPLOSION_RADIUS:
                color = new Color(255, 80, 80);
                break;
            case EXPLOSIVE_SHOTS:
                color = new Color(255, 255, 200);
                ttl = 4.0;
                break;
            default:
                color = Color.WHITE;
                break;
        }

        velocity = new Vector2D(
                (Math.random() - 0.5) * 3,
                (Math.random() - 0.5) * 3
        );
    }

    private BonusType rollType() {
        double r = Math.random();
        if (r < 0.25) return BonusType.HEALTH;
        if (r < 0.45) return BonusType.NEW_SOLDIER;
        if (r < 0.60) return BonusType.FIRE_RATE;
        if (r < 0.75) return BonusType.MOVE_SPEED;
        if (r < 0.88) return BonusType.DAMAGE;
        if (r < 0.97) return BonusType.EXPLOSION_RADIUS;
        return BonusType.EXPLOSIVE_SHOTS;
    }

    public void update(double deltaTime) {
        if (!isActive) return;

        position.x += velocity.x;
        position.y += velocity.y;

        velocity.x *= 0.98;
        velocity.y *= 0.98;

        floatOffset += deltaTime * 2.0;
        position.y += Math.sin(floatOffset) * 0.5;

        if (ttl > 0) {
            ttl -= deltaTime;
            if (ttl <= 0) {
                isActive = false;
            }
        }
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isActive) return;

        double pulse = 1.0;
        if (type == BonusType.EXPLOSIVE_SHOTS) {
            pulse = 1.0 + 0.2 * Math.sin(floatOffset * 4.0);
        }

        Ellipse2D.Double circle = new Ellipse2D.Double(
                position.x - radius * pulse - camera.getOffsetX(),
                position.y - radius * pulse - camera.getOffsetY(),
                radius * 2 * pulse,
                radius * 2 * pulse
        );

        g2d.setColor(color);
        g2d.fill(circle);

        g2d.setColor(Color.WHITE);
        g2d.fill(new Ellipse2D.Double(
                position.x - radius / 2 - camera.getOffsetX(),
                position.y - radius / 2 - camera.getOffsetY(),
                radius,
                radius
        ));

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString(getSymbol(),
                (int) (position.x - 4 - camera.getOffsetX()),
                (int) (position.y + 4 - camera.getOffsetY())
        );
    }

    private String getSymbol() {
        switch (type) {
            case HEALTH:
                return "+";
            case NEW_SOLDIER:
                return "S";
            case FIRE_RATE:
                return "F";
            case MOVE_SPEED:
                return "M";
            case DAMAGE:
                return "D";
            case EXPLOSION_RADIUS:
                return "R";
            case EXPLOSIVE_SHOTS:
                return "X";
            default:
                return "?";
        }
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
