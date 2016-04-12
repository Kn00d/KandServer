package Server;


import CSDConnection.ClientInfo;
import Database.Processor;

import java.io.*;
import java.net.Socket;

/**
 * Created by Fredrik on 2016-03-16.
 */
public class ServerThread extends Thread {
    protected Socket socket;
    protected ClientInfo clientInfo;
    Processor processor;

    BufferedReader in;
    PrintWriter out;

    public ServerThread(Socket clientSocket) {
        this.socket = clientSocket;
//        clientInfo = new ClientInfo(clientSocket);
        in = null;
        out = null;
    }

    public void run() {

        try {
            System.out.println("Connection received from: " + socket.getInetAddress().getHostAddress());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter( new OutputStreamWriter(socket.getOutputStream()));

            String clientMessage = null;

            do {
                if (in != null) {
                    clientMessage = in.readLine();
                }
                System.out.println(socket.getInetAddress().getHostAddress() + "> " + clientMessage);
                if (!clientMessage.equalsIgnoreCase("bye")) {
                    processor = new Processor(clientInfo, clientMessage);
                    String reply = processor.processMessage(clientMessage);
                    System.out.println("Send to client> " + reply);
                    out.println(reply);
                    out.flush();
                }
            } while (!clientMessage.equalsIgnoreCase("bye"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Thread ID: " + this.getId());
            closeConnection();
        }
    }

    /**
     * Closes socket and end connection
     */
    private void closeConnection() {
        try {
            socket.close();
            in.close();
            out.close();
            System.out.println("Connection to " + socket.getInetAddress().getHostAddress() + " closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}