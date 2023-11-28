package java_code.processes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java_code.setup.MqttHandler;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author reto
 */
public class EmergencyStop implements MessageListener, Runnable {
private final JSONParser parser = new JSONParser();
private final MqttHandler handler;
private boolean isEmergencyActive;
private final BlockingQueue<MqttMessageRecord> mqttVehicleIntentMessageQueue = new LinkedBlockingQueue<>();
private final BlockingQueue<MqttMessageRecord> mqttEmergencyQueue = new LinkedBlockingQueue<>();
private final ObjectMapper objectMapper = new ObjectMapper();

public EmergencyStop(MqttHandler handler) throws MqttException {
    this.handler = handler;
    this.handler.addMessageListener(this);
    this.handler.subscribe("EmergencyProxy/U/quantasy/I/#");
    this.handler.subscribe("EmergencyProxy/U/quantasy/Anki/Vehicles/#");
}

@Override
public void addMQTTMessage(String topic, MqttMessage message) {
    if (topic.startsWith("EmergencyProxy/U/quantasy/I")) {
        mqttEmergencyQueue.add(new MqttMessageRecord(topic, message));
    } else if (topic.startsWith("EmergencyProxy/U/quantasy/Anki/Vehicles")) {
        mqttVehicleIntentMessageQueue.add(new MqttMessageRecord(topic,message));
    }
}

@Override
public void run() {
    Thread emergencyStatusThread = new Thread(() -> this.processEmergencyStatusMessages());
    emergencyStatusThread.start();

    try {
        processVehicleIntentMessages();
    } finally {
        emergencyStatusThread.interrupt();
    }
}

private void processEmergencyStatusMessages() {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            MqttMessageRecord record = mqttEmergencyQueue.take();
            updateEmergencyStatus(record);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Emergency status thread interrupted");
    }
}

private void updateEmergencyStatus(MqttMessageRecord record) {
    try {
        JsonNode rootNode = objectMapper.readTree(record.message.getPayload());
        JsonNode payloadNode = rootNode.path("payload");
        JsonNode valueNode = payloadNode.path("value");
        isEmergencyActive = valueNode.asBoolean();
        publishEmergencyStatus();
    } catch (IOException | MqttException e) {
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error processing emergency status", e);
    }
}

private void publishEmergencyStatus() throws MqttException {
    {
        JSONObject payload = new JSONObject();
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("value", isEmergencyActive);
        handler.publish("EmergencyProxy/U/quantasy/S/emergency", payload.toJSONString());
    }
    // If you have only one/some specific vehicles in your system, you can call those vehicles explicite, instead of calling all vehicles implicite.
    if (isEmergencyActive) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "speed");
        payload.putObject("payload").put("velocity", "0");
        payload.putObject("payload").put("acceleration", "2000");
        handler.publish("Anki/Vehicles/I/EmergencyProxy/U/quantasy", payload.toString());
    }
}

private void processVehicleIntentMessages() {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            MqttMessageRecord record = mqttVehicleIntentMessageQueue.take();
            processVehicleIntentMessage(record);
        }
    } catch (InterruptedException e) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Vehicle message processing thread interrupted");
    }
}

private void processVehicleIntentMessage(MqttMessageRecord record) {
    try {
        byte[] payload = record.message.getPayload();
        String targetTopic = record.topic.replace("EmergencyProxy/U/quantasy/Anki/Vehicles", "Anki/Vehicles");
        if (isEmergencyActive) {
            payload = removeEntries(payload);
        }
        if (payload != null) {
            handler.publish(targetTopic, new String(payload));
        }
    } catch (IOException | MqttException e) {
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error processing vehicle message", e);
    }
}

private byte[] removeEntries(byte[] jsonInput) throws JsonProcessingException, IOException {
    JsonNode rootNode = objectMapper.readTree(jsonInput);
    if (rootNode.isArray()) {
        ArrayNode arrayNode = (ArrayNode) rootNode;
        removeEntriesFromArray(arrayNode);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(arrayNode);
    } else if (rootNode.isObject() && shouldBeRemoved(rootNode)) {
        return null;
    }
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(rootNode);
}

private void removeEntriesFromArray(ArrayNode arrayNode) {
    Iterator<JsonNode> iterator = arrayNode.elements();
    while (iterator.hasNext()) {
        JsonNode node = iterator.next();
        if (shouldBeRemoved(node)) {
            iterator.remove();
        }
    }
}

private boolean shouldBeRemoved(JsonNode node) {
    if (!node.has("type")) {
        return false;
    }
    String type = node.get("type").asText();
    return "speed".equals(type) || "lane".equals(type);
}

private record MqttMessageRecord(String topic, MqttMessage message) {

}

}

}
