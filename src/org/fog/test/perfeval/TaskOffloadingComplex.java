package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogDeviceParameter;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fog.application.AppLoop.createAppLoop;

public class TaskOffloadingComplex {

    private static final double SENSOR_THROUGHPUT = 200;
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static List<Application> applications = new ArrayList<>();
    private static Map<String, Integer> deviceNameToId = new HashMap<>();

    public static void main(String[] args) {

        try {
            Log.enable();

            CloudSim.init(1, Calendar.getInstance(), true);

            FogBroker broker = new FogBroker("broker");
            int userId = broker.getId();

            // APPLICATION
            String appId = "PDM-Demonstrator";
            Application application = createApplication(appId, userId);
            application.setUserId(userId);
            applications.add(application);

            createTopology(userId);

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("big_data", "cloud");
            moduleMapping.addModuleToDevice("big_data", "localCloud_1");
            moduleMapping.addModuleToDevice("big_data", "localCloud_2");
            moduleMapping.addModuleToDevice("local_client", "localServer_1");
            moduleMapping.addModuleToDevice("local_client", "localServer_2");
            moduleMapping.addModuleToDevice("local_client", "localServer_3");
            moduleMapping.addModuleToDevice("local_client", "localServer_4");
            moduleMapping.addModuleToDevice("mobile_client", "mobileDevice_1");
            moduleMapping.addModuleToDevice("mobile_client", "mobileDevice_2");
            moduleMapping.addModuleToDevice("mobile_client", "mobileDevice_3");
            moduleMapping.addModuleToDevice("mobile_client", "mobileDevice_4");
            moduleMapping.addModuleToDevice("mobile_client", "mobileDevice_5");
            moduleMapping.addModuleToDevice("mobile_client", "mobileDevice_6");

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
            FogDevice localCloud = createLocalCloud("localCloud_" + i, 1);
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
                attachSensorAndActuator(localServer, userId, appId, application, "SENSOR_1", "MOTOR_1");
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
            attachSensorAndActuator(mobile, userId, appId, application, "SENSOR_2", "MOTOR_2");
            fogDevices.add(mobile);
            deviceNameToId.put(mobile.getName(), mobile.getId());
        }
    }

    private static void attachSensorAndActuator(FogDevice fogDevice, int userId, String appId, Application application, String sensorType, String actuatorType) {
        Sensor sensor = new Sensor(fogDevice.getName() + "_sensor", sensorType, userId, appId, new DeterministicDistribution(1000 / (SENSOR_THROUGHPUT / 9 * 10)));
        sensor.setApp(application);
        sensor.setGatewayDeviceId(fogDevice.getId());
        sensor.setLatency(10.0);
        sensors.add(sensor);

        Actuator motor = new Actuator(fogDevice.getName() + "_motor", userId, appId, actuatorType);
        motor.setApp(application);
        motor.setGatewayDeviceId(fogDevice.getId());
        motor.setLatency(5.0);
        actuators.add(motor);
    }

    private static FogDevice createCloud() {
        FogDeviceParameter cloudParameter = FogDeviceParameter.getDefaultFogDevice();
        cloudParameter.setName("cloud");
        cloudParameter.setMipsPerPe(50000); // 10 x normal PC
        cloudParameter.setNumberOfPes(10);
        cloudParameter.setRam(1048576); // 1 TB
        cloudParameter.setUplinkBandwidth(10000000); // 10 Gbps
        cloudParameter.setDownlinkBandwidth(10000000);
        cloudParameter.setLevel(0);
        cloudParameter.setRatePerMips(0.01);
        cloudParameter.setBusyPower(16 * 103);
        cloudParameter.setIdlePower(16 * 83.25);
        cloudParameter.setHostBandwidth(10000000);
        cloudParameter.setHostStorage(16777216); // 16 TB

        return createFogDeviceNew(cloudParameter);
    }

    private static FogDevice createLocalCloud(String name, int level) {
        FogDeviceParameter localCloudParam = FogDeviceParameter.getDefaultFogDevice();
        localCloudParam.setName(name);
        localCloudParam.setMipsPerPe(10000); // 2 x normal PC
        localCloudParam.setNumberOfPes(8);
        localCloudParam.setRam(262144); // 256 GB
        localCloudParam.setUplinkBandwidth(2000000); // 2 Gbps
        localCloudParam.setDownlinkBandwidth(2000000);
        localCloudParam.setLevel(level);
        localCloudParam.setRatePerMips(0.01);
        localCloudParam.setBusyPower(4 * 103);
        localCloudParam.setIdlePower(4 * 83.25);
        localCloudParam.setHostBandwidth(2000000);
        localCloudParam.setHostStorage(4194304); // 4 TB
        localCloudParam.setUplinkLatency(25); // 25 ms

        return createFogDeviceNew(localCloudParam);
    }

    private static FogDevice createLocalServer(String name, int level) {
        FogDeviceParameter lsParameter = FogDeviceParameter.getDefaultFogDevice();
        lsParameter.setName(name);
        lsParameter.setMipsPerPe(5000); // normal PC
        lsParameter.setNumberOfPes(4);
        lsParameter.setRam(131072); // 128 GB
        lsParameter.setUplinkBandwidth(1000000); // 1 Gbps
        lsParameter.setDownlinkBandwidth(1000000);
        lsParameter.setLevel(level);
        lsParameter.setRatePerMips(0.0);
        lsParameter.setBusyPower(107.339);
        lsParameter.setIdlePower(83.4333);
        lsParameter.setHostBandwidth(1000000);
        lsParameter.setHostStorage(1048576); // 1 TB
        lsParameter.setUplinkLatency(150); // 150 ms

        return createFogDeviceNew(lsParameter);
    }

    private static FogDevice createMobile(String name, int level) {
        FogDeviceParameter mobileParameter = FogDeviceParameter.getDefaultFogDevice();
        mobileParameter.setName(name);
        mobileParameter.setMipsPerPe(1000); // normal mobile
        mobileParameter.setNumberOfPes(4);
        mobileParameter.setRam(4096); // 4 GB
        mobileParameter.setUplinkBandwidth(10000); // 10 Mbps
        mobileParameter.setDownlinkBandwidth(5000); // 5 Mbps
        mobileParameter.setLevel(level);
        mobileParameter.setRatePerMips(0.0);
        mobileParameter.setBusyPower(87.53);
        mobileParameter.setIdlePower(82.44);
        mobileParameter.setHostBandwidth(10000);
        mobileParameter.setHostStorage(65536); // 64 GB
        mobileParameter.setUplinkLatency(200); // 200 ms

        return createFogDeviceNew(mobileParameter);
    }


    private static FogDevice createFogDeviceNew(FogDeviceParameter p) {

        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < p.getNumberOfPes(); i++) {
            peList.add(new Pe(i, new PeProvisionerSimple(p.getMipsPerPe())));
        }

        PowerHost host = new PowerHost(
                FogUtils.generateEntityId(),
                new RamProvisionerSimple(p.getRam()),
                new BwProvisionerOverbooking(p.getHostBandwidth()),
                p.getHostStorage(),
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(p.getBusyPower(), p.getIdlePower())
        );

        List<Host> hostList = asList(host);

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(p.getSystemArchitecture(),
                p.getSystemOS(), p.getSystemVmm(), host, p.getSystemTimezone(), p.getProcessingCost(), p.getMemoryCost(), p.getStorageCost(), p.getBandwidthCost());

        try {
            FogDevice fogDevice = new FogDevice(p.getName(), characteristics, new AppModuleAllocationPolicy(hostList), emptyList(), p.getSchedulingInterval(), p.getUplinkBandwidth(), p.getDownlinkBandwidth(), p.getUplinkLatency(), p.getRatePerMips());
            fogDevice.setLevel(p.getLevel());
            return fogDevice;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        // Adding modules (vertices) to the application model (directed graph)
        application.addAppModule("big_data", 2048, 2000, 16384, 100000, 4); // ram in MB, MIPS, storageSize in MB, bw in kbps
        application.addAppModule("local_client", 512, 1000, 8192, 100000, 2);
        application.addAppModule("mobile_client", 256, 500, 2048, 10000, 1);

        // Connecting the application modules (vertices) in the application model (directed graph) with edges
        application.addAppEdge("SENSOR_1", "local_client", 100, 2048, "SENSOR_1", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("SENSOR_2", "mobile_client", 100, 2048, "SENSOR_2", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("local_client", "big_data", 100, 6000, 131072, "SENSOR_VALUES", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mobile_client", "big_data", 100, 3000, 65536, "SENSOR_VALUES", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("big_data", "local_client", 100, 1000, 4096, "ANALYSIS", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("big_data", "mobile_client", 100, 1000, 4096, "ANALYSIS", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("local_client", "MOTOR_1", 1000, 1024, "ACTION", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("mobile_client", "MOTOR_2", 1000, 1024, "ACTION", Tuple.DOWN, AppEdge.ACTUATOR);

        // Defining the input-output relationships (represented by selectivity) of the application modules.
        application.addTupleMapping("local_client", "SENSOR_1", "ACTION", new FractionalSelectivity(1.0));
        application.addTupleMapping("mobile_client", "SENSOR_2", "ACTION", new FractionalSelectivity(1.0));
        application.addTupleMapping("big_data", "SENSOR_VALUES", "ANALYSIS", new FractionalSelectivity(1.0));
        application.addTupleMapping("local_client", "ANALYSIS", "ACTION", new FractionalSelectivity(1.0));
        application.addTupleMapping("mobile_client", "ANALYSIS", "ACTION", new FractionalSelectivity(1.0));

        //Defining application loops to monitor the latency of.
        application.setLoops(asList(
                createAppLoop("local_client", "MOTOR_1"),
                createAppLoop("mobile_client", "MOTOR_2"),
                createAppLoop("big_data", "local_client"),
                createAppLoop("mobile_client", "big_data"),
                createAppLoop("local_client", "big_data", "mobile_client")));

        application.setSpecialPlacementInfo("big_data", "cloud");
        application.createDAG();
        return application;
    }
}
