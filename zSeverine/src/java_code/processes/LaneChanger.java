package java_code.processes;

import lecture08.messages.MessageHandler;
import lecture08.setup.MqttHandler;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Hashtable;

public class LaneChanger implements Runnable {
    private final MqttHandler handler;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private Hashtable<String, String> topicPathByName;

    public LaneChanger(MqttHandler handler){
        this.handler = handler;
        this.topicPathByName = handler.getTopicPathByName();
    }
    @Override
    public void run() {
        changeLane();
    }

    private void changeLane(){
        try {
            String vehicleIntentTopic = topicPathByName.get("singleVehicleIntent");
            int laneOffset = 0;
            String vehicleIntentLaneChange ;
            boolean positiveDir = true;
            while (true) {
                if (positiveDir){
                    if (laneOffset < 1000){
                        laneOffset += 1;
                    } else {
                        positiveDir = false;
                        laneOffset -= 1;
                    }
                } else {
                    if (laneOffset>-1000){
                        laneOffset -= 1;
                    } else {
                        positiveDir = true;
                        laneOffset += 1;
                    }
                }
                vehicleIntentLaneChange = MessageHandler.createIntentMsg("lane", new String[][]{{"offset", String.valueOf(laneOffset)}});
                handler.publish(vehicleIntentTopic, vehicleIntentLaneChange);
                Thread.sleep(500);
            }
        } catch (MqttException | InterruptedException e){
            e.getMessage();
            e.getStackTrace();
        }
    }
}
