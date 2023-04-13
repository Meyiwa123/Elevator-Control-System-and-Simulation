public class Building {

    public static final int TOTAL_FLOOR = 22; //Floor numbers 0-21
    public static final int TOTAL_ELEVATOR = 4; //Elevator numbers 0-3
    public static final double MAX_SPEED = 1.71;
    public static final double ACCELERATION = 0.182;
    public static final int DOOR_MOVE_TIME = 1000;
    public static final double REPAIR_PROBABILITY = 0.6;
    public static final int MAX_ELEVATOR_MESSAGE = 10;
    public static String SIMULATION_FILE = "simulations.txt";

    // Ports
    public final static int SCHEDULER_PORT_NUM = 23;
    public final static int ELEVATOR_SUBSYSTEM_PORT_NUM = 69;
    public final static int FLOOR_SUBSYSTEM_PORT_NUM = 667;
    public final static int ELEVATOR_VIEW_PORT_NUM = 22;

    // Elevator
    // Lower number gives a higher priority
    public static final byte ELEVATOR_STUCK = 0;
    public static final byte DOOR_ISSUE = 1;
    public static final byte GET_ELEVATOR_REQUEST = 2;
    public static final byte ISSUE_FIXED = 3;
    public static final byte FIX_ELEVATOR_ERROR = 4;
    public final static byte ELEVATOR_ARRIVAL = 5;
    public final static byte REQUEST_ELEVATOR = 6;
    public static final byte ACKNOWLEDGE = 7;
    public static final byte AVERAGE_TRAVEL_TIME = 8;
    public static final byte TOTAL_SIMULATION_TIME = 9;

    private Building() {}
}