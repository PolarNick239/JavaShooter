import java.util.Objects;

public class GridCell {
    public int row, col;
    public boolean walkable;
    public double worldX, worldY;
    public Obstacle obstacle;

    // Для A*
    public GridCell parent;
    public double gCost, hCost;

    public GridCell(int row, int col, double worldX, double worldY) {
        this.row = row;
        this.col = col;
        this.walkable = true;
        this.worldX = worldX;
        this.worldY = worldY;
    }

    public double fCost() {
        return gCost + hCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GridCell gridCell = (GridCell) obj;
        return row == gridCell.row && col == gridCell.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}