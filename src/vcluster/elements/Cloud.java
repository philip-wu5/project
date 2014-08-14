package vcluster.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import vcluster.Vcluster;
import vcluster.executors.BatchExecutor;
import vcluster.executors.PlugmanExecutor;
import vcluster.managers.CloudManager;
import vcluster.managers.PluginManager;
import vcluster.managers.VmManager;
import vcluster.managers.VmManager.VMState;
import vcluster.plugInterfaces.CloudInterface;
import vcluster.ui.CmdComb;
import vcluster.elements.Host;


/**
 *A class representing a cloud 
 * 
 */
public class Cloud{
	
	/**
	 *The constructor without any member initiation.
	 * 
	 */
	public Cloud() {
		// TODO Auto-generated constructor stub
	}
	/**
	 *The constructor, by using this constructor some of the member value will be initiated.
	 *@param conf, a List of the cloud configuration value. Such as the cloud name, interface name, etc.
	 * 
	 */
	public Cloud(List<String> conf) {
		this.conf = conf;
		for(String aLine : conf){
			
			StringTokenizer st = new StringTokenizer(aLine, "=");
			
			if (!st.hasMoreTokens()) break;
			
			/* get a keyword */
			String aKey = st.nextToken().trim();
		
			/* get a value */
			if (!st.hasMoreTokens()) break;

			String aValue = st.nextToken().trim();
			
			if (aKey.equalsIgnoreCase("type")){
				setCloudType(aValue);	
				
			}else if((aKey.equalsIgnoreCase("Interface"))){
				setCloudpluginName(aValue);
				cp = (CloudInterface)PluginManager.pluginList.get(cloudpluginName).getInstance();
			}else if((aKey.equalsIgnoreCase("Name"))){
				//System.out.println("name");
				setCloudName(aValue);
			//	System.out.println(aValue);
			}else if(aKey.equalsIgnoreCase("hosts")){
			//	System.out.println(aValue);
				hostList = new TreeMap<String,Host> ();
				String [] hostlist = aValue.split(",");
				//System.out.println("host");
				for(int i = 0 ; i<hostlist.length;i++){
					String [] hostStr = hostlist[i].split("/");
					String hostId = hostStr[0];
					String hostname = hostStr[1];
					String MaxVMNum = hostStr[2];
					String ipmiID = hostStr[3];
					Host host = new Host(Integer.parseInt(MaxVMNum),hostId,hostname,this.cloudName);
					host.setIpmiID(ipmiID);
					//System.out.println(host.getId()+ i + "");
					hostList.put(hostname,host);
				}
				
			}else if (aKey.equalsIgnoreCase("imagesize")){
				setImageSize(Integer.parseInt(aValue));
			}else if (aKey.equalsIgnoreCase("cluster")){
				setCluster(aValue);
			}
		}
		isLoaded = false;
		
	}
		
	/**
	 *This function is for loading a cloud into vcluster. 
	 *Inovke this function the vcluster would connect the corresponding cloud and get the vms'
	 *information.  
	 */
	public String load(){	
		StringBuffer str = new StringBuffer();
		if(cloudName==null||cloudType==null||cloudpluginName==null){
			return null;
		}
		if(!PluginManager.isLoaded(cloudpluginName))PlugmanExecutor.load(new CmdComb("plugman load -c "+cloudpluginName));		
		cp = (CloudInterface)PluginManager.pluginList.get(cloudpluginName).getInstance();
		this.listVMs();
		this.listHost();
		matchVMtoHost();
		if(getVmList()==null)return null;
		for(Vm vm : getVmList().values()){
			Integer id = new Integer(VmManager.getcurrId());
			VmManager.getVmList().put(id, vm);
		}
		
		CloudManager.setCurrentCloud(this);		
		isLoaded = true;
		String fName = String.format("%-12s", getCloudName());
		String fInterface =String.format("%-20s", getCloudpluginName());
		String fType = String.format("%-12s", getCloudType());
		String fVMs = String.format("%-8s", vmList.size());
		String fHosts = String.format("%-8s", hostList.size());
		str.append(fName+fInterface+fType+fVMs+fHosts);
		//HandleXML.setCloudAttribute(cloudName,"isLoaded", "true");
		//System.out.println(str);
		return str.toString();
	}
	
	public int getCurrentVMs() {
		return currentVMs;
	}
	
	public void setCurrentVMs(int vms) {
		currentVMs = vms;
	}
	
	protected void incCurrentVMs(int vms) {
		currentVMs += vms;
	}
		
	/**
	 *Get the configurations of the cloud
	 *@return A list of configurations, such as cloud name, interface name connection configurations. 
	 */
	public List<String> getConf() {
		return conf;
	}

	/**
	 *Set the configurations of the cloud
	 *@param conf, a list a the configurations. 
	 */
	public void setConf(List<String> conf) {
		this.conf = conf;
	}

	public void dump(){
		for(String aLine : conf){
			System.out.println(aLine);
		}
	}

	/**
	 * To create a virtual machine.
	 * Through the fucntion creates specified number of virtual machines on the specific host of the cloud.
	 * @param maxCount, the number of virtual machines that you want to create.
	 * @param hostId, the host ID where you want to create vms on.
	 */
	public String createVM(int maxCount,String hostName) {
		// TODO Auto-generated method stub
		StringBuffer str = new StringBuffer();
		/*if(!hostName.equalsIgnoreCase("host1")){
			int i = 100;
			for(int j =0;j<conf.size();j++){
				if(conf.get(j).contains("template")){
					i = j;
					break;
				}
			}
			//if(i!=100)conf.set(i, "template = templates/"+hostName+".one");
		}*/
		cp.RegisterCloud(conf);
		ArrayList<Vm> vmlist = cp.createVM(maxCount);
		if(vmlist==null || vmlist.isEmpty()){
			str.append("Operation failed!");
			return str.toString();
		}
		for(Vm vm : vmlist){
			vm.setCloudName(cloudName);
			vm.setHostname(hostName);
			this.vmList.put(vm.getId(), vm);
			Integer uId = VmManager.getcurrId();
			vm.setuId(uId);
			VmManager.getVmList().put(uId, vm);
			str.append(cloudName+"   "+vm.getId()+"   "+vm.getDNSName()+System.getProperty("line.separator"));
		}
		if(maxCount==1){
			str.append(maxCount + " virture machine has been created successfully"+System.getProperty("line.separator"));
		}else{
			str.append(maxCount +" virture machines have been created successfully"+System.getProperty("line.separator"));
		}
		//System.out.println(str);
		Vcluster.writeSysLogFile(str.toString());
		return str.toString();
	}

	public String createVMonHost(int maxCount, String hostID){
		StringBuffer str = new StringBuffer();
		cp.RegisterCloud(conf);
		ArrayList<Vm> vmlist = cp.createVM(maxCount,hostID);
		if(vmlist==null || vmlist.isEmpty()){
			str.append("Operation failed!");
			return str.toString();
		}
		for(Vm vm : vmlist){
			vm.setCloudName(cloudName);
			//vm.setHostname(hostName);
			this.vmList.put(vm.getId(), vm);
			Integer uId = VmManager.getcurrId();
			vm.setuId(uId);
			VmManager.getVmList().put(uId, vm);
			str.append(cloudName+"   "+vm.getId()+"   "+vm.getDNSName()+System.getProperty("line.separator"));
		}
		if(maxCount==1){
			str.append(maxCount + " virture machine has been created successfully"+System.getProperty("line.separator"));
		}else{
			str.append(maxCount +" virture machines have been created successfully"+System.getProperty("line.separator"));
		}
		//System.out.println(str);
		return str.toString();
	}
	/**
	 *List up the virtual machine that is running on the cloud.
	 * 
	 */
	public boolean listVMs() {
		// TODO Auto-generated method stub
		cp.RegisterCloud(conf);
	//	HashMap<String,VMelement> vms = new HashMap<String,VMelement>();
		ArrayList<Vm> cVmList = cp.listVMs();
		//System.out.println(cVmList.size());
		vmList = new TreeMap<String,Vm>();
		//int i = 1;
		if(cVmList==null||cVmList.size()==0)return false;
		for(Vm vm : cVmList){
			if(this.cloudType==CloudType.PUBLIC){
				vm.setHostname("host1");
			}
			vm.setCloudName(getCloudName());
			vmList.put(vm.getId(), vm);			
		}
		/*
		 * Why need to mapping the activity at the time load the cloud?
		 */
		//BatchExecutor.mapingActivityToVm();
		return true;
	}
		
	/**
	 * Terminate the given virtual machine on the cloud.
	 * @param id, the given virtual machine's id.
	 */
	public boolean destroyVM(Vm vm) {
		// TODO Auto-generated method stub
		cp.RegisterCloud(conf);
		cp.destroyVM(vm);
		
		 return true;
	}

	/**
	 *Resume a given virtual machine form suspend.
	 *@param id, the given virtual machine's id.
	 *@return boolean. 
	 */
	public boolean startVM(String id) {
		// TODO Auto-generated method stub
		cp.RegisterCloud(conf);
		ArrayList<Vm> vmlist = cp.startVM(id);
		
		if(vmlist==null || vmlist.isEmpty()){
			System.out.println("Operation failed!");
			return false;
		}
		for(Vm vm : vmlist){
			vmList.get(vm.getId()).setState(vm.getState());
			System.out.println(cloudName+"   "+vm.getId()+"   "+vm.getState());
		}
		return true;
	}
	
	/**
	 * Suspend a given virtual machine from running.
	 * @param id, the virtual machine's id.
	 * 
	 */
	
	public boolean suspendVM(String id) {
		// TODO Auto-generated method stub
		cp.RegisterCloud(conf);
		ArrayList<Vm> vmlist = cp.suspendVM(id);
		if(vmlist==null || vmlist.isEmpty()){
			System.out.println("Operation failed!");
			return false;
		}
		for(Vm vm : vmlist){
			vmList.get(vm.getId()).setState(vm.getState());
			System.out.println(cloudName+"   "+vm.getId()+"   "+vm.getState());
		}
		 return true;
	}
	
	/**
	 *Get the type of the cloud :private or public.
	 *@return cloudType.
	 */
	public CloudType getCloudType() {
		
		return cloudType;
	}
	
	/**
	 * Set the type of a cloud
	 * @see class CloudType.
	 * @param cloudType,private or public.
	 */
	public void setCloudType(CloudType type) {
		cloudType = type;
	}
	
	public void setCloudType(String type) {
		if (type.equalsIgnoreCase("private")) 
			cloudType = CloudType.PRIVATE;
		else if (type.equalsIgnoreCase("public")) 
			cloudType = CloudType.PUBLIC;
		else {
			System.out.println("undefined type, "+type+", found");
			cloudType = CloudType.NOT_DEFINED;
		}
		
	}
	
	public String stringCloudType() {
		
		switch(cloudType) {
		case PRIVATE: return "PRIVATE";
		case PUBLIC: return "PUBLIC";
		case NOT_DEFINED: return "NOT_DEFINED";
		}
		return "NOT_DEFINED";
	}

	/**
	 *Get the cloud name .
	 *@return cloudName. 
	 */
	public String getCloudName() {
		return cloudName;
	}

	/**
	 *Set the cloud name.
	 *@param cloudName , the name of the cloud. 
	 */
	public void setCloudName(String cloudName) {
		this.cloudName = cloudName;
	}

	
	/**
	 * Get the name of cloud plugin
	 * @return cloudpluginName, a string of the cloud plugin name.
	 */
	public String getCloudpluginName() {
		return cloudpluginName;
	}

	/**
	 * Set the name of cloud plugin
	 * @param cloudpluginName, The name of cloud plugin
	 */
	public void setCloudpluginName(String cloudpluginName) {
		this.cloudpluginName = cloudpluginName;
	}



	/**
	 * Get the virtual machines list
	 * @return the instances collection of virtual machines
	 *
	 */
	public TreeMap<String,Vm> getVmList() {
		return vmList;
	}

	/**
	 * Set the virtual machines list
	 * @param vmList, a TreeMap collection of virtual machine's instances,the key set is virtual machines' id 
	 */
	public void setVmList(TreeMap<String, Vm> vmList) {
		this.vmList = vmList;
	}



	/**
	 * Judge the cloud is loaded or not.
	 * @return isLoaded, the load status of the cloud.
	 */
	public boolean isLoaded() {
		return isLoaded;
	}


	/**
	 * Set the load status of the cloud
	 * @param isLoaded, boolean type.
	 */
	public void setIsLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	public boolean listHost(){
		hostList=new TreeMap<String,Host>();
		if (this.cloudType==Cloud.CloudType.PRIVATE)
		{
			cp.RegisterCloud(conf);
			ArrayList<Host> hlist=cp.listHost();
			if(hlist!=null){
				for (Host h:hlist)
					hostList.put(h.getId(),h);
			}
			return true;
		}
		return false;
	}
	/**
	 * Get the host list of the cloud
	 * @return a TreeMap of host instance list, as key set is the host's id.
	 */
	public TreeMap<String, Host> getHostList() {
		
		return hostList;
	}
	
	/**
	 * Set the host list
	 * @param hostList. a TreeMap of host instances' list
	 */
	public void setHostList(TreeMap<String, Host> hostList) {
		this.hostList = hostList;
	}
	
	/**
	 * Set the usage priority of the cloud, the smaller number is higher priority.
	 * @param int i, is the value of the priority.
	 */
	public void setPriority(int i) {
		// TODO Auto-generated method stub
		this.priority = i;
	}

	/**
	 * Get the priority of the cloud
	 * @return a number of priotiry.
	 * 
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Migrate a given virtual machine to a specific host.
	 * @param vmId , hostid, The virtual machine's id and the target host id.
	 */
	public String migrate(String vmID,String hostid) {
		// TODO Auto-generated method stub
	/*	if(!vmList.keySet().contains(vmID)||!hostList.keySet().contains(hostid)){
			System.out.println("This virtual machine or host does not exist!");
			return false;
		}*/
		String str="";
		cp.RegisterCloud(conf);
		if(cp.migrate(vmID,hostid))
		str = "virtual machine "+vmID+" is being migrated to the host "+hostid+"... ...";
		else str = "Migration failed";
		return str;
	}
	
	/**
	 * Physically turn off the given host.
	 * @param hostID, The given host's id.
	 */
	public boolean hostoff(String hostID) {
		// TODO Auto-generated method stub
		Host host = hostList.get(hostID);
		if(host.getPowerStat()==0){
			System.out.println("Host current status is power off!!");
			return false;
		}
		if(!host.getVmList().isEmpty()){
			System.out.println("Host still has vms is running,cannot be shutted down!");
			return false;
		}
		cp.RegisterCloud(conf);
		cp.hostoff(host.getIpmiID());
		host.setPowerStat(0);		
		return true;
	}
	
	/**
	 * Physically turn on the given host.
	 * @param the given host's id.
	 */
	public boolean hoston(String hostID) {
		// TODO Auto-generated method stub
		Host host = hostList.get(hostID);
		if(host.getPowerStat()==1){
			System.out.println("Host current status is power on!!");
			return false;
		}
		
		host.setPowerStat(3);		
		return true;
	}
	
	public int getImageSize()
	{return imageSize;}
	
	public void setImageSize(int size)
	{imageSize=size;}
	
	public String getCluster(){
		return cluster;
	}
	
	public void setCluster(String cName){
		cluster=cName;
	}
	
	/**
	 *  Need to override this function, all the VMs should match using dnsnames
	 */
	public String slotNameToVMId(String slotName){
		String vmId="";
		if(cloudName.equals("Gcloud")){
			if(slotName.contains(".")){
				vmId=slotName.replace(".kisti", "").split("-")[1].replace("vm", "").trim();
			}else{
				vmId=slotName.split("-")[1].replace("vm", "");
			}
			
		}else if(cloudName.equalsIgnoreCase("fermicloud")){
			
		}else if(cloudName.equalsIgnoreCase("amazon")){
			
			String ip = slotName.replace(".amaz", "").replaceAll("-", ".");
			System.out.println(ip);
			for(Vm vm : vmList.values()){
				if(vm.getPrivateIP().equalsIgnoreCase(ip)){
					vmId = vm.getId();
				}
			}
		}
		return vmId;
	}
	class HostControlRunner implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}

	public enum CloudType {PRIVATE, PUBLIC, NOT_DEFINED};


	private String cloudName;
	private String cloudpluginName;
	private List<String> conf;
	private int currentVMs;
	private CloudType cloudType;
	private CloudInterface cp;
	private TreeMap<String, Vm> vmList;
	private boolean isLoaded;
	private TreeMap<String,Host> hostList;
	private int priority;

	/*
	 * Added functions by Hao
	 */
	/*
	 * The size of image, this attribute is temp here. Maybe future in VM class
	 */
	private int imageSize;
	/*
	 * cluster name of the cloud host machine
	 */
	private String cluster;
	
	/**
	 * To match short hostname and full dns hostnames
	 */
	public static boolean hostNameMatch(String n1, String n2){
	    	String [] sn1=n1.split("\\.");
	    	String [] sn2=n2.split("\\.");
	    	return (n1.equals(n2)||sn1[0].equals(sn2[0]));
	}
	
	/**
	 * Add virtual machines to its host
	 */
	public boolean matchVMtoHost(){
		if(vmList.isEmpty()||hostList.isEmpty())
			return false;
		for (Vm v:vmList.values())
		{
			String str=null;
			if((str=v.getHostname())!=null)
			{
				for(Host h:hostList.values())
				{
					if(hostNameMatch(str,h.getName())){
						h.getVmList().put(v.getId(), v);
						if (v.getState()==VMState.RUNNING)
							h.setCurrVmNum(h.getCurrVmNum()+1);
						break;
					}
				}
			}
		}
		return true;
	}
}
