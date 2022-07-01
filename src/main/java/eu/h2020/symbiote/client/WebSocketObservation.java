package eu.h2020.symbiote.client;

import java.util.ArrayList;

public class WebSocketObservation {

    SecResponse SecResponseObject;
    Payload PayloadObject;


    // Getter Methods
    public SecResponse getSecResponse() {
        return SecResponseObject;
    }//end
//--------------------------------------------
    public Payload getPayload() {
        return PayloadObject;
    }//end
//--------------------------------------------
    // Setter Methods
    public void setSecResponse(SecResponse secResponseObject) {
        this.SecResponseObject = secResponseObject;
    }//end
//--------------------------------------------
    public void setPayload(Payload payloadObject) {
        this.PayloadObject = payloadObject;
    }//end





}//end of class
