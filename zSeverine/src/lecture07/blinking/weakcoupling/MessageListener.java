package lecture07.blinking.weakcoupling;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface MessageListener {
    public void addMQTTMessage(String topic, MqttMessage message);
    
}