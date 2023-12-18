import paho.mqtt.client as mqtt
import time
import json
import threading
import tkinter as tk
from tkinter import ttk
from ttkbootstrap import Style
import atexit

# TODO:
#        - disconnect / cleanup (make sure it works)
#        - add threads
#        - improve gui
#        - change lane right and left values need to be modified to work
#        - fix the slider for acceleration and velocity (values in between still seem to be published)

ip_address = '192.168.4.1'  # ip address of the hyperdrive
port = 1883  # port for MQTT
client = mqtt.Client('hyperdrive') # create an instance of the MQTT client with hyperdrive as the client id
vehicleID = 'cec233dec1cb'  # change according to the vehicle ID

# Topics
group_intent_topic = "/I/groupD"  # group intent topic for the vehicle
intent_topic = "Anki/Hosts/U/hyperdrive/I/"  # intent topic for the host
status_topic = f"Anki/Vehicles/U/{vehicleID}/S/status"  # status topic for the vehicle
battery_topic = f"Anki/Vehicles/U/{vehicleID}/S/battery"  # battery topic for the vehicle
track_topic = f"Anki/Vehicles/U/{vehicleID}/E/track"  # track topic for the vehicle
emergency_topic = "Anki/Emergency/U"  # path for emergency topic

emergency_flag = False  # setting initial flag value to False

# Declare velocity_slider, acceleration_slider and sliders_updated as global variables
velocity_slider = None
acceleration_slider = None
sliders_updated = False

# Labels for battery, tracks and direction
battery_label = None
battery_level = 0

track_label = None
current_track_id = 0

direction_label = None
current_direction = 0

turning_track_label = None
is_turning_track = False

LOW_BATTERY_THRESHOLD = 20 # threshold at which the low battery popup will appear

tkinter_thread = None

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


def on_connect(client, userdata, flags, rc):
    # Callback function called when the client successfully connects to the broker
    print(f"Connected with result code {rc}")

    payload_discover = {"type": "discover", "payload": {"value": True}}

    # Publish the discover payload to the specified topic
    publish(client, intent_topic, payload_discover)
    print("Published the discover payload successfully")

    # Check if the connection result code is 0, indicating a successful connection
    if rc == 0:
        print(f"Connected to broker at {ip_address}:{port}")

        # Publish a status message indicating that the vehicle is connected
        publish(client, status_topic, {"value": "connected"})
        print("Published the connecting payload successfully")


def on_message(client, userdata, msg):
    # Callback function called when a message is received from the broker
    print(f"Received {msg.payload} from {msg.topic}")

    # Parse the JSON payload from the message
    payload = json.loads(msg.payload.decode("utf-8"))

    global emergency_flag, battery_level, current_track_id, current_direction

     # Check the topic of the received message
    if msg.topic == emergency_topic:
        # Update the emergency flag based on the received payload
        emergency_flag = payload.get("value", False)
        print(f"Emergency flag is set to {emergency_flag}")
    elif msg.topic == battery_topic:
        # Update battery level based on the received payload
        battery_value = payload.get("value", 0)
        print(f"Battery value is {battery_value}")
        battery_level = battery_value

        # Show a low battery popup if the battery level is below the threshold
        if battery_value < LOW_BATTERY_THRESHOLD:
            show_low_battery_popup()
    elif msg.topic == track_topic:
        # Update current track information based on the received payload
        track_id = payload.get("trackId", None)
        if track_id is not None: 
            print(f"Received track information. Track ID: {track_id}")
            current_track_id = track_id
        else:
            print("Received track information, but track_id is None.")

        # Update current direction information based on the received payload
        direction = payload.get("direction", "unknown")
        print(f"Received track information. Direction: {direction}")
        current_direction = direction


def subscribe(client: mqtt.Client):
    client.subscribe(f"Anki/Vehicles/U/{vehicleID}/S/status")  # subscribe to the status of the vehicle
    client.subscribe(f"Anki/Vehicles/U/{vehicleID}/S/battery")  # subscribe to the battery topic
    client.subscribe(f"Anki/Vehicles/U/{vehicleID}/E/track")  # subscribe to the track topic
    client.on_message = on_message


def publish(client: mqtt.Client, topic: str, payload: dict):
    try:
        message = json.dumps(payload)
        client.publish(topic, message)
        print(f"Published to topic {topic} with {payload}")
    except Exception as e:
        print(f"Error publishing to topic {topic}: {e}")


def show_low_battery_popup():
    low_battery_popup = tk.Toplevel()
    low_battery_popup.title("Low Battery Warning")

    label = tk.Label(low_battery_popup, text="Warning: Low Battery!")
    label.pack(padx=10, pady=10)

    ok_button = tk.Button(low_battery_popup, text="OK", command=low_battery_popup.destroy)
    ok_button.pack(pady=10)


def blink_lights(vehicleID):
    try:
        print(f"Start the blink on vehicle: {vehicleID}")

        while True:
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


def change_lane_right():
    print("Changing to the right lane")

    if not emergency_flag and not sliders_updated:
        offset = 750
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

    publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_lane)


def change_lane_left():
    print("Changing to the left lane")

    if not emergency_flag and not sliders_updated:
        offset = -750
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

    publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_lane)


def stop_vehicle():
    print(f"Stopping vehicle: {vehicleID}")
    # If emergency_flag is True, set velocity and acceleration to stop the car
    velocity = 0
    acceleration = 0

    payload_speed = {
        "type": "speed", "payload": {"velocity": velocity, "acceleration": acceleration}}

    publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_speed)
    pause_drive_event.set()
    pause_lane_event.set()


def change_flag_status():
    global emergency_flag
    emergency_flag = not emergency_flag # change the status of the flag
    print(f"Emergency flag status changed to: {emergency_flag}")


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
                stop_vehicle()
                blink_lights(vehicleID)
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
        publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_acceleration)
        print(f"Acceleration updated to: {value}")


def sliders_released():
    global sliders_updated
    sliders_updated = False


def update_battery_label():
    # Update the battery label
    battery_label.config(text=f"Battery Level: {battery_level}%")
    # Schedule the next update after 1s
    battery_label.after(1000, update_battery_label)

def update_track_label():
    # Update the track label
    track_label.config(text=f"Current Track: {current_track_id}")
    # Schedule the next update after 1s
    track_label.after(1000, update_track_label)

def update_direction_label():
    # Update the direction label
    direction_label.config(text=f"Direction: {current_direction}")
    # Schedule the next update after 1s
    direction_label.after(1000, update_direction_label)

def update_turning_track_label():
    global is_turning_track
    # Update the turning track label
    turning_track_label.config(text=f"Turning Track: {is_turning_track}")
    # Schedule the next update after 1s
    turning_track_label.after(1000, update_turning_track_label)

    turning_tracks = [1, 2, 3, 4, 5, 6, 9, 12, 13, 14, 15, 16, 18, 19]

    if current_track_id in turning_tracks:
        print(f"{vehicleID} is on a turning track")
        is_turning_track = True
    else: 
        is_turning_track = False

def update_gui():
    update_battery_label()
    update_track_label()
    update_direction_label()
    update_turning_track_label()


def run_tkinter():
    def create_tkinter_window():
        global velocity_slider, acceleration_slider, battery_label, track_label, direction_label, turning_track_label, is_turning_track
        # Main application window
        app = tk.Toplevel()
        app.title("Emergency Flag Controller")

        # Create a ttk style using the bootstrap theme
        style = Style(theme='superhero')

        # Button to toggle the emergency flag
        button = ttk.Button(app, text="Toggle Emergency Flag", command=change_flag_status)
        button.pack(pady=10)

        # Slider for changing the velocity
        velocity_label = ttk.Label(app, text="Velocity")
        velocity_label.pack()
        velocity_slider = ttk.Scale(app, from_=-100, to=2000,
                                   orient=tk.HORIZONTAL,
                                   command=update_velocity_slider)
        velocity_slider.pack()
        velocity_slider.bind("<ButtonRelease-1>", lambda event: sliders_released())

        # Slider for changing the acceleration
        acceleration_label = ttk.Label(app, text="Acceleration:")
        acceleration_label.pack()
        acceleration_slider = ttk.Scale(app, from_=0, to=2000,
                                       orient=tk.HORIZONTAL,
                                       command=update_acceleration_slider)

        acceleration_slider.pack()
        acceleration_slider.bind("<ButtonRelease-1>", lambda event: sliders_released())

        # Buttons for changing lanes
        change_lane_frame = ttk.Frame(app)
        change_lane_frame.pack(side=tk.TOP, pady=20)

        button_left = ttk.Button(change_lane_frame, text="Change Lane Left", command=change_lane_left)
        button_left.pack(side=tk.LEFT, padx=5)

        button_right = ttk.Button(change_lane_frame, text="Change Lane Right", command=change_lane_right)
        button_right.pack(side=tk.RIGHT, padx=5)

        # Frame for light buttons
        lights_frame = ttk.Frame(app)
        lights_frame.pack(side=tk.TOP, pady=20)

        # Button for turning the lights off
        button_off = ttk.Button(lights_frame, text="Lights Off",
                               command=lambda: publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_off))
        button_off.pack(side=tk.LEFT, padx=5)

        # Button for turning the lights on
        button_on = ttk.Button(lights_frame, text="Lights On",
                              command=lambda: publish(client, f"Anki/Vehicles/U/{vehicleID}{group_intent_topic}", payload_on))
        button_on.pack(side=tk.RIGHT, padx=5)

        # Label for displaying battery level
        battery_label = ttk.Label(app, text=f"Battery Level: {battery_level}%")
        battery_label.pack(pady=10)

        # Label for current track
        track_label = ttk.Label(app, text=f"Current Track: {current_track_id}")
        track_label.pack(pady=10)

        # Label for displaying the direction
        direction_label = ttk.Label(app, text=f"Direction: {current_direction}")
        direction_label.pack(pady=10)

        turning_track_label = ttk.Label(app, text=f"Turning Track: {is_turning_track}")
        turning_track_label.pack(pady=10)

        update_gui()

        app.mainloop()  # run the tkinter event loop

    def start_tkinter():
        global tkinter_thread
        app_ref = tk.Tk()
        app_ref.withdraw()
        app_ref.after(0, create_tkinter_window)
        app_ref.mainloop()

    # Run the Tkinter window in a separate thread
    tkinter_thread = threading.Thread(target=start_tkinter())
    tkinter_thread.start()

    # Keep a reference to the app so that it doesn't get garbage collected
    return tkinter_thread

def cleanup(): ### NOT TESTED YET ###
    client.disconnect()
    pause_drive_event.set()
    pause_lane_event.set()
    emergency_thread.join()
    tkinter_thread.join()


atexit.register(cleanup) # call cleanup when the script exits ### NOT TESTED YET ### 

# Publish the initial connect message
payload_connect = {
    "type": "connect",
    "payload": {
        "value": "true"
    }
}

# Print a message indicating the intention to connect to the MQTT broker
print(f"Connecting to broker at {ip_address}:{port}")

# Establish a connection to the MQTT broker
client.connect(ip_address, port=port)

client.loop_start()  # start the loop
subscribe(client)  # subscribe to the client
publish(client, f"Anki/Vehicles/U/{vehicleID}/I/", payload_connect)  # publish the json payload
time.sleep(5)

# Event objects to control the pause/resume of threads
pause_drive_event = threading.Event()
pause_lane_event = threading.Event()

# Creation of the different threads
emergency_thread = threading.Thread(target=emergency_stop_process)
emergency_thread.start()

run_tkinter()

client.loop_stop()  # stop the MQTT client loop when your script exits