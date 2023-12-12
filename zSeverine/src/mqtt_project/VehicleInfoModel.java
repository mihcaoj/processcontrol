package mqtt_project;

import java.util.Observable;

public class VehicleInfoModel extends Observable {
    public static final int SPEED_UPDATE = 1;
    public static final int TRACK_ID_UPDATE = 2;
    public static final int TURNING_STATUS_UPDATE = 3;
    public static final int BATTERY_LEVEL_UPDATE = 4;
    protected int measuredSpeed=0;
    private int currentTrackId=0;
    private Boolean turningStatus=false;
    private int batteryLevel=100;
    private boolean lowBatteryStatus=false;

    public VehicleInfoModel(){
    }

    public void setMeasuredSpeed(int measuredSpeed){
        this.measuredSpeed = measuredSpeed;
        setChanged();
        notifyObservers(VehicleInfoModel.SPEED_UPDATE);
    }

    public int getMeasuredSpeed(){
        return this.measuredSpeed;
    }

    public void setCurrentTrackId(int currentTrackId){
        this.currentTrackId = currentTrackId;
        setChanged();
        notifyObservers(VehicleInfoModel.TRACK_ID_UPDATE);
    }

    public int getCurrentTrackId(){
        return this.currentTrackId;
    }

    public void estimateIfTurning(int leftWheelDistance, int rightWheelDistance){
        this.turningStatus = leftWheelDistance < rightWheelDistance - 10 | rightWheelDistance < leftWheelDistance - 10;
        setChanged();
        notifyObservers(VehicleInfoModel.TURNING_STATUS_UPDATE);
    }

    public boolean getTurningStatus(){
        return this.turningStatus;
    }

    public void setBatteryLevel(int batteryLevel){
        this.batteryLevel = batteryLevel;
        this.lowBatteryStatus = this.batteryLevel < 70;
        setChanged();
        notifyObservers(VehicleInfoModel.BATTERY_LEVEL_UPDATE);
    }

    public int getBatteryLevel(){
        return this.batteryLevel;
    }

    public boolean getLowBatteryStatus(){
        return this.lowBatteryStatus;
    }
}
