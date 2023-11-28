import paho.mqtt.client as mqtt
import time


class EmergencyStop:
    def __init__(self, controller):
        self.controller = controller

    def run(self):
        client_emergency = mqtt.Client('emergency_stop_process')
        client_emergency.on_connect = self.controller.on_connect
        client_emergency.on_message = self.controller.on_message

        print(f"Connecting to broker at {self.controller.ip_address}:{self.controller.port} for emergency stop process")
        client_emergency.connect(self.controller.ip_address, port=self.controller.port)

        client_emergency.loop_start()

        try:
            last_print_time = time.time()
            while True:
                time.sleep(1)
                if self.controller.emergency_flag:
                    self.controller.stop_vehicle()
                else:
                    current_time = time.time()
                    if current_time - last_print_time >= 10:
                        print("Emergency flag status: False.")
                        last_print_time = current_time

        except KeyboardInterrupt:
            print("Emergency stop process interrupted.")
        finally:
            client_emergency.disconnect()
