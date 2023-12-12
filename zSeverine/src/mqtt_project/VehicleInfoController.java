package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

public class VehicleInfoController implements Observer, Runnable {
    private final VehicleInfoModel vehicleInfoModel;
    private final View view;
    private final MessageListener batteryStatusListener;
    private final MessageListener speedEventListener;
    private final MessageListener trackEventListener;
    private final MessageListener wheelDistanceEventListener;
    private final JSONParser parser;

    public VehicleInfoController(MqttHandler mqttHandler, VehicleInfoModel vehicleInfoModel, View view) {
        Hashtable<String, String> topicPathByName = mqttHandler.topicPathByName;
        this.vehicleInfoModel = vehicleInfoModel;
        this.vehicleInfoModel.addObserver(this);
        this.view = view;

        // Subscribe to battery status of the vehicle
        String batteryStatusTopic = topicPathByName.get("singleVehicleBatteryStatus");
        this.batteryStatusListener = new MessageListener(batteryStatusTopic);
        mqttHandler.subscribe(batteryStatusTopic, this.batteryStatusListener);

        // Subscribe to track event of the vehicle
        String trackEventTopic = topicPathByName.get("singleVehicleTrackEvent");
        this.trackEventListener = new MessageListener(trackEventTopic);
        mqttHandler.subscribe(trackEventTopic, this.trackEventListener);

        // Subscribe to speed event of the vehicle
        String speedEventTopic = topicPathByName.get("singleVehicleSpeedEvent");
        this.speedEventListener = new MessageListener(speedEventTopic);
        mqttHandler.subscribe(speedEventTopic, this.speedEventListener);

        // Subscribe to wheelDistance event of the vehicle
        String wheelDistanceEventTopic = topicPathByName.get("singleVehicleWheelDistanceEvent");
        this.wheelDistanceEventListener = new MessageListener(wheelDistanceEventTopic);
        mqttHandler.subscribe(wheelDistanceEventTopic, this.wheelDistanceEventListener);

        this.parser = new JSONParser();
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof VehicleInfoModel) {
            switch ((int) arg){
                case VehicleInfoModel.SPEED_UPDATE:
                    view.updateMeasuredSpeedLabel(this.vehicleInfoModel.getMeasuredSpeed());
                    break;
                case VehicleInfoModel.TRACK_ID_UPDATE:
                    view.updateTrackIdLabel(this.vehicleInfoModel.getCurrentTrackId());
                    break;
                case VehicleInfoModel.TURNING_STATUS_UPDATE:
                    view.updateTurningStatusLabel(this.vehicleInfoModel.getTurningStatus());
                    break;
                case VehicleInfoModel.BATTERY_LEVEL_UPDATE:
                    view.updateBatteryLevelLabel(this.vehicleInfoModel.getBatteryLevel());
                    break;
            }
        }
    }

    @Override
    public void run() {
        while(true) {
            measureBatteryLevel();
            measureSpeed();
            measureTrackId();
            measureWheelDistance();
        }
    }

    private void measureSpeed(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.speedEventListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            this.vehicleInfoModel.setMeasuredSpeed(((Long) jsonObj.get("value")).intValue());
        } catch (ParseException | InterruptedException e) {
            System.out.println(e.getMessage());
            e.getStackTrace();
        }
    }

    private void measureTrackId(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.trackEventListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            this.vehicleInfoModel.setCurrentTrackId(((Long) jsonObj.get("trackID")).intValue());
        } catch (ParseException | InterruptedException e) {
            System.out.println(e.getMessage());
            e.getStackTrace();
        }
    }

    private void measureWheelDistance() {
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.wheelDistanceEventListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            int leftWheelDistance = ((Long) jsonObj.get("left")).intValue();
            int rightWheelDistance = ((Long) jsonObj.get("right")).intValue();
            this.vehicleInfoModel.estimateIfTurning(leftWheelDistance, rightWheelDistance);
        } catch (ParseException | InterruptedException e) {
            System.out.println(e.getMessage());
            e.getStackTrace();
        }
    }

    public void measureBatteryLevel(){
        MqttMessage receivedMsg;
        try {
            receivedMsg = this.batteryStatusListener.getMessageQueue().take();
            JSONObject jsonObj = (JSONObject) this.parser.parse(new String(receivedMsg.getPayload()));
            int batteryLevel = ((Long) jsonObj.get("value")).intValue();
            this.vehicleInfoModel.setBatteryLevel(batteryLevel);
            view.updateBatteryLevelLabel(batteryLevel);
        } catch (ParseException | InterruptedException e) {
            System.out.println(e.getMessage());
            e.getStackTrace();
        }
    }
}