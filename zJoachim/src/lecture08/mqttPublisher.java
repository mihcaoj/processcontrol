package lecture08;

import org.eclipse.paho.client.mqttv3.*;

public class MqttPublisher {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883";
        String clientId = "Publisher";
        String topic = "test/topic";

        try {
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions connectOpts = new MqttConnectOptions();
            client.connect(connectOpts);

            String message = "Hello, MQTT!";
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage);

            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
