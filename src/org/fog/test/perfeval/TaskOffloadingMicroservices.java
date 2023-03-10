package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.fog.application.Application;
import org.fog.entities.*;
import org.fog.placement.MicroservicesController;
import org.fog.placement.PlacementLogicFactory;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogDeviceParameter;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fog.test.perfeval.TaskOffloadingSimple.*;

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
            Application application = createApplication(APP_ID_PDM_DEMO, userId);
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
            return LOCAL_CLIENT;
        } else if (deviceName.startsWith("mobile")) {
            return MOBILE_CLIENT;
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
        Sensor sensor1 = new Sensor("sensor_1", SENSOR_1, userId, appId, new DeterministicDistribution(1000 / (throughput / 9 * 10)));
        sensor1.setApp(application);
        sensor1.setGatewayDeviceId(localServer.getId());
        sensor1.setLatency(10.0);
        sensors.add(sensor1);

        Actuator motor1 = new Actuator("motor_1", userId, appId, MOTOR_1);
        motor1.setApp(application);
        motor1.setGatewayDeviceId(localServer.getId());
        motor1.setLatency(5.0);
        actuators.add(motor1);

        FogDevice mobile = createMobile();
        mobile.setParentId(cloud.getId());
        ((MicroserviceFogDevice) mobile).setFonID(mobile.getParentId());
        fogDevices.add(mobile);

        Sensor sensor2 = new Sensor("sensor_2", SENSOR_2, userId, appId, new DeterministicDistribution(1000 / (throughput / 9 * 10)));
        sensor2.setApp(application);
        sensor2.setGatewayDeviceId(mobile.getId());
        sensor2.setLatency(10.0);
        sensors.add(sensor2);

        Actuator motor2 = new Actuator("motor_2", userId, appId, MOTOR_2);
        motor2.setApp(application);
        motor2.setGatewayDeviceId(mobile.getId());
        motor2.setLatency(5.0);
        actuators.add(motor2);
    }

    private static FogDevice createCloud() {
        FogDeviceParameter cloudParameter = FogDeviceParameter.getDefaultFogDevice();
        cloudParameter.setName(CLOUD);
        cloudParameter.setMipsPerPe(50000); // 10 x normal PC
        cloudParameter.setNumberOfPes(10);
        cloudParameter.setRam(1048576); // 1 TB
        cloudParameter.setUplinkBandwidth(10000000); // 10 Gbps
        cloudParameter.setDownlinkBandwidth(10000000);
        cloudParameter.setLevel(0);
        cloudParameter.setRatePerMips(0.01);
        cloudParameter.setMaxBusyPower(16 * 103.0);
        cloudParameter.setIdlePowerPercent(0.1);
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
        lsParameter.setMaxBusyPower(107.339);
        lsParameter.setIdlePowerPercent(0.07);
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
        mobileParameter.setMaxBusyPower(87.53);
        mobileParameter.setIdlePowerPercent(0.04);
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
                new PowerModelLinear(p.getMaxBusyPower(), p.getIdlePowerPercent())
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

}
