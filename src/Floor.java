/**
 * Floor class represents a floor in a building. The constructor creates
 * a Lamp object with a number of lamps equal to the Building.TOTAL_ELEVATOR constant
 */

public class Floor {
    Lamp lamp;
    private int floorNumber;

    public Floor(int floorNumber) {
        lamp = new Lamp(Building.TOTAL_ELEVATOR);
        this.floorNumber = floorNumber;
    }
}
