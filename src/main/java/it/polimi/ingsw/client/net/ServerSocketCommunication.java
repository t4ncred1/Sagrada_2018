package it.polimi.ingsw.client.net;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.polimi.ingsw.client.MainClient;
import it.polimi.ingsw.client.Proxy;
import it.polimi.ingsw.client.configurations.ConfigHandler;
import it.polimi.ingsw.client.configurations.DataRetriever;
import it.polimi.ingsw.client.custom_exception.*;
import it.polimi.ingsw.client.custom_exception.invalid_operations.*;
import it.polimi.ingsw.server.custom_exception.DisconnectionException;
import it.polimi.ingsw.server.custom_exception.NotValidConfigPathException;
import it.polimi.ingsw.server.custom_exception.ReconnectionException;
import it.polimi.ingsw.server.model.components.Die;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;

public class ServerSocketCommunication extends Thread implements ServerCommunicatingInterface {


    private transient Socket socket;
    private transient DataInputStream inputStream;
    private transient DataOutputStream outputStream;

    private int serverPort = 11000;
    private String serverAddress="127.0.0.1";

    private Lock lock;
    private Condition condition;
    private Logger logger;
    private static final long SLEEP_TIME = 50;

    private static final String PING_MESSAGE= "";
    private static final String HELLO_MESSAGE = "hello";

    private static final String LOGIN_MESSAGE_FROM_SERVER = "login";
    private static final String SUCCESSFULLY_LOGGED = "logged";
    private static final String SERVER_FULL = "notLogged_server_full";
    private static final String USERNAME_NOT_AVAILABLE= "notLogged_username_not_available";
    private static final String RECONNECTED ="reconnected";


    private static final String LAUNCHING_GAME = "launching_game";
    private static final String GAME_STARTED = "game_started";

    private static final String TRY_LOGOUT= "try_logout";
    private static final String SUCCESSFULLY_LOGGED_OUT= "logged_out";

    private static final String OK_MESSAGE="ok";
    private static final String NOT_OK_MESSAGE= "retry";
    private static final String REQUEST_GRID = "get_grids";
    private static final String GRID_ALREADY_SELECTED= "grid_selected";
    private static final String CHOOSE_GRID="set_grid";


    private static final String TURN_PLAYER = "turn_player";
    private static final String GAME_FINISHED= "finished";


    private static final String INSERT_DIE = "insert_die";
    private static final String USE_TOOL_CARD= "tool_card";
    private static final String EXECUTE_TOOL_CARD = "execute_tool";
    private static final String INVALID_POSITION = "invalid_index";
    private static final String ALREADY_DONE_OPERATION = "already_done";
    private static final String NOT_ENOUGH_TOKENS = "not_tokens";
    private static final String END_TURN ="end_turn";


    private static final String END_DATA= "end_data";
    private static final String DISCONNECTION = "disconnected";

    private static final String PLAYERS_POINTS="points";


    private boolean myGridSet;
    private boolean gameFinished;
    private boolean doneOperation;

    private static final String DEFAULT_LOG_DIR = "src/main/resources/client_log/ClientLog_%u.log";

    private Handler handler;

    /**
     * Constructor for ServerSocketCommunication.
     */
    public ServerSocketCommunication(){
        this.lock = new ReentrantLock();
        this.condition= lock.newCondition();
        logger = Logger.getLogger(ServerSocketCommunication.class.getName());
        boolean succeeded=true;
        String LOG_DIR = new File(MainClient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getAbsolutePath()+"/resources/client_log/ClientLog_%u.log";
        try {
            handler = new FileHandler(LOG_DIR);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            logger.setLevel(Level.FINER);
            logger.addHandler(handler);

        } catch (IOException e) {
            logger.log(Level.CONFIG, "Unable to get log directory: "+ LOG_DIR+", trying default directory "+ DEFAULT_LOG_DIR);
            succeeded=false;
        }
        if(!succeeded){
            try {
                handler = new FileHandler(DEFAULT_LOG_DIR);
                SimpleFormatter formatter = new SimpleFormatter();
                handler.setFormatter(formatter);
                logger.setLevel(Level.FINER);
                logger.addHandler(handler);
            } catch (IOException e) {
                logger.log(Level.CONFIG, "No log file found in "+DEFAULT_LOG_DIR,e);
            }
            logger.log(Level.FINEST,"correctly got loggers at "+DEFAULT_LOG_DIR);
        }
        myGridSet =false;
        gameFinished=false;
        doneOperation=false;
    }

    @Override
    public void run(){

        try {
            handleWaitForGame();
            handleGameStarting();
            handleGridSetting();
            handleGameLogic();
        } catch (IOException e) {
            logger.log(Level.WARNING,"Can't connect to server, something went wrong!");
            System.exit(-1);
        } finally {
            handler.close();
        }

    }

    /**
     * Handle a user's grid selection.
     *
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleGridSetting() throws IOException {
        try {
            setGridSelectionInProxy();
            waitForGridSelection();
        } catch (GameInProgressException e) {
            logger.log(Level.CONFIG, "grids already selected", e);
        }
    }

    /**
     * Handles game logic (turn initialization, turn logic).
     *
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleGameLogic() throws IOException {
        do{
            String serverResponse= readRemoteInput();
            switch (serverResponse){
                case TURN_PLAYER:
                    serverResponse=readRemoteInput();
                    logger.log(Level.FINE, "turn player received from player: {0}",serverResponse);
                    Proxy.getInstance().setTurnPlayer(serverResponse); //todo throw an exception if necessary
                    handleTurnInitialization();
                    MainClient.getInstance().notifyTurnUpdated();
                    handleTurn(serverResponse.equals(Proxy.getInstance().getMyUsername()));
                    break;
                case GAME_FINISHED:
                    logger.log(Level.FINE, "{0}",serverResponse);
                    logger.log(Level.FINE, "turn player received from player: {0}",serverResponse);
                    Proxy.getInstance().setGameToFinished();
                    gameFinished=true;
                    MainClient.getInstance().notifyTurnUpdated();
                    break;
                default:
                    logger.log(Level.SEVERE,"Unexpected game status from server: {0}", serverResponse);
            }
        }while(!gameFinished);
        handleGameEnd();
    }

    /**
     * Handles match end logic (ranks, etc).
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleGameEnd() throws IOException {
        String serverMessage = readRemoteInput();
        if(!serverMessage.equals(PLAYERS_POINTS)) logger.log(Level.SEVERE,"Unexpected data type from server: {0}", serverMessage);
        else logger.log(Level.FINE,"Server will send points in the next stream: {0}", serverMessage);
        Gson gson= new Gson();
        serverMessage=readRemoteInput();
        TypeToken<LinkedHashMap<String,String>> typeToken= new TypeToken<LinkedHashMap<String,String>>(){};
        LinkedHashMap<String,String> playerPoints= gson.fromJson(serverMessage,typeToken.getType());
        Proxy.getInstance().setPoints(playerPoints);
        logger.log(Level.FINE,"Points received and set");
        MainClient.getInstance().notifyEndDataInProxy();
    }

    /**
     * Handles turn logic (operations, data retrieving).
     *
     * @param myTurn True if this is "player"'s turn.
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleTurn(boolean myTurn) throws IOException {
        boolean turnFinished=false;
        while(!turnFinished){
            if(myTurn){
                waitForAnOperation();
            }
            turnFinished= handleDataRetrieving();
            if(myTurn){
                MainClient.getInstance().notifyDataRetrieved();
            } else{
                if(!turnFinished) MainClient.getInstance().notifySomethingChanged();
                else MainClient.getInstance().notifyEndTurn();
            }
        }
    }

    private void waitForAnOperation() {
        while(!doneOperation){
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lock.unlock();
        }
        doneOperation=false;
    }

    /**
     * Handles the data retrieving.
     *
     * @return True if data retrieving is fine.
     * @throws IOException Thrown when an I/O error occurs.
     */
    private boolean handleDataRetrieving() throws IOException {
        boolean endData=false;
        do{
            String dataType=readRemoteInput();
            switch (dataType){
                case DISCONNECTION:
                    logger.log(Level.FINE,"Server notified about a disconnection");
                    Proxy.getInstance().setPlayerToDisconnected();
                    break;
                case END_TURN:
                    logger.fine("End turn notified by server");
                    return true;
                case END_DATA:
                    logger.log(Level.FINE,"End data notified by server");
                    endData=true;
                    break;
                default:
                    DataRetriever.retrieve(dataType, inputStream, logger);
            }
        }while (!endData);
        return false;
    }

    private void waitForGridSelection() {
        while (!myGridSet){
            try {
                lock.lock();
                condition.await();
                lock.unlock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Handles turn initialization.
     *
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleTurnInitialization() throws IOException {
        boolean ok=false;
        do{
            String serverResponse= readRemoteInput();
            if (END_DATA.equals(serverResponse)) {
                logger.log(Level.FINE, "End data notified by server");
                ok = true;
            } else {
                DataRetriever.retrieve(serverResponse, inputStream, logger);
            }
        }while (!ok);
        MainClient.getInstance().setGameToInitialized();
    }

    /**
     * Sets user's grid selection.
     *
     * @throws IOException Thrown when an I/O error occurs.
     * @throws GameInProgressException Thrown when the game is already started.
     */
    private void setGridSelectionInProxy() throws IOException, GameInProgressException {
        String serverResponse;
        String logUnexpectedResponse= "Unexpected response: {0}";
        do {
            logger.log(Level.FINE,"sent a grid request to server");
            outputStream.writeUTF(REQUEST_GRID);
            serverResponse=readRemoteInput();
            if(serverResponse.equals(NOT_OK_MESSAGE)) logger.log(Level.SEVERE, "Invalid request from this client to server");
            else if(!serverResponse.equals(OK_MESSAGE))logger.log(Level.SEVERE, logUnexpectedResponse, serverResponse);
            else logger.log(Level.FINE,"Grid request accepted by server");
        }while (serverResponse.equals(NOT_OK_MESSAGE));
        serverResponse=readRemoteInput();
        if (GRID_ALREADY_SELECTED.equals(serverResponse)) {
            logger.log(Level.FINE, "Server notified you already selected grids in a session before this one");
            MainClient.getInstance().setGridsAlreadySelected(true);
            throw new GameInProgressException();
        } else {
            DataRetriever.retrieve(serverResponse, inputStream, logger);
        }
    }

    /**
     * Handles game starting phase.
     *
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleGameStarting() throws IOException {
        boolean ok=false;
        String serverResponse;
        do{
            serverResponse=readRemoteInput();
            switch (serverResponse){
                case LAUNCHING_GAME:
                    logger.log(Level.FINE, "Game countdown restarted: {0} ",serverResponse);
                    MainClient.getInstance().notifyGameStarting();
                    break;
                case GAME_STARTED:
                    logger.log(Level.FINE, "Received game started from server: {0}",serverResponse);
                    MainClient.getInstance().notifyGameStarted();
                    ok=true;
                    break;
                default:
                    logger.log(Level.SEVERE, "Unexpected response from server ({0}) while waiting game start",serverResponse);
            }
        }while (!ok);
    }

    /**
     * Handles the 'waitForAMatch' phase.
     * @throws IOException Thrown when an I/O error occurs.
     */
    private void handleWaitForGame() throws IOException {
        String serverResponse;
        boolean ok=false;
        do{
            sleepALittle();
            lock.lock();
            if(inputStream.available()>0) {
                serverResponse = inputStream.readUTF();
                if(serverResponse.equals(LAUNCHING_GAME)){
                    ok=true;
                    logger.log(Level.FINE, "Received game starting from server");
                }else if(!serverResponse.equals(PING_MESSAGE)){
                    logger.log(Level.SEVERE, "Unexpected message from server");
                }
            }
            lock.unlock();
        }while (!ok);
        MainClient.getInstance().notifyGameStarting();
    }

    private void sleepALittle() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sets the connection.
     *
     * @throws ServerIsDownException Thrown if the server is down.
     */
    public void setUpConnection() throws ServerIsDownException {
        try {
            getParametersFromConfigurations();
            socket= new Socket(serverAddress, serverPort);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream= new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF(HELLO_MESSAGE);
        } catch (IOException e) {
            throw new ServerIsDownException();
        }
    }

    /**
     * Gets server IP and socket port from config files.
     */
    private void getParametersFromConfigurations() {
        try {
            serverPort = ConfigHandler.getInstance().getSocketPort();
            serverAddress = ConfigHandler.getInstance().getServerIp();
        } catch (NotValidConfigPathException e) {
            logger.log(Level.CONFIG,"Wrong configuration file, using defaults.");
        }
    }

    /**
     * Handles the login.
     *
     * @param username Username chosen.
     * @throws ServerIsFullException Thrown if the server is full.
     * @throws InvalidUsernameException Thrown when the username is not valid (already used).
     * @throws ServerIsDownException Thrown when the server is down.
     * @throws ReconnectionException Thrown if the user successfully reconnected.
     */
    public void login(String username) throws ServerIsFullException, InvalidUsernameException, ServerIsDownException, ReconnectionException {
        String loginResponse;
        try {
            waitForLoginMessageFromServer();
            outputStream.writeUTF(username);
            logger.log(Level.FINE,"sent username to server: {0}", username);
            boolean ok=false;
            while (!ok) {
                loginResponse=readRemoteInput();
                switch (loginResponse) {
                    case SERVER_FULL:
                        logger.log(Level.FINE, "Server is full");
                        throw new ServerIsFullException();
                    case USERNAME_NOT_AVAILABLE:
                        logger.log(Level.CONFIG, "Username not available");
                        throw new InvalidUsernameException();
                    case SUCCESSFULLY_LOGGED:
                        logger.log(Level.FINE, "Connected successfully");
                        Proxy.getInstance().setMyUsername(username);
                        this.start();
                        ok=true;
                        break;
                    case RECONNECTED:
                        logger.log(Level.FINE, "Reconnected successfully");
                        Proxy.getInstance().setMyUsername(username);
                        this.start();
                        throw new ReconnectionException();
                    default:
                        logger.log(Level.SEVERE, "Unexpected login response from server: {0}", loginResponse);
                }
            }
        }
        catch (IOException e){
            throw new ServerIsDownException();
        }
    }

    private void waitForLoginMessageFromServer() throws IOException {
        String loginResponse;
        boolean ok=false;
        do {
            loginResponse = inputStream.readUTF();
            if(loginResponse.equals(LOGIN_MESSAGE_FROM_SERVER)) {
                logger.log(Level.FINE, "Received a login request from server");
                ok=true;
            } else {
                logger.log(Level.SEVERE, "Server did not send a login request: {0}",loginResponse);
            }
        }
        while (!ok);
    }

    @Override
    public void insertDie(int position, int column, int row) throws ServerIsDownException, InvalidMoveException, DieNotExistException, AlreadyDoneOperationException, DisconnectionException {
        try {
            outputStream.writeUTF(INSERT_DIE);
            logger.log(Level.FINE,"sent Insert_die to server");
            outputStream.writeInt(position);
            outputStream.writeInt(column);
            outputStream.writeInt(row);
            String response= readRemoteInput();
            switch (response){
                case OK_MESSAGE:
                    logger.log(Level.FINE,"operation went well");
                    lock.lock();
                    doneOperation=true;
                    condition.signal();
                    lock.unlock();
                    break;
                case NOT_OK_MESSAGE:
                    logger.log(Level.FINE,"NOT_OK_MESSAGE received");
                    throw new InvalidMoveException();
                case INVALID_POSITION:
                    logger.log(Level.FINE,"INVALID_POSITION_IN_DICE_POOL received");
                    throw new DieNotExistException();
                case ALREADY_DONE_OPERATION:
                    logger.log(Level.FINE,"OPERATION_ALREADY_DONE received");
                    throw new AlreadyDoneOperationException();
                case DISCONNECTION:
                    logger.log(Level.FINE,"DISCONNECTION message received");
                    throw new DisconnectionException();
                default:
                    logger.log(Level.SEVERE, "Unexpected response from server: {0}", response);
            }
        } catch (IOException e) {
            throw new ServerIsDownException();
        }
    }

    @Override
    public void endTurn() throws ServerIsDownException, DisconnectionException {
        try {
            outputStream.writeUTF(END_TURN);
            logger.log(Level.FINE,"sent an END_TURN request");
            String response= readRemoteInput();
            if(!response.equals(OK_MESSAGE)){
                if(response.equals(DISCONNECTION)) {
                    logger.log(Level.FINE, "disconnected for inactivity");
                    throw new DisconnectionException();
                }
                else
                    logger.log(Level.SEVERE, "Unexpected message from server: {0}", response);
            } else{
                logger.log(Level.FINE,"turn ended successfully");
            }
            lock.lock();
            doneOperation=true;
            condition.signal();
            lock.unlock();
        } catch (IOException e) {
            throw new ServerIsDownException();
        }
    }

    @Override
    public void doEffect(String effectName, List<String> params) throws ServerIsDownException, DisconnectionException, InvalidMoveException {
        try {
            logger.log(Level.FINE, "Sending effect data");
            outputStream.writeUTF(effectName);
            ArrayList<String> temp= (ArrayList<String>) params;
            Gson gson = new Gson();
            outputStream.writeUTF(gson.toJson(temp));
            logger.log(Level.FINE, "Sent effect name and parameters");
            String response= readRemoteInput();
            switch (response){
                case DISCONNECTION:
                    logger.log(Level.FINE, "disconnected for inactivity");
                    throw new DisconnectionException();
                case OK_MESSAGE:
                    logger.log(Level.FINE,"Effect executed correctly");
                    break;
                case NOT_OK_MESSAGE:
                    logger.log(Level.FINE, "Invalid parameter passed");
                    throw new InvalidMoveException();
                default:
                    logger.log(Level.WARNING,"Unexpected response during effect params setting: {0}",response);
            }
        } catch (IOException e) {
            throw new ServerIsDownException();
        }

    }

    @Override
    public void useToolCard(int i) throws ServerIsDownException, DisconnectionException, ToolCardNotExistException, AlreadyDoneOperationException, NotEnoughTokensException {
        try {
            outputStream.writeUTF(USE_TOOL_CARD);
            outputStream.writeInt(i);
            logger.log(Level.FINE,"Sent use tool card request");
            String response = readRemoteInput();
            switch (response){
                case DISCONNECTION:
                    logger.log(Level.FINE, "disconnected for inactivity");
                    throw new DisconnectionException();
                case OK_MESSAGE:
                    logger.log(Level.FINE,"Server is ready to handle tool card logic");
                    break;
                case NOT_OK_MESSAGE:
                    logger.log(Level.FINE, "Server notified that a tool card have been already used");
                    throw new AlreadyDoneOperationException();
                case INVALID_POSITION:
                    logger.log(Level.FINE, "Server notified a tool card doesn't exist at that index");
                    throw new ToolCardNotExistException();
                case NOT_ENOUGH_TOKENS:
                    logger.log(Level.FINE,"Server notified not enough tokens");
                    throw new NotEnoughTokensException();
                default:
                    logger.log(Level.SEVERE,"Unexpected response during use tool card request: {0}",response);
            }
        } catch (IOException e) {
            throw new ServerIsDownException();
        }
    }

    @Override
    public void launchToolCards() throws ServerIsDownException, DisconnectionException, InvalidMoveException {
        try {
            outputStream.writeUTF(EXECUTE_TOOL_CARD);
            String response= readRemoteInput();
            switch (response){
                case OK_MESSAGE:
                    logger.fine("ToolCard executed without problems");
                    lock.lock();
                    doneOperation=true;
                    condition.signal();
                    lock.unlock();
                    break;
                case NOT_OK_MESSAGE:
                    logger.fine("Tool card can't be executed with these parameters");
                    throw new InvalidMoveException();
                case DISCONNECTION:
                    logger.log(Level.FINE, "disconnected for inactivity");
                    throw new DisconnectionException();
                default:
                    logger.log(Level.SEVERE,"Unexpected message waiting for use tool response {0}" , response);
            }
        } catch (IOException e) {
            throw new ServerIsDownException();
        }
    }

    /**
     * Sends a logout request. Handles the response.
     *
     * @throws ServerIsDownException Thrown if the server is down.
     * @throws GameStartingException Thrown if the game is starting.
     * @throws LoggedOutException Thrown if the user has correctly logged out.
     */
    public void askForLogout() throws ServerIsDownException, GameStartingException, LoggedOutException {
        try{
            lock.lock();
            outputStream.writeUTF(TRY_LOGOUT);
            logger.log(Level.FINE,"sent a logout request");
            String response= readRemoteInput();
            switch (response) {
                case SUCCESSFULLY_LOGGED_OUT:
                    logger.log(Level.FINE,"notified about log out: {0}", response);
                    throw new LoggedOutException();
                case LAUNCHING_GAME:
                    logger.log(Level.FINE,"notified about game starting: {0}", response);
                    throw new GameStartingException();
                default:
                    logger.log(Level.SEVERE,"Unexpected response from server: {0}", response);
            }
            lock.unlock();
        } catch (IOException e) {
            throw new ServerIsDownException();

        }
    }

    /**
     * Gets a message from server.
     *
     * @return A string containing a message received from server.
     * @throws IOException Thrown when an I/O error occurs.
     */
    private String readRemoteInput() throws IOException {
        String read;
        do{
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            read=inputStream.readUTF();
        }while (read.equals(PING_MESSAGE));
        return read;
    }

    /**
     * Handles the grid choice.
     *
     * @param gridIndex Grid's index in the grid selection.
     * @throws ServerIsDownException Thrown if the server is down.
     * @throws DisconnectionException Thrown if the user is trying to disconnect.
     * @throws InvalidIndexException Thrown if 'gridIndex' is not valid.
     */
    public void selectGrid(int gridIndex) throws ServerIsDownException, DisconnectionException, InvalidIndexException {
        try {
            logger.log(Level.FINE,"sent CHOOSE_GRID to server. Parameter: {0}",gridIndex);
            outputStream.writeUTF(CHOOSE_GRID);
            String response= readRemoteInput();
            if(response.equals(DISCONNECTION)){
                logger.log(Level.FINE, "Received unexpected response {0}", response);
                throw new DisconnectionException();
            } else if(!response.equals(OK_MESSAGE)) {
                logger.log(Level.SEVERE, "Received unexpected response {0}", response);
                return;
            }

            outputStream.writeInt(gridIndex);
            response=readRemoteInput();
            if(response.equals(NOT_OK_MESSAGE))
                throw new InvalidIndexException();
            lock.lock();
            myGridSet =true;
            condition.signal();
            lock.unlock();
        } catch (IOException e) {
            throw new ServerIsDownException();
        }
    }
}
