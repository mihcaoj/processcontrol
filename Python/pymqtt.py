import paho.mqtt.client as mqtt
import time
import json
import threading
import tkinter as tk
import atexit
import re
import sys

ip_address = '192.168.4.1'  # ip address of the hyperdrive
port = 1883  # port for MQTT
client = mqtt.Client('hyperdrive') # mqtt client object (instance of paho mqtt class)
availableVehicles = [] # list of available vehicles
connectedVehicles = [] # list of connected vehicles
vehicleID = 'd205effe02cb'  # individual vehicle that we may want to use
vhButtons = {} # buttons for the vehicles on the UI

# connect flag
wantToConnect = True

# Topics
group_intent_topic = "/I/groupD"  # group intent topic for the vehicle
intent_topic = "Anki/Hosts/U/hyperdrive/I/"  # intent topic for the host

# emergency
emergency_topic = "Anki/Emergency/U"  # path for emergency topic
emergency_flag = False  # setting initial flag value to False

# Declare velocity_slider and acceleration_slider as global variables
velocity_slider = None
acceleration_slider = None
sliders_updated = False

# dictionnaries to store each connected vehicle's name associated with
# its battery, track status ... and label for the UI
battery_labels = {}
battery_level = {}

track_label = None
current_track_id = {}

LOW_BATTERY_THRESHOLD = 20

turning_track_label = None
is_turning_track = {}

# thread for the UI
tkinter_thread = None

# flag to take away blinking when disconnecting from the emergency topic
blinking_flag = True

# Payloads for the lights
payload_on = {
    "type": "lights",
    "payload": {
        "back": "on",
        "front": "on"
    }
}

payload_off = {
    "type": "lights",
    "payload": {
        "back": "off",
        "front": "off"
    }
}

#  ----------------------------- BROKER CONNECTION --------------------------------

# CONNECT the client to the MQTT broker
def connect_broker():
    global wantToConnect
    wantToConnect = True
    client.connect(ip_address, port=port)
    print(f"Trying to connect to broker {ip_address}:{port}")

# ON CONNECT - Callback function called when the client connects to the broker
def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")
    
    # Check if the connection result code is 0, indicating a successful connection
    if rc == 0:
        print(f"Succesfully connected to broker at {ip_address}:{port}")
        # make the topic discoverable
        payload_discover = {"type": "discover", "payload": {"value": True}}
        publish(client, intent_topic, payload_discover)
        print("Made the host discoverable")
    else:
        print(f"Connection to broker {ip_address}:{port} failed")
        print("Attempting reconnection to broker: ", ip_address, ":", port)
        try:
            connect_broker()
        except Exception as e:
            on_connect(client, userdata, connectflags, 1)

#make sure on_connect is invoked automatically
client.on_connect = on_connect

# DISCONNECT from the broker
def disconnect_broker():
    client.disconnect()
    global wantToConnect
    wantToConnect = False
    print(f"Disconnecting from broker ... {ip_address}:{port}")

# handle disconnection again if it fails
def on_disconnect(client, userdata, rc):
    global wantToConnect
    if wantToConnect == True:
        print("Attempting reconnection to broker: ",ip_address,":",port)
        try:
            connect_broker()
        except Exception as e:
            global connectflags
            on_connect(client, userdata, connectflags, 1)
    else:
        if rc == 0:
            print(f"Disconnected from broker")
        else:
            print("Disconnection failed")
            if (wantToConnect == False):
                print("Attempting to disconnect from broker: ", ip_address, ":", port)
                try:
                    connect_broker()
                except Exception as e:
                    on_disconnect(client, userdata, 1)

#make sure on_disconnect is invoked automatically
client.on_disconnect = on_disconnect

#  ----------------------------- VEHICLE CONNECTION, SUBSCRIBE, MESSAGES --------------------------------

# HANDLING INCOMING MESSAGES
def on_message(client, userdata, msg):
    
    print(f"Received {msg.payload} from {msg.topic}")

    # Parse the JSON payload from the message
    payload = json.loads(msg.payload.decode("utf-8"))

    global emergency_flag, battery_level, current_track_id, current_direction
    batteryPattern = r"Anki/Vehicles/U/(.*?)/S/battery"
    trackPattern = r"Anki/Vehicles/U/(.*?)/E/track"

    # fetch the avaialable vehicles and store them
    if (msg.topic == "Anki/Hosts/U/hyperdrive/S/vehicles"):
            global availableVehicles
            availableVehicles = payload['value']
     # Check the topic of the received message
    if msg.topic == emergency_topic:
        # Update the emergency flag based on the received payload
        emergency_flag = payload.get("value", False)
        print(f"Emergency flag is set to {emergency_flag}")
    elif "battery" in msg.topic:
        # Update battery level based on the received payload
        battery_value = payload.get("value", 0)
        match = re.search(batteryPattern, msg.topic)
        vhID = match.group(1)
        if vhID:
            print(f"Battery of {vhID} is {battery_value}")
            battery_level[vhID] = battery_value

        # Show a low battery popup if the battery level is below the threshold
        if battery_value < LOW_BATTERY_THRESHOLD:
            show_low_battery_popup()
    
    elif "track" in msg.topic:
        # Update current track information based on the received payload
        track_id = payload.get("trackId", None)
        match = re.search(trackPattern, msg.topic)
        vhID = match.group(1)
        if (track_id is not None) and (vhID): 
            print(f"Track ID of vehicle {vhID} is {track_id}")
            current_track_id[vhID] = track_id
        elif(track_id is None) and (vhID):
            print("Received track information, but track_id is None.")
            current_track_id[vhID] = "None"

#SUBSCRIBE to the list of available vehicles
def subscribe_general(client: mqtt.Client):
    client.subscribe("Anki/Hosts/U/hyperdrive/S/vehicles") 
    client.on_message = on_message

#SUBSCRIBE to the connected vehicle's information topics
def subscribe_vehicle(client: mqtt.Client, vehicleID):
    topic = "Anki/Vehicles/U/" + vehicleID + "S/status"
    client.subscribe(topic)  # subscribe to the status of the vehicle
    client.subscribe(f"Anki/Vehicles/U/{vehicleID}/S/battery")  # subscribe to the battery topic
    client.subscribe(f"Anki/Vehicles/U/{vehicleID}/E/track")  # subscribe to the track topic
    client.on_message = on_message
    
def unsubscribe_vehicle(client: mqtt.Client, vehicleID):
    topic = "Anki/Vehicles/U/" + vehicleID + "S/status"
    client.unsubscribe(topic)  # unsubscribe from the status of the vehicle
    client.unsubscribe(f"Anki/Vehicles/U/{vehicleID}/S/battery")  # unsubscribe from the battery topic
    client.unsubscribe(f"Anki/Vehicles/U/{vehicleID}/E/track")  # unsubscribe from the track topic

#PUBLISH
def publish(client: mqtt.Client, topic: str, payload: dict):
    try:
        message = json.dumps(payload)
        client.publish(topic, message)
        print(f"Published to topic {topic} with {payload}")
    except Exception as e:
        print(f"Error publishing to topic {topic}: {e}")

# CONNECT to the desired vehicle
def connect_vehicle(vehicle_id, button):
    topic = "Anki/Vehicles/U/" + vehicle_id + "/I/"
    connect_payload = {
        "type": "connect",
        "payload": {
            "value": True
        }
    }
    publish(client, topic, connect_payload)
    print(f"Connecting to vehicle: {vehicle_id}")

    # update status
    statusTopic = "Anki/Vehicles/U/"+vehicle_id+"/S/status"
    publish(client, statusTopic, {"value": "connected"})
    
    # subscribe to information topics of the connected vehicles
    subscribe_vehicle(client, vehicle_id)
    
    # add vehicle to the list of connected vehicles
    connectedVehicles.append(vehicle_id)
    print("CONNECTED VEHICLES:", connectedVehicles)

    if vhButtons[vehicle_id] is not None:
         #bind the connect button on the gui to the disconnect funcion and change its color
        vhButtons[vehicle_id].configure(bg="lightgreen")
        vhButtons[vehicle_id].configure(command=lambda id=vehicle_id, btn=vhButtons[vehicle_id]: disconnect_vehicle(id, btn))


# DISCONNET from the vehicle
def disconnect_vehicle(vehicle_id, button):
    topic = "Anki/Vehicles/U/" + vehicle_id + "/I/"
    connect_payload = {
        "type": "connect",
        "payload": {
            "value": False
        }
    }
    publish(client, topic, connect_payload)
    print(f"Disconnecting from vehicle: {vehicle_id}")

    # unubscribe from information topics of the connected vehicles
    unsubscribe_vehicle(client, vehicle_id)

    # remove vehicle from the list of connected vehicles
    connectedVehicles.remove(vehicle_id)

    if vhButtons[vehicle_id] is not None:
        # bind the connect button on the gui to the connect function and change its color
        vhButtons[vehicle_id].configure(bg="red")
        vhButtons[vehicle_id].configure(command=lambda id=vehicle_id, btn=vhButtons[vehicle_id]: connect_vehicle(id, btn))

# BATTERY POPUP when it is too low
def show_low_battery_popup():
    low_battery_popup = tk.Toplevel()
    low_battery_popup.title("Low Battery Warning")

    label = tk.Label(low_battery_popup, text="Warning: Low Battery!")
    label.pack(padx=10, pady=10)

    ok_button = tk.Button(low_battery_popup, text="OK", command=low_battery_popup.destroy)
    ok_button.pack(pady=10)

# --------------------------------------------- LIGHTS ------------------------------------------------------

def blink_lights(vehicleID):
    global blinking_flag, emergency_flag
    try:
        print(f"Start the blink on vehicle: {vehicleID}")

        while blinking_flag and emergency_flag:  # Use the global flag to control the loop
            # Publish the json payload to turn on the lights
            publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_on)
            print(f"Published: {payload_on} to {vehicleID}")
            time.sleep(1)

            # Publish the json payload to turn off the lights
            publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_off)
            print(f"Published: {payload_off} to {vehicleID}")
            time.sleep(1)

    except KeyboardInterrupt:
        print("Blinking interrupted. Stopping the lights.")
    finally:
        client.disconnect()

# turn the front lights on
def frontLightsOn():
    payload = {
        "type": "lights",
        "payload": {
            "front": "on"
        }
    }
    for vehicleID in connectedVehicles:
        try:
            print(f"Turning on the front lights on vehicle: {vehicleID}")

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+group_intent_topic,
                    payload)  # publish the json payload to turn on the lights
            print(f"Published: {payload} to {vehicleID}")

        except Exception as e:
            print(f"ERROR:{e}")
            print("Error turning the front lights on")

# turn the front lights off
def frontLightsOff():
    payload = {
        "type": "lights",
        "payload": {
            "front": "off"
        }
    }
    for vehicleID in connectedVehicles:
        try:
            print(f"Turning on the front lights on vehicle: {vehicleID}")

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+group_intent_topic,
                    payload)  # publish the json payload to turn on the lights
            print(f"Published: {payload} to {vehicleID}")


        except Exception as e:
            print(f"ERROR:{e}")
            print(f"Error turning the front lights off on vehicle {vehicleID}")

# turn the back lights on
def backLightsOn():
    payload = {
        "type": "lights",
        "payload": {
            "back": "on"
        }
    }
    for vehicleID in connectedVehicles:
        try:
            print(f"Turning on the front lights on vehicle: {vehicleID}")

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + group_intent_topic,
                    payload)  # publish the json payload to turn on the lights
            print(f"Published: {payload} to {vehicleID}")
            
        except Exception as e:
            print(f"ERROR:{e}")
            print(f"Error turning the back lights on on vehicle {vehicleID}")

# turn the back lights off
def backLightsOff():
    payload = {
        "type": "lights",
        "payload": {
            "back": "off"
        }
    }
    for vehicleID in connectedVehicles:
        try:
            print(f"Turning on the front lights on vehicle: {vehicleID}")

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + group_intent_topic,
                    payload)  # publish the json payload to turn on the lights
            print(f"Published: {payload} to {vehicleID}")
        except Exception as e:
            print(f"ERROR:{e}")
            print(f"Error turning the back lights off on vehicle {vehicleID}")

# turn the all the lights on
def allLightsOn():
    payload = {
        "type": "lights",
        "payload": {
            "back": "on",
            "front":"on"
        }
    }
    for vehicleID in connectedVehicles:
        try:
            print(f"Turning on all the lights on vehicle: {vehicleID}")

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + group_intent_topic,
                    payload)  # publish the json payload to turn on the lights
            print(f"Published: {payload} to {vehicleID}")

        except Exception as e:
            print(f"ERROR:{e}")
            print(f"Error turning all the lights on on vehicle {vehicleID}")

# turn the all the lights off
def allLightsOff():
    payload = {
        "type": "lights",
        "payload": {
            "back": "off",
            "front":"off"
        }
    }
    
    for vehicleID in connectedVehicles:
        try:
            print(f"Turning off all the lights on vehicle: {vehicleID}")

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + group_intent_topic,
                    payload)  # publish the json payload to turn off the lights
            print(f"Published: {payload} to {vehicleID}")

        except Exception as e:
            print(f"ERROR:{e}")
            print(f"Error turning all the lights off on vehicle {vehicleID}")




# -------------------------------------------- DRIVE COMMANDS  --------------------------------------------

# SLIDE TO THE LANE ON THE RIGHT
def change_lane_right(vehicleID):
    print("Changing to the right lane")

    if not emergency_flag and not sliders_updated:
        offset = 1000
        velocity = 250
        acceleration = 250
    else:
        offset = 0
        velocity = 0
        acceleration = 0

    payload_lane = {
        "type": "lane",
        "payload": {
            "offset": offset,
            "velocity": velocity,
            "acceleration": acceleration
        }
    }
    for vehicleID in connectedVehicles:
        publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_lane)

# SLIDE TO THE LANE ON THE LEFT
def change_lane_left(vehicleID):
    print("Changing to the left lane")

    if not emergency_flag and not sliders_updated:
        offset = -1000
        velocity = 250
        acceleration = 250
    else:
        # If emergency_flag is True, set offset, velocity and acceleration to stop lane change
        offset = 0
        velocity = 0
        acceleration = 0

    payload_lane = {
        "type": "lane",
        "payload": {
            "offset": offset,
            "velocity": velocity,
            "acceleration": acceleration
        }
    }
    for vehicleID in connectedVehicles:
        publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_lane)

# --------------------------------------- EMERGENCY COMMANDS ----------------------------------------

# STOP THE VEHICLE ENTIRELY
def stop_vehicle(vehicleID):
    print(f"Stopping vehicle: {vehicleID}")
    # If emergency_flag is True, set velocity and acceleration to stop the car
    velocity = 0
    acceleration = 0

    payload_speed = {
        "type": "speed", "payload": {"velocity": velocity, "acceleration": acceleration}}

    publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_speed)
    pause_drive_event.set()
    pause_lane_event.set()

# CHANGE THE STATUS OF THE EMERGENCY FLAG
def change_flag_status():
    global emergency_flag
    emergency_flag = not emergency_flag
    print(f"Emergency flag status changed to: {emergency_flag}")

# STOP ALL THE CARS AND MAKE THEM BLINK IF EMERGENCY
def emergency_stop_process():

    client_emergency = mqtt.Client('emergency_stop_process')
    client_emergency.on_connect = on_connect
    client_emergency.on_message = on_message

    print(f"Connecting to broker at {ip_address}:{port} for emergency stop process")
    client_emergency.connect(ip_address, port=port)

    client_emergency.loop_start()

    try:
        last_print_time = time.time()
        while True:
            time.sleep(1)  # check the emergency flag every second
            if emergency_flag:
                for vehicle in connectedVehicles:
                    stop_vehicle(vehicle)
                    blink_lights(vehicle)
            else:
                current_time = time.time()
                if current_time - last_print_time >= 5:
                    print("Emergency flag status: False.")
                    last_print_time = current_time

    except KeyboardInterrupt:
        print("Emergency stop process interrupted.")
    finally:
        client_emergency.disconnect()


def update_velocity_slider(value):
    global sliders_updated
    if velocity_slider:

        sliders_updated = True

        # Send the updated velocity value to the broker
        payload_velocity = {
            "type": "speed",
            "payload": {
                "velocity": float(value)
            }
        }
        for vehicleID in connectedVehicles:
            publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_velocity)
        print(f"Velocity updated to: {value}")


def update_acceleration_slider(value):
    global sliders_updated
    if acceleration_slider:
        sliders_updated = True

        # Send the updated acceleration value to the broker
        payload_acceleration = {
            "type": "speed",
            "payload": {
                "acceleration": float(value)
            }
        }
        for vehicleID in connectedVehicles:
            publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_acceleration)
        print(f"Acceleration updated to: {value}")

def sliders_released():
    global sliders_updated
    sliders_updated = False

def update_turning_track_label():
    global ibattery_labels_turning_track
    # Update the turning track label
    turning_track_label.config(text=f"Turning Track: {is_turning_track}")
    # Schedule the next update after 1s
    turning_track_label.after(500, update_turning_track_label)

    turning_tracks = [1, 2, 3, 4, 5, 6, 9, 12, 13, 14, 15, 16, 18, 19]

    for key, value in current_track_id.items():
        if value in turning_tracks:
            print(f"{key} is on a turning track")
            is_turning_track[key] = True
        else: 
            is_turning_track[key] = False

# ----------------------------------- GRAPHICAL USER INTERFACE ----------------------

def run_tkinter():

    def create_tkinter_window():
        global velocity_slider, acceleration_slider, battery_labels, track_label, turning_track_label, is_turning_track
        # Create the main application window
        app = tk.Toplevel()
        app.title("Anki Vehicle Control GUI")

        # Create a StringVar to store console output
        console_var1 = tk.StringVar()
        console_var2 = tk.StringVar()
        console_var3 = tk.StringVar()

        # battery name label
        connectName = tk.Label(app, text=f"Click on the vehicle name to connect or disconnect")
        connectName.pack(pady=5)

        # Display vehicle IDs as buttons to connect to
        for vehicle_id in availableVehicles:
            print(f"VEHICLE:{vehicle_id}")
            vhButtons[vehicle_id] = tk.Button(app, text=vehicle_id, command=lambda id=vehicle_id, btn=None: connect_vehicle(id, vhButtons[vehicle_id]), bg="red")
            vhButtons[vehicle_id].pack()


        # Create a button to toggle the emergency flag
        button = tk.Button(app, text="Toggle Emergency Flag", command=change_flag_status)
        button.pack(pady=10)

        # Slider for changing the velocity
        velocity_label = tk.Label(app, text="Velocity")
        velocity_label.pack()
        velocity_slider = tk.Scale(app, from_=-100, to=2000,
                                   orient=tk.HORIZONTAL,
                                   command=update_velocity_slider)
        velocity_slider.pack()
        velocity_slider.bind("<ButtonRelease-1>", lambda event: sliders_released())

        # Slider for changing the acceleration
        acceleration_label = tk.Label(app, text="Acceleration:")
        acceleration_label.pack()
        acceleration_slider = tk.Scale(app, from_=0, to=2000,
                                       orient=tk.HORIZONTAL,
                                       command=update_acceleration_slider)

        acceleration_slider.pack()
        acceleration_slider.bind("<ButtonRelease-1>", lambda event: sliders_released())

        # Buttons for changing lanes
        change_lane_frame = tk.Frame(app)
        change_lane_frame.pack(side=tk.TOP, pady=20)

        button_left = tk.Button(change_lane_frame, text="Change Lane Left", command=change_lane_left)
        button_left.pack(side=tk.LEFT, padx=5)

        button_right = tk.Button(change_lane_frame, text="Change Lane Right", command=change_lane_right)
        button_right.pack(side=tk.RIGHT, padx=5)

        # Frame for light buttons
        lights_frame = tk.Frame(app)
        lights_frame.pack(side=tk.TOP, pady=20)

        # ----------- LIGHTS ---------------------------

        # Button for turning the lights off
        button_off1 = tk.Button(lights_frame, text="ALl lights Off",
                               command=lambda: allLightsOff())
        button_off1.pack(side=tk.LEFT, padx=5)

        # Button for turning the lights on
        button_on1 = tk.Button(lights_frame, text="ALl lights On",
                              command=lambda: allLightsOn())
        button_on1.pack(side=tk.RIGHT, padx=5)

        # Button for turning front lights off
        button_off2 = tk.Button(lights_frame, text="Front lights Off",
                               command=lambda: frontLightsOff())
        button_off2.pack(side=tk.LEFT, padx=5)

        # Button for turning front lights on
        button_on2 = tk.Button(lights_frame, text="Front lights On",
                              command=lambda: frontLightsOn())
        button_on2.pack(side=tk.LEFT, padx=5)

        # Button for turning back lights off
        button_off3 = tk.Button(lights_frame, text="Back lights Off",
                               command=lambda: backLightsOff())  
        button_off3.pack(side=tk.LEFT, padx=5)

        # Button for turning back lights on
        button_on3 = tk.Button(lights_frame, text="Back lights On",
                              command=lambda: backLightsOn()) 
        button_on3.pack(side=tk.RIGHT, padx=5)

        # ----------------- BATTERY -----------
        
        # battery name label
        batteryName = tk.Label(app, text=f"Battery Levels")
        batteryName.pack(pady=5)

        # Create a text widget to display the console output
        console_text1 = tk.Text(app, wrap=tk.WORD, height=7, width=50, state=tk.DISABLED)
        console_text1.pack(padx=10, pady=5)

        # tracks name label
        trackName = tk.Label(app, text=f"Track")
        trackName.pack(pady=5)
        # Create a text widget to display the console output
        console_text2 = tk.Text(app, wrap=tk.WORD, height=5, width=50, state=tk.DISABLED)
        console_text2.pack(padx=10, pady=5)

        # tracks name label
        ttrackName = tk.Label(app, text=f"Turning tracks")
        ttrackName.pack(pady=5)
        # Create a text widget to display the console output
        console_text3 = tk.Text(app, wrap=tk.WORD, height=5, width=50, state=tk.DISABLED)
        console_text3.pack(padx=10, pady=5)
        

           # function to create a sort of mini terminal in the GUI
        def write_to_console1(message):
            console_var1.set(console_var1.get() + message + '\n')
            console_text1.config(state=tk.NORMAL)
            console_text1.insert(tk.END, message + '\n')
            console_text1.yview(tk.END)  # Scroll to the end of the text widget
        def write_to_console2(message):
            console_var2.set(console_var2.get() + message + '\n')
            console_text2.config(state=tk.NORMAL)
            console_text2.insert(tk.END, message + '\n')
            console_text2.yview(tk.END)  # Scroll to the end of the text widget
        def write_to_console3(message):
            console_var3.set(console_var3.get() + message + '\n')
            console_text3.config(state=tk.NORMAL)
            console_text3.insert(tk.END, message + '\n')
            console_text3.yview(tk.END)  # Scroll to the end of the text widget
        
        # get the battery levels function
        def get_battery_lvls():
            for key, value in battery_level.items():
                write_to_console1(f"Battery Level of {key}: {value}%")
            app.after(2000, get_battery_lvls)

        app.after(500, get_battery_lvls)
        # Label for displaying battery level
        for key, value in battery_level.items():
            battery_labels[key] = tk.Label(app, text=f"Battery Level of {key}: {value}%")
            battery_labels[key].pack(pady=10)

        # Label for current track
        def get_tracks():
            if current_track_id:
                for key, value in current_track_id.items():
                    write_to_console2(f"Track of {key}: {value}%")
            else:
                write_to_console2(f"No vehicle on tracks detected")
            app.after(2000, get_tracks)
        
        app.after(500, get_tracks)

        def get_turning_tracks():
            if is_turning_track:
                for key, value in is_turning_track.items():
                    write_to_console3(f"Is {key} on a turning track? {value}%")
            else:
                write_to_console3("No vehicles on tracks detected")
            app.after(500, get_turning_tracks)
        

        app.after(500, get_turning_tracks)

        update_turning_track_label()

        
        app.mainloop()  # Run the Tkinter event loop
    
    def start_tkinter():
        app_ref = tk.Tk()
        app_ref.withdraw()
        app_ref.after(0, create_tkinter_window)
        app_ref.mainloop()

    # Run the Tkinter window in a separate thread
    tkinter_thread = threading.Thread(target=start_tkinter())
    tkinter_thread.start()

    # Keep a reference to the app so that it doesn't get garbage collected
    return tkinter_thread



# event objects to control the pause/resume of threads
pause_drive_event = threading.Event()
pause_lane_event = threading.Event()
# creation of the different threads
emergency_thread = threading.Thread(target=emergency_stop_process)
emergency_thread.start()

def cleanup(): ### NOT TESTED YET ###
    client.disconnect()
    pause_drive_event.set()
    pause_lane_event.set()
    emergency_thread.join()
    tkinter_thread.join()

atexit.register(cleanup) # call cleanup when the script exits ### NOT TESTED YET ### 

# ---------------------------- RUN THE SCRIPT --------------------------
connect_broker()

client.loop_start()  # start the loop
subscribe_general(client)  # subscribe to the client
time.sleep(2)


run_tkinter()
get_battery_lvls()

client.loop_stop() # stop the MQTT client loop when your script exits