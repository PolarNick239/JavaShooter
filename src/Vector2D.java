public class Vector2D {
    public double x, y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D() {
        this(0, 0);
    }

    public void normalize() {
        double length = Math.sqrt(x * x + y * y);
        if (length > 0) {
            x /= length;
            y /= length;
        }
    }

    public void multiply(double scalar) {
        x *= scalar;
        y *= scalar;
    }

    public double distanceTo(Vector2D other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Vector2D copy() {
        return new Vector2D(x, y);
    }
}