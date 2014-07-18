package vcluster.executors;

import java.util.ArrayList;
import java.util.TreeMap;

import org.opennebula.client.host.Host;

import vcluster.Vcluster.uiType;
import vcluster.elements.*;
import vcluster.managers.CloudManager;
import vcluster.ui.CmdComb;
/**
 *A class representing Cloud manager executor. Cloud-related commands would be sent to this class and the commands will be analyzed,
 *then corresponding functions will be invoked according the commands.
 *
 */
public class CloudmanExecutor {


	/**
	 *Execute the command of register clouds on vcluster
	 *step 1. analyze the command, extract the parameters  
	 *step 2. invoke the corresponding function in cloud manager class and send the parameters.
	 *@param cmdLine, the command line of register cloud from a conf file.
	 */
	public static String register(CmdComb cmd) {
		// TODO Auto-generated method stub
		StringBuffer str = new StringBuffer();
		if (cmd.getParaset().size()==0) {
			str.append("[USAGE] : cloudman register <cloudelement conf file>");
			System.out.println(str);
			return str.toString();
		}
		String confFile = cmd.getParaset().get(0);
		//System.out.println(token);
		String result =  CloudManager.registerCloud(confFile);
		if(cmd.getUi().equals(uiType.CMDLINE))System.out.println(result);
		return result;
	}

	
	/**
	 * Load a cloud into vcluster. command line would be analyzed and the corresponding function in cloud manager would be invoked.
	 */
	public static String load(CmdComb cmd) {
		// TODO Auto-generated method stub
		if (cmd.getParaset().size()==0) {
			System.out.println("[ERROR : ] Expect a cloud name!");
			return "[ERROR : ] Expect a cloud name!";
		}
		else{	
			ArrayList<String> para = cmd.getParaset();
			int size = para.size();
			String [] arg =(String [])para.toArray(new String[size]);	
			
			String result =  CloudManager.loadCloud(arg);
			if(cmd.getUi().equals(uiType.CMDLINE))System.out.println(result);
			return result;
		}
	}

	/**
	 * list up all the registered clouds, the relevant information would be list up.  
	 */
	public static TreeMap<String,Cloud> getCloudList() {
		// TODO Auto-generated method stub
		return CloudManager.getCloudList();
	}

	public static boolean undefined(CmdComb cmd) {
		// TODO Auto-generated method stub
		/* skip the command */
		
		if (cmd.getParaset().size()==0) {
			System.out.println("[USAGE] : cloudman dump [<private | public>>]");
			System.out.println("        : cloudman register <cloudelement conf file>");
			System.out.println("        : cloudman set <private | public> <cloud num>");			
			return false;
		}		
		return true;
	}

	/**
	 *unload a cloud from vcluster. the cloud status would be change to "unloaded", 
	 *and the connection between vcluster and related real cloud system would be cut off.
	 */
	public static boolean unload(CmdComb cmd) {
		// TODO Auto-generated method stub
		
		if (cmd.getParaset().size()==0) {
			System.out.println("[ERROR : ] Expect a cloud name!");
			return false;
		}else{			
			String [] arg = (String[])cmd.getParaset().toArray();	
			
			boolean result =  CloudManager.unLoadCloud(arg);
			return result;
		}
		
	}

	/**
	 *Turn on a physical host 
	 */
	public static boolean hoston(CmdComb cmd) {
		// TODO Auto-generated method stub
		
		if (cmd.getParaset().size()==0) {
			System.out.println("[ERROR : ] Expect a cloud name!");
			return false;
		}
		String cloudname = cmd.getParaset().get(0);
		if (cmd.getParaset().size()<2) {
			System.out.println("[ERROR : ] Expect a host id!");
			return false;
		}
		String hostID = cmd.getParaset().get(1).trim();
		Cloud cloud = CloudManager.getCloudList().get(cloudname);
		
		
		return cloud.hoston(hostID);
		
		}

	/**
	 * Shut down a physical host.
	 */
	public static boolean hostoff(CmdComb cmd) {
		// TODO Auto-generated method stub
		if (cmd.getParaset().size()==0) {
			System.out.println("[ERROR : ] Expect a cloud name!");
			return false;
		}
		String cloudname = cmd.getParaset().get(0).trim();
		if (cmd.getParaset().size()<2) {
			System.out.println("[ERROR : ] Expect a host id!");
			return false;
		}
		String hostID = cmd.getParaset().get(1).trim();
		
		Cloud cloud = CloudManager.getCloudList().get(cloudname);
		
		
		return cloud.hostoff(hostID);
		
	}


	public static String dump(CmdComb cmd) {
		// TODO Auto-generated method stub
		StringBuffer str = new StringBuffer();
		if(cmd.getParaset().size()==0){
			System.out.println("[ERROR : ] Expect a Cloud name!");
			return str.append("[ERROR : ] Expect a Cloud name!").toString();
		}
		Cloud c = CloudManager.getCloudList().get(cmd.getParaset().get(0));
		c.dump();
		String tId=  String.format("%-8s","ID");
		String tName=  String.format("%-20s", "Name");
		String tMax = String.format("%-6s", "Max");
		String tStat =  String.format("%-6s","stat");
		String tip = String.format("%-20s","Private IP");
		str.append("-------------------------------------------------------"+System.getProperty("line.separator"));
		str.append(tId+tName+tMax+tStat+tip+System.getProperty("line.separator"));
		str.append("-------------------------------------------------------"+System.getProperty("line.separator"));
		if(!(c.getHostList()==null||c.getHostList().size()==0)){
		for(vcluster.elements.Host h : c.getHostList().values()){
			String id = String.format("%-8s", h.getId());
			String name = String.format("%-20s",h.getName());
			String max = String.format("%-6s",h.getMaxVmNum()+"");
			String stat = String.format("%-6s",h.getStat().toString());
			String ip = String.format("%-20s", h.getIpmiID());
			if(h.getPowerStat()==0)
			stat = String.format("%-6s","OFF");
			str.append(id+name+max+stat+ip+System.getProperty("line.separator"));
		}
		}
		str.append("-------------------------------------------------------"+System.getProperty("line.separator"));
		if(cmd.getUi()==uiType.CMDLINE)System.out.println(str);
		return str.toString();
	}


	public static String hostlist(CmdComb cmd) {
		// TODO Auto-generated method stub
		StringBuffer str=new StringBuffer();
		String tId=  String.format("%-8s","ID");
		String tName=  String.format("%-20s", "Name");
		String tStat =  String.format("%-6s","stat");
		String tTcpu = String.format("%-8s","TCPU");
		String tAcpu = String.format("%-8s", "ACPU");
		String tFcpu = String.format("%-8s", "FCPU");
		String tTmem = String.format("%-12s","TMEM");
		String tAmem = String.format("%-12s", "AMEM");
		String tFmem = String.format("%-12s", "FMEM");
		String tVms = String.format("%-4s", "VMs");
		String tRunVms=String.format("%-4s", "RunVMs");
		str.append("-------------------------------------------------------------------------------------------------"+System.getProperty("line.separator"));
		str.append(tId+tName+tStat+tTcpu+tAcpu+tFcpu+tTmem+tAmem+tFmem+tVms+tRunVms+System.getProperty("line.separator"));
		str.append("-------------------------------------------------------------------------------------------------"+System.getProperty("line.separator"));
		for (Cloud c : CloudManager.getCloudList().values()){
			if (!c.isLoaded())
				c.load();
			for(vcluster.elements.Host h: c.getHostList().values())
			{
				String hId=  String.format("%-8s", h.getId());
				String hName= String.format("%-20s", h.getName());
				String hStat = String.format("%-6s", h.getStat().toString());
				String hTcpu = String.format("%-8s",Integer.toString(h.getTCPU()));
				String hAcpu = String.format("%-8s",Integer.toString(h.getACPU()));
				String hFcpu = String.format("%-8s",Integer.toString(h.getFCPU()));
				String hTmem = String.format("%-12s",Integer.toString((int)h.getTMEM()));
				String hAmem = String.format("%-12s",Integer.toString((int)h.getAMEM()));
				String hFmem = String.format("%-12s",Integer.toString((int)h.getFMEM()));
				String hVms = String.format("%-4s", Integer.toString(h.getVmList().size()));
				String hRunVms = String.format("%-4s", Integer.toString(h.getCurrVmNum()));
				str.append(hId+hName+hStat+hTcpu+hAcpu+hFcpu+hTmem+hAmem+hFmem+hVms+hRunVms+System.getProperty("line.separator"));
			}
		}
		str.append("--------------------------------------------------------------------------------------------------"+System.getProperty("line.separator"));
		if(cmd.getUi()==uiType.CMDLINE)System.out.println(str);
		return str.toString();
	}


	public static String showhost(CmdComb cmd) {
		// TODO Auto-generated method stub
		return null;
	}



}
