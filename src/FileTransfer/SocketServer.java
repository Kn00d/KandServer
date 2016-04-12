package FileTransfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {

    private static ServerSocket serverSocket;
    private int port;

    public SocketServer(int port){
        this.port = port;
    }
    public void start() throws IOException{
        System.out.println("Starting the socket server at port:" + port);
        serverSocket = new ServerSocket(port);

        Socket client = null;

        while(true){
            System.out.println("Waiting for clients...");
            client = serverSocket.accept();
            System.out.println("The following client has connected:"+client.getInetAddress().getCanonicalHostName());
            //A client has connected to this server. Send welcome message
            Thread thread = new Thread(new CLIENTConnection(client));
            thread.start();
        }
    }

    public static void main(String[] args) throws IOException{
        int portNumber = 9000;
        try{
            SocketServer socketServer = new SocketServer(portNumber);
            socketServer.start();
            System.out.println("Server started.");
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
