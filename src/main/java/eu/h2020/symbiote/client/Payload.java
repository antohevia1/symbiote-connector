package eu.h2020.symbiote.client;

import java.util.ArrayList;

public class Payload {
    private String resourceId;
    Location LocationObject;
    private String resultTime;
    private String samplingTime;
    ArrayList< ObsValue > obsValues = new ArrayList < ObsValue > ();

    public Payload(){

    }

    // Getter Methods


    public ArrayList<ObsValue> getObsValues() {
        return obsValues;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Location getLocation() {
        return LocationObject;
    }

    public String getResultTime() {
        return resultTime;
    }

    public String getSamplingTime() {
        return samplingTime;
    }

    // Setter Methods


    public void setObsValues(ArrayList<ObsValue> obsValues) {
        this.obsValues = obsValues;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setLocation(Location locationObject) {
        this.LocationObject = locationObject;
    }

    public void setResultTime(String resultTime) {
        this.resultTime = resultTime;
    }

    public void setSamplingTime(String samplingTime) {
        this.samplingTime = samplingTime;
    }
}//end of class
