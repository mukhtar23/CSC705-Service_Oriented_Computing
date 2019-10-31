package controllers;

import models.AirlineActor;
import models.AirlineActor.*;

import models.BookingActor;
import models.BookingActor.*;

import java.util.*;
import play.mvc.*;
import akka.actor.*;
import akka.util.Timeout;
import scala.compat.java8.FutureConverters;
import javax.inject.*;
import java.util.concurrent.CompletionStage;
import scala.concurrent.Future;
import scala.concurrent.Await;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static akka.pattern.Patterns.ask;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
@Singleton
public class HomeController extends Controller {

    private final String SUCCESS = "success";
    private final String ERROR = "error";
    private final String[] OPERATORS = new String[]{"AA", "BA", "CA"};
    
    private ActorRef aaActor;
    private ActorRef baActor;
    private ActorRef caActor;
    private ActorRef bookingActor;
    private Timeout defaultTimeOut;

    private HashMap<String, Integer> aaDesignatedFlights;
    private HashMap<String, Integer> baDesignatedFlights;
    private HashMap<String, Integer> caDesignatedFlights;
    

    @Inject
    public HomeController(ActorSystem system) {
        aaActor = system.actorOf(AirlineActor.getProps("AA","AmericanAirlines"), "AmericanAirlines");
        baActor = system.actorOf(AirlineActor.getProps("BA", "BritishAirways"), "BritishAirways");
        caActor = system.actorOf(AirlineActor.getProps("CA", "AirChina"), "Airchina");
        
        defaultTimeOut = new Timeout(Duration.create(5, "seconds"));
        
        createFlightsList();

        bookingActor = system.actorOf(BookingActor.getProps(aaActor, baActor, caActor), "BookingActor");
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

    private void createFlightsList() {
        aaDesignatedFlights = new HashMap<String,Integer>();
        baDesignatedFlights = new HashMap<String,Integer>();
        caDesignatedFlights = new HashMap<String,Integer>();

        aaDesignatedFlights.put("AA001", 3);
        aaDesignatedFlights.put("AA002", 1);

        baDesignatedFlights.put("BA001", 1);

        caDesignatedFlights.put("CA001", 1);
        caDesignatedFlights.put("CA002", 1);

        aaActor.tell(new SetFlights(aaDesignatedFlights), ActorRef.noSender());
        baActor.tell(new SetFlights(baDesignatedFlights), ActorRef.noSender());
        caActor.tell(new SetFlights(caDesignatedFlights), ActorRef.noSender());
    }

    //////////////
    // API Methods
    //////////////

    /**
     * get list of airline operators
     */
    public Result getAirlineOperators(){
        // ObjectNode response = Json.newObject();
        // response.put("status", SUCCESS);
        // response.put("operators", OPERATORS);

        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        response.put("status", SUCCESS);
        response.putPOJO("operators", OPERATORS);

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
      

        // return ok(response);
    }

    /**
     * get an airline operator based on code input
     */
    private ActorRef determineAirlineOperator(String operator) {
        ActorRef airlineOperator;
        switch (operator) {
            case "AA":
                airlineOperator = aaActor;
                break;
            case "BA":
                airlineOperator = baActor;
                break;
            case "CA":
                airlineOperator = caActor;
                break;
            default:
                airlineOperator = null;
                break;
        }
        return airlineOperator;
    }

    /**
     * return list of flights for an airline operator
     */
    public Result getFlightsOfOperator(String operatorCode){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        ActorRef airlineOperator = determineAirlineOperator(operatorCode);
        // HashMap<String, Integer> flights = (AirlineActor)airlineOperator.getFlights();
        // GetAllFlights message = new GetAllFlights();
        if(airlineOperator != null){
            Future<Object> future = ask(airlineOperator, new GetAllFlights(), defaultTimeOut);
            HashMap<String, Integer> flights;
            
            try {
                flights =  (HashMap<String,Integer>) Await.result(future, defaultTimeOut.duration());
            } catch (Exception e) {
                // e.printStackTrace(System.out);
                flights = null;
            }

            if(flights!= null){
                response.put("status", SUCCESS);
                response.putPOJO("flights", flights.keySet());
            }else{
                response.put("status", ERROR);
                response.put("message", "Unable to get flights for operator " + operatorCode);
            }

        }else{
            response.put("status", ERROR);
            response.put("message", "No operator with code " + operatorCode + " exists.");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }

    /**
     * return number of available seats for a flight
     */
    public Result getSeatsOfFlight(String operatorCode, String flightCode){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        ActorRef airlineOperator = determineAirlineOperator(operatorCode);
        // HashMap<String, Integer> flights = (AirlineActor)airlineOperator.getFlights();
        // GetAllFlights message = new GetAllFlights();
        if(airlineOperator != null){
            // Future<Object> future = ask(airlineOperator, new GetAllFlights(), defaultTimeOut);
            Future<Object> future = ask(airlineOperator, new GetAvailableSeatsForFlight(flightCode), defaultTimeOut);
            // HashMap<String, Integer> flights;
            Integer seats;

            try {
                // flights = (HashMap<String,Integer>) Await.result(future, defaultTimeOut.duration());
                seats = (int)  Await.result(future, defaultTimeOut.duration());
            } catch (Exception e) {
                // e.printStackTrace(System.out);
                // flights = null;
                seats = -100;
            }

            if(seats != -100){
                if(seats >= 0 ){
                    response.put("status", SUCCESS);
                    response.put("seats", seats);
                }else{
                    response.put("status", SUCCESS);
                    response.put("seats", "No more seats available for flight " + flightCode);
                }
            }else{
                response.put("status", ERROR);
                response.put("message", "Flight with id: " + flightCode + " doesn't exist");
            }


        }else{
            response.put("status", ERROR);
            response.put("message", "No operator with code " + operatorCode + " exists.");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }

    /**
     * Books a trip from starting point and destination given
     */
    public Result bookTrip(String from, String to){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        if(from.equals("X") && to.equals("Y")){
            Future<Object> future = ask(bookingActor, new BookTrip(), defaultTimeOut);
            int tripId = 0;
            
            try{
                tripId =  (int) Await.result(future, defaultTimeOut.duration());
            }catch (Exception e){
                // time out
                tripId = -2;
            }
            
            if(tripId == 0){
                response.put("status", ERROR);
                response.put("message", "There are no available paths for the flight");
            }else if(tripId == -1){
                response.put("status", ERROR);
                response.put("message", "Failed to confirm booking");
            }else if(tripId == -2){
                response.put("status", ERROR);
                response.put("message", "Confirm/Holding Request timed out");
            }else{
                response.put("status", SUCCESS);
                response.putPOJO("tripID", tripId);
            }

        }else{
            response.put("status", ERROR);
            response.put("message", "From location needs to be X and To location needs to be Y");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }

    /**
     * Returns all booked trips
     */
    public Result getAllBookedTrips(){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        Future<Object> future = ask(bookingActor, new GetAllBookedTrips(), defaultTimeOut);
        HashMap<Integer, String[]> trips;

        try{
            trips =  (HashMap<Integer, String[]>) Await.result(future, defaultTimeOut.duration());
        }catch (Exception e){
            trips = null;
        }

        if(trips != null){
            response.put("status", SUCCESS);
            response.putPOJO("trips", trips.keySet());
        }else{
            response.put("status", SUCCESS);
            response.put("trips", "No booked trips");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }

    /**
     * Returns all segments for a trip when the id is given
     */
    public Result getTripSegments(String tripId){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        int tripID = Integer.parseInt(tripId);
        
        Future<Object> future = ask(bookingActor, new GetTripSegments(tripID), defaultTimeOut);
        String[] segments;

        try{
            segments =  (String[]) Await.result(future, defaultTimeOut.duration());
        }catch (Exception e){
            segments = null;
        }

        if(segments != null && segments.length != 0){
            response.put("status", SUCCESS);
            response.putPOJO("segments", segments);
        }else{
            response.put("status", ERROR);
            response.put("message", "No booked trips with id " + tripId + " exists");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }

    ////////////////////
    // Debug API methods
    ///////////////////

    public Result confirmFail(String airline){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        ActorRef airlineOperator = determineAirlineOperator(airline);

        if(airlineOperator != null){
            Future<Object> future = ask(airlineOperator, new ConfirmFail(), defaultTimeOut);
            // HashMap<String, Integer> flights;
            boolean feedback;

            try {
                feedback = (boolean)  Await.result(future, defaultTimeOut.duration());
            } catch (Exception e) {
                feedback = false;
            }

            if(feedback){
                response.put("status", SUCCESS);
            }else{
                response.put("status", ERROR);
            }
        }else{
            response.put("status", ERROR);
            response.put("message", "No airline with code " + airline + " exists");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }

    }

    public Result confirmNoResponse(String airline){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        ActorRef airlineOperator = determineAirlineOperator(airline);

        if(airlineOperator != null){
            Future<Object> future = ask(airlineOperator, new ConfirmNoResponse(), defaultTimeOut);
            // HashMap<String, Integer> flights;
            boolean feedback;

            try {
                feedback = (boolean)  Await.result(future, defaultTimeOut.duration());
            } catch (Exception e) {
                feedback = false;
            }

            if(feedback){
                response.put("status", SUCCESS);
            }else{
                response.put("status", ERROR);
            }
        }else{
            response.put("status", ERROR);
            response.put("message", "No airline with code " + airline + " exists");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }

    public Result reset(String airline){
        ObjectNode response = Json.newObject();
        ObjectMapper mapper = new ObjectMapper();

        ActorRef airlineOperator = determineAirlineOperator(airline);

        if(airlineOperator != null){
            Future<Object> future = ask(airlineOperator, new Reset(), defaultTimeOut);
            // HashMap<String, Integer> flights;
            boolean feedback;

            try {
                feedback = (boolean)  Await.result(future, defaultTimeOut.duration());
            } catch (Exception e) {
                feedback = false;
            }

            if(feedback){
                response.put("status", SUCCESS);
            }else{
                response.put("status", ERROR);
            }
        }else{
            response.put("status", ERROR);
            response.put("message", "No airline with code " + airline + " exists");
        }

        try {
            return ok(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.out);
            return ok(response);
        }
    }
}
