import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameManager {
    private Squad squad;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<Particle> particles;
    private List<Bonus> bonuses;
    private Camera camera;

    private int score = 0;
    private int playerHealth = 100;
    private double enemySpawnTimer = 0;
    private double shootCooldown = 0;
    private boolean isShooting = false;
    private Vector2D mousePosition = new Vector2D();

    public GameManager() {
        squad = new Squad(400, 300);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        particles = new ArrayList<>();
        bonuses = new ArrayList<>();
        camera = new Camera();
    }

    public void update(double deltaTime, int screenWidth, int screenHeight) {
        // Обновление отряда
        squad.update(deltaTime);

        // Обновление камеры
        camera.update(squad.getMainPosition(), screenWidth, screenHeight);

        // Спавн врагов
        enemySpawnTimer += deltaTime;
        if (enemySpawnTimer >= 1.0) { // Каждую секунду
            spawnEnemy(screenWidth, screenHeight);
            enemySpawnTimer = 0;
        }

        // Обновление врагов
        for (Enemy enemy : enemies) {
            enemy.update(deltaTime, squad.getMainPosition());

            // Проверка столкновения с игроком
            for (PlayerSoldier soldier : squad.getSoldiers()) {
                double distance = soldier.getPosition().distanceTo(enemy.getPosition());
                if (distance < soldier.getRadius() + enemy.getRadius()) {
                    playerHealth -= 1;
                    camera.shake(5, 0.3);
                }
            }
        }

        // Стрельба
        if (isShooting && shootCooldown <= 0) {
            shoot();
            shootCooldown = 0.1; // 10 выстрелов в секунду
        }
        shootCooldown -= deltaTime;

        // Обновление пуль
        for (Bullet bullet : bullets) {
            bullet.update();
        }

        // Проверка столкновений пуль с врагами
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();

            Iterator<Enemy> enemyIter = enemies.iterator();
            while (enemyIter.hasNext()) {
                Enemy enemy = enemyIter.next();
                if (bullet.checkCollision(enemy)) {
                    if (enemy.takeDamage(bullet.getDamage())) {
                        // Враг убит
                        createExplosion(enemy.getPosition());
                        camera.shake(10, 0.5);
                        score += 100;

                        // Шанс выпадения бонуса
                        if (Math.random() < 0.3) {
                            bonuses.add(new Bonus(
                                    enemy.getPosition().x,
                                    enemy.getPosition().y
                            ));
                        }

                        enemyIter.remove();
                    }
                    break;
                }
            }

            if (!bullet.isActive()) {
                bulletIter.remove();
            }
        }

        // Обновление частиц
        Iterator<Particle> particleIter = particles.iterator();
        while (particleIter.hasNext()) {
            Particle particle = particleIter.next();
            particle.update(deltaTime);
            if (!particle.isActive()) {
                particleIter.remove();
            }
        }

        // Обновление бонусов
        Iterator<Bonus> bonusIter = bonuses.iterator();
        while (bonusIter.hasNext()) {
            Bonus bonus = bonusIter.next();
            bonus.update();

            // Проверка столкновения с игроком
            for (PlayerSoldier soldier : squad.getSoldiers()) {
                if (bonus.checkCollision(soldier.getPosition(), soldier.getRadius())) {
                    applyBonus(bonus);
                    bonusIter.remove();
                    break;
                }
            }
        }

        // Удаление вышедших за границы пуль
        bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            Vector2D pos = bullet.getPosition();
            if (pos.x < -100 || pos.x > screenWidth + 100 ||
                    pos.y < -100 || pos.y > screenHeight + 100) {
                bulletIter.remove();
            }
        }
    }

    private void spawnEnemy(int screenWidth, int screenHeight) {
        // Спавн с края экрана
        double x, y;
        if (Math.random() < 0.5) {
            x = Math.random() < 0.5 ? -50 : screenWidth + 50;
            y = Math.random() * screenHeight;
        } else {
            x = Math.random() * screenWidth;
            y = Math.random() < 0.5 ? -50 : screenHeight + 50;
        }

        enemies.add(new Enemy(x, y, squad.getMainPosition()));
    }

    private void shoot() {
        for (PlayerSoldier soldier : squad.getSoldiers()) {
            Vector2D pos = soldier.getPosition();
            bullets.add(new Bullet(
                    pos.x, pos.y,
                    mousePosition.x, mousePosition.y,
                    new Color(255, 255, 100) // Желтые пули
            ));
        }
    }

    private void createExplosion(Vector2D position) {
        Color explosionColor = new Color(255, 150, 0); // Оранжевый взрыв

        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(
                    position.x,
                    position.y,
                    explosionColor
            ));
        }
    }

    private void applyBonus(Bonus bonus) {
        switch (bonus.getType()) {
            case HEALTH:
                playerHealth = Math.min(100, playerHealth + 30);
                break;
            case NEW_SOLDIER:
                Vector2D mainPos = squad.getMainPosition();
                squad.addSoldier(
                        mainPos.x + (Math.random() - 0.5) * 100,
                        mainPos.y + (Math.random() - 0.5) * 100,
                        false
                );
                break;
        }
    }

    public void draw(Graphics2D g2d, int screenWidth, int screenHeight) {
        // Отрисовка отряда
        squad.draw(g2d, camera);

        // Отрисовка врагов
        for (Enemy enemy : enemies) {
            enemy.draw(g2d, camera);
        }

        // Отрисовка пуль
        for (Bullet bullet : bullets) {
            bullet.draw(g2d, camera);
        }

        // Отрисовка частиц
        for (Particle particle : particles) {
            particle.draw(g2d, camera);
        }

        // Отрисовка бонусов
        for (Bonus bonus : bonuses) {
            bonus.draw(g2d, camera);
        }

        // Отрисовка UI
        drawUI(g2d, screenWidth, screenHeight);
    }

    private void drawUI(Graphics2D g2d, int screenWidth, int screenHeight) {
        // Панель здоровья
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 10, 200, 30);

        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(12, 12, (int)(196 * (playerHealth / 100.0)), 26);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Здоровье: " + playerHealth, 20, 30);

        // Счет
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(screenWidth - 160, 10, 150, 30);

        g2d.setColor(Color.WHITE);
        g2d.drawString("Счет: " + score, screenWidth - 150, 30);

        // Размер отряда
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 50, 200, 30);

        g2d.setColor(Color.WHITE);
        g2d.drawString("Отряд: " + squad.getSize() + " солдат", 20, 70);

        // Инструкции
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("WASD - движение, ЛКМ - стрельба", 10, screenHeight - 30);

        // Крестик прицела
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine((int)mousePosition.x - 10, (int)mousePosition.y,
                (int)mousePosition.x + 10, (int)mousePosition.y);
        g2d.drawLine((int)mousePosition.x, (int)mousePosition.y - 10,
                (int)mousePosition.x, (int)mousePosition.y + 10);
    }

    // Геттеры и сеттеры
    public void setShooting(boolean shooting) {
        this.isShooting = shooting;
    }

    public void setMousePosition(double x, double y) {
        this.mousePosition.x = x + camera.getOffsetX();
        this.mousePosition.y = y + camera.getOffsetY();
        squad.setTargetPosition(this.mousePosition.x, this.mousePosition.y);
    }

    public void movePlayer(double dx, double dy) {
        Vector2D mainPos = squad.getMainPosition();
        squad.setTargetPosition(mainPos.x + dx * 5, mainPos.y + dy * 5);
    }

    public boolean isGameOver() {
        return playerHealth <= 0;
    }

    public int getScore() {
        return score;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }
}