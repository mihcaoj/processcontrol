# imports
import json
import paho.mqtt.client as mqtt
import time
import asyncio #to make the functions asynchronous = that they can run simultaneously

# create a client
client = mqtt.Client('hyperdrive')
# ip address - BROKER
ip_address = "192.168.4.1"
port = "1883"
# Anki vehicle nb:
ankiID = "c40caf091413" # "d716ea410e89"
# name of the topic of the host
topicName = "elise"
# boolean to make sure connexion is maintained
wantToConnect: bool = False
# boolean for the emergency stop
needToStop: bool = False


# ---------------------------- Connection etc --------------------------------------

# CONNECT the client to the MQTT broker
def connect_broker():
    client.connect(ip_address)
    global wantToConnect
    wantToConnect = not wantToConnect
    print("Connected to broker: ",ip_address,":",port)

# ON CONNECT to handle eventually failed connection

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"Succesfully connected to broker at {ip_address}:{port}")
        # make an explicitly connected message
        publish(client, "Anki/Vehicles/U/" + ankiID + "/S/status", {"value": "connected"})
    else:
        print("Connection failed")
        if (wantToConnect == True):
            print("Attempting reconnection to broker: ", ip_address, ":", port)
            try:
                connect_broker()
                publish(client, "Anki/Vehicles/U/" + ankiID + "/S/status", {"value": "connected"})
            except Exception as e:
                print("Reconnection attempt failed. Please try reconnect manually")

#make sure on_connect is invoked automatically
client.on_connect = on_connect

# DISCONNECTED and AUTOMATIC RECONNECTION handling
def on_disconnect(client, userdata, rc):
    print("Connexion to broker lost with result code: %s", rc)
    global wantToConnect
    if (wantToConnect == True):
        print("Attempting reconnection to broker: ",ip_address,":",port)
        try:
            connect_broker()
        except Exception as e:
            print("Reconnection attempt failed. Please try reconnect manually")

#make sure on_disconnect is invoked automatically
client.on_disconnect = on_disconnect

# DISCONNECT function
def disconnect_broker ():
    client.disconnect()
    global wantToConnect
    wantToConnect = not wantToConnect
    print("Disconnected from the broker: ",ip_address,":",port)

#----------------------------- Subscribe etc -----------------------------------

subscribed = True
#SUBSCRIBE (client as argument)
#print the message received by the subscribe function of the client class
def subscribe (client: mqtt):
    client.subscribe("Anki/Vehicles/U/" + ankiID + "/S/status")
    # handles messages
    def on_message(client, userdata, msg):
        print("Received {message} from {topic} topic".format(message=msg.payload.decode(), topic=msg.topic))
    client.on_message = on_message

#ON_SUBSCRIBE to check subscription status (callback)
def on_subscribe(client, userdata, mid, granted_qos):
    global subscribed
    subscribed = True
    print("Subscribed with QoS:", granted_qos)

#UNSUBSCRIBE function
''' Note: idk if it works '''
def unsubscribe (client: mqtt):
    global subscribed
    subscribed = False
    client.unsubscribe("Anki/Vehicles/U/"+ankiID+"/S/status")
    print("client unsubscribed from broker")

#PUBLISH
def publish(client: mqtt.Client, topic: str, payload: dict):
    message = json.dumps(payload)
    client.publish(topic, message)
    #print(f"Sent publish to topic {topic} with {payload}")
    # no error handling

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
def connect_vehicle():
    topic = "Anki/Vehicles/U/"+ankiID+"/I/"
    connect_payload = {
        "type": "connect",
        "payload": {
            "value": True
        }
    }
    publish(client, topic, connect_payload)

# disconnect from the vehicle
def disconnect_vehicle():
    topic = "Anki/Vehicles/U/"+ankiID+"/I/"
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
            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName,
                    payload_on)  # publish the json payload to turn on the lights
            print(f"Published: {payload_on} to {vehicleID}")
            time.sleep(2)

            publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/"+topicName,
                    payload_off)  # publish the json payload to turn off the lights
            print(f"Published: {payload_off} to {vehicleID}")
            time.sleep(2)

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

    try:
        print(f"Turning on the front lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn on the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning the back lights off")

# turn the all the lights off
def allLightsOff(vehicleID):
    payload = {
        "type": "lights",
        "payload": {
            "back": "off",
            "front":"off"
        }
    }

    try:
        print(f"Turning off all the lights on vehicle: {vehicleID}")

        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn off the lights
        print(f"Published: {payload} to {vehicleID}")


    except Exception as e:
        print("Error turning all the lights off")


#        drive commands -----------

# drive at the speed and acceleration provided as argument. speed in mm/s, acceleration in mm/s^2
def drive(vehicleID, speed: int, acceleration: int):

    # checking if the requested speed is within the limits, if not, adjust it
    lowerSpeedBound = -100
    upperSpeedBound = 2000

    if speed > upperSpeedBound:
        #print(f"Driving at speed: {upperSpeedBound}. Requested speed: {speed} is too high (max = {upperSpeedBound})")
        speed = upperSpeedBound
    if speed < lowerSpeedBound:
        #print(f"Driving at speed: {lowerSpeedBound}. Requested speed: {speed} is too low (min = {lowerSpeedBound})")
        speed = lowerSpeedBound
    #else:
    #    print(f"Driving at speed: {speed}")

    # checking if the requested acceleration is within the limits, if not, adjust it
    lowerAccBound = 0
    upperAccBound = 2000
    if acceleration > upperAccBound:
        #print(f"Setting acceleration to: {upperAccBound}. Requested acceleration: {acceleration} is too high (max = {upperAccBound})")
        acceleration = upperAccBound
    if acceleration < lowerAccBound:
        #print(f"Setting acceleration to: {lowerAccBound}. Requested acceleration: {acceleration} is too low (min = {lowerAccBound})")
        acceleration = lowerAccBound
    #else:
    #    print(f"Setting acceleration to: {acceleration}")

    # defining the json payload
    stringspeed = str(speed)
    stringacc = str(acceleration)
    payload = {
            "type": "speed",
            "payload": {
                "velocity": stringspeed,
                "acceleration": stringacc
            }
        }

    # publishing the request
    try:
        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                payload)  # publish the json payload to turn on the lights
    except Exception as e:
        print("Error making the vehicle drive")

# stop the vehicle upon invocation
def stopEmergency(vehicleID):
    zero = str(0)
    stopPayload= {
        "type": "speed",
        "payload": {
            "velocity": zero,
            "acceleration": zero
        }
    }
    try:
        publish(client, "Anki/Vehicles/U/" + vehicleID + "/I/" + topicName,
                stopPayload)  # publish the json payload to turn on the lights
    except Exception as e:
        print("ERROR EMERGENCY STOPPING THE VEHICLE")


# drive at the speed and acceleration passed as argument, stop if requested
def autoDrive(vehicleID, speed: int, acceleration: int):

    while True:
        if not needToStop:
            drive(vehicleID, speed, acceleration)
        else:
            stopEmergency(vehicleID)


# ----------------------  function invokations ----------------------

# connect to the broker
connect_broker()
# make the host discoverable if it is not already
make_discoverable()
# subscribe to the broker
subscribe(client)
# connect to the right vehicle
connect_vehicle()
# operate commands on that vehicle
autoDrive(ankiID, 1000, 1000)
#blink_lights(ankiID)

