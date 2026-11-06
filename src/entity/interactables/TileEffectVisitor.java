package entity.interactables;

// visitor over the tile effect types. a visitor holds one job that needs a
// different answer per effect, so the code asking the question stops needing
// instanceof.
// R is whatever the job produces: an Image for sprites, a String for tooltips.
public interface TileEffectVisitor<R> {
    R visitHill(Hill hill);

    R visitCoin(Coin coin);

    R visitTrap(Trap trap);
}
