package entity.troops.melee;

import entity.troops.Troop;

public class Cavalry extends Melee {
    /* VARIABLES */
    private static final int BASE_HEALTH = 20;

    /* CONSTRUCTOR */
    public Cavalry(int amour,int atk,int atkRange,int row, int column, int health, int speed, int troopSize, Team team){
        super(amour,atk,atkRange,row, column, health, speed, troopSize, team);
    }

    /* PUBLIC METHODS*/
    @Override
    public void attack(Troop enemy) {

    }


    @Override
    public void move() {

    }
}
