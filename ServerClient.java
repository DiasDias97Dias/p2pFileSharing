/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ft;

import ft.ServerClients.CFile;
import ft.ServerClients.Score;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Диас
 */
public class ServerClient extends Thread {

    private final Socket cSocket;
    private final ServerClients sClients;
    private String ipAddress = null;
    private int cPort = -1;
    private boolean logged = false;
    private OutputStream oStream;

    public ServerClient(ServerClients sClients, Socket cSocket, String ipAddress) {
        this.sClients = sClients;
        this.cSocket = cSocket;
        this.ipAddress = ipAddress;
    }

    //this threads handles multiple clients
    @Override
    public void run() {
        try {
            createClientThread();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(ServerClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createClientThread() throws IOException, InterruptedException {
        this.oStream = cSocket.getOutputStream();
        InputStream iStream = cSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
        String cMsg;
        while ((cMsg = reader.readLine()) != null) {
            System.out.println(cMsg);
            if (cMsg.equals("BYE")) {
                System.out.println("BYE received");
                sClients.hmAll.put(cPort, sClients.hm.get(this.cPort));
                sClients.hm.remove(this.cPort);
                break;
            }
            if (cMsg.equals("HELLO")) {
                System.out.println("HELLO received");
                oStream.write("Hi\r\n".getBytes());
            }

            if (logged == false) {
                if (cMsg.equals("DONE")) {
                    System.out.println("DONEr");
                    Score score = new Score(0, 0, 0);
                    System.out.println("hm " + cPort);
                    if (sClients.hmAll.containsKey(cPort)) {
                        System.out.println("Registered");
                        sClients.hm.put(cPort, sClients.hmAll.get(cPort));
                    } else {
                        sClients.hm.put(cPort, score);
                    }
                    logged = true;
                } else {
                    String[] tokens = cMsg.split(",");
                    if (tokens != null && tokens.length > 1 && tokens[3] != null) {
                        ipAddress = tokens[4];
                        cPort = Integer.parseInt(tokens[5]);

                        long dateInMillis = Long.parseLong(tokens[3]);
                        Date date = new Date(dateInMillis);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
                        String lastModified = dateFormat.format(date);

                        CFile cfile = new CFile(tokens[0], tokens[1], Integer.parseInt(tokens[2]), lastModified, this.ipAddress, this.cPort);
                        sClients.ht.add(cfile);
                        System.out.println("File added");
                    }
                }
            }
            if (logged == true) {
                String[] tokens = cMsg.split(",");
                if (tokens[0].equals("SEARCH:")) {
                    System.out.println("Searching...");
                    getResult(tokens[1], tokens[2], Integer.parseInt(tokens[3])).getBytes();
                }
                if (tokens[0].equals("SCORE")) {
                    System.out.println("Updating");
                    updateScore(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
                }
            }
        }
        cSocket.close();

    }

    public String getResult(String fileName, String fileType, int fileSize) throws IOException {
        boolean found = false;
        String result = "FOUND:";
        for (CFile cf : sClients.ht) {
            if (cf.fileName.equals(fileName) && cf.fileType.equals(fileType) && cf.fileSize == fileSize && sClients.hm.containsKey(cf.port)) {
                System.out.println("FOUND " + cf.port);
                found = true;
                result = "IP&Port:" + cf.ipAddress + " " + cf.port + " Date:" + cf.date + " Score:" + sClients.hm.get(cf.port).score * 100 + "%\r\n";
                oStream.write(result.getBytes());
                System.out.println("Result");
            }
        }
        System.out.println("Send");
        result = "DONE\r\n";
        oStream.write(result.getBytes());
        return result;
    }

    public void updateScore(int port, int score) {
        Score newScore = sClients.hm.get(port);
        newScore.requests++;
        newScore.uploads += score;
        newScore.score = (float) newScore.uploads / newScore.requests;
        sClients.hm.replace(port, newScore);
    }

}
