package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Hashtable;
import java.util.Objects;

public class SetupVehicleManager implements Runnable{
    private final MqttHandler mqttHandler;
    private JSONParser parser;

    // Dictionary(Key: TopicName, Value: TopicPath)
    private final Hashtable<String, String> topicPathByName;
    private final String discoveringStatusTopic;
    private final MessageListener discoveringStatusListener;
    private final String vehiclesStatusTopic;
    private final MessageListener vehiclesStatusListener;
    private final String vehicleStatusTopic;
    private final MessageListener vehicleStatusListener;

    public SetupVehicleManager(MqttHandler mqttHandler) {
        this.mqttHandler = mqttHandler;
        topicPathByName = mqttHandler.topicPathByName;

        // Subscribe to status "discovering" of the host
        discoveringStatusTopic = topicPathByName.get("HostDiscoveringStatus");
        discoveringStatusListener = new MessageListener(discoveringStatusTopic);
        this.mqttHandler.subscribe(discoveringStatusTopic, discoveringStatusListener);

        // Subscribe to status "vehicles" of the host
        vehiclesStatusTopic = topicPathByName.get("HostVehiclesStatus");
        vehiclesStatusListener = new MessageListener(vehiclesStatusTopic);
        this.mqttHandler.subscribe(vehiclesStatusTopic, vehiclesStatusListener);

        // Subscribe to status of the vehicle
        this.vehicleStatusTopic = topicPathByName.get("singleVehicleStatus");
        this.vehicleStatusListener = new MessageListener(vehicleStatusTopic);
        this.mqttHandler.subscribe(vehicleStatusTopic, vehicleStatusListener);

        this.parser = new JSONParser();
    }

    public void run() {
        System.out.println("======= DISCOVER STEP =======");
        activateDiscovering();
        findVehicle(this.mqttHandler.getVehicleId());
        System.out.println("======= CONNECTION STEP =======");
        connectVehicle();
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
                mqttHandler.publish(discoverIntentTopic, discoverIntentMsg);
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

    private void connectVehicle(){
        try {
            // Publish an intent to connect the vehicle
            String connectIntentTopic = topicPathByName.get("singleVehicleIntent");
            String connectIntentMsg = MessageHandler.createIntentMsg("connect", new String[][]{{"value", "true"}});
            mqttHandler.publish(connectIntentTopic, connectIntentMsg);

            MqttMessage receivedMsg;
            String connectionStatus;
            while (true) {
                // Verify the vehicle is connected
                receivedMsg = this.vehicleStatusListener.getMessageQueue().take();
                JSONObject jsonObj = (JSONObject) parser.parse(new String(receivedMsg.getPayload()));
                connectionStatus = (String) jsonObj.get("value");
                System.out.printf("Status of vehicle %s: %s \n",this.mqttHandler.getVehicleId(), connectionStatus);
                if (Objects.equals(connectionStatus, "ready")){
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
            this.mqttHandler.unsubscribe(discoveringStatusTopic);
            this.mqttHandler.unsubscribe(vehiclesStatusTopic);
            this.mqttHandler.unsubscribe(vehicleStatusTopic);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }
}
