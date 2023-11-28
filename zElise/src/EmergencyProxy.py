
import asyncio
import json
import time
import gmqtt
from gmqtt import Client as MQTTClient

class EmergencyProxy:
    """Class to handle emergency situations with vehicles using MQTT protocol."""

    def __init__(self):
        """Initialize the EmergencyProxy."""
        print("Initializing EmergencyProxy")
        self.is_emergency_active = False
        self.mqtt_vehicle_intent_message_queue = asyncio.Queue()
        self.mqtt_emergency_queue = asyncio.Queue()
        will_message = gmqtt.Message("EmergencyProxy/U/quantasy/S/online", '{"value":"false"}')
        self.client = MQTTClient("EmergencyProxyQuantasy", will_message=will_message)
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message

    async def start(self):
        """Connect to MQTT broker and start processing messages."""
        await self.client.connect("localhost", 1883)
        asyncio.create_task(self.process_vehicle_intent_messages())
        asyncio.create_task(self.process_emergency_status_messages())

    def on_connect(self, client, flags, rc, properties):
        """Handle connection to MQTT broker."""
        print("Connected with result code", rc)
        client.subscribe("EmergencyProxy/U/quantasy/I/#")
        client.subscribe("EmergencyProxy/U/quantasy/Anki/Vehicles/#")
        client.publish("EmergencyProxy/U/quantasy/S/online", '{"value":"true"}')

    def on_message(self, client, topic, payload, qos, properties):
        """Handle incoming MQTT messages."""
        payload = payload.decode("utf-8")  # Decoding payload from bytes to string
        if topic.startswith("EmergencyProxy/U/quantasy/I"):
            asyncio.create_task(self.mqtt_emergency_queue.put((topic, payload)))
        elif topic.startswith("EmergencyProxy/U/quantasy/Anki/Vehicles"):
            asyncio.create_task(self.mqtt_vehicle_intent_message_queue.put((topic, payload)))

    async def process_vehicle_intent_messages(self):
        """Process messages related to vehicle intent."""
        while True:
            topic, payload = await self.mqtt_vehicle_intent_message_queue.get()
            print(f"Processing vehicle intent message: {topic}")  # Debug print
            await self.process_vehicle_intent_message(topic, payload)

    async def process_emergency_status_messages(self):
        """Process messages related to emergency status."""
        while True:
            topic, payload = await self.mqtt_emergency_queue.get()
            print(f"Processing emergency Intent message: {topic}")  # Debug print
            await self.update_emergency_status(payload)

    async def update_emergency_status(self, payload):
        """Update the current emergency status."""
        try:
            data = json.loads(payload)
            print(f"update emergency status: {data}")  # Debug print
            payload = data.get("payload")
            print(f"update emergency status: {payload}")  # Debug print
            value_str = payload.get("value", False)
            self.is_emergency_active = value_str.lower() == "true"
            await self.publish_emergency_status()
        except Exception as e:
            print(f"Error processing emergency status: {e}")

    async def publish_emergency_status(self):
        """Publish the current emergency status."""
        try:
            payload = json.dumps({
                "timestamp": int(time.time() * 1000),
                "value": self.is_emergency_active
            })
            self.client.publish("EmergencyProxy/U/quantasy/S/emergency", payload)
            print(f"Published emergency status: {self.is_emergency_active}")  # Debug print

            if self.is_emergency_active:
                emergency_payload = json.dumps({
                    "type": "speed",
                    "payload": {
                        "velocity": "0",
                        "acceleration": "2000"
                    }
                })
                self.client.publish("Anki/Vehicles/I/EmergencyProxy/U/quantasy", emergency_payload)
                print("Published emergency vehicle intent")  # Debug print
        except Exception as e:
            print(f"Error publishing emergency status: {e}")

    async def process_vehicle_intent_message(self, topic, payload):
        """Process individual vehicle intent messages."""
        try:
            if self.is_emergency_active:
                print("Modifying payload due to active emergency")  # Debug print
                payload = self.remove_entries(payload)
            if payload:
                target_topic = topic.replace("EmergencyProxy/U/quantasy/Anki/Vehicles", "Anki/Vehicles")
                self.client.publish(target_topic, payload)
                print(f"Published vehicle intent to {target_topic}")  # Debug print
        except Exception as e:
            print(f"Error processing vehicle message: {e}")

    def remove_entries(self, json_input):
        """Remove certain entries from the JSON input based on defined criteria."""
        data = json.loads(json_input)
        if isinstance(data, list):
            return json.dumps([item for item in data if not self.should_be_removed(item)])
        elif isinstance(data, dict) and self.should_be_removed(data):
            return None
        return json.dumps(data)

    def should_be_removed(self, node):
        """Define criteria for removing nodes."""
        node_type = node.get("type")
        return node_type in ["speed", "lane"]

    async def cleanup(self):
        """Clean up and disconnect from MQTT broker."""
        self.client.publish("EmergencyProxy/U/quantasy/S/online", '{"value":"false"}')
        await self.client.disconnect()


async def main():
    """Main function to run the EmergencyProxy."""
    emergency_proxy = EmergencyProxy()
    try:
        await emergency_proxy.start()
        while True:
            await asyncio.sleep(60)
    except KeyboardInterrupt:
        print("Interrupt received, cleaning up...")
    finally:
        await emergency_proxy.cleanup()

asyncio.run(main())
