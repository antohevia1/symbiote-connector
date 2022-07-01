package eu.h2020.symbiote.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KeepAliveThread extends Thread{

 public List<WebSocketApi> webSocketApiList = new ArrayList<WebSocketApi>();
 public boolean running = false;

    public void startThread(){

        if(running == true){
            System.out.println("Thread already running");
            return;
        }
        running = true;
        start();
    }//end
//----------------------------------------------------------------------
   public void stopThread(){
      running = false;
   }//end
//----------------------------------------------------------------------
    public void run(){
        System.out.println("MyThread running");
        while(running == true) {

            for (WebSocketApi w : webSocketApiList) {
                try {
                    System.out.println("Sending keep alive message");
                    w.sendKeepAliveMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to send keep alive message");
                }
            }

            try {
                Thread.sleep(WebSocketApi.KEEP_ALIVE_MESSAGE_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("thread is terminated");

    }//end
//-------------------------------------------------------
    public void addWebSocketApi(WebSocketApi webSocketApi){
     webSocketApiList.add(webSocketApi);
    }//end
//--------------------------------------------------------
   public void removeWebSocketApi(WebSocketApi webSocketApi){
    if(webSocketApi != null)
      webSocketApiList.remove(webSocketApi);
   }//end

}//end of class
