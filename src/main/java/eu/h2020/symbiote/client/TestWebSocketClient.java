package eu.h2020.symbiote.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.client.interfaces.CRAMClient;
import eu.h2020.symbiote.client.interfaces.RAPClient;
import eu.h2020.symbiote.client.interfaces.RHClient;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.model.cim.Observation;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;


import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.getFactory;


/*
 * DONT FORGET TO ADD THE VM PROXY SETTINGS
 * IN EDIT CONFIGURATIONS !!!!!!!!!!!
 *
 */
public class TestWebSocketClient {

    /*
     * Parameters:
     * String platformID,
     * String internalID,
     * String coreUrl
     * String interworkingInterface
     */


    /*
     * TODO
     *  check messageHandler,session.addMessageHandler();
     */



    public static KeepAliveThread keepAliveThread;
    public static boolean shouldSessionRestart = false;


    public static void main(String[] args) {

        /*
         * The following parameters are the
         * parameters of notification request message.
         */

        /*
         * TODO Fill interWorkingInterface,
         * internalID and platformID.
         */
        /*
         * TODO Fill interWorkingInterface,
         * internalID and platformID.
         */
        DEIntegration test = new DEIntegration();
        try {
            String input = "[{\"code\": {\"coding\":[{\"display\": \"Blood Glucose\"}]}}]";//"{\"first_name\":\"pEPE\",\"last_name\":\"PER\",\"email\":\"correo@gmail.com\"}";
            test.sendMessageToDE(input);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return;
        }
        
        
        if (args.length == 5) {
            System.out.println(
                "The command line arguments are:");

            // iterating the args array and printing
            // the command line arguments
            String[] inputParams = {"email", "password","internalID", "interWorkingInterface", "platformID"};
            for (int i =0 ; i < 3 ; i++) {
                System.out.println(inputParams[i]+ " : " +args[i]);
            }
        }
        else{
            System.out.println("Incorrect command line "
                               + "arguments found.");
            System.exit(1);
        }
        String internalID = args[2]; //required
        String interWorkingInterface   = args[3];//required
        String platformID            = args[4];//required

        /*
         * The following parameters are
         * the credentials of the data consumer.
         */

        /*
         * TODO Fill email and passsword
         */

        String email                 = args[0];
        String password              = args[1];

        LocalClientSocket clientSocket = new LocalClientSocket();
        WebSocketContainer container   = ContainerProvider.getWebSocketContainer();

        /*
         * If platformID is never used in the past
         * create new webSocketApi object.
         * One webSocketApi per platformID.
         */

        WebSocketApi webSocketApi = new WebSocketApi(platformID,interWorkingInterface,email,password);

        keepAliveThread = new KeepAliveThread();
        keepAliveThread.startThread();

        /*
         * Keep the webSocketApi to a list of WebSocketApi objects.
         * No need to open again a new web socket session for the same platform id.
         */

         /*
          * Get the websocket uri
          * of platformID's RAP
          */

        String endpointURI = null;

        try {
            endpointURI = webSocketApi.getWebSocketURL();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return;
        }

        Session session    = null;

        /*
         * Get the resource id
         * of the resource to be observed
         * from the internalID.
         */
        String resourceId = null;
         try {
            resourceId = webSocketApi.getResourceIdFromInternalID(internalID);
         } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return;
         }
        

        if(resourceId == null){
            System.out.println("Failed to find the resource id");
            String errorMessage = webSocketApi.getErrorMessage();
            System.out.println("ERROR MESSAGE: " + errorMessage);
            return;
        }



        /*
         * Open a web socket to the
         * remote RAP of platformID.
         */

        try {
            session = container.connectToServer(clientSocket, URI.create(endpointURI));
            webSocketApi.setSession(session);

            if (session != null) {

                /*
                 * Create the subscribe to resource
                 * request json message.
                 */
                String message = null;
                try {
                    message = webSocketApi.getMessage(webSocketApi.SUBSCRIBE_COMMAND, resourceId);
                }catch(Exception ex){
                    System.out.println(ex.getMessage().toString());
                    return;
                }

                /*
                 * Send the subscribe command
                 * to the remote RAP microservice.
                 */
                try {
                    webSocketApi.sendMessageToRAP(message);
                }catch (Exception ex){
                    System.out.println("Failed to send message to RAP");
                    return;
                }

                /*
                 * Update the list of
                 * subscribed resource ids.
                 */
                webSocketApi.addResourceID(resourceId);

                /*
                 * Wait here some time
                 * before unsubscribe the resource.
                 */
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date = new Date();
                System.out.println("Test started at: " + formatter.format(date));

                long start = System.currentTimeMillis();
                long duration = 60 * 1000; //60 seconds

                /*
                 * Add the webSocketApi to
                 * keepAlive thread
                 */
                keepAliveThread.addWebSocketApi(webSocketApi);

                while (true){
                    long now = System.currentTimeMillis();

                    if(shouldSessionRestart == true){
                        System.out.println("##### shouldSessionRestart is true ####");
                        webSocketApi.restartSession(container,clientSocket);
                        shouldSessionRestart = false;
                    }

                    if (now - start > duration)
                        break;
                }

                /*
                 * Now is time to unsubscribe
                 * and  close the session.
                 */


                /*
                 * Build the unsubscribe
                 * request json message.
                 */

                try {
                    message = webSocketApi.getMessage(webSocketApi.UNSUBSCRIBE_COMMAND, resourceId);
                }catch(Exception ex){
                    System.out.println(ex.getMessage().toString());
                    return;
                }

                /*
                 * Send the unsubscribe command
                 * to the remote RAP microservice.
                 */
                try {
                    webSocketApi.sendMessageToRAP(message);
                }catch (Exception ex){
                    System.out.println("Failed to send message to RAP");
                    return;
                }

                /*
                 * Remove the resource id
                 * from the stored list of resource ids.
                 */

                webSocketApi.removeResourceID(resourceId);
                keepAliveThread.stopThread();
                System.out.println("Closing session...");
                webSocketApi.closeSession();

            }else{
                System.out.println("ERROR: Session is null");
            }
        } catch (Exception e) {
            System.out.println("Exception");
            if(session == null){
                System.out.println("Failed to create session for web socket end point: " + endpointURI);
            }
             throw new RuntimeException(e);
        }
    }//end

//--------------------------------------------------------------------------------------
    /*
     * This is the local web socket that receives
     * messages from the remote RAP Web Socket.
     */
    @ClientEndpoint
    public static class LocalClientSocket {
        public String messageEchoed;
        public volatile boolean spin = true;

        @OnOpen
        public void onWebSocketConnect(Session session) throws IOException, EncodeException {
            System.out.println("onWebSocketConnect " + session.getId());
            System.out.println("Web Socket Connection established with remote RAP Web socket ");
            System.out.println("LocalClientSocket session.getOpenSessions().size() " + session.getOpenSessions().size());
            System.out.println("LocalClientSocket session.getMaxIdleTimeout() " + session.getMaxIdleTimeout());
        }//end
//--------------------------------------------------------------------------------------------
        @OnMessage
        public void onWebSocketText(String message) throws IOException, EncodeException {
            messageEchoed = message;
            spin = false;

            boolean isObservationMessage = WebSocketApi.checkIfIsObservationMessage(message);

            if(isObservationMessage == false) {
                System.out.println("Is not observation message, ignore it ");
                return;
            }
            else
                System.out.println("Is observation message");

            System.out.println("Received Observation message from remote RAP web socket =  " + message);

            DEIntegration DEApi = new DEIntegration();
            try {
                System.out.println("Sending message to DE");
                DEApi.sendMessageToDE(message);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Payload payload     = objectMapper.readValue(message,WebSocketObservation.class).PayloadObject;
                String resultTime   = payload.getResultTime();
                String samplingTime = payload.getSamplingTime();
                String resourceId   = payload.getResourceId();
                float  longitude    = payload.getLocation().getLongitude();
                float  latitude     = payload.getLocation().getLatitude();

                System.out.println(payload);

                ArrayList<Object> descriptionList  = payload.getLocation().getDescription();
                ArrayList<ObsValue> obsList        = payload.getObsValues();

                for (int i  = 0; i <  obsList.size(); i++ ){
                    String observedValue    = obsList.get(i).getValue();
                    String observedSymbol   = obsList.get(i).getUom().getSymbol();
                    System.out.println("Property name: " + obsList.get(i).getObsProperty().getName() +  " Property description " + obsList.get(i).getObsProperty().getDescription() + " has value : " + observedValue + " " + observedSymbol);
                }
            } catch (IOException e) {

                e.printStackTrace();
            }
        }//end
//--------------------------------------------------------------------------------------------
        @OnClose
        public void onWebSocketClose(CloseReason reason) {
            System.out.println("onWebSocketClose");
            System.out.println("Closing connection with remote RAP web socket,reason =  " + reason);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            System.out.println("Closed at : " + formatter.format(date));

            shouldSessionRestart = true;
        }//end
//--------------------------------------------------------------------------------------------
        @OnError
        public void onWebSocketError(Throwable cause) {
            System.out.println("onWebSocketError");

            if(cause !=null)
             System.out.println("onWebSocketError cause" + cause.getMessage());
        }//end
}//end of class


}//end of class
