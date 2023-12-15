import paho.mqtt.client as mqtt
import json
import threading


# Class for handling communication with Anki vehicles
class AnkiController:
    def __init__(self, ip_address, port, vehicle_id):
        self.ip_address = ip_address
        self.port = port
        self.vehicle_id = vehicle_id
        self.client = mqtt.Client('hyperdrive')
        self._emergency_flag = False
        self.current_track = None

        # Create a lock for synchronizing access to emergency_flag
        self.emergency_flag_lock = threading.Lock()

        # Set the on_connect and on_message callbacks directly in the constructor
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message

        # Connect to the MQTT broker and start the client loop
        self.client.connect(self.ip_address, port=self.port)
        self.client.loop_start()

    def on_connect(self, client, userdata, flags, rc):
        print(f"Connected with result code {rc}")

        payload_discover = {"type": "discover",
                            "payload": {"value": True}}

        payload_connect = {"type": "connect",
                           "payload": {"value": 'true'}}

        self.publish("Anki/Hosts/U/hyperdrive/I/", payload_discover)
        print("Published the discover payload successfully")

        # Check if the connection result code is 0 (success)
        if rc == 0:
            print(f"Connected to broker at {self.ip_address}:{self.port}")
            self.subscribe()
            self.publish(f"Anki/Vehicles/U/{self.vehicle_id}/I/", payload_connect)
            print("Publish successful")

    def on_message(self, client, userdata, msg):
        try:
            print(f"Received {msg.payload} from {msg.topic}")
            # Decode the JSON payload and handle messages related to emergency and track information
            payload = json.loads(msg.payload.decode("utf-8"))

            if msg.topic == "Anki/Emergency/U":
                self.emergency_flag = payload.get("value", False)
                print(f"Emergency flag is set to {self.emergency_flag}")

        except json.JSONDecodeError as e:
            print(f"Error decoding JSON message: {e}")
        except Exception as e:
            print(f"Error in on_message: {e}")

    def subscribe(self):
        self.client.subscribe(f"Anki/Vehicles/U/{self.vehicle_id}/S/status")  # subscribe to status topic
        self.client.subscribe(f"Anki/Vehicles/U/{self.vehicle_id}/E/track")  # subscribe to track topic

    def publish(self, topic, payload):
        message = json.dumps(payload)
        self.client.publish(topic, message)
        print(f"Published to topic {topic} with {payload}")

    def disconnect(self):
        self.client.disconnect()

    def process_track_information(self):
        print("Processing Track Information:")

        # Accessing the current_track property to retrieve its value
        current_track_value = self.current_track
        print(f"Current Track: {current_track_value}")

        # Modifying the current_track property
        new_track_value = "new_track_id"
        self.current_track = new_track_value
        print(f"Updated Current Track: {self.current_track}")

    @property  # Getter method for the emergency_flag property
    def emergency_flag(self):
        with self.emergency_flag_lock:
            return self._emergency_flag

    @emergency_flag.setter  # Setter method for the emergency flag
    def emergency_flag(self, value):
        with self.emergency_flag_lock:
            self._emergency_flag = value
            print(f"Emergency flag status changed to: {self._emergency_flag}")

    @property  # Getter method for the current_track property
    def current_track(self):
        return self._current_track

    @current_track.setter  # Setter method for the current_track property
    def current_track(self, value):
        self._current_track = value
        print(f"Current track ID changed to: {self._current_track}")
