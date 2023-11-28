import processes.FrontBlinker;
import processes.LaneChanger;
import processes.SpeedChanger;
import setup.MqttHandler;
import setup.VehicleConnector;
import setup.VehicleDiscoverer;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        // Enable logging
        Logger.getLogger("").setLevel(Level.WARNING);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.WARNING);

        // Parameters
        String mqttBrokerUri = "tcp://192.168.4.1:1883";
        String clientId = "Severine";
        String vehicleId = "d205effe02cb"; // TODO: adapt each time among ["cec233dec1cb", "f2e85f2f5770", "e10a07218a87", "d11d2fea5c74", "cb443e1e4025", "d205effe02cb", "d716ea410e89", "ef7e50abbe16", "f4c22c6c0382"]

        // Create MQTT Handler to have a client and connect it to the MQTT broker
        MqttHandler handler = null;
        try {
            handler = new MqttHandler(mqttBrokerUri, clientId, vehicleId);
        } catch (MqttException e){
            e.getMessage();
        }

        // Discover vehicle
        VehicleDiscoverer discoverer = new VehicleDiscoverer(handler);
        discoverer.run();

        // Connect vehicle
        VehicleConnector vehicleConnector = new VehicleConnector(handler);
        vehicleConnector.run();

        // Process 1 - Make the vehicle front lights blink
        FrontBlinker blinker = new FrontBlinker(handler);
        Thread blinkerThread = new Thread(blinker);

        // Process 2 - Change lane
        LaneChanger laneChanger = new LaneChanger(handler);
        Thread laneChangerThread = new Thread(laneChanger);

        // Process 3 - Make the vehicle front lights blink
        SpeedChanger speedChanger = new SpeedChanger(handler);
        Thread speedChangerThread = new Thread(speedChanger);

        blinkerThread.start();
        laneChangerThread.start();
        speedChangerThread.start();

        try {System.in.read();}
        catch (IOException e){
            e.getMessage();
            e.getStackTrace();
        }
        blinkerThread.interrupt();
        laneChangerThread.interrupt();
        speedChangerThread.interrupt();
    }
}
