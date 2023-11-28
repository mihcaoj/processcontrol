import paho.mqtt.client as mqtt
import json


class AnkiController:
    def __init__(self, ip_address, port, vehicle_id):
        self.ip_address = ip_address
        self.port = port
        self.vehicle_id = vehicle_id
        self.client = mqtt.Client('hyperdrive')
        self.emergency_flag = False
        self.current_track = None

        self.initialize()

    def initialize(self):
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        self.client.connect(self.ip_address, port=self.port)
        self.client.loop_start()

    def on_connect(self, client, userdata, flags, rc):
        print(f"Connected with result code {rc}")

        payload_discover = {"type": "discover",
                            "payload": {
                                "value": True
                            }
                            }

        self.publish("Anki/Hosts/U/hyperdrive/I/", payload_discover)
        print("Published the discover payload successfully")

        if rc == 0:
            print(f"Connected to broker at {self.ip_address}:{self.port}")
            self.subscribe()
            self.publish("Anki/Vehicles/U/{self.vehicle_id}/S/status", {"value": "connected"})
            print("Publish successful")

    def on_message(self, client, userdata, msg):
        print(f"Received {msg.payload} from {msg.topic}")
        payload = json.loads(msg.payload.decode("utf-8"))

        if msg.topic == "Anki/Emergency/U":
            self.emergency_flag = payload.get("value", False)
            print(f"Emergency flag is set to {self.emergency_flag}")

    def subscribe(self):
        self.client.subscribe("Anki/Vehicles/U/{self.vehicle_id}/S/status")
        self.client.subscribe("Anki/Vehicles/U/{self.vehicle_id}/E/track")

    def publish(self, topic, payload):
        message = json.dumps(payload)
        self.client.publish(topic, message)
        print(f"Published to topic {topic} with {payload}")

    def disconnect(self):
        self.client.disconnect()

    @property  # This decorator is applied to the emergency_flag method, making it a read-only property
    def emergency_flag(self):
        return self._emergency_flag

    @emergency_flag.setter  # This designates the emergency_flag method as a setter for the corresponding property
    def emergency_flag(self, value):
        self._emergency_flag = value
        print(f"Emergency flag status changed to: {self._emergency_flag}")

    @property # This decorator is applied to the current_track
    def current_track(self):
        return self._current_track

    @current_track.setter
    def current_track(self, value):
        self._current_track = value
        print(f"Current track ID changed to: {self._current_track}")
