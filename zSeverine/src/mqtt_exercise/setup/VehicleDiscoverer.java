package mqtt_exercise.setup;

import mqtt_exercise.messages.MessageHandler;
import mqtt_exercise.messages.MessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Hashtable;

public class VehicleDiscoverer implements Runnable {
    private final MqttHandler handler;
    private JSONParser parser;
    // Dictionary(Key: TopicName, Value: TopicPath)
    private Hashtable<String, String> topicPathByName;
    private final String discoveringStatusTopic;
    private MessageListener discoveringStatusListener;
    private final String vehiclesStatusTopic;
    private MessageListener vehiclesStatusListener;

    public VehicleDiscoverer(MqttHandler handler){
        this.handler = handler;
        topicPathByName = handler.topicPathByName;

        // Subscribe to status "discovering" of the host
        discoveringStatusTopic = topicPathByName.get("HostDiscoveringStatus");
        discoveringStatusListener = new MessageListener(discoveringStatusTopic);
        this.handler.subscribe(discoveringStatusTopic, discoveringStatusListener);

        // Subscribe to status "vehicles" of the host
        vehiclesStatusTopic = topicPathByName.get("HostVehiclesStatus");
        vehiclesStatusListener = new MessageListener(vehiclesStatusTopic);
        this.handler.subscribe(vehiclesStatusTopic, vehiclesStatusListener);

        parser = new JSONParser();
    }

    public void run() {
        System.out.println("======= DISCOVER STEP =======");
        activateDiscovering();
        findVehicle(this.handler.getVehicleId());
        cleanUp();
    }

    private void activateDiscovering() {
        MqttMessage receivedMsg;
        boolean discovering = false;
        try {while (!discovering) {
                receivedMsg = discoveringStatusListener.getMessageQueue().take();
                JSONObject jsonObj = (JSONObject) parser.parse(new String(receivedMsg.getPayload()));
                if ((Boolean) jsonObj.get("value")) {
                    discovering = true;
                } else {
                    // Publish an intent to discover
                    String discoverIntentTopic = topicPathByName.get("HostIntent");
                    String discoverIntentMsg = MessageHandler.createIntentMsg("discover",new String[][]{{"value", "true"}});
                    handler.publish(discoverIntentTopic, discoverIntentMsg);
                }
            }
        } catch (ParseException | MqttException | InterruptedException e) {
            e.getMessage();
            e.getStackTrace();
        }
    }

    private void findVehicle(String vehicleId) {
        MqttMessage receivedMsg;
        boolean found = false;
        try {
            while (!found) {
                receivedMsg = vehiclesStatusListener.getMessageQueue().take();
                JSONObject jsonObj = (JSONObject) parser.parse(new String(receivedMsg.getPayload()));
                JSONArray vehicles = (JSONArray) jsonObj.get("value");
                System.out.print("INFO: Discovered vehicles: " + vehicles+"\n");
                for (int i = 0; i < vehicles.size(); i++) {
                    String foundVehicleId = (String) vehicles.get(i);
                    if (foundVehicleId.equals(vehicleId)) {
                        found = true;
                        break; // No need to continue searching once found
                    }
                }
            }
        } catch (ParseException | InterruptedException e) {
            e.getMessage();
            e.getStackTrace();
        }
    }

    private void cleanUp() {
        try {
            this.handler.unsubscribe(discoveringStatusTopic);
            this.handler.unsubscribe(vehiclesStatusTopic);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }

}
