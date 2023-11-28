package java_code.setup;

import java.util.Hashtable;

// Dictionary with key: topicName and value: topicPath
public class TopicDictionary {
        public Hashtable<String, String> topicPathByName;

        public TopicDictionary(String clientId, String vehicleId) {
            topicPathByName = new Hashtable<>();
            topicPathByName.put("HostIntent","Anki/Hosts/U/hyperdrive/I/"+clientId);
            topicPathByName.put("HostDiscoveringStatus","Anki/Hosts/U/hyperdrive/S/discovering");
            topicPathByName.put("HostVehiclesStatus","Anki/Hosts/U/hyperdrive/S/vehicles");
            topicPathByName.put("singleVehicleIntent","Anki/Vehicles/U/"+vehicleId+"/I/"+clientId);
            topicPathByName.put("singleVehicleStatus","Anki/Vehicles/U/"+vehicleId+"/S/status");
        }
}
