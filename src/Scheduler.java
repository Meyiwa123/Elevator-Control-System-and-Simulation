import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Scheduler in a building elevator simulation
 *
 * Scheduler receives requests for elevators from the floor subsystem, scheduling these requests to available elevators,
 * and forwarding messages to the appropriate subsystems when elevator events occur.
 *
 * The Scheduler class extends the MessageReceiver class, which provides methods for receiving and sending UDP packets.
 * It has several instance variables, including an array of the current floors of each elevator, an array of the
 * states of each elevator, and an ArrayList of pending requests.
 */

public class Scheduler extends MessageReceiver {
    private State state;
    private LocalTime startTime;
    private byte[] receivedMessage, sendMessage;
    private DatagramSocket sendSocket;
    private DatagramPacket sendPacket;
    private ArrayList<Request> requests;
    private int[] currentElevatorFloors;
    private int[] nextElevatorFloors;
    private LocalTime[] elevatorArrivalTimes;
    private Elevator.State[] elevatorStates;
    private enum State {RECEIVING_MESSAGE, SCHEDULING, CHECK_ELEVATOR_STUCK, FIXING_ELEVATOR_ERROR};

    public Scheduler() throws IOException, ClassNotFoundException, InterruptedException {
        super(Building.SCHEDULER_PORT_NUM);
        requests = new ArrayList<>();
        state = State.RECEIVING_MESSAGE;
        sendSocket = new DatagramSocket();
        startTime = LocalTime.now();

        elevatorStates = new Elevator.State[Building.TOTAL_ELEVATOR];
        Arrays.fill(elevatorStates, Elevator.State.IN_SERVICE);
        currentElevatorFloors = new int[Building.TOTAL_ELEVATOR];
        Arrays.fill(currentElevatorFloors, 1);
        nextElevatorFloors = new int[Building.TOTAL_ELEVATOR];
        Arrays.fill(nextElevatorFloors, 1);
        elevatorArrivalTimes = new LocalTime[Building.TOTAL_ELEVATOR];


        while(true) {
            updateState();
        }
    }

    /**
     * Uses a switch statement to determine what action to take based on its current state.
     * If it is currently receiving a message, it checks the message to determine what action to take.
     * If it is scheduling a request, it checks the pending requests and schedules the request to the closest
     * available elevator. If it is fixing an elevator error, it checks whether the elevator can be repaired and sends
     * a message to the appropriate subsystems.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void updateState() throws IOException, ClassNotFoundException, InterruptedException {
        switch (state) {
            case RECEIVING_MESSAGE -> {
                receivedMessage = getMessage();
                checkMessage();
            }
            case SCHEDULING -> {
                checkRequest();
            }
            case FIXING_ELEVATOR_ERROR -> {
                fixElevatorError();
            }
            case CHECK_ELEVATOR_STUCK -> {
                checkTravelTimeArrival();
            }
        }
    }

    /**
     *  Implements an algorithm that schedules the closest available elevator to the request floor. It iterates through
     *  all the elevators and finds the one that is closest to the request floor and is currently in service.
     *  If the request came from within the elevator it schedules the request from the source elevator
     *
     * @param request Request to schedule
     * @throws IOException
     */
    public void schedule(Request request) throws IOException {
        int closestElevator = -1;
        int minDistance = Integer.MAX_VALUE;
        int requestFloor = request.getFloorNumber();

        if(request.getRequestType() == Request.RequestType.INTERNAL) {
            if(elevatorStates[request.getElevatorNumber()] == Elevator.State.OUT_OF_SERVICE) {
                System.out.println("Error!!! Unable to schedule internal request, elevator out of service");
            }
            else {
                System.out.println("Scheduler: Scheduled internal request for floor " + requestFloor + " to elevator " + request.getElevatorNumber());
                sendRequest(request.getElevatorNumber(), request.getFloorNumber());
            }
            return;
        }

        for(int i=0; i< Building.TOTAL_ELEVATOR; i++) {
            if(elevatorStates[i] == Elevator.State.OUT_OF_SERVICE) {
                continue;
            }

            int distance = Math.abs(nextElevatorFloors[i] - requestFloor);
            if(distance < minDistance) {
                minDistance = distance;
                closestElevator = i;
            }
        }

        if(closestElevator == -1) {
            System.out.println("Error!!! Unable to schedule request, all elevators are out of service");
            return;
        }

        System.out.println("Scheduler: Scheduled request for floor " + requestFloor + " to elevator "+ closestElevator);
        sendRequest(closestElevator, request.getFloorNumber());
        LocalTime time = getElevatorArrivalTime(closestElevator, currentElevatorFloors[closestElevator], requestFloor);
        elevatorArrivalTimes[closestElevator] = time;
        nextElevatorFloors[closestElevator] = requestFloor;
    }

    /**
     * Returns the expected arrival time for the scheduled elevator by calculating the travel time and adding the
     * remaining travel time if the elevator is moving to a new floor
     *
     * @param elevatorNumber int elevator scheduled
     * @param currentFloor int current elevator floor
     * @param newFloor int new elevator floor
     * @return LocalTime expected time of arrival
     */
    public LocalTime getElevatorArrivalTime(int elevatorNumber, int currentFloor, int newFloor) {
        int distanceToTravel = Math.abs(newFloor - currentFloor);
        double timeToReachMaxSpeed = Building.MAX_SPEED / Building.ACCELERATION;
        double timeToReachDestination;

        if (timeToReachMaxSpeed * 2 >= distanceToTravel / Building.MAX_SPEED) {
            timeToReachDestination = Math.sqrt(2 * distanceToTravel / Building.ACCELERATION);
        } else {
            timeToReachDestination = timeToReachMaxSpeed + (distanceToTravel - Building.MAX_SPEED * timeToReachMaxSpeed) / Building.MAX_SPEED;
        }

        long timeMillis = System.currentTimeMillis();
        LocalTime currentTime = LocalTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault());
        LocalTime arrivalTime = currentTime.plusSeconds((long) (timeToReachDestination + 3));   // Add UDP send receive delay time of 3 seconds

        // Add time to travel to current travel time if elevator is moving
        if (currentElevatorFloors[elevatorNumber] != nextElevatorFloors[elevatorNumber]) {
            LocalTime elevatorArrivalTime = elevatorArrivalTimes[elevatorNumber];
            if (elevatorArrivalTime != null) {
                Duration duration = Duration.between(LocalTime.now(), elevatorArrivalTime);
                long seconds = duration.getSeconds();
                arrivalTime = arrivalTime.plusSeconds(seconds);
            }
        }


        if (currentElevatorFloors[elevatorNumber] != nextElevatorFloors[elevatorNumber]) {
            Duration duration = Duration.between(LocalTime.now(), elevatorArrivalTimes[elevatorNumber]);
            long seconds = duration.getSeconds();

            arrivalTime = arrivalTime.plusSeconds(seconds);
        }

        return arrivalTime;
    }

    /**
     * Checks if any elevator moving has exceeded their arrival time. If so the elevator is to be assumed stuck and
     * an elevator stuck error will be notified to the Elevator Subsystem
     *
     * @throws IOException
     */
    public void checkTravelTimeArrival() throws IOException {
        for(int i=0; i<Building.TOTAL_ELEVATOR; i++) {
            // Does not check elevators that are out of service
            if(elevatorStates[i] == Elevator.State.OUT_OF_SERVICE) {
                continue;
            }
            if(currentElevatorFloors[i] != nextElevatorFloors[i]) {
                try {
                    long value = LocalTime.now().compareTo(elevatorArrivalTimes[i]);
                    if(value>=0) {
                        receivedMessage = new byte[]{Building.ELEVATOR_STUCK, (byte) i};
                        System.out.println("Scheduler: Elevator " + i + " is out of service, elevator is stuck");
                        updateElevatorState(i, "OUT_OF_SERVICE");
                        forwardMessage("Elevator Subsystem");
                    }
                }
                catch(NullPointerException ignore) {}
            }
        }
        state = State.RECEIVING_MESSAGE;
    }

    public void updateCurrentElevatorFloor(int elevator, int newFloor) {
        currentElevatorFloors[elevator] = newFloor;
    }

    public void updateElevatorState(int elevator,  String newState) {
        elevatorStates[elevator] = Elevator.State.valueOf(newState);
    }

    /**
     * Checks if there are any pending requests. If there are, it schedules the next request and removes it from the
     * list of pending requests. If there are no requests, it returns to the receiving message state.
     * @throws IOException
     */
    public void checkRequest() throws IOException {
        if(requests.isEmpty()) {
            state = State.CHECK_ELEVATOR_STUCK;
        }

        Request currentRequest = null;
        for(Request request : requests) {
            schedule(request);
            currentRequest = request;
            break;
        }

        requests.remove(currentRequest);
    }

    /**
     * Checks whether the elevator can be repaired and updates the state of the elevator. If the elevator can be
     * repaired, it sends a message to the elevator subsystem to indicate that the issue has been fixed. If the elevator
     * cannot be repaired, it does not send a message.
     * @throws IOException
     */
    public void fixElevatorError() throws IOException {
        Random rand = new Random();
        float repairChance = rand.nextFloat(10);
        if((0.1*repairChance) <= Building.REPAIR_PROBABILITY) {
            System.out.println("Scheduler: Elevator " + receivedMessage[1] + " is in service");
            updateElevatorState(receivedMessage[1], "IN_SERVICE");

            receivedMessage[0] = Building.ISSUE_FIXED;
            System.out.println("Scheduler: Sending issue fixed sendPacket to Elevator Subsystem " + receivedMessage[1]);
            sendPacket = new DatagramPacket(receivedMessage, receivedMessage.length, InetAddress.getLocalHost(), Building.ELEVATOR_SUBSYSTEM_PORT_NUM+receivedMessage[1]);
            sendSocket.send(sendPacket);
            System.out.println("Scheduler: Packets sent.");
        }
        else {
            System.out.println("Scheduler: Unable to repair elevator " + receivedMessage[1]);
            getElevatorRequests();
        }
        state = State.CHECK_ELEVATOR_STUCK;
    }

    /**
     * Sends a message to the ElevatorSubsystem to get all the remaining requests it is unable to do
     */
    public void getElevatorRequests() throws IOException {
        byte[] msg = new byte[]{Building.GET_ELEVATOR_REQUEST};
        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.ELEVATOR_SUBSYSTEM_PORT_NUM + receivedMessage[1]);
        System.out.println("Scheduler: Sending message to Elevator Subsystem " + receivedMessage[1] + " to get all request floors");
        sendSocket.send(sendPacket);
        System.out.println("Scheduler: Packet sent.");
    }

    /**
     * Sends a request for an elevator to a specific elevator subsystem and to the floor subsystem.
     * It creates a DatagramPacket object containing the message and sends it to the appropriate address.
     *
     * @param elevatorNumber int request elevator
     * @param newFloor int request floor
     * @throws IOException
     */
    public void sendRequest(int elevatorNumber, int newFloor) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(Building.REQUEST_ELEVATOR);
        bos.write(elevatorNumber);
        bos.write(newFloor);

        byte[] msg = bos.toByteArray();
        bos.close();

        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.ELEVATOR_SUBSYSTEM_PORT_NUM + elevatorNumber);
        System.out.println("Scheduler: Sending request for elevator " + elevatorNumber + " to floor " + newFloor + " to Elevator Subsystem");
        sendSocket.send(sendPacket);
        System.out.println("Scheduler: Packet sent.");

        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.FLOOR_SUBSYSTEM_PORT_NUM);
        System.out.println("Scheduler: Sending request to Floor Subsystem");
        sendSocket.send(sendPacket);
        System.out.println("Scheduler: Packet sent.");
    }

    /**
     * Forwards received message to the appropriate subsystem (either the floor subsystem or the elevator subsystem)
     * depending on the value of the destination parameter.
     *
     * @param destination String message destination
     * @throws IOException
     */
    public void forwardMessage(String destination) throws IOException {
        //Elevator arrival
        if(destination.equals("Floor Subsystem")) {
            System.out.println("Scheduler: Sending elevator arrival packet to Floor Subsystem");
            sendPacket = new DatagramPacket(receivedMessage, receivedMessage.length, InetAddress.getLocalHost(), Building.FLOOR_SUBSYSTEM_PORT_NUM);
            sendSocket.send(sendPacket);
        }
        //Door issue
        if(destination.equals("Elevator Subsystem")) {
            System.out.println("Scheduler: Sending issue packet to Elevator Subsystem");
            sendPacket = new DatagramPacket(receivedMessage, receivedMessage.length, InetAddress.getLocalHost(), Building.ELEVATOR_SUBSYSTEM_PORT_NUM+receivedMessage[1]);
            sendSocket.send(sendPacket);
        }
        //Simulation time
        if(destination.equals("Simulation time")) {
            System.out.println("Scheduler: Sending simulation time packet to Elevator View");
            sendPacket = new DatagramPacket(sendMessage, sendMessage.length, InetAddress.getLocalHost(), Building.ELEVATOR_VIEW_PORT_NUM);
            sendSocket.send(sendPacket);
        }
        System.out.println("Scheduler: Packet sent.");
    }

    /**
     * Updates the time the simulation takes to complete all request
     */
    public void updateSimulationTime() throws IOException {
        boolean complete = true;
        for(int i=0; i<Building.TOTAL_ELEVATOR; i++) {
            if(elevatorStates[i] == Elevator.State.OUT_OF_SERVICE) {
                continue;
            }
            if(currentElevatorFloors[i] != nextElevatorFloors[i]) {
                complete = false;
                break;
            }
        }
        if(complete) {
            Duration duration = Duration.between(startTime, LocalTime.now());
            long diffInSeconds = duration.getSeconds();
            System.out.println("Time taken to complete simulation(s): " + diffInSeconds);

            sendMessage = new byte[]{Building.TOTAL_SIMULATION_TIME, (byte) diffInSeconds};
            forwardMessage("Simulation time");
        }
    }

    public void checkMessage() throws IOException, ClassNotFoundException {
        System.out.println("Scheduler: Packet received");

        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivedMessage));
            Request request = (Request) in.readObject();
            if(request != null) {
                requests.add(request);
                in.close();
                state = State.SCHEDULING;
            }
        }
        catch(StreamCorruptedException ignore){}

        if(receivedMessage[0] == Building.DOOR_ISSUE) {
            System.out.println("Scheduler: Elevator " + receivedMessage[1] + " is out of service");
            updateElevatorState(receivedMessage[1], "OUT_OF_SERVICE");
            forwardMessage("Elevator Subsystem");
        }

        if(receivedMessage[0] == Building.ELEVATOR_STUCK) {
            System.out.println("Scheduler: Elevator " + receivedMessage[1] + " is out of service");
            updateElevatorState(receivedMessage[1], "OUT_OF_SERVICE");
            forwardMessage("Elevator Subsystem");
            getElevatorRequests();
        }

        if(receivedMessage[0] == Building.ELEVATOR_ARRIVAL) {
            updateCurrentElevatorFloor(receivedMessage[1], receivedMessage[2]);
            forwardMessage("Floor Subsystem");
            updateSimulationTime();
        }

        if(receivedMessage[0] == Building.FIX_ELEVATOR_ERROR) {
            state = State.FIXING_ELEVATOR_ERROR;
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        new Scheduler();
    }
}
