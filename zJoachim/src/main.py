from anki_controller import AnkiController
from vehicle_control import VehicleControl
from emergency_stop import EmergencyStop
from tkinter_gui import GUI
import threading


def main():
    ip_address = '192.168.4.1'
    port = 1883
    vehicle_id = 'd205effe02cb'

    controller = AnkiController(ip_address, port, vehicle_id)
    vehicle_control = VehicleControl(controller)
    emergency_process = EmergencyStop(controller)
    tkinter_gui = GUI(vehicle_control)

    # Initialize thread variables
    blink_thread = threading.Thread(target=vehicle_control.blink_lights, args=(vehicle_id,))
    drive_thread = threading.Thread(target=vehicle_control.drive_car, args=(vehicle_id,))
    change_lane_thread = threading.Thread(target=vehicle_control.change_lane, args=(vehicle_id,))
    emergency_thread = threading.Thread(target=emergency_process.run)
    tkinter_thread = threading.Thread(target=tkinter_gui.create_tkinter_window)

    try:
        # Start the threads
        blink_thread.start()
        drive_thread.start()
        change_lane_thread.start()
        emergency_thread.start()

        # Run the GUI
        tkinter_thread.start()

    except KeyboardInterrupt:
        print("Main program interrupted.")
    finally:
        # Stop the threads and perform cleanup
        vehicle_control.stop_vehicle()

        # Wait for threads to finish
        blink_thread.join()
        drive_thread.join()
        change_lane_thread.join()
        emergency_thread.join()

        # Disconnect the MQTT controller
        controller.disconnect()

        # Stop the MQTT client loop
        controller.client.loop_stop()


if __name__ == "__main__":
    main()
