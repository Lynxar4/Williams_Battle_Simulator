package entity;
import java.util.ArrayList;

public class Team{
    private static final int INITIAL_BUDGET = 400;

    int budget, winCount;
    String name;
    ArrayList<Troop> army; //! Needs to be changed to group object later on

    public Team(String name){
        budget = INITIAL_BUDGET;
        winCount =0;
        army  = new ArrayList<>();
        this.name = name;
    }
}