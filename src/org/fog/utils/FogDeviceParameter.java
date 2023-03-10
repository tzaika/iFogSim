package org.fog.utils;

import org.fog.entities.MicroserviceFogDevice;

public class FogDeviceParameter {

    private String name;
    private long mipsPerPe;
    private int numberOfPes;
    private int ram;
    private long uplinkBandwidth;
    private long downlinkBandwidth;
    private int level;
    private double ratePerMips;
    private double maxBusyPower;
    private double idlePowerPercent;
    private String deviceType;
    private long hostStorage;
    private int hostBandwidth;
    private String systemArchitecture;
    private String systemOS;
    private String systemVmm;
    private double systemTimezone;
    private double processingCost;
    private double memoryCost;
    private double storageCost;
    private double bandwidthCost;
    private double schedulingInterval;
    private double clusterLinkBandwidth;
    private double uplinkLatency;

    public FogDeviceParameter() {
    }

    public static FogDeviceParameter getDefaultFogDevice() {
        FogDeviceParameter fogDeviceParameter = new FogDeviceParameter();
        fogDeviceParameter.setName("DefaultFogDevice");
        fogDeviceParameter.setMipsPerPe(1000);
        fogDeviceParameter.setNumberOfPes(1);
        fogDeviceParameter.setRam(2048);
        fogDeviceParameter.setUplinkBandwidth(10000);
        fogDeviceParameter.setDownlinkBandwidth(10000);
        fogDeviceParameter.setLevel(0);
        fogDeviceParameter.setRatePerMips(0.05);
        fogDeviceParameter.setMaxBusyPower(100);
        fogDeviceParameter.setIdlePowerPercent(0.05);
        fogDeviceParameter.setDeviceType(MicroserviceFogDevice.CLOUD);
        fogDeviceParameter.setHostStorage(262144);
        fogDeviceParameter.setHostBandwidth(10000);
        fogDeviceParameter.setSystemArchitecture("x86");
        fogDeviceParameter.setSystemOS("Linux");
        fogDeviceParameter.setSystemVmm("Xen");
        fogDeviceParameter.setSystemTimezone(10.0);
        fogDeviceParameter.setProcessingCost(3.0);
        fogDeviceParameter.setMemoryCost(0.05);
        fogDeviceParameter.setStorageCost(0.001);
        fogDeviceParameter.setBandwidthCost(0.0001);
        fogDeviceParameter.setSchedulingInterval(0.05);
        fogDeviceParameter.setClusterLinkBandwidth(10000);
        fogDeviceParameter.setUplinkLatency(0.1);
        return fogDeviceParameter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMipsPerPe() {
        return mipsPerPe;
    }

    public void setMipsPerPe(long mipsPerPe) {
        this.mipsPerPe = mipsPerPe;
    }


    public int getNumberOfPes() {
        return numberOfPes;
    }

    public void setNumberOfPes(int numberOfPes) {
        this.numberOfPes = numberOfPes;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }

    public long getUplinkBandwidth() {
        return uplinkBandwidth;
    }

    public void setUplinkBandwidth(long uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }

    public long getDownlinkBandwidth() {
        return downlinkBandwidth;
    }

    public void setDownlinkBandwidth(long downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public void setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
    }

    public double getMaxBusyPower() {
        return maxBusyPower;
    }

    public void setMaxBusyPower(double maxBusyPower) {
        this.maxBusyPower = maxBusyPower;
    }

    public double getIdlePowerPercent() {
        return idlePowerPercent;
    }

    public void setIdlePowerPercent(double idlePowerPercent) {
        this.idlePowerPercent = idlePowerPercent;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public long getHostStorage() {
        return hostStorage;
    }

    public void setHostStorage(long hostStorage) {
        this.hostStorage = hostStorage;
    }

    public int getHostBandwidth() {
        return hostBandwidth;
    }

    public void setHostBandwidth(int hostBandwidth) {
        this.hostBandwidth = hostBandwidth;
    }

    public String getSystemArchitecture() {
        return systemArchitecture;
    }

    public void setSystemArchitecture(String systemArchitecture) {
        this.systemArchitecture = systemArchitecture;
    }

    public String getSystemOS() {
        return systemOS;
    }

    public void setSystemOS(String systemOS) {
        this.systemOS = systemOS;
    }

    public String getSystemVmm() {
        return systemVmm;
    }

    public void setSystemVmm(String systemVmm) {
        this.systemVmm = systemVmm;
    }

    public double getSystemTimezone() {
        return systemTimezone;
    }

    public void setSystemTimezone(double systemTimezone) {
        this.systemTimezone = systemTimezone;
    }

    public double getProcessingCost() {
        return processingCost;
    }

    public void setProcessingCost(double processingCost) {
        this.processingCost = processingCost;
    }

    public double getMemoryCost() {
        return memoryCost;
    }

    public void setMemoryCost(double memoryCost) {
        this.memoryCost = memoryCost;
    }

    public double getStorageCost() {
        return storageCost;
    }

    public void setStorageCost(double storageCost) {
        this.storageCost = storageCost;
    }

    public double getBandwidthCost() {
        return bandwidthCost;
    }

    public void setBandwidthCost(double bandwidthCost) {
        this.bandwidthCost = bandwidthCost;
    }

    public double getSchedulingInterval() {
        return schedulingInterval;
    }

    public void setSchedulingInterval(double schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
    }

    public double getClusterLinkBandwidth() {
        return clusterLinkBandwidth;
    }

    public void setClusterLinkBandwidth(double clusterLinkBandwidth) {
        this.clusterLinkBandwidth = clusterLinkBandwidth;
    }

    public double getUplinkLatency() {
        return uplinkLatency;
    }

    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }
}
