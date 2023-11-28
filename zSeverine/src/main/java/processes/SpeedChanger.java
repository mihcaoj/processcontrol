package processes;

import messages.MessageHandler;
import setup.MqttHandler;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Hashtable;

public class SpeedChanger implements Runnable {
    private final MqttHandler handler;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private Hashtable<String, String> topicPathByName;

    public SpeedChanger(MqttHandler handler){
        this.handler = handler;
        this.topicPathByName = handler.getTopicPathByName();
    }

    @Override
    public void run() {
        changeSpeed();
    }

    private void changeSpeed(){
        try {
            String vehicleIntentTopic = topicPathByName.get("singleVehicleIntent");
            boolean increasingSpeed = true;
            int speed = 0;

            String vehicleIntentSpeedChange;
            while (true) {
                if (increasingSpeed){
                    if (speed < 100){
                        speed += 1;
                    } else {
                        increasingSpeed = false;
                        speed -= 1;
                    }
                } else {
                    if (speed>-100){
                        speed -= 1;
                    } else {
                        increasingSpeed = true;
                        speed += 1;
                    }
                }
                vehicleIntentSpeedChange= MessageHandler.createIntentMsg("speed", new String[][]{{"velocity",String.valueOf(speed)}});
                handler.publish(vehicleIntentTopic, vehicleIntentSpeedChange);
                Thread.sleep(500);
            }
        } catch (MqttException | InterruptedException e){
            e.getMessage();
            e.getStackTrace();
        }
    }
}
