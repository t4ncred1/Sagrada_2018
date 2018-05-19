package it.polimi.ingsw.serverPart.netPart_container;


import it.polimi.ingsw.serverPart.MatchController;
import it.polimi.ingsw.serverPart.MatchHandler;
import it.polimi.ingsw.serverPart.custom_exception.DisconnectionException;
import it.polimi.ingsw.serverPart.custom_exception.InvalidOperationException;
import it.polimi.ingsw.serverPart.custom_exception.InvalidUsernameException;
import it.polimi.ingsw.serverPart.custom_exception.ReconnectionException;

import java.io.*;
import java.net.Socket;

public class SocketUserAgent extends Thread implements ClientInterface {

    private Socket socket;
    private MatchController currentMatch;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private String username;

    public SocketUserAgent(Socket client) {
        this.socket=client;
        try{
            this.inputStream= new DataInputStream(socket.getInputStream());
            this.outputStream= new DataOutputStream(socket.getOutputStream());
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }


    @Override
    public void run(){
        System.out.println("Connection request received");
        try {
            MatchHandler.login(this);
            System.out.println("Connection protocol ended. Connected");
            try {
                outputStream.writeUTF("logged");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (InvalidOperationException e) {
            System.out.println("Connection protocol ended. Server is full");
            e.printStackTrace();
            try {
                outputStream.writeUTF("notLogged_server_full");
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            return;
        } catch (DisconnectionException e) {
            System.out.println("Connection protocol ended. Client disconnected.");
            e.printStackTrace();
            return;
        }

    }

    @Override
    public boolean isConnected() {
        try{
            outputStream.writeUTF("");
            return true;
        }
        catch (IOException e){
            System.out.println("Disconnected");
            return false;
        }
    }

    //Observer
    public String getUsername(){
        return new String(this.username);
    }


    //-----------------------------------------------------------------------------
    //                             funzioni per login
    //-----------------------------------------------------------------------------
    @Override
    public void chooseUsername() throws DisconnectionException {
        final String chooseUsername = new String("login");
        try {
            outputStream.writeUTF(chooseUsername);
        } catch (IOException e) {
            throw new DisconnectionException();
        }
    }

    @Override
    public void arrangeForUsername(int trial) throws InvalidOperationException, DisconnectionException, ReconnectionException, InvalidUsernameException {
        final String notAvailableMessage = new String("notLogged_username_not_available");

        try {
            if(trial>1) outputStream.writeUTF(notAvailableMessage);
            username= inputStream.readLine();
            System.out.println("received: " + username);
        } catch (IOException e) {
            throw new DisconnectionException();
        }
        MatchHandler.getInstance().requestUsername(username);

        System.out.println(username);
    }




}