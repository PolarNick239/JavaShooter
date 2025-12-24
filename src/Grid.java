import java.util.*;

public class Grid {
    private GridCell[][] cells;
    private int rows, cols;
    private double cellSize;

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
        GridCell cell = getCellAtWorldPos(obstacle.getPosition().x, obstacle.getPosition().y);
        if (cell != null) {
            cell.walkable = false;
            cell.obstacle = obstacle;
        }
    }

    public void removeObstacle(Obstacle obstacle) {
        GridCell cell = getCellAtWorldPos(obstacle.getPosition().x, obstacle.getPosition().y);
        if (cell != null) {
            cell.walkable = true;
            cell.obstacle = null;
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

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public double getCellSize() { return cellSize; }
}