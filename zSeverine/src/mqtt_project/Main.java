package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Parameters
        String mqttBrokerUri = "tcp://192.168.4.1:1883";
        String clientId = "GroupD";
        String vehicleId = "cb443e1e4025"; // TODO: adapt each time among ["d98ebab7c206", "cec233dec1cb", "f2e85f2f5770", "e10a07218a87", "d11d2fea5c74", "cb443e1e4025", "d205effe02cb", "d716ea410e89", "ef7e50abbe16", "f4c22c6c0382"]

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

        // MVC pattern split in two with a common view
        SteeringModel steeringModel = new SteeringModel();
        VehicleInfoModel vehicleInfoModel = new VehicleInfoModel();

        View view = new View(steeringModel, vehicleInfoModel);

        SteeringController steeringController = new SteeringController(mqttHandler, steeringModel, view);
        VehicleInfoController vehicleInfoController = new VehicleInfoController(mqttHandler, vehicleInfoModel, view);
        // Steering thread (controls the processes like the speed, lights, track offset,...)
        Thread steeringThread = new Thread(steeringController);

        // Thread measuring all infos from vehicle
        Thread infoThread = new Thread(vehicleInfoController);

        // Start threads
        steeringThread.start();
        infoThread.start();

        try {System.in.read();}
        catch (IOException e){
            System.out.println(e.getMessage());
            e.getStackTrace();
        }

        steeringThread.interrupt();
        infoThread.interrupt();
    }
}

