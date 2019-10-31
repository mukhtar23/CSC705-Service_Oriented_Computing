package models;

import akka.actor.*;
import akka.japi.*;
import java.util.*;

public class AirlineActor extends AbstractActor {

  private String airlineCode;
  private String name;
  private HashMap<String, Integer> flights;

  private boolean confirmFail = false;
  private boolean confirmNoResponse = false;
  
  public static Props getProps(String code, String name) {
    return Props.create(AirlineActor.class, () -> new AirlineActor(code, name));
  }

  
  public AirlineActor(String code, String name){
    this.name = name;
    airlineCode = code;
    flights =  new HashMap<String, Integer>();
  }

  ////////////////////
  // Message Classes
  ///////////////////

  // API message classes

  public static class GetAllFlights { 
    public GetAllFlights(){
    } 
  }

  public static class SetFlights{

    HashMap<String, Integer> flights;

    public SetFlights(HashMap<String,Integer> flights){
      this.flights = flights;
    }
  }

  public static class GetAvailableSeatsForFlight {

      private String flightCode;

      public GetAvailableSeatsForFlight(String flightCode) {
          this.flightCode = flightCode;
      }

      public String getFlightCode() {
          return this.flightCode;
      }
  }

  public static class ReserveSeat{

    private String flightCode;

    public ReserveSeat(String flightCode){
      this.flightCode = flightCode;
    }
  }

  public static class Hold{

    private String flightCode;

    public Hold(String flightCode){
      this.flightCode = flightCode;
    }
  }

  public static class Confirm{

    private String flightCode;

    public Confirm(String flightCode){
      this.flightCode = flightCode;
    }
  }

  // Debug API classes

	public static class ConfirmFail {
		
    private boolean confirmFail;

		public ConfirmFail() {
			confirmFail = true;
		}
	}
	
	public static class ConfirmNoResponse {
		
    private boolean confirmNoResponse;

		public ConfirmNoResponse() {
			confirmNoResponse = true;
		}
	}
	
	public static class Reset {
		
    private boolean confirmFail;
    private boolean confirmNoResponse;
    private boolean reset = false;

		public Reset() {
			confirmFail = false;
			confirmNoResponse = false;
      reset = true;
		}
	}


  // End of Message classes
 
  public void setFlights(HashMap<String, Integer> flights){
    this.flights = flights;
  }

  public void getFlights(){
    sender().tell(flights, self());
   
  }

  public void getSeats(String flightCode){
    if(flights.containsKey(flightCode)){
      sender().tell(flights.get(flightCode), self());
    }else{
      sender().tell(-100, self());
    }
  }

  public void reserveSeat(String flightCode){
    int currentSeats = flights.get(flightCode);
    int newSeats = currentSeats - 1;
    flights.replace(flightCode,newSeats);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            SetFlights.class, flights -> {
              setFlights(flights.flights);
            })
        .match(
            GetAllFlights.class, getFlights -> {
              getFlights();
            })
        .match(
          GetAvailableSeatsForFlight.class, getSeats -> {
            getSeats(getSeats.flightCode);
          })
        .match(
          ReserveSeat.class, reserveSeat -> {
            reserveSeat(reserveSeat.flightCode);
          })
        .match(
          ConfirmFail.class, fail -> {
            this.confirmFail = fail.confirmFail;
            sender().tell(true, self());
          })
        .match(
          ConfirmNoResponse.class, noResponse -> {
            this.confirmNoResponse = noResponse.confirmNoResponse;
            sender().tell(true, self());
          })
        .match(
          Reset.class, reset -> {
            this.confirmFail = reset.confirmFail;
            this.confirmNoResponse = reset.confirmNoResponse;

            sender().tell(true, self());
          })
        .match(
          Hold.class, hold -> {
            
            boolean held = false;

            if(flights.get(hold.flightCode) > 0){
              held = true;
            }

            getSender().tell(held, self());
          })
        .match(
          Confirm.class, confirm -> {
            boolean confirmed = true;
            if(confirmNoResponse){
              // do nothing
            }else{
              if(confirmFail){
                confirmed = false;
              }else{
                reserveSeat(confirm.flightCode);
              }
              getSender().tell(confirmed, self());
            }
            
          })
        .build();
  }

}