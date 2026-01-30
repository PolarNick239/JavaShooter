public interface Boss {
    void update(double deltaTime, Vector2D playerPos, GameManager game);
    void draw(java.awt.Graphics2D g2d, Camera camera);
    boolean isAlive();
    Vector2D getPosition();
    double getRadius();
    int getHealth();
    int getMaxHealth();
    void takeDamage(int damage);
    String getName();
}
