package mqtt_project;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

/*This view is simultaneously the view for steering MVC and vehicle info MVC.
* - For steering MVC, it displays graphical components (sliders for speed and lane offset, radio buttons for lights and toggle button to (de)activate emergency status.
*   On each of the component, an event listener determines the actions performed when user manipulate these components.
*   The view sends the steering values set by the user to the steeringModel.
*   When emergency toggle button is selected, the vehicle speed and the lane offset are set on 0 until deactivation of emergency flag.
* - For the vehicle info MVC, it waits for the instruction of vehicleInfoController to know the labels to update
*   and to which value set the battery gauge. Below 30% battery, a label warns the user that speed is reduced because of low battery.*/
public class View extends JFrame implements Observer {
    private final SteeringModel steeringModel;
    private final VehicleInfoModel vehicleInfoModel;
    private JLabel wishedSpeedLabel;
    private JLabel measuredSpeedLabel;
    private JSlider speedSlider;
    private JLabel laneOffsetLabel;
    private JSlider laneOffsetSlider;
    private JToggleButton emergencyToggleButton;
    private JLabel connectionStatusLabel;
    private JLabel trackIdLabel;
    private JLabel turningStatusLabel;
    private JLabel lowBatteryLabel;
    private JProgressBar batteryProgressBar;

    public View(SteeringModel steeringModel, VehicleInfoModel vehicleInfoModel){
        super("Hyperdrive");
        this.steeringModel = steeringModel;
        this.vehicleInfoModel = vehicleInfoModel;
        this.steeringModel.addObserver(this);
        this.vehicleInfoModel.addObserver(this);

        initializeComponents();
    }
    public void update(Observable o, Object arg) {
        repaint();
    }

    public void initializeComponents(){
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new GridLayout(2, 1));
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e){
            e.printStackTrace();
        }

        JPanel steeringPanel = new JPanel();
        JPanel infoPanel = new JPanel();
        this.add(steeringPanel);
        this.add(infoPanel);

        // ===== STEERING PANEL =====
        steeringPanel.setLayout(new GridLayout(1,3));
        JPanel scrollBarsPanel = new JPanel();
        JPanel emergencyPanel = new JPanel();
        emergencyPanel.setPreferredSize(new Dimension(200,300));
        JPanel lightsPanel = new JPanel();
        steeringPanel.add(scrollBarsPanel);
        steeringPanel.add(emergencyPanel);
        steeringPanel.add(lightsPanel);

        // -- Scroll bars panel --
        JPanel speedPanel = new JPanel();
        JPanel laneOffsetPanel = new JPanel();
        scrollBarsPanel.setLayout(new GridLayout(2,1));
        scrollBarsPanel.add(speedPanel);
        scrollBarsPanel.add(laneOffsetPanel);

        // Speed control
        speedPanel.setLayout(new GridLayout(3,1));
        int wishedSpeed = steeringModel.getWishedSpeed();
        this.wishedSpeedLabel = new JLabel();
        this.wishedSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateWishedSpeedLabel(wishedSpeed);
        this.measuredSpeedLabel = new JLabel();
        this.measuredSpeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateMeasuredSpeedLabel(vehicleInfoModel.getMeasuredSpeed());
        this.speedSlider = new JSlider(JSlider.HORIZONTAL, wishedSpeed);
        speedSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int speedValue = speedSlider.getValue();
                steeringModel.setWishedSpeed(speedValue);
                updateWishedSpeedLabel(speedValue);
            }
        });
        speedPanel.add(wishedSpeedLabel);
        speedPanel.add(measuredSpeedLabel);
        speedPanel.add(speedSlider);

        // Lane offset control
        laneOffsetPanel.setLayout(new GridLayout(2,1));
        int wishedLaneOffset = steeringModel.getWishedLaneOffset();
        this.laneOffsetLabel = new JLabel("Wished lane offset: "+wishedLaneOffset);
        this.laneOffsetLabel.setHorizontalAlignment(JLabel.CENTER);
        this.laneOffsetSlider = new JSlider(JSlider.HORIZONTAL, wishedLaneOffset);
        laneOffsetSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int laneOffsetSliderValue = laneOffsetSlider.getValue();
                steeringModel.setWishedLaneOffset(laneOffsetSliderValue);
                updateLaneOffsetLabel(laneOffsetSliderValue);
            }
        });
        laneOffsetPanel.add(laneOffsetLabel);
        laneOffsetPanel.add(laneOffsetSlider);
        setMinMaxSpeedLaneOffset();

        // Emergency control and connection status
        this.emergencyToggleButton = new JToggleButton("Activate emergency");
        this.emergencyToggleButton.setPreferredSize(new Dimension(180, 50));
        this.emergencyToggleButton.setHorizontalAlignment(JToggleButton.CENTER);
        emergencyToggleButton.addActionListener(e -> {
            boolean emergency = emergencyToggleButton.isSelected();
            String text = emergency ? "Deactivate emergency": "Activate emergency";
            this.emergencyToggleButton.setText(text);
            this.steeringModel.setEmergency(emergencyToggleButton.isSelected());
        });
        JLabel emptyLabel = new JLabel();
        emptyLabel.setPreferredSize(new Dimension(180, 50));
        this.connectionStatusLabel = new JLabel("Connection status: UNKNOWN", JLabel.CENTER);
        this.connectionStatusLabel.setOpaque(true);
        this.connectionStatusLabel.setBackground(Color.GRAY);
        this.connectionStatusLabel.setForeground(Color.BLACK);
        this.connectionStatusLabel.setPreferredSize(new Dimension(180, 50));
        emergencyPanel.add(emergencyToggleButton);
        emergencyPanel.add(emptyLabel);
        emergencyPanel.add(connectionStatusLabel);

        // Lights control
        lightsPanel.setLayout(new GridLayout(0,1));

        JPanel frontLightsPanel = new JPanel(new FlowLayout());
        frontLightsPanel.add(new JLabel("Front lights"));
        ButtonGroup frontLightsButtonGroup = new ButtonGroup();
        JRadioButton frontLightsOnButton = new JRadioButton("On");
        JRadioButton frontLightsOffButton = new JRadioButton("Off");
        frontLightsOffButton.setSelected(true);
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
        backLightsOffButton.setSelected(true);
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


        // ====== VEHICLE INFO PANEL =======
        infoPanel.setLayout(new GridLayout(1,2));
        infoPanel.setMaximumSize(new Dimension(1000,200));

        // --- Track Info ---
        JPanel trackPanel = new JPanel();
        trackPanel.setLayout(new GridLayout(0,1));
        this.trackIdLabel = new JLabel();
        this.trackIdLabel.setHorizontalAlignment(JLabel.CENTER);
        updateTrackIdLabel(this.vehicleInfoModel.getCurrentTrackId());
        this.turningStatusLabel = new JLabel();
        this.turningStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        updateTurningStatusLabel(this.vehicleInfoModel.getTurningStatus());
        trackPanel.add(trackIdLabel);
        trackPanel.add(turningStatusLabel);
        infoPanel.add(trackPanel);

        // --- Battery Info ---
        JPanel batteryPanel = new JPanel();
        batteryPanel.setLayout(new GridLayout(3,1));
        JLabel batteryLevelLabel = new JLabel();
        batteryLevelLabel.setHorizontalAlignment(JLabel.CENTER);
        batteryLevelLabel.setText("Battery level");
        this.lowBatteryLabel = new JLabel();
        this.lowBatteryLabel.setHorizontalAlignment(JLabel.CENTER);
        this.lowBatteryLabel.setText("!! LOW BATTERY -> Reduced speed !!");
        this.lowBatteryLabel.setVisible(false);
        this.batteryProgressBar = new JProgressBar();
        this.batteryProgressBar.setMinimum(0);
        this.batteryProgressBar.setMaximum(100);
        this.batteryProgressBar.setStringPainted(true);
        this.batteryProgressBar.setString("Unknown");
        this.batteryProgressBar.setValue(0);
        this.batteryProgressBar.setPreferredSize(new Dimension(300,30));
        this.batteryProgressBar.setMaximumSize(new Dimension(500,40));
        batteryPanel.add(batteryLevelLabel);
        batteryPanel.add(batteryProgressBar);
        batteryPanel.add(lowBatteryLabel);
        infoPanel.add(batteryPanel);
        pack();
        setVisible(true);
    }

    public void updateWishedSpeedLabel(int wishedSpeed) {
        wishedSpeedLabel.setText("Wished speed: " + wishedSpeed);
    }

    public void updateMeasuredSpeedLabel(int measuredSpeed) {
        if (measuredSpeed == 10000){   // Initialization, before any measure is possible
            measuredSpeedLabel.setText("Measured speed: UNKNOWN");
        } else {
            measuredSpeedLabel.setText("Measured speed: " + measuredSpeed);
        }
    }

    public void updateLaneOffsetLabel(int laneOffsetValue){
        laneOffsetLabel.setText("Wished lane offset: " + laneOffsetValue);
    }

    public void updateConnectionStatus(String connectionStatus){
        Color bgColor = Color.GRAY;
        switch (connectionStatus){
            case "ready":
            case "connected":
                bgColor = Color.GREEN;
                break;
            case "connecting":
                bgColor = Color.BLUE;
                break;
            case "lost":
            case "disconnecting":
            case "disconnected":
                bgColor = Color.RED;
                break;
        }
        this.connectionStatusLabel.setBackground(bgColor);
        this.connectionStatusLabel.setText("Connection status: " + connectionStatus);
    }

    public void updateTrackIdLabel(int trackId){
        if (trackId == 10000){
            trackIdLabel.setText("Current track ID: UNKNOWN");
        } else {
            trackIdLabel.setText("Current track ID: " + trackId);
        }
    }

    public void updateTurningStatusLabel(boolean turningStatus){
        turningStatusLabel.setText("Turning track: " + turningStatus);
    }

    public void updateBatteryLevelLabel(int batteryLevel){
        if (batteryLevel==10000){
            lowBatteryLabel.setVisible(false);
            this.batteryProgressBar.setValue(0);
            this.batteryProgressBar.setString("UNKNOWN");
        } else {
            lowBatteryLabel.setVisible(this.vehicleInfoModel.getLowBatteryStatus());
            this.batteryProgressBar.setValue(batteryLevel);
            this.batteryProgressBar.setString(batteryLevel + "%");
        }
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
