package view;

import entity.interactables.Coin;
import entity.interactables.Hill;
import entity.interactables.Trap;
import entity.interactables.TileEffectVisitor;
import entity.troops.Troop;

// builds the hover text for an effect.
// second use of the same visitor interface, with a different return type. each
// effect reports what it is and its current state, which only it knows: a hill
// knows who holds it, a trap knows whether it has gone off.

class TileEffectDescriptionVisitor implements TileEffectVisitor<String> {
    /* ---- OVERRIDES ---- */
    @Override
    public String visitHill(Hill hill) {
        String bonuses = "+" + hill.getDamageBonus() + " dmg, +" + hill.getRangeBonus() + " range for ranged";
        if (!hill.isClaimed()) {
            return "Hill | unclaimed | " + bonuses + " to whoever takes it";
        }
        return "Hill | held by " + teamName(hill.getHolder()) + " | " + bonuses;
    }

    @Override
    public String visitCoin(Coin coin) {
        return "Gold | $" + coin.getAmount() + " to the first group here";
    }

    @Override
    public String visitTrap(Trap trap) {
        if (trap.isTriggered()) {
            return "Trap | already sprung";
        }
        return "Trap | " + teamName(trap.getOwner()) + "'s | -" + trap.getDamage() + " dmg to enemies";
    }

    /* ---- PRIVATE METHODS ---- */
    private String teamName(Troop.Team team) {
        return team == Troop.Team.teamA ? "Team A" : "Team B";
    }
}
