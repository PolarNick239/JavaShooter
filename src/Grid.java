import java.util.*;

public class Grid {
    private GridCell[][] cells;
    private int rows, cols;
    private double cellSize;
    private GridCell distanceTarget;
    private boolean distanceFieldReady = false;

    public Grid(int screenWidth, int screenHeight, double cellSize) {
        this.cellSize = cellSize;
        this.cols = (int) Math.ceil(screenWidth / cellSize);
        this.rows = (int) Math.ceil(screenHeight / cellSize);

        cells = new GridCell[rows][cols];

        // Инициализация всех ячеек
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double worldX = c * cellSize + cellSize / 2;
                double worldY = r * cellSize + cellSize / 2;
                cells[r][c] = new GridCell(r, c, worldX, worldY);
            }
        }
    }

    public GridCell getCellAtWorldPos(double x, double y) {
        int col = (int)(x / cellSize);
        int row = (int)(y / cellSize);

        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return cells[row][col];
        }
        return null;
    }

    public GridCell getCellAtGridPos(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return cells[row][col];
        }
        return null;
    }

    public void setObstacle(Obstacle obstacle) {
        int minCol = (int) Math.floor((obstacle.getPosition().x - obstacle.getWidth() / 2) / cellSize);
        int maxCol = (int) Math.floor((obstacle.getPosition().x + obstacle.getWidth() / 2) / cellSize);
        int minRow = (int) Math.floor((obstacle.getPosition().y - obstacle.getHeight() / 2) / cellSize);
        int maxRow = (int) Math.floor((obstacle.getPosition().y + obstacle.getHeight() / 2) / cellSize);

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                GridCell cell = getCellAtGridPos(r, c);
                if (cell != null) {
                    cell.walkable = false;
                    cell.obstacle = obstacle;
                }
            }
        }
    }

    public void removeObstacle(Obstacle obstacle) {
        int minCol = (int) Math.floor((obstacle.getPosition().x - obstacle.getWidth() / 2) / cellSize);
        int maxCol = (int) Math.floor((obstacle.getPosition().x + obstacle.getWidth() / 2) / cellSize);
        int minRow = (int) Math.floor((obstacle.getPosition().y - obstacle.getHeight() / 2) / cellSize);
        int maxRow = (int) Math.floor((obstacle.getPosition().y + obstacle.getHeight() / 2) / cellSize);

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                GridCell cell = getCellAtGridPos(r, c);
                if (cell != null && cell.obstacle == obstacle) {
                    cell.walkable = true;
                    cell.obstacle = null;
                }
            }
        }
    }

    public List<GridCell> getNeighbors(GridCell cell) {
        List<GridCell> neighbors = new ArrayList<>();

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int newRow = cell.row + dr;
                int newCol = cell.col + dc;

                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    GridCell neighbor = cells[newRow][newCol];
                    if (neighbor.walkable) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
        return neighbors;
    }

    public void updateDistanceField(Vector2D target) {
        distanceFieldReady = false;
        distanceTarget = getCellAtWorldPos(target.x, target.y);
        if (distanceTarget == null || !distanceTarget.walkable) {
            return;
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                GridCell cell = cells[r][c];
                cell.distance = Double.POSITIVE_INFINITY;
            }
        }

        PriorityQueue<GridCell> open = new PriorityQueue<>(Comparator.comparingDouble(a -> a.distance));
        distanceTarget.distance = 0;
        open.add(distanceTarget);

        while (!open.isEmpty()) {
            GridCell current = open.poll();
            double currentDistance = current.distance;

            for (GridCell neighbor : getNeighbors(current)) {
                double stepCost = getStepCost(current, neighbor);
                double newDist = currentDistance + stepCost;
                if (newDist < neighbor.distance) {
                    neighbor.distance = newDist;
                    open.add(neighbor);
                }
            }
        }

        distanceFieldReady = true;
    }

    public List<GridCell> buildPathFrom(GridCell start, int maxSteps) {
        List<GridCell> path = new ArrayList<>();
        if (!distanceFieldReady || distanceTarget == null || start == null) {
            return path;
        }
        GridCell current = start;
        for (int i = 0; i < maxSteps; i++) {
            if (current.equals(distanceTarget)) {
                break;
            }
            GridCell next = getBestNeighbor(current);
            if (next == null || next.distance >= current.distance) {
                break;
            }
            path.add(next);
            current = next;
        }
        return path;
    }

    private GridCell getBestNeighbor(GridCell cell) {
        GridCell best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (GridCell neighbor : getNeighbors(cell)) {
            if (neighbor.distance < bestDistance) {
                bestDistance = neighbor.distance;
                best = neighbor;
            }
        }
        return best;
    }

    private double getStepCost(GridCell a, GridCell b) {
        int dr = Math.abs(a.row - b.row);
        int dc = Math.abs(a.col - b.col);
        if (dr + dc == 1) {
            return 1.0;
        }
        return Math.sqrt(2);
    }

    public boolean hasDistanceField() {
        return distanceFieldReady;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public double getCellSize() { return cellSize; }
}
