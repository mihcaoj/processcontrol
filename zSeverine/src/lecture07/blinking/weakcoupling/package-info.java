/**
 * The second code also aims to control a vehicle system through MQTT, just like the first one. 
 * However, there are certain differences in the structure and the design of the two. 
 * Let's dive deep into the comparative analysis between the two:
 * 
 * <h3>Design Philosophy:</h3>
 * <ul>
 *     <li><b>Strong Coupling (First Code):</b>
 *         <ul>
 *             <li>The different components (observers and blinker) are intertwined and directly interact with each other.</li>
 *             <li>There's a direct dependency between classes. For example, {@code VehicleDiscoverObserver} directly informs the {@code VehicleConnectedObserver} about the discovered vehicle.</li>
 *         </ul>
 *     </li>
 *     <li><b>Weak Coupling (Second Code):</b>
 *         <ul>
 *             <li>The components (observers and blinker) are decoupled and interact through MQTT messages.</li>
 *             <li>The {@code VehicleDiscoverObserver} communicates the ID of the discovered vehicle by publishing it to an MQTT topic. The {@code VehicleConnectedObserver} then subscribes to this topic to get the vehicle ID.</li>
 *             <li>The advantage of this approach is modularity. Components can be added, removed, or replaced without impacting others.</li>
 *         </ul>
 *     </li>
 * </ul>
 * 
 * <h3>Flow of Execution:</h3>
 * <ul>
 *     <li><b>Strong Coupling:</b>
 *         <ul>
 *             <li>{@code VehicleDiscoverObserver} discovers a vehicle and directly passes the vehicle ID to {@code VehicleConnectedObserver}.</li>
 *             <li>{@code VehicleConnectedObserver} waits for the vehicle to be ready and then informs the {@code VehicleFrontBlinker} to start blinking the lights.</li>
 *         </ul>
 *     </li>
 *     <li><b>Weak Coupling:</b>
 *         <ul>
 *             <li>{@code VehicleDiscoverObserver} discovers a vehicle and then publishes the vehicle ID to a specific MQTT topic.</li>
 *             <li>{@code VehicleConnectedObserver} listens to the aforementioned topic, gets the vehicle ID, waits for the vehicle to be ready, and then publishes the vehicle's connected status to another MQTT topic.</li>
 *             <li>{@code VehicleFrontBlinker} listens to the vehicle connected topic and starts blinking the lights upon receiving the vehicle ID.</li>
 *         </ul>
 *     </li>
 * </ul>
 * 
 * <h3>Advantages of Weak Coupling:</h3>
 * <ul>
 *     <li><b>Modularity:</b> Components are modular and can operate independently. You can add more observers or replace existing ones without affecting the rest of the system.</li>
 *     <li><b>Flexibility:</b> It's easier to make changes in a weakly coupled system.</li>
 *     <li><b>Scalability:</b> New components can be added seamlessly. For instance, if a new feature is to be added that requires knowing when a vehicle is discovered, a new observer can be created that listens to the vehicle discovered topic without altering existing code.</li>
 * </ul>
 * 
 * <h3>Drawbacks:</h3>
 * <ul>
 *     <li><b>Complexity:</b> Weak coupling might add a layer of complexity because of the indirect communication between components.</li>
 *     <li><b>Latency:</b> There might be slight latency due to the round-trip of MQTT messages.</li>
 * </ul>
 * 
 * <h3>Conclusion:</h3>
 * <p>Both approaches have their merits. The strong coupling approach has direct interactions and might be easier to understand and debug. However, it lacks the modularity that comes with the weak coupling approach.</p>
 * <p>On the other hand, the weak coupling approach, while being more modular and flexible, might be a bit more complex due to the indirect communication between components.</p>
 * <p>Choosing between the two depends on the specific requirements and future plans for the system. If the system is expected to evolve and scale, a weakly coupled design might be more beneficial. If the system is relatively static and simplicity is a priority, a strongly coupled design might be more appropriate.</p>
 */

package lecture07.blinking.weakcoupling;
