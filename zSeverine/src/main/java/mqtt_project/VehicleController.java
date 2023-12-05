package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttException;
import java.util.Hashtable;

public class VehicleController implements Runnable {
    private final MqttHandler mqttHandler;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private final Hashtable<String, String> topicPathByName;
    private String lightStatus;
    private int speed;
    private int laneOffset;

    public VehicleController(MqttHandler mqttHandler){
        this.mqttHandler = mqttHandler;
        this.topicPathByName = mqttHandler.topicPathByName;
        this.lightStatus = "off";
        this.speed = 0;
        this.laneOffset = 0;
    }

    @Override
    public void run() {
        blink();
    }

    private void blink(){
        try {
            String vehicleIntentTopic = topicPathByName.get("singleVehicleIntent");
            String vehicleIntentFrontOnMsg = mqtt_exercise.messages.MessageHandler.createIntentMsg("lights", new String[][]{{"front", "on"}});
            String vehicleIntentFrontOffMsg = MessageHandler.createIntentMsg("lights", new String[][]{{"front", "off"}});
            while (true) {
                mqttHandler.publish(vehicleIntentTopic, vehicleIntentFrontOnMsg);
                Thread.sleep(500);
                mqttHandler.publish(vehicleIntentTopic, vehicleIntentFrontOffMsg);
                Thread.sleep(500);
            }
        } catch (MqttException | InterruptedException e){
            e.getMessage();
            e.getStackTrace();
        }
    }

    private void changeSpeed(int speed){
        try {
            String vehicleIntentTopic = topicPathByName.get("singleVehicleIntent");
            this.speed = speed;
            String vehicleIntentSpeedChange= MessageHandler.createIntentMsg("speed", new String[][]{{"velocity",String.valueOf(speed)}});
            mqttHandler.publish(vehicleIntentTopic, vehicleIntentSpeedChange);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }

    private void changeLane(int laneOffset){
        try {
            String vehicleIntentTopic = topicPathByName.get("singleVehicleIntent");
            String vehicleIntentLaneChange = MessageHandler.createIntentMsg("lane", new String[][]{{"offset", String.valueOf(laneOffset)}});
            mqttHandler.publish(vehicleIntentTopic, vehicleIntentLaneChange);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }
}
