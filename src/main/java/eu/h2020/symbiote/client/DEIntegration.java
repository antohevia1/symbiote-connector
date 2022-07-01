package eu.h2020.symbiote.client;


import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;




public class DEIntegration {

    public String server = "0gpy0ohtge.execute-api.eu-west-1.amazonaws.com"; //"http://host.docker.internal:3030/employees";
    public String stage = "dev";
    public String path = "save_data";
    public String protocol = "https://";
    public String url = protocol+server+"/"+stage+"/"+path;

    

    public void sendMessageToDE(String message) throws IOException {
        HttpClient client = new DefaultHttpClient();
        System.out.println(url);
        HttpPost post = new HttpPost(url);
        StringEntity input = new StringEntity(message);
        input.setContentType("application/json");
        post.setEntity(input);
        HttpResponse response = client.execute(post);
        

    }


    


    
}
