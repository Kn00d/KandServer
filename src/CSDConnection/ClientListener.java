package CSDConnection;

/**
 * Created by dennisdufback on 2016-03-07.
 */
import com.google.gson.*;
import java.io.*;
import java.net.*;

public class ClientListener extends Thread
{
    private ServerDispatcher mServerDispatcher;
    private ClientInfo mClientInfo;
    private BufferedReader mIn;
    private String query;
    private String action;

    public ClientListener(ClientInfo aClientInfo, ServerDispatcher aServerDispatcher)
            throws IOException
    {
        mClientInfo = aClientInfo;
        mServerDispatcher = aServerDispatcher;
        Socket socket = aClientInfo.mSocket;
        mIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Until interrupted, reads messages from the client socket, forwards them
     * to the server dispatcher's queue and notifies the server dispatcher.
     */
    public void run()
    {
        try {
            while (!isInterrupted()) {

                String jsonString = mIn.readLine();
                if (jsonString == null) {
                    break;
                }
                processMessage(mClientInfo, jsonString);
                mServerDispatcher.dispatchMessage(mClientInfo, query,action);
            }
        } catch (IOException ioex) {
            // Problem reading from socket (communication is broken)
        }

        // Communication is broken. Interrupt both listener and sender threads
        mClientInfo.mClientListener.interrupt();
        mClientInfo.mClientSender.interrupt();
        mServerDispatcher.deleteClient(mClientInfo);

    }

    private void processMessage(ClientInfo mClientInfo, String jsonMessage) {
        JsonParser parser = new JsonParser();
        JsonObject jo = (JsonObject)parser.parse(jsonMessage);
        String context = jo.get("context").getAsString().toLowerCase();
        String action = jo.get("action").getAsString().toLowerCase();
//        System.out.println(jsonMessage);
        if(context.equalsIgnoreCase("mapevent")){
             JsonArray data = (JsonArray)jo.get("data");
            handleMap(action,data);
        } else if(context.equalsIgnoreCase("auth")){
            handleAuth(action);
        }
    }

    private void handleAuth(String what) {
        query = "SELECT NFCid FROM users";
    }

    private void handleMap(String what, JsonArray mapData) {

        JsonObject data = (JsonObject) mapData.get(0);
        String lon;
        String lat;


        if(what.equalsIgnoreCase("add")){
            action = "add";
            lon = data.get("lon").getAsString();
            lat = data.get("lat").getAsString();
            String event = data.get("event").getAsString().toLowerCase();
            query = "INSERT INTO event (lat,lon,event) VALUES ";
            query += "(" + lat + "," + lon + ",'" + event + "')";
        } else if(what.equalsIgnoreCase("delete")){
            action = "delete";
            lon = data.get("lon").getAsString();
            lat = data.get("lat").getAsString();
            query = "DELETE FROM event WHERE (lat,lon) = (" + lat + "," + lon + ")";
        } else if(what.equalsIgnoreCase("update")){
            action = "update";
            query = "SELECT * FROM EVENT";
        }else if(what.equalsIgnoreCase("get")){
            action = "get";
            query = "SELECT NFCid FROM users";
        }
//        switch (action) {
//            case "add":
//                lon = data.get("lon").getAsString();
//                lat = data.get("lat").getAsString();
//                String event = data.get("event").getAsString().toLowerCase();
//                query = "INSERT INTO event (lat,lon,event) VALUES ";
//                query += "(" + lat + "," + lon + ",'" + event + "')";
//
//                break;
//            case "delete":
//                lon = data.get("lon").getAsString();
//                lat = data.get("lat").getAsString();
//                query = "DELETE FROM event WHERE (lat,lon) = (" + lat + "," + lon + ")";
//                break;
//            case "update":
//                query = "SELECT * FROM EVENT";
//                // mServerDispatcher.dispatchToAll = true;
//                break;
//
//        }
//        System.out.println("query: " + query);
    }

}