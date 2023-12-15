# **Overview**

This project utilizes the MQTT protocol for communication with the Anki Hyperdrive vehicle and provides a graphical user interface (GUI) that allows users to interact with a remote-controlled vehicle.

# **Features**

-  Choose which vehicle you want to connect to
-  **_Emergency Flag Control:_** Toggle the emergency flag to enable or disable emergency stops.
-  **_Velocity and Acceleration Control:_** Adjust the velocity and acceleration of the vehicle using sliders in the GUI.
-  **_Lane Change:_** Change lanes to the left or right, with options to modify offset, velocity, and acceleration.
-  **_Lights Control:_** Turn the vehicle lights on or off.

TODO: ADD REST OF THE FEATURES

# Dependencies & Usage

## For the Python script:
    
  **Install Dependencies:**
    ```
    pip install paho-mqtt ttkbootstrap
    ```

  **Run the Script:**
    ```
    python3 pymqtt.py
    ```

  **_GUI Interface:_**
   - Toggle the emergency flag using the "Toggle Emergency Flag" button.
   - Adjust velocity and acceleration using the sliders.

  ***_Clean Up:_***
   - The script will automatically disconnect from the MQTT broker and stop the threads when it exits.

  ***_Known Issues:_***


## For the Java program:
