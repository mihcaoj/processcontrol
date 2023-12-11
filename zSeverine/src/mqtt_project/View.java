package mqtt_project;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;


public class View extends JFrame implements Observer {
    private final SteeringModel steeringModel;
    private final VehicleInfoModel vehicleInfoModel;
    private JPanel speedPanel;
    private JLabel wishedSpeedLabel;
    private JLabel effectiveSpeedLabel;

    private JSlider speedSlider;
    private JPanel laneOffsetPanel;
    private JLabel laneOffsetLabel;
    private JSlider laneOffsetSlider;

    private JPanel lightsPanel;
    private JPanel frontLightsPanel;
    private JRadioButton frontLightsOnButton;
    private JRadioButton frontLightsOffButton;
    private JPanel backLightsPanel;
    private JRadioButton backLightsOnButton;
    private JRadioButton backLightsOffButton;
    private JRadioButton backLightsFlickerButton;

    private JToggleButton emergencyToggleButton;

    private JPanel trackPanel;
    private JLabel trackIdLabel;
    private JLabel turningStatusLabel;

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
        this.speedPanel = new JPanel();
        int wishedSpeed = steeringModel.getWishedSpeed();
        this.wishedSpeedLabel = new JLabel();
        this.wishedSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateWishedSpeedLabel(wishedSpeed);
        this.effectiveSpeedLabel = new JLabel();
        this.effectiveSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateEffectiveSpeedLabel(vehicleInfoModel.getMeasuredSpeed());
        this.speedSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, wishedSpeed);
        speedSlider.addChangeListener(e -> {
            int speedValue = speedSlider.getValue();
            steeringModel.setWishedSpeed(speedValue);
            updateWishedSpeedLabel(speedValue);
        });
        this.speedPanel.add(wishedSpeedLabel);
        this.speedPanel.add(effectiveSpeedLabel);
        this.speedPanel.add(speedSlider);

        // Lane offset control
        int wishedLaneOffset = steeringModel.getWishedLaneOffset();
        this.laneOffsetPanel = new JPanel();
        this.laneOffsetLabel = new JLabel("Wished lane offset: "+wishedLaneOffset);
        this.laneOffsetLabel.setHorizontalAlignment(JLabel.CENTER);
        this.laneOffsetSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, wishedLaneOffset);
        laneOffsetSlider.addChangeListener(e -> {
            int laneOffsetValue = laneOffsetSlider.getValue();
            // Update the wished lane offset in the steering model
            steeringModel.setWishedLaneOffset(laneOffsetValue);
            updateLaneOffsetLabel(laneOffsetValue);
        });
        this.laneOffsetPanel.add(laneOffsetLabel);
        this.laneOffsetPanel.add(laneOffsetSlider);

        // Lights control
        this.lightsPanel = new JPanel(new FlowLayout());

        this.frontLightsPanel = new JPanel(new FlowLayout());
        frontLightsPanel.add(new JLabel("Front lights"));
        ButtonGroup frontLightsButtonGroup = new ButtonGroup();
        frontLightsOnButton = new JRadioButton("On");
        frontLightsOffButton = new JRadioButton("Off");
        frontLightsButtonGroup.add(frontLightsOnButton);
        frontLightsButtonGroup.add(frontLightsOffButton);
        frontLightsPanel.add(frontLightsOnButton);
        frontLightsPanel.add(frontLightsOffButton);
        lightsPanel.add(frontLightsPanel);
        frontLightsOnButton.addActionListener(e -> {
            this.steeringModel.setWishedFrontLightStatus("on");
        });
        frontLightsOffButton.addActionListener(e -> {
            this.steeringModel.setWishedFrontLightStatus("off");
        });

        this.backLightsPanel = new JPanel(new FlowLayout());
        backLightsPanel.add(new JLabel("Back lights"));
        ButtonGroup backLightsButtonGroup = new ButtonGroup();
        backLightsOnButton = new JRadioButton("On");
        backLightsOffButton = new JRadioButton("Off");
        backLightsFlickerButton = new JRadioButton("Flicker");
        backLightsButtonGroup.add(backLightsOnButton);
        backLightsButtonGroup.add(backLightsOffButton);
        backLightsButtonGroup.add(backLightsFlickerButton);
        backLightsPanel.add(backLightsOnButton);
        backLightsPanel.add(backLightsOffButton);
        backLightsPanel.add(backLightsFlickerButton);
        lightsPanel.add(backLightsPanel);
        backLightsOnButton.addActionListener(e -> {
            this.steeringModel.setWishedBackLightStatus("on");
        });

        backLightsOffButton.addActionListener(e -> {
            this.steeringModel.setWishedBackLightStatus("off");
        });

        backLightsFlickerButton.addActionListener(e -> {
            this.steeringModel.setWishedBackLightStatus("flicker");
        });

        // Emergency control
        this.emergencyToggleButton = new JToggleButton("Activate emergency");
        emergencyToggleButton.addActionListener(e -> {
            boolean emergency = emergencyToggleButton.isSelected();
            String text = emergency ? "Deactivate emergency": "Activate emergency";
            int speedMin = emergency ? 0: -100;
            int speedMax = emergency ? 0: 2000;
            this.speedSlider.setMinimum(speedMin);
            this.speedSlider.setMaximum(speedMax);
            int laneOffsetMin = emergency ? 0:-100;
            int laneOffsetMax = emergency ? 0:200;
            this.laneOffsetSlider.setMinimum(laneOffsetMin);
            this.laneOffsetSlider.setMaximum(laneOffsetMax);
            this.emergencyToggleButton.setText(text);
            this.steeringModel.setEmergency(emergencyToggleButton.isSelected());
        });

        // Track information
        this.trackPanel = new JPanel();
        this.trackIdLabel = new JLabel();
        updateTrackIdLabelLabel(this.vehicleInfoModel.getCurrentTrackId());
        this.turningStatusLabel = new JLabel();
        updateTurningStatusLabel(this.vehicleInfoModel.getTurningStatus());

        add(speedPanel);
        add(laneOffsetPanel);
        add(lightsPanel);
        add(emergencyToggleButton);
        add(trackPanel);

        pack();
        setVisible(true);
    }

    public void updateWishedSpeedLabel(int wishedSpeed) {
        wishedSpeedLabel.setText("Wished speed: " + wishedSpeed);
    }

    public void updateEffectiveSpeedLabel(int effectiveSpeed) {
        effectiveSpeedLabel.setText("Effective speed: " + effectiveSpeed);
    }

    public void updateLaneOffsetLabel(int laneOffsetValue){
        laneOffsetLabel.setText("Wished lane offset: " + laneOffsetValue);
    }

    public void updateTrackIdLabelLabel(int trackId){
        laneOffsetLabel.setText("Current track ID: "+trackId);
    }

    public void updateTurningStatusLabel(boolean turningStatus){
        laneOffsetLabel.setText("Turning track: " + turningStatus);
    }
}
