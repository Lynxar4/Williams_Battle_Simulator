package controller;

import entity.InvalidPlacementException;
import entity.Positioned;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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

    // true when nothing held here sits at the given cell
    public boolean isCellFree(int row, int column) {
        return contents.stream().noneMatch(item -> item.isAt(row, column));
    }

    /*
     * Whatever is held at this cell, if anything. Optional rather than a
     * nullable return so the caller cannot forget the empty case, which is the
     * common one on a mostly empty board.
     */
    public Optional<T> at(int row, int column) {
        return contents.stream()
                .filter(item -> item.isAt(row, column))
                .findFirst();
    }

    // everything matching the caller's test, as a list it cannot change
    public List<T> matching(Predicate<T> test) {
        return contents.stream().filter(test).toList();
    }

    // Drops everything matching the test. The caller decides what "spent" means.
    public void removeIf(Predicate<T> spent) {
        contents.removeIf(spent);
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
