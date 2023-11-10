package lecture07.blinking.weakcoupling;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private final MqttAsyncClient client;
    private final MqttConnectOptions options;
    private final Set<MessageListener> messageListeners;

    public MqttHandler(String broker, String clientId) throws MqttException {
        messageListeners = new HashSet<>();
        client = new MqttAsyncClient(broker, clientId, new MemoryPersistence());
        client.setCallback(new MessageHandler());
        options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options).waitForCompletion();

    }

    public void subscribe(String topic) throws MqttException {
        client.subscribe(topic, 1);  // QoS level 1
    }

    public void unsubscribe(String topic) throws MqttException {
        client.unsubscribe(topic);
    }

    public void publish(String topic, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        client.publish(topic, message);
    }

    public void addMessageListener(MessageListener listener) {
        this.messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        this.messageListeners.remove(listener);
    }

    class MessageHandler implements MqttCallbackExtended {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            for (MessageListener listener : messageListeners) {
                listener.addMQTTMessage(topic, message);
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
    }
}
