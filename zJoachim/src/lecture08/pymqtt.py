import paho.mqtt.client as mqtt
import time
import json

ip_address = '192.168.4.1' #ip address of the hyperdrive
port = 1883 #port for MQTT
client = mqtt.Client('hyperdrive')
vehicleID = 'd716ea410e89' #change according to the vehicle ID

def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")
    if rc == 0:
        print(f"Connected to broker at {ip_address}:{port}")
        #explicitly publish a "connected" message when connected
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
            time.sleep(2)

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/jb", payload_off) #publish the json payload to turn off the lights
            print(f"Published: {payload_off} to {vehicleID}")
            time.sleep(2)

    except KeyboardInterrupt:
        print("Blinking interrupted. Stopping the lights.")
    finally:
        client.disconnect()

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

blink_lights(vehicleID) #blinking lights function

client.loop_stop() #stop the client
