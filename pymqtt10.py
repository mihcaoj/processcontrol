# imports
import json
import paho.mqtt.client as mqtt
import time
import threading  # to make the functions asynchronous = that they can run simultaneously
import tkinter as tk

# TODO: - disconnect / cleanup
#        - slider + bounds DONE
#        - add threads DONE
#        - improve gui
#        / NOT NECESSARY - put everything in a class
#        - handle errors when publishing (in publish functions) + check error handling  @ vehicle connection
#        - fix the buttons w/ the payload

# create a client
client = mqtt.Client('hyperdrive')
# ip address - BROKER
ip_address = "192.168.4.1"
port = 1883
# Anki vehicle nb:
vehicleID = "a  "  # "d716ea410e89"
# name of the topic of the host
topicName = "jb"
# boolean to make sure connexion is maintained
wantToConnect: bool = False
# path for emergency topic
emergency_topic = "Anki/Emergency/U"
# boolean for the emergency stop
emergency_flag: bool = False
# track the subscription status to the vehicle
subscribed = False
# Declare velocity_slider and acceleration_slider as global variables
velocity_slider = None
acceleration_slider = None
sliders_updated = False

#flags for the reconnection
connectflags = ""

# TODO: remove that
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


# ---------------------------- Broker connection --------------------------------------

# CONNECT the client to the MQTT broker
def connect_broker():
    client.connect(ip_address)
    global wantToConnect
    wantToConnect = True # flag to handle automatic reconnection
    print(f"Trying to connect to broker {ip_address}:{port}")

# ON CONNECT to handle eventually failed connection
def on_connect(client, userdata, flags, rc):
    global connectflags
    connectflags = flags
    if rc == 0:
        print(f"Succesfully connected to broker at {ip_address}:{port}")
    else:
        print("Connection failed")
        if (wantToConnect == True):
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
    global subscribed
    subscribed = False
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

#------------------------------ Publish, subscribe and mqtt functions -----------------------------------
# def handling messages
def on_message(client, userdata, msg):
    print("Received {message} from {topic} topic".format(message=msg.payload.decode(), topic=msg.topic))
    # track when the vehicle looses connection
    if msg.payload.decode() == 'lost':
        global subscribed
        subscribed = False
    # set the emergency flag to true when receiving an emergency topic
    if msg.topic == emergency_topic:
        global emergency_flag
        emergency_flag = True
        print(f"Emergency flag is set to {emergency_flag}")

#SUBSCRIBE (client as argument)
#print the message received by the subscribe function of the client class
def subscribe(client: mqtt):
    client.subscribe("Anki/Vehicles/U/" + vehicleID + "/S/status")
    # handles messages
    client.on_message = on_message

#ON_SUBSCRIBE to check subscription status (callback)
def on_subscribe(client, userdata, mid, granted_qos):
    # apparently, according to the paho mqtt docs, raises an error only if granted qos not equal to 0,1 or 2
    if granted_qos == 0 or granted_qos == 1 or granted_qos == 2:
        global subscribed
        subscribed = True
    print("Subscribed with QoS:", granted_qos)

#UNSUBSCRIBE function
def unsubscribe (client: mqtt):
    global subscribed
    subscribed = False
    client.unsubscribe("Anki/Vehicles/U/" + vehicleID + "/S/status")
    print("client unsubscribed from broker")

#PUBLISH
def publish(client: mqtt.Client, topic: str, payload: dict):
    message = json.dumps(payload)
    client.publish(topic, message)
    print(f"Published to topic {topic} with {payload}")

# ---------------------- Vehicle connection and discoverability -------------------------------

#make the host discoverable
def make_discoverable():
    discoverable_payload = {
        "type": "connect",
        "payload": {
            "value": True
        }
    }
    try:
        publish(client, "Anki/Hosts/U/hyperdrive/I/", discoverable_payload)
    except Exception as e:
        print("Failed to make the host discoverable")

# connect to the desired vehicle
def connect_vehicle(vhID):
    topic = "Anki/Vehicles/U/" + vhID + "/I/"
    connect_payload = {
        "type": "connect",
        "payload": {
            "value": True
        }
    }
    publish(client, topic, connect_payload)

# disconnect from the vehicle
def disconnect_vehicle(vhID):
    topic = "Anki/Vehicles/U/" + vhID + "/I/"
    connect_payload = {
        "type": "connect",
        "payload": {
            "value": False
        }
    }
    publish(client, topic, connect_payload)

# ---------------------- Vehicle commands -------------------------------
#        lights -----------

#back and front lights blinking every 2 seconds
def blink_lights(vehicleID, trueORFalse: bool):
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
    if trueORFalse == True:
        try:
            print(f"Start the blink on vehicle: {vehicleID}")

            while True:
                publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName,
                        payload_on)  # publish the json payload to turn on the lights
                print(f"Published: {payload_on} to {vehicleID}")
                time.sleep(1)

                publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName,
                        payload_off)  # publish the json payload to turn off the lights
                print(f"Published: {payload_off} to {vehicleID}")
                time.sleep(1)

        except KeyboardInterrupt:
            print("Blinking interrupted. Stopping the lights.")
        finally:
            client.disconnect()

# turn the front lights on
def frontLightsOn(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "front": "on"
        }
    }

    blink_lights(vehicleID, False)

    try:
        print(f"Turning on the front lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName,
                payload)  # publish the json payload to turn on the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning the front lights on")

# turn the front lights off
def frontLightsOff(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "front": "off"
        }
    }

    blink_lights(vehicleID, False)

    try:
        print(f"Turning on the front lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName,
                payload)  # publish the json payload to turn on the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning the front lights off")


# turn the back lights on
def backLightsOn(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "back": "on"
        }
    }

    blink_lights(vehicleID, False)

    try:
        print(f"Turning on the front lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn on the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning the back lights off")


# turn the back lights off
def backLightsOff(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "back": "off"
        }
    }

    blink_lights(vehicleID, False)

    try:
        print(f"Turning on the front lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn on the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning the back lights off")

# turn the all the lights on
def allLightsOn(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "back": "on",
            "front":"on"
        }
    }

    blink_lights(vehicleID, False)

    try:
        print(f"Turning on all the lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn on the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning all the lights off")

# turn the all the lights off
def allLightsOff(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "back": "off",
            "front":"off"
        }
    }

    blink_lights(vehicleID, False)

    try:
        print(f"Turning off all the lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn off the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning all the lights off")


#        drive commands -----------

# change lane on which the car is driving
def change_lane(ankiID):
    try:
        print("Driving interrupted. Stopping vehicle: " + ankiID)

        while True:
            if not emergency_flag and not sliders_updated:
                offset = 0
                velocity = 300
                acceleration = 300
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

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_lane)
            print(f"Published lane change: {payload_lane}")

            time.sleep(5)  # change lane every 5 seconds

    except KeyboardInterrupt:
        print("Lane change interrupted. Stopping vehicle: " + vehicleID)
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

    publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_lane)


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

    publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_lane)


# event objects to control the pause/resume of threads
pause_drive_event = threading.Event()
pause_lane_event = threading.Event()
def stop_vehicle(vehicleID):
    # sets the pause event to pause the drive_car and change_lane threads
    print("Stopping vehicle: " + vehicleID)
    #stops the vehicle
    velocity = 0
    acceleration = 0
    payload_speed = {
        "type": "speed", "payload": {"velocity": velocity, "acceleration": acceleration}}

    publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_speed)
    '''????  Changer et comprendre ça:'''
    pause_drive_event.set()
    pause_lane_event.set()
'''' marche: '''


# change the velocity of the car on the gui
def update_velocity_slider(value):
    global sliders_updated
    if velocity_slider:
        velocity_slider.set(int(value))

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
        acceleration_slider.set(int(value))

        sliders_updated = True

        payload_acceleration = {
            "type": "speed",
            "payload": {
                "velocity": int(value)
            }
        }
        publish(client, f"Anki/Vehicles/U/{vehicleID}/I/jb", payload_acceleration)
        print(f"Acceleration updated to: {value}")


#        emergency commands -----------

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
                blink_lights(vehicleID)
                stop_vehicle(vehicleID)
            else:
                current_time = time.time()
                if current_time - last_print_time >= 5:
                    print("Emergency flag status: False.")
                    last_print_time = current_time

    except KeyboardInterrupt:
        print("Emergency stop process interrupted.")
    finally:
        client_emergency.disconnect()


#           GUI -------------

def run_tkinter():
    def create_tkinter_window():
        global velocity_slider, acceleration_slider
        # Create the main application window
        app = tk.Toplevel()
        app.geometry('1280x720')
        app.title("Emergency Flag Controller")

        #text input box to enter the name of the vehicle
        vhNameBox = tk.Entry(app, width=30)
        def on_connect_submit():
            vehicleID = vhNameBox.get()
            connect_vehicle(vehicleID)
        def on_disconnect_submit():
            vehicleID = vhNameBox.get()
            disconnect_vehicle(vehicleID)
        #button to submit the input
        connectButton = tk.Button(app, text="Connect to vehicle", command=on_connect_submit)
        disconnectButton = tk.Button(app, text="Disconnect from vehicle", command=on_disconnect_submit)
        vhNameBox.pack(pady=10)
        connectButton.pack(pady=10)
        disconnectButton.pack(pady=10)

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

        # Slider for changing the acceleration
        acceleration_label = tk.Label(app, text="Acceleration:")
        acceleration_label.pack()
        acceleration_slider = tk.Scale(app, from_=0, to=2000,
                                       orient=tk.HORIZONTAL,
                                       command=update_acceleration_slider)

        acceleration_slider.pack()

        # stop button
        stopbutton = tk.Button(text="Stop Vehicle", command=stop_vehicle(vehicleID))
        stopbutton.pack(side=tk.LEFT, padx=5)

        # Buttons for changing lane
        change_lane_frame = tk.Frame(app)
        change_lane_frame.pack(side=tk.TOP, pady=20)

        button_left = tk.Button(change_lane_frame, text="Change Lane Left", command=change_lane_left)
        button_left.pack(side=tk.LEFT, padx=5)

        button_right = tk.Button(change_lane_frame, text="Change Lane Right", command=change_lane_right)
        button_right.pack(side=tk.RIGHT, padx=5)

        # Frame for lights buttons
        lights_frame = tk.Frame(app)
        lights_frame.pack(side=tk.TOP, pady=20)

        # Button for turning front lights off
        button_off = tk.Button(lights_frame, text="Front lights Off",
                               command=lambda: frontLightsOff(vehicleID))#publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_off))
        button_off.pack(side=tk.LEFT, padx=5)

        # Button for turning front lights on
        button_on = tk.Button(lights_frame, text="Front lights On",
                              command=lambda: frontLightsOn(vehicleID))#publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_on))
        button_on.pack(side=tk.RIGHT, padx=5)

        # Button for turning back lights off
        button_off = tk.Button(lights_frame, text="Back lights Off",
                               command=lambda: backLightsOff(
                                   vehicleID))  # publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_off))
        button_off.pack(side=tk.LEFT, padx=5)

        # Button for turning back lights on
        button_on = tk.Button(lights_frame, text="Back lights On",
                              command=lambda: backLightsOn(
                                  vehicleID))  # publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_on))
        button_on.pack(side=tk.RIGHT, padx=5)

        # Button for making the lights blink
        button_on = tk.Button(lights_frame, text="Blink lights",
                              command=lambda: blink_lights(vehicleID, trueORFalse=True))  # publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName, payload_on))
        button_on.pack(pady=5)

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

#------------------------------ Function invokation -----------------------------------

'''
# connect to the broker
connect_broker()
# make the host discoverable if it is not already
make_discoverable()

# ----- start the loop to open the GUI
#client.loop_start()

# subscribe to the broker
subscribe(client)
# connect to the right vehicle
connect_vehicle(vehicleID)
# operate commands on that vehicle

# creation of the different threads
emergency_thread = threading.Thread(target=emergency_stop_process)
emergency_thread.start()

blink_thread = threading.Thread(target=blink_lights, args=(vehicleID))
blink_thread.start()
'''

# run the GUI
run_tkinter()

#client.loop_stop()
