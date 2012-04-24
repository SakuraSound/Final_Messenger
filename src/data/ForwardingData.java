package data;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlRootElement(name="forwarding_data")
@XmlSeeAlso({ServerInfo.class, })
public final class ForwardingData {
	@XmlElement(name="from")
	private ServerInfo from;
	@XmlElement(name="table")
	private HashMap<ServerInfo, Integer> table;
	
	public ServerInfo get_start(){ return from; }
	
	public HashMap<ServerInfo, Integer> get_table(){ return table; }
	
	private ForwardingData(){}
	
	private ForwardingData(ServerInfo from, HashMap<ServerInfo, Integer> table){
		this.table = table;
		this.from = from;
	}
	
	public static ForwardingData make(ServerInfo from, HashMap<ServerInfo, Integer> table){
		return new ForwardingData(from, table);
	}
	
	
}
