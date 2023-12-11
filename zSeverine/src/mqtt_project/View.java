package mqtt_project;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;


public class View extends JFrame implements Observer {
    private final SteeringModel steeringModel;
    private final VehicleInfoModel vehicleInfoModel;
    private JLabel wishedSpeedLabel;
    private JLabel measuredSpeedLabel;
    private JSlider speedSlider;
    private JLabel laneOffsetLabel;
    private JSlider laneOffsetSlider;

    private JToggleButton emergencyToggleButton;

    private JLabel trackIdLabel;
    private JLabel turningStatusLabel;
    private JLabel batteryLevelLabel;
    private JLabel lowBatteryLabel;

    public View(SteeringModel steeringModel, VehicleInfoModel vehicleInfoModel){
        super("Hyperdrive");

        this.steeringModel = steeringModel;
        this.vehicleInfoModel = vehicleInfoModel;
        this.steeringModel.addObserver(this);
        this.vehicleInfoModel.addObserver(this);

        initializeComponents();
    }
    public void update(Observable o, Object arg) {
        this.trackIdLabel.setText("Current track ID: "+this.vehicleInfoModel.getCurrentTrackId());
        this.turningStatusLabel.setText("Turning track: "+ this.vehicleInfoModel.getTurningStatus());
        repaint();
    }

    public void initializeComponents(){
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new GridLayout(2, 2));

        // Speed control
        JPanel speedPanel = new JPanel();
        int wishedSpeed = steeringModel.getWishedSpeed();
        this.wishedSpeedLabel = new JLabel();
        this.wishedSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateWishedSpeedLabel(wishedSpeed);
        this.measuredSpeedLabel = new JLabel();
        this.measuredSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateEffectiveSpeedLabel(vehicleInfoModel.getMeasuredSpeed());
        this.speedSlider = new JSlider(JSlider.HORIZONTAL, wishedSpeed);
        speedSlider.addChangeListener(e -> {
            int speedValue = speedSlider.getValue();
            steeringModel.setWishedSpeed(speedValue);
            updateWishedSpeedLabel(speedValue);
        });
        speedPanel.add(wishedSpeedLabel);
        speedPanel.add(measuredSpeedLabel);
        speedPanel.add(speedSlider);

        // Lane offset control
        int wishedLaneOffset = steeringModel.getWishedLaneOffset();
        JPanel laneOffsetPanel = new JPanel();
        this.laneOffsetLabel = new JLabel("Wished lane offset: "+wishedLaneOffset);
        this.laneOffsetLabel.setHorizontalAlignment(JLabel.CENTER);
        this.laneOffsetSlider = new JSlider(JSlider.HORIZONTAL, wishedLaneOffset);
        laneOffsetSlider.addChangeListener(e -> {
            int laneOffsetValue = laneOffsetSlider.getValue();
            // Update the wished lane offset in the steering model
            steeringModel.setWishedLaneOffset(laneOffsetValue);
            updateLaneOffsetLabel(laneOffsetValue);
        });
        laneOffsetPanel.add(laneOffsetLabel);
        laneOffsetPanel.add(laneOffsetSlider);

        setMinMaxSpeedLaneOffset();

        // Lights control
        JPanel lightsPanel = new JPanel(new FlowLayout());

        JPanel frontLightsPanel = new JPanel(new FlowLayout());
        frontLightsPanel.add(new JLabel("Front lights"));
        ButtonGroup frontLightsButtonGroup = new ButtonGroup();
        JRadioButton frontLightsOnButton = new JRadioButton("On");
        JRadioButton frontLightsOffButton = new JRadioButton("Off");
        frontLightsButtonGroup.add(frontLightsOnButton);
        frontLightsButtonGroup.add(frontLightsOffButton);
        frontLightsPanel.add(frontLightsOnButton);
        frontLightsPanel.add(frontLightsOffButton);
        lightsPanel.add(frontLightsPanel);
        frontLightsOnButton.addActionListener(e -> this.steeringModel.setWishedFrontLightStatus("on"));
        frontLightsOffButton.addActionListener(e -> this.steeringModel.setWishedFrontLightStatus("off"));

        JPanel backLightsPanel = new JPanel(new FlowLayout());
        backLightsPanel.add(new JLabel("Back lights"));
        ButtonGroup backLightsButtonGroup = new ButtonGroup();
        JRadioButton backLightsOnButton = new JRadioButton("On");
        JRadioButton backLightsOffButton = new JRadioButton("Off");
        JRadioButton backLightsFlickerButton = new JRadioButton("Flicker");
        backLightsButtonGroup.add(backLightsOnButton);
        backLightsButtonGroup.add(backLightsOffButton);
        backLightsButtonGroup.add(backLightsFlickerButton);
        backLightsPanel.add(backLightsOnButton);
        backLightsPanel.add(backLightsOffButton);
        backLightsPanel.add(backLightsFlickerButton);
        lightsPanel.add(backLightsPanel);
        backLightsOnButton.addActionListener(e -> this.steeringModel.setWishedBackLightStatus("on"));

        backLightsOffButton.addActionListener(e -> this.steeringModel.setWishedBackLightStatus("off"));

        backLightsFlickerButton.addActionListener(e -> this.steeringModel.setWishedBackLightStatus("flicker"));

        // Emergency control
        this.emergencyToggleButton = new JToggleButton("Activate emergency");
        emergencyToggleButton.addActionListener(e -> {
            boolean emergency = emergencyToggleButton.isSelected();
            String text = emergency ? "Deactivate emergency": "Activate emergency";
            this.emergencyToggleButton.setText(text);
            this.steeringModel.setEmergency(emergencyToggleButton.isSelected());
        });

        // Vehicle information
        JPanel infoPanel = new JPanel();
        this.trackIdLabel = new JLabel();
        updateTrackIdLabel(this.vehicleInfoModel.getCurrentTrackId());
        this.turningStatusLabel = new JLabel();
        updateTurningStatusLabel(this.vehicleInfoModel.getTurningStatus());
        this.batteryLevelLabel = new JLabel();
        this.lowBatteryLabel = new JLabel();
        this.lowBatteryLabel.setText("!! LOW BATTERY -> Reduced speed !!");
        updateBatteryLevelLabel(this.vehicleInfoModel.getBatteryLevel());
        infoPanel.add(trackIdLabel);
        infoPanel.add(turningStatusLabel);
        infoPanel.add(batteryLevelLabel);
        infoPanel.add(lowBatteryLabel);

        add(speedPanel);
        add(laneOffsetPanel);
        add(lightsPanel);
        add(emergencyToggleButton);
        add(infoPanel);

        pack();
        setVisible(true);
    }

    public void updateWishedSpeedLabel(int wishedSpeed) {
        wishedSpeedLabel.setText("Wished speed: " + wishedSpeed);
    }

    public void updateEffectiveSpeedLabel(int effectiveSpeed) {
        measuredSpeedLabel.setText("Effective speed: " + effectiveSpeed);
    }

    public void updateLaneOffsetLabel(int laneOffsetValue){
        laneOffsetLabel.setText("Wished lane offset: " + laneOffsetValue);
    }

    public void updateTrackIdLabel(int trackId){
        laneOffsetLabel.setText("Current track ID: "+trackId);
    }

    public void updateTurningStatusLabel(boolean turningStatus){
        laneOffsetLabel.setText("Turning track: " + turningStatus);
    }

    public void updateBatteryLevelLabel(int batteryLevel){
        batteryLevelLabel.setText("Battery level: "+ batteryLevel);
        lowBatteryLabel.setVisible(this.vehicleInfoModel.getLowBatteryStatus());
    }

    public void setMinMaxSpeedLaneOffset(){
        int minSpeed;
        int maxSpeed;
        int minLaneOffset;
        int maxLaneOffset;
        if (this.steeringModel.getEmergency()){
            minSpeed =0;
            maxSpeed =0;
            minLaneOffset =0;
            maxLaneOffset =0;
        } else if (this.vehicleInfoModel.getBatteryLevel()<70){
            minSpeed =-50;
            maxSpeed =50;
            minLaneOffset =-20;
            maxLaneOffset =20;
        } else {
            minSpeed =-100;
            maxSpeed =2000;
            minLaneOffset =-100;
            maxLaneOffset =2000;
        }
        this.speedSlider.setMinimum(minSpeed);
        this.speedSlider.setMaximum(maxSpeed);
        int oldWishedSpeed = this.steeringModel.getWishedSpeed();
        int newWishedSpeed = Math.max(Math.min(maxSpeed, oldWishedSpeed), minSpeed);
        this.steeringModel.setWishedSpeed(newWishedSpeed);
        this.laneOffsetSlider.setMinimum(minLaneOffset);
        this.laneOffsetSlider.setMaximum(maxLaneOffset);
        int oldWishedLaneOffset = this.steeringModel.getWishedLaneOffset();
        int newWishedLaneOffset = Math.max(Math.min(maxLaneOffset, oldWishedLaneOffset), minLaneOffset);
        this.steeringModel.setWishedLaneOffset(newWishedLaneOffset);
    }
}
