import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PlayerSoldier {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 10;
    private Color color;
    private boolean isMain;
    private double followDistance;
    private int index;
    private BufferedImage skin;

    private static final String[] SKIN_PATHS = {
            "/data/player_1.png",
            "/data/player_2.png"
    };
    private static final BufferedImage[] SKINS = loadSkins(SKIN_PATHS);

    public PlayerSoldier(double x, double y, Color color, boolean isMain, int index) {
        this.position = new Vector2D(x, y);
        this.velocity = new Vector2D();
        this.color = color;
        this.isMain = isMain;
        this.index = index;
        this.followDistance = 30 + index * 15;
        this.skin = pickSkin(index);
    }

    public void update(double deltaTime, Vector2D mainPosition, Vector2D target, List<Obstacle> obstacles) {
        Vector2D oldPosition = position.copy();

        if (isMain) {
            position.x += velocity.x;
            position.y += velocity.y;

            for (Obstacle obstacle : obstacles) {
                if (!obstacle.isActive()) continue;

                if (obstacle.collidesWithCircle(position.x, position.y, radius)) {
                    position = oldPosition;
                    Vector2D push = new Vector2D(position.x - obstacle.getPosition().x,
                            position.y - obstacle.getPosition().y);
                    push.normalize();
                    velocity.x = push.x * 2;
                    velocity.y = push.y * 2;
                    break;
                }
            }

            velocity.x *= 0.9;
            velocity.y *= 0.9;
        } else {
            double angle = (index * 2 * Math.PI / 6);
            double targetX = mainPosition.x + Math.cos(angle) * followDistance;
            double targetY = mainPosition.y + Math.sin(angle) * followDistance;

            position.x += (targetX - position.x) * 0.1;
            position.y += (targetY - position.y) * 0.1;

            for (Obstacle obstacle : obstacles) {
                if (!obstacle.isActive()) continue;

                if (obstacle.collidesWithCircle(position.x, position.y, radius)) {
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
        double drawX = position.x - radius - camera.getOffsetX();
        double drawY = position.y - radius - camera.getOffsetY();

        if (skin != null) {
            g2d.drawImage(skin, (int) drawX, (int) drawY, (int) (radius * 2), (int) (radius * 2), null);
            if (isMain) {
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval((int) drawX, (int) drawY, (int) (radius * 2), (int) (radius * 2));
            }
            return;
        }

        Ellipse2D.Double circle = new Ellipse2D.Double(
                drawX,
                drawY,
                radius * 2,
                radius * 2
        );

        g2d.setColor(color);
        g2d.fill(circle);

        if (isMain) {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(circle);
        }

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

    public void setPosition(double x, double y) {
        this.position.x = x;
        this.position.y = y;
        this.velocity.x = 0;
        this.velocity.y = 0;
    }

    private static BufferedImage[] loadSkins(String[] paths) {
        List<BufferedImage> list = new ArrayList<>();
        for (String path : paths) {
            try {
                list.add(Assets.loadImage(path));
            } catch (Exception e) {
                // ignore missing skins
            }
        }
        return list.toArray(new BufferedImage[0]);
    }

    private static BufferedImage pickSkin(int index) {
        if (SKINS.length == 0) return null;
        int idx = Math.abs(index) % SKINS.length;
        return SKINS[idx];
    }
}
