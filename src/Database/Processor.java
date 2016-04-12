package Database;
import com.google.gson.*;

import java.sql.SQLDataException;
import java.sql.SQLException;

/**
 * Created by Fredrik on 2016-03-15.
 */
public class Processor {
    private static final int SQL_DUPLICATE_KEY_ERR_CODE = 1062;
    private Database database;
    private JsonObject responseObject = null;
    private String activity, action, id;
    private JsonArray data;
    private int requiredPermission;
    /**
     *
     */
    public Processor(String jsonRequest) {
        database = new Database();
        responseObject = new JsonObject();
        //Create parser for JSON String
        JsonParser parser = new JsonParser();

        // Convert Json formatted string to one JsonObject
        JsonObject object = (JsonObject) parser.parse(jsonRequest);
//        System.out.println("Message received: " +  jsonRequest);
        // Assign the required id, action and data parameters
        activity = object.get("activity").getAsString().toLowerCase();
        action = object.get("action").getAsString().toLowerCase();
        requiredPermission = object.get("min_per").getAsInt();
        data = (JsonArray) object.get("data");
    }

    /**
     * Process the message received from a client and makes
     * different request depending on what the message asks for
     */
    public String processMessage() {

        // Manipulating data requires that the user has permission to do so
        // The exception is when the user logs in to the system with nfc
        boolean permitted;
        if(activity.equalsIgnoreCase("nfc") || activity.equalsIgnoreCase("contact")){
            permitted = true;
        } else {
            permitted = checkPermission(data);
        }

        String response;
        if(permitted){
            // Handle the matching activity the client refers to
            if (activity.equalsIgnoreCase("map")) {
                handleMap();
            } else if (activity.equalsIgnoreCase("nfc")) {
                handleNFC();
            } else if (activity.equalsIgnoreCase("video")) {
                handleVideo();
            } else if (activity.equalsIgnoreCase("contact")) {
                handleContacts();
            } else {
                System.out.println("ERROR! Wrong activity asked in processor");
            }
        } else {
            //User doesn't have the required permission level to manipulate the data
            JsonArray data = new JsonArray();
            JsonObject denied = new JsonObject();
            denied.addProperty("permitted",false);
            data.add(denied);
            responseObject.add("data",data);
        }
        response = responseObject.toString();
        return response;
    }

    private void handleContacts() {
            JsonElement e = data.get(0);
            JsonObject params = e.getAsJsonObject();
            // Get the user's id
            id = params.get("id").getAsString();
            String getPerm = "SELECT Permission FROM users WHERE NFCid=" + id;
        if (action.equalsIgnoreCase("get")) {
            try {
                // Get data from database and save as JsonArray
                JsonArray permData = database.selectData(getPerm);
                JsonObject dataObject = (JsonObject) permData.get(0);

                // Grab the users permission level and gets contact info accordingly
                int userPermission = dataObject.get("permission").getAsInt();
                String query = "";
                switch (userPermission) {
                    case 0:
                        query = "SELECT * FROM View_2";
                        break;
                    case 1:
                        query = "SELECT * FROM View_1";
                        break;
                    case 2:
                        query = "SELECT * FROM Contacts";
                        break;
                    default:
                        System.out.println("DU HAR FEL BEHÖRIGHET");
                }

                JsonArray sqlResult = database.selectData(query);
                responseObject.addProperty("permission",userPermission);
                responseObject.add("data", sqlResult);

            } catch (SQLException sqle) {
                System.err.println("Error in your SQL query");
            }

        } else if (action.equalsIgnoreCase("delete")){
            String deleteKey = params.get("deletekey").getAsString();
            String query = "DELETE FROM Contacts WHERE pnr = " + deleteKey;
            try {
                database.updateData(query);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

    }

    private boolean checkPermission(JsonArray data) {
        boolean permitted = false;
        JsonElement e = data.get(0);
        JsonObject params = e.getAsJsonObject();
        // Get the user's id
        id = params.get("id").getAsString();

        String query = "SELECT Permission FROM users WHERE NFCid=" + id;
        try{
            // Get data from database and save as JsonArray
            JsonArray sqlResult = database.selectData(query);
            JsonObject dataObject = (JsonObject) sqlResult.get(0);

            // Grab the users permission level and compare to required permission level
            int userPermission = dataObject.get("permission").getAsInt();
            permitted = userPermission >= requiredPermission;
        } catch (SQLException sqle) {
            System.err.println("Error in your SQL query");
            sqle.printStackTrace();
        }
        return permitted;
    }

    /**
     * Handles the NFC requests
     * If a "get" command, returns user info
     */
    private void handleNFC() {
        if (action.equalsIgnoreCase("get")) {
            // Gets first JsonObject from the array containing the scanned NFC id
            // according to {"NFCid":"..some number"}
            JsonObject dataObj = (JsonObject) data.get(0);

            // Get the user's id
            int id = dataObj.get("NFCid").getAsInt();

            String query = "SELECT Name, NFCid FROM users WHERE EXISTS (SELECT * FROM users WHERE NFCid=\'" + id + "\') AND NFCid =\'"  + id + "\'";
            try {
                // Get data from database and save as JsonArray
                JsonArray sqlResult = database.selectData(query);
//                System.out.println("DATABASE ANSWER> " + sqlResult);
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
     */
    private void handleMap() {
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
                        + lat + "," + lon + "," + "'"+event+ "'"+ ")";
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
            String lat, lon, query;

            // Loop through
            for (JsonElement o: data) {
                JsonObject obj = o.getAsJsonObject();
                lat = obj.get("lat").getAsString();
                lon = obj.get("lon").getAsString();
                query = "DELETE FROM event WHERE (lat,lon) = ("
                        + lat + "," + lon + ")";
                try {
                    // try to insert, if we have duplicate coordinates in the db we update the row instead
                    database.updateData(query);
                } catch (SQLException e) {
                    // If we have duplicate primary keys
//                    if(e.getErrorCode() == SQL_DUPLICATE_KEY_ERR_CODE) {
//                        query = "UPDATE event SET event = \"" + event + "\" WHERE (lat, lon) = (" + lat + "," + lon + ")";
//                        try {
//                            // Update instead
//                            database.updateData(query);
//                        } catch (SQLException e1) {
//                            // If the update fails
//                            e1.printStackTrace();
//                        }
//                    } else
                        e.printStackTrace();
                }
            }
        }
    }
    private void handleVideo(){

    }






}