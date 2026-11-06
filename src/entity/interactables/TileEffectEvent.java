package entity.interactables;

import entity.troops.Troop;

// what happened when a troop stepped onto an effect.
// the effect fills this in itself, including any budget change, so a listener
// can react without knowing which kind of effect fired.

public record TileEffectEvent(TileEffect effect, Troop troop, String description, int budgetChange) {
    /* ---- FACTORY METHODS ---- */
    // an event that changes nothing but is worth reporting
    public static TileEffectEvent of(TileEffect effect, Troop troop, String description) {
        return new TileEffectEvent(effect, troop, description, 0);
    }

    // an event that pays the troop's team
    public static TileEffectEvent paying(TileEffect effect, Troop troop, String description, int amount) {
        return new TileEffectEvent(effect, troop, description, amount);
    }
}
