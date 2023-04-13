import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Elevator View is the Graphical User Interface (GUI) for an elevator simulation
 *
 * The ElevatorView class extends the MessageReceiver class and implements the ActionListener interface. creates a
 * JMenuBar with a Simulation JMenu that has two JMenuItem options, "Add Issue" and "Add Request". These options are
 * then added to the menu bar. The method also sets an action command and action listener for each JMenuItem.
 */

public class ElevatorView extends MessageReceiver implements ActionListener {
    private JPanel panel;
    private JFrame frame;
    private JMenuBar menuBar;
    private JPanel floorPanel;
    private JTextArea console;
    private byte[] receivedMessage;
    private ArrayList<Floor> floors;
    private ArrayList<Elevator> elevators;

    public ElevatorView() throws IOException, InterruptedException {
        super(Building.ELEVATOR_VIEW_PORT_NUM);
        frame = new JFrame("Elevator System");
        panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        createSimulationTimeDisplay();
        createElevatorDisplay();
        createLightDisplay();
        createMessageDisplay();
        createMenu();

        // Configure frame
        frame.add(panel);
        frame.setJMenuBar(menuBar);
        frame.setSize(1500, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        while (true) {
            if (!receivedMessagesIsEmpty()) {
                receivedMessage = getMessage();
                update();
            }
        }
    }

    /**
     * Creates a JPanel for the time taken to complete all the elevator requests
     */
    public void createSimulationTimeDisplay() {
        Font font = new Font("Roboto", Font.PLAIN, 40);
        JPanel timePanel = new JPanel();
        timePanel.setName("Simulation time");
        timePanel.setBackground(Color.LIGHT_GRAY);
        timePanel.add(new JLabel("Total simulation time(s): 0"));
        timePanel.setFont(font);
        panel.add(timePanel);
    }

    /**
     * Creates a JMenuBar with a Simulation JMenu that has two JMenuItem options, "Add Issue" and "Add Request".
     * These options are then added to the menu bar. The method also sets an action command and action listener for each JMenuItem.
     */
    public void createMenu() {
        // Create and configure MenuBar
        menuBar = new JMenuBar();
        JMenu simulation = new JMenu("Simulation");
        JMenuItem addIssue = new JMenuItem("Add Issue");
        JMenuItem addRequest = new JMenuItem("Add Request");
        simulation.add(addIssue);
        simulation.add(addRequest);
        menuBar.add(simulation);

        // Configure ActionListener
        addIssue.setActionCommand("addIssue");
        addIssue.addActionListener(this);
        addRequest.setActionCommand("addRequest");
        addRequest.addActionListener(this);
    }

    /**
     *  Creates a JPanel for each elevator in the building and displays the current floor and elevator state.
     */
    public void createElevatorDisplay() {
        elevators = new ArrayList<>();
        for (int i = 0; i < Building.TOTAL_ELEVATOR; i++) {
            elevators.add(new Elevator(i));
        }

        Font font = new Font("Roboto", Font.PLAIN, 16);
        for (Elevator elevator : elevators) {
            JPanel elevatorPanel = new JPanel();
            elevatorPanel.setName("Elevator " + elevator.getElevatorNumber());
            elevatorPanel.setLayout(new GridLayout(4, 1));
            elevatorPanel.setBackground(Color.LIGHT_GRAY);
            elevatorPanel.setOpaque(true);
            elevatorPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            elevatorPanel.add(new JLabel("Elevator " + elevator.getElevatorNumber(), SwingConstants.CENTER));
            elevatorPanel.add(new JLabel("Current Floor: " + elevator.getCurrentFloor(), SwingConstants.CENTER));
            elevatorPanel.add(new JLabel("Average Time (s): " + elevator.getAverageTravelTime(), SwingConstants.CENTER));
            elevatorPanel.add(new JLabel("Elevator State: " + elevator.getElevatorState().toString(), SwingConstants.CENTER));
            for (int i = 0; i < 4; i++) {
                JLabel label = (JLabel) elevatorPanel.getComponent(i);
                label.setFont(font);
            }
            panel.add(elevatorPanel);
        }
    }

    /**
     * Creates a JPanel with a grid layout that displays the lamp state for each elevator and floor in the building.
     * The method creates JLabels for each floor and elevator lamp and adds them to the panel.
     */
    public void createLightDisplay() {
        floorPanel = new JPanel();
        floorPanel.setLayout(new GridLayout(Building.TOTAL_FLOOR + 1, Building.TOTAL_FLOOR + 1));
        floorPanel.add(new JLabel("Floor", SwingConstants.CENTER));
        for (int i = 0; i < Building.TOTAL_ELEVATOR; i++) {
            floorPanel.add(new JLabel("Elevator " + i + " Lamp", SwingConstants.CENTER));
        }
        for (int i = 0; i < Building.TOTAL_FLOOR; i++) {
            floorPanel.add(new JLabel(String.valueOf(i), SwingConstants.CENTER));

            for (int j = 0; j < Building.TOTAL_ELEVATOR; j++) {
                JLabel label = new JLabel(Lamp.LightState.OFF.toString(), SwingConstants.CENTER);
                label.setBackground(Color.WHITE);
                floorPanel.add(label);
            }
        }
        panel.add(floorPanel);
    }

    /**
     * Creates a JTextArea for displaying console messages and redirects the System.out stream to this JTextArea.
     * The JTextArea is then added to the panel.
     */
    public void createMessageDisplay() {
        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(console);

        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) throws IOException {
                console.append(String.valueOf((char) b));
            }
        }));
        panel.add(scrollPane);
    }

    /**
     * Creates a JOptionPane that allows the user to select an elevator and the type of issue to simulate.
     * @throws IOException
     */
    public void simulateIssue() throws IOException {
        // create an array of elevator numbers
        Integer[] elevatorNumbers = new Integer[Building.TOTAL_ELEVATOR];
        for(int i=0; i< Building.TOTAL_ELEVATOR; i++) {
            elevatorNumbers[i] = i;
        }

        // create a dropdown for elevator number selection
        JComboBox<Integer> elevatorNumDropDown = new JComboBox<>(elevatorNumbers);

        // create a dropdown for internal/external option selection
        JComboBox<String> issueTypeDropDown = new JComboBox<>(new String[] {"Door issue"});

        // create a panel to hold the dropdowns
        JPanel panel = new JPanel();
        panel.add(new JLabel("Elevator Number:"));
        panel.add(elevatorNumDropDown);
        panel.add(new JLabel("Type of Issue:"));
        panel.add(issueTypeDropDown);

        // display the message box and wait for user input
        int result = JOptionPane.showConfirmDialog(null, panel, "Elevator Selection", JOptionPane.OK_CANCEL_OPTION);

        // check if the user clicked OK
        if (result == JOptionPane.OK_OPTION) {
            // get the selected values from the dropdowns
            int elevatorNum = (int) elevatorNumDropDown.getSelectedItem();
            String issueType = (String) issueTypeDropDown.getSelectedItem();

            if(issueType.equals("Door issue")) {
                Issue issue = new Issue(Building.DOOR_ISSUE, elevatorNum, LocalTime.now());
                sendIssue(issue);
            }
        }
    }

    public void callElevator() throws IOException {
        // create an array of elevator numbers
        Integer[] elevatorNumbers = new Integer[Building.TOTAL_ELEVATOR];
        for(int i=0; i< Building.TOTAL_ELEVATOR; i++) {
            elevatorNumbers[i] = i;
        }

        // create an array of destination floors
        Integer[] floorNumber = new Integer[Building.TOTAL_FLOOR];
        for(int i=0; i< Building.TOTAL_FLOOR; i++) {
            floorNumber[i] = i;
        }

        // create a dropdown for elevator number selection
        JComboBox<Integer> elevatorNumDropDown = new JComboBox<>(elevatorNumbers);

        // create a dropdown for internal/external option selection
        JComboBox<String> internalExternalDropDown = new JComboBox<>(new String[] {"Internal", "External"});

        // create a dropdown for destination floor selection
        JComboBox<Integer> floorDropDown = new JComboBox<>(floorNumber);

        // create a panel to hold the dropdowns
        JPanel panel = new JPanel();
        panel.add(new JLabel("Elevator Number:"));
        panel.add(elevatorNumDropDown);
        panel.add(new JLabel("Destination Floor:"));
        panel.add(floorDropDown);
        panel.add(new JLabel("Internal/External call:"));
        panel.add(internalExternalDropDown);

        // display the message box and wait for user input
        int result = JOptionPane.showConfirmDialog(null, panel, "Elevator Selection", JOptionPane.OK_CANCEL_OPTION);

        // check if the user clicked OK
        if (result == JOptionPane.OK_OPTION) {
            // get the selected values from the dropdowns
            int floor = (int) floorDropDown.getSelectedItem();
            int elevatorNum = (int) elevatorNumDropDown.getSelectedItem();
            String internalExternal = (String) internalExternalDropDown.getSelectedItem();

            Request request = new Request(floor, elevatorNum, "UP", LocalTime.now(), internalExternal.toUpperCase());
            sendRequest(request);
        }
    }

    public void sendRequest(Request request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(request);
        out.flush();
        byte[] msg = bos.toByteArray();
        bos.close();

        DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
        System.out.println(LocalTime.now() + ": Sending request packet to Scheduler");
        DatagramSocket sendSocket = new DatagramSocket();
        sendSocket.send(sendPacket);
        System.out.println(LocalTime.now() + ": Request packet sent.");
    }

    public void sendIssue(Issue issue) throws IOException {
        byte[] msg = new byte[0];
        if(issue.getIssue() == Building.DOOR_ISSUE) {
            msg = new byte[]{Building.DOOR_ISSUE, (byte) issue.getElevatorNumber()};
        }

        DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Building.SCHEDULER_PORT_NUM);
        System.out.println(LocalTime.now() + ": Sending issue packet to Scheduler");
        DatagramSocket sendSocket = new DatagramSocket();
        sendSocket.send(sendPacket);
        System.out.println(LocalTime.now() + ": Issue packet sent.");
    }

    public void update() {
        if(receivedMessage[0] == Building.DOOR_ISSUE) {
            updateElevatorState(receivedMessage[1], "OUT_OF_SERVICE, DOOR_ISSUE");
            clearLight(receivedMessage[1]);
        }
        if(receivedMessage[0] == Building.ELEVATOR_STUCK) {
            updateElevatorState(receivedMessage[1], "OUT_OF_SERVICE, ELEVATOR_STUCK");
            clearLight(receivedMessage[1]);
        }
        if(receivedMessage[0] == Building.ISSUE_FIXED) {
            updateElevatorState(receivedMessage[1], "IN_SERVICE");
            clearLight(receivedMessage[1]);
        }
        if(receivedMessage[0] == Building.AVERAGE_TRAVEL_TIME) {
            updateTime(receivedMessage[1], receivedMessage[2]);
        }
        if(receivedMessage[0] == Building.REQUEST_ELEVATOR) {
            updateLight(receivedMessage[1], receivedMessage[2], Lamp.LightState.ON.toString());
        }
        if(receivedMessage[0] == Building.ELEVATOR_ARRIVAL) {
            updateElevatorFloor(receivedMessage[1], receivedMessage[2]);
            updateLight(receivedMessage[1], receivedMessage[2], Lamp.LightState.OFF.toString());
        }
        if(receivedMessage[0] == Building.TOTAL_SIMULATION_TIME) {
            updateSimulationTime(receivedMessage[1]);
        }
    }

    public void clearLight(int elevatorIndex) {
        for(int i=0; i<Building.TOTAL_FLOOR; i++) {
            updateLight(elevatorIndex, i, "CLEAR");
        }
    }

    public void updateSimulationTime(int newSimulationTime) {
        Component[] components = panel.getComponents(); // Get all components in the panel
        for (Component component : components) {
            if (component instanceof JPanel && component.getName().equals("Simulation time")) {
                JPanel timePanel = (JPanel) component;
                JLabel timeLabel = (JLabel) timePanel.getComponent(0); // Get the first component, which should be the JLabel
                timeLabel.setText("Total simulation time(s): " + newSimulationTime); // Update the JLabel text
                break;
            }
        }
    }

    public void updateTime(int elevatorIndex, double time) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component.getName() != null && component.getName().equals("Elevator " + elevatorIndex)) {
                JPanel elevatorPanel = (JPanel) component;
                ((JLabel) elevatorPanel.getComponent(2)).setText("Average Time (s): " + time);
            }
        }
    }

    public void updateElevatorFloor(int elevatorIndex,int newFloor) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component.getName() != null && component.getName().equals("Elevator " + elevatorIndex)) {
                JPanel elevatorPanel = (JPanel) component;
                ((JLabel) elevatorPanel.getComponent(1)).setText("Current Floor: " + newFloor);
            }
        }
        System.out.println(LocalTime.now() + ": Elevator " + elevatorIndex + " arrived at floor " + newFloor);
    }

    public void updateElevatorState(int elevatorIndex, String newState) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component.getName() != null && component.getName().equals("Elevator " + elevatorIndex)) {
                JPanel elevatorPanel = (JPanel) component;
                ((JLabel) elevatorPanel.getComponent(3)).setText("Elevator State: " + newState);
            }
        }
        System.out.println(LocalTime.now() + ": Elevator " + elevatorIndex + " is " + newState);
    }

    public void updateLight(int elevatorIndex,int floorIndex, String newLightState) {
        int column = elevatorIndex + 1;
        int row = (floorIndex+1) * (Building.TOTAL_ELEVATOR + 1) + column;

        Component component = floorPanel.getComponent(row);
        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            if(newLightState.equals("ON")) {
                label.setBackground(Color.GREEN);
                label.setText(newLightState);
            }
            else if(newLightState.equals("OFF")) {
                label.setBackground(Color.RED);
                label.setText(newLightState);
            }
            else if(newLightState.equals("CLEAR")) {
                label.setBackground(null);
                label.setText("OFF");
            }
            label.setOpaque(true);
        }
        System.out.println(LocalTime.now() + ": Floor light turned " + newLightState + " for elevator " + elevatorIndex + " at floor " + floorIndex);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("addRequest")) {
            try {
                callElevator();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        if(e.getActionCommand().equals("addIssue")) {
            try {
                simulateIssue();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new ElevatorView();
    }
}
