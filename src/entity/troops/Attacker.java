package entity.troops;

// Base class for attacking troop types.
// Melee and ranged troop classes can extend this later.
public abstract class Attacker extends Troop implements Healable {

    /* VARIABLES */
    private int atk;
    private int atkRange;

    /* CONSTRUCTOR */
    public Attacker(int atk,int atkRange,int row, int column, int health, int speed, int troopSize, Team team){
        super(row, column, health, speed, troopSize, team);
        this.atk = atk;
        this.atkRange = atkRange;
    }

    /* PUBLIC METHODS */
    @Override
    public  void beHealed(){

    }

    /* ABSTRACT */
    public abstract void attack(Troop enemy);


    /* GETTER METHODS*/
    public int getAttack() {

        return atk;
    }

    public int getRange() {
        return atkRange;
    }
}
