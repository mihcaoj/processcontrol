import paho.mqtt.client as mqtt
import time
import json
import random
import threading
import tkinter as tk

ip_address = '192.168.4.1'  # ip address of the hyperdrive
port = 1883  # port for MQTT
client = mqtt.Client('hyperdrive')
vehicleID = 'f4c22c6c0382'  # change according to the vehicle ID

emergency_topic = "Anki/Emergency/U"  # path for emergency topic
emergency_flag = False  # setting initial flag value to False


# TODO: IMPLEMENT A FUNCTION THAT PARSES THROUGH AVAILABLE VEHICLES AND CHOOSES ONE
# TODO: IMPLEMENT A DISCONNECT FUNCTION

def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")

    payload_discover = {"type": "discover",
                        "payload": {
                            "value": True
                        }
                        }

    publish(client, "Anki/Hosts/U/hyperdrive/I/",
            payload_discover)  # publish the payload to change discover from false to true
    print("Published the discover payload successfully")

    if rc == 0:
        print(f"Connected to broker at {ip_address}:{port}")
        publish(client, "Anki/Vehicles/U/" + vehicleID + "/S/status", {"value": "connected"})
        print("Publish successful")


def on_message(client, userdata, msg):
    print(f"Received {msg.payload} from {msg.topic}")
    payload = json.loads(msg.payload.decode("utf-8"))

    global emergency_flag
    if msg.topic == emergency_topic:
        emergency_flag = payload.get("value", False)
        print(f"Emergency flag is set to {emergency_flag}")


def subscribe(client: mqtt.Client):
    client.subscribe("Anki/Vehicles/U/" + vehicleID + "/S/status")  # subscribe to the status of the vehicle
    client.on_message = on_message


def publish(client: mqtt.Client, topic: str, payload: dict):
    message = json.dumps(payload)
    client.publish(topic, message)
    print(f"Published to topic {topic} with {payload}")


def blink_lights(vehicleID):
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


def drive_car(vehicleID):
    try:
        print(f"Start driving the car: {vehicleID}")

        while True:
            if not emergency_flag:
                # set initial values for velocity and acceleration
                velocity = random.randint(-100, 2000)
                acceleration = random.randint(0, 2000)
            else:
                # If emergency_flag is True, set velocity and acceleration to stop the car
                velocity = 0
                acceleration = 0

            payload_speed = {
                "type": "speed",
                "payload": {
                    "velocity": velocity,
                    "acceleration": acceleration
                }
            }

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_speed)
            print(f"Published speed: {payload_speed}")

            time.sleep(3)  # change velocity every 3 seconds

            velocity = random.randint(-100, 2000)
            acceleration = random.randint(0, 2000)

    except KeyboardInterrupt:
        print("Driving interrupted. Stopping the car.")
    finally:
        client.disconnect()


def change_lane(vehicleID):
    try:
        print(f"Driving interrupted. Stopping the car")

        while True:
            if not emergency_flag:
                offset = random.randint(-1000, 1000)
                velocity = random.randint(0, 1000)
                acceleration = random.randint(0, 2000)
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
            print(f"Published lane change: {payload_lane}")

            time.sleep(5)  # change lane every 5 seconds

    except KeyboardInterrupt:
        print("Lane change interrupted. Stopping the car.")
    finally:
        client.disconnect()


def stop_vehicle():
    # sets the pause event to pause the drive_car and change_lane threads
    print("Stopping vehicle")
    pause_drive_event.set()
    pause_lane_event.set()


# def resume_vehicle():
# clears the pause event so vehicles can resume their activity
#    print("Resuming vehicle")
#    pause_drive_event.clear()
#    pause_lane_event.clear()


def emergency_stop_process():
    client_emergency = mqtt.Client('emergency_stop_process')
    client_emergency.on_connect = on_connect
    client_emergency.on_message = on_message

    print(f"Connecting to broker at {ip_address}:{port} for emergency stop process")
    client_emergency.connect(ip_address, port=port)

    client_emergency.loop_start()

    try:
        while True:
            time.sleep(1)  # check the emergency flag every second
            if emergency_flag:
                stop_vehicle()
            else:
                print("Emergency stop is inactive. Vehicles can resume normal operations.")

    except KeyboardInterrupt:
        print("Emergency stop process interrupted.")
    finally:
        client_emergency.disconnect()


def change_flag_status():
    global emergency_flag
    emergency_flag = not emergency_flag
    print(f"Emergency flag is now: {emergency_flag}")


def run_tkinter():
    def create_tkinter_window():
        # Create the main application window
        app = tk.Tk()
        app.title("Emergency Flag Controller")

        # Create a button to toggle the emergency flag
        button = tk.Button(app, text="Toggle Emergency Flag", command=change_flag_status)
        button.pack(pady=10)

        app.mainloop()  # Run the Tkinter event loop

    app_ref = None

    def start_tkinter():
        nonlocal app_ref
        app_ref = tk.Tk()
        app_ref.withdraw()
        app_ref.after(0, create_tkinter_window())
        app_ref.mainloop()

    # Run the Tkinter window in a separate thread
    tkinter_thread = threading.Thread(target=start_tkinter())
    tkinter_thread.start()

    # Keep a reference to the app so that it doesn't get garbage collected
    return app_ref


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

blink_thread = threading.Thread(target=blink_lights, args=(vehicleID,))
blink_thread.start()

drive_thread = threading.Thread(target=drive_car, args=(vehicleID,))
drive_thread.start()

change_lane_thread = threading.Thread(target=change_lane, args=(vehicleID,))
change_lane_thread.start()

run_tkinter()

client.loop_stop()  # stop the client
