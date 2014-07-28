package vcluster.plugin.balancer.fermibalancer;

import java.util.ArrayList;

/*
 * In the model class, the start time is always 0, 
 * for each VM, the time line needs to be converted to its' own time line
 */
public class VmLaunchOverheadModel {

	/*
	 * All the parameters to calculate the VM launching overhead
	 */
	private int readBandwidth;
	private int writeBandwidth;
	private int cacheBandwidth;
	private int readSpeed;
	private int writeSpeed;
	private int cacheSpeed;
	private int imageSize;
	private int memorySize;
	private boolean cached;
	
	private double epsilon;
	private double gamma;
	private double beta;
	private double bootMin;
	private double a;
	private double c;
	private double b;
	
	private int MAX_TIME_FRAME;
	private ArrayList<Double> CpuUtil;
	private ArrayList<Double> IOUtil;
	
	private int startTime;
	
	/**
	 * Default constructor. 
	 * Assume Fermi machine specifications: 100m/s read, 400m/s write, 1.2G/s cache, 16G memory
	 */
	public VmLaunchOverheadModel(){
		readBandwidth=100;
		writeBandwidth=400;
		cacheBandwidth=1200;
		memorySize=16000;
		epsilon=0.004;
		gamma=0.03;
		beta=0.7;
		bootMin=0.02;
		a=0.1;
		c=0.6;
		b=64;
		cached=false;
		initialize();
	}
	
	/**
	 * Constructor with customized parameters
	 * @return
	 */
	public VmLaunchOverheadModel(int readB, int writeB, int cacheB, int mem_size, int imageSize,
			double epsilon, double gamma, double beta, double bootMin, double a, double b, double c, boolean cache){
		this.readBandwidth=readB;
		this.writeBandwidth=writeB;
		this.cacheBandwidth=cacheB;
		this.memorySize=mem_size;
		this.imageSize=imageSize;
		this.epsilon=epsilon;
		this.gamma=gamma;
		this.beta=beta;
		this.bootMin=bootMin;
		this.a=a;
		this.b=b;
		this.c=c;
		this.cached=cache;
		initialize();
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
	
	public int getReadSpeed(){
		return readSpeed;
	}
	public void setReadSpeed(int value){
		readSpeed=value;
	}
	
	public int getWriteSpeed(){
		return writeSpeed;
	}
	public void setWriteSpeed(int value){
		writeSpeed=value;
	}
	
	public int getCacheSpeed(){
		return cacheSpeed;
	}
	public void setCacheSpeed(int value){
		cacheSpeed=value;
	}
	
	public boolean ifCached(){
		return cached;
	}
	public void setCached(boolean value){
		cached=value;
	}
	
	public double getEpsilon(){
		return epsilon;
	}
	public void setEpsilon(double value){
		epsilon=value;
	}
	
	public double getGamma(){
		return gamma;
	}
	public void setGamma(double value){
		gamma=value;
	}
	
	public double getBeta(){
		return beta;
	}
	public void setBeta(double value){
		beta=value;
	}
	
	public double getBootMin(){
		return bootMin;
	}
	public void setBootMin(double value){
		bootMin=value;
	}
	
	public int getMaxTimeFrame(){
		return MAX_TIME_FRAME;
	}
	public void setMaxTimeFrame(int value){
		MAX_TIME_FRAME=value;
	}
	
	public ArrayList<Double> getCpuU(){
		return CpuUtil;
	}
	public void setCpuU(ArrayList<Double> value){
		CpuUtil=value;
	}
	
	public ArrayList<Double> getIOU(){
		return IOUtil;
	}
	public void setIOU(ArrayList<Double> value){
		IOUtil=value;
	}
	public int getImageSize() {
		return imageSize;
	}
	public void setImageSize(int imageSize) {
		this.imageSize = imageSize;
	}
	public int getMemorySize() {
		return memorySize;
	}
	public void setMemorySize(int memorySize) {
		this.memorySize = memorySize;
	}
	public int getStartTime(){
		return startTime;
	}
	public void setStartTime(int value){
		startTime=value;
	}
	public double getA() {
		return a;
	}
	public void setA(double a) {
		this.a = a;
	}
	public double getC() {
		return c;
	}
	public void setC(double c) {
		this.c = c;
	}
	public double getB() {
		return b;
	}
	public void setB(double b) {
		this.b = b;
	}
	
	private ArrayList<TimeSet> writeTimeSet;
    private int TStartWrite;
    private int TFinishWrite;
    private int t_trans;

    private int tStartWrite()
    {
        if (cached)
            return Math.round(startTime+Math.min(30,Math.min(imageSize,memorySize/10)/cacheSpeed));
        else
            return Math.round(startTime+Math.min(30,Math.min(imageSize,memorySize/10)/readSpeed));

    }
    private int tFinishWrite()
    {
       return findWriteTimeSet();
    }

    public int transTime()
    {
        if (cached)
        {
            double t1 = Math.min(imageSize / cacheSpeed, memorySize / 10 / cacheSpeed);
            if (imageSize <= (memorySize / 10))
                return (int)t1;
            else
            {
                double remain = imageSize - memorySize / 10;
                double t2 = remain / (cacheSpeed - writeSpeed);
                return (int)(t1 + t2);
            }
        }
        else
            return (int)Math.round(imageSize / readSpeed);
    }

    public double util_trans(long t)
    {
        double ftime=t_trans+startTime;
       // double temp=1/(1+Math.pow(Math.E,(-0.5*ftime*(t-startTime))))-1/(1+Math.pow(Math.E,(-0.5*ftime*(t-ftime))));
        if (t >= startTime && t <= ftime)
        {
            if (cached)
                return cacheSpeed / cacheBandwidth;
            else
                return readSpeed / (readBandwidth / beta);
        }
        else
            return 0;
    }

    public int findWriteTimeSet()
    {
        writeTimeSet = new ArrayList<TimeSet>();
        double t1;
        if(!cached)
         t1 = Math.min(Math.min(30,Math.min(imageSize,memorySize/10)/readSpeed)*readSpeed/Math.max(1, (writeSpeed-readSpeed)),imageSize/writeSpeed);
        else
            t1 = Math.ceil(imageSize / writeSpeed);
        TimeSet s1 = new TimeSet();
        s1.start = tStartWrite();
        s1.finish =  s1.start + (int)t1;
        writeTimeSet.add(s1);
        if (cached)
            return s1.finish;
        else {
            double remain = imageSize - t1 * writeSpeed;
            double wt_sleep = 5 * readSpeed /Math.max(1, (writeSpeed - readSpeed));
            double N = Math.ceil(remain/writeSpeed/wt_sleep);
            double lasttime = s1.finish;
            for(int i=1;i<=N;i++)
            {
                TimeSet stmp = new TimeSet();
                if (i != N)
                {
                    stmp.start = (int) (lasttime + 5);
                    stmp.finish = (int) (stmp.start + wt_sleep);
                    writeTimeSet.add(stmp);
                    lasttime = stmp.finish;
                }
                else
                {
                    stmp.start = (int)(lasttime + 5);
                    stmp.finish = (int) (stmp.start + (remain/writeSpeed)%wt_sleep);
                    writeTimeSet.add(stmp);
                    lasttime = stmp.finish;
                }
            }
            return (int)lasttime;
        }
     }
    public boolean inTimeSet(long t)
    {
        for (int i = 0; i < writeTimeSet.size(); i++)
        {
            if (t <= writeTimeSet.get(i).finish && t >= writeTimeSet.get(i).start)
                return true;
        }
        return false;

    }
    public double IO(int t)
    {
        if (inTimeSet(t))
            return writeSpeed / writeBandwidth* (1 / (1 + Math.pow(Math.E, (-0.5 * TFinishWrite * (t - TStartWrite)))) - 1 / (1 + Math.pow(Math.E, (-0.5 * TFinishWrite * (t - TFinishWrite)))));
        else
            return 0;
    }

    public double U_IO(int t, int n_cores)
    {
        return IO(t) / n_cores;
    }

    public double U_Boot(long t, double io)
    {
        
        //return Math.Max(c*Math.Pow(Math.E,-Data.gamma*(1-io)*(t-t_trans)),0.001);
        //double tmp = 1 + Data.boot_min - io;
        double t_mid = Math.ceil(c / a)+startTime+t_trans;
        if(t>=t_mid)
        {
            //double tmp = bootMin * (1 / (gamma + io));
            double time = (t - t_mid);
            return c * Math.pow(Math.E, -time * gamma * (bootMin*(1+io + b / (time + b))));
        }
        else
            return (t - startTime - t_trans) * a;
    }

    public void initialize()
    {
    	readSpeed=readBandwidth;
    	writeSpeed=writeBandwidth;
    	cacheSpeed=cacheBandwidth;
    	//cached=false;
    	startTime=0;
    	CpuUtil=new ArrayList<Double>();
    	IOUtil=new ArrayList<Double> ();
    	writeTimeSet=new ArrayList<TimeSet> ();
        t_trans = transTime();
        TStartWrite = tStartWrite();
        TFinishWrite = tFinishWrite();
    }
	
    public int bootTime(){
    	int time=t_trans;
    	try{
    		if(CpuUtil.size()<=t_trans)
    			throw new Exception("CPU utilization is not estimated!");
    		for(int i=t_trans;i<CpuUtil.size();i++)
    		{
    			double diff=CpuUtil.get(i)-CpuUtil.get(i+1);
    			if(diff>=0&&diff<epsilon)
    			{
    				time=i;
    				break;
    			}
    		}
    		
    	}
    	catch (Exception e){
    		System.out.println(e.getMessage());		
    	}
    	return time;
    }

	
}
