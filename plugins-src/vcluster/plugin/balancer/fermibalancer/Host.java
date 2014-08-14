package vcluster.plugin.balancer.fermibalancer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import vcluster.plugin.balancer.fermibalancer.VM.VmStat;


public class Host {
	private String id; 
	private String addr;
	private int port;
	private int memorySize;
	private int cpuCount;
	private CopyOnWriteArrayList<VM> vmList;
	private static final String localImageLocation="/var/lib/one/local/test";
	public static final String configFile="host.conf";
	public static final int MAX_TIME_FRAME = 3600;
	
	/*
	 * sysCpuU and sysIOU map system time (long format) and its corresponding utilization
	 */
    private CopyOnWriteArrayList<SystemStat>  sysStat;
        
    /*
     * System properties for the overhead model
     */
    private String cloudName;
    private int readBandwidth;
    private int writeBandwidth;
    private int cacheBandwidth;
    private double epsilon;
    private double gamma;
    private double beta;
    private double bootMin;
    private double a;
    private double b;
    private double c;
    
    private int trainRound;
    private boolean trained;
    
    public String getCloudName(){
    	return cloudName;
    }
    public void setCloudName(String value){
    	cloudName=value;
    }
    public String getId(){
    	return id;
    }
    public void setId(String value){
    	id=value;
    }
    
    public String getAddress(){
    	return addr;
    }
    public void setAddress(String value){
    	addr=value;
    }
    
    public int getPort(){
    	return port;
    }
    public void setPort(int value){
    	port=value;
    }
   
	public int getMemorySize() {
		return memorySize;
	}
	public void setMemorySize(int memorySize) {
		this.memorySize = memorySize;
	}
	
	public int getCpuCount(){
		return cpuCount;
	}
	public void setCpuCount(int value){
		cpuCount=value;
	}
	
    public CopyOnWriteArrayList<VM> getVmList(){
    	return vmList;
    }
    public void setVmList(CopyOnWriteArrayList<VM> value){
    	vmList=value;
    }
    
    public CopyOnWriteArrayList<SystemStat> getSysStat(){
    	return sysStat;
    }
    public void setSysStat(CopyOnWriteArrayList<SystemStat> value){
    	sysStat=value;
    }
    
    
    public int getReadBandwidth(){
    	return readBandwidth;
    }
    public void setReadBandwidth(int value){
    	readBandwidth=value;
    }
    
    public int getWriteBandwidth(){
    	return writeBandwidth;
    }
    public void setWriteBandwidth(int value){
    	writeBandwidth=value;
    }
    
    public int getCacheBandwidth(){
    	return cacheBandwidth;
    }
    public void setCacheBandwidth(int value){
        cacheBandwidth=value;
    }
      
	public double getEpsilon() {
		return epsilon;
	}
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}
	public double getGamma() {
		return gamma;
	}
	public void setGamma(double gamma) {
		this.gamma = gamma;
	}
	public double getBeta() {
		return beta;
	}
	public void setBeta(double beta) {
		this.beta = beta;
	}
	public double getBootMin() {
		return bootMin;
	}
	public void setBootMin(double bootMin) {
		this.bootMin = bootMin;
	}
	public double getA() {
		return a;
	}
	public void setA(double a) {
		this.a = a;
	}
	public double getB() {
		return b;
	}
	public void setB(double b) {
		this.b = b;
	}
	public double getC() {
		return c;
	}
	public void setC(double c) {
		this.c = c;
	}
	/**
     * Constructor, need id, server address and port number. Initialize the utilization sets(CPU and IO)
   	 * @param cloudName 
	 * @param _id 
	 * @param _addr 
	 * @param _port
	 * @param memSize
	 * @param cpuCount
	 * 
	 */
    public Host(String cloudName, String _id, String _addr, int _port, int memSize, int cpuCount) {
    	this.cloudName=cloudName;id=_id; addr=_addr; port=_port;memorySize=memSize;this.cpuCount=cpuCount;
    	readBandwidth=0;
    	writeBandwidth=0;
    	cacheBandwidth=0;
    	epsilon=0;
    	gamma=0;
    	beta=0;
    	bootMin=0;
    	a=0;
    	b=0;
    	c=0;
    	trainRound=0;
    	sysStat=new CopyOnWriteArrayList<SystemStat>();
    	vmList=new CopyOnWriteArrayList<VM>();
    	if(!readConfigFile()){
    		if(!benchmarking())
    		{
    			String msg="[MSG: ]Trying to bench mark onemore time!";
    			System.out.println(msg);
    			FermiBalancer.writeLogFile(msg);
    			benchmarking();
    		}
    		if(!readDefualtConfigFile())
    			defaultParameters();
    	}
    	if(!checkParameters())
    	{
    		
    		String line="[Error:] Reading Host Config failure: Missing parameters!\n";
    		System.out.println(line);
    		FermiBalancer.writeLogFile(line);
   			System.exit(1);
    	}
    	if(trainRound<FermiBalancer.MAX_CONVERGE_ROUND)
    		trained=false;
    	else
    		trained=true;
    	writeConfigFile();
    }
    public boolean readConfigFile(){
    	String file="hosts"+File.separator+id;
    	File f=new File(file);
    	
     	if(!f.exists())
    		return false;
    	
    	BufferedReader br ;
		try{
			br = new BufferedReader(new FileReader(file));
			String aLine = "";
			while ((aLine = br.readLine()) != null) {
				if(aLine.contains("#")||!aLine.contains("="))continue;
				String[] pair = aLine.split("=");
				if (pair[0].trim().equalsIgnoreCase("ReadBandwidth")) {
					readBandwidth = Integer.parseInt(pair[1].trim());				
				}else if(pair[0].trim().equalsIgnoreCase("WriteBandwidth")){
					writeBandwidth = Integer.parseInt(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("CacheBandwidth")){
					cacheBandwidth = Integer.parseInt(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("Epsilon")){
					epsilon = Double.parseDouble(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("Gamma")){
					gamma = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("Beta")){
					beta = Double.parseDouble(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("BootMin")){
					bootMin = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("a")){
					a = Double.parseDouble(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("b")){
					b = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("c")){
					c = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("TRAIN")){
					trainRound=Integer.parseInt(pair[1].trim());
				}
			}
			
			br.close();
			
		}catch(Exception e){
			System.out.println(e.getMessage());	
			FermiBalancer.writeLogFile(e.getMessage());
			return false;		
		}
    	return true;
    }
    
    public boolean readDefualtConfigFile(){
    	String file=Host.configFile;
    	File f=new File(file);
    	if(!f.exists())
    		return false;
    	
    	BufferedReader br ;
		try{
			br = new BufferedReader(new FileReader(file));
			String aLine = "";
			while ((aLine = br.readLine()) != null) {
				if(aLine.contains("#")||!aLine.contains("="))continue;
				String[] pair = aLine.split("=");
				if (pair[0].trim().equalsIgnoreCase("ReadBandwidth")) {
					if(readBandwidth==0)
						readBandwidth = Integer.parseInt(pair[1].trim());				
				}else if(pair[0].trim().equalsIgnoreCase("WriteBandwidth")){
					if(writeBandwidth==0)
						writeBandwidth = Integer.parseInt(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("CacheBandwidth")){
					if(cacheBandwidth==0)
						cacheBandwidth = Integer.parseInt(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("Epsilon")){
					epsilon = Double.parseDouble(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("Gamma")){
					gamma = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("Beta")){
					beta = Double.parseDouble(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("BootMin")){
					bootMin = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("a")){
					a = Double.parseDouble(pair[1].trim());
				}else if(pair[0].trim().equalsIgnoreCase("b")){
					b = Double.parseDouble(pair[1].trim());
				} else if(pair[0].trim().equalsIgnoreCase("c")){
					c = Double.parseDouble(pair[1].trim());
				}
			}
			
			br.close();
			
		}catch(Exception e){
			System.out.println(e.getMessage());	
			FermiBalancer.writeLogFile(e.getMessage());
			return false;		
		}
    	return true;
    }
    public boolean benchmarking(){
    	String s="Bench Marking Host: "+ this.addr;
    	System.out.println(s);
    	FermiBalancer.writeLogFile(s);
    	writeBandwidth=benchMarkWriteBandwidth();
    	String out="Writing Bandwidth : " + Integer.toString(writeBandwidth)+"MB/s";
    	System.out.println(out);
    	FermiBalancer.writeLogFile(out);
    	clearCache();
    	readBandwidth=benchMarkReadBandwidth();
    	out="Reading Bandwidth : " + Integer.toString(readBandwidth)+"MB/s";
    	System.out.println(out);
    	FermiBalancer.writeLogFile(s);
    	cacheBandwidth=benchMarkReadBandwidth();
    	out="Caching Bandwidth : " + Integer.toString(cacheBandwidth)+"MB/s";
    	System.out.println(out);
    	FermiBalancer.writeLogFile(s);
    	cleanBenchMarkFiles();
    	if(writeBandwidth!=0&&readBandwidth!=0&&cacheBandwidth!=0)
    	{
    		String msg="Bench Marking successful!";
    		System.out.println(msg);
    		FermiBalancer.writeLogFile(msg);
    		return true;
    	}else{
    		String msg="Bench Marking unsuccessful!!";
    		System.out.println(msg);
    		FermiBalancer.writeLogFile(msg);
    		return false;
    	}
    }
    
    //TODO: Training function to train the parameters for the overhead model
    public void trainning(boolean debug){
    	/*
    	 * updating epsilon, gamma, beta and bootmin
    	 */
     	if(trainRound>FermiBalancer.MAX_CONVERGE_ROUND){
    		if(!trained){
    			writeConfigFile();
    			trained=true;
    		}
    	}else {
    		ArrayList<Double> actual=new ArrayList<Double>();
    		ArrayList<Double> predict=new ArrayList<Double>();
    		for(int i=0;i<vmList.size();i++){
    			if(vmList.get(i).getBootTime()!=-1&&vmList.get(i).getRealBootTime()!=-1){
    				actual.add((double)vmList.get(i).getRealBootTime());
    				predict.add((double)vmList.get(i).getBootTime());
    			}
    		}
    		double error=Evaluation.MASE(actual, predict);
    		if(error<=FermiBalancer.CONVERGE_THRESHOLD){
    			trainRound++;
    		}else{
    			boolean terminate=false;
    			double granularity=getCurrentGranularity();
    			double tmpEpsilon=granularity;
    			long start=System.currentTimeMillis();
    			long finish=start;
    			while (error>FermiBalancer.CONVERGE_THRESHOLD||!terminate){
    				if(granularity<=FermiBalancer.MIN_GRANULARITY){
    					terminate=true;
    					if(debug){
    						finish=System.currentTimeMillis();
       						System.out.println("[DEBUG:] Model Tranning Not converged but terminated. Using: "+
    						Long.toString(finish-start)+" MS.");
    					}
    					break;
    				}
    				else{
    					double minError=error;
    					double minEpsilon=tmpEpsilon;
    					double oriEpsilon=tmpEpsilon;
    					for(int i=0;i<9;i++){
    						double tmp=calPredictError(tmpEpsilon);
    						if(tmp<=minError)
    						{
    							minError=tmp;
    							minEpsilon=tmpEpsilon;
    						}
    						tmpEpsilon=tmpEpsilon+granularity;
    					}
    					tmpEpsilon=oriEpsilon;
    					for(int i=0;i<9;i++){
    						tmpEpsilon=tmpEpsilon-granularity;
    						if(tmpEpsilon<=0)
    							break;
    						double tmp=calPredictError(tmpEpsilon);
    						if(tmp<=minError)
    						{
    							minError=tmp;
    							minEpsilon=tmpEpsilon;
    						}
     					}
    					tmpEpsilon=minEpsilon;
    					error=minError;
    					granularity=granularity/10;
    				}

    			}
    			finish=System.currentTimeMillis();
    			if(!terminate&&debug){
    				System.out.println("[DEBUG:] Model Tranning converged and terminated. Using: "+
    						Long.toString(finish-start)+" MS.");
    			}
    			if(debug){
    				System.out.println(dumpTrainData());
    				System.out.println(Double.toString(tmpEpsilon));
    			}
    			this.epsilon=tmpEpsilon;
    			trainRound++;
    			writeConfigFile();
    		}
    	}
    }
    
    private String dumpTrainData(){
    	StringBuilder sb=new StringBuilder();
    	String title="--"+this.getAddress()+":Training Data--"+System.lineSeparator();
    	sb.append(title);
    	String test=String.format("%-8s", "EST.");
    	String treal=String.format("%-8s", "REAL");
    	sb.append(test+treal+System.lineSeparator());
    	for(int i=0;i<vmList.size();i++){
    		test=String.format("%-8s",Integer.toString(vmList.get(i).getBootTime()));
        	treal=String.format("%-8s", Integer.toString(vmList.get(i).getRealBootTime()));
        	sb.append(test+treal+System.lineSeparator());
    	}
    	sb.append("-------------------------------------");
    	return sb.toString();
    }
    public double calPredictError(double epsilon){
    	ArrayList<Double> actual=new ArrayList<Double>();
    	ArrayList<Double> predict=new ArrayList<Double>();
    	for(int i=0;i<vmList.size();i++){
    		if(vmList.get(i).getBootTime()!=-1&&vmList.get(i).getRealBootTime()!=-1)
    		{
    			vmList.get(i).getOverhead().setEpsilon(epsilon);
    			actual.add((double)vmList.get(i).getRealBootTime());
    			predict.add((double)vmList.get(i).getOverhead().bootTime());
 			
    		}
    	}
    	return Evaluation.MASE(actual, predict);
    }
    
    public double getCurrentGranularity(){
    	double granularity=0.1;
    	while((epsilon-granularity)<=0)
    	{
    		granularity=granularity/10;
    	}
    	return granularity*10;
    }
    
    private ArrayList<String> socketToproxy(String cmd){
		String cmdLine=cmd+"\n";
		ArrayList<String> feedBack = new ArrayList<String>();
		 Socket socket = null;
	        BufferedReader in = null;
	        DataOutputStream out = null;
	        //System.out.println(cmdLine);
	        try {
	        	socket = new Socket(addr, port);
	        	
	            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
	            //di = new DataInputStream(socket.getInputStream());
	        	out = new DataOutputStream(socket.getOutputStream());
	            out.flush();
	            /* make an integer to unsigned int */
	            int userInput = 5;
	            userInput <<= 8;
	            userInput |=  1;
	            userInput &= 0x7FFFFFFF;

	            String s = Integer.toString(userInput);
	            byte[] b = s.getBytes();
	            
	            out.write(b, 0, b.length);
	            out.write(cmdLine.getBytes(), 0, cmdLine.getBytes().length);
	            // out.writeBytes("Dump info, test!!!");
	           // out.flush();
	            
	            String str=null;
	                            
	            while((str=in.readLine())!=null){
	            	str=str.trim();
	            	feedBack.add(str);
	            }
	            
	        
	        } catch (UnknownHostException e) {
	        	
	    		System.out.print("ERROR: " +e.getMessage());
	            closeStream(in, out, socket);
	            return feedBack;
	        } catch (IOException e) {
	    		System.out.print("ERROR: " +e.getMessage());
	            closeStream(in, out, socket);
	            return feedBack;
	        }
	        closeStream(in, out, socket);
	        
	        return feedBack;
	}
    
    private static void closeStream(BufferedReader in, DataOutputStream out, Socket socket)
	{
		try {
	        if (in != null) in.close();
	        if (out != null) out.close();
	        if (socket != null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    private int benchMarkWriteBandwidth(){
    	String cmd="(dd bs=1M count=2000 if=/dev/zero of="+ localImageLocation +" conv=fdatasync)2>&1";
    	ArrayList<String> strArr=socketToproxy(cmd);
    	for(String s :strArr){
    		if (s.contains("copied"))
    			return bandwidthExtract(s);
    	}
		return 0;
    	
    }
    
    private int benchMarkReadBandwidth(){
    	String cmd="(dd bs=1M count=2000 if="+ localImageLocation +" of=/dev/null)2>&1";
    	ArrayList<String> strArr=socketToproxy(cmd);
    	for(String s :strArr){
    		if (s.contains("copied"))
    			return bandwidthExtract(s);
    	}
		return 0;
    	
    }
    
    private int bandwidthExtract(String s){
    	double bandwidth=0;
    	String [] sub=s.split(" ");
    	String speed=sub[7];
    	if(sub[8].equalsIgnoreCase("MB/s"))
    		bandwidth=Double.parseDouble(speed);
    	else if(sub[8].equalsIgnoreCase("GB/s"))
    		bandwidth=Double.parseDouble(speed)*1000;
    	return (int)bandwidth;
    }
    
    private void clearCache(){
    	String cmd="sync; echo 3 > /proc/sys/vm/drop_caches";
    	socketToproxy(cmd);
     }
    private void cleanBenchMarkFiles(){
    	String cmd="rm -f test";
    	socketToproxy(cmd);
    }

	public void dump()
	{
		String lineSeparator = "----------------------------------------------";
		System.out.println(lineSeparator);
		System.out.println(printConfig());
		System.out.println(lineSeparator);
	}
	
	public String printConfig(){
		StringBuilder sb=new StringBuilder();
		sb.append("Cloud Name = "+cloudName+System.lineSeparator());
		sb.append("Host ID = " + id+System.lineSeparator());
		sb.append("Host Address = "+addr+System.lineSeparator());
		sb.append("Port No. = "+Integer.toString(port)+System.lineSeparator());
		sb.append("Memory Size = "+Integer.toString(memorySize)+System.lineSeparator());
		sb.append("ReadBandwidth = " + Integer.toString(readBandwidth)+System.lineSeparator());
		sb.append("WriteBandwidth = "+Integer.toString(writeBandwidth)+System.lineSeparator());
		sb.append("CacheBandwidth = " + Integer.toString(cacheBandwidth)+System.lineSeparator());
		sb.append("Epsilon = "+ Double.toString(epsilon)+System.lineSeparator());
		sb.append("Gamma = " + Double.toString(gamma)+System.lineSeparator());
		sb.append("Beta = " +Double.toString(beta)+System.lineSeparator());
		sb.append("BootMin = "+ Double.toString(bootMin)+System.lineSeparator());
		sb.append("a = " + Double.toString(a)+System.lineSeparator());
		sb.append("b = "+Double.toString(b)+System.lineSeparator());
		sb.append("c = "+Double.toString(c)+System.lineSeparator());
		sb.append("TRAIN = "+Integer.toString(trainRound)+System.lineSeparator());
		return sb.toString();
	}
	
	public boolean writeConfigFile(){
		String file="hosts"+File.separator+id;
		File f=new File(file);
		if(f.exists())
			return false;
		try {
			FileWriter fw= new FileWriter(f);
			fw.write(printConfig());
			fw.flush();
			fw.close();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean checkParameters(){
		return readBandwidth !=0 && writeBandwidth !=0 && cacheBandwidth !=0 && epsilon !=0&&
				gamma !=0 && beta !=0 && bootMin !=0 && a !=0 && b !=0 && c!=0;
	}
	
	public void addVM(VM vm){
		vmList.add(vm);
	}
	
	/**
	 * Only change the status of the vm to be off, keep the record of the vm
	 * @param vmid
	 */
	public void deleteVM(String vmid){
		for(int i=0;i<vmList.size();i++){
			if(vmList.get(i).getId().equals(vmid)){
				//vmList.remove(i);
				vmList.get(i).setStatus(VmStat.DELETED);
				System.out.printf("VM %s has been deleted from the host list!\n", vmid);
				break;
			}
		}
		System.out.printf("[ERROR : ]CANNOT find VM %s in the host list!\n ",vmid);
	}
	
	public int runningVMs(){
		int count=0;
		for(int i=0;i<vmList.size();i++){
			if(vmList.get(i).getStatus()!=VmStat.DELETED&&vmList.get(i).getStatus()!=VmStat.UNKNOWN)
				count++;
		}
		return count;
	}
	
	private void defaultParameters(){
		this.epsilon = 0.0045;
		this.gamma = 0.03;
		this.beta = 0.7;
		this.bootMin = 0.02;
		this.a = 0.1;
		this.b = 64;
		this.c = 0.6;
	}
}
