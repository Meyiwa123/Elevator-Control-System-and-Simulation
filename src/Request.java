import java.io.Serializable;
import java.time.LocalTime;

/***
 * Request class represents an elevator call made by a passenger.
 */

public class Request implements Serializable {
    private int floorNumber;
    private int elevatorNumber;
    private LocalTime requestTime;
    private Direction direction;
    private RequestType requestType;
    public enum Direction {UP, DOWN};
    public enum RequestType {INTERNAL, EXTERNAL};

    /**
     * Constructor, creates a new request.
     *
     * @param floorNumber int request floor
     * @param elevatorNumber int elevator number
     * @param direction String direction of travel
     * @param requestTime int time (seconds) of elevator request by passenger
     * @param requestType indicated if the request is from inside/outside the elevator
     */
    public Request(int floorNumber, int elevatorNumber, String direction, LocalTime requestTime, String requestType) {
        this.floorNumber = floorNumber;
        this.elevatorNumber = elevatorNumber;
        this.requestTime = requestTime;
        this.direction = Direction.valueOf(direction);
        this.requestType = RequestType.valueOf(requestType);
    }

    /**
     * Return the floor in which the request was made from.
     * @return int floor number
     */
    public int getFloorNumber() {
        return floorNumber;
    }

    /**
     * Return the elevator that the request was made from.
     * @return int elevator number
     */
    public int getElevatorNumber() {
        return elevatorNumber;
    }

    /**
     * Return the direction the passenger intends to travel.
     * @return String direction
     */
    public String getDirection() {
        return direction.toString();
    }

    /**
     * Return the time (hh:mm:ss.mmm) in which the request will be made after
     * the simulation begins.
     * @return int time of request
     */
    public LocalTime getRequestTime() {
        return requestTime;
    }

    /**
     * Return where the request was generated from
     * @return RequestType source of request
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * Returns a description of the request made.
     * @return String request to string
     */
    public String toString() {
        return "Floor: " +floorNumber + ", Elevator: " +elevatorNumber + ", Direction: " + direction.toString()  + ", Time of request: " + requestTime.toString() + " Type of request: " + requestType.toString();
    }
}