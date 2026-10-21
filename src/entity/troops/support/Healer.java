package entity.troops.support;

import entity.troops.Healable;
import entity.troops.Support;

public abstract class Healer extends Support {
    /* VARIABLES */
    private  int healAmount;

    /*PUBLIC METHODS*/
    /* ABSTRACT*/
    public abstract void heal(Healable ally);

    /* GETTER METHODS*/
    public int getHealAmount() {
        return healAmount;
    }
}
