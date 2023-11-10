# imports
import json

import paho.mqtt.client as mqtt
import time

# create a client
client = mqtt.Client('hyperdrive')
# ip address - BROKER
ip_address = "192.168.4.1"
port = "1883"
# Anki vehicle nb:
ankiID = "d205effe02cb" # "d716ea410e89"


# CONNECT the client to the MQTT broker
client.connect(ip_address)
# RECONNECT function: client.reconnect(). you must have called connect() before calling this function.
# DISCONNECT function from paho mqtt: client.disconnect()
def disconnect (client: mqtt):
    client.disconnect()
    print("Disconnected")

# CONNEXION LOST error message
'''if client.on_disconnect():
    print("Connection lost")'''

#SUBSCRIBE (client as argument)
#print the message received by the subscribe function of the client class
def subscribe (client: mqtt):
    # handles messages
    def on_message(client, userdata, msg):
        print("Received {message} from {topic} topic".format(message=msg.payload.decode(), topic=msg.topic))
        client.subscribe("Anki/Vehicles/U/"+ankiID+"/S/status")
        client.on_message = on_message

#UNSUBSCRIBE function
#client.unsubscribe("Anki/Vehicles/U/"+ankiID+"/S/status")
def unsubscribe (client: mqtt):
    client.unsubscribe("Anki/Vehicles/U/"+ankiID+"/S/status")
    print("client unsubscribed from broker")


#PUBLISH (pay attention to \ before ").
def publish (client: mqtt.Client, ankiID, msg):
    topic = "Anki/Vehicles/U/"+ankiID+"/I/elise"
    client.publish(topic, json.dumps(msg))
    # handles messages
    if mqtt.MQTTMessageInfo(msg).is_published() == True:
        print("Message successfully sent:"+msg)
    else:
        print("Failed to send message")

client.loop_start()
subscribe(client)

#client.publish("Anki/Vehicles/I/"+ankiID,"{{\"type\":\"connect\",\"payload\":{{\"value\":\"true\"}}}}".format())
publish(client, ankiID,"{{\"type\":\"connect\",\"payload\":{{\"value\":\"true\"}}}}")

publish(client, ankiID,"{{\"type\":\"lights\",\"payload\":{{\"back\":\"on\"}}}}")
time.sleep(0.5)
publish(client, ankiID,"{{\"type\":\"lights\",\"payload\":{{\"back\":\"off\"}}}}")

time.sleep(4)

#disconnect(client)

client.loop_stop()

'''{{"type":"connect","payload":{{"value":"true"}}}}
{
"type":"connect",
  "payload":{
    "value":false
  }
}

{\"type\":\"connect\",\"payload\":{\"value\":false}}
'''
