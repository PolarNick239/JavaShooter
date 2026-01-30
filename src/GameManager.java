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
    private List<Grenade> grenades;
    private List<Particle> particles;
    private List<Bonus> bonuses;
    private List<Obstacle> obstacles;
    private Camera camera;
    private Grid grid;

    private int score = 0;
    private int playerHealth = 100;
    private double enemySpawnTimer = 0;
    private double enemySpawnInterval = 1.0;
    private double shootCooldown = 0;
    private boolean isShooting = false;
    private Vector2D mousePosition = new Vector2D();
    private boolean showPaths = false;
    private boolean godMode = false;

    private double fireRateMultiplier = 1.0;
    private double moveSpeedMultiplier = 1.0;
    private int bulletDamageBonus = 0;
    private double explosionRadiusBonus = 0;
    private double explosiveShotsTimer = 0;

    private int grenadesLeft = BASE_GRENADE_LIMIT;
    private double grenadeCooldownTimer = 0;

    private double playerVelocityX = 0;
    private double playerVelocityY = 0;
    private final double PLAYER_SPEED = 3.0;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private final double CELL_SIZE = 40.0;
    private double pathFieldTimer = 0;

    private int level = 1;
    private int killsThisLevel = 0;
    private int killsToAdvance = 10;
    private double levelBannerTimer = 0;
    private LevelConfig levelConfig;
    private int worldWidth;
    private int worldHeight;
    private boolean[][] visibleCells;
    private boolean[][] seenObstacleCells;
    private int fogRows;
    private int fogCols;
    private java.awt.image.BufferedImage fogLayer;
    private int fogLayerW;
    private int fogLayerH;

    private static final double BASE_SHOOT_COOLDOWN = 0.1;
    private static final double BASE_GRENADE_COOLDOWN = 1.5;
    private static final int BASE_GRENADE_LIMIT = 3;
    private static final int BASE_BULLET_DAMAGE = 25;
    private static final double BASE_GRENADE_RADIUS = 80;
    private static final int BASE_GRENADE_DAMAGE = 80;
    private static final double EXPLOSIVE_SHOTS_DURATION = 8.0;
    private static final double EXPLOSIVE_SHOT_RADIUS = 60;
    private static final int EXPLOSIVE_SHOT_DAMAGE = 35;
    private static final double PATH_FIELD_INTERVAL = 0.25;

    public GameManager(int screenWidth, int screenHeight) {
        worldWidth = screenWidth;
        worldHeight = screenHeight;

        squad = new Squad(screenWidth / 2.0, screenHeight / 2.0);
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        grenades = new ArrayList<>();
        particles = new ArrayList<>();
        bonuses = new ArrayList<>();
        obstacles = new ArrayList<>();
        camera = new Camera();

        applyLevelConfig(buildLevelConfig(level), true);
    }

    public void update(double deltaTime, int screenWidth, int screenHeight) {
        if (levelBannerTimer > 0) {
            levelBannerTimer -= deltaTime;
        }
        if (grenadeCooldownTimer > 0) {
            grenadeCooldownTimer -= deltaTime;
        }
        if (explosiveShotsTimer > 0) {
            explosiveShotsTimer -= deltaTime;
            if (explosiveShotsTimer < 0) {
                explosiveShotsTimer = 0;
            }
        }

        handleMovement(deltaTime, screenWidth, screenHeight);

        squad.update(deltaTime, obstacles);

        camera.update(squad.getMainPosition(), screenWidth, screenHeight);

        pathFieldTimer += deltaTime;
        if (pathFieldTimer >= PATH_FIELD_INTERVAL) {
            grid.updateDistanceField(squad.getMainPosition());
            pathFieldTimer = 0;
        }

        enemySpawnTimer += deltaTime;
        if (enemySpawnTimer >= enemySpawnInterval) {
            spawnEnemy();
            enemySpawnTimer = 0;
        }

        for (Enemy enemy : enemies) {
            enemy.update(deltaTime, squad.getMainPosition(), grid);
            if (!godMode) {
                for (PlayerSoldier soldier : squad.getSoldiers()) {
                    double distance = soldier.getPosition().distanceTo(enemy.getPosition());
                    if (distance < soldier.getRadius() + enemy.getRadius()) {
                        playerHealth -= 1;
                        camera.shake(5, 0.3);
                    }
                }
            }
        }

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (!enemy.isAlive()) {
                enemyIterator.remove();
            }
        }

        if (isShooting && shootCooldown <= 0) {
            shoot();
            shootCooldown = BASE_SHOOT_COOLDOWN / Math.max(0.1, fireRateMultiplier);
        }
        shootCooldown -= deltaTime;

        updateBullets();
        updateGrenades(deltaTime);
        updateParticles(deltaTime);
        updateBonuses(deltaTime);
        updateObstacles();
        updateVisibility();

        if (killsThisLevel >= killsToAdvance) {
            nextLevel();
        }
    }

    private void updateBullets() {
        boolean explosiveShotsActive = explosiveShotsTimer > 0;
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update(obstacles);

            if (!bullet.isActive()) {
                if (explosiveShotsActive) {
                    explodeAt(bullet.getPosition(),
                            EXPLOSIVE_SHOT_RADIUS + explosionRadiusBonus,
                            EXPLOSIVE_SHOT_DAMAGE + bulletDamageBonus,
                            true, 6, 0.3);
                }
                bulletIterator.remove();
                continue;
            }

            boolean bulletRemoved = false;
            for (Enemy enemy : enemies) {
                if (bullet.checkCollision(enemy)) {
                    applyDamageToEnemy(enemy, bullet.getDamage());
                    if (explosiveShotsActive) {
                        explodeAt(bullet.getPosition(),
                                EXPLOSIVE_SHOT_RADIUS + explosionRadiusBonus,
                                EXPLOSIVE_SHOT_DAMAGE + bulletDamageBonus,
                                true, 6, 0.3);
                    }
                    bulletIterator.remove();
                    bulletRemoved = true;
                    break;
                }
            }

            if (!bulletRemoved && !bullet.isActive()) {
                bulletIterator.remove();
            }
        }
    }

    private void updateGrenades(double deltaTime) {
        Iterator<Grenade> grenadeIterator = grenades.iterator();
        while (grenadeIterator.hasNext()) {
            Grenade grenade = grenadeIterator.next();
            if (grenade.update(deltaTime, obstacles)) {
                explodeAt(grenade.getPosition(),
                        BASE_GRENADE_RADIUS + explosionRadiusBonus,
                        BASE_GRENADE_DAMAGE,
                        true, 10, 0.5);
                grenadeIterator.remove();
            }
        }
    }

    private void updateParticles(double deltaTime) {
        Iterator<Particle> particleIter = particles.iterator();
        while (particleIter.hasNext()) {
            Particle particle = particleIter.next();
            particle.update(deltaTime);
            if (!particle.isActive()) {
                particleIter.remove();
            }
        }
    }

    private void updateBonuses(double deltaTime) {
        Iterator<Bonus> bonusIter = bonuses.iterator();
        while (bonusIter.hasNext()) {
            Bonus bonus = bonusIter.next();
            bonus.update(deltaTime);
            if (!bonus.isActive()) {
                bonusIter.remove();
                continue;
            }

            for (PlayerSoldier soldier : squad.getSoldiers()) {
                if (bonus.checkCollision(soldier.getPosition(), soldier.getRadius())) {
                    applyBonus(bonus);
                    bonusIter.remove();
                    break;
                }
            }
        }
    }

    private void updateObstacles() {
        Iterator<Obstacle> obstacleIter = obstacles.iterator();
        while (obstacleIter.hasNext()) {
            Obstacle obstacle = obstacleIter.next();
            if (!obstacle.isActive()) {
                grid.removeObstacle(obstacle);
                obstacleIter.remove();
            }
        }
    }

    private void handleMovement(double deltaTime, int screenWidth, int screenHeight) {
        double targetVelX = 0;
        double targetVelY = 0;
        double speed = PLAYER_SPEED * moveSpeedMultiplier;

        if (upPressed) targetVelY -= speed;
        if (downPressed) targetVelY += speed;
        if (leftPressed) targetVelX -= speed;
        if (rightPressed) targetVelX += speed;

        if (targetVelX != 0 && targetVelY != 0) {
            targetVelX *= 0.7071;
            targetVelY *= 0.7071;
        }

        playerVelocityX += (targetVelX - playerVelocityX) * 0.2;
        playerVelocityY += (targetVelY - playerVelocityY) * 0.2;

        if (!squad.getSoldiers().isEmpty()) {
            PlayerSoldier mainSoldier = squad.getSoldiers().get(0);
            mainSoldier.setVelocity(playerVelocityX, playerVelocityY);

            Vector2D pos = mainSoldier.getPosition();
            pos.x = Math.max(50, Math.min(worldWidth - 50, pos.x));
            pos.y = Math.max(50, Math.min(worldHeight - 50, pos.y));
        }
    }

    private void spawnEnemy() {
        double x, y;
        if (Math.random() < 0.5) {
            x = Math.random() < 0.5 ? -50 : worldWidth + 50;
            y = Math.random() * worldHeight;
        } else {
            x = Math.random() * worldWidth;
            y = Math.random() < 0.5 ? -50 : worldHeight + 50;
        }

        double speed = randomRange(levelConfig.enemySpeedMin, levelConfig.enemySpeedMax);
        int health = randomIntRange(levelConfig.enemyHealthMin, levelConfig.enemyHealthMax);
        Enemy enemy = new Enemy(x, y, speed, health);
        enemies.add(enemy);
    }

    private void shoot() {
        int damage = BASE_BULLET_DAMAGE + bulletDamageBonus;
        for (PlayerSoldier soldier : squad.getSoldiers()) {
            Vector2D pos = soldier.getPosition();
            bullets.add(new Bullet(
                    pos.x, pos.y,
                    mousePosition.x, mousePosition.y,
                    new Color(255, 255, 100),
                    damage
            ));
        }
    }

    private void explodeAt(Vector2D position, double radius, int damage,
                           boolean affectPlayer, double shakeIntensity, double shakeDuration) {
        createExplosionEffect(position, new Color(255, 150, 0), 20);
        camera.shake(shakeIntensity, shakeDuration);
        applyExplosionDamage(position, radius, damage, affectPlayer);
    }

    private void applyExplosionDamage(Vector2D position, double radius, int damage, boolean affectPlayer) {
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            double distance = enemy.getPosition().distanceTo(position);
            if (distance <= radius + enemy.getRadius()) {
                int scaled = scaleDamage(damage, distance, radius);
                applyDamageToEnemy(enemy, scaled);
            }
        }

        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isActive()) continue;
            double distance = obstacle.getPosition().distanceTo(position);
            double obstacleRadius = Math.max(obstacle.getWidth(), obstacle.getHeight()) / 2.0;
            if (distance <= radius + obstacleRadius) {
                int scaled = scaleDamage(damage, distance, radius);
                obstacle.takeDamage(scaled);
            }
        }

        if (affectPlayer && !godMode) {
            double minDistance = Double.POSITIVE_INFINITY;
            for (PlayerSoldier soldier : squad.getSoldiers()) {
                double distance = soldier.getPosition().distanceTo(position);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
            if (minDistance <= radius + 10) {
                int scaled = scaleDamage(damage, minDistance, radius);
                playerHealth = Math.max(0, playerHealth - scaled);
            }
        }
    }

    private int scaleDamage(int baseDamage, double distance, double radius) {
        if (baseDamage <= 0) return 0;
        double factor = Math.max(0, 1.0 - (distance / radius));
        return Math.max(1, (int) Math.round(baseDamage * factor));
    }

    private void applyDamageToEnemy(Enemy enemy, int damage) {
        if (!enemy.isAlive()) return;
        if (enemy.takeDamage(damage)) {
            onEnemyKilled(enemy);
        }
    }

    private void onEnemyKilled(Enemy enemy) {
        createExplosionEffect(enemy.getPosition(), new Color(255, 150, 0), 20);
        camera.shake(10, 0.5);
        score += 100;
        killsThisLevel += 1;

        if (Math.random() < 0.3) {
            bonuses.add(new Bonus(
                    enemy.getPosition().x,
                    enemy.getPosition().y
            ));
        }
    }

    private void createExplosionEffect(Vector2D position, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    position.x,
                    position.y,
                    color
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
            case FIRE_RATE:
                fireRateMultiplier = Math.min(3.0, fireRateMultiplier + 0.15);
                break;
            case MOVE_SPEED:
                moveSpeedMultiplier = Math.min(3.0, moveSpeedMultiplier + 0.1);
                break;
            case DAMAGE:
                bulletDamageBonus = Math.min(40, bulletDamageBonus + 5);
                break;
            case EXPLOSION_RADIUS:
                explosionRadiusBonus = Math.min(80, explosionRadiusBonus + 10);
                break;
            case EXPLOSIVE_SHOTS:
                explosiveShotsTimer = Math.max(explosiveShotsTimer, EXPLOSIVE_SHOTS_DURATION);
                break;
        }
    }

    public void draw(Graphics2D g2d, int screenWidth, int screenHeight) {
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g2d, camera);
        }

        squad.draw(g2d, camera);

        for (Enemy enemy : enemies) {
            enemy.draw(g2d, camera, showPaths);
        }

        for (Bullet bullet : bullets) {
            bullet.draw(g2d, camera);
        }

        for (Grenade grenade : grenades) {
            grenade.draw(g2d, camera);
        }

        for (Particle particle : particles) {
            particle.draw(g2d, camera);
        }

        for (Bonus bonus : bonuses) {
            bonus.draw(g2d, camera);
        }

        drawFog(g2d, screenWidth, screenHeight);

        drawUI(g2d, screenWidth, screenHeight);
    }

    private void drawUI(Graphics2D g2d, int screenWidth, int screenHeight) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(10, 10, 230, 200);

        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(12, 12, (int) (226 * (playerHealth / 100.0)), 16);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Health: " + playerHealth, 16, 24);
        g2d.drawString("Score: " + score, 16, 42);
        g2d.drawString("Squad: " + squad.getSize(), 16, 58);
        g2d.drawString("Level: " + level + " (" + killsThisLevel + "/" + killsToAdvance + ")", 16, 74);
        g2d.drawString("Grenades: " + grenadesLeft + " CD: " + formatTime(grenadeCooldownTimer), 16, 90);
        g2d.drawString("FireRate x" + formatFloat(fireRateMultiplier), 16, 106);
        g2d.drawString("MoveSpeed x" + formatFloat(moveSpeedMultiplier), 16, 122);
        g2d.drawString("Damage +" + bulletDamageBonus, 16, 138);

        if (explosionRadiusBonus > 0) {
            g2d.drawString("Explosion +" + (int) explosionRadiusBonus, 16, 154);
        }
        if (explosiveShotsTimer > 0) {
            g2d.drawString("Explosive: " + formatTime(explosiveShotsTimer), 16, 170);
        }
        if (godMode) {
            g2d.setColor(new Color(255, 255, 100));
            g2d.drawString("GOD MODE", screenWidth - 110, 22);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("WASD - move, LMB - shoot, G - grenade", 10, screenHeight - 30);
        g2d.drawString("P - pause, O - god mode, I - paths", 10, screenHeight - 50);

        if (levelBannerTimer > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("LEVEL " + level, screenWidth / 2 - 60, 40);
        }
    }

    private String formatTime(double value) {
        if (value < 0) value = 0;
        return String.format("%.1f", value);
    }

    private String formatFloat(double value) {
        return String.format("%.2f", value);
    }

    public void setShooting(boolean shooting) {
        this.isShooting = shooting;
    }

    public void setMousePosition(double x, double y) {
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

    public void toggleGodMode() {
        godMode = !godMode;
    }

    public void toggleShowPaths() {
        showPaths = !showPaths;
    }

    public void tryThrowGrenade() {
        if (grenadesLeft <= 0) return;
        if (grenadeCooldownTimer > 0) return;

        Vector2D pos = squad.getMainPosition();
        grenades.add(new Grenade(pos.x, pos.y, mousePosition.x, mousePosition.y));
        grenadesLeft -= 1;
        grenadeCooldownTimer = BASE_GRENADE_COOLDOWN;
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

    private void nextLevel() {
        level += 1;
        applyLevelConfig(buildLevelConfig(level), true);
    }

    private void applyLevelConfig(LevelConfig config, boolean resetPlayer) {
        levelConfig = config;
        enemySpawnInterval = config.spawnInterval;
        killsThisLevel = 0;
        killsToAdvance = 8 + level * 4;
        levelBannerTimer = 2.0;

        enemySpawnTimer = 0;
        shootCooldown = 0;
        grenadeCooldownTimer = 0;
        grenadesLeft = BASE_GRENADE_LIMIT;

        enemies.clear();
        bullets.clear();
        grenades.clear();
        particles.clear();
        bonuses.clear();
        obstacles.clear();

        grid = new Grid(worldWidth, worldHeight, CELL_SIZE);
        generateObstacles(config);
        resetFog();

        if (resetPlayer) {
            squad.resetPosition(worldWidth / 2.0, worldHeight / 2.0);
        }
        pathFieldTimer = PATH_FIELD_INTERVAL;
    }

    private void generateObstacles(LevelConfig config) {
        Random random = new Random();
        double safeRadius = 140;

        if (config.theme == LevelTheme.MAZE) {
            generateMaze(config, random, safeRadius);
            return;
        }

        int walls = randomIntRange(config.wallCountMin, config.wallCountMax);
        for (int w = 0; w < walls; w++) {
            boolean horizontal = random.nextBoolean();
            int wallLength = randomIntRange(config.wallLengthMin, config.wallLengthMax);
            double size = randomRange(config.boxSizeMin, config.boxSizeMax);
            int health = randomIntRange(config.boxHealthMin, config.boxHealthMax);

            double startX = randomRange(80, worldWidth - 80);
            double startY = randomRange(80, worldHeight - 80);

            for (int i = 0; i < wallLength; i++) {
                double x = startX + (horizontal ? i * size : 0);
                double y = startY + (horizontal ? 0 : i * size);
                if (!isWithinWorld(x, y, size)) continue;
                Obstacle obstacle = new Obstacle(x, y, size, health);
                if (isAreaFree(obstacle, safeRadius)) {
                    obstacles.add(obstacle);
                    grid.setObstacle(obstacle);
                }
            }
        }

        int singles = randomIntRange(config.singleBoxesMin, config.singleBoxesMax);
        int attempts = singles * 4;
        for (int i = 0; i < attempts && singles > 0; i++) {
            double size = randomRange(config.boxSizeMin, config.boxSizeMax);
            int health = randomIntRange(config.boxHealthMin, config.boxHealthMax);
            double x = randomRange(60, worldWidth - 60);
            double y = randomRange(60, worldHeight - 60);
            if (!isWithinWorld(x, y, size)) continue;
            Obstacle obstacle = new Obstacle(x, y, size, health);
            if (isAreaFree(obstacle, safeRadius)) {
                obstacles.add(obstacle);
                grid.setObstacle(obstacle);
                singles--;
            }
        }
    }

    private void generateMaze(LevelConfig config, Random random, double safeRadius) {
        int rows = (int) Math.floor(worldHeight / CELL_SIZE);
        int cols = (int) Math.floor(worldWidth / CELL_SIZE);

        for (int r = 1; r < rows - 1; r += 2) {
            for (int c = 1; c < cols - 1; c += 2) {
                if (random.nextDouble() > config.mazeFillChance) continue;
                double x = c * CELL_SIZE + CELL_SIZE / 2.0;
                double y = r * CELL_SIZE + CELL_SIZE / 2.0;
                double size = config.boxSizeMin;
                int health = randomIntRange(config.boxHealthMin, config.boxHealthMax);
                Obstacle obstacle = new Obstacle(x, y, size, health);
                if (isAreaFree(obstacle, safeRadius)) {
                    obstacles.add(obstacle);
                    grid.setObstacle(obstacle);
                }
            }
        }
    }

    private boolean isWithinWorld(double x, double y, double size) {
        double half = size / 2.0;
        return x - half > 0 && x + half < worldWidth && y - half > 0 && y + half < worldHeight;
    }

    private boolean isAreaFree(Obstacle candidate, double safeRadius) {
        double dx = candidate.getPosition().x - worldWidth / 2.0;
        double dy = candidate.getPosition().y - worldHeight / 2.0;
        if (Math.sqrt(dx * dx + dy * dy) < safeRadius) {
            return false;
        }
        for (Obstacle existing : obstacles) {
            if (rectsOverlap(candidate, existing)) {
                return false;
            }
        }
        return true;
    }

    private boolean rectsOverlap(Obstacle a, Obstacle b) {
        double ax = a.getPosition().x;
        double ay = a.getPosition().y;
        double bx = b.getPosition().x;
        double by = b.getPosition().y;
        return Math.abs(ax - bx) < (a.getWidth() / 2 + b.getWidth() / 2 + 2) &&
                Math.abs(ay - by) < (a.getHeight() / 2 + b.getHeight() / 2 + 2);
    }

    private double randomRange(double min, double max) {
        if (max <= min) return min;
        return min + Math.random() * (max - min);
    }

    private int randomIntRange(int min, int max) {
        if (max <= min) return min;
        return min + (int) Math.floor(Math.random() * (max - min + 1));
    }

    private LevelConfig buildLevelConfig(int level) {
        LevelTheme theme = LevelTheme.values()[(level - 1) % LevelTheme.values().length];
        LevelConfig config = new LevelConfig();
        config.theme = theme;

        config.spawnInterval = Math.max(0.4, 1.0 - level * 0.05);
        config.enemySpeedMin = 1.0 + level * 0.05;
        config.enemySpeedMax = 2.0 + level * 0.1;
        config.enemyHealthMin = 80 + level * 5;
        config.enemyHealthMax = 120 + level * 7;

        if (theme == LevelTheme.SPARSE) {
            config.wallCountMin = 3;
            config.wallCountMax = 4;
            config.wallLengthMin = 3;
            config.wallLengthMax = 5;
            config.singleBoxesMin = 6;
            config.singleBoxesMax = 9;
            config.boxSizeMin = 35;
            config.boxSizeMax = 45;
            config.boxHealthMin = 80;
            config.boxHealthMax = 120;
        } else if (theme == LevelTheme.DENSE) {
            config.spawnInterval = Math.max(0.35, 0.9 - level * 0.05);
            config.wallCountMin = 7;
            config.wallCountMax = 9;
            config.wallLengthMin = 4;
            config.wallLengthMax = 7;
            config.singleBoxesMin = 18;
            config.singleBoxesMax = 24;
            config.boxSizeMin = 35;
            config.boxSizeMax = 45;
            config.boxHealthMin = 100;
            config.boxHealthMax = 160;
        } else if (theme == LevelTheme.HUGE) {
            config.wallCountMin = 2;
            config.wallCountMax = 3;
            config.wallLengthMin = 2;
            config.wallLengthMax = 4;
            config.singleBoxesMin = 5;
            config.singleBoxesMax = 7;
            config.boxSizeMin = 70;
            config.boxSizeMax = 120;
            config.boxHealthMin = 200;
            config.boxHealthMax = 400;
            config.enemySpeedMin = Math.max(0.8, config.enemySpeedMin - 0.2);
            config.enemySpeedMax = Math.max(1.2, config.enemySpeedMax - 0.2);
            config.enemyHealthMin += 40;
            config.enemyHealthMax += 60;
        } else if (theme == LevelTheme.MAZE) {
            config.wallCountMin = 0;
            config.wallCountMax = 0;
            config.wallLengthMin = 0;
            config.wallLengthMax = 0;
            config.singleBoxesMin = 0;
            config.singleBoxesMax = 0;
            config.boxSizeMin = CELL_SIZE;
            config.boxSizeMax = CELL_SIZE;
            config.boxHealthMin = 120;
            config.boxHealthMax = 180;
            config.mazeFillChance = 0.7;
        }

        return config;
    }

    private void resetFog() {
        visibleCells = null;
        seenObstacleCells = null;
        fogRows = 0;
        fogCols = 0;
        fogLayer = null;
        fogLayerW = 0;
        fogLayerH = 0;
    }

    private void ensureFogArrays() {
        if (grid == null) return;
        int rows = grid.getRows();
        int cols = grid.getCols();
        if (visibleCells == null || rows != fogRows || cols != fogCols) {
            visibleCells = new boolean[rows][cols];
            seenObstacleCells = new boolean[rows][cols];
            fogRows = rows;
            fogCols = cols;
        }
    }

    private void updateVisibility() {
        if (grid == null || squad == null) return;
        ensureFogArrays();
        if (visibleCells == null) return;

        for (int r = 0; r < fogRows; r++) {
            for (int c = 0; c < fogCols; c++) {
                visibleCells[r][c] = false;
            }
        }

        Vector2D playerPos = squad.getMainPosition();
        GridCell playerCell = grid.getCellAtWorldPos(playerPos.x, playerPos.y);
        if (playerCell == null) return;

        double radius = worldWidth * 0.4;
        double radiusSq = radius * radius;

        for (int r = 0; r < fogRows; r++) {
            for (int c = 0; c < fogCols; c++) {
                GridCell cell = grid.getCellAtGridPos(r, c);
                if (cell == null) continue;
                double dx = cell.worldX - playerPos.x;
                double dy = cell.worldY - playerPos.y;
                if (dx * dx + dy * dy > radiusSq) {
                    continue;
                }
                if (grid.hasLineOfSight(playerCell, cell)) {
                    visibleCells[r][c] = true;
                }
            }
        }

        for (int r = 0; r < fogRows; r++) {
            for (int c = 0; c < fogCols; c++) {
                if (!visibleCells[r][c]) continue;
                GridCell cell = grid.getCellAtGridPos(r, c);
                if (cell == null) continue;
                if (cell.obstacle != null && cell.obstacle.isActive()) {
                    seenObstacleCells[r][c] = true;
                } else {
                    seenObstacleCells[r][c] = false;
                }
            }
        }
    }

    private void drawFog(Graphics2D g2d, int screenWidth, int screenHeight) {
        if (visibleCells == null || grid == null) return;
        if (fogLayer == null || fogLayerW != screenWidth || fogLayerH != screenHeight) {
            fogLayerW = screenWidth;
            fogLayerH = screenHeight;
            fogLayer = new java.awt.image.BufferedImage(screenWidth, screenHeight,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D fg = fogLayer.createGraphics();
        fg.setComposite(AlphaComposite.Src);
        fg.setColor(Color.BLACK);
        fg.fillRect(0, 0, screenWidth, screenHeight);

        fg.setComposite(AlphaComposite.Clear);
        double cellSize = grid.getCellSize();
        int size = (int) Math.ceil(cellSize) + 1;

        for (int r = 0; r < fogRows; r++) {
            for (int c = 0; c < fogCols; c++) {
                if (!visibleCells[r][c]) continue;
                GridCell cell = grid.getCellAtGridPos(r, c);
                if (cell == null) continue;
                int x = (int) Math.floor(cell.worldX - cellSize / 2 - camera.getOffsetX());
                int y = (int) Math.floor(cell.worldY - cellSize / 2 - camera.getOffsetY());
                fg.fillRect(x, y, size, size);
            }
        }

        fg.setComposite(AlphaComposite.SrcOver);
        fg.setColor(new Color(15, 15, 15));
        for (int r = 0; r < fogRows; r++) {
            for (int c = 0; c < fogCols; c++) {
                if (visibleCells[r][c]) continue;
                if (!seenObstacleCells[r][c]) continue;
                GridCell cell = grid.getCellAtGridPos(r, c);
                if (cell == null) continue;
                int x = (int) Math.floor(cell.worldX - cellSize / 2 - camera.getOffsetX());
                int y = (int) Math.floor(cell.worldY - cellSize / 2 - camera.getOffsetY());
                fg.fillRect(x, y, size, size);
            }
        }

        fg.dispose();
        g2d.drawImage(fogLayer, 0, 0, null);
    }

    private enum LevelTheme {
        SPARSE,
        DENSE,
        HUGE,
        MAZE
    }

    private static class LevelConfig {
        LevelTheme theme;
        int wallCountMin;
        int wallCountMax;
        int wallLengthMin;
        int wallLengthMax;
        int singleBoxesMin;
        int singleBoxesMax;
        double boxSizeMin;
        double boxSizeMax;
        int boxHealthMin;
        int boxHealthMax;
        double spawnInterval;
        double enemySpeedMin;
        double enemySpeedMax;
        int enemyHealthMin;
        int enemyHealthMax;
        double mazeFillChance = 0.0;
    }
}
