package data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ClientInfo")
public class ClientInfo extends AbstractRecord {

	@XmlElement(name="server")
	private String server;
	
	public String get_server(){ return server; }
	
	public static ClientInfo create_data(final String server, final Record record){
		return new ClientInfo(server, record);
	}
	
	private ClientInfo(){}
	
	private ClientInfo(final String server, final Record record){
		
	}
}
