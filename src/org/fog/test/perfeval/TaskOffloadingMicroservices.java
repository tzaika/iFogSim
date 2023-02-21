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
import org.fog.placement.MicroservicesController;
import org.fog.placement.PlacementLogicFactory;
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

public class TaskOffloadingMicroservices {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static List<Application> applications = new ArrayList<>();
    static Map<Integer, List<FogDevice>> monitored = new HashMap<>();

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

            List<Integer> clusterLevelIdentifier = asList(1);

            int placementAlgo = PlacementLogicFactory.DISTRIBUTED_MICROSERVICES_PLACEMENT;
            MicroservicesController microservicesController = new MicroservicesController("controller", fogDevices, sensors, applications, clusterLevelIdentifier, 0.0, placementAlgo, monitored);

            microservicesController.submitPlacementRequests(generatePlacementRequests(), 0);

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("Task offloading finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }

    }

    private static List<PlacementRequest> generatePlacementRequests() {
        List<PlacementRequest> placementRequests = new ArrayList<>();
        for (Sensor s : sensors) {
            Map<String, Integer> placedMicroservicesMap = new HashMap<>();
            int sensorGatewayDeviceId = s.getGatewayDeviceId();
            FogDevice gatewayDevice = findFogDeviceById(sensorGatewayDeviceId);
            String microserviceName = gatewayDevice != null ? getAppModuleNameByDeviceName(gatewayDevice.getName()) : "ERROR_NO_APP_MODULE_NAME_FOUND";
            placedMicroservicesMap.put(microserviceName, sensorGatewayDeviceId);
            PlacementRequest p = new PlacementRequest(s.getAppId(), s.getId(), sensorGatewayDeviceId, placedMicroservicesMap);
            placementRequests.add(p);
        }
        return placementRequests;
    }

    private static String getAppModuleNameByDeviceName(String deviceName) {
        if (deviceName.startsWith("local")) {
            return "local_client";
        } else if (deviceName.startsWith("mobile")) {
            return "mobile_client";
        } else {
            return "ERROR_NO_APP_MODULE_NAME_FOUND";
        }
    }

    private static FogDevice findFogDeviceById(int gatewayDeviceId) {
        for (FogDevice fogDevice : fogDevices) {
            if (fogDevice.getId() == gatewayDeviceId) {
                return fogDevice;
            }
        }
        return null;
    }

    private static void createTopology(int userId) {
        Application application = applications.get(0);
        String appId = application.getAppId();

        FogDevice cloud = createCloud();
        cloud.setParentId(-1);
        monitored.put(cloud.getId(), asList(cloud));
        ((MicroserviceFogDevice) cloud).setFonID(cloud.getId());
        fogDevices.add(cloud);

        FogDevice localServer = createLocalServer();
        localServer.setParentId(cloud.getId());
        monitored.put(localServer.getId(), asList(localServer));
        ((MicroserviceFogDevice) localServer).setFonID(localServer.getId());
        fogDevices.add(localServer);

        double throughput = 200;
        Sensor sensor1 = new Sensor("sensor_1", "SENSOR_1", userId, appId, new DeterministicDistribution(1000 / (throughput / 9 * 10)));
        sensor1.setApp(application);
        sensor1.setGatewayDeviceId(localServer.getId());
        sensor1.setLatency(10.0);
        sensors.add(sensor1);

        Actuator motor1 = new Actuator("motor_1", userId, appId, "MOTOR_1");
        motor1.setApp(application);
        motor1.setGatewayDeviceId(localServer.getId());
        motor1.setLatency(5.0);
        actuators.add(motor1);

        FogDevice mobile = createMobile();
        mobile.setParentId(cloud.getId());
        ((MicroserviceFogDevice) mobile).setFonID(mobile.getParentId());
        fogDevices.add(mobile);

        Sensor sensor2 = new Sensor("sensor_2", "SENSOR_2", userId, appId, new DeterministicDistribution(1000 / (throughput / 9 * 10)));
        sensor2.setApp(application);
        sensor2.setGatewayDeviceId(mobile.getId());
        sensor2.setLatency(10.0);
        sensors.add(sensor2);

        Actuator motor2 = new Actuator("motor_2", userId, appId, "MOTOR_2");
        motor2.setApp(application);
        motor2.setGatewayDeviceId(mobile.getId());
        motor2.setLatency(5.0);
        actuators.add(motor2);
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
        cloudParameter.setDeviceType(MicroserviceFogDevice.CLOUD);
        cloudParameter.setHostBandwidth(10000000);
        cloudParameter.setHostStorage(16777216); // 16 TB
        cloudParameter.setClusterLinkBandwidth(1250000);

        return createFogDeviceNew(cloudParameter);
    }

    private static FogDevice createLocalServer() {
        FogDeviceParameter lsParameter = FogDeviceParameter.getDefaultFogDevice();
        lsParameter.setName("localServerNode");
        lsParameter.setMipsPerPe(5000); // normal PC
        lsParameter.setNumberOfPes(4);
        lsParameter.setRam(131072); // 128 GB
        lsParameter.setUplinkBandwidth(1000000); // 1 Gbps
        lsParameter.setDownlinkBandwidth(1000000);
        lsParameter.setLevel(1);
        lsParameter.setRatePerMips(0.0);
        lsParameter.setBusyPower(107.339);
        lsParameter.setIdlePower(83.4333);
        lsParameter.setDeviceType(MicroserviceFogDevice.FON);
        lsParameter.setHostBandwidth(1000000);
        lsParameter.setHostStorage(1048576); // 1 TB
        lsParameter.setClusterLinkBandwidth(1250000);
        lsParameter.setUplinkLatency(150); // 150 ms

        return createFogDeviceNew(lsParameter);
    }

    private static FogDevice createMobile() {
        FogDeviceParameter mobileParameter = FogDeviceParameter.getDefaultFogDevice();
        mobileParameter.setName("mobileNode");
        mobileParameter.setMipsPerPe(1000); // normal mobile
        mobileParameter.setNumberOfPes(4);
        mobileParameter.setRam(4096); // 4 GB
        mobileParameter.setUplinkBandwidth(10000); // 10 Mbps
        mobileParameter.setDownlinkBandwidth(5000); // 5 Mbps
        mobileParameter.setLevel(1);
        mobileParameter.setRatePerMips(0.0);
        mobileParameter.setBusyPower(87.53);
        mobileParameter.setIdlePower(82.44);
        mobileParameter.setDeviceType(MicroserviceFogDevice.CLIENT);
        mobileParameter.setHostBandwidth(10000);
        mobileParameter.setHostStorage(65536); // 64 GB
        mobileParameter.setClusterLinkBandwidth(1250000);
        mobileParameter.setUplinkLatency(200); // 200 ms

        return createFogDeviceNew(mobileParameter);
    }


    private static MicroserviceFogDevice createFogDeviceNew(FogDeviceParameter p) {

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
            MicroserviceFogDevice fogDevice = new MicroserviceFogDevice(p.getName(), characteristics, new AppModuleAllocationPolicy(hostList), emptyList(), p.getSchedulingInterval(), p.getUplinkBandwidth(), p.getDownlinkBandwidth(), p.getClusterLinkBandwidth(), p.getUplinkLatency(), p.getRatePerMips(), p.getDeviceType());
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
        application.addAppEdge("local_client", "MOTOR_1", 1000, 1024, "ACTION", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("mobile_client", "MOTOR_2", 1000, 1024, "ACTION", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("local_client", "big_data", 100, 6000, 131072, "SENSOR_VALUES", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mobile_client", "big_data", 100, 3000, 65536, "SENSOR_VALUES", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("big_data", "local_client", 100, 1000, 4096, "ANALYSIS", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("big_data", "mobile_client", 100, 1000, 4096, "ANALYSIS", Tuple.DOWN, AppEdge.MODULE);

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
                createAppLoop("local_client", "big_data", "local_client"),
                createAppLoop("mobile_client", "big_data")));

        application.setSpecialPlacementInfo("big_data", "cloud");
        application.createDAG();
        return application;
    }
}
