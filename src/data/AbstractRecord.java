package data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import utils.UtilityBelt;

@XmlSeeAlso({Record.class})
public abstract class AbstractRecord{
	
	@XmlElement(name="name")
	protected String name;
	@XmlElement(name="ip_addr")
	protected String ip_addr;
	@XmlElement(name="port_num")
	protected int port_num;
	@XmlAttribute(name="timestamp")
	protected String timestamp;
	
	
	public String get_name(){ return name;}
	public String get_ip(){ return ip_addr;}
	public int get_port(){ return port_num;}
	public String get_timestamp(){ return timestamp; }
	
	
	
	private static final String IP_PATTERN = 
	        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	
	   private static final String WILD_IP_PATTERN = 
	            "^([01*]?[\\d*]{1,2}?|2[0-4*][\\d*]| 25[0-5*])\\." +
	            "([01*]?[\\d*]{1,2}?|2[0-4*][\\d*]| 25[0-5*])\\." +
	            "([01*]?[\\d*]{1,2}?|2[0-4*][\\d*]| 25[0-5*])\\." +
	            "([01*]?[\\d*]{1,2}?|2[0-4*][\\d*]| 25[0-5*])$";
	
	/**
	 * Validates an ip address
	 * @param ip the address to be validated
	 * @return true if valid, false otherwise
	 */
	public static final boolean valid_ip(final String ip){
		Pattern pattern = Pattern.compile(IP_PATTERN);
	    Matcher matcher = pattern.matcher(ip);
	    return matcher.matches(); 
	}
	
	public static final boolean valid_wild_ip(final String wild_ip){
	    Pattern pattern = Pattern.compile(WILD_IP_PATTERN);
        Matcher matcher = pattern.matcher(wild_ip);
        return matcher.matches(); 
	}
	
	/**
	 * Validates the port number received. valid port numbers are greater than 1023 and less than 65536
	 * @param port_num the port number we are validating
	 * @return true if valid, false otherwise
	 */
	public static final boolean valid_port(final int port_num){
		return port_num > 1023 && port_num < 65536;
	}
	
	/**
	 * Validates the name of the record. A valid record is between 1 and 80 chracters long
	 * @param name the name we want to validate
	 * @return true if valid, false otherwise
	 */
	public static final boolean valid_name(final String name){
		return name.length() > 0 && name.length() < 81;
	}
	
	
	protected static final boolean validate(final String name, final String ip, final int port_num){
		return valid_ip(ip) &&
			   valid_name(name) &&
			   valid_port(port_num);
	}
	
	public boolean equals(Object record){
		return (record.getClass() == this.getClass())? 
					this.name.equals(this.getClass().cast(record).get_name()) 
						&& this.ip_addr.equals(this.getClass().cast(record).get_ip()):
					false;
	}
	
	protected AbstractRecord(String name, String ip, int port_num){
		this.name = name;
		this.ip_addr = ip;
		this.port_num = port_num;
		this.timestamp = UtilityBelt.get_timestamp();
	}
	
	protected AbstractRecord(String name, String ip, int port_num, String timestamp){
		this.name = name;
		this.ip_addr = ip;
		this.port_num = port_num;
		this.timestamp = timestamp;
	}
	
	protected AbstractRecord(){}
	

}
