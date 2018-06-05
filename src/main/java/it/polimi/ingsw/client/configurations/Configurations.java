package it.polimi.ingsw.client.configurations;

public class Configurations {
private int RmiPort;
private int ServerIp;
private int SocketPort;
private String RegisterName;

    public Configurations(){
    }

    public Configurations(int rmiPort, int serverIp, int socketPort, String registerName){
        this.RmiPort=rmiPort;
        this.ServerIp=serverIp;
        this.SocketPort=socketPort;
        this.RegisterName=registerName;
    }

    public int getRmiPort() {
        return RmiPort;
    }

    public int getServerIp() {
        return ServerIp;
    }

    public int getSocketPort() {
        return SocketPort;
    }

    public String getRegisterName() {
        return RegisterName;
    }
}
