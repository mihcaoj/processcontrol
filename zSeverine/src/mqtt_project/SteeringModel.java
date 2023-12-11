package mqtt_project;

import java.util.Observable;

// In MVC, the Model uses "Observer" design pattern by being an observable, containing the application states and logic for state change.
// Controller and View registers as observers of the Model to be notified as soon as the Model state changes.
public class SteeringModel extends Observable {
    private boolean emergency = false;
    private String wishedFrontLightStatus;
    private String wishedBackLightStatus;
    private int wishedSpeed;
    private int wishedLaneOffset;

    public SteeringModel(){
        this.emergency=false;
        this.wishedFrontLightStatus = "off";
        this.wishedBackLightStatus = "off";
        this.wishedSpeed = 10;
        this.wishedLaneOffset = 0;
    }

    public int getWishedSpeed() {
        return wishedSpeed;
    }

    public void setWishedSpeed(int wishedSpeed) {
        this.wishedSpeed = wishedSpeed;
        setChanged();
        notifyObservers("wished speed");
    }

    public int getWishedLaneOffset() {
        return wishedLaneOffset;
    }

    public void setWishedLaneOffset(int wishedLaneOffset) {
        this.wishedLaneOffset = wishedLaneOffset;
        setChanged();
        notifyObservers("wished lane offset");
    }

    public boolean getEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
        setChanged();
        notifyObservers("emergency");
    }

    public String getWishedFrontLightStatus() {
        return wishedFrontLightStatus;
    }

    public void setWishedFrontLightStatus(String wishedFrontLightStatus) {
        this.wishedFrontLightStatus = wishedFrontLightStatus;
        setChanged();
        notifyObservers("front lights status");
    }

    public String getWishedBackLightStatus() {
        return wishedBackLightStatus;
    }

    public void setWishedBackLightStatus(String wishedBackLightStatus) {
        this.wishedBackLightStatus = wishedBackLightStatus;
        setChanged();
        notifyObservers("back lights status");
    }
}
