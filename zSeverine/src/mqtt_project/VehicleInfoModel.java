package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Hashtable;
import java.util.Observable;

public class VehicleInfoModel extends Observable implements Runnable {
    private final MqttHandler mqttHandler;

    // Battery level related variables
    private final MessageListener batteryStatusListener;
    protected int batteryLevel;

    // Speed related variables
    private final MessageListener speedEventListener;
    protected int measuredSpeed;

    // trackId related variables
    private final MessageListener trackEventListener;
    protected int currentTrackId;

    // turn related variables
    private final MessageListener wheelDistanceEventListener;
    private Boolean isTurning;

    private JSONParser parser;

    public VehicleInfoModel(MqttHandler mqttHandler){
        this.mqttHandler = mqttHandler;
        Hashtable<String, String> topicPathByName = mqttHandler.topicPathByName;

        // Subscribe to battery status of the vehicle
        String batteryStatusTopic = topicPathByName.get("singleVehicleBatteryStatus");
        this.batteryStatusListener = new MessageListener(batteryStatusTopic);
        this.mqttHandler.subscribe(batteryStatusTopic, this.batteryStatusListener);

        // Subscribe to track event of the vehicle
        String trackEventTopic = topicPathByName.get("singleVehicleTrackEvent");
        this.trackEventListener = new MessageListener(trackEventTopic);
        this.mqttHandler.subscribe(trackEventTopic, this.trackEventListener);

        // Subscribe to speed event of the vehicle
        String speedEventTopic = topicPathByName.get("singleVehicleSpeedEvent");
        this.speedEventListener = new MessageListener(speedEventTopic);
        this.mqttHandler.subscribe(speedEventTopic, this.speedEventListener);

        // Subscribe to wheelDistance event of the vehicle
        String wheelDistanceEventTopic = topicPathByName.get("singleVehicleWheelDistanceEvent");
        this.wheelDistanceEventListener = new MessageListener(wheelDistanceEventTopic);
        this.mqttHandler.subscribe(wheelDistanceEventTopic, this.wheelDistanceEventListener);

        this.parser = new JSONParser();
    }

    @Override
    public void run() {
        while(true) {
            measureBatteryLevel();
            measureSpeed();
            measureTrackId();
            estimateIfTurning();
        }
    }
    public void measureBatteryLevel(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.batteryStatusListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            System.out.println(jsonObj.get("value"));
            //this.batteryLevel = jsonObj.get("value");

        } catch (ParseException | InterruptedException e) {
            e.getMessage();
            e.getStackTrace();
        }
    }

    public void setBatteryLevel(int batteryLevel){
        this.batteryLevel = batteryLevel;
    }

    public int getBatteryLevel(){
        return this.batteryLevel;
    }

    private void measureSpeed(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.speedEventListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            this.measuredSpeed = (int) jsonObj.get("value");
        } catch (ParseException | InterruptedException e) {
            e.getMessage();
            e.getStackTrace();
        }
    }

    public void setMeasuredSpeed(int measuredSpeed){
        this.measuredSpeed = measuredSpeed;
    }

    public int getMeasuredSpeed(){
        return this.measuredSpeed;
    }

    private void measureTrackId(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.trackEventListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            this.currentTrackId = (int) jsonObj.get("trackID");
        } catch (ParseException | InterruptedException e) {
            e.getMessage();
            e.getStackTrace();
        }
    }

    public void setCurrentTrackId(int currentTrackId){
        this.currentTrackId = currentTrackId;
    }

    public int getCurrentTrackId(){
        return this.currentTrackId;
    }

    public void estimateIfTurning(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.wheelDistanceEventListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            int leftWheelDistance = (int) jsonObj.get("left");
            int rightWheelDistance = (int) jsonObj.get("right");
            this.isTurning = leftWheelDistance < rightWheelDistance - 10 | rightWheelDistance < leftWheelDistance - 10;
        } catch (ParseException | InterruptedException e) {
            e.getMessage();
            e.getStackTrace();
        }
    }

    public boolean getTurningStatus(){
        return this.isTurning;
    }

}
