package mqtt_project;

import java.util.Observable;

/*In MVC, the Model uses "Observer" design pattern by being an observable, containing the application states and logic for state change.
 Controller and View registers as observers of the Model to be notified as soon as the Model state changes.
 This Model stores all variables to steer the vehicle: the wished speed, lane offset and lights as well as emergency flag */
public class SteeringModel extends Observable {
    public static final int WISHED_SPEED_UPDATE = 1;
    public static final int WISHED_LANE_OFFSET = 2;
    public static final int EMERGENCY_UPDATE = 3;
    public static final int FRONT_LIGHTS_UPDATE = 4;
    public static final int BACK_LIGHTS_UPDATE = 5;
    private boolean emergency = false;
    private String wishedFrontLightStatus;
    private String wishedBackLightStatus;
    private int wishedSpeed;
    private int wishedLaneOffset;

    public SteeringModel(){
        this.emergency=false;
        this.wishedFrontLightStatus = "off";
        this.wishedBackLightStatus = "off";
        this.wishedSpeed = 0;
        this.wishedLaneOffset = 0;
    }

    public int getWishedSpeed() {
        return wishedSpeed;
    }

    public void setWishedSpeed(int wishedSpeed) {
        this.wishedSpeed = wishedSpeed;
        setChanged();
        notifyObservers(SteeringModel.WISHED_SPEED_UPDATE);
    }

    public int getWishedLaneOffset() {
        return wishedLaneOffset;
    }

    public void setWishedLaneOffset(int wishedLaneOffset) {
        this.wishedLaneOffset = wishedLaneOffset;
        setChanged();
        notifyObservers(SteeringModel.WISHED_LANE_OFFSET);
    }

    public boolean getEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
        setChanged();
        notifyObservers(SteeringModel.EMERGENCY_UPDATE);
    }

    public String getWishedFrontLightStatus() {
        return wishedFrontLightStatus;
    }

    public void setWishedFrontLightStatus(String wishedFrontLightStatus) {
        this.wishedFrontLightStatus = wishedFrontLightStatus;
        setChanged();
        notifyObservers(SteeringModel.FRONT_LIGHTS_UPDATE);
    }

    public String getWishedBackLightStatus() {
        return wishedBackLightStatus;
    }

    public void setWishedBackLightStatus(String wishedBackLightStatus) {
        this.wishedBackLightStatus = wishedBackLightStatus;
        setChanged();
        notifyObservers(SteeringModel.BACK_LIGHTS_UPDATE);
    }


}
