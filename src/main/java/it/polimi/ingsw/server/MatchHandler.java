package it.polimi.ingsw.server;


import it.polimi.ingsw.server.configurations.ConfigurationHandler;
import it.polimi.ingsw.server.custom_exception.*;
import it.polimi.ingsw.server.net.UserInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatchHandler extends Thread {

    private static MatchHandler instance;

    private Map<String, MatchController> connectedPlayers;
    private static final Object connectedPlayersGuard= new Object();
    private Map<String, MatchController> disconnectedInGamePlayers;
    private static final Object disconnectedInGamePlayersGuard = new Object();
    private static MatchController startingMatch;
    private static final Object startingMatchGuard= new Object();
    private ArrayList<MatchController> startedMatches; //to handle multi-game.
    private Lock startedMatchesGuard;
    private Condition startedMatchesCondition;
    private Lock lock;
    private Condition condition;


    private boolean timeout;
    private int maximumMatchNumber =2;
    private int maxPlayersInGame =4;
    private int minPlayersInGame =2;
    private GameTimer timer;
    private boolean shutdown;


    private Logger logger;
    //these color are used to highlight server log message and they are
    //not supposed in any way to be part of the view.
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";


    /**
     *Constructor of MatchHandler
     *
     */
    private MatchHandler(){
        try {
            maxPlayersInGame =ConfigurationHandler.getInstance().getMaxPlayersNumber();
        } catch (NotValidConfigPathException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.CONFIG,"Failed retrieving Max Players Number");
        }
        try {
            minPlayersInGame =ConfigurationHandler.getInstance().getMinPlayersNumber();
        } catch (NotValidConfigPathException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.CONFIG,"Failed retrieving Min Players Number");
        }
        connectedPlayers= new HashMap<>();
        lock= new ReentrantLock();
        condition= lock.newCondition();
        startedMatches= new ArrayList<>();
        disconnectedInGamePlayers= new HashMap<>();
        timeout=true;
        shutdown= false;
        startedMatchesGuard = new ReentrantLock();
        startedMatchesCondition= startedMatchesGuard.newCondition();
    }
    /**
     * Getter for MatchHandler.
     *
     * @return MatchHandler.
     */
    public static MatchHandler getInstance(){
        if(instance==null) {
            instance = new MatchHandler();
        }
        return instance;
    }

    /**
     * Puts a client in the game queue.
     *
     * @param username Client's username.
     * @param game Game controller.
     */
    public void setPlayerInGame(String username, MatchController game) {
        synchronized (connectedPlayersGuard){
            connectedPlayers.put(username, game);
        }
    }


    //Observer

    /**
     *
     * @return An integer containing the number of players in the match.
     */
    public int connectedPlayers(){
        synchronized (connectedPlayersGuard) {
            return connectedPlayers.size();
        }
    }

    /**
     *
     * @return An integer containing the maximum number of matches handled.
     */
    public int getMaximumMatchNumber(){
        return maximumMatchNumber;
    }


    //

    /**
     * Wake up a thread after a timeout event.
     */
    public void notifyTimeout() {
        instance.timeout=true;
        lock.lock();
        condition.signal();
        lock.unlock();
    }

    /**
     * Wake up a thread after a notification of starting match.
     */
    public void notifyMatchCanStart() {
        lock.lock();
        condition.signal();
        lock.unlock();
    }

    @Override
    public void run() {
        logger = Logger.getLogger(MatchHandler.class.getName());
        try {
            maximumMatchNumber=ConfigurationHandler.getInstance().getNumberOfMatchHandled();
        } catch (NotValidConfigPathException e) {
            logger.log(Level.SEVERE, "Can't load maximum match number", e);
        }
        logger.log(Level.INFO,"MatchHandlerStarted");
        while (!shutdown) {
            boolean ok;
            loadNewGame();
            do{
                ok= setUpPhase();
            }while (!ok);
            timeout=true;
            do {
                ok=startGameCountdown();
            }
            while (!ok);
            startedMatchesGuard.lock();
            logger.log(Level.INFO,"Game started, actually handling {0} game(s)",startedMatches.size());
            while(startedMatches.size()>=maximumMatchNumber){
                try {
                    startedMatchesCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.log(Level.INFO,"A game ended, actually handling {0} game(s)",startedMatches.size());
            }
            startedMatchesGuard.unlock();

        }
    }

    /**
     * Load a new game.
     */
    private void loadNewGame(){
        logger.log(Level.CONFIG, "A new game has been loaded");
        startingMatch = new MatchController();
        startingMatch.start();
    }

    /**
     *
     * @return True if setUpPhase has ended.
     */
    private boolean setUpPhase(){
        boolean result;
        try {
            lock.lock();
            condition.await();
        } catch (InterruptedException e) {
            logger.severe("Interrupted");
            Thread.currentThread().interrupt();
        }finally {
            lock.unlock();
        }
        synchronized (startingMatchGuard){
            result =(startingMatch.playerInGame()>= minPlayersInGame);
        }
        return result;
    }

    /**
     *
     * @return True if match is starting.
     */
    //@requires timeout=true (*at first execution*);
    private boolean startGameCountdown() {
        lock.lock();
        try {
            if(instance.timeout){
                System.out.println("A game will start soon...");
                timer =new GameTimer("game");
                timeout=false;
            }
            synchronized (startingMatchGuard){
                startingMatch.setGameToStartingSoon(timeout);
            }
            condition.await();
            synchronized (startingMatchGuard) {
                if (startingMatch.playerInGame() == maxPlayersInGame) {
                    startingMatch.setGameToStarted();
                    startedMatchesGuard.lock();
                    startedMatches.add(startingMatch);
                    startedMatchesGuard.unlock();
                    startingMatch = null;
                    timer.stop();
                    return true;
                } else if (instance.timeout && startingMatch.playerInGame() >= minPlayersInGame) {
                    startingMatch.setGameToStarted();
                    instance.timeout = false;
                    startedMatchesGuard.lock();
                    startedMatches.add(startingMatch);
                    startedMatchesGuard.unlock();
                    startingMatch = null;
                    timer.stop();
                    return true;

                }
                else if (instance.timeout && startingMatch.playerInGame() < minPlayersInGame){
                    timer.stop();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } finally {
            lock.unlock();
        }
        return false;
    }

    /**
     *
     * @param client Client trying to log in.
     * @throws InvalidOperationException See requestUsername method.
     * @throws DisconnectionException See arrangeForUsername doc in UserInterface class.
     * @throws InvalidUsernameException See requestUsername method.
     * @throws ReconnectionException See requestUsername method.
     */
    public void login(UserInterface client) throws InvalidOperationException, DisconnectionException, InvalidUsernameException, ReconnectionException {
        client.chooseUsername();
        try {
            client.arrangeForUsername();
            lock.lock();
            synchronized (startingMatchGuard) {
                startingMatch.insert(client);
            }
            condition.signal();
            lock.unlock();
            System.out.println(ANSI_BLUE +client.getUsername()+ " connected successfully." + ANSI_RESET);
        }
        catch (ReconnectionException e){
            MatchController game;
            synchronized (disconnectedInGamePlayersGuard) {
                game = disconnectedInGamePlayers.remove(client.getUsername());
            }
            synchronized (connectedPlayersGuard){
                connectedPlayers.put(client.getUsername(), game);
                System.out.println(ANSI_BLUE+ client.getUsername() + " reinserted in game #"+startedMatches.indexOf(game) +ANSI_RESET);
            }
            game.handleReconnection(client);
            throw new ReconnectionException();
        }
    }

    /**
     * Checks username chosen by client.
     *
     * @param username Username chosen by client.
     * @throws InvalidOperationException Thrown when match is not starting.
     * @throws ReconnectionException Thrown when this client is trying to reconnect after a disconnection.
     * @throws InvalidUsernameException Thrown when this username is already used.
     */
    public void requestUsername(String username) throws InvalidOperationException, ReconnectionException, InvalidUsernameException {
        if(username.equals("")) throw new InvalidUsernameException();
        synchronized (startingMatchGuard) {
            synchronized (disconnectedInGamePlayersGuard) {
                for (Map.Entry<String,MatchController> client: disconnectedInGamePlayers.entrySet()) {
                    if (client.getKey().equals(username)) {
                        throw new ReconnectionException();
                    }
                }
                if (startingMatch == null) {
                    throw new InvalidOperationException();
                }
                else
                    startingMatch.updateQueue();
            }

            synchronized (connectedPlayersGuard) {
                if (connectedPlayers.containsKey(username)) {
                    throw new InvalidUsernameException();
                }
                connectedPlayers.put(username, null);
            }
        }

    }

    /**
     *
     * @param username Player disconnected.
     */
    public void notifyAboutDisconnection(String username) {
        MatchController gameHandlingPlayer;
        synchronized (connectedPlayersGuard) {
            gameHandlingPlayer=connectedPlayers.remove(username);
        }
        if (gameHandlingPlayer == null) {
            //I don't have to do nothing. Players wasn't in a started game so he should be just removed from connected players
        } else {
            synchronized (disconnectedInGamePlayersGuard){
                if(!disconnectedInGamePlayers.containsKey(username)) disconnectedInGamePlayers.put(username, gameHandlingPlayer);
            }
        }

    }

    /**
     *
     * @param client Client trying to logout.
     * @throws InvalidOperationException See remove doc in MatchController class.
     */
    public void logOut(UserInterface client) throws InvalidOperationException {
        String username= client.getUsername();
        MatchController gameHandlingClient;
        synchronized (connectedPlayersGuard){
            gameHandlingClient = connectedPlayers.remove(username);
        }
        synchronized (startingMatchGuard) {
            if (gameHandlingClient!=null) {
                synchronized (disconnectedInGamePlayersGuard) {
                    disconnectedInGamePlayers.put(username, gameHandlingClient);

                    //FIXME if necessary.
                }
            }
            else
                startingMatch.remove(client);
        }
        System.out.println(ANSI_PURPLE+ username + " logged out."+ ANSI_RESET);

    }

    public void notifyEndGame(MatchController matchController) {
        startedMatchesGuard.lock();
        startedMatches.remove(matchController);
        startedMatchesCondition.signal();
        startedMatchesGuard.unlock();
        synchronized (connectedPlayersGuard){
            connectedPlayers.entrySet().removeIf(player -> player.getValue().equals(matchController));
        }
        synchronized (disconnectedInGamePlayersGuard){
            disconnectedInGamePlayers.entrySet().removeIf(player -> player.getValue().equals(matchController));
        }
    }
}
