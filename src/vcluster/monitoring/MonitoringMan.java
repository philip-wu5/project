package vcluster.monitoring;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import vcluster.control.vmman.VmManager;
import vcluster.global.Config;
import vcluster.util.PrintMsg;
import vcluster.util.PrintMsg.DMsgType;

public class MonitoringMan extends Thread {
	
	public MonitoringMan(VmManager vmman)
	{
		if (vmman == null) {
			PrintMsg.print(DMsgType.ERROR, "vmman is null");
		}
		msgQueue =  new ArrayBlockingQueue <MonMessage>(100);
		qc = new QStatusChecker(Config.DEFAULT_SLEEP_SEC, msgQueue);

	}
	
	public void dump()
	{
		qc.printQStatusChecker();
		
	}
	
	public void run() {
		qc.start();
		MonMessage aMsg = null;

		while(!done) {
			try {
				aMsg = msgQueue.take();
				PrintMsg.print(DMsgType.MSG, "message type = "+aMsg.toString());
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	

	
	
	public void shutDwon() 
	{
		qc.shutDwon();
		done = true;
	
		/*
		try {
			qc.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
	}
	
	
	
	private boolean done = false;
	private QStatusChecker qc = null;
	private BlockingQueue <MonMessage> msgQueue;



	
}
