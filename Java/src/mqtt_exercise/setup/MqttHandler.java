package mqtt_exercise.setup;

import mqtt_exercise.messages.MessageHandler;
import mqtt_exercise.messages.MessageListener;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Hashtable;

/* MQTT handler is a class responsible for managing the communication between the application and the MQTT broker.*/
public class MqttHandler {
    private String vehicleId;
    private final MqttAsyncClient client;
    private final MqttConnectOptions options;
    private final MessageHandler msgHandler;
    public Hashtable<String, String> topicPathByName;

    public MqttHandler(String mqttBrokerUri, String clientId, String vehicleId) throws MqttException {
        this.vehicleId = vehicleId;

        // client creation - MemoryPersistence make the state to be in RAM, not on disk.
        this.client = new MqttAsyncClient(mqttBrokerUri, clientId, new MemoryPersistence());

        // message handling
        this.msgHandler = new MessageHandler();
        this.client.setCallback(this.msgHandler);

        // connection options (10 seconds timeout)
        this.options = new MqttConnectOptions();
        this.options.setAutomaticReconnect(true);
        this.options.setCleanSession(true);
        this.options.setConnectionTimeout(10);

        // client connection to the broker
        try{
            this.client.connect(this.options).waitForCompletion();
        } catch (MqttException e){
            e.getMessage();
            e.getStackTrace();
        }

        // Set up a dictionary (key: TopicName, value: TopicPath)
        topicPathByName = new Hashtable<>();
        topicPathByName.put("HostIntent","Anki/Hosts/U/hyperdrive/I/"+clientId);
        topicPathByName.put("HostDiscoveringStatus","Anki/Hosts/U/hyperdrive/S/discovering");
        topicPathByName.put("HostVehiclesStatus","Anki/Hosts/U/hyperdrive/S/vehicles");
        topicPathByName.put("singleVehicleIntent","Anki/Vehicles/U/"+vehicleId+"/I/"+clientId);
        topicPathByName.put("singleVehicleStatus","Anki/Vehicles/U/"+vehicleId+"/S/status");
    }

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

    public void publish(String topic, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        client.publish(topic, message);
    }

    public String getVehicleId() {
        return vehicleId;
    }
}
