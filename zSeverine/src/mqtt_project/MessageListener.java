package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttMessage;
// TODO remove:
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageListener {
    private final String topic;
    private MqttMessage lastMessage;
    // TODO remove: private final BlockingQueue<MqttMessage> messageQueue;
    public MessageListener(String topic){
        this.topic = topic;
        this.lastMessage=null;
        // TODO remove: this.messageQueue = new LinkedBlockingQueue<>();
    };

    public void addMessage(MqttMessage payload){
        this.lastMessage = payload;
        //TODO: remove messageQueue.offer(payload);
    };

    public String getTopic(){
        return topic;
    };

    //TODO remove: public BlockingQueue<MqttMessage> getMessageQueue() {return messageQueue;}
    public MqttMessage getLastMessage(){
        return lastMessage;
    }
}