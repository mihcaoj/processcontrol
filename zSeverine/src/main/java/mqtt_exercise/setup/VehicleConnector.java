package mqtt_exercise.setup;

import mqtt_exercise.messages.MessageHandler;
import mqtt_exercise.messages.MessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Hashtable;
import java.util.Objects;

public class VehicleConnector implements Runnable {
    private final MqttHandler handler;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private Hashtable<String, String> topicPathByName;
    private JSONParser parser;
    private final String vehicleStatusTopic;
    private MessageListener vehicleStatusListener;
    private String connectionStatus;

    public VehicleConnector(MqttHandler handler){
        this.parser = new JSONParser();
        this.handler = handler;
        this.topicPathByName = handler.topicPathByName;

        // Subscribe to status "discovering" of the host
        this.vehicleStatusTopic = topicPathByName.get("singleVehicleStatus");
        this.vehicleStatusListener = new MessageListener(vehicleStatusTopic);
        this.handler.subscribe(vehicleStatusTopic, vehicleStatusListener);
    }
    @Override
    public void run() {
        System.out.println("======= CONNECTION STEP =======");
        connectVehicle();
        cleanUp();
    }

    private void connectVehicle(){
        try {
            // Publish an intent to connect the vehicle
            String connectIntentTopic = topicPathByName.get("singleVehicleIntent");
            String connectIntentMsg = MessageHandler.createIntentMsg("connect", new String[][]{{"value", "true"}});
            handler.publish(connectIntentTopic, connectIntentMsg);

            MqttMessage receivedMsg;
            while (true) {
                // Verify the vehicle is connected
                receivedMsg = this.vehicleStatusListener.getMessageQueue().take();
                JSONObject jsonObj = (JSONObject) parser.parse(new String(receivedMsg.getPayload()));
                this.connectionStatus = (String) jsonObj.get("value");
                System.out.printf("Status of vehicle %s: %s \n",this.handler.getVehicleId(), this.connectionStatus);
                if (Objects.equals(this.connectionStatus, "ready")){
                    return;
                }
            }
        } catch (MqttException | InterruptedException | ParseException e){
            e.getMessage();
            e.getStackTrace();
        }
    }

    private void cleanUp() {
        try {
            this.handler.unsubscribe(vehicleStatusTopic);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }
}
