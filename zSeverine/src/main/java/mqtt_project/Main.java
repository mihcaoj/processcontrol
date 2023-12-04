package mqtt_project;

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
        MqttHandler mqttHandler = null;
        try {
            mqttHandler = new MqttHandler(mqttBrokerUri, clientId, vehicleId);
        } catch (MqttException e){
            e.getMessage();
        }

        // Discover and connect vehicle
        SetupVehicleManager setupVehicleManager = new SetupVehicleManager(mqttHandler);
        setupVehicleManager.run();

        // Steering thread (controls the processes like the speed, lights, track offset,...)
        Thread steeringThread = new Thread(new VehicleController(mqttHandler));

        // Start threads
        steeringThread.start();

        try {System.in.read();}
        catch (IOException e){
            e.getMessage();
            e.getStackTrace();
        }
        steeringThread.interrupt();
    }
}

