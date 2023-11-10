package ch.quantasy.hyperdrive.blinking.strongcoupling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class VehicleDiscoverObserver implements MessageListener, Runnable {

    private record MqttMessageRecord(String topic, MqttMessage message) {

    }
    ;

    JSONParser parser = new JSONParser();
    ObjectMapper objectMapper =new ObjectMapper();
    private final MqttHandler mqttHandler;
    private final BlockingQueue<MqttMessageRecord> queue = new LinkedBlockingQueue<>();
    private final VehicleConnectedObserver vehicleConnectedObserver;

    public VehicleDiscoverObserver(MqttHandler mqttHandler,VehicleConnectedObserver vehicleConnectedObserver) throws MqttException {
        this.vehicleConnectedObserver=vehicleConnectedObserver;
        this.mqttHandler = mqttHandler;
        this.mqttHandler.addMessageListener(this);
        this.mqttHandler.subscribe("Anki/Hosts/U/+/E/vehicle/discovered");
    }

    @Override
    public void addMQTTMessage(String topic, MqttMessage message) {
        queue.offer(new MqttMessageRecord(topic, message));
    }

    public void run() {
        try {
            MqttMessageRecord mqttMessageRecord;
            String vehicleId = null;
            {
                boolean conditionAchieved = false;
                while (!conditionAchieved) {
                    mqttMessageRecord = queue.take();
                    if (mqttMessageRecord.topic.endsWith("E/vehicle/discovered")) {
                        try {
                          
                            JSONObject jsonObj = (JSONObject) parser.parse(new String(mqttMessageRecord.message.getPayload()));
                            vehicleId = (String) jsonObj.get("value");
                            System.out.println("A new Vehicle has been discovered: " + vehicleId);
                            //if(vehicleId.equals("your desiredID"))
                            conditionAchieved = true;
                    
                        } catch (ParseException ex) {
                            Logger.getLogger(VehicleDiscoverObserver.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            vehicleConnectedObserver.addVehicleId(vehicleId);
            
            
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", "connect");
            payload.putObject("payload").put("value", "true");
            mqttHandler.publish("Anki/Vehicles/U/" + vehicleId + "/I", payload.toString());

        } catch (InterruptedException ex) {
            System.out.println("We have been interrupted. So leaving the observer");
        } catch (MqttException ex) {
            Logger.getLogger(VehicleDiscoverObserver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
        this.mqttHandler.removeMessageListener(this);
        //this.mqttHandler.unsubscribe("Hosts/U/+/E/vehicle/discovered");  
    }
}
