package entity.troops.melee;

import entity.troops.Attacker;

public abstract class Melee extends Attacker {
    /* VARIABLE */
    private  int armour;

    /* CONSTRUCTOR*/
    public Melee(int amour,int atk,int atkRange,int row, int column, int health, int speed, int troopSize, Team team){
        super(atk,atkRange,row, column, health, speed, troopSize,  team);
        this.armour = amour;
    }

    /* GETTER METHODS*/
    public int getArmour(){
        return armour;
    }
}
