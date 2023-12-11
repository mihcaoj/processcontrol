import paho.mqtt.client as mqtt
import time
import json
import threading
import tkinter as tk

# TODO:
#        - disconnect / cleanup
#        - add threads
#        - track the tracks controller (final project part 0.2.3)
#        - improve gui
#        - put everything in a class
#        - add battery level to GUI
#        - change lane right and left values need to be modified to work
#        - fix the slider for acceleration and velocity (values in between still seem to be published)

ip_address = '192.168.4.1'  # ip address of the hyperdrive
port = 1883  # port for MQTT
client = mqtt.Client('hyperdrive')
vehicleID = 'e10a07218a87'  # change according to the vehicle ID

emergency_topic = "Anki/Emergency/U"  # path for emergency topic
emergency_flag = False  # setting initial flag value to False

# Declare velocity_slider and acceleration_slider as global variables
velocity_slider = None
acceleration_slider = None
sliders_updated = False

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
    print(f"Connected with result code {rc}")

    payload_discover = {"type": "discover", "payload": {"value": True}}

    publish(client, "Anki/Hosts/U/hyperdrive/I/", payload_discover)
    print("Published the discover payload successfully")

    if rc == 0:
        print(f"Connected to broker at {ip_address}:{port}")
        publish(client, "Anki/Vehicles/U/" + vehicleID + "/S/status", {"value": "connected"})
        print("Published the connecting payload successfully")


def on_message(client, userdata, msg):
    print(f"Received {msg.payload} from {msg.topic}")
    payload = json.loads(msg.payload.decode("utf-8"))

    global emergency_flag
    if msg.topic == emergency_topic:
        emergency_flag = payload.get("value", False)
        print(f"Emergency flag is set to {emergency_flag}")


def subscribe(client: mqtt.Client):
    client.subscribe("Anki/Vehicles/U/" + vehicleID + "/S/status")  # subscribe to the status of the vehicle
    client.subscribe("Anki/Vehicles/U/" + vehicleID + "/S/battery")  # subscribe to the battery topic
    client.on_message = on_message


def publish(client: mqtt.Client, topic: str, payload: dict):
    message = json.dumps(payload)
    client.publish(topic, message)
    print(f"Published to topic {topic} with {payload}")


def blink_lights(vehicleID):
    try:
        print(f"Start the blink on vehicle: {vehicleID}")

        while True:
            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb",
                    payload_on)  # publish the json payload to turn on the lights
            print(f"Published: {payload_on} to {vehicleID}")
            time.sleep(1)

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb",
                    payload_off)  # publish the json payload to turn off the lights
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

    publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_lane)


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

    publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_lane)


def stop_vehicle():
    # sets the pause event to pause the drive_car and change_lane threads
    print("Stopping vehicle: " + vehicleID)
    # If emergency_flag is True, set velocity and acceleration to stop the car
    velocity = 0
    acceleration = 0

    payload_speed = {
        "type": "speed", "payload": {"velocity": velocity, "acceleration": acceleration}}

    publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_speed)
    pause_drive_event.set()
    pause_lane_event.set()


def change_flag_status():
    global emergency_flag
    emergency_flag = not emergency_flag
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
        # velocity_slider.set(int(value))

        sliders_updated = True

        # Send updated velocity value to the broker
        payload_velocity = {
            "type": "speed",
            "payload": {
                "velocity": int(value)
            }
        }
        publish(client, f"Anki/Vehicles/U/{vehicleID}/I/jb", payload_velocity)
        print(f"Velocity updated to: {value}")


def update_acceleration_slider(value):
    global sliders_updated
    if acceleration_slider:
        # acceleration_slider.set(int(value))

        sliders_updated = True

        payload_acceleration = {
            "type": "speed",
            "payload": {
                "velocity": int(value)
            }
        }
        publish(client, f"Anki/Vehicles/U/{vehicleID}/I/jb", payload_acceleration)
        print(f"Acceleration updated to: {value}")


def sliders_released():
    global sliders_updated
    sliders_updated = False


def run_tkinter():
    def create_tkinter_window():
        global velocity_slider, acceleration_slider
        # Create the main application window
        app = tk.Toplevel()
        app.title("Emergency Flag Controller")

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

        # Button for turning the lights off
        button_off = tk.Button(lights_frame, text="Lights Off",
                               command=lambda: publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_off))
        button_off.pack(side=tk.LEFT, padx=5)

        # Button for turning the lights on
        button_on = tk.Button(lights_frame, text="Lights On",
                              command=lambda: publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_on))
        button_on.pack(side=tk.RIGHT, padx=5)

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


# publish the initial connect message
json_payload = {
    "type": "connect",
    "payload": {
        "value": "true"
    }
}

print(f"Connecting to broker at {ip_address}:{port}")
client.connect(ip_address, port=port)

client.loop_start()  # start the loop
subscribe(client)  # subscribe to the client
publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/", json_payload)  # publish the json payload
time.sleep(5)

# event objects to control the pause/resume of threads
pause_drive_event = threading.Event()
pause_lane_event = threading.Event()

# creation of the different threads
emergency_thread = threading.Thread(target=emergency_stop_process)
emergency_thread.start()

run_tkinter()

client.loop_stop()
