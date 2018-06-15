/*
DUISEMBAYEV DIAS, 30.03.2018
 */
package peer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

class Peer extends JFrame implements ActionListener {

    private JButton search;  //Buttons
    private JButton dload;
    private JButton close;
    int localPort;
    int peerPort;
    int isDownloaded = -1;
    boolean clear = false;
    String sendMe;
    String fReceived;
    private JList jl;   // List that will show found files
    private JLabel label; //Label "File Name
    private JTextField tf, tf2; // Two textfields: one is for typing a file name, the other is just to show the selected file
    DefaultListModel listModel; // Used to select items in the list of found files

    ArrayList<String> files; // Files information

    File folder;
    File[] listOfFiles;
    Socket clientSocket;
    Socket peerSocket;
    BufferedReader inFromUser;
    DataOutputStream outToServer;
    DataOutputStream outToPeer;
    BufferedReader inFromServer;

    public Peer() throws IOException, InterruptedException {
        super("Peer GUI");
        setLayout(null);
        setSize(500, 600);

        label = new JLabel("File name:");
        label.setBounds(50, 50, 80, 20);
        add(label);

        tf = new JTextField();
        tf.setBounds(130, 50, 220, 20);
        add(tf);

        search = new JButton("Search");
        search.setBounds(360, 50, 80, 20);
        search.addActionListener(this);
        add(search);

        listModel = new DefaultListModel();
        jl = new JList(listModel);

        JScrollPane listScroller = new JScrollPane(jl);
        listScroller.setBounds(50, 80, 300, 300);

        add(listScroller);

        dload = new JButton("Download");
        dload.setBounds(200, 400, 130, 20);
        dload.addActionListener(this);
        add(dload);

        tf2 = new JTextField();
        tf2.setBounds(200, 430, 130, 20);
        add(tf2);

        close = new JButton("Close");
        close.setBounds(360, 470, 80, 20);
        close.addActionListener(this);
        add(close);

        setVisible(true);

        folder = new File(System.getProperty("user.dir") + "\\src\\shared");
        listOfFiles = folder.listFiles();

        clientSocket = new Socket("localhost", 43434);

        try {
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }

        Thread in = new Thread() {
            @Override
            public void run() {
                try {
                    boolean hello = false;
                    outToServer.writeBytes("HELLO\n");
                    String rec;
                    while (true) {
                        rec = inFromServer.readLine();
                        System.out.flush();
                        if (hello) {
                            int count = 0;
                            if (clear) {
                                listModel.clear();
                            }
                            System.out.println("CLEARED");
                            while (!rec.equalsIgnoreCase("DONE")) {
                                System.out.println(rec);
                                if (!listModel.contains(rec)) {
                                    listModel.add(count, rec);
                                    clear = false;
                                } else {
                                    clear = true;
                                }
                                rec = inFromServer.readLine();
                                count++;
                            }
                            if (listModel.isEmpty()) {
                                listModel.add(0, "NOT FOUND");
                            }
                        }

                        if (rec.equalsIgnoreCase("Hi") && !hello) {
                            hello = true;
                            if (listOfFiles.length > 0) {
                                for (File file : listOfFiles) {
                                    String records = "";
                                    if (file.isFile()) {
                                        String fileName = "";
                                        String extension = "";
                                        int i = file.getName().lastIndexOf(".");
                                        if (i > 0) {
                                            fileName = file.getName().substring(0, i);
                                            extension = file.getName().substring(i + 1);
                                        }
                                        records = fileName + "," + extension + "," + file.length() + "," + file.lastModified() + "," + "localhost" + "," + localPort;
                                        System.out.println(records);
                                        System.out.println(outToServer);
                                        outToServer.writeBytes(records + "\n");
                                    }
                                }
                                outToServer.writeBytes("DONE\r\n");
                            }

                        }
                    }

                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        };

        Thread send = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket sSocket = new ServerSocket(27015);
                    localPort = sSocket.getLocalPort();
                    while (true) {
                        Socket cSocket = sSocket.accept();
                        InputStream iStream = cSocket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
                        String cMsg;
                        if ((cMsg = reader.readLine()) != null) {
                            System.out.println(cMsg);
                            String[] tokens = cMsg.split(",");
                            if (tokens != null && tokens.length > 1 && tokens[2] != null) {
                                String fNameSend = tokens[0];
                                String fExtSend = tokens[1];
                                int fSizeSend = Integer.parseInt(tokens[2]);
                                for (File file : listOfFiles) {
                                    if (file.isFile()) {
                                        int i = file.getName().lastIndexOf(".");
                                        if (i > 0 && fNameSend.equals(file.getName().substring(0, i)) && fExtSend.equals(file.getName().substring(i + 1)) && file.length() == fSizeSend) {
                                            System.out.println("Found");
                                            // byte[] mybytearray = new byte[(int) file.length()];
                                            FileInputStream fis = new FileInputStream(file);
                                            BufferedInputStream bis = new BufferedInputStream(fis);
                                            //  bis.read(mybytearray, 0, mybytearray.length);
                                            OutputStream oStream = cSocket.getOutputStream();
                                            byte[] buffer = new byte[8192]; // any size greater than 0 will work
                                            int count;
                                            int readBytes = 0;
                                            while ((count = bis.read(buffer)) != -1 || readBytes < fSizeSend) {
                                                readBytes += count;
                                                oStream.write(buffer, 0, count);
                                                oStream.flush();
                                            }
                                            cSocket.close();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        send.start();
        Thread.sleep(2500);
        in.start();
    }

    public static void main(String argv[]) throws IOException, InterruptedException {
        Peer peer = new Peer();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == search) { //If search button is pressed show 25 randomly generated file info in text area             
            String fileName = tf.getText();
            System.out.println(fileName);
            sendMe = fileName;
            String tokenR[] = fileName.split(",");
            fReceived = tokenR[0] + "." + tokenR[1];
            try {
                outToServer.writeBytes("SEARCH:," + fileName + "\n");
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (e.getSource() == dload) {   //If download button is pressed get the selected value from the list and show it in text field
            String selected = jl.getSelectedValue().toString();
            String[] tokens = selected.split(" ");
            peerPort = Integer.parseInt(tokens[1]);
            Thread receive = new Thread() {
                @Override
                public void run() {
                    Random ran = new Random();
                    int x = ran.nextInt(101);
                    if (x > 50) {
                        System.out.println("x>50");
                        try {
                            peerSocket = new Socket("localhost", peerPort);
                            outToPeer = new DataOutputStream(peerSocket.getOutputStream());
                            System.out.println(sendMe);
                            outToPeer.writeBytes(sendMe + "\n");
                            String[] tokens = sendMe.split(",");

                            byte[] mybytearray = new byte[10000000];
                            InputStream is = peerSocket.getInputStream();
                            FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "\\src\\received\\" + fReceived);
                            BufferedOutputStream bos = new BufferedOutputStream(fos);

                            int count;
                            int readBytes = 0;
                            byte[] buffer = new byte[8192];
                            while ((count = is.read(buffer)) != -1 || readBytes < Integer.parseInt(tokens[2])) {
                                readBytes += count;
                                bos.write(buffer, 0, count);
                                bos.flush();
                            }

                            bos.flush();
                            fos.close();
                            bos.close();
                            peerSocket.close();
                            isDownloaded = 1;
                            outToServer.writeBytes("SCORE," + peerPort + "," + 1 + "\n");
                        } catch (IOException ex) {
                            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        isDownloaded = 0;
                        try {
                            outToServer.writeBytes("SCORE," + peerPort + "," + 0 + "\n");
                        } catch (IOException ex) {
                            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

            };
            receive.start();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (isDownloaded == 1) {
                tf2.setText(jl.getSelectedValue().toString() + " donwloaded");
            } else {
                tf2.setText("Failed");
            }
        } else if (e.getSource() == close) {
            try {
                outToServer.writeBytes("BYE\n");
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
        }

    }

}
