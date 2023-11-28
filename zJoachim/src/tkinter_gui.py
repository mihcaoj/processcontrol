import tkinter as tk


class GUI:
    def __init__(self, vehicle_control):
        self.vehicle_control = vehicle_control
        self.create_tkinter_window()

    def create_tkinter_window(self):
        app = tk.Tk()
        app.title("Emergency Flag Controller")

        # Button to toggle the emergency flag
        button = tk.Button(app, text="Toggle Emergency Flag", command=self.vehicle_control.change_flag_status)
        button.pack(pady=10)

        # Slider for changing the velocity
        velocity_label = tk.Label(app, text="Velocity:")
        velocity_label.pack()
        velocity_slider = tk.Scale(app, from_=0, to=100, orient=tk.HORIZONTAL, command=self.update_velocity)
        velocity_slider.pack()

        # Slider for changing acceleration
        acceleration_label = tk.Label(app, text="Acceleration:")
        acceleration_label.pack()
        acceleration_slider = tk.Scale(app, from_=0, to=100, orient=tk.HORIZONTAL, command=self.update_acceleration)
        acceleration_slider.pack()

        app.mainloop()
