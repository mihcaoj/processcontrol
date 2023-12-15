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

  **GUI Interface:**
   - Toggle the emergency flag using the "Toggle Emergency Flag" button.
   - Adjust velocity and acceleration using the sliders.

**Threads:**

- **_Emergency Thread (emergency_thread):_**
        Responsible for running the emergency_stop_process() function, which continuously checks the emergency_flag and takes appropriate actions if an emergency is detected.
        Handles emergency situations, such as stopping the vehicle and blinking lights.

- **_Tkinter Thread (tkinter_thread):_**
        Responsible for running the Tkinter GUI using the run_tkinter() function.
        Manages the GUI window and provides an interface for user interactions.

- **_Main Thread:_**
        The main thread starts the MQTT client loop (client.loop_start()).
        Handles the connection to the MQTT broker, subscription to topics, and initial payload publication.
        Manages the cleanup process when the script exits (cleanup() function is registered with atexit).
  

***Clean Up:***
   - The script will automatically disconnect from the MQTT broker and stop the threads when it exits.

***Known Issues:***


## For the Java program:
