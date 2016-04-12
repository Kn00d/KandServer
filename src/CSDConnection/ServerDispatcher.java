package CSDConnection;
/**
 * Created by dennisdufback on 2016-03-07.
 */

import java.util.*;

public class ServerDispatcher extends Thread
{
    private Vector<ClientInfo> mClients = new Vector<ClientInfo>();
    private LinkedList<String> messageQueue = new LinkedList<String>();
    private HashMap<UUID, ClientInfo> clients = new HashMap<>();
    private UUID currentClient;
    public boolean dispatchToAll = false;
    public String action;

    /**
     * Adds given client to the server's client list.
     */
    public synchronized void addClient(ClientInfo aClientInfo)
    {
//        mClients.add(aClientInfo);
//        client.add(aClientInfo);
        UUID id = UUID.randomUUID();
        clients.put(id,aClientInfo);
        aClientInfo.setId(id);
    }


    /**
     * Deletes given client from the server's client list
     * if the client is in the list.
     */
    public synchronized void deleteClient(ClientInfo aClientInfo)
    {
        int clientIndex = mClients.indexOf(aClientInfo);
        if (clientIndex != -1)
            mClients.removeElementAt(clientIndex);
    }

    /**
     * Adds given message to the dispatcher's message queue and notifies this
     * thread to wake up the message queue reader (getNextMessageFromQueue method).
     * dispatchMessage method is called by other threads (ClientListener) when
     * a message is arrived.
     */
    public synchronized void dispatchMessage(ClientInfo aClientInfo, String aMessage, String action)
    {
//        Socket socket = aClientInfo.mSocket;
//        String senderIP = socket.getInetAddress().getHostAddress();
//        String senderPort = "" + socket.getPort();
//        aMessage = senderIP + ":" + senderPort + " : " + aMessage;
        currentClient = aClientInfo.getId();
        messageQueue.add(aMessage);
        this.action = action;
        notify();
    }


    /**
     * @return and deletes the next message from the message queue. If there is no
     * messages in the queue, falls in sleep until notified by dispatchMessage method.
     */
    private synchronized String getNextMessageFromQueue()
            throws InterruptedException
    {

        while(messageQueue.size()==0){
            wait();
        }
        String message = messageQueue.get(0);
        messageQueue.remove(message);
        return message;
    }

    /**
     * Sends given message to all clients in the client list. Actually the
     * message is added to the client sender thread's message queue and this
     * client sender thread is notified.
     */
    private synchronized void sendMessageToAllClients(String aMessage)
    {
        for (ClientInfo clientInfo : clients.values()) {
            clientInfo.mClientSender.sendMessage(aMessage);
        }
        dispatchToAll = false;
    }

    /**
     * Infinitely reads messages from the queue and dispatch them
     * to all clients connected to the server.
     */
    public void run()
    {
        try {
            while (true) {
                String message = getNextMessageFromQueue();
                System.out.println("message to send: " + message);
                if(dispatchToAll){
//                    System.out.println("?");
                    sendMessageToAllClients(message);
                } else {
//                    System.out.println("!");
                    //Send to current Client
                    clients.get(currentClient).mClientSender.sendMessage(message);
                }


            }
        } catch (InterruptedException ie) {
            // Thread interrupted. Stop its execution
        }
    }

}