package data;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ServerInfo")
public final class ServerInfo{
	@XmlElement(name="port_num")
	public int port_num;
	@XmlElement(name="ip_addr")
	public String ip_addr;
	@XmlElement(name="name")
	public String name;
	public InetAddress inet;
	
	
	public Record to_record() throws InvalidRecordException{
		return Record.create_record(name, ip_addr, port_num);
	}
	
	private ServerInfo(String name, String ip_addr, int port_num) throws UnknownHostException{
		this.port_num = port_num;
		this.ip_addr = ip_addr;
		this.name = name;
		this.inet = InetAddress.getByName(ip_addr);
	}
	
	private ServerInfo(){}
	
	public static ServerInfo get_nfo(String name, String ip_addr, int port_num) throws UnknownHostException{
		return new ServerInfo(name, ip_addr, port_num);
	}
	
	public static ServerInfo get_nfo(String name, int port_num) throws UnknownHostException{
		return get_nfo(name, InetAddress.getLocalHost().getHostAddress(), port_num);
	}
	
	public boolean appx_equals(ServerInfo info){
		if(info.name == null){
			return this.ip_addr == info.ip_addr && this.port_num == info.port_num;
		}else return this.name == info.name;
	}
	
	public boolean equals(Object info){
		try{
			ServerInfo nfo = (ServerInfo) info;
			return (this.name == nfo.name && 
					this.ip_addr == nfo.ip_addr &&
					 this.port_num == nfo.port_num);
		}catch(ClassCastException cce){ return false; }
	}
}