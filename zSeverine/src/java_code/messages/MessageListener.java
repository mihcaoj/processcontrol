package java_code.messages;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageListener {
    private final String topic;
    private final BlockingQueue<MqttMessage> messageQueue;

    public MessageListener(String topic){
        this.topic = topic;
        this.messageQueue = new LinkedBlockingQueue<>();
    };

    public void addMessage(MqttMessage payload){
        messageQueue.offer(payload);
    };

    public String getTopic(){
        return topic;
    };

    public BlockingQueue<MqttMessage> getMessageQueue() {
        return messageQueue;
    }
}