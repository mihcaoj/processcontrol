package mqtt_blinking_prof_code.strongcoupling;

import org.eclipse.paho.client.mqttv3.MqttMessage;


public interface MessageListener {
    public void addMQTTMessage(String topic, MqttMessage message);
    
}