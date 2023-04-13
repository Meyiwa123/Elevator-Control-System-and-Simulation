import java.time.LocalTime;

public class Issue {
    private int issue;
    private int elevatorNumber;
    private LocalTime requestTime;

    public Issue(int issue, int elevatorNumber, LocalTime requestTime) {
        this.issue = issue;
        this.elevatorNumber = elevatorNumber;
        this.requestTime = requestTime;
    }

    /**
     * Return the issue to be simulated
     * @return int type of issue
     */
    public int getIssue() {
        return issue;
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
     * Return the elevator the issue is to be simulated
     * @return int elevator number
     */
    public int getElevatorNumber() {
        return elevatorNumber;
    }

    /**
     * Returns a description of the issue made.
     * @return String issue to string
     */
    public String toString() {
        if(issue == Building.DOOR_ISSUE) {
            return "Issue: Door Issue, Elevator Number: " + elevatorNumber + ", Time of issue: " + requestTime;
        }
        return "Elevator Number: " + elevatorNumber + ", Time of issue: " + requestTime;
    }
}
