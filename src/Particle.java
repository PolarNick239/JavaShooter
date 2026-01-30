import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Particle {
    private Vector2D position;
    private Vector2D velocity;
    private double size;
    private Color color;
    private double life = 1.0;
    private boolean isActive = true;

    public Particle(double x, double y, Color color) {
        this(x, y, color, 3, 8, 2, 8);
    }

    public Particle(double x, double y, Color color, double sizeMin, double sizeMax,
                    double speedMin, double speedMax) {
        this.position = new Vector2D(x, y);
        this.color = color;
        double clampedSizeMax = Math.max(sizeMin, sizeMax);
        this.size = sizeMin + Math.random() * (clampedSizeMax - sizeMin);

        double angle = Math.random() * Math.PI * 2;
        double clampedSpeedMax = Math.max(speedMin, speedMax);
        double speed = speedMin + Math.random() * (clampedSpeedMax - speedMin);
        this.velocity = new Vector2D(
                Math.cos(angle) * speed,
                Math.sin(angle) * speed
        );
    }

    public void update(double deltaTime) {
        if (!isActive) return;

        position.x += velocity.x;
        position.y += velocity.y;

        velocity.x *= 0.95;
        velocity.y *= 0.95;

        life -= 0.02;
        if (life <= 0) {
            isActive = false;
        }
    }

    public void draw(Graphics2D g2d, Camera camera) {
        if (!isActive) return;

        double alpha = life;
        Color particleColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int)(alpha * 255)
        );

        g2d.setColor(particleColor);
        g2d.fill(new Ellipse2D.Double(
                position.x - size / 2 - camera.getOffsetX(),
                position.y - size / 2 - camera.getOffsetY(),
                size,
                size
        ));
    }

    public boolean isActive() {
        return isActive;
    }
}
