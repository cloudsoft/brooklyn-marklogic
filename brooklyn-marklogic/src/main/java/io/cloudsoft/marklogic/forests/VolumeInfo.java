package io.cloudsoft.marklogic.forests;

import java.io.Serializable;

public class VolumeInfo implements Serializable {
    private  String osDeviceName;
    private String volumeId;
    private String volumeDeviceName;

    public VolumeInfo(String volumeDeviceName, String volumeId,String osDeviceName) {
        this.volumeDeviceName = volumeDeviceName;
        this.volumeId = volumeId;
        this.osDeviceName = osDeviceName;
    }

    public String getVolumeDeviceName() {
        return volumeDeviceName;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getOsDeviceName() {
        return osDeviceName;
    }

    @Override
    public String toString() {
        return "VolumeInfo{" +
                "osDeviceName='" + osDeviceName + '\'' +
                ", volumeId='" + volumeId + '\'' +
                ", volumeDeviceName='" + volumeDeviceName + '\'' +
                '}';
    }
}
