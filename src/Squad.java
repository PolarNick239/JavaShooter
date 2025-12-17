import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Squad {
    private List<PlayerSoldier> soldiers;
    private Vector2D targetPosition; // Позиция мыши для следования
    private Color[] soldierColors = {
            new Color(0, 150, 255),    // Главный - синий
            new Color(0, 200, 100),    // Зеленый
            new Color(255, 100, 0),    // Оранжевый
            new Color(200, 0, 200),    // Фиолетовый
            new Color(255, 200, 0),    // Желтый
            new Color(100, 255, 200)   // Бирюзовый
    };

    public Squad(double startX, double startY) {
        soldiers = new ArrayList<>();
        targetPosition = new Vector2D(startX, startY);

        // Создаем главного солдата
        addSoldier(startX, startY, true);
    }

    public void addSoldier(double x, double y, boolean isMain) {
        Color color = soldierColors[soldiers.size() % soldierColors.length];
        soldiers.add(new PlayerSoldier(x, y, color, isMain, soldiers.size()));
    }

    public void update(double deltaTime, List<Obstacle> obstacles) {
        for (PlayerSoldier soldier : soldiers) {
            soldier.update(deltaTime, soldiers.get(0).getPosition(), targetPosition, obstacles);
        }
    }

    public void draw(Graphics2D g2d, Camera camera) {
        for (PlayerSoldier soldier : soldiers) {
            soldier.draw(g2d, camera);
        }
    }

    public void setTargetPosition(double x, double y) {
        targetPosition.x = x;
        targetPosition.y = y;
    }

    public List<PlayerSoldier> getSoldiers() {
        return soldiers;
    }

    public Vector2D getMainPosition() {
        if (!soldiers.isEmpty()) {
            return soldiers.get(0).getPosition();
        }
        return new Vector2D(0, 0);
    }

    public int getSize() {
        return soldiers.size();
    }
}