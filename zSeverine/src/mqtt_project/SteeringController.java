package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Observable;
import java.util.Observer;

public class SteeringController implements Observer, Runnable{
    private final SteeringModel steeringModel;
    private final MqttHandler mqttHandler;
    private final String vehicleIntentTopic;

    public SteeringController(MqttHandler mqttHandler, SteeringModel steeringModel, View view) {
        this.mqttHandler = mqttHandler;
        this.vehicleIntentTopic = mqttHandler.topicPathByName.get("singleVehicleIntent");
        this.steeringModel = steeringModel;
        this.steeringModel.addObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof SteeringModel) {
            String changedValue = (String) arg;
            String msg = "";
            switch (changedValue){
                case "wished speed":
                    int speed = this.steeringModel.getEmergency() ? 0 : this.steeringModel.getWishedSpeed();
                    msg = MessageHandler.createIntentMsg("speed", new String[][]{{"velocity", String.valueOf(speed)}});
                case "wished lane offset":
                    int laneOffset = this.steeringModel.getEmergency() ? 0 : this.steeringModel.getWishedLaneOffset();
                    msg = MessageHandler.createIntentMsg("lane", new String[][]{{"offset", String.valueOf(laneOffset)}});
                case "emergency":
                    msg = MessageHandler.createIntentMsg("speed", new String[][]{{"velocity", String.valueOf(0)}});
                case "front lights status":
                    msg = MessageHandler.createIntentMsg("lights", new String[][]{{"front", this.steeringModel.getWishedFrontLightStatus()}});
                case "back lights status":
                    msg = MessageHandler.createIntentMsg("lights", new String[][]{{"back", this.steeringModel.getWishedBackLightStatus()}});
            }
            try {
                mqttHandler.publish(vehicleIntentTopic, msg);
            } catch (MqttException e){
                System.out.println(e.getMessage());
                e.getStackTrace();
            }
        }
    }

    @Override
    public void run() {

    }
}