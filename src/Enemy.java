import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Enemy {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 15;
    private Color color;
    private int health = 100;
    private int maxHealth = 100;
    private boolean isAlive = true;
    private double speed;
    private BufferedImage skin;

    private static final String[] SKIN_PATHS = {
            "/data/enemy_1.png",
            "/data/enemy_2.png",
            "/data/enemy_3.png"
    };
    private static final BufferedImage[] SKINS = loadSkins(SKIN_PATHS);

    private List<GridCell> currentPath = new ArrayList<>();
    private double pathUpdateTimer = 0;
    private static final double PATH_UPDATE_INTERVAL = 0.5;

    public Enemy(double x, double y, double speed, int maxHealth) {
        this.position = new Vector2D(x, y);
        this.velocity = new Vector2D();
        this.color = new Color(200, 50, 50);
        this.speed = speed;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.skin = pickSkin();
    }

    public void update(double deltaTime, Vector2D target, Grid grid) {
        if (!isAlive) return;

        pathUpdateTimer += deltaTime;
        if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
            updatePath(grid);
            pathUpdateTimer = 0;
        }

        if (!currentPath.isEmpty()) {
            followPath(deltaTime, grid);
        } else {
            moveDirectly(deltaTime, target);
        }
    }

    private void updatePath(Grid grid) {
        GridCell start = grid.getCellAtWorldPos(position.x, position.y);
        if (start == null || !grid.hasDistanceField()) {
            currentPath.clear();
            return;
        }
        currentPath = grid.buildPathFrom(start, 60);
    }

    private void followPath(double deltaTime, Grid grid) {
        if (currentPath.isEmpty()) return;

        GridCell targetCell = currentPath.get(0);
        Vector2D targetPos = new Vector2D(targetCell.worldX, targetCell.worldY);

        Vector2D direction = new Vector2D(targetPos.x - position.x, targetPos.y - position.y);
        double distance = direction.distanceTo(new Vector2D(0, 0));

        if (distance > 0.1) {
            direction.normalize();
            velocity.x = direction.x * speed;
            velocity.y = direction.y * speed;

            position.x += velocity.x;
            position.y += velocity.y;
        }

        if (distance < 5) {
            currentPath.remove(0);
        }
    }

    private void moveDirectly(double deltaTime, Vector2D target) {
        Vector2D direction = new Vector2D(target.x - position.x, target.y - position.y);
        double distance = direction.distanceTo(new Vector2D(0, 0));

        if (distance > 0.1) {
            direction.normalize();
            velocity.x = direction.x * speed;
            velocity.y = direction.y * speed;

            position.x += velocity.x;
            position.y += velocity.y;
        }
    }

    public void draw(Graphics2D g2d, Camera camera, boolean showPath) {
        if (!isAlive) return;

        double drawX = position.x - radius - camera.getOffsetX();
        double drawY = position.y - radius - camera.getOffsetY();

        if (skin != null) {
            g2d.drawImage(skin, (int) drawX, (int) drawY, (int) (radius * 2), (int) (radius * 2), null);
        } else {
            Ellipse2D.Double circle = new Ellipse2D.Double(
                    drawX,
                    drawY,
                    radius * 2,
                    radius * 2
            );
            g2d.setColor(color);
            g2d.fill(circle);
        }

        double healthRatio = maxHealth > 0 ? (health / (double) maxHealth) : 0;
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

        if (skin == null) {
            g2d.setColor(Color.BLACK);
            g2d.fillOval(
                    (int)(position.x - radius * 0.3 - camera.getOffsetX()),
                    (int)(position.y - radius * 0.3 - camera.getOffsetY()),
                    (int)(radius * 0.6),
                    (int)(radius * 0.6)
            );
        }

        if (showPath && !currentPath.isEmpty()) {
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.setStroke(new BasicStroke(2));

            GridCell nextCell = currentPath.get(0);
            g2d.drawLine(
                    (int)(position.x - camera.getOffsetX()),
                    (int)(position.y - camera.getOffsetY()),
                    (int)(nextCell.worldX - camera.getOffsetX()),
                    (int)(nextCell.worldY - camera.getOffsetY())
            );

            for (int i = 0; i < currentPath.size(); i++) {
                GridCell cell = currentPath.get(i);
                g2d.setColor(new Color(0, 255, 0, Math.min(255, 100 + i * 50)));
                g2d.fillOval(
                        (int)(cell.worldX - 3 - camera.getOffsetX()),
                        (int)(cell.worldY - 3 - camera.getOffsetY()),
                        6, 6
                );
            }
        }
    }

    public boolean takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            isAlive = false;
            return true;
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

    private static BufferedImage pickSkin() {
        if (SKINS.length == 0) return null;
        int idx = (int) Math.floor(Math.random() * SKINS.length);
        return SKINS[idx];
    }
}
