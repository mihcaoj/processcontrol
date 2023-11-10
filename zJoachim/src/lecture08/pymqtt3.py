import paho.mqtt.client as mqtt
import time
import json
import random
import threading

ip_address = '192.168.4.1' #ip address of the hyperdrive
port = 1883 #port for MQTT
client = mqtt.Client('hyperdrive')
vehicleID = 'd716ea410e89' #change according to the vehicle ID

emergency_topic = "Anki/Emergency/U" #path for emergency topic
emergency_flag = False #setting initial flag value to False

#TODO: FINISH THE FINAL EXERCISE IMPLEMENTATION FOR THE EMERGENCY STOP
#TODO: IMPLEMENT A FUNCTION THAT PARSES THROUGHT AVAILABLE VEHICLES AND CHOOSES ONE
#TODO: IMPLEMENT THE PAYLOAD_DISCOVER TO AVOID HAVING TO DO IT MANUALLY EACH TIME


def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")

    #payload_discover = {"type": "discover",
    #    "payload": {
    #    "value": True
    #    }
    #}

    #publish(client, "Anki/Hosts/U/hyperdrive/I/", payload_discover) #publish the payload to change the discover from false to true
    #print("Published the discover payload succesfully")

    if rc == 0:
        print(f"Connected to broker at {ip_address}:{port}") #explicitly publish a "connected" message when connected
        publish(client, "Anki/Vehicles/U/" + vehicleID + "/S/status", {"value": "connected"})
        print("Publish successful")

client.on_connect = on_connect

def subscribe(client: mqtt.Client):
    def on_message(client, userdata, msg):
        print("Received {message} from {topic} topic".format(message=msg.payload.decode("utf-8"), topic=msg.topic))
        print("Subscription sucessful")

    client.subscribe("Anki/Vehicles/U/" + vehicleID + "/S/status") #subscribe to the status of the vehicle
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
            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_on) #publish the json payload to turn on the lights
            print(f"Published: {payload_on} to {vehicleID}")
            time.sleep(1)

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_off) #publish the json payload to turn off the lights
            print(f"Published: {payload_off} to {vehicleID}")
            time.sleep(1)

    except KeyboardInterrupt:
        print("Blinking interrupted. Stopping the lights.")
    finally:
        client.disconnect()

def drive_car(vehicleID):
    try:
        print(f"Start driving the car: {vehicleID}")

        #set initial values for velocity and acceleration
        velocity = 0
        acceleration = 50

        while True:
            payload_speed = {
                "type": "speed",
                "payload": {
                    "velocity": velocity,
                    "acceleration": acceleration
                }
            }

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_speed)
            print(f"Published speed: {payload_speed}")

            time.sleep(3) #change velocity every 3 seconds

            velocity = random.randint(50, 100) 

    except KeyboardInterrupt:
        print("Driving interrupted. Stopping the car.")
    finally:
        client.disconnect()

def change_lane(vehicleID):
    try:
        print(f"Driving interrupted. Stopping the car")

        while True:
            offset = random.randint(-1000, 1000)
            velocity = random.randint(0, 1000)
            acceleration = random.randint(0, 2000)

            payload_lane = {
                "type": "lane",
                "payload": {
                    "offset": offset,
                    "velocity": velocity,
                    "acceleration": acceleration
                }
            }
        
            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb" , payload_lane)
            print(f"Published lane change: {payload_lane}")

            time.sleep(5) #change lane every 5 seconds

    except KeyboardInterrupt:
        print("Lane change interrupted. Stopping the car.")
    finally:
        client.disconnect()

def stop_vehicles():
    #sets the pause event to pause the drive_car and change_lane threads
    print("Stopping vehicle")
    pause_drive_event.set()
    pause_lane_event.set()
    
    time.sleep(10)

    #clears the pause event so vehicles can resume their activity
    pause_drive_event.clear()
    pause_lane_event.clear()

    print("Resuming vehicle")
    


def emergency_stop_process():
    client_emergency = mqtt.Client('emergency_stop_process')
    client_emergency.on_connect = on_connect
    client_emergency.on_message = on_message

    print(f"Connecting to broker at {ip_address}:{port} for emergency stop process")
    client_emergency.connect(ip_address, port=port)

    client_emergency.loop_start()

    try:
        while True:
            time.sleep(1)  #check the emergency flag every second
            if emergency_flag:
                stop_vehicles() 
            else:
                print("Emergency stop is inactive. Vehicles can resume normal operations.")

    except KeyboardInterrupt:
        print("Emergency stop process interrupted.")
    finally:
        client_emergency.disconnect()
        
#publish the initial connect message        
json_payload = {
    "type": "connect",
    "payload":{
    "value": "true"
    }
}

print(f"Connecting to broker at {ip_address}:{port}")
client.connect(ip_address, port=port)

client.loop_start() #start the loop
subscribe(client) #subscribe to the client
publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/", json_payload) #publish the json payload
time.sleep(5)

#blink_lights(vehicleID) #blinking lights function
#drive_car(vehicleID) #drive_car function
#change_lane(vehicleID) #change lane function

#event objects to control the pause/resume of threads
pause_drive_event = threading.Event()
pause_lane_event = threading.Event()

blink_thread = threading.Thread(target=blink_lights, args=(vehicleID,)) #create a thread for the blink_lights function
blink_thread.start()

drive_thread = threading.Thread(target=drive_car, args=(vehicleID,)) #create a thread for the drive_car function
drive_thread.start()

change_lane_thread = threading.Thread(target=change_lane, args=(vehicleID,)) #create a thread for the change_lane function
change_lane_thread.start()

client.loop_stop() #stop the client
