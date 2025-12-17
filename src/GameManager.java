import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameManager {
    private Squad squad;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<Particle> particles;
    private List<Bonus> bonuses;
    private List<Obstacle> obstacles;
    private Camera camera;

    private int score = 0;
    private int playerHealth = 100;
    private double enemySpawnTimer = 0;
    private double shootCooldown = 0;
    private boolean isShooting = false;
    private Vector2D mousePosition = new Vector2D();

    // Для управления WASD
    private double playerVelocityX = 0;
    private double playerVelocityY = 0;
    private final double PLAYER_SPEED = 3.0;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    public GameManager() {
        squad = new Squad(400, 300);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        particles = new ArrayList<>();
        bonuses = new ArrayList<>();
        obstacles = new ArrayList<>();
        camera = new Camera();

        // Генерируем случайные препятствия
        generateObstacles();
    }

    private void generateObstacles() {
        Random random = new Random();

        // Генерируем несколько стенок из коробок
        int numWalls = 5 + random.nextInt(3); // 5-7 стенок

        for (int w = 0; w < numWalls; w++) {
            // Случайная позиция для стены
            double wallX = 100 + random.nextInt(600);
            double wallY = 100 + random.nextInt(400);

            // Случайная ориентация стены (горизонтальная или вертикальная)
            boolean horizontal = random.nextBoolean();
            int wallLength = 3 + random.nextInt(4); // 3-6 коробок

            for (int i = 0; i < wallLength; i++) {
                double x, y;

                if (horizontal) {
                    x = wallX + i * 50;
                    y = wallY;
                } else {
                    x = wallX;
                    y = wallY + i * 50;
                }

                // Проверяем, не слишком ли близко к начальной позиции игрока
                double distanceToPlayer = Math.sqrt(Math.pow(x - 400, 2) + Math.pow(y - 300, 2));
                if (distanceToPlayer > 100) {
                    obstacles.add(new Obstacle(x, y));
                }
            }
        }

        // Добавляем несколько отдельных коробок
        int numSingleBoxes = 8 + random.nextInt(5); // 8-12 отдельных коробок

        for (int i = 0; i < numSingleBoxes; i++) {
            double x = 50 + random.nextInt(700);
            double y = 50 + random.nextInt(500);

            // Проверяем расстояние до игрока и других препятствий
            double distanceToPlayer = Math.sqrt(Math.pow(x - 400, 2) + Math.pow(y - 300, 2));
            boolean tooClose = false;

            for (Obstacle existing : obstacles) {
                double dx = x - existing.getPosition().x;
                double dy = y - existing.getPosition().y;
                if (Math.sqrt(dx*dx + dy*dy) < 60) {
                    tooClose = true;
                    break;
                }
            }

            if (distanceToPlayer > 120 && !tooClose) {
                obstacles.add(new Obstacle(x, y));
            }
        }
    }

    public void update(double deltaTime, int screenWidth, int screenHeight) {
        // Обработка движения WASD
        handleMovement(deltaTime, screenWidth, screenHeight);

        // Обновление отряда с препятствиями
        squad.update(deltaTime, obstacles);

        // Обновление камеры
        camera.update(squad.getMainPosition(), screenWidth, screenHeight);

        // Спавн врагов
        enemySpawnTimer += deltaTime;
        if (enemySpawnTimer >= 1.0) { // Каждую секунду
            spawnEnemy(screenWidth, screenHeight);
            enemySpawnTimer = 0;
        }

        // Обновление врагов с препятствиями
        for (Enemy enemy : enemies) {
            enemy.update(deltaTime, squad.getMainPosition(), obstacles);

            // Проверка столкновения с игроком
            for (PlayerSoldier soldier : squad.getSoldiers()) {
                double distance = soldier.getPosition().distanceTo(enemy.getPosition());
                if (distance < soldier.getRadius() + enemy.getRadius()) {
                    playerHealth -= 1;
                    camera.shake(5, 0.3);
                }
            }
        }

        // Удаляем мертвых врагов (с использованием итератора для безопасного удаления)
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (!enemy.isAlive()) {
                enemyIterator.remove();
            }
        }

        // Стрельба
        if (isShooting && shootCooldown <= 0) {
            shoot();
            shootCooldown = 0.1; // 10 выстрелов в секунду
        }
        shootCooldown -= deltaTime;

        // Обновление пуль с препятствиями
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update(obstacles);

            // Проверка столкновений пуль с врагами
            boolean bulletRemoved = false;
            for (Enemy enemy : enemies) {
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
                    }
                    bulletIterator.remove();
                    bulletRemoved = true;
                    break;
                }
            }

            // Если пуля не была удалена при столкновении с врагом, проверяем ее активность
            if (!bulletRemoved && !bullet.isActive()) {
                bulletIterator.remove();
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
            boolean bonusCollected = false;
            for (PlayerSoldier soldier : squad.getSoldiers()) {
                if (bonus.checkCollision(soldier.getPosition(), soldier.getRadius())) {
                    applyBonus(bonus);
                    bonusIter.remove();
                    bonusCollected = true;
                    break;
                }
            }
        }

        // Удаляем разрушенные препятствия
        obstacles.removeIf(obstacle -> !obstacle.isActive());
    }

    private void handleMovement(double deltaTime, int screenWidth, int screenHeight) {
        // Сбрасываем скорость
        double targetVelX = 0;
        double targetVelY = 0;

        // Устанавливаем скорость в зависимости от нажатых клавиш
        if (upPressed) targetVelY -= PLAYER_SPEED;
        if (downPressed) targetVelY += PLAYER_SPEED;
        if (leftPressed) targetVelX -= PLAYER_SPEED;
        if (rightPressed) targetVelX += PLAYER_SPEED;

        // Нормализация диагонального движения
        if (targetVelX != 0 && targetVelY != 0) {
            targetVelX *= 0.7071; // 1/√2
            targetVelY *= 0.7071;
        }

        // Плавное изменение скорости
        playerVelocityX += (targetVelX - playerVelocityX) * 0.2;
        playerVelocityY += (targetVelY - playerVelocityY) * 0.2;

        // Применяем скорость к главному солдату
        if (!squad.getSoldiers().isEmpty()) {
            PlayerSoldier mainSoldier = squad.getSoldiers().get(0);

            // Устанавливаем скорость для солдата
            mainSoldier.setVelocity(playerVelocityX, playerVelocityY);

            // Ограничение движения границами экрана
            Vector2D pos = mainSoldier.getPosition();
            pos.x = Math.max(50, Math.min(screenWidth - 50, pos.x));
            pos.y = Math.max(50, Math.min(screenHeight - 50, pos.y));
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
        // Отрисовка препятствий
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d, camera);
        }

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

        // Количество препятствий
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 90, 200, 30);

        g2d.setColor(Color.WHITE);
        g2d.drawString("Препятствия: " + obstacles.size(), 20, 110);

        // Инструкции
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("WASD - движение, ЛКМ - стрельба", 10, screenHeight - 30);
        g2d.drawString("Коричневые коробки - препятствия", 10, screenHeight - 50);

        // Крестик прицела (используем экранные координаты мыши)
        double screenMouseX = mousePosition.x - camera.getOffsetX();
        double screenMouseY = mousePosition.y - camera.getOffsetY();

        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine((int)screenMouseX - 10, (int)screenMouseY,
                (int)screenMouseX + 10, (int)screenMouseY);
        g2d.drawLine((int)screenMouseX, (int)screenMouseY - 10,
                (int)screenMouseX, (int)screenMouseY + 10);
    }

    // Геттеры и сеттеры
    public void setShooting(boolean shooting) {
        this.isShooting = shooting;
    }

    public void setMousePosition(double x, double y) {
        // Преобразуем экранные координаты в мировые с учетом камеры
        this.mousePosition.x = x + camera.getOffsetX();
        this.mousePosition.y = y + camera.getOffsetY();
        squad.setTargetPosition(this.mousePosition.x, this.mousePosition.y);
    }

    public void setMovementKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                upPressed = pressed;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                downPressed = pressed;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                leftPressed = pressed;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                rightPressed = pressed;
                break;
        }
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