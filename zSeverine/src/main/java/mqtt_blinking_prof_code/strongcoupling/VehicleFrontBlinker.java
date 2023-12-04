package mqtt_blinking_prof_code.strongcoupling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.paho.client.mqttv3.MqttException;

public class VehicleFrontBlinker implements Runnable {

    private final MqttHandler mqttHandler;
    private final BlockingQueue<String> vehicleQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VehicleFrontBlinker(MqttHandler mqttHandler) {
        this.mqttHandler = mqttHandler;
    }

    public void addVehicleId(String vehicleId) {
        vehicleQueue.offer(vehicleId);
    }

    @Override
    public void run() {
        try {
            String vehicleID = vehicleQueue.take();
            System.out.println("Start the blink on vehicle: "+vehicleID);
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", "lights");
            while (true) {
                payload.putObject("payload").put("front", "on");
                mqttHandler.publish("Anki/Vehicles/U/" + vehicleID + "/I", payload.toString());
                Thread.sleep(500);

                payload.putObject("payload").put("front", "off");
                mqttHandler.publish("Anki/Vehicles/U/" + vehicleID + "/I", payload.toString());
                Thread.sleep(500);
            }
        } catch (InterruptedException | MqttException e) {
            System.out.println("We have been interrupted. So leaving the observer");
        }
    }
}
