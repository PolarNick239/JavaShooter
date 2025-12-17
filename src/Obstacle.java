import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Obstacle {
    private Vector2D position;
    private double width = 40;
    private double height = 40;
    private int health = 100;
    private int maxHealth = 100;
    private Color baseColor;
    private boolean isActive = true;

    public Obstacle(double x, double y) {
        this.position = new Vector2D(x, y);
        // Коричневый цвет для коробок
        this.baseColor = new Color(139, 69, 19);
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            isActive = false;
        }
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isActive) return;

        // Вычисляем прозрачность на основе здоровья
        float alpha = (float)health / maxHealth;
        Color currentColor = new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                (int)(alpha * 255)
        );

        Rectangle2D.Double rect = new Rectangle2D.Double(
                position.x - width/2 - camera.getOffsetX(),
                position.y - height/2 - camera.getOffsetY(),
                width,
                height
        );

        // Основной цвет
        g2d.setColor(currentColor);
        g2d.fill(rect);

        // Обводка
        g2d.setColor(new Color(101, 67, 33));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(rect);

        // Полоска здоровья
        if (health < maxHealth) {
            double healthWidth = (width - 4) * ((double)health / maxHealth);
            g2d.setColor(new Color(0, 255, 0, 150));
            g2d.fillRect(
                    (int)(position.x - width/2 + 2 - camera.getOffsetX()),
                    (int)(position.y - height/2 - 8 - camera.getOffsetY()),
                    (int)healthWidth,
                    4
            );
        }
    }

    public boolean collidesWithCircle(double circleX, double circleY, double radius) {
        if (!isActive) return false;

        // Находим ближайшую точку на прямоугольнике к кругу
        double closestX = Math.max(position.x - width/2, Math.min(circleX, position.x + width/2));
        double closestY = Math.max(position.y - height/2, Math.min(circleY, position.y + height/2));

        // Вычисляем расстояние от этой точки до центра круга
        double distanceX = circleX - closestX;
        double distanceY = circleY - closestY;
        double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        return distance < radius;
    }

    public boolean collidesWithLine(Vector2D start, Vector2D end, double lineRadius) {
        if (!isActive) return false;

        // Проверяем пересечение линии с прямоугольником
        double rectLeft = position.x - width/2;
        double rectRight = position.x + width/2;
        double rectTop = position.y - height/2;
        double rectBottom = position.y + height/2;

        // Расширяем прямоугольник на радиус линии
        rectLeft -= lineRadius;
        rectRight += lineRadius;
        rectTop -= lineRadius;
        rectBottom += lineRadius;

        // Алгоритм Коэна-Сазерленда для проверки пересечения
        int code1 = computeOutCode(start.x, start.y, rectLeft, rectRight, rectTop, rectBottom);
        int code2 = computeOutCode(end.x, end.y, rectLeft, rectRight, rectTop, rectBottom);

        if ((code1 & code2) != 0) {
            return false; // Оба конца за пределами
        }

        return true;
    }

    private int computeOutCode(double x, double y, double left, double right, double top, double bottom) {
        int code = 0;
        if (x < left) code |= 1; // слева
        else if (x > right) code |= 2; // справа
        if (y < top) code |= 4; // сверху
        else if (y > bottom) code |= 8; // снизу
        return code;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getHealth() {
        return health;
    }
}