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
            String msg = "";
            switch ((int) arg){
                case SteeringModel.WISHED_SPEED_UPDATE:
                    int speed = this.steeringModel.getEmergency() ? 0 : this.steeringModel.getWishedSpeed();
                    msg = MessageHandler.createIntentMsg("speed", new String[][]{{"velocity", String.valueOf(speed)}});
                    break;
                case SteeringModel.WISHED_LANE_OFFSET:
                    int laneOffset = this.steeringModel.getEmergency() ? 0 : this.steeringModel.getWishedLaneOffset();
                    msg = MessageHandler.createIntentMsg("lane", new String[][]{{"offset", String.valueOf(laneOffset)}});
                    break;
                case SteeringModel.EMERGENCY_UPDATE:
                    msg = MessageHandler.createIntentMsg("speed", new String[][]{{"velocity", String.valueOf(0)}});
                    break;
                case SteeringModel.FRONT_LIGHTS_UPDATE:
                    msg = MessageHandler.createIntentMsg("lights", new String[][]{{"front", this.steeringModel.getWishedFrontLightStatus()}});
                    break;
                case SteeringModel.BACK_LIGHTS_UPDATE:
                    msg = MessageHandler.createIntentMsg("lights", new String[][]{{"back", this.steeringModel.getWishedBackLightStatus()}});
                    break;
            }
            try {
                mqttHandler.publish(vehicleIntentTopic, msg);
                System.out.println("INTENT PUBLICATION - Message:" + msg);
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