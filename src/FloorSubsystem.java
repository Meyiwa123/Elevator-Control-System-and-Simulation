import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Floor Subsystem in a building elevator simulation.
 *
 * The Floor Subsystem is responsible for sending requests to the Scheduler, receiving arrival notifications from
 * elevators, and turning on/off request lamps on the floor. The class implements the Runnable interface.
 *
 * The floorNumber represents the number of the floor the subsystem is responsible for.
 * The startTime represents the startTime at which the subsystem starts running.
 * The receivedMessage represents the latest message received from an elevator.
 * The sendSocket and SendPacket are used to send messages to the Scheduler.
 * The requests ArrayList contains all requests that the FloorSubsystem is responsible for sending to the Scheduler.
 * The floor variable represents an instance of the Floor class, which has a Lamp object that represents the request lamps on the floor.
 */

public class FloorSubsystem extends MessageReceiver implements Runnable {
    private LocalTime startTime;
    private byte[] receivedMessage;
    private ArrayList<Floor> floors;
    private ArrayList<Issue> issues;
    private DatagramSocket sendSocket;
    private DatagramPacket SendPacket;
    private ArrayList<Request> requests;

    /**
     * Constructor of FloorSubsystem.
     * The constructor initializes global variables, then waits for the Scheduler to start running before reading the simulation file.
     * Then, the method checks for any received messages and calls the checkMessage() method if there are any.
     * If there are no received messages, the method calls the checkRequest() method.
     * The run() method loops continuously, performing these checks repeatedly.
     */
    public FloorSubsystem() throws IOException {
        super(Building.FLOOR_SUBSYSTEM_PORT_NUM);
        sendSocket = new DatagramSocket();
        requests = new ArrayList<>();
        issues = new ArrayList<>();
        floors = new ArrayList<>();
        startTime = LocalTime.now();
        createFloors();
    }

    public void createFloors() {
        for(int i=0; i<Building.TOTAL_FLOOR; i++) {
            floors.add(new Floor(i));
        }
    }

    /**
     * FloorSubsystem receives a message from an elevator indicating
     * that it has arrived at the floor. The method turns off the request
     * lamp for the elevator that arrived.
     */
    public void notifyArrival() {
        System.out.println("Floor Subsystem: Elevator " + receivedMessage[1] + " has arrived at floor " + receivedMessage[2]);
        System.out.println("Floor Subsystem: Elevator " + receivedMessage[1] + " request lamp is turned off at floor " + receivedMessage[2]);
        floors.get(receivedMessage[2]).lamp.setLightState(receivedMessage[1], Lamp.LightState.OFF);
    }

    /**
     * FloorSubsystem receives a message from an elevator indicating
     * that a request lamp has been turned on. The method turns on the request
     * lamp for the elevator that made the request.
     */
    public void notifyRequest() {
        System.out.println("Floor Subsystem: Elevator " + receivedMessage[1] + " request lamp is turned on at floor " + receivedMessage[2]);
        floors.get(receivedMessage[2]).lamp.setLightState(receivedMessage[1], Lamp.LightState.ON);
    }

    /**
     * Checks the type of the latest received message and calls the corresponding method
     */
    public void checkMessage() throws IOException {
        System.out.println("Floor Subsystem: Packet received");
        if(receivedMessage[0] == Building.ELEVATOR_ARRIVAL) {
            notifyArrival();
        }
        if(receivedMessage[0] == Building.REQUEST_ELEVATOR) {
           notifyRequest();
        }
        forwardMessage();
    }

    /**
     * Forwards received message to the ElevatorView
     *
     * @throws IOException
     */
    public void forwardMessage() throws IOException {
        System.out.println("Floor Subsystem: Sending packet to Elevator View");
        SendPacket = new DatagramPacket(receivedMessage, receivedMessage.length, InetAddress.getLocalHost(), Building.ELEVATOR_VIEW_PORT_NUM);
        sendSocket.send(SendPacket);
        System.out.println("Floor Subsystem: Packet sent.");
    }

    /**
     *  Reads the simulation file and generates requests for the
     *  FloorSubsystem to send to the Scheduler. The method also generates door issue requests for elevators
     *
     * @throws IOException
     */
    public synchronized void readFile() throws IOException {
        FileInputStream fstream = new FileInputStream(Building.SIMULATION_FILE);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = br.readLine()) != null) {
            String[] tmp = line.split(" ");

            if(tmp[2].equals("DOOR_ISSUE")) {
                Issue issue = new Issue(Building.DOOR_ISSUE, Integer.parseInt(tmp[1]) , LocalTime.parse(tmp[0]));
                System.out.println("Floor Subsystem: Door issue generated - " + issue.toString());
                issues.add(issue);
                continue;
            }

            if(tmp[2].equals("ELEVATOR_STUCK")) {
                Issue issue = new Issue(Building.ELEVATOR_STUCK, Integer.parseInt(tmp[1]) , LocalTime.parse(tmp[0]));
                System.out.println("Floor Subsystem: Elevator stuck issue generated - " + issue.toString());
                issues.add(issue);
                continue;
            }

            int floorNumber = Integer.parseInt(tmp[1]);
            int elevatorNumber = Integer.parseInt(tmp[3]);
            LocalTime requestTime = LocalTime.parse(tmp[0]);

            Request request = new Request(floorNumber, elevatorNumber, tmp[2], requestTime, tmp[4]);

            System.out.println("Floor Subsystem: Request generated - " + request.toString());
            requests.add(request);
        }
    }

    /**
     * Checks if there are any pending requests that the FloorSubsystem needs to send to the Scheduler.
     * If there are, the method selects the first request that has a request startTime that has already passed,
     * removes it from the requests ArrayList, and sends it to the Scheduler using the sendRequest() method.
     *
     * @throws IOException
     */
    public void checkRequest() throws IOException {
        if(requests.isEmpty()) {
            return;
        }

        Request currentRequest = null;
        for(Request request : requests) {
            int value = startTime.compareTo(request.getRequestTime());
            if(value>=0) {
                currentRequest = request;
                break;
            }
        }

        if(currentRequest == null) {
            return;
        }

        requests.remove(currentRequest);
        sendRequest(currentRequest);
    }

    /**
     * Check if there are any pending issues that the FloorSubsystem needs to send to the Scheduler.
     * If there are, the method selects the first issue that has a request startTime that has already passed,
     * removes it from the requests ArrayList, and sends it to the Scheduler using the sendIssue() method
     */
    public void checkIssues() throws IOException {
        if(issues.isEmpty()) {
            return;
        }

        Issue currentIssue = null;
        for(Issue issue: issues) {
            int value = startTime.compareTo(issue.getRequestTime());
            if(value>=0) {
                currentIssue = issue;
                break;
            }
        }

        if(currentIssue==null) {
            return;
        }

        issues.remove(currentIssue);
        sendIssue(currentIssue);
    }

    /**
     * Sends a issue to the Scheduler. The method converts the issue object to a
     * byte array and sends it to the Scheduler using the sendSocket and SendPacket instance variables.
     *
     * @param issue
     * @throws IOException
     */
    public void sendIssue(Issue issue) throws IOException {
        byte[] msg = new byte[0];
        if(issue.getIssue() == Building.DOOR_ISSUE) {
            msg = new byte[]{Building.DOOR_ISSUE, (byte) issue.getElevatorNumber()};
        }
        if(issue.getIssue() == Building.ELEVATOR_STUCK) {
            msg = new byte[]{Building.ELEVATOR_STUCK, (byte) issue.getElevatorNumber()};
        }

        SendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
        System.out.println("Floor Subsystem: Sending issue packet to Scheduler");
        sendSocket.send(SendPacket);
        System.out.println("Floor Subsystem: Packet sent.");
    }

    /**
     * Sends a request to the Scheduler. The method converts the request object to a
     * byte array and sends it to the Scheduler using the sendSocket and SendPacket instance variables.
     *
     * @param request
     * @throws IOException
     */
    public void sendRequest(Request request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(request);
        out.flush();
        byte[] msg = bos.toByteArray();
        bos.close();

        SendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
        System.out.println("Floor Subsystem: Sending request packet to Scheduler");
        sendSocket.send(SendPacket);
        System.out.println("Floor Subsystem: Packet sent.");
    }

    @Override
    public void run() {
        //Wait for Scheduler to run
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            readFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while(true) {
            if(receivedMessagesIsEmpty()) {
                try {
                    checkRequest();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    checkIssues();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            try {
                receivedMessage = getMessage();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                checkMessage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creates and starts several FloorSubsystem threads, one for each floor in the building
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException {
        Thread thread = new Thread(new FloorSubsystem());
        thread.start();
    }
}

