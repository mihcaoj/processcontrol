/**
 * The provided code demonstrates a classic example of asynchronous programming. Here, in 
 * the context of controlling and managing a vehicle system through the MQTT protocol. 
 * The code shows how to discover, connect, and control vehicles' lights using multiple threads.
 * 
 * <p>Let's break down the core concepts and mechanisms involved:</p>
 * 
 * <h3>1. MQTT (Message Queuing Telemetry Transport):</h3>
 * MQTT is a lightweight publish-subscribe messaging protocol, often used for IoT 
 * applications due to its efficient use of bandwidth and energy.
 * 
 * <h3>2. Asynchronous Programming:</h3>
 * The code uses multiple threads to perform different tasks concurrently. For instance, 
 * one thread is responsible for discovering vehicles, another for connecting to a 
 * vehicle, and another for controlling the vehicle's lights.
 * 
 * <h3>3. Inter-Thread Communication:</h3>
 * Different threads need to communicate to synchronize their operations:
 * <ul>
 *   <li>{@code BlockingQueue} is used for this purpose. It is a thread-safe queue that 
 *   blocks a thread if it tries to dequeue an element from an empty queue or enqueue an 
 *   element into a full queue. In this code, {@code BlockingQueue} is used to pass vehicle 
 *   IDs between threads.</li>
 *   <li>Threads are interrupted using the {@code interrupt()} method to signal them to stop 
 *   their execution.</li>
 * </ul>
 * 
 * <h3>Code Flow:</h3>
 * 
 * <h4>1. Main.java:</h4>
 * <ul>
 *   <li>Initializes the MQTT handler.</li>
 *   <li>Creates and starts three threads: {@code blinkerThread}, {@code connectThread}, 
 *   and {@code discovererThread}.</li>
 *   <li>Sends a "discover" intent to find vehicles.</li>
 *   <li>Waits for user input, then interrupts all threads.</li>
 * </ul>
 * 
 * <h4>2. MqttHandler.java:</h4>
 * <ul>
 *   <li>Manages MQTT connections, subscriptions, and publications.</li>
 *   <li>Distributes incoming MQTT messages to all registered {@code MessageListener} instances.</li>
 * </ul>
 * 
 * <h4>3. VehicleDiscoverObserver.java:</h4>
 * <ul>
 *   <li>Subscribes to the topic for vehicle discovery.</li>
 *   <li>Waits for a vehicle to be discovered.</li>
 *   <li>Sends a "connect" intent for the discovered vehicle.</li>
 *   <li>Passes the vehicle ID to {@code VehicleConnectedObserver}.</li>
 * </ul>
 * 
 * <h4>4. VehicleConnectedObserver.java:</h4>
 * <ul>
 *   <li>Subscribes to the status topic of the vehicle.</li>
 *   <li>Waits for the vehicle's status to become "ready".</li>
 *   <li>Passes the vehicle ID to {@code VehicleFrontBlinker}.</li>
 * </ul>
 * 
 * <h4>5. VehicleFrontBlinker.java:</h4>
 * <ul>
 *   <li>Waits for a vehicle ID.</li>
 *   <li>Sends intents to turn the vehicle's front lights on and off in a loop.</li>
 * </ul>
 * 
 * <h3>Asynchronous Programming Concepts Demonstrated:</h3>
 * <ul>
 *   <li>Callbacks: Used in {@code MqttHandler} to handle asynchronous events like message arrivals.</li>
 *   <li>Blocking Operations: {@code BlockingQueue.take()} method is used, which blocks the current thread 
 *   until an item is available.</li>
 *   <li>Concurrency: Multiple threads work concurrently to perform tasks like discovery, connection, 
 *   and light control.</li>
 *   <li>Synchronization: Threads synchronize their operations using shared resources like {@code BlockingQueue}.</li>
 *   <li>Thread Interruption: Threads are interrupted to stop their execution.</li>
 * </ul>
 * 
 * <p>Conclusion:</p>
 * 
 * <p>The code demonstrates how to manage and control vehicles asynchronously using multiple threads and 
 * the MQTT protocol. It showcases essential asynchronous programming concepts like callbacks, blocking 
 * operations, concurrency, synchronization, and thread interruption. The use of {@code BlockingQueue} 
 * for inter-thread communication ensures that threads work in a coordinated manner.</p>
 */

package mqtt_blinking_prof_code.strongcoupling;
