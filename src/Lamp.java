/**
 * Lamp class represents a set of lamps, which can be on or off
 */

public class Lamp {
    public LightState[] lightState;
    public enum LightState {ON, OFF};

    public Lamp(int num) {
        lightState = new LightState[num];
        for(int i=0; i<num; i++) {
            lightState[i] = LightState.OFF;
        }
    }

    /**
     * Updates the corresponding element of the lightState array with the new value
     *
     * @param floor int index of lamp
     * @param newLightState LightState new state of lamp
     */
    public void setLightState(int floor, LightState newLightState) {
        lightState[floor] = newLightState;
    }
}