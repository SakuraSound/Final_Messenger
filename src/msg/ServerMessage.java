package msg;


import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import server.job.Job;
import utils.UtilityBelt;

@XmlRootElement(name="server_msg")
public class ServerMessage implements Message{
	@XmlAttribute(name="timestamp")
	private String timestamp;
	@XmlElement(name="job_type")
	private Job type;
	@XmlElement(name="data")
	private byte[] data;
	
	public String get_timestamp(){ return timestamp; }
	public Job get_job(){ return type; }
	public byte[] get_data(){ return data; }
	
	// This empty constructor is for JAXB marshalling/unmarshalling
	private ServerMessage(){}
	
	private ServerMessage(Job type, byte[] bytes){
		this.type = type;
		this.data = bytes;
		timestamp = UtilityBelt.get_timestamp();
	}
	
	public static ServerMessage write_msg(Job type, byte[] data){
		return new ServerMessage(type, data);
	}
	
	public byte[] to_bytes() throws JAXBException {
		// TODO Auto-generated method stub
		return UtilityBelt.java_2_bytes(this, getClass());
	}

}
