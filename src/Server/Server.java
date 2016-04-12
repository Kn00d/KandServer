package Server;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Fredrik on 2016-03-16.
 *
 * Server class running a multithreaded server
 * Accepts multiple client connections
 */
public class Server {

    static final int PORT = 9000;

    public Server(){}

    public void start() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        SSLServerSocketFactory socketFactory = null;

        try {
            socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            serverSocket = new ServerSocket(PORT);
//            serverSocket = socketFactory.createServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();

        }
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            // new thread for a client
            new ServerThread(socket).start();
        }
    }
}