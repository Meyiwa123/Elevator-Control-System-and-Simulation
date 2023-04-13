import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Elevator class represents an elevator in a building
 */

public class Elevator {
    private Lamp lamp;
    private State state;
    private int numRequest;
    public boolean newFloor;
    private int currentFloor;
    private int elevatorNumber;
    public Direction direction;
    private DoorState doorState;
    private int totalTravelTime;
    private ArrayList<int[]> travelTimes;
    public enum Direction {UP, DOWN};
    public enum DoorState {OPEN, CLOSED};
    public enum State {MOVING, OUT_OF_SERVICE, IN_SERVICE, TOGGLE_DOOR, ON_LAMP, OFF_LAMP};

    public Elevator(int elevatorNumber) {
        numRequest = 0;
        currentFloor = 0;
        newFloor = false;
        totalTravelTime = 0;
        direction = Direction.UP;
        state = State.IN_SERVICE;
        doorState = DoorState.CLOSED;
        travelTimes = new ArrayList<>();
        this.elevatorNumber = elevatorNumber;
        lamp = new Lamp(Building.TOTAL_FLOOR);
    }

    /**
     * Return the average time it takes for the elevator to travel between floors
     *
     * @return double averageTravelTime
     */
    public double getAverageTravelTime() {
        if(numRequest==0) {
            return 0;
        } else {
            return totalTravelTime / numRequest;
        }
    }

    /**
     * Return the floor in which the elevator is at
     * @return int current floor
     */
    public int getCurrentFloor() {
        return currentFloor;
    }

    /**
     * Return the number of the elevator
     * @return int elevator number
     */
    public int getElevatorNumber() {
        return elevatorNumber;
    }

    /**
     * Return the current state of the elevator
     * @return State elevator state
     */
    public State getElevatorState() {
        return state;
    }

    /**
     * Return the direction in which the elevator is traveling
     * @return Direction direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns a boolean indicating if the elevator has arrived at a new floor
     * @return boolean newFloor
     */
    public boolean isNewFloor() {
        return newFloor;
    }

    /**
     * Sets newFloor variable to false
     */
    public void setNewFloorFalse() {
        this.newFloor = false;
    }

    /**
     * Changes the doorState variable and pauses the program for a certain period of time to
     * simulate the door opening or closing.
     * @param doorState
     */
    public void setDoorState(DoorState doorState) {
        try {
            Thread.sleep(Building.DOOR_MOVE_TIME);
        } catch (InterruptedException ignored) {}
        this.doorState = doorState;
    }

    /**
     * Changes the currentFloor variable and pauses the program for a certain period of time
     * to simulate the elevator moving to a new floor.
     * @param newFloor
     */
    public void setCurrentFloor(int newFloor) {
        int distanceToTravel = Math.abs(newFloor - currentFloor);
        double timeToReachMaxSpeed = Building.MAX_SPEED / Building.ACCELERATION;
        double timeToReachDestination;

        if (timeToReachMaxSpeed * 2 >= distanceToTravel / Building.MAX_SPEED) {
            timeToReachDestination = Math.sqrt(2 * distanceToTravel / Building.ACCELERATION);
        }
        else {
            timeToReachDestination = timeToReachMaxSpeed + (distanceToTravel - Building.MAX_SPEED * timeToReachMaxSpeed) / Building.MAX_SPEED;
        }
        try {
            Thread.sleep(1000 * (long) timeToReachDestination);
        } catch (InterruptedException ignored) {}

        numRequest++;
        int travelTime = (int) ((1000*timeToReachDestination) / 1000);

        // Update total travel time of the elevator
        totalTravelTime += travelTime;

        // Record travel time for request
        int[] request = new int[]{distanceToTravel, travelTime};
        travelTimes.add(request);

        // Export Measured times to file
        saveTime();

        System.out.println(totalTravelTime);
        currentFloor = newFloor;
        this.newFloor = true;
    }

    /**
     * Saves the contents of the travel time ArrayList and the average time along with calculated
     * values to a specified file.
     *
     * @throws IOException if there is an error writing to the file
     */

    public void saveTime() {
        // Calculate values
        double ave = getAverageTravelTime();
        double variance1 = 0.0;
        for (int i = 0; i<travelTimes.size(); i++) {
            variance1 += (travelTimes.get(i)[1] - ave) * (travelTimes.get(i)[1] - ave);
        }
        double variance = variance1 / (travelTimes.size() - 1);
        double standardDeviation= Math.sqrt(variance);
        // Critical value (z) of 95% confidence is 1.96
        double lower = ave - 1.96 * standardDeviation;
        double higher = ave + 1.96 * standardDeviation;


        String fileName = "Elevator " + elevatorNumber + " measured time.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (int[] time : travelTimes) {
                writer.write("Traveled distance(floors): " + time[0] + ", time taken (s): " + time[1]);
                writer.newLine();
            }
            writer.write("Average time (s): " + getAverageTravelTime());
            writer.newLine();
            writer.write("sample variance: " + variance);
            writer.newLine();
            writer.write("Sample standard deviation: " + standardDeviation);
            writer.newLine();
            writer.write("Approximate confidence interval: [" + lower + ", " + higher + "]");
            writer.newLine();
            writer.write("With 95% confidence the mean is between " + lower+ " and " + higher + " , based on " + travelTimes.size() + " samples.");
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
    }
    public void loadUnloadPassengers() {
        System.out.println("Elevator " + elevatorNumber + ": Opening Doors");
        setDoorState(DoorState.OPEN);
        System.out.println("Elevator " + elevatorNumber + ": Doors open");
        System.out.println("Elevator " + elevatorNumber + ": Closing Doors");
        setDoorState(DoorState.CLOSED);
        System.out.println("Elevator " + elevatorNumber + ": Doors closed");
    }

    public void setDirection(int num) {
        if(num > currentFloor) {
            direction = Direction.UP;
        }
        else {
            direction = Direction.DOWN;
        }
    }

    /**
     * Changes the state variable and performs the corresponding action depending on the new state.
     * If the new state is MOVING, the elevator will move to a new floor and print a message to the console.
     * If the new state is IN_SERVICE, the elevator will become available for use again.
     * If the new state is OFF_LAMP, the corresponding light inside the elevator will turn off.
     * @param state
     * @param num
     */
    public void updateState(State state, Integer num) {
        if(this.state == State.OUT_OF_SERVICE && state != State.IN_SERVICE) {
            System.out.println("Error!!! Elevator unable to move, out of service");
            return;
        }
        switch (state) {
            case MOVING -> {
                System.out.println("Elevator " + elevatorNumber + ": Moving to floor " + num  + " at " + LocalTime.now());
                setDirection(num);
                setCurrentFloor(num);
                System.out.println("Elevator " + elevatorNumber + ": Arrived at floor " + num  + " at " + LocalTime.now());
            }
            case OUT_OF_SERVICE -> {
                this.state = State.OUT_OF_SERVICE;
                System.out.println("Elevator " + elevatorNumber + ": Out of service");
            }
            case IN_SERVICE -> {
                this.state = State.IN_SERVICE;
                System.out.println("Elevator " + elevatorNumber + ": In service");
            }
            case TOGGLE_DOOR -> {
                loadUnloadPassengers();
            }
            case ON_LAMP -> {
                lamp.setLightState(num, Lamp.LightState.ON);
                System.out.println("Elevator " + elevatorNumber + ": Elevator lamp " + num + " turned on");
            }
            case OFF_LAMP -> {
                lamp.setLightState(num, Lamp.LightState.OFF);
                System.out.println("Elevator " + elevatorNumber + ": Elevator lamp " + num + " turned off");
            }
        }
    }
}
