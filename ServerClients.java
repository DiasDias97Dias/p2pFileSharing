/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ft;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Диас
 */
public class ServerClients extends Thread {

    private final int port;
    public HashMap<Integer,Score> hm = new HashMap< Integer, Score>(); 
    public HashMap<Integer,Score> hmAll = new HashMap< Integer, Score>(); 
    public ArrayList<CFile> ht = new ArrayList<>();
   public  ServerClients(int i) {
       this.hm=hm;
       this.ht=ht;
        this.port=i;
    }
  

   @Override
   public void run(){
       
        try {
            ServerSocket  sSocket =  new ServerSocket(port);
            
            while(true){
                //here we accept new socket and create sClient, which is a list of clients
                Socket cSocket =   sSocket.accept();
                String ipAddress=sSocket.getLocalSocketAddress().toString();
                ServerClient sClient = new ServerClient(this, cSocket, ipAddress);
                sClient.start();
                
            }   } catch (IOException ex) {
            Logger.getLogger(ServerClients.class.getName()).log(Level.SEVERE, null, ex);
        }
      
   
   }



    public static class CFile {
        String fileName;
        String fileType;
        int fileSize;
        String date;
        String ipAddress;
        int    port;
        public CFile(String n, String t, int s, String d, String i,  int p) {
           this.fileName=n;
           this.fileSize=s;
           this.fileType=t;
           this.date=d;
           this.ipAddress=i;
           this.port=p;
        }

    }

    public static class Score {
        float score;
        int requests;
        int uploads;
        public Score(int s, int r, int u) {
        this.score=s;
        this.requests=r;
        this.uploads=u;
        }
    }

}
