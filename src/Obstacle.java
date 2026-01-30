import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Obstacle {
    private Vector2D position;
    private double width = 40;
    private double height = 40;
    private int health = 100;
    private int maxHealth = 100;
    private boolean isActive = true;

    // Загруженные изображения
    private static BufferedImage boxImage = null;
    private static BufferedImage crackImage = null;

    static {
        // Загружаем изображения при первой загрузке класса
        try {
            boxImage = loadImage("/data/box.png");
            crackImage = loadImage("/data/crack.png");
        } catch (IOException e) {
            System.err.println("Не удалось загрузить изображения коробки: " + e.getMessage());
        }
    }

    private static BufferedImage loadImage(String resourcePath) throws IOException {
        try (InputStream in = Obstacle.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image == null) {
                    throw new IOException("Unsupported image format: " + resourcePath);
                }
                return image;
            }
        }
        String filePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        BufferedImage image = ImageIO.read(new File(filePath));
        if (image == null) {
            throw new IOException("Image not found or unsupported: " + filePath);
        }
        return image;
    }

    public Obstacle(double x, double y) {
        this.position = new Vector2D(x, y);
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            isActive = false;
        }
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isActive) return;

        double drawX = position.x - width/2 - camera.getOffsetX();
        double drawY = position.y - height/2 - camera.getOffsetY();

        // Если изображение коробки загружено, рисуем его
        if (boxImage != null) {
            float alpha = 1.0f; // Всегда полная непрозрачность

            // Сохраняем текущую композитную операцию
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Рисуем изображение коробки
            g2d.drawImage(boxImage,
                    (int)drawX, (int)drawY,
                    (int)width, (int)height,
                    null);

            // Восстанавливаем композитную операцию
            g2d.setComposite(oldComposite);

        } else {
            // Если изображение не загружено, рисуем простой прямоугольник
            Rectangle2D.Double rect = new Rectangle2D.Double(drawX, drawY, width, height);
            Color baseColor = new Color(139, 69, 19);

            Color currentColor = new Color(
                    baseColor.getRed(),
                    baseColor.getGreen(),
                    baseColor.getBlue(),
                    255
            );

            g2d.setColor(currentColor);
            g2d.fill(rect);

            // Обводка
            g2d.setColor(new Color(101, 67, 33));
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(rect);
        }

        // Рисуем трещины в зависимости от повреждений
        if (crackImage != null && health < maxHealth) {
            drawCracks(g2d, camera);
        } else if (health < maxHealth) {
            // Если изображение трещин не загружено, рисуем простые линии
            drawSimpleCracks(g2d, camera);
        }

        // Полоска здоровья
        if (health < maxHealth) {
            drawHealthBar(g2d, camera, drawX, drawY);
        }
    }

    private void drawCracks(Graphics2D g2d, Camera camera) {
        // Количество трещин зависит от повреждений (0-4 трещины)
        int numCracks = 4 - (int)((float)health / maxHealth * 4);
        numCracks = Math.max(0, Math.min(4, numCracks));

        // Углы для трещин (случайные для каждой коробки, но фиксированные на основе позиции)
        long seed = (long)(position.x * 1000 + position.y);
        double[] angles = {
                (seed % 360) * Math.PI / 180,
                ((seed * 7) % 360) * Math.PI / 180,
                ((seed * 13) % 360) * Math.PI / 180,
                ((seed * 29) % 360) * Math.PI / 180
        };

        float crackAlpha = 1.0f;

        // Сохраняем текущую композитную операцию
        Composite oldComposite = g2d.getComposite();

        for (int i = 0; i < numCracks; i++) {
            // Увеличиваем прозрачность с каждой новой трещиной
            float currentAlpha = crackAlpha * (0.5f + i * 0.5f / numCracks);
            currentAlpha = Math.min(1.0f, currentAlpha);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlpha));

            // Сохраняем текущее преобразование
            AffineTransform oldTransform = g2d.getTransform();

            // Переходим к центру коробки
            g2d.translate(position.x - camera.getOffsetX(),
                    position.y - camera.getOffsetY());

            // Поворачиваем на угол трещины
            g2d.rotate(angles[i]);

            // Масштабируем трещину (немного меньше коробки)
            double scale = 0.8 + i * 0.05; // Каждая следующая трещина немного больше
            int scaledWidth = (int)(width * scale);
            int scaledHeight = (int)(height * scale);

            // Рисуем трещину
            g2d.drawImage(crackImage,
                    -scaledWidth/2, -scaledHeight/2,
                    scaledWidth, scaledHeight,
                    null);

            // Восстанавливаем преобразование
            g2d.setTransform(oldTransform);
        }

        // Восстанавливаем композитную операцию
        g2d.setComposite(oldComposite);
    }

    private void drawSimpleCracks(Graphics2D g2d, Camera camera) {
        // Количество трещин зависит от повреждений
        int numCracks = 4 - (int)((float)health / maxHealth * 4);
        numCracks = Math.max(0, Math.min(4, numCracks));

        // Углы для трещин
        long seed = (long)(position.x * 1000 + position.y);
        double[] angles = {
                (seed % 360) * Math.PI / 180,
                ((seed * 7) % 360) * Math.PI / 180,
                ((seed * 13) % 360) * Math.PI / 180,
                ((seed * 29) % 360) * Math.PI / 180
        };

        // Толщина трещины зависит от повреждений
        float crackThickness = 1.0f + (1.0f - (float)health / maxHealth) * 3.0f;

        g2d.setColor(new Color(50, 50, 50, 150));
        g2d.setStroke(new BasicStroke(crackThickness));

        double centerX = position.x - camera.getOffsetX();
        double centerY = position.y - camera.getOffsetY();

        for (int i = 0; i < numCracks; i++) {
            // Сохраняем текущее преобразование
            AffineTransform oldTransform = g2d.getTransform();

            // Переходим к центру коробки
            g2d.translate(centerX, centerY);

            // Поворачиваем на угол трещины
            g2d.rotate(angles[i]);

            // Длина трещины зависит от повреждений
            double crackLength = width * (0.3f + i * 0.15f);

            // Рисуем трещину (несколько линий для эффекта)
            for (int j = 0; j < 3; j++) {
                double offset = (j - 1) * 1.5;
                g2d.drawLine(-(int)(crackLength/2), (int)offset,
                        (int)(crackLength/2), (int)offset);
            }

            // Восстанавливаем преобразование
            g2d.setTransform(oldTransform);
        }
    }

    private void drawHealthBar(Graphics2D g2d, Camera camera, double drawX, double drawY) {
        double healthWidth = (width - 4) * ((double)health / maxHealth);

        // Фон полоски здоровья
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(
                (int)(drawX + 2),
                (int)(drawY - 8),
                (int)(width - 4),
                4
        );

        // Сама полоска здоровья
        Color healthColor = new Color(
                (int)(255 * (1.0 - (double)health / maxHealth)),
                (int)(255 * ((double)health / maxHealth)),
                0,
                200
        );
        g2d.setColor(healthColor);
        g2d.fillRect(
                (int)(drawX + 2),
                (int)(drawY - 8),
                (int)healthWidth,
                4
        );

        // Обводка полоски здоровья
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(
                (int)(drawX + 2),
                (int)(drawY - 8),
                (int)(width - 4),
                3
        );
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
