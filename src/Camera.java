public class Camera {
    private double offsetX = 0;
    private double offsetY = 0;
    private double targetOffsetX = 0;
    private double targetOffsetY = 0;
    private double shakeIntensity = 0;
    private double shakeDuration = 0;

    public void update(Vector2D targetPosition, int screenWidth, int screenHeight) {
        // Плавное следование за целью
        targetOffsetX = targetPosition.x - screenWidth / 2.0;
        targetOffsetY = targetPosition.y - screenHeight / 2.0;

        offsetX += (targetOffsetX - offsetX) * 0.1;
        offsetY += (targetOffsetY - offsetY) * 0.1;

        // Эффект тряски
        if (shakeDuration > 0) {
            offsetX += (Math.random() - 0.5) * shakeIntensity;
            offsetY += (Math.random() - 0.5) * shakeIntensity;

            shakeDuration -= 0.1;
            shakeIntensity *= 0.9;
        }
    }

    public void shake(double intensity, double duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = duration;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }
}