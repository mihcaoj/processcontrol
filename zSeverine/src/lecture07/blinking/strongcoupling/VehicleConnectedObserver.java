package lecture07.blinking.strongcoupling;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class VehicleConnectedObserver implements MessageListener, Runnable {

    private record MqttMessageRecord(String topic, MqttMessage message) {

    }
    ;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JSONParser parser = new JSONParser();

    private final MqttHandler mqttHandler;
    private final BlockingQueue<MqttMessageRecord> mqttMessageQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> vehicleIdQueue = new LinkedBlockingDeque<>();
    private final VehicleFrontBlinker blinker;

    public VehicleConnectedObserver(MqttHandler mqttHandler, VehicleFrontBlinker blinker) throws MqttException {
        this.blinker = blinker;
        this.mqttHandler = mqttHandler;
        this.mqttHandler.addMessageListener(this);

    }

    @Override
    public void addMQTTMessage(String topic, MqttMessage message) {
        mqttMessageQueue.offer(new MqttMessageRecord(topic, message));
    }

    public void addVehicleId(String vehicleId) {
        vehicleIdQueue.offer(vehicleId);
    }

    public void run() {
        try {
            MqttMessageRecord mqttMessageRecord;
            String vehicleId = vehicleIdQueue.take();
            System.out.println("Start observing the status of vehicle: "+vehicleId);
            
            mqttHandler.subscribe("Anki/Vehicles/U/" + vehicleId + "/S/status");
            {
                boolean conditionAchieved = false;
                while (!conditionAchieved) {

                    mqttMessageRecord = mqttMessageQueue.take();
                    if (mqttMessageRecord.topic.equals("Anki/Vehicles/U/" + vehicleId + "/S/status")) {
                        try {
                            JSONObject jsonObj = (JSONObject) parser.parse(new String(mqttMessageRecord.message.getPayload()));
                            String connectionStatus = (String) jsonObj.get("value");
                            if (connectionStatus.equals("ready")) {
                                conditionAchieved = true;
                            }
                        } catch (ParseException ex) {
                            Logger.getLogger(VehicleConnectedObserver.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            this.blinker.addVehicleId(vehicleId);
        } catch (InterruptedException ex) {
            System.out.println("We have been interrupted. So leaving the observer");
        } catch (MqttException ex) {
            Logger.getLogger(VehicleConnectedObserver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
        this.mqttHandler.removeMessageListener(this);
        //this.mqttHandler.unsubscribe("Hosts/U/+/E/vehicle/discovered");  
    }
}
