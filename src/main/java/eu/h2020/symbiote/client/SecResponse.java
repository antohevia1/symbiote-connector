package eu.h2020.symbiote.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SecResponse {

    @JsonProperty("x-auth-response")
    public String xAuthResponse;


    public SecResponse(){

    }

    public String getxAuthResponse() {
        return xAuthResponse;
    }

    // Setter Methods

    public void setxAuthResponse(String xAuthResponse) {
        this.xAuthResponse = xAuthResponse;
    }
}//end of class
