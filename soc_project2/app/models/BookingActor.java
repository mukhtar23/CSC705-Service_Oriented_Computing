package models;

import akka.actor.*;
import akka.japi.*;
import java.util.*;
import akka.util.Timeout;
import scala.compat.java8.FutureConverters;
import java.util.concurrent.CompletionStage;
import scala.concurrent.Future;
import scala.concurrent.Await;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import akka.event.Logging;
import akka.event.LoggingAdapter;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;


import java.util.Date;
import java.sql.Timestamp;

import models.AirlineActor.*;
import models.AirlineActor;
import models.AirlineActorProtocol.*;
import models.AirlineActorProtocol;

import static akka.pattern.Patterns.ask;


public class BookingActor extends AbstractActor {
  
    private final String FROM = "X";
    private final String TO = "Y";

    private final String[] PATH1 = new String[]{"CA001"};
    private final String[] PATH2 = new String[]{"AA001", "BA001"};
    private final String[] PATH3 = new String[]{"AA001", "CA002", "AA002"};
    private final String[] NOPATH = null;

    private ActorRef aaActor;
    private ActorRef baActor;
    private ActorRef caActor;
    private HashMap<Integer, String[]> trips;

    private int tripId = 1;

    private Timeout defaultTimeOut; 

    // private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    // final Logger log = LoggerFactory.getLogger(this.getClass());
    public static final Logger log = Logger.getLogger(BookingActor.class.getName());
    // public Logger log = Logger.getLogger("Logger");
    // final Logger log = getContext().getLogger();
    

    public ConsoleHandler ch = new ConsoleHandler();

    private int transactionId = 1;


    public static Props getProps(ActorRef aaActor, ActorRef baActor, ActorRef caActor) {
        return Props.create(BookingActor.class, () -> new BookingActor(aaActor, baActor, caActor));
    }

    public BookingActor(ActorRef aaActor, ActorRef baActor, ActorRef caActor) {
        this.aaActor = aaActor;
        this.baActor = baActor;
        this.caActor = caActor;
        this.trips = new HashMap<Integer, String[]>();
        defaultTimeOut = new Timeout(Duration.create(5, "seconds"));
		ch.setLevel(Level.INFO);
		log.addHandler(ch);
		log.setLevel(Level.INFO);
        log.setUseParentHandlers(false);
    }

    // Message Classes

    public static class BookTrip{
    
        public BookTrip(){

        }
    }

    public static class GetAllBookedTrips{

        public GetAllBookedTrips(){

        }
    }

    public static class GetTripSegments{
        private int tripId;

        public GetTripSegments(int tripId){
            this.tripId = tripId;
        }
    }

    // confirmation of hold requests from AirlineActor
	public static class HoldSuccess {
	
		private boolean held = false;

		public HoldSuccess(boolean held) {
            this.held = held;
        }
	}
	
	// confirmation of confirm requests from AirlineActor
	public static class ConfirmSuccess {

		private boolean confirmed = false;

		public ConfirmSuccess(boolean confirmed) {
            this.confirmed = confirmed;
		}
	}

    // End of Message Classes

    private void getBookedTrips(){
        sender().tell(trips, self());
    }

    private void getTripSegments(int tripId){
        if(trips.containsKey(tripId)){
            sender().tell(trips.get(tripId), self());
        }else{
            sender().tell(new String[0], self());
        }
        
    }

    private void bookTrip(){

		// ch.setLevel(Level.INFO);
		// log.addHandler(ch);
		// log.setLevel(Level.INFO);
        // log.setUseParentHandlers(false);

        int id;
        
        boolean caHeld = false;
        boolean caHeld2 = false;
        boolean aaHeld = false;
        boolean aaHeld2 = false;
        boolean baHeld = false;
        boolean caConfirm = false;
        boolean caConfirm2 = false;
        boolean aaConfirm = false;
        boolean aaConfirm2 = false;
        boolean aaConfirm3 = false;
        boolean baConfirm = false;

        boolean timeout1 = false;
        boolean timeout2 = false;
        boolean timeout3 = false;

        Future<Object> future;
        Future<Object> future2;
        Future<Object> future3;

        // Hold request For CA001

        int cc1ID = transactionId;
        transactionId++;
        Timestamp c1Time;
        future = ask(caActor, new Hold("CA001"), defaultTimeOut);
        try{
            caHeld = (boolean) Await.result(future, defaultTimeOut.duration());
            c1Time = new Timestamp(System.currentTimeMillis());
        }catch(Exception e){
            c1Time = new Timestamp(System.currentTimeMillis());
            log.info("Holding Request Timed Out for: CA001, Transactio ID: " + cc1ID + ", Timstamp: " + c1Time);
            log.warning("Error for CA001, ID: " + cc1ID + " - " + e.toString());
            return;

        }

        if(caHeld){ // Hold for CA001 was successful
            log.info("Hold Request Successful for: CA001, Transaction ID: " + cc1ID + ", Timestamp: " + c1Time);
            // Confirm Request

            future = ask(caActor, new Confirm("CA001"), defaultTimeOut);
            try{
                caConfirm = (boolean) Await.result(future, defaultTimeOut.duration());
                c1Time = new Timestamp(System.currentTimeMillis());

            }catch(Exception e){
                c1Time = new Timestamp(System.currentTimeMillis());
                log.info("Confirm Request Timed Out for: CA001, Transactio ID: " + cc1ID + ", Timstamp: " + c1Time);
                log.warning("Error for CA001, ID: " + cc1ID + " - " + e.toString());
                return;
            }

            if(caConfirm){ // Confirm was succesful
                log.info("Confirm Request Successful for: CA001, Transaction ID: " + cc1ID + ", Timestamp: " + c1Time);
                trips.put(tripId, PATH1);
                id = tripId;
                tripId++;
            }else{ // Confirm was not successful
                log.info("Confirm Request Failed for: CA001, Transaction ID: " + cc1ID + ", Timestamp: " + c1Time);
                id = -1;
            }

        }else{ // Hold for CA001 was not successful
            log.info("Hold Request Failed for: CA001, Transaction ID: " + cc1ID + ", Timestamp: " + c1Time);

            // Hold request for AA011 and BA001
            int aa1ID = transactionId;
            transactionId++;
            int ba1ID = transactionId;
            transactionId++;

            Timestamp aa1Time;
            Timestamp ba1Time;
        
            future = ask(aaActor, new Hold("AA001"), defaultTimeOut);
            try{
                aaHeld = (boolean) Await.result(future, defaultTimeOut.duration());
                aa1Time = new Timestamp(System.currentTimeMillis());
            }catch(Exception e){
                aa1Time = new Timestamp(System.currentTimeMillis());
                log.info("Holding Request Timed Out for AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);      
                log.warning("Error for AA001, ID: " + aa1ID + " - " + e.toString());
                return;
            }

            future2 = ask(baActor, new Hold("BA001"), defaultTimeOut);
            try{
                baHeld = (boolean) Await.result(future2, defaultTimeOut.duration());
                ba1Time = new Timestamp(System.currentTimeMillis());
            }catch(Exception e){
                ba1Time = new Timestamp(System.currentTimeMillis());
                log.info("Holding Request Timed Out for BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);
                log.warning("Error for BA001, ID: " + ba1ID + " - " + e.toString());
                return;
            }

            if(aaHeld && baHeld){ // Hold for AA001 and BA001 was successful


                log.info("Hold Request Successful for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                log.info("Hold Request Successful for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);
            
                future = ask(aaActor, new Confirm("AA001"), defaultTimeOut);
                
                try{
                    aaConfirm = (boolean) Await.result(future, defaultTimeOut.duration());
                    aa1Time = new Timestamp(System.currentTimeMillis());

                }catch(Exception e){
                    
                    aa1Time = new Timestamp(System.currentTimeMillis());
                    log.info("Confirm Request Timed Out for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    log.warning("Error for AA001, ID: " + aa1ID + " - " + e.toString());
                    
                    return;
                }
                
                
                future2 = ask(baActor, new Confirm("BA001"), defaultTimeOut);
        
                try{
                    
                    baConfirm = (boolean) Await.result(future2, defaultTimeOut.duration());
                    ba1Time = new Timestamp(System.currentTimeMillis());

                }catch(Exception e){
                    
                    ba1Time = new Timestamp(System.currentTimeMillis());
                    log.info("Confirm Request Timed Out for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);
                    log.warning("Error for BA001, ID: " + ba1ID + " - " + e.toString());
                    return;
                }

                if(aaConfirm && baConfirm){ // Confirm was succesful
                    log.info("Confirm Request Successful for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                    log.info("Confirm Request Successful for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);
                    
                    trips.put(tripId, PATH2);
                    id = tripId;
                    tripId++;
                }else if(aaConfirm && !baConfirm){
                    log.info("Confirm Request Successful for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                    log.info("Confirm Request Failed for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);
                    id = -1;
                }else if(!aaConfirm && baConfirm){

                    log.info("Confirm Request Failed for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                    log.info("Confirm Request Successful for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);

                    id = -1;
                }else{ // Confirm was not successful
                    log.info("Confirm Request Failed for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                    log.info("Confirm Request Failed for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);
                    
                    id = -1;
                }
            }else{ // Hold for AA001 and BA001 was not successful
                log.info("Hold Request Failed for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                
                log.info("Hold Request Failed for: BA001, Transaction ID: " + ba1ID + ", Timestamp: " + ba1Time);

                // Hold request for AA001, CA002, and AA002

                aa1ID = transactionId;
                transactionId++;
                int ca2ID = transactionId;
                transactionId++;
                int aa2ID = transactionId;
                transactionId++;

                Timestamp aa2Time;
                Timestamp ca2Time;

                future = ask(aaActor, new Hold("AA001"), defaultTimeOut);
                
                try{
                    aaHeld = (boolean) Await.result(future, defaultTimeOut.duration());
                    aa1Time = new Timestamp(System.currentTimeMillis());

                }catch(Exception e){
                    aa1Time = new Timestamp(System.currentTimeMillis());
                    log.info("Hold Request Timed Out for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);

                    log.warning("Error for AA001, ID: " + aa1ID + " - " + e.toString());
                    return;
                }

                future2 = ask(caActor, new Hold("CA002"), defaultTimeOut);

                try{
                    caHeld2 = (boolean) Await.result(future2, defaultTimeOut.duration());
                    ca2Time = new Timestamp(System.currentTimeMillis());

                }catch(Exception e){
                    ca2Time = new Timestamp(System.currentTimeMillis());
                    log.info("Hold Request Timed Out for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);
                    log.warning("Error for CA002, ID: " + ca2ID + " - " + e.toString());
                    return;
                }

                future3 = ask(aaActor, new Hold("AA002"), defaultTimeOut);

                try{
                    aaHeld2 = (boolean) Await.result(future3, defaultTimeOut.duration());
                    aa2Time = new Timestamp(System.currentTimeMillis());

                }catch(Exception e){
                    aa2Time = new Timestamp(System.currentTimeMillis());
                    log.info("Hold Request Timed Out for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);
                    log.warning("Error for AA002, ID: " + aa2ID + " - " + e.toString());
                    return;
                } 

                if(aaHeld && caHeld2 && aaHeld2){ // Hold for AA001, CA002, and AA002 was successful
                    log.info("Hold Request Successful for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                    log.info("Hold Request Successful for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);

                    log.info("Hold Request Successful for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);

                    future = ask(aaActor, new Confirm("AA001"), defaultTimeOut);

                    try{
                        aaConfirm = (boolean) Await.result(future, defaultTimeOut.duration());
                        aa1Time = new Timestamp(System.currentTimeMillis());

                    }catch(Exception e){
                        aa1Time = new Timestamp(System.currentTimeMillis());
                        log.info("Confirm Request Timed Out for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                        log.warning("Error for AA001, ID: " + aa1ID + " - " + e.toString());
                        return;
                    }

                    future2 = ask(caActor, new Confirm("CA002"), defaultTimeOut);

                    try{
                        caConfirm2 = (boolean) Await.result(future2, defaultTimeOut.duration());
                        ca2Time = new Timestamp(System.currentTimeMillis());;

                    }catch(Exception e){
                        ca2Time = new Timestamp(System.currentTimeMillis());
                        log.info("Confirm Request Timed Out for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);
                        log.warning("Error for CA002, ID: " + ca2ID + " - " + e.toString());
                        return;
                    }

                    future3 = ask(aaActor, new Confirm("AA002"), defaultTimeOut);

                    try{
                        aaConfirm2 = (boolean) Await.result(future3, defaultTimeOut.duration());
                        aa2Time = new Timestamp(System.currentTimeMillis());

                    }catch(Exception e){
                        aa2Time = new Timestamp(System.currentTimeMillis());
                        log.info("Confirm Request Timed Out for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time); 
                        log.warning("Error for AA002, ID: " + aa2ID + " - " + e.toString());
                        return;
                    }

                    if(aaConfirm && caConfirm2 && aaConfirm2){ // Confirm was succesful
                        log.info("Confirm Request Successful for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                        log.info("Confirm Request Successful for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);

                        log.info("Confirm Request Successful for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);
                        
                        trips.put(tripId, PATH3);
                        id = tripId;
                        tripId++;
                    }else if(!aaConfirm && caConfirm2 && !aaConfirm2){
                        log.info("Confirm Request Failed for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                        log.info("Confirm Request Successful for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);

                        log.info("Confirm Request Failed for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);

                        id = -1;
                    }else if(aaConfirm && !caConfirm2 && aaConfirm2){
                        log.info("Confirm Request Successful for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                        log.info("Confirm Request Failed for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);

                        log.info("Confirm Request Successful for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);

                        id = -1;
                    }else{ // Confirm was not successful
                        log.info("Confirm Request Failed for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                        log.info("Confirm Request Failed for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);

                        log.info("Confirm Request Failed for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);
                        id = -1;
                    }

                }else{ // Hold for AA001, CA002, and AA002 was not successful
                    log.info("Hold Request Failed for: AA001, Transaction ID: " + aa1ID + ", Timestamp: " + aa1Time);
                    
                    log.info("Hold Request Failed for: CA002, Transaction ID: " + ca2ID + ", Timestamp: " + ca2Time);

                    log.info("Hold Request Failed for: AA002, Transaction ID: " + aa2ID + ", Timestamp: " + aa2Time);
                    
                    // No path available
                    id = 0;
                }
            }
        }

        System.out.println("Trip ID: " + id);
        System.out.println();
        System.out.println();
        sender().tell(id, self());
    }

    @Override
    public Receive createReceive() {
      return receiveBuilder()
            .match(
                BookTrip.class, bookTrip -> {
                    bookTrip();
                })
            .match(
                GetAllBookedTrips.class, allBookedTrips -> {
                    getBookedTrips();
                })
            .match(
                GetTripSegments.class, tripSegments -> {
                    getTripSegments(tripSegments.tripId);
                })
            .build();
    }
}