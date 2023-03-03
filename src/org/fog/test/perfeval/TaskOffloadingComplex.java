package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.FogDeviceParameter;
import org.fog.utils.TimeKeeper;

import java.util.*;

import static org.fog.test.perfeval.TaskOffloadingSimple.*;

public class TaskOffloadingComplex {

    public static final String LOW_LOAD = "low";
    public static final String MEDIUM_LOAD = "medium";
    public static final String FULL_LOAD = "full";
    public static final String CLOUD_ONLY = "cloudOnly";
    public static final String LOCAL_CLOUD = "localCloud";
    public static final String CLOUDS = "clouds";
    public static final String LOCAL_SERVERS = "localServers";
    public static final String MOBILE_DEVICES = "mobileDevices";
    private static final Map<String, Integer> deviceNameToId = new HashMap<>();
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static List<Application> applications = new ArrayList<>();

    public static void main(String[] args) {

        try {
            Log.disable();

            CloudSim.init(1, Calendar.getInstance(), true);

            FogBroker broker = new FogBroker("broker");
            int userId = broker.getId();

            // APPLICATION
            String appId = "PDM-Demonstrator";
            Application application = createApplication(appId, userId);
            application.setUserId(userId);
            applications.add(application);

            createTopology(userId);

            // ModuleMapping moduleMapping = generateModuleMapping(CLOUD_ONLY, createTaskLoad(FULL_LOAD));
            ModuleMapping moduleMapping = createExampleMapping();
            // ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            // moduleMapping.addModuleToDevice(LOCAL_CLIENT, "mobileDevice_1");

            Controller controller = new Controller("controller", fogDevices, sensors, actuators);
            controller.submitApplication(application, new ModulePlacementMapping(fogDevices, application, moduleMapping));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("Task offloading finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }

    }

    private static void createTopology(int userId) {
        Application application = applications.get(0);
        String appId = application.getAppId();

        FogDevice cloud = createCloud();
        cloud.setParentId(-1);
        fogDevices.add(cloud);
        deviceNameToId.put(cloud.getName(), cloud.getId());

        for (int i = 1; i <= 2; i++) {
            FogDevice localCloud = createLocalCloud("localCloud_" + i);
            localCloud.setParentId(cloud.getId());
            fogDevices.add(localCloud);
            deviceNameToId.put(localCloud.getName(), localCloud.getId());
        }

        for (int i = 1; i <= 4; i++) {
            FogDevice localServer = createLocalServer("localServer_" + i, 2);
            if (i <= 2) {
                localServer.setParentId(deviceNameToId.get("localCloud_1"));
            } else {
                localServer.setParentId(deviceNameToId.get("localCloud_2"));
                sensors.add(attachSensor(localServer, userId, appId, application, SENSOR_1));
                actuators.add(attachActuator(localServer, userId, appId, application, MOTOR_1));
            }
            fogDevices.add(localServer);
            deviceNameToId.put(localServer.getName(), localServer.getId());
        }

        for (int i = 1; i <= 6; i++) {
            FogDevice mobile = createMobile("mobileDevice_" + i, 3);
            if (i <= 2) {
                mobile.setParentId(deviceNameToId.get("localServer_1"));
            } else if (i <= 4) {
                mobile.setParentId(deviceNameToId.get("localServer_2"));
            } else {
                mobile.setLevel(2);
                mobile.setParentId(deviceNameToId.get("localCloud_2"));
            }
            sensors.add(attachSensor(mobile, userId, appId, application, SENSOR_2));
            actuators.add(attachActuator(mobile, userId, appId, application, MOTOR_2));
            fogDevices.add(mobile);
            deviceNameToId.put(mobile.getName(), mobile.getId());
        }
    }

    private static FogDevice createLocalCloud(String name) {
        FogDeviceParameter localCloudParam = FogDeviceParameter.getDefaultFogDevice();
        localCloudParam.setName(name);
        localCloudParam.setMipsPerPe(10000); // 2 x normal PC
        localCloudParam.setNumberOfPes(8);
        localCloudParam.setRam(262144); // 256 GB
        localCloudParam.setUplinkBandwidth(2000000); // 2 Gbps
        localCloudParam.setDownlinkBandwidth(2000000);
        localCloudParam.setLevel(1);
        localCloudParam.setRatePerMips(0.0004);
        localCloudParam.setBusyPower(4 * 103.0);
        localCloudParam.setIdlePower(4 * 83.25);
        localCloudParam.setHostBandwidth(2000000);
        localCloudParam.setHostStorage(4194304); // 4 TB
        localCloudParam.setUplinkLatency(150); // 150 ms

        return createFogDeviceNew(localCloudParam);
    }

    private static ModuleMapping createExampleMapping() {
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

        moduleMapping.addModuleToDevice(BIG_DATA, CLOUD);
        moduleMapping.addModuleToDevice(BIG_DATA, "localCloud_1");
        moduleMapping.addModuleToDevice(BIG_DATA, "localCloud_2");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer_1");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer_2");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer_3");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer_4");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice_1");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice_2");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice_3");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice_4");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice_5");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice_6");

        return moduleMapping;
    }

    private static List<String> createTaskLoad(String loadType) {
        switch (loadType) {
            case LOW_LOAD:
                return Arrays.asList(BIG_DATA, LOCAL_CLIENT, MOBILE_CLIENT);

            case MEDIUM_LOAD:
                return Arrays.asList(BIG_DATA, BIG_DATA, LOCAL_CLIENT, LOCAL_CLIENT,
                        MOBILE_CLIENT, MOBILE_CLIENT, MOBILE_CLIENT);

            case FULL_LOAD:
                return Arrays.asList(BIG_DATA, BIG_DATA, BIG_DATA, LOCAL_CLIENT, LOCAL_CLIENT, LOCAL_CLIENT, LOCAL_CLIENT,
                        MOBILE_CLIENT, MOBILE_CLIENT, MOBILE_CLIENT, MOBILE_CLIENT, MOBILE_CLIENT, MOBILE_CLIENT);

            default:
                return Collections.emptyList();

            // TODO: Add overbooked (wiht mobile clients)
        }
    }

    private static ModuleMapping generateModuleMapping(String locationStrategy, List<String> modules) {
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
        switch (locationStrategy) {
            case CLOUD_ONLY:
                for (String module : modules) {
                    moduleMapping.addModuleToDevice(module, CLOUD);
                }
                break;
            case LOCAL_CLOUD:
                for (int i = 0; i < modules.size(); i++) {
                    int cloudIndex = (i % 2) + 1;
                    moduleMapping.addModuleToDevice(modules.get(i), "localCloud_" + cloudIndex);
                }
                break;
            case CLOUDS:
                for (int i = 0; i < modules.size(); i++) {
                    int cloudIndex = i % 3;
                    moduleMapping.addModuleToDevice(modules.get(i), cloudIndex == 0 ? CLOUD : "localCloud_" + cloudIndex);
                }
                break;
            case LOCAL_SERVERS:
                // TODO: Create local server mapping
                break;
            case MOBILE_DEVICES:
                // TODO: Create mobile device mapping
                break;
            default:
                break;
        }
        return moduleMapping;
    }

}
