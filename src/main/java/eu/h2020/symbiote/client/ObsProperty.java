package eu.h2020.symbiote.client;

import java.util.ArrayList;

public class ObsProperty{
    public String name;
    public String iri;
    public ArrayList<String> description;

    public ObsProperty() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIri() {
        return iri;
    }

    public void setIri(String iri) {
        this.iri = iri;
    }

    public ArrayList<String> getDescription() {
        return description;
    }

    public void setDescription(ArrayList<String> description) {
        this.description = description;
    }
}//end of class
