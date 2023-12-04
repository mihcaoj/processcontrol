package mqtt_exercise.processes;

import mqtt_exercise.messages.MessageHandler;
import mqtt_exercise.setup.MqttHandler;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Hashtable;

public class FrontBlinker implements Runnable {
    private final MqttHandler handler;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private Hashtable<String, String> topicPathByName;

    public FrontBlinker(MqttHandler handler){
        this.handler = handler;
        this.topicPathByName = handler.topicPathByName;
    }

    @Override
    public void run() {
        blink();
    }

    private void blink(){
        try {
            String vehicleIntentTopic = topicPathByName.get("singleVehicleIntent");
            String vehicleIntentFrontOnMsg = MessageHandler.createIntentMsg("lights", new String[][]{{"front", "on"}});
            String vehicleIntentFrontOffMsg = MessageHandler.createIntentMsg("lights", new String[][]{{"front", "off"}});
            while (true) {
                handler.publish(vehicleIntentTopic, vehicleIntentFrontOnMsg);
                Thread.sleep(500);
                handler.publish(vehicleIntentTopic, vehicleIntentFrontOffMsg);
                Thread.sleep(500);
            }
        } catch (MqttException | InterruptedException e){
            e.getMessage();
            e.getStackTrace();
        }
    }


}
