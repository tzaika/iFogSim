package org.fog.placement;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.*;

import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

public class Controller extends SimEntity {

    public static boolean ONLY_CLOUD = false;
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMAN);

    private List<FogDevice> fogDevices;
    private List<Sensor> sensors;
    private List<Actuator> actuators;
    private Map<String, Application> applications;
    private Map<String, Integer> appLaunchDelays;
    private Map<String, ModulePlacement> appModulePlacementPolicy;

    public Controller(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
        super(name);
        this.applications = new HashMap<String, Application>();
        setAppLaunchDelays(new HashMap<String, Integer>());
        setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
        for (FogDevice fogDevice : fogDevices) {
            fogDevice.setControllerId(getId());
        }
        setFogDevices(fogDevices);
        setActuators(actuators);
        setSensors(sensors);
        connectWithLatencies();
    }

    private FogDevice getFogDeviceById(int id) {
        for (FogDevice fogDevice : getFogDevices()) {
            if (id == fogDevice.getId())
                return fogDevice;
        }
        return null;
    }

    private void connectWithLatencies() {
        for (FogDevice fogDevice : getFogDevices()) {
            FogDevice parent = getFogDeviceById(fogDevice.getParentId());
            if (parent == null)
                continue;
            double latency = fogDevice.getUplinkLatency();
            parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
            parent.getChildrenIds().add(fogDevice.getId());
        }
    }

    @Override
    public void startEntity() {
        for (String appId : applications.keySet()) {
            if (getAppLaunchDelays().get(appId) == 0)
                processAppSubmit(applications.get(appId));
            else
                send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
        }

        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);

        send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);

        for (FogDevice dev : getFogDevices())
            sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.APP_SUBMIT:
                processAppSubmit(ev);
                break;
            case FogEvents.TUPLE_FINISHED:
                processTupleFinished(ev);
                break;
            case FogEvents.CONTROLLER_RESOURCE_MANAGE:
                manageResources();
                break;
            case FogEvents.STOP_SIMULATION:
                CloudSim.stopSimulation();
                writeResultsHeaderToFile();
                writeResultValuesToCsvFile();
                writeTotalsToCsvFile();
                // printTimeDetails();
                // printPowerDetails();
                // printNetworkUsageDetails();
                // printCostDetails();
                System.exit(0);
                break;

        }
    }

    private void printNetworkUsageDetails() {
        System.out.println("Total network usage = " + NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME);
    }

    private FogDevice getCloud() {
        for (FogDevice dev : getFogDevices())
            if (dev.getName().equals("cloud"))
                return dev;
        return null;
    }

    private void printCostDetails() {
        System.out.println("\nCost of execution in cloud = " + getCloud().getTotalCost());
        double totalCost = 0.0;
        for (FogDevice fogDevice : getFogDevices()) {
            double cost = fogDevice.getTotalCost();
            totalCost += cost;
            System.out.println(fogDevice.getName() + " : Cost = " + cost);
        }
        System.out.println("\nTotal cost = " + totalCost);

    }

    private void printPowerDetails() {
        double totalEnergyConsumption = 0.0;
        for (FogDevice fogDevice : getFogDevices()) {
            double energyConsumption = fogDevice.getEnergyConsumption();
            totalEnergyConsumption += energyConsumption;
            System.out.println(fogDevice.getName() + " : Energy Consumed = " + energyConsumption);
        }
        System.out.println("\nTotal energy consumption = " + totalEnergyConsumption);
    }

    private String getStringForLoopId(int loopId) {
        for (String appId : getApplications().keySet()) {
            Application app = getApplications().get(appId);
            for (AppLoop loop : app.getLoops()) {
                if (loop.getLoopId() == loopId)
                    return loop.getModules().toString();
            }
        }
        return null;
    }

    private void printTimeDetails() {
        System.out.println("=========================================");
        System.out.println("============== RESULTS ==================");
        System.out.println("=========================================");
        System.out.println("EXECUTION TIME : " + (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
        System.out.println("=========================================");
        System.out.println("APPLICATION LOOP DELAYS");
        System.out.println("=========================================");
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			/*double average = 0, count = 0;
			for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
				Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
				Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
				if(startTime == null || endTime == null)
					break;
				average += endTime-startTime;
				count += 1;
			}
			System.out.println(getStringForLoopId(loopId) + " ---> "+(average/count));*/
            System.out.println(getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
        }
        System.out.println("=========================================");
        System.out.println("TUPLE CPU EXECUTION DELAY");
        System.out.println("=========================================");

        for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
            System.out.println(tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
        }

        System.out.println("=========================================");
    }

    private void writeResultsHeaderToFile() {
        try (FileWriter fw = new FileWriter("./results/" + this.getName() + "_header.txt")) {
            fw.write("============== " + this.getName() + " =============" + System.lineSeparator());
            fw.write("=========================================" + System.lineSeparator());
            fw.write("================ RESULTS ================" + System.lineSeparator());
            fw.write("=========================================" + System.lineSeparator());
            fw.write("EXECUTION TIME : " + (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()) + System.lineSeparator());
            fw.write("=========================================" + System.lineSeparator());
            fw.write("APPLICATION LOOP DELAYS" + System.lineSeparator());
            fw.write("=========================================" + System.lineSeparator());
            for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
                fw.write(getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId) + System.lineSeparator());
            }
            fw.write("=========================================" + System.lineSeparator());
            fw.write("TUPLE CPU EXECUTION DELAY" + System.lineSeparator());
            fw.write("=========================================" + System.lineSeparator());

            for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
                fw.write(tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType) + System.lineSeparator());
            }

            fw.write("=========================================" + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeResultValuesToCsvFile() {
        try (FileWriter fw = new FileWriter("./results/" + this.getName() + "_values.csv")) {
            fw.write("fogDevice ; energy ; cost" + System.lineSeparator());
            for (FogDevice fogDevice : getFogDevices()) {
                fw.write(fogDevice.getName() + " ; " + numberFormat.format(fogDevice.getEnergyConsumption())
                        + " ; " + numberFormat.format(fogDevice.getTotalCost()) + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTotalsToCsvFile() {
        try (FileWriter fw = new FileWriter("./results/" + this.getName() + "_totals.csv")) {
            fw.write("total ; value" + System.lineSeparator());
            fw.write("energy ; " + numberFormat.format(calculateTotalEnergyConsumption()) + System.lineSeparator());
            fw.write("cost ; " + numberFormat.format(calculateTotalCost()) + System.lineSeparator());
            fw.write("network ; " + numberFormat.format(NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME) + System.lineSeparator());
            fw.write(" ; " + System.lineSeparator());
            fw.write("loop ; delay" + System.lineSeparator());
            for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
                fw.write(getStringForLoopId(loopId) + " ; " + numberFormat.format(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)) + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double calculateTotalEnergyConsumption() {
        double totalEnergyConsumption = 0.0;
        for (FogDevice fogDevice : getFogDevices()) {
            double energyConsumption = fogDevice.getEnergyConsumption();
            totalEnergyConsumption += energyConsumption;
        }
        return totalEnergyConsumption;
    }

    private double calculateTotalCost() {
        double totalCost = 0.0;
        for (FogDevice fogDevice : getFogDevices()) {
            double cost = fogDevice.getTotalCost();
            totalCost += cost;
        }
        return totalCost;
    }

    protected void manageResources() {
        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
    }

    private void processTupleFinished(SimEvent ev) {
    }

    @Override
    public void shutdownEntity() {
    }

    public void submitApplication(Application application, int delay, ModulePlacement modulePlacement) {
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);
        getAppLaunchDelays().put(application.getAppId(), delay);
        getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);

        for (Sensor sensor : sensors) {
            sensor.setApp(getApplications().get(sensor.getAppId()));
        }
        for (Actuator ac : actuators) {
            ac.setApp(getApplications().get(ac.getAppId()));
        }

        for (AppEdge edge : application.getEdges()) {
            if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                String moduleName = edge.getSource();
                for (Actuator actuator : getActuators()) {
                    if (actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
                        application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
                }
            }
        }
    }

    public void submitApplication(Application application, ModulePlacement modulePlacement) {
        submitApplication(application, 0, modulePlacement);
    }


    private void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        processAppSubmit(app);
    }

    private void processAppSubmit(Application application) {
        System.out.println(CloudSim.clock() + " Submitted application " + application.getAppId());
        FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
        getApplications().put(application.getAppId(), application);

        ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
        for (FogDevice fogDevice : fogDevices) {
            sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
        }

        Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
        for (Integer deviceId : deviceToModuleMap.keySet()) {
            for (AppModule module : deviceToModuleMap.get(deviceId)) {
                sendNow(deviceId, FogEvents.APP_SUBMIT, application);
                sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
            }
        }
    }

    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    public void setFogDevices(List<FogDevice> fogDevices) {
        this.fogDevices = fogDevices;
    }

    public Map<String, Integer> getAppLaunchDelays() {
        return appLaunchDelays;
    }

    public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
        this.appLaunchDelays = appLaunchDelays;
    }

    public Map<String, Application> getApplications() {
        return applications;
    }

    public void setApplications(Map<String, Application> applications) {
        this.applications = applications;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        for (Sensor sensor : sensors)
            sensor.setControllerId(getId());
        this.sensors = sensors;
    }

    public List<Actuator> getActuators() {
        return actuators;
    }

    public void setActuators(List<Actuator> actuators) {
        this.actuators = actuators;
    }

    public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
        return appModulePlacementPolicy;
    }

    public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
        this.appModulePlacementPolicy = appModulePlacementPolicy;
    }
}