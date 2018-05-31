package it.polimi.ingsw.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRemoteInterface extends Remote {
    void isConnected() throws RemoteException;

    void chooseUsername() throws RemoteException;

    String requestAUsername() throws RemoteException;

    void notifyGameStarting() throws RemoteException;

    void notifyStartedGame() throws RemoteException;

    void notifyReconnection() throws RemoteException;
}