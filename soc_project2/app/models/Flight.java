public class Flight{

    String flightCode;
    int numberOfSeats;

    public Flight(String code, int seats){
        flightCode = code;
        numberOfSeats = seats;
    }

    public int getSeats(){
        return numberOfSeats;
    }

    public String getFlightCode(){
        return flightCode;
    }

    public void reserveSeat(){
        numberOfSeats--;
    }

    public boolean hasSeats(){
        boolean response = true;
        if(numberOfSeats <= 0){
            response = false;
        }

        return response;
    }
}