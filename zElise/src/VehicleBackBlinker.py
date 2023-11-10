import asyncio
import json
from gmqtt import Client as MQTTClient

BROKER = "192.168.4.1"
PORT = 1883
vhname = "cec233dec1cb" #d205effe02cb

class VehicleFrontBlinker:
    def __init__(self):
        self.client = MQTTClient("AsyncBlinker")
        self.broker = BROKER
        self.port = PORT

    async def connect(self):
        print("Attempting to connect...")
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        try:
            await self.client.connect(self.broker, self.port)
            print("Connected successfully!")
        except Exception as e:
            print(f"Connection failed with error: {e}")

        try:
            # CHANGE THE FIRST WORD TO A UNIQUE ONE: DIFFERENT THREAD
            self.client.subscribe("BlinkingElise/U/knr1/WeakCoupling/ConnectedObserver/E/vehicleConnected")
            print("Subscribed to topic!")
        except Exception as e:
            print(f"Subscription failed with error: {e}")

    async def disconnect(self):
        print("Disconnecting...")
        await self.client.disconnect()
        print("Disconnected successfully!")

    async def start_blinking(self, vehicle_id):
        print(f"Starting blinking for vehicle {vehicle_id}...")
        while True:
            print(f"Turning ON lights for vehicle {vehicle_id}")
            self.publish(f"Anki/Vehicles/U/{vehicle_id}/I", {"type": "lights", "payload": {"back": "on"}})
            await asyncio.sleep(0.7)
            print(f"Turning OFF lights for vehicle {vehicle_id}")
            self.publish(f"Anki/Vehicles/U/{vehicle_id}/I", {"type": "lights", "payload": {"back": "off"}})
            await asyncio.sleep(0.7)

    def publish(self, topic, payload):
        try:
            self.client.publish(topic, json.dumps(payload), qos=0)
            print(f"Published to topic {topic} with payload {payload}")
        except Exception as e:
            print(f"Publish failed with error: {e}")

    def on_connect(self, client, flags, rc, properties):
        print(f"Connected with result code: {rc}")

    def on_message(self, client, topic, payload, qos, properties):
        vehicle_id = payload.decode("utf-8")
        print(f"Received message for vehicle: {vehicle_id}")
        asyncio.create_task(self.start_blinking(vehicle_id))

async def main():
    blinker = VehicleFrontBlinker()
    await blinker.connect()
    await blinker.start_blinking(vhname)
    await asyncio.sleep(600)  # Run for 10 minutes
    await blinker.disconnect()

asyncio.run(main())

# The code provided works along with the weakcoupling java-project.
# The code provided uses asynchronous programming, which is slightly different from traditional multi-threading but shares some similarities:
# Concurrency without Threads: In the code, asyncio is used to achieve concurrency without creating new threads.
# It's based on the idea of writing code that can be paused (awaited) and resumed, allowing other tasks to run in the meantime.
#
# Tasks:
# In the context of asyncio, tasks are a way to run co-routines concurrently.
# In the provided code, every time a message is received, a new task is created to handle the blinking of the vehicle's lights.
#
# Inter-task Signalling:
# The MQTT client in the code essentially acts as a signal. When a message is received, it signals the application to start a new blinking task.
