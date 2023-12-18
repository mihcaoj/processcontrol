package mqtt_blinking_prof_code.strongcoupling;
// Uses a queue to have multiple threads

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            MqttHandler mqttHandler = new MqttHandler("tcp://192.168.4.1:1883", "VehicleClientSev");
            VehicleFrontBlinker blinker = new VehicleFrontBlinker(mqttHandler);
            Thread blinkerThread = new Thread(blinker);

            VehicleConnectedObserver connectObserver = new VehicleConnectedObserver(mqttHandler, blinker);
            Thread connectThread = new Thread(connectObserver);

            VehicleDiscoverObserver discoverer = new VehicleDiscoverObserver(mqttHandler, connectObserver);
            Thread discovererThread = new Thread(discoverer);
            
            blinkerThread.start();
            connectThread.start();
            discovererThread.start();

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", "discover");
            payload.putObject("payload").put("value", "true");
            mqttHandler.publish("Anki/Hosts/U/hyperdrive/I", payload.toString());

            System.in.read();
            discovererThread.interrupt();
            connectThread.interrupt();
            blinkerThread.interrupt();
        } catch (IOException | MqttException e) {
            e.printStackTrace();
        }
    }
}
