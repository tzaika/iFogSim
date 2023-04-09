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

            //ModuleMapping moduleMapping = generateModuleMapping(CLOUD_ONLY, createTaskLoad(FULL_LOAD));
            ModuleMapping moduleMapping = createExampleMapping();
            // ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            // moduleMapping.addModuleToDevice(LOCAL_CLIENT, "mobileDevice_1");

            // Controller controller = new Controller("cloud_only_full_load", fogDevices, sensors, actuators);
            Controller controller = new Controller("complex_full_fair", fogDevices, sensors, actuators);
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
            FogDevice localCloud = createLocalCloud("localCloud-" + i);
            localCloud.setParentId(cloud.getId());
            fogDevices.add(localCloud);
            deviceNameToId.put(localCloud.getName(), localCloud.getId());
        }

        for (int i = 1; i <= 4; i++) {
            FogDevice localServer = createLocalServer("localServer-" + i, 2);
            if (i <= 2) {
                localServer.setParentId(deviceNameToId.get("localCloud-1"));
            } else {
                localServer.setParentId(deviceNameToId.get("localCloud-2"));
                sensors.add(attachSensor(localServer, userId, appId, application, SENSOR_1));
                actuators.add(attachActuator(localServer, userId, appId, application, MOTOR_1));
            }
            fogDevices.add(localServer);
            deviceNameToId.put(localServer.getName(), localServer.getId());
        }

        for (int i = 1; i <= 6; i++) {
            FogDevice mobile = createMobile("mobileDevice-" + i, 3);
            if (i <= 2) {
                mobile.setParentId(deviceNameToId.get("localServer-1"));
            } else if (i <= 4) {
                mobile.setParentId(deviceNameToId.get("localServer-2"));
            } else {
                mobile.setLevel(2);
                mobile.setParentId(deviceNameToId.get("localCloud-2"));
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
        localCloudParam.setMipsPerPe(50000); // 20 x normal PC (8 Pe * 50 000 = 400 000 MIPS)
        localCloudParam.setNumberOfPes(8);
        localCloudParam.setRam(262144); // 256 GB
        localCloudParam.setUplinkBandwidth(2000000); // 2 Gbps
        localCloudParam.setDownlinkBandwidth(2000000);
        localCloudParam.setLevel(1);
        localCloudParam.setRatePerMips(0.0004);
        localCloudParam.setMaxBusyPower(4 * 103.0);
        localCloudParam.setIdlePowerPercent(0.15);
        localCloudParam.setHostBandwidth(2000000);
        localCloudParam.setHostStorage(4194304); // 4 TB
        localCloudParam.setUplinkLatency(150); // 150 ms

        return createFogDeviceNew(localCloudParam);
    }

    private static ModuleMapping createExampleMapping() {
        ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

        moduleMapping.addModuleToDevice(BIG_DATA, CLOUD);
        moduleMapping.addModuleToDevice(BIG_DATA, "localCloud-1");
        moduleMapping.addModuleToDevice(BIG_DATA, "localCloud-2");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer-1");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer-2");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer-3");
        moduleMapping.addModuleToDevice(LOCAL_CLIENT, "localServer-4");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice-1");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice-2");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice-3");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice-4");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice-5");
        moduleMapping.addModuleToDevice(MOBILE_CLIENT, "mobileDevice-6");

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
                    moduleMapping.addModuleToDevice(modules.get(i), "localCloud-" + cloudIndex);
                }
                break;
            case CLOUDS:
                for (int i = 0; i < modules.size(); i++) {
                    int cloudIndex = i % 3;
                    moduleMapping.addModuleToDevice(modules.get(i), cloudIndex == 0 ? CLOUD : "localCloud-" + cloudIndex);
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
