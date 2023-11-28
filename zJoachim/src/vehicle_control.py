import json
import time
import random


# Class for controlling the vehicles
class VehicleControl:
    def __init__(self, controller):
        self.emergency_flag = False
        self.controller = controller

    def blink_lights(self, vehicle_id):
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
            print(f"Start the blink on vehicle: {vehicle_id}")

            while True:
                self.controller.publish(f"Anki/Vehicles/U/{vehicle_id}/I/jb", payload_on)
                print(f"Published: {payload_on} to {vehicle_id}")
                time.sleep(1)

                self.controller.publish(f"Anki/Vehicles/U/{vehicle_id}/I/jb", payload_off)
                print(f"Published: {payload_off} to {vehicle_id}")
                time.sleep(1)

        except KeyboardInterrupt:
            print("Blinking interrupted. Stopping the lights.")
        finally:
            self.controller.disconnect()

    def drive_car(self, vehicle_id):
        try:
            print(f"Start driving the car: {vehicle_id}")

            while True:
                if not self.controller.emergency_flag:
                    velocity = 500  # random.randint(-100, 2000)
                    acceleration = 500  # random.randint(0, 2000)
                else:
                    velocity = 0
                    acceleration = 0

                payload_speed = {
                    "type": "speed",
                    "payload": {
                        "velocity": velocity,
                        "acceleration": acceleration
                    }
                }

                self.controller.publish(f"Anki/Vehicles/U/{vehicle_id}/I/jb", payload_speed)
                print(f"Published speed: {payload_speed}")

                time.sleep(3)

        except KeyboardInterrupt:
            print("Driving interrupted. Stopping the car.")
        finally:
            self.controller.disconnect()

    def change_lane(self, vehicle_id):
        try:
            print(f"Driving interrupted. Stopping vehicle: " + vehicle_id)

            while True:
                if not self.controller.emergency_flag:
                    offset = 500  # random.randint(-1000, 1000)
                    velocity = 500  # random.randint(0, 1000)
                    acceleration = 500  # random.randint(0, 2000)
                else:
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

                self.controller.publish(f"Anki/Vehicles/U/{vehicle_id}/I/jb", payload_lane)
                print(f"Published lane change: {payload_lane}")

                time.sleep(5)

        except KeyboardInterrupt:
            print("Lane change interrupted. Stopping vehicle: " + vehicle_id)
        finally:
            self.controller.disconnect()

    def stop_vehicle(self):
        self.controller.stop_vehicle()

    def publish(self, topic, payload):
        message = json.dumps(payload)
        self.controller.client.publish(topic, message)
        print(f"Published to topic {topic} with {payload}")

    def change_flag_status(self):
        self.emergency_flag = not self.emergency_flag
        print(f"Emergency flag status changed to: {self.emergency_flag}")
