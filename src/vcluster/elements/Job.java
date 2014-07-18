package vcluster.elements;

public class Job {

	private String id;
	private String owner;
	private String submitted;
	private String runt;
	private String st;
	private String pri;
	private String size;
	private String host;
	
	public Job()
	{
		id="";
		owner="";
		submitted="";
		runt="";
		st="";
		pri="";
		size="";
		host="";
	}
	
	public String getID()
	{return id;}
	public void setID(String i)
	{id=i;}
	
	public String getOwner()
	{return owner;}
	public void setOwner(String o)
	{owner=o;}
	
	public String getSubmit()
	{return submitted;}
	public void setSubmit(String s)
	{submitted=s;}
	
	public String getRunT()
	{return runt;}
	public void setRunT(String rt)
	{runt=rt;}
	
	public String getStat()
	{return st;}
	public void setStat(String s)
	{st=s;}
	
	public String getPri()
	{return pri;}
	public void setPri(String p)
	{pri=p;}
	
	public String getSize()
	{return size;}
	public void setSize(String s)
	{size=s;}
	
	public String getHost(){
		return host;
	}
	
	public void setHost(String hostName)
	{host=hostName;}
}
