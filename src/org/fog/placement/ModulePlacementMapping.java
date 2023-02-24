package org.fog.placement;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModulePlacementMapping extends ModulePlacement {

    private ModuleMapping moduleMapping;

    public ModulePlacementMapping(List<FogDevice> fogDevices, Application application,
                                  ModuleMapping moduleMapping) {
        this.setFogDevices(fogDevices);
        this.setApplication(application);
        this.setModuleMapping(moduleMapping);
        this.setModuleToDeviceMap(new HashMap<>());
        this.setDeviceToModuleMap(new HashMap<>());
        this.setModuleInstanceCountMap(new HashMap<>());
        for (FogDevice device : getFogDevices())
            getModuleInstanceCountMap().put(device.getId(), new HashMap<>());
        mapModules();
    }

    @Override
    protected void mapModules() {
        Map<String, List<String>> mapping = moduleMapping.getDeviceToModules();
        for (String deviceName : mapping.keySet()) {
            FogDevice device = getDeviceByName(deviceName);
            for (String moduleName : mapping.get(deviceName)) {

                AppModule module = getApplication().getModuleByName(moduleName);
                if (module == null)
                    continue;
                createModuleInstanceOnDevice(module, device);
            }
        }
    }

    public ModuleMapping getModuleMapping() {
        return moduleMapping;
    }

    public void setModuleMapping(ModuleMapping moduleMapping) {
        this.moduleMapping = moduleMapping;
    }


}
