package it.polimi.ingsw.server;

import it.polimi.ingsw.server.configurations.ConfigurationHandler;
import it.polimi.ingsw.server.custom_exception.NotValidConfigPathException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameTimer {

    private int secondsPassed = 0;
    private int minutesPassed = 0;
    private long timerSet;
    private long timeForGridsInitialization=30;
    private MatchController gameHandled;
    private boolean gameTimeout;
    private boolean stopped;

    private final int SECOND_TO_MINUTE_RATIO = 60;

    private static final String GAME_START_TIMER = "game";
    private long timerToStart=15;
    private static final String OPERATION_TIMER = "operation";
    private long timerForOperation=10;
    private static final String GRID_CHOOSE_TIMER="initialization";

    private Timer timer = new Timer();

    /**
     * Constructor for GameTimer.
     *
     * @param message Type of the timer (game start).
     */
    public GameTimer(String message){
        try {
            timerToStart=ConfigurationHandler.getInstance().getTimerBeforeMatch();
        } catch (NotValidConfigPathException e) {
            System.err.println("Configuration file wasn't read correctly.");
        }
        //if there's an error on read from config file, standard time (as declared between globals) will kick in
        switch (message){
            case GAME_START_TIMER:
                timerSet= timerToStart;
                break;
        }
        TimerTask handlerTimerTask = new TimerTask() {
            @Override
            public void run() {
                secondsPassed++;
                if (secondsPassed >= 60) {
                    secondsPassed = secondsPassed % SECOND_TO_MINUTE_RATIO;
                    minutesPassed++;
                }
                System.out.println(minutesPassed + ":" + secondsPassed);
                if ((secondsPassed + minutesPassed * SECOND_TO_MINUTE_RATIO) >= timerSet) {
                    System.out.println("Timeout");
                    MatchHandler.getInstance().notifyTimeout();
                }

            }
        };
        timer.scheduleAtFixedRate(handlerTimerTask,1,1000);
    }

    /**
     * Constructor for GameTimer.
     *
     * @param matchController Match controller.
     * @param message Type of the timer (operation, grid choose).
     */
    public GameTimer(MatchController matchController, String message){
        this.gameTimeout=false;
        this.stopped=false;
        this.gameHandled=matchController;
        try {
            this.timeForGridsInitialization=ConfigurationHandler.getInstance().getTimerToChooseGrids();
            this.timerForOperation=ConfigurationHandler.getInstance().getTimerForOperation();
        } catch (NotValidConfigPathException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Configuration file wasn't read correctly.",e);
        }
        //if there's an error on read from config file, standard time (as declared between globals) will kick in
        switch (message){
            case GRID_CHOOSE_TIMER:
                timerSet= timeForGridsInitialization;
                break;
            case OPERATION_TIMER:
                timerSet=timerForOperation;
                break;
        }
        TimerTask gameTimerTask = new TimerTask() {
            @Override
            public void run() {
                secondsPassed++;
                if (secondsPassed >= 60) {
                    secondsPassed = secondsPassed % SECOND_TO_MINUTE_RATIO;
                    minutesPassed++;
                }
                System.out.println(minutesPassed + ":" + secondsPassed);
                if ((secondsPassed + minutesPassed * SECOND_TO_MINUTE_RATIO) >= timerSet) {
                    System.out.println("Timeout");
                    gameTimeout = true;
                    gameHandled.wakeUpController();
                }

            }
        };
        timer.scheduleAtFixedRate(gameTimerTask,1,1000);
    }

    /**
     * Terminates the timer.
     */
    public void stop() {
        timer.cancel();
        this.stopped=true;
    }

    /**
     *
     * @return True if a timeout event occurred.
     */
    public boolean getTimeoutEvent() {
        return this.gameTimeout;
    }

    /**
     *
     * @return True if timer has been terminated.
     */
    public boolean isStopped() {
        return this.stopped;
    }
}
