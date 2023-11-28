import tkinter as tk


class GUI:
    def __init__(self, vehicle_control):
        self.vehicle_control = vehicle_control
        self.create_tkinter_window()

    def create_tkinter_window(self):
        app = tk.Tk()
        app.title("Emergency Flag Controller")

        button = tk.Button(app, text="Toggle Emergency Flag", command=self.vehicle_control.change_flag_status)
        button.pack(pady=10)

        app.mainloop()
