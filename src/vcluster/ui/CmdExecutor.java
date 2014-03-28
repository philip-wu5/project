package vcluster.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import vcluster.control.vmman.VmManager;
import vcluster.engine.groupexecutor.BatchExecutor;
import vcluster.engine.groupexecutor.CloudmanExecutor;
import vcluster.engine.groupexecutor.PlugmanExecutor;
import vcluster.engine.groupexecutor.VClusterExecutor;
import vcluster.global.Config;
import vcluster.plugins.plugman.PluginManager;

/**
 * @author Seo-Young Noh, Modified by Dada Huang
 * This class 
 * 
 */
public class CmdExecutor {

	/**
	 * if quit, it checks if monitoring process and vm manager process are still running
	 */
	public static void quit()
	{
		/* shutdown Manager first */
		if (Config.monMan != null) Config.monMan.shutDwon();
	
	}

	public static boolean isQuit(String aCmd)
	{
		String cmd = aCmd.trim();
		if(Command.QUIT.contains(cmd)) {
			/* shutdown Manager first */
			if (Config.monMan != null) Config.monMan.shutDwon();	
			
			return true;
		}

		return false;
	}
	
	public static boolean execute(String cmdLine)
	{
		StringTokenizer st = new StringTokenizer(cmdLine);
		
		String cmd = st.nextToken().trim();
		
		Command command = getCommand(null,cmd);
				
		switch(command.getCmdGroup()){
		case VCLMAN:return executeVCLMAN(command, cmdLine);
		default:
			break;
		}

		switch (command) {
		case VMMAN: return executeVMMAN(cmdLine);
		case CLOUDMAN:return executeCLOUDMAN(cmdLine);
		case PLUGMAN: return executePLUGMAN(cmdLine);		
		case NOT_DEFINED: return false;
		default:
			break;
		}		
		return true;
	}
	
	private static boolean executeCLOUDMAN(String cmdLine) {
		// TODO Auto-generated method stub
		StringTokenizer st = new StringTokenizer(cmdLine);
		String cmdg= st.nextToken().trim();		
		String cmd = st.nextToken().trim();
		Command command = getCommand(Command.CMD_GROUP.CLOUDMAN.toString(),cmd);
		cmdLine = cmdLine.replace(cmdg, "").trim();
		switch (command) {
		case REGISTER:
			return CloudmanExecutor.register(cmdLine); 
		case LOADCLOUD:
			return CloudmanExecutor.load(cmdLine);
		case LISTCLOUD:
			return CloudmanExecutor.list(cmdLine);
		case UNLOADCLOUD:
			return CloudmanExecutor.unload(cmdLine);	
		case HOSTON:
			return CloudmanExecutor.hoston(cmdLine);
		case HOSTOFF:
			return CloudmanExecutor.hostoff(cmdLine);	
		default:
			return CloudmanExecutor.undefined(cmdLine);					
		}		
	}

	private static boolean executePLUGMAN(String cmdLine) {
		cmdLine = cmdLine.replace("plugman ", "");
		StringTokenizer st = new StringTokenizer(cmdLine);
		String cmd = st.nextToken().trim();
		//System.out.println(cmdLine);
		Command command = getCommand(Command.CMD_GROUP.PLUGMAN.toString(),cmd);
		
		switch (command) {
		case LOAD:
			return PlugmanExecutor.load(cmdLine); 
		case UNLOAD:
			return PlugmanExecutor.unload(cmdLine);
		case INFO:
			return PlugmanExecutor.getInfo(cmdLine);
		case LIST:
			return PlugmanExecutor.list(cmdLine);
		default:
			return PlugmanExecutor.undefined(cmdLine);					
		}

	}

	private static boolean executeVCLMAN(Command command, String cmdLine)
	{
		
		switch (command) {
		case TESTDEMO:
			 PluginManager.current_loadbalancer.activate();	
			 return true;		
		case TESTALGO:
			return vcluster.plugins.PriorityBased.algo();			
		case TESTCHKQ:
			String time = cmdLine.split(" ")[1].trim();
			int t = Integer.parseInt(time);
			return vcluster.plugins.PriorityBased.chkq(t);	
		case VHELP:
			return VClusterExecutor.help();
		case DEBUG_MODE:
			return VClusterExecutor.debug_mode(cmdLine);
		case VMMAN:
			return VClusterExecutor.vmman(cmdLine);
		case MONITOR:
			return VClusterExecutor.monitor(cmdLine);
		case CLOUDMAN:
			return VClusterExecutor.cloudman(cmdLine);
		case LOADCONF:
			return VClusterExecutor.load(cmdLine);
		case CHECK_P: 
				if(PluginManager.current_proxyExecutor==null){
					if(yesORno()){
						PlugmanExecutor.load("load -b proxy-HTCondor");	
					}else{					
						return false;
						}

				}
			try {
				return BatchExecutor.getPoolStatus().printPoolStatus();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				return false;
			}
			
	    case CHECK_Q: 
			if(PluginManager.current_proxyExecutor==null){
				if(yesORno()){
					PlugmanExecutor.load("load -b proxy-HTCondor");				
				}else{
					return false;
				}
			}
	    	return PluginManager.current_proxyExecutor.getQStatus().printQStatus();
		default:
			break;		
			
		}
		
		return true;
	}
	
	private static boolean yesORno(){
		System.out.println("No batch system,do you want to load proxy-HTCondor plugin?(y/n)");
	    String yn = "";
	    InputStreamReader input = new InputStreamReader(System.in);
	    BufferedReader reader = new BufferedReader(input);
	    
	    try {
		    /* get a command string */
	    	yn = reader.readLine(); 
	    	if(yn.equalsIgnoreCase("y")){return true;}else if(yn.equalsIgnoreCase("n")){return false;}
	    	else{System.out.println("has to be y or n!");return false;}
	    }
	    catch(Exception e){return false;}
	}
	

	private static boolean executeVMMAN(String cmdLine)
	{		
		cmdLine = cmdLine.replace("vmman ", "");
		StringTokenizer st = new StringTokenizer(cmdLine);
		String cmd = st.nextToken().trim();
		Command command = getCommand(Command.CMD_GROUP.VMMAN.toString(),cmd);
		
		switch (command) {
		case SHOW: return VmManager.showVM(cmdLine);
		case CREATE: return VmManager.createVM(cmdLine);
		case LISTVM: return VmManager.listVM(cmdLine);
		case DESTROY: return VmManager.destroyVM(cmdLine);
		case SUSPEND: return VmManager.suspendVM(cmdLine);
		case START: return VmManager.startVM(cmdLine);
		case MIGRATE:
			return VmManager.migrate(cmdLine);
		default:System.out.println("command is not defined"); 
			break;
		}
		
		return true;
	}
	
	public static Command getCommand(String cmdGroup, String aCmdLine) 
	{
		StringTokenizer st = new StringTokenizer(aCmdLine);
		String aCmd = st.nextToken().trim();
    	if (cmdGroup==null){
            for (Command cmd : Command.values()){
            	if (cmd.contains(aCmd)) return cmd;
            }
    	}
        for (Command cmd : Command.values()){
        	if (cmd.getCmdGroup().toString().equalsIgnoreCase(cmdGroup)&cmd.contains(aCmd)) return cmd;
        }
        return Command.NOT_DEFINED;
 	}
}
