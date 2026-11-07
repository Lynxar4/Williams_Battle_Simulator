package entity;

import entity.troops.Troop;
import java.util.ArrayList;

public class Team {
    /* ---- PRIVATE VARIABLES ---- */
    int budget, winCount;
    String name;
    ArrayList<Troop> army; // ! Needs to be changed to group object later on

    /* ---- CONSTRUCTORS ---- */
    public Team(String name) {
        budget = 0; // AllocateState supplies the allowance when each round begins.
        winCount = 0;
        army = new ArrayList<>();
        this.name = name;
    }

    /* ---- PUBLIC METHODS ---- */
    // Used when a troop collects gold from the battlefield.
    public void addBudget(int amount) {
        budget += amount;
    }

    public void spendBudget(int amount) {
        if (amount <= 0 || amount > budget) {
            throw new IllegalArgumentException("Spending must be positive and within the team's budget.");
        }
        budget -= amount;
    }

    public void addTroop(Troop troop) {
        army.add(troop);
    }

    public void clearArmy() {
        army.clear();
    }

    /* ---- GETTER METHODS ---- */
    public ArrayList<Troop> getArmy() {
        return army;
    }

    public int getBudget() {
        return budget;
    }

    public String getName() {
        return name;
    }
}
