import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Elevator Subsystem in a building elevator simulation.
 *
 * The ElevatorSubsystem class is responsible for managing the state of a single elevator in the building,
 * receiving and sending messages from and to the Scheduler. The class extends the MessageReceiver class, which provides
 * a way to receive messages from other components through a socket connection. The constructor initializes the state
 * of the elevator subsystem and creates a DatagramSocket for sending messages.
 *
 * The class implements the Runnable interface, which allows it to be run as a separate thread.
 * The run() method continuously updates the state of the elevator subsystem by calling the updateState() method.
 */
public class ElevatorSubsystem extends MessageReceiver implements Runnable{
    private State state;
    private Elevator elevator;
    private int elevatorNumber;
    private byte[] receivedMessage;
    private DatagramSocket sendSocket;
    private DatagramPacket sendPacket;
    private ArrayList<Integer> requestFloors;
    private enum State {RECEIVING_MESSAGE, MOVING_ELEVATOR, NEW_FLOOR};

    public ElevatorSubsystem(int elevatorNumber) throws IOException {
        super(Building.ELEVATOR_SUBSYSTEM_PORT_NUM + elevatorNumber);
        elevator = new Elevator(elevatorNumber);
        this.elevatorNumber = elevatorNumber;
        requestFloors = new ArrayList<>();
        sendSocket = new DatagramSocket();
        state = State.RECEIVING_MESSAGE;
    }

    @Override
    public byte[] getMessage() throws InterruptedException {
        synchronized(receivedMessages) {
            while(receivedMessages.isEmpty()) {
                receivedMessages.wait();
            }

            if(receivedMessages.peek()[0]==Building.REQUEST_ELEVATOR) {
                return null;
            }

            return receivedMessages.poll();
        }
    }

    /**
     * Determine the current state of the elevator subsystem and performs actions based on that state.
     * If the state is "RECEIVING_MESSAGE", the method receives a message from the socket connection and checks it.
     * If the state is "NEW_FLOOR", the method checks whether the elevator has arrived at a new floor.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void updateState() throws IOException, InterruptedException {
        switch(state) {
            case RECEIVING_MESSAGE -> {
                receivedMessage = getMessage();
                checkMessage();
                getNewFloor();
            }
            case MOVING_ELEVATOR -> {
                moveElevator();
            }
            case NEW_FLOOR -> {
                checkNewFloor();
            }
        }
    }

    /**
     * Checks the type of message received and updates the state of the elevator accordingly.
     * If the message is a request for the elevator, the elevator is updated to the "MOVING" state and the state of the
     * elevator subsystem is changed to "NEW_FLOOR". If the message is a request to move the elevator doors,
     * the elevator is updated to the "TOGGLE_DOOR" state. If the message indicates an elevator door issue, the elevator
     * is updated to the "OUT_OF_SERVICE" state and a message is sent to the Scheduler to attempt to fix the issue.
     * If the message indicates that an elevator issue has been fixed, the elevator is updated to the "IN_SERVICE" state.
     *
     * @throws IOException
     */
    public void checkMessage() throws IOException {
        System.out.println(Thread.currentThread().getName() + ": Elevator packet received");
        if(receivedMessage == null) {
            return;
        }
        if(receivedMessage[0] == Building.DOOR_ISSUE) {
            elevator.updateState(Elevator.State.OUT_OF_SERVICE, null);
            forwardMessage();
            sendAttemptFix();
        }
        if(receivedMessage[0] == Building.ELEVATOR_STUCK) {
            elevator.updateState(Elevator.State.OUT_OF_SERVICE, null);
            forwardMessage();
        }
        if(receivedMessage[0] == Building.ISSUE_FIXED) {
            elevator.updateState(Elevator.State.IN_SERVICE, null);
            forwardMessage();
        }
        if(receivedMessage[0] == Building.GET_ELEVATOR_REQUEST) {
            sendRequests();
        }
    }

    /**
     * Checks the receivedMessages list for any new floor request. If there is a floor request the request floor for the
     * elevator is added to the requestFloors list
     * @throws InterruptedException
     */
    public void getNewFloor() throws InterruptedException {
        synchronized(receivedMessages) {
            while(receivedMessages.isEmpty()) {
                receivedMessages.wait();
            }

            ArrayList<byte[]> removeList = new ArrayList<>();
            for(byte[] message : receivedMessages) {
                if(message[0] == Building.REQUEST_ELEVATOR) {
                    requestFloors.add((int) message[2]);
                    removeList.add(message);
                }
            }
            receivedMessages.removeAll(removeList);
        }

        if(!requestFloors.isEmpty()) {
            state = State.MOVING_ELEVATOR;
        }
    }

    /**
     * This method moves the elevator to all the requested floors in the requestFloors list. If there is no elevator
     * request made the state switches to RECEIVING_MESSAGE
     */
    public void moveElevator() {
        if(requestFloors.isEmpty() || elevator.getElevatorState()== Elevator.State.OUT_OF_SERVICE) {
            state = State.RECEIVING_MESSAGE;
        } else {
            sortRequestFloors();
            elevator.updateState(Elevator.State.MOVING, requestFloors.remove(0));
            state = State.NEW_FLOOR;
        }
    }

    /**
     * Creates a new sorted list containing the floor order the elevator should travel in
     * @return ArrayList<Integer> requestFloors
     */
    public void sortRequestFloors() {
        requestFloors.add(elevator.getCurrentFloor());  // add elevator current floor
        Collections.sort(requestFloors);
        ArrayList<Integer> sortedRequest = new ArrayList<>();
        int index = requestFloors.indexOf(elevator.getCurrentFloor());

        List<Integer> sublist1 = requestFloors.subList(0, index); // create first sublist
        List<Integer> sublist2 = requestFloors.subList(index, requestFloors.size()); // create second sublist
        Collections.sort(sublist1, Comparator.reverseOrder()); // sort first sublist in descending order

        // combine sublist
        if(elevator.getDirection() == Elevator.Direction.UP) {
            sortedRequest.addAll(sublist2);
            sortedRequest.addAll(sublist1);
        }
        else {
            sortedRequest.addAll(sublist1);
            sortedRequest.addAll(sublist2);
        }
        requestFloors = sortedRequest;
        requestFloors.remove(requestFloors.indexOf(elevator.getCurrentFloor()));   // remove elevator current floor
    }

    /**
     * Checks whether the elevator has arrived at a new floor, and if so, sends a message to the
     * Scheduler indicating the current floor of the elevator.
     * @throws IOException
     */
    public void checkNewFloor() throws IOException {
        if(elevator.isNewFloor()) {
            System.out.println(Thread.currentThread().getName() + ": Elevator arrived at floor " + elevator.getCurrentFloor());
            elevator.updateState(Elevator.State.TOGGLE_DOOR, null);
            sendElevatorFloor(elevator.getCurrentFloor());
            elevator.setNewFloorFalse();
            forwardTime();
        }
        state = State.MOVING_ELEVATOR;
    }

    /**
     * Sends a message to the Scheduler to attempt to fix an elevator door issue.
     * @throws IOException
     */
    public void sendAttemptFix() throws IOException {
        byte[] msg = {Building.FIX_ELEVATOR_ERROR, (byte) elevatorNumber};

        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
        System.out.println(Thread.currentThread().getName() + ": Sending fix elevator packet for elevator to Scheduler");
        sendSocket.send(sendPacket);
        System.out.println(Thread.currentThread().getName() + ": Packet sent.");
    }

    /**
     * Sends a message to the Scheduler indicating the current floor of the elevator.
     * @param newFloor
     * @throws IOException
     */
    public void sendElevatorFloor(int newFloor) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(Building.ELEVATOR_ARRIVAL);
        bos.write(elevatorNumber);
        bos.write(newFloor);

        byte[] msg = bos.toByteArray();
        bos.close();

        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
        System.out.println(Thread.currentThread().getName() + ": Sending arrival packet for elevator to Scheduler");
        sendSocket.send(sendPacket);
        System.out.println(Thread.currentThread().getName() + ": Packet sent.");
    }

    /**
     * Sends the elevator request to the scheduler, in the case the elevator is out of service
     */
    public void sendRequests() throws IOException {
        for(Integer requestFloor : requestFloors) {
            Request request = new Request(requestFloor, elevatorNumber, "UP", LocalTime.now(), "EXTERNAL");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(request);
            out.flush();
            byte[] msg = bos.toByteArray();
            bos.close();

            sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
            System.out.println(Thread.currentThread().getName() + ": Sending elevator request to Scheduler");
            sendSocket.send(sendPacket);
            System.out.println(Thread.currentThread().getName() + ": Packet sent.");
        }
    }

    /**
     * Forwards received message to the ElevatorView
     *
     * @throws IOException
     */
    public void forwardMessage() throws IOException {
        System.out.println(Thread.currentThread().getName() + ": Sending packet to Elevator View");
        sendPacket = new DatagramPacket(receivedMessage, receivedMessage.length, InetAddress.getLocalHost(), Building.ELEVATOR_VIEW_PORT_NUM);
        sendSocket.send(sendPacket);
        System.out.println(Thread.currentThread().getName() + ": Packet sent.");
    }

    public void forwardTime() throws IOException {
        byte[] msg = new byte[] {Building.AVERAGE_TRAVEL_TIME, (byte) elevatorNumber, (byte) elevator.getAverageTravelTime()};
        System.out.println(Thread.currentThread().getName() + ": Sending packet to Elevator View");
        sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.ELEVATOR_VIEW_PORT_NUM);
        sendSocket.send(sendPacket);
        System.out.println(Thread.currentThread().getName() + ": Packet sent.");
    }

    @Override
    public void run() {
        while(true) {
            try {
                updateState();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creates multiple instances of the ElevatorSubsystem class and starts a thread for each instance.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        for(int i=0; i<Building.TOTAL_ELEVATOR; i++) {
            Thread thread = new Thread(new ElevatorSubsystem(i), "Elevator Subsystem "+ i);
            thread.start();
        }
    }
}
