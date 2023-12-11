package mqtt_project;

import java.util.Observable;

public class VehicleInfoModel extends Observable {
    protected int measuredSpeed;
    private int currentTrackId;
    private Boolean turningStatus;
    private int batteryLevel;
    private boolean lowBatteryStatus;

    public VehicleInfoModel(){
    }

    public void setMeasuredSpeed(int measuredSpeed){
        this.measuredSpeed = measuredSpeed;
        setChanged();
        notifyObservers("measured speed");
    }

    public int getMeasuredSpeed(){
        return this.measuredSpeed;
    }

    public void setCurrentTrackId(int currentTrackId){
        this.currentTrackId = currentTrackId;
        setChanged();
        notifyObservers("track id");
    }

    public int getCurrentTrackId(){
        return this.currentTrackId;
    }

    public void estimateIfTurning(int leftWheelDistance, int rightWheelDistance){
        this.turningStatus = leftWheelDistance < rightWheelDistance - 10 | rightWheelDistance < leftWheelDistance - 10;
        setChanged();
        notifyObservers("turning status");
    }

    public boolean getTurningStatus(){
        return this.turningStatus;
    }

    public void setBatteryLevel(int batteryLevel){
        this.batteryLevel = batteryLevel;
        this.lowBatteryStatus = this.batteryLevel < 70;
        setChanged();
        notifyObservers("battery level");
    }

    public int getBatteryLevel(){
        return this.batteryLevel;
    }

    public boolean getLowBatteryStatus(){
        return this.lowBatteryStatus;
    }
}
