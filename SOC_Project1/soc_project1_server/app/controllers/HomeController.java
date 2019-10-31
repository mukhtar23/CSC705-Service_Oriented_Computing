package controllers;

import java.util.*;
import play.mvc.*;
import com.google.inject.Inject;
import play.data.DynamicForm;
import play.data.FormFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import javax.inject.*;
import play.db.*;
import java.sql.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.Logger;


/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {
        
    double prevLat;
    double prevLong;
    double totalDist;
    String prevUser = "";

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }


    /**
     * Use the haversine formula to compute distance
     * found this formaula from https://www.movable-type.co.uk/scripts/latlong.html
     * @param newLat
     * @param newLong
     * @return
     */
    public double computeDistance(double newLat, double newLong) {
        
        double a = Math.sin((Math.toRadians(newLat - prevLat))/2) * Math.sin((Math.toRadians(newLat - prevLat))/2) +
                Math.cos(Math.toRadians(prevLat)) * Math.cos(Math.toRadians(newLat)) *
                        Math.sin(Math.toRadians(newLong - prevLong)) * Math.sin(Math.toRadians(newLong - prevLong));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = 6371e3 * c; // in meters
        return d;
    }

    @Inject
    FormFactory formFactory;
    public Result handleupdates() {
        DynamicForm dynamicForm = formFactory.form().bindFromRequest();
        
        Logger.info("POST Request Body");
        Logger.info("Username is: " + dynamicForm.get("username"));
        Logger.info("Time is: " + dynamicForm.get("timestamp"));
        Logger.info("Latitude is: " + dynamicForm.get("latitude"));
        Logger.info("Longitude is: " + dynamicForm.get("longitude"));
        Logger.info("\n");

        String username = dynamicForm.get("username");
        String timestamp = dynamicForm.get("timestamp");
        String jsonLatitude = dynamicForm.get("latitude");
        String jsonLongitude = dynamicForm.get("longitude");

        // Convert lat/long to double for distance calculations
        double latitude = Double.parseDouble(jsonLatitude);
        double longitude = Double.parseDouble(jsonLongitude);

        insertLocation(username, timestamp, latitude, longitude);
        String totalDistance = Double.toString(totalDist);


        // response that server will send to client
        Logger.info("Server Response");
        Logger.info("Username is: " + username);
        Logger.info("Time is: " + timestamp);
        Logger.info("Latitude is: " + jsonLatitude);
        Logger.info("Longitude is: " + jsonLongitude);
        Logger.info("Total Distance Traveled: " + totalDistance);
        Logger.info("\n");

        // Send back a response to the client
        ObjectNode locationInfo = Json.newObject();
        locationInfo.put("username", username);
        locationInfo.put("timestamp", timestamp);
        locationInfo.put("latitude", jsonLatitude);
        locationInfo.put("longitude", jsonLongitude);
        locationInfo.put("totalDistance", totalDistance);

        return ok(locationInfo);
    }

    /**
     * Connect to the locationTracker.db database
     *
     * @return the Connection object
     */
    private Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:sqlite/db/locationTracker.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
    /**
     * Method used to insert JSON location info in the sqlite db
     */
    public void insertLocation(String username, String timestamp, double latitude, double longitude){

        // Generate SQL statements to execute:

        //  SQL statement for creating a new table
        String sqlCreateTable =
            "CREATE TABLE IF NOT EXISTS locationData(\n"
            + "	username TEXT NOT NULL,\n"
            + "	timestamp TEXT,\n"
            + "	latitude REAL,\n"
            + "	longitude REAL,\n"
            + " totalDistance REAL \n"
            + ");";

        //  SQL statement to append location data to table
        String sqlInsert = "INSERT INTO locationData(username, timestamp, latitude, longitude, totalDistance) VALUES(?,?,?,?,?);";

        // SQL Statement to retrieve totaldistance, lat, and long for existing users
        String recDist = "SELECT totalDistance FROM locationData WHERE username ='" + username + "' ORDER BY totalDistance DESC LIMIT 1;";
        String recLat = "SELECT latitude FROM locationData WHERE username ='" + username + "' ORDER BY timestamp DESC LIMIT 1;";
        String recLong = "SELECT longitude FROM locationData WHERE username ='" + username + "' ORDER BY timestamp DESC LIMIT 1;";


        try {
            // Connection conn = db.getConnection(); 
            Connection conn = this.connect();

            Statement stmt = conn.createStatement();

            // Execute the create table statement
            stmt.execute(sqlCreateTable);


            // // First time the user adds data or if there is a new user who is tracking their location
            if(prevUser.isEmpty() || !prevUser.equals(username)){
                totalDist = 0;
                prevLat = latitude;
                prevLong = longitude;
            }

            Logger.info("Info before sql select");
            Logger.info("Username " + username);
            Logger.info("Time " + timestamp);
            Logger.info("Latitude " + Double.toString(prevLat));
            Logger.info("Longitude " + Double.toString(prevLong));
            Logger.info("\n");

            // Get total Distance for a user
            ResultSet rs1    = stmt.executeQuery(recDist);
            while (rs1.next()) {
                totalDist = rs1.getDouble("totalDistance");
            }

            // Get Latitude for a user
            ResultSet rs2    = stmt.executeQuery(recLat);
            while (rs2.next()) {
                prevLat = rs2.getDouble("latitude");
            }

            // Get longitude for a user
            ResultSet rs3    = stmt.executeQuery(recLong);
            while (rs3.next()) {
                prevLong = rs3.getDouble("longitude");
            }

            Logger.info("Info After sql select");
            Logger.info("Username " + username);
            Logger.info("Time " + timestamp);
            Logger.info("Latitude " + Double.toString(prevLat));
            Logger.info("Longitude " + Double.toString(prevLong));
            Logger.info("\n");

            totalDist = totalDist + computeDistance(latitude, longitude);

            // need the most recent lat/long  and user for next computeDistance
            prevLat = latitude;
            prevLong = longitude;
            prevUser = username;

            PreparedStatement pstmt = conn.prepareStatement(sqlInsert);
                pstmt.setString(1, username);
                pstmt.setString(2, timestamp);
                pstmt.setDouble(3, latitude);
                pstmt.setDouble(4, longitude);
                pstmt.setDouble(5, totalDist);
                pstmt.executeUpdate();

        } catch (Exception e) {
            Logger.info(e.getMessage());
        }
    }
}
