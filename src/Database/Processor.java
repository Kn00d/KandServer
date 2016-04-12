package Database;


import CSDConnection.ClientInfo;
import com.google.gson.*;


import java.sql.SQLException;

/**
 * Created by Fredrik on 2016-03-15.
 */
public class Processor {
    private static final int SQL_DUPLICATE_KEY_ERR_CODE = 1062;
    private String jsonRequest;
    private ClientInfo clientInfo;
    private Database database;
    private JsonObject responseObject;


    /**
     *
     * @param clientInfo
     * @param jsonRequest
     */
    public Processor(ClientInfo clientInfo, String jsonRequest) {
//        this.jsonRequest = jsonRequest;
        this.clientInfo = clientInfo;
        database = new Database();
        responseObject = new JsonObject();
//        processMessage(jsonRequest);
    }



    /**
     * Process the message received from a client and makes
     * different request depending on what the message asks for
     */
    public String processMessage(String jsonRequest) {
        //Create parser for JSON String
        JsonParser parser = new JsonParser();
        Gson gson = new Gson();

        // Convert JSON formatted string to one JsonObject
        JsonObject object = (JsonObject) parser.parse(jsonRequest);
//        JsonObject object = (JsonObject) gson.toJsonTree(jsonRequest);
        System.out.println("Message received: " +  jsonRequest);


        // Divide the id, action and data to different data types
        String id = object.get("activity").getAsString().toLowerCase();
        String action = object.get("action").getAsString().toLowerCase();
        JsonArray data = (JsonArray) object.get("data");

        // Calls different handlers depending on what id the client asks for
        if (id.equalsIgnoreCase("map")) {
            mapHandler(action, data);
        } else if (id.equalsIgnoreCase("nfc")) {
            NFCHandler(action, data);
        } else if (id.equalsIgnoreCase("video")) {

        } else {
            System.out.println("ERROR! Wrong id asked in processor");
        }

        String response = responseObject.toString();

        return response;
    }

    /**
     * Handles the NFC requests
     * If a "get" command, returns user info
     * @param action
     * @param data
     */
    private void NFCHandler(String action, JsonArray data) {
        if (action.equalsIgnoreCase("get")) {
            // Gets first JsonObject from the array containing the scanned NFC id
            // according to {"NFCid":"..some number"}
            JsonObject dataObj = (JsonObject) data.get(0);

            //
            int id = dataObj.get("NFCid").getAsInt(); // Select the NFC id
            String query = "SELECT Name, NFCid FROM users WHERE EXISTS (SELECT * FROM users WHERE NFCid=\'" + id + "\') AND NFCid =\'"  + id + "\'";
            try {
                // Get data from database and save as JsonArray
                JsonArray sqlResult = database.selectData(query);
                System.out.println("DATABASE ANSWER> " + sqlResult);
                JsonObject dataObject;

                // If the result from the database in the empty we user does not exists
                if (sqlResult.size() != 0) {
                    dataObject = (JsonObject) sqlResult.get(0);
                    dataObject.addProperty("access", true);
                } else {
                    dataObject = new JsonObject();
                    dataObject.addProperty("access", false);
                }
                JsonArray jsonArray = new JsonArray();
                jsonArray.add(dataObject);
                responseObject.add("data", jsonArray);
            } catch (SQLException e) {
                System.err.println("Error in your SQL query");
                e.printStackTrace();
            }
        } else {
            System.out.println("ERROR! Wrong action in NFCHandler!");
        }
    }

    /**
     * Handles the Map requests.
     * If it's a get action we return all events to the map
     * If it's a add action we add the new events to the database
     * @param action  server action to be performed "get", "add" or "delete"
     * @param data  data to be added or deleted
     */
    private void mapHandler(String action, JsonArray data) {
        if (action.equalsIgnoreCase("get")) {
            String query = "SELECT * FROM event";
            try {
                // selects all events from the database
                JsonArray sqlResult = database.selectData(query);
                System.out.println("DATABASE ANSWER> " + sqlResult);
                // Adds the result to the servers response
                responseObject.add("data", sqlResult);
            } catch (SQLException e) {
                System.err.println("Error in your SQL syntax");
                e.printStackTrace();
            }
        } else if (action.equalsIgnoreCase("add")) {
            String lat, lon, event, query;
            // Loop through
            for (JsonElement o: data) {
                JsonObject obj = o.getAsJsonObject();
                lat = obj.get("lat").getAsString();
                lon = obj.get("lon").getAsString();
                event = obj.get("event").getAsString();
                query = "INSERT INTO event (lat, lon, event) VALUES ("
                        + lat + "," + lon + "," + event + ")";
                try {
                    // try to insert, if we have duplicate coordinates in the db we update the row instead
                    database.updateData(query);
                } catch (SQLException e) {
                    // If we have duplicate primary keys
                    if(e.getErrorCode() == SQL_DUPLICATE_KEY_ERR_CODE) {
                        query = "UPDATE event SET event = \"" + event + "\" WHERE (lat, lon) = (" + lat + "," + lon + ")";
                        try {
                            // Update instead
                            database.updateData(query);
                        } catch (SQLException e1) {
                            // If the update fails
                            e1.printStackTrace();
                        }
                    } else
                        e.printStackTrace();
                }
            }
        } else if (action.equalsIgnoreCase("delete")) {

        }
    }






}