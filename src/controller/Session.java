package controller;

import entity.*;
import javax.swing.Timer;
import view.*;

public class Session {
    private static final int TICK_DURATION = 100;

    Team teamA, teamB;
    Round round;
    Timer timer;
    BattlefieldPanel panel;
    
    /*
    starts the session by creating the two teams and initiate the first round
    */
   public void start(){
        teamA = new Team("teamA");
        teamB = new Team("teamB");
        //Creating a new round
        round = new Round(teamA,teamB);
        
        //Start the timer
        timer = new Timer(TICK_DURATION, e->{
            System.out.println("hello");
        });
        timer.start();
        
    }
}
