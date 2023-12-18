package mqtt_project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashSet;
import java.util.Set;

/*Component handling incoming messages (reception + processing)*/
public class MessageHandler implements MqttCallbackExtended {

    private final Set<MessageListener> msgListeners;

    public MessageHandler(){
        super();
        this.msgListeners = new HashSet<>();
    }

    @Override
    public void messageArrived(String topic, MqttMessage payload) {
        for (MessageListener listener : msgListeners) {
            if (topic.equals(listener.getTopic())) {
                listener.addMessage(payload);
            }
        }
    }

    @Override
    public void connectComplete(boolean bln, String string) {
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
    }

    public Set<MessageListener> getMsgListeners() {
        return msgListeners;
    }

    public MessageListener getMsgListenerFromTopic(String topic){
        try {
            for (MessageListener listener : msgListeners) {
                if (topic.equals(listener.getTopic())) {
                    return listener;
                }
            }
            throw new Exception("No listener exists with this topic.");
        } catch (Exception noListenerException){
            noListenerException.getMessage();
        }
        return null;
    }

    public void addMessageListener(MessageListener listener) {
        this.msgListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        this.msgListeners.remove(listener);
    }

    public static String createIntentMsg(String IntentType, String[][] payload){
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeMsg = objectMapper.createObjectNode();
        objectNodeMsg.put("type", IntentType);
        int lengthPayload = payload.length;
        for (int i=0; i<lengthPayload;i++){
            objectNodeMsg.putObject("payload").put(payload[i][0], payload[i][1]);
        }
        return objectNodeMsg.toString();
    }
}
