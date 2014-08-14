package vcluster.plugin.balancer.fermibalancer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
public class InfoCollector implements Runnable{

	private int port;
	public InfoCollector(int portNo){
		port=portNo;}
	
	public void run(){
		try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Waiting for clients to connect...");
            while (!vcluster.Vcluster.terminate) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new clientInfoCollector(clientSocket));
                clientThread.start();
                //clientInfoCollector client=new clientInfoCollector(clientSocket);
                //client.run();
            }
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Unable to process client request");
            e.printStackTrace();
        }
    }
	
	
}

