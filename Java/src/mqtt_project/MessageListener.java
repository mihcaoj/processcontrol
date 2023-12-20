package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/* Recipient of the last message published in a given topic to which the customer has subscribed */
public class MessageListener {
    private final String topic;
    private MqttMessage lastMessage;
    public MessageListener(String topic){
        this.topic = topic;
        this.lastMessage=null;
    };

    public void addMessage(MqttMessage payload){
        this.lastMessage = payload;
    };

    public String getTopic(){
        return topic;
    };

    public MqttMessage getLastMessage(){
        return lastMessage;
    }
}