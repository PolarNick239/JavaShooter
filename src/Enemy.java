import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

public class Enemy {
    private Vector2D position;
    private Vector2D velocity;
    private double radius = 15;
    private Color color;
    private int health = 100;
    private boolean isAlive = true;
    private double speed;

    // Для поиска пути
    private List<GridCell> currentPath = new ArrayList<>();
    private double pathUpdateTimer = 0;
    private static final double PATH_UPDATE_INTERVAL = 0.5; // секунды

    public Enemy(double x, double y, Vector2D target) {
        this.position = new Vector2D(x, y);
        this.velocity = new Vector2D(); // ИНИЦИАЛИЗАЦИЯ ВЕКТОРА СКОРОСТИ
        this.color = new Color(200, 50, 50);
        this.speed = 1 + Math.random() * 2;
    }

    public void update(double deltaTime, Vector2D target, Grid grid) {
        if (!isAlive) return;

        // Обновляем путь с интервалом
        pathUpdateTimer += deltaTime;
        if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
            updatePath(grid, target);
            pathUpdateTimer = 0;
        }

        // Двигаемся по пути, если он есть
        if (!currentPath.isEmpty()) {
            followPath(deltaTime, grid);
        } else {
            // Старая логика движения напрямую (на случай, если путь не найден)
            moveDirectly(deltaTime, target);
        }
    }

    private void updatePath(Grid grid, Vector2D target) {
        GridCell start = grid.getCellAtWorldPos(position.x, position.y);
        GridCell end = grid.getCellAtWorldPos(target.x, target.y);

        if (start != null && end != null && !start.equals(end)) {
            currentPath = Pathfinder.findPath(grid, start, end);
        } else {
            currentPath.clear();
        }
    }

    private void followPath(double deltaTime, Grid grid) {
        if (currentPath.isEmpty()) return;

        // Берем первую ячейку в пути
        GridCell targetCell = currentPath.get(0);
        Vector2D targetPos = new Vector2D(targetCell.worldX, targetCell.worldY);

        // Направление к цели
        Vector2D direction = new Vector2D(targetPos.x - position.x, targetPos.y - position.y);
        double distance = direction.distanceTo(new Vector2D(0, 0));

        if (distance > 0.1) {
            direction.normalize();
            velocity.x = direction.x * speed;
            velocity.y = direction.y * speed;

            position.x += velocity.x;
            position.y += velocity.y;
        }

        // Если достигли ячейки, удаляем ее из пути
        if (distance < 5) {
            currentPath.remove(0);
        }
    }

    private void moveDirectly(double deltaTime, Vector2D target) {
        // Прямое движение к цели (запасной вариант)
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

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isAlive) return;

        // Основной круг
        Ellipse2D.Double circle = new Ellipse2D.Double(
                position.x - radius - camera.getOffsetX(),
                position.y - radius - camera.getOffsetY(),
                radius * 2,
                radius * 2
        );

        // Цвет врага
        Color currentColor = color;

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

        // Визуализация пути (отладка)
        if (!currentPath.isEmpty()) {
            g2d.setColor(new Color(0, 255, 0, 100));
            g2d.setStroke(new BasicStroke(2));

            // Линия от врага к следующей ячейке
            GridCell nextCell = currentPath.get(0);
            g2d.drawLine(
                    (int)(position.x - camera.getOffsetX()),
                    (int)(position.y - camera.getOffsetY()),
                    (int)(nextCell.worldX - camera.getOffsetX()),
                    (int)(nextCell.worldY - camera.getOffsetY())
            );

            // Точки пути
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