package mqtt_blinking_prof_code.weakcoupling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VehicleFrontBlinker implements MessageListener, Runnable {

    private record MqttMessageRecord(String topic, MqttMessage message) {

    }
    ;
    private final MqttHandler mqttHandler;
    private final BlockingQueue<MqttMessageRecord> mqttMessageQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VehicleFrontBlinker(MqttHandler mqttHandler) throws MqttException {
        this.mqttHandler = mqttHandler;
        this.mqttHandler.addMessageListener(this);
        this.mqttHandler.subscribe("Blinking/U/knr1/WeakCoupling/ConnectedObserver/E/vehicleConnected");
    }

    public void addVehicleId(String vehicleId) {
    }

    @Override
    public void addMQTTMessage(String topic, MqttMessage message) {
        mqttMessageQueue.offer(new MqttMessageRecord(topic, message));
    }

    @Override
    public void run() {
        try {
            MqttMessageRecord mqttMessageRecord;
            String vehicleId = null;
            {
                boolean conditionAchieved = false;
                while (!conditionAchieved) {
                    mqttMessageRecord = mqttMessageQueue.take();
                    if (mqttMessageRecord.topic.endsWith("U/knr1/WeakCoupling/ConnectedObserver/E/vehicleConnected")) {
                        vehicleId = new String(mqttMessageRecord.message.getPayload());
                        conditionAchieved = true;
                    }
                }
                System.out.println("Start the blink on vehicle: " + vehicleId);
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("type", "lights");
                while (true) {
                    payload.putObject("payload").put("front", "on");
                    mqttHandler.publish("Anki/Vehicles/U/" + vehicleId + "/I", payload.toString());
                    Thread.sleep(500);

                    payload.putObject("payload").put("front", "off");
                    mqttHandler.publish("Anki/Vehicles/U/" + vehicleId + "/I", payload.toString());
                    Thread.sleep(500);
                }
            }

        } catch (InterruptedException ex) {
            System.out.println("We have been interrupted. So leaving the observer");
        } catch (MqttException ex) {
            Logger.getLogger(VehicleFrontBlinker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
