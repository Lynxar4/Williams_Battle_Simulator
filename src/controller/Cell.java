package controller;

// a single square on the grid. exists so a spawn strategy can hand back a
// position without packing two ints into an array.
public record Cell(int row, int column) {
}
