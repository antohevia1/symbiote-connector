package eu.h2020.symbiote.client;

public class ObsValue {

    public String value;
    public ObsProperty obsProperty;
    public Uom uom;

    public ObsValue() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ObsProperty getObsProperty() {
        return obsProperty;
    }

    public void setObsProperty(ObsProperty obsProperty) {
        this.obsProperty = obsProperty;
    }

    public Uom getUom() {
        return uom;
    }

    public void setUom(Uom uom) {
        this.uom = uom;
    }
}//end of class
