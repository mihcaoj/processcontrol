package java_code.processes;

import lecture08.messages.MessageHandler;
import lecture08.setup.MqttHandler;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Hashtable;

public class FrontBlinker implements Runnable {
    private final MqttHandler handler;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private Hashtable<String, String> topicPathByName;

    public FrontBlinker(MqttHandler handler){
        this.handler = handler;
        this.topicPathByName = handler.getTopicPathByName();
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
