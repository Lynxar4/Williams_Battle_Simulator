package controller;

import entity.*;
import view.*;
import controller.states.*;

public class Round{
    Team teamA,teamB;
    GameState currentState ;

    Round(Team teamA,Team teamB){
        this.teamA = teamA;
        this.teamB = teamB;
    }

    public void start(){
        currentState = new AllocateState();
    }

    public void update(){
        currentState.update(this); // State needs access to Round's data
    }

    public Team getTeamA() { return teamA; }
    public Team getTeamB() { return teamB; }


}