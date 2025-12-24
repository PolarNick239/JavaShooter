import java.util.*;

public class Pathfinder {

    public static List<GridCell> findPath(Grid grid, GridCell start, GridCell target) {
        if (start == null || target == null || !target.walkable) {
            return new ArrayList<>();
        }

        List<GridCell> openSet = new ArrayList<>();
        Set<GridCell> closedSet = new HashSet<>();
        openSet.add(start);

        // Сброс стоимости для всех ячеек
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getCols(); c++) {
                GridCell cell = grid.getCellAtGridPos(r, c);
                if (cell != null) {
                    cell.gCost = 0;
                    cell.hCost = 0;
                    cell.parent = null;
                }
            }
        }

        while (!openSet.isEmpty()) {
            GridCell current = openSet.get(0);

            // Находим ячейку с наименьшей fCost
            for (GridCell cell : openSet) {
                if (cell.fCost() < current.fCost() ||
                        (Math.abs(cell.fCost() - current.fCost()) < 0.001 && cell.hCost < current.hCost)) {
                    current = cell;
                }
            }

            openSet.remove(current);
            closedSet.add(current);

            // Достигли цели
            if (current.equals(target)) {
                return retracePath(start, target);
            }

            // Проверяем соседей
            for (GridCell neighbor : grid.getNeighbors(current)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double newCostToNeighbor = current.gCost + getDistance(current, neighbor);
                if (newCostToNeighbor < neighbor.gCost || !openSet.contains(neighbor)) {
                    neighbor.gCost = newCostToNeighbor;
                    neighbor.hCost = getDistance(neighbor, target);
                    neighbor.parent = current;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return new ArrayList<>(); // Путь не найден
    }

    private static List<GridCell> retracePath(GridCell start, GridCell end) {
        List<GridCell> path = new ArrayList<>();
        GridCell current = end;

        while (current != null && !current.equals(start)) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static double getDistance(GridCell a, GridCell b) {
        // Евклидово расстояние для более естественного движения
        int dx = a.col - b.col;
        int dy = a.row - b.row;
        return Math.sqrt(dx * dx + dy * dy);
    }
}