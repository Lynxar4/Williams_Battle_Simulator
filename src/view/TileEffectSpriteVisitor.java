package view;

import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.Trap;
import entity.interactables.TileEffectVisitor;
import java.awt.Image;

// picks the sprite for an effect.
// the panel used to chain instanceof checks to work this out. now each effect
// hands itself to this visitor and the right method runs, so a new effect type
// only has to be added here and to the visitor interface, which the compiler
// then forces.

class TileEffectSpriteVisitor implements TileEffectVisitor<Image> {
    /* ---- PRIVATE VARIABLES ---- */
    private final Image hillSprite;
    private final Image coinSprite;
    private final Image trapSprite;

    /* ---- CONSTRUCTORS ---- */
    TileEffectSpriteVisitor(Image hillSprite, Image coinSprite, Image trapSprite) {
        this.hillSprite = hillSprite;
        this.coinSprite = coinSprite;
        this.trapSprite = trapSprite;
    }

    /* ---- OVERRIDES ---- */
    @Override
    public Image visitHill(Hill hill) {
        return hillSprite;
    }

    @Override
    public Image visitCoin(Coin coin) {
        return coinSprite;
    }

    @Override
    public Image visitTrap(Trap trap) {
        return trapSprite;
    }
}
