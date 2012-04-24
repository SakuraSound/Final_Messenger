package msg;

import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import utils.UtilityBelt;
import data.Record;

@XmlRootElement(name="instant_msg")
public class InstantMessage implements Message{
	@XmlAttribute(name="timestamp")
	private String timestamp;
	@XmlElement(name="message")
	private String message;
	@XmlElement(name="from_user")
	private Record receiver;
	@XmlElement(name="sender_info")
	private Record sender;
	@XmlAttribute(name="uuid")
	private String uuid;
	
	
	public String get_id(){ return uuid; }
	public String get_timestamp(){ return timestamp; }
	public String get_message(){ return message; }
	public Record get_receiver_nfo(){ return receiver; }
	public Record get_sender_nfo(){ return sender; }
	
	public static InstantMessage write_message(final Record sender, final Record receiver, final String msg){
		return new InstantMessage(sender, receiver, msg);
	}
	
	private InstantMessage(final Record sender, final Record receiver, final String msg){
		this.timestamp = UtilityBelt.get_timestamp();
		this.sender = sender;
		this.receiver = receiver;
		this.message = msg;
		this.uuid = UUID.randomUUID().toString();
	}
	
	private InstantMessage(){ }
	
	public byte[] to_bytes() throws JAXBException {
		// TODO Auto-generated method stub
		return UtilityBelt.java_2_bytes(this, getClass());
	}

}
