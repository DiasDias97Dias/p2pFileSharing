/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ft;





/**
 *
 * @author Диас
 */

public class Server {
    public static void main(String argv[]) throws Exception {
        ServerClients sClients = new ServerClients(43434);
        sClients.start();
}
}
