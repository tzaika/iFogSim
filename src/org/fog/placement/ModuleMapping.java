package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleMapping {

    private Map<String, List<String>> deviceToModules;

    protected ModuleMapping() {
        setDeviceToModules(new HashMap<>());
    }

    public static ModuleMapping createModuleMapping() {
        return new ModuleMapping();
    }

    public Map<String, List<String>> getDeviceToModules() {
        return deviceToModules;
    }

    public void setDeviceToModules(Map<String, List<String>> deviceToModules) {
        this.deviceToModules = deviceToModules;
    }

    public void addModuleToDeviceIfNotPresent(String moduleName, String deviceName) {
        if (!getDeviceToModules().containsKey(deviceName)) {
            deviceToModules.put(deviceName, new ArrayList<>());
        }
        if (!deviceToModules.get(deviceName).contains(moduleName)) {
            deviceToModules.get(deviceName).add(moduleName);
        }
    }

    public void addModuleToDevice(String moduleName, String deviceName) {
        if (!getDeviceToModules().containsKey(deviceName)) {
            deviceToModules.put(deviceName, new ArrayList<>());
        }
        deviceToModules.get(deviceName).add(moduleName);
    }

}
