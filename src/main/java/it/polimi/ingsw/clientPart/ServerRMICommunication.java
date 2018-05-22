package it.polimi.ingsw.clientPart;

import it.polimi.ingsw.serverPart.custom_exception.InvalidOperationException;
import it.polimi.ingsw.serverPart.netPart_container.ServerRemoteInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerRMICommunication implements ServerCommunicatingInterface {

    private transient ServerRemoteInterface stub;
    private transient ClientRMI thisClient;


    private static String registerName = "MatchHandler";
    private static int serverPort = 11001;
    private static String serverAddress="127.0.0.1";

    private transient boolean startingGame;
    private transient boolean gameStarted;

    public ServerRMICommunication(){
        try {
            Registry registry = LocateRegistry.getRegistry(serverAddress,serverPort);
            stub = (ServerRemoteInterface) registry.lookup(registerName);
            thisClient=new ClientRMI();
            thisClient.setRMICommunication(this);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void login() {
        try {
            stub.login(thisClient);
            System.out.println("You successfully logged. You have been inserted in game queue.");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InvalidOperationException e) {
            System.err.println("Server is now full, retry later.");
            System.exit(0);
        }

    }

    @Override
    public boolean waitForGame() {
        if(!startingGame&&!gameStarted){
            return false;
        }
        else{
            while (!gameStarted){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public boolean logout() {
        Boolean ok=false;
        try{
            stub.logout(thisClient);
            ok=true;
        } catch (InvalidOperationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            //TODO
            e.printStackTrace();
        }
        return ok;

    }

    public void notifyStarting() {
        if(!startingGame)System.out.println("A game will start soon...");
        this.startingGame=true;
    }

    public void notifyStarted() {
        if(!gameStarted)System.out.println("Game started");
        this.gameStarted=true;
        this.startingGame=false;
    }
}
