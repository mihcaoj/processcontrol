import paho.mqtt.client as mqtt
import time


# Class for handling Emergency Stop functionality
class EmergencyStop:
    def __init__(self, controller):
        self.controller = controller
        self.client_emergency = controller.client

    # Method to run the emergency stop process
    def run(self):
        client_emergency = self.controller.client
        client_emergency.on_connect = self.controller.on_connect
        client_emergency.on_message = self.controller.on_message

        print(f"Connecting to broker at {self.controller.ip_address}:{self.controller.port} for emergency stop process")
        self.client_emergency.connect(self.controller.ip_address, port=self.controller.port)
        self.client_emergency.loop_start()

        try:
            last_print_time = time.time()
            while True:
                if self.controller.emergency_flag:
                    self.controller.stop_vehicle()
                else:
                    # If the flag is not raised, print status updates periodically
                    current_time = time.time()
                    if current_time - last_print_time >= 10:
                        print("Emergency flag status: False.")
                        last_print_time = current_time

        except KeyboardInterrupt:
            print("Emergency stop process interrupted.")
        finally:
            self.client_emergency.disconnect()
