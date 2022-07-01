package eu.h2020.symbiote.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class Location {
    //private String @c;
    @JsonProperty("@c")
    public String c;

    private float longitude;
    private float latitude;
    private float altitude;
    private String name;
    ArrayList< Object > description = new ArrayList < Object > ();

    public Location(){

    }

    // Getter Methods


    public ArrayList<Object> getDescription() {
        return description;
    }

    public String getc() {
        return c;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public String getName() {
        return name;
    }

    // Setter Methods


    public void setDescription(ArrayList<Object> description) {
        this.description = description;
    }

    public void setc(String c) {
        this.c = c;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public void setName(String name) {
        this.name = name;
    }
}//end of class
