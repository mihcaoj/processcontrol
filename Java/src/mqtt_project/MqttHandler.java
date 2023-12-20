package mqtt_project;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Hashtable;

/* MQTT handler is a class responsible for managing the communication between the application and the MQTT broker.*/
public class MqttHandler {
    private final String vehicleId;
    private final MqttAsyncClient client;
    private final MessageHandler msgHandler;
    public Hashtable<String, String> topicPathByName;

    public MqttHandler(String mqttBrokerUri, String clientId, String vehicleId) throws MqttException {
        this.vehicleId = vehicleId;

        // client creation - MemoryPersistence makes the state to be in RAM, not on disk.
        this.client = new MqttAsyncClient(mqttBrokerUri, clientId, new MemoryPersistence());

        // message handling
        this.msgHandler = new MessageHandler();
        this.client.setCallback(this.msgHandler);

        // connection options (10 seconds timeout)
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        // client connection to the broker
        try{
            this.client.connect(options).waitForCompletion();
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }

        // Set up a topic dictionary for the MQTT hyperdrive API (key: TopicName, value: TopicPath)
        topicPathByName = new Hashtable<>();
        topicPathByName.put("HostIntent","Anki/Hosts/U/hyperdrive/I/"+clientId);
        topicPathByName.put("HostDiscoveringStatus","Anki/Hosts/U/hyperdrive/S/discovering");
        topicPathByName.put("HostVehiclesStatus","Anki/Hosts/U/hyperdrive/S/vehicles");
        topicPathByName.put("singleVehicleIntent","Anki/Vehicles/U/"+vehicleId+"/I/"+clientId);
        topicPathByName.put("singleVehicleStatus","Anki/Vehicles/U/"+vehicleId+"/S/status");
        topicPathByName.put("singleVehicleBatteryStatus", "Anki/Vehicles/U/"+vehicleId+"/S/battery");
        topicPathByName.put("singleVehicleSpeedEvent", "Anki/Vehicles/U/"+vehicleId+"/E/speed");
        topicPathByName.put("singleVehicleTrackEvent", "Anki/Vehicles/U/"+vehicleId+"/E/track");
        topicPathByName.put("singleVehicleWheelDistanceEvent", "Anki/Vehicles/U/"+vehicleId+"/E/wheelDistance");
    }

    // Subscribe to a specific topic (then each time a new message is published in the topic, we save it)
    public void subscribe(String topic, MessageListener msgListener){
        try {
            client.subscribe(topic, 1);  // QoS level 1
            msgHandler.addMessageListener(msgListener);
            System.out.printf("INFO: Subscribe to %s\n", topic);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }

    // Unsubscribe from a specific topic
    public void unsubscribe(String topic) throws MqttException {
        try {
            client.unsubscribe(topic);
            msgHandler.removeMessageListener(msgHandler.getMsgListenerFromTopic(topic));
            System.out.printf("INFO: Unsubscribe from %s\n", topic);
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }
    }

    // Publish a message (payload) on a specific topic
    public void publish(String topic, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        client.publish(topic, message);
    }

    public String getVehicleId() {
        return vehicleId;
    }
}
