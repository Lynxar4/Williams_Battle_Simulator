package controller;

import entity.InvalidPlacementException;
import entity.Positioned;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Every entity of one kind occupying cells on the grid. The Positioned bound
 * lets this class check positions without knowing the concrete type, so one
 * class serves tile effects, castles and troops alike.
 */
public class GridContents<T extends Positioned> {
    /* ---- PRIVATE VARIABLES ---- */
    private final ArrayList<T> contents = new ArrayList<>();
    private final int rowCount;
    private final int columnCount;

    /* ---- CONSTRUCTORS ---- */
    public GridContents(int rowCount, int columnCount) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    /* ---- PUBLIC METHODS ---- */
    public void place(T item) throws InvalidPlacementException {
        if (!isWithinBounds(item.getRow(), item.getColumn())) {
            throw new InvalidPlacementException(
                    "Cell " + item.getRow() + "," + item.getColumn() + " is outside the battlefield.");
        }

        contents.add(item);
    }

    public boolean isWithinBounds(int row, int column) {
        return row >= 0 && row < rowCount && column >= 0 && column < columnCount;
    }

    // True when nothing held here sits at the given cell.
    public boolean isCellFree(int row, int column) {
        for (T item : contents) {
            if (item.isAt(row, column)) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        contents.clear();
    }

    public int size() {
        return contents.size();
    }

    /* ---- GETTER METHODS ---- */
    public List<T> getContents() {
        return Collections.unmodifiableList(contents);
    }
}
