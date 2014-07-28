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


public class Host {
	private String id; 
	private String addr;
	private int port;
	private int memorySize;
	private int cpuCount;
	private ArrayList<VM> vmList;
	public static final String configFile="host.conf";
	public static final int MAX_TIME_FRAME = 3600;
	
	/*
	 * sysCpuU and sysIOU map system time (long format) and its corresponding utilization
	 */
    private ArrayList<SystemStat>  sysStat;
        
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
	
    public ArrayList<VM> getVmList(){
    	return vmList;
    }
    public void setVmList(ArrayList<VM> value){
    	vmList=value;
    }
    
    public ArrayList<SystemStat> getSysStat(){
    	return sysStat;
    }
    public void setSysStat(ArrayList<SystemStat> value){
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
     */
    public Host(String cloudName, String _id, String _addr, int _port, int memSize, int cpuCount){
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
    	sysStat=new ArrayList<SystemStat>();
    	vmList=new ArrayList<VM>();
    	if(!readConfigFile()){
    		benchmarking();
    		defaultParameters();
    	}
    	if(!checkParameters())
    	{
    		System.out.println("Reading Host Config failure: Missing parameters!");
    		System.exit(1);
    	}
    }
    public boolean readConfigFile(){
    	String file="hosts"+File.separator+id;
    	File f=new File(file);
    	if(!f.exists()){
    		f=new File(Host.configFile);
    		file=configFile;
    	}
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
				}
			}
			
			br.close();
			
		}catch(Exception e){
			System.out.println(e.getMessage());	
			return false;		
		}
    	return true;
    }
    public boolean benchmarking(){
    	String s="Bench Marking Host: "+ this.addr;
    	System.out.println(s);
    	writeBandwidth=benchMarkWriteBandwidth();
    	String out="Writing Bandwidth : " + Integer.toString(writeBandwidth)+"MB/s";
    	System.out.println(out);
    	clearCache();
    	readBandwidth=benchMarkReadBandwidth();
    	out="Reading Bandwidth : " + Integer.toString(readBandwidth)+"MB/s";
    	System.out.println(out);
    	cacheBandwidth=benchMarkReadBandwidth();
    	out="Caching Bandwidth : " + Integer.toString(cacheBandwidth)+"MB/s";
    	System.out.println(out);
    	cleanBenchMarkFiles();
    	if(writeBandwidth!=0&&readBandwidth!=0&&cacheBandwidth!=0)
    	{
    		System.out.println("Bench Marking successful!");
    		return true;
    	}else{
    		System.out.println("Bench Marking unsuccessful!!");
    		return false;
    	}
    }
    
    //TODO: Training function to train the parameters for the overhead model
    public void trainning(){
    	/*
    	 * updating epsilon, gamma, beta and bootmin
    	 */
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
    	String cmd="(dd bs=1M count=2000 if=/dev/zero of=./test conv=fdatasync)2>&1";
    	ArrayList<String> strArr=socketToproxy(cmd);
    	for(String s :strArr){
    		if (s.contains("copied"))
    			return bandwidthExtract(s);
    	}
		return 0;
    	
    }
    
    private int benchMarkReadBandwidth(){
    	String cmd="(dd bs=1M count=2000 if=./test of=/dev/null)2>&1";
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
	
	public void deleteVM(String vmid){
		for(int i=0;i<vmList.size();i++){
			if(vmList.get(i).getId().equals(vmid)){
				vmList.remove(i);
				System.out.printf("VM %s has been deleted from the host list!\n", vmid);
				break;
			}
		}
		System.out.printf("[ERROR : ]CANNOT find VM %s in the host list!\n ",vmid);
	}
	
	private void defaultParameters(){
		this.epsilon = 0.004;
		this.gamma = 0.03;
		this.beta = 0.7;
		this.bootMin = 0.02;
		this.a = 0.1;
		this.b = 64;
		this.c = 0.6;
	}
}
