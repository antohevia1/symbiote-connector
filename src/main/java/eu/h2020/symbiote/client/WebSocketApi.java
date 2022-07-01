package eu.h2020.symbiote.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.client.interfaces.CRAMClient;
import eu.h2020.symbiote.client.interfaces.RAPClient;
import eu.h2020.symbiote.client.interfaces.RHClient;
import eu.h2020.symbiote.client.interfaces.SearchClient;
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
import java.util.*;

import static eu.h2020.symbiote.client.AbstractSymbIoTeClientFactory.getFactory;

/*
 * Parameters:
 * String platformID,
 * String coreUrl
 * String interworkingInterface
 */


public class WebSocketApi {


    public static String NO_ERROR = "NO ERROR";
    public static String RESOURCE_ID_NULL     = "ERROR: RESOURCE ID FOUND NULL";
    public static String CLOUD_RESOURCE_NULL  = "ERROR: CLOUD RESOURCE FOUND NULL";
    public static String INVALID_PLATFORM_ID  = " ERROR: INVALID PLATFORM ID ";

    public static String ERROR_MESSAGE = NO_ERROR;
                                                
    public static String GET_DEVICE_ID_URL = "https://symbiote-core.intracom-telecom.com/symbiote/resource/getdeviceid";
    public static String ASAPA_LOGIN_URL   = "https://kubernetes.pasiphae.eu/shapes/asapa/auth/login";

    public static String KEEP_ALIVE_MESSAGE = "000";
    public static long KEEP_ALIVE_MESSAGE_PERIOD = 30000;

    /*
     * TEST CASES:
     * 1) WRONG PLATFORM ID: ERROR MESSAGE: "Exception 3: null rhClient is null, check platform id,"
     * 2) WRONG INTERNAL ID: ERROR MESSAGE: ERROR: CLOUD RESOURCE FOUND NULL. Check the validity of internal id: 8999671. Is registered ?
     * 3) WRONG INTERWORKING INTERFACE: ERROR MESSAGE:Failed to create session for web socket end point: wss://xxxxxxx/rap/notification

     */

    public static String SUBSCRIBE_COMMAND   = "SUBSCRIBE";
    public static String UNSUBSCRIBE_COMMAND = "UNSUBSCRIBE";

    public String interWorkingInterface = ""; //required
    public String coreUrl               = "https://symbiote-core.intracom-telecom.com";
    public String platformID            = "";//required
    public String userName              = "xxx";
    public String password              = "xxx";
    public String email                 = "";

    public Session session = null;

    public List<String> resourceIdList = new ArrayList<String>();


    public WebSocketApi(String platformID,String interWorkingInterface ){
      this.platformID = platformID;
      this.interWorkingInterface = interWorkingInterface;
    }//end
//------------------------------------------------------------------------------------------
    public WebSocketApi(String platformID, String interWorkingInterface, String email, String password ){
     this.platformID = platformID;
     this.interWorkingInterface = interWorkingInterface;
     this.password  = password;
     this.email     = email;
}//end
//------------------------------------------------------------------------------------------
     public void setCoreAddress(String coreUrl,String userName,String password){
        this.coreUrl    = coreUrl;
        this.userName   = userName;
        this.password   = password;
     }//end
//--------------------------------------------------------------------------------------
    public String getWebSocketURL() throws Exception{
        String rapWebSocketUrl = "";
        String tempUrl = null;

        try {
            tempUrl = this.interWorkingInterface.split("//")[1].split("/")[0].split(":")[0];
        }catch(Exception ex){
            tempUrl = null;
        }

        if(tempUrl == null)
            throw new Exception("Failed to parse interworking interface " + this.interWorkingInterface);

        rapWebSocketUrl = "wss://" + tempUrl + "/rap/notification";
        return(rapWebSocketUrl);
    }//end
//------------------------------------------------------------------------------------------
public void setSession(Session session){
   this.session = session;
}//end
//------------------------------------------------------------------------------------------
public Session getSession(){
  return this.session;
}//end
//------------------------------------------------------------------------------------------
public void closeSession() {
    try {
        session.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}//end
//------------------------------------------------------------------------------------------
    public static boolean checkIfIsObservationMessage(String message){
        if(message.contains("BAD_REQUEST")) {
            return false;
        }
        boolean res = checkIfIsKeepAliveMessage(message);
        if(res == true)
            return false;

        return true;
    }//end
//--------------------------------------------------------------------------------------
    public static boolean checkIfIsKeepAliveMessage(String message){

        System.out.println("checkIfIsKeepAliveMessage");//
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Payload payload   = objectMapper.readValue(message, WebSocketObservation.class).PayloadObject;
            String resultTime = payload.getResultTime();

            System.out.println("resultTime = " + resultTime);
            if(resultTime.equals(KEEP_ALIVE_MESSAGE)) {
                System.out.println("is keep alive message");
                return true;
            }
            else {
                System.out.println("is not keep alive message");
                return false;
            }
        }catch(Exception ex){
            System.out.println("checkIfIsKeepAliveMessage exception");
            return false;
        }

    }//end
//------------------------------------------------------------------------------------------
    public void sendKeepAliveMessage() throws IOException{
        try {
            sendMessageToRAP(KEEP_ALIVE_MESSAGE);
        }catch(Exception ex){
            System.out.println("Failed to send keep alive message");
        }
    }//end
//------------------------------------------------------------------------------------------
public void sendMessageToRAP(String message) throws IOException {
    session.getBasicRemote().sendText(message);
}//end
//------------------------------------------------------------------------------------------
public static String getMessage(String command,String resourceId) throws Exception{
    String message = "";
    JSONObject rootJsonObject       = new JSONObject();
    JSONObject secRequestJsonObject = new JSONObject();
    JSONObject payloadJsonObject    = new JSONObject();

    if(resourceId == null){
        throw new Exception("Exception resourceId is null");
    }

    String[] resourceIds = new String[1];
    resourceIds[0] = resourceId;

    if(!command.equals(SUBSCRIBE_COMMAND) && !command.equals(UNSUBSCRIBE_COMMAND)){
        throw new Exception("command: " + command + " not supported");
    }

    if(resourceIds.length == 0){
        throw new Exception("Resource ids array should not be empty");
    }

    /*
     * Build the payload json object
     */

    payloadJsonObject.put("action", command);
    payloadJsonObject.put("ids", resourceIds);

    /*
     * Build the security request json object
     */

    String timestamp = Long.toString(new Timestamp(System.currentTimeMillis()).getTime());
    secRequestJsonObject.put("x-auth-timestamp", timestamp);
    secRequestJsonObject.put("x-auth-size", 0);
    secRequestJsonObject.put("authenticationChallenge", "");
    secRequestJsonObject.put("clientCertificate", "");
    secRequestJsonObject.put("clientCertificateSigningAAMCertificate", "");
    secRequestJsonObject.put("foreignTokenIssuingAAMCertificate", "");

    rootJsonObject.put("payload", payloadJsonObject);
    rootJsonObject.put("secRequest", secRequestJsonObject);
    message = rootJsonObject.toString();
    return message;
}//end
//--------------------------------------------------------------------------------------
public  String getErrorMessage(){
   return ERROR_MESSAGE;
}//end
//--------------------------------------------------------------------------------------
public String getAsapaToken(String email,String password){
         
    RestTemplate restTemplate = new RestTemplate();

    /*
     * Build headers.
     */
    HttpHeaders httpHeaders   = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    httpHeaders.add("X-Shapes-Key", "7Msbb3w^SjVG%j");

    /*
     * Build body.
     */

    Map<String,String> map = new HashMap<>();
    map.put("email",email);
    map.put("password",password);

    HttpEntity<Map<String,String>> entity = new HttpEntity<>(map,httpHeaders);
    ResponseEntity<?> responseEntity      = restTemplate.postForEntity(ASAPA_LOGIN_URL,entity,String.class);
    HttpStatus httpStatus                 = responseEntity.getStatusCode();

    String body              = (String) responseEntity.getBody();
    JSONObject jsonObject    = new JSONObject(body);
    JSONArray itemsJsonArray = jsonObject.getJSONArray("items");

    /*
     * Get the token.
     */

    String token = (String) itemsJsonArray.getJSONObject(0).get("token");
    return token;
}//end
//--------------------------------------------------------------------------------------
public String getResourceIdFromInternalID(String internalID){
    String resourceId   = null;
    String resourcesURL = interWorkingInterface + "/rh/resources";

    /*
     * Get token from ASAPA.
     */

    String token = "";

    try {
        token = getAsapaToken(email, password);
    }catch(Exception ex){
        ERROR_MESSAGE = "Failed to get token from ASAPA";
        return  null;
    }

    if(token == null){
        ERROR_MESSAGE = "Failed to get token from ASAPA";
        return null;
    }

    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<?> responseEntity;

    HttpStatus httpStatus;

    try {
        responseEntity = restTemplate.getForEntity(resourcesURL, String.class);
        httpStatus     = responseEntity.getStatusCode();
    }catch(Exception ex){
        System.out.println("getResourceIdFromInternalID exception 1");
        return  resourceId;
    }

    if(httpStatus.value() != HttpStatus.OK.value()){

        System.out.println("getResourceIdFromInternalID exception 2");
        return  resourceId;
    }



    JSONArray resourcesJsonArray = new JSONArray(responseEntity.getBody().toString());

    int numberOfResources = resourcesJsonArray.length();

    for(int i = 0; i < numberOfResources; i++ ){

        JSONObject jsonObject = resourcesJsonArray.getJSONObject(i);
        String resourceInternalID = jsonObject.getString("internalId");

        if(resourceInternalID.equals(internalID)){
            /*
             * Found the resource with the requested internal id
             */
            JSONObject resourceJsonObject =  jsonObject.getJSONObject("resource");
            resourceId = resourceJsonObject.getString("id");
            System.out.println("Found resource id = " + resourceId);
            return  resourceId;
        }
    }

    System.out.println("Not found resource with internal id = " + internalID);


    return resourceId;
}//end
//------------------------------------------------------------------------------------------
    public void addResourceID(String resourceID){
        /*
         * Check first if the resourceID
         * is already stored.
         */
        for (String id : resourceIdList) {
            if(id.equals(resourceID)){
                System.out.println("resourceID " + resourceID + " already is stored");
                return;
            }
        }
        resourceIdList.add(resourceID);
}//end
//---------------------------------------------------------------------------------------
public void removeResourceID(String resourceID){
    /*
     * Check first if the resourceID
     * is already stored.
     */
    for (String id : resourceIdList) {
        if(id.equals(resourceID)){
            resourceIdList.remove(resourceID);
            return;
        }
    }
}//end
//------------------------------------------------------------------------------------------
 public List<String> getListOfResourceIds(){
   return resourceIdList;
}//end
//--------------------------------------------------------------------------------------
public  String getResourceIdFromInternalID(String internalID,String token){

    RestTemplate restTemplate = new RestTemplate();

    /*
     * Build headers.
     */

    HttpHeaders httpHeaders   = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);

    /*
     * Build Body.
     */

    Map<String,String> map = new HashMap<>();
    map.put("asapatoken",token);
    map.put("resourceinternalid",internalID);
    map.put("resourceplatformid",platformID);

    HttpEntity<Map<String,String>> entity = new HttpEntity<>(map,httpHeaders);
    ResponseEntity<?>  responseEntity     = restTemplate.postForEntity(GET_DEVICE_ID_URL,entity,String.class);
    HttpStatus httpStatus = responseEntity.getStatusCode();

    if(httpStatus.value() == 200) {
        String body = (String) responseEntity.getBody();
        JSONObject jsonObject = new JSONObject(body);
        String resourceId     = (String)jsonObject.get("result");
        return resourceId;
    }else {
        ERROR_MESSAGE = "Response code: " + httpStatus.value();
        return null;
    }

}//end
//--------------------------------------------------------------------------------------
public  boolean restartSession(WebSocketContainer container,TestWebSocketClient.LocalClientSocket clientSocket) {

    System.out.println("Restarting session");

    String endpointURI = null;

    try {
        /*
         * Create now new session.
         * Connect again to RAP.
         */
        session = container.connectToServer(clientSocket, URI.create(getWebSocketURL()));
        setSession(session);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println(e.getMessage());
        return false;
    }

    if (session != null) {

        /*
         * Create the subscribe resource
         * request json message.
         */

        for (String resourceId:resourceIdList) {
            String message = null;
            try {
                message = getMessage(SUBSCRIBE_COMMAND, resourceId);
            } catch (Exception ex) {
                System.out.println(ex.getMessage().toString());
                return false;
            }

            /*
             * Send the subscribe command
             * to the remote RAP microservice.
             */
            try {
                sendMessageToRAP(message);
            } catch (Exception ex) {
                System.out.println("Failed to send message to RAP");
                return false;
            }
        }
    }else
        return false;

    return true;
}//end
//--------------------------------------------------------------------------------------
    public  String getResourceIdFromInternalID(String internalID,boolean checkIfIsObserved){
        String coreAddress      =  this.coreUrl;
        String keystorePath     = "testKeystore" +  System.currentTimeMillis();
        String keystorePassword = "testKeystore";
        String exampleHomePlatformIdentifier = "SymbIoTe_Core_AAM";

        Set<String> platformIds = new HashSet<>(Collections.singletonList(exampleHomePlatformIdentifier));
        AbstractSymbIoTeClientFactory.Type type = AbstractSymbIoTeClientFactory.Type.FEIGN;

        // Get the configuration
        AbstractSymbIoTeClientFactory.Config config = new AbstractSymbIoTeClientFactory.Config(coreAddress, keystorePath, keystorePassword, type);

        // Get the factory
        AbstractSymbIoTeClientFactory factory;
        try {
            factory = getFactory(config);

            // OPTIONAL section... needs to be run only once
            // - per new platform
            // and/or after revoking client certificate in an already initialized platform


            // ATTENTION: This MUST be an interactive procedure to avoid persisting credentials (password)
            // Here, you can add credentials FOR MORE THAN 1 platforms
            Set<AbstractSymbIoTeClientFactory.HomePlatformCredentials> platformCredentials = new HashSet<>();

            // example credentials
            String username = this.userName;
            String password = this.password;
            String clientId = "webSocketClient";
            AbstractSymbIoTeClientFactory.HomePlatformCredentials exampleHomePlatformCredentials = new AbstractSymbIoTeClientFactory.HomePlatformCredentials(
                    exampleHomePlatformIdentifier,
                    username,
                    password,
                    clientId);
            platformCredentials.add(exampleHomePlatformCredentials);
            /*
             * Get Certificates for the specified platforms.
             */
            factory.initializeInHomePlatforms(platformCredentials);


            // end of optional section..
            // After running it the first time and creating the client keystore you should comment out this section.
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception1 raised");
            ERROR_MESSAGE = "EXCEPTION 1: " + e.getMessage();
            return null;
        }

        /*
         * Get the necessary component clients.
         */

        try {
            factory = getFactory(config);
        }catch(Exception ex){
            System.out.println("Exception2");
            ERROR_MESSAGE = "EXCEPTION 2: " + ex.getMessage();
            return null;
        }

        CRAMClient cramClient       = null;
        RAPClient rapClient         = null;
        RHClient rhClient           = null;
        CloudResource cloudResource = null;

        try {
            // SearchClient searchClient   = factory.getSearchClient();
            cramClient    = factory.getCramClient();
            rapClient     = factory.getRapClient();
            rhClient      = factory.getRHClient(this.platformID);
            if(rhClient == null){
                ERROR_MESSAGE = INVALID_PLATFORM_ID + ": " + this.platformID;
                return null;
            }
            cloudResource = rhClient.getResource(internalID);
            // prcClient =factory.getPRClient("wew");
        }catch(Exception ex ){
            ERROR_MESSAGE = "Exception 3: " + ex.getMessage();
            if(rhClient == null)
                ERROR_MESSAGE += " rhClient is null, check platform id, ";
            if(cramClient == null)
                ERROR_MESSAGE += " cramClient is null, ";
            if(rapClient == null)
                ERROR_MESSAGE += " rapClient is null, ";
            return null;
        }

        if(cloudResource == null){
            ERROR_MESSAGE = CLOUD_RESOURCE_NULL + ". Check the validity of internal id: " + internalID + ". Is registered ? ";
            return null;
        }

        String resourceId = cloudResource.getResource().getId();

        if(resourceId == null){
            ERROR_MESSAGE = RESOURCE_ID_NULL;
            return null;
        }

        String resourceUrl = "";

        try {
            String id   = cloudResource.getResource().getId();
            ResourceUrlsResponse resourceUrlsResponse = cramClient.getResourceUrl(cloudResource.getResource().getId(), true, platformIds);
            resourceUrl = resourceUrlsResponse.getBody().get(cloudResource.getResource().getId());
            Observation observation = null;

            if(checkIfIsObserved == true) {
                observation = rapClient.getLatestObservation(resourceUrl, true, platformIds);//check the correct value of platformIds
                //System.out.println("Latest Observation = " + observation.toString());
            }

        }catch(Exception ex ){
            ERROR_MESSAGE = "EXCEPTION 4: " + ex.getMessage();
            return null;
        }


        return resourceId;
    }//end
//---------------------------------------------------------------------------------------------


}//end of class
