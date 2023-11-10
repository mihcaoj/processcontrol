import paho.mqtt.client as mqtt
import time
import json
import random
import threading
import queue

# create a client
client = mqtt.Client('hyperdrive')
# coordinates of the broker
ip_address = "192.168.4.1"
port = "1883"

# topics in which the commands will be handled
topic = "BlinkingElise"
discoverVehicleTopic = "BlinkingElise/U/knr1/WeakCoupling/DiscoverObserver/E/vehicleDiscovered"
# ids for vehicle detection
desired_vehicle_id = "cec233dec1cb" # vehicle id for the one we want to connect
discovered_vehicles = queue.LifoQueue() # discovered vehicles id queue

class mqttHandler:
