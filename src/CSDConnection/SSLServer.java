package CSDConnection;

import Database.Processor;
import java.io.*;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.*;

/**
 * Created by dennisdufback on 2016-03-16.
 */
public class SSLServer  {

    public static void main(String[] args) throws Exception {
        int PORT = 9001;
        SSLServerSocket serverSocket = getConnection(PORT);
//        serverSocket.setNeedClientAuth(true);
        System.out.println("Server is up!");
        System.out.println("Waiting for client connection at the port: " + PORT);

        try {
            while (true) {
                new ConnectionListener(serverSocket.accept()).start();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
    protected static SSLServerSocket getConnection(int port) throws IOException{

        try{
            // Load server private key
            KeyStore serverKeys = KeyStore.getInstance("JKS");
            KeyManagerFactory serverKeyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            serverKeys.load(new FileInputStream("/srv/certificates/server/serverkeystore.jks"), "hallonsorbet".toCharArray());
//            serverKeys.load(new FileInputStream("/Users/dennisdufback/Desktop/cert/server/serverkeystore.jks"), "hallonsorbet".toCharArray());
            serverKeyManager.init(serverKeys, "hallonsorbet".toCharArray());

            //Use keys to create socket
            SSLContext context = SSLContext.getInstance("TLS");
//            context.init(serverKeyManager.getKeyManagers(), trustKeyManager.getTrustManagers(), null);
            context.init(serverKeyManager.getKeyManagers(), null, null);

            SSLServerSocketFactory factory = context.getServerSocketFactory();
            return (SSLServerSocket)factory.createServerSocket(port);

        }catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////                                                        ////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    static class ConnectionListener extends Thread {

        private Socket socket;
        Processor processor;

        public ConnectionListener(Socket socketValue) {
            socket = socketValue;
        }

        public void run() {

            try {
                PrintWriter out = null;
                BufferedReader in;
                try {
                    System.out.println("Connection received from: " + socket.getInetAddress().getHostAddress());

                        in = new BufferedReader(new InputStreamReader(
                                socket.getInputStream()));
                        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                    String clientMessage = in.readLine();
                    while (!clientMessage.isEmpty()){
                            processor = new Processor(clientMessage);
                            String reply = processor.processMessage();
                             System.out.println("REPLY: " + reply);
                            out.print(reply);
                            out.flush();
                            clientMessage = "";
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.out.println("Någon försöker snacka plain text? No cert lol");
                }
                if (out != null) {
                    out.close();
                }
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(SSLServer.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }
    }
}