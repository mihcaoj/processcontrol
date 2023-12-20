package mqtt_project;

import java.util.Observable;

/*In MVC, the Model uses "Observer" design pattern by being an observable, containing the application states and logic for state change.
 Controller and View registers as observers of the Model to be notified as soon as the Model state changes.
 This Model stores information about the target vehicle, like its connection status, measured speed, trackId and batteryLevel.
 It also estimates if the vehicle is on a turning track, depending on the difference between wheel distances on the left, resp. on the right*/
public class VehicleInfoModel extends Observable {
    public static final int CONNECTION_UPDATE = 1;
    public static final int SPEED_UPDATE = 2;
    public static final int TRACK_ID_UPDATE = 3;
    public static final int TURNING_STATUS_UPDATE = 4;
    public static final int BATTERY_LEVEL_UPDATE = 5;
    protected String connectionStatus="UNKNOWN";
    protected int measuredSpeed=0;
    private int currentTrackId=0;
    private Boolean turningStatus=false;
    private int batteryLevel=100;
    private boolean lowBatteryStatus=false;

    public VehicleInfoModel(){
    }

    public void setConnectionStatus(String connectionStatus){
        this.connectionStatus = connectionStatus;
        setChanged();
        notifyObservers(VehicleInfoModel.CONNECTION_UPDATE);
    }

    public String getConnectionStatus(){
        return this.connectionStatus;
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
        this.turningStatus = leftWheelDistance < rightWheelDistance - 4 | rightWheelDistance < leftWheelDistance - 4;
        setChanged();
        notifyObservers(VehicleInfoModel.TURNING_STATUS_UPDATE);
    }

    public boolean getTurningStatus(){
        return this.turningStatus;
    }

    public void setBatteryLevel(int batteryLevel){
        this.batteryLevel = batteryLevel;
        this.lowBatteryStatus = this.batteryLevel < 75;
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
