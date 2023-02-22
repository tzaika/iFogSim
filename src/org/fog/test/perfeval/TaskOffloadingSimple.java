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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fog.application.AppLoop.createAppLoop;

public class TaskOffloadingSimple {

    public static final String APP_ID_PDM_DEMO = "PDM-Demonstrator";
    public static final String BIG_DATA = "big_data";
    public static final String LOCAL_CLIENT = "local_client";
    public static final String MOBILE_CLIENT = "mobile_client";
    public static final String SENSOR_1 = "SENSOR_1";
    public static final String SENSOR_2 = "SENSOR_2";
    public static final String MOTOR_1 = "MOTOR_1";
    public static final String MOTOR_2 = "MOTOR_2";
    public static final String SENSOR_VALUES = "SENSOR_VALUES";
    public static final String ANALYSIS = "ANALYSIS";
    public static final String ACTION = "ACTION";
    public static final String CLOUD = "cloud";
    public static final String MOBILE = "mobileNode";
    public static final String LOCAL_SERVER = "localServerNode";
    public static final double SENSOR_THROUGHPUT = 200;
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static List<Application> applications = new ArrayList<>();

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

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice(BIG_DATA, CLOUD);
            moduleMapping.addModuleToDevice(LOCAL_CLIENT, LOCAL_SERVER);
            moduleMapping.addModuleToDevice(MOBILE_CLIENT, MOBILE);

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

        FogDevice localServer = createLocalServer(LOCAL_SERVER, 1);
        localServer.setParentId(cloud.getId());
        fogDevices.add(localServer);
        sensors.add(attachSensor(localServer, userId, appId, application, SENSOR_1));
        actuators.add(attachActuator(localServer, userId, appId, application, MOTOR_1));

        FogDevice mobile = createMobile(MOBILE, 1);
        mobile.setParentId(cloud.getId());
        fogDevices.add(mobile);
        sensors.add(attachSensor(mobile, userId, appId, application, SENSOR_2));
        actuators.add(attachActuator(mobile, userId, appId, application, MOTOR_2));
    }

    protected static Sensor attachSensor(FogDevice fogDevice, int userId, String appId, Application application, String sensorType) {
        Sensor sensor = new Sensor(fogDevice.getName() + "_sensor", sensorType, userId, appId, new DeterministicDistribution(1000 / (SENSOR_THROUGHPUT / 9 * 10)));
        sensor.setApp(application);
        sensor.setGatewayDeviceId(fogDevice.getId());
        sensor.setLatency(10.0);
        return sensor;
    }

    protected static Actuator attachActuator(FogDevice fogDevice, int userId, String appId, Application application, String actuatorType) {
        Actuator motor = new Actuator(fogDevice.getName() + "_motor", userId, appId, actuatorType);
        motor.setApp(application);
        motor.setGatewayDeviceId(fogDevice.getId());
        motor.setLatency(5.0);
        return motor;
    }

    protected static FogDevice createCloud() {
        FogDeviceParameter cloudParameter = FogDeviceParameter.getDefaultFogDevice();
        cloudParameter.setName(CLOUD);
        cloudParameter.setMipsPerPe(50000); // 10 x normal PC
        cloudParameter.setNumberOfPes(10);
        cloudParameter.setRam(1048576); // 1 TB
        cloudParameter.setUplinkBandwidth(10000000); // 10 Gbps
        cloudParameter.setDownlinkBandwidth(10000000);
        cloudParameter.setLevel(0);
        cloudParameter.setRatePerMips(0.01);
        cloudParameter.setBusyPower(16 * 103.0);
        cloudParameter.setIdlePower(16 * 83.25);
        cloudParameter.setHostBandwidth(10000000);
        cloudParameter.setHostStorage(16777216); // 16 TB

        return createFogDeviceNew(cloudParameter);
    }

    protected static FogDevice createLocalServer(String name, int level) {
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

    protected static FogDevice createMobile(String name, int level) {
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

    protected static FogDevice createFogDeviceNew(FogDeviceParameter p) {

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

    protected static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        // Adding modules (vertices) to the application model (directed graph)
        application.addAppModule(BIG_DATA, 2048, 2000, 16384, 100000, 4); // ram in MB, MIPS, storageSize in MB, bw in kbps
        application.addAppModule(LOCAL_CLIENT, 512, 1000, 8192, 100000, 2);
        application.addAppModule(MOBILE_CLIENT, 256, 500, 2048, 10000, 1);

        // Connecting the application modules (vertices) in the application model (directed graph) with edges
        application.addAppEdge(SENSOR_1, LOCAL_CLIENT, 100, 2048, SENSOR_1, Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge(SENSOR_2, MOBILE_CLIENT, 100, 2048, SENSOR_2, Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge(LOCAL_CLIENT, BIG_DATA, 100, 6000, 131072, SENSOR_VALUES, Tuple.UP, AppEdge.MODULE);
        application.addAppEdge(MOBILE_CLIENT, BIG_DATA, 100, 3000, 65536, SENSOR_VALUES, Tuple.UP, AppEdge.MODULE);
        application.addAppEdge(BIG_DATA, LOCAL_CLIENT, 100, 1000, 4096, ANALYSIS, Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge(BIG_DATA, MOBILE_CLIENT, 100, 1000, 4096, ANALYSIS, Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge(LOCAL_CLIENT, MOTOR_1, 1000, 1024, ACTION, Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge(MOBILE_CLIENT, MOTOR_2, 1000, 1024, ACTION, Tuple.DOWN, AppEdge.ACTUATOR);

        // Defining the input-output relationships (represented by selectivity) of the application modules.
        application.addTupleMapping(LOCAL_CLIENT, SENSOR_1, ACTION, new FractionalSelectivity(1.0));
        application.addTupleMapping(MOBILE_CLIENT, SENSOR_2, ACTION, new FractionalSelectivity(1.0));
        application.addTupleMapping(BIG_DATA, SENSOR_VALUES, ANALYSIS, new FractionalSelectivity(1.0));
        application.addTupleMapping(LOCAL_CLIENT, ANALYSIS, ACTION, new FractionalSelectivity(1.0));
        application.addTupleMapping(MOBILE_CLIENT, ANALYSIS, ACTION, new FractionalSelectivity(1.0));

        //Defining application loops to monitor the latency of.
        application.setLoops(asList(
                createAppLoop(LOCAL_CLIENT, MOTOR_1),
                createAppLoop(MOBILE_CLIENT, MOTOR_2),
                createAppLoop(BIG_DATA, LOCAL_CLIENT),
                createAppLoop(MOBILE_CLIENT, BIG_DATA),
                createAppLoop(LOCAL_CLIENT, BIG_DATA, MOBILE_CLIENT)));

        application.setSpecialPlacementInfo(BIG_DATA, CLOUD);
        application.createDAG();
        return application;
    }
}
