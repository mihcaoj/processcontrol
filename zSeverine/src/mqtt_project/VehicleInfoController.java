package mqtt_project;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Observable;
import java.util.Observer;

public class VehicleInfoController implements Observer {
    private final SteeringModel steeringModel;
    private final View view;
    private final MqttHandler mqttHandler;
    private final String vehicleIntentTopic;

    public VehicleInfoController(MqttHandler mqttHandler, VehicleInfoModel vehicleInfoModel, View view) {
        this.mqttHandler = mqttHandler;
        this.vehicleIntentTopic = mqttHandler.topicPathByName.get("singleVehicleIntent");
        this.steeringModel = steeringModel;
        this.view = view;
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
                e.getMessage();
                e.getStackTrace();
            }
        }
    }



    private void updateView() {
        int wishedSpeed = steeringModel.getWishedSpeed();
        view.updateWishedSpeedLabel(wishedSpeed);

        int wishedLaneOffset = steeringModel.getWishedLaneOffset();
        view.updateLaneOffsetLabel(wishedLaneOffset);

    }

    private void updateVehicle() {
        // Update the vehicle based on changes in the model
        // For example:
        String wishedFrontLightStatus = steeringModel.getWishedFrontLightStatus();
        String wishedBackLightStatus = steeringModel.getWishedBackLightStatus();
        // Send messages to the vehicle to change lights
        // vehicle.changeLights(wishedFrontLightStatus, wishedBackLightStatus);

        int wishedSpeed = steeringModel.getWishedSpeed();
        // Send message to the vehicle to change speed
        // vehicle.changeSpeed(wishedSpeed);

        int wishedLaneOffset = steeringModel.getWishedLaneOffset();
        // Send message to the vehicle to change lane offset
        // vehicle.changeLane(wishedLaneOffset);

        // Update the vehicle based on other changes in the model...
    }
}