package client;

import static java.lang.System.out;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import msg.InstantMessage;
import net.SpecialSocket;
import utils.UtilityBelt;
import data.InvalidRecordException;
import data.ServerInfo;

public abstract class AbstractMessageArbiter extends Thread{
	
	private SpecialSocket socket;
	private List<InstantMessage> messages;
	private AtomicBoolean working;
	private int port_num;
	

	private ServerInfo me;
	
	public String get_handle(){ return me.name; }
	public ServerInfo get_profile(){ return me; }
	public int get_port(){ return port_num; }
	
	private void add_message(InstantMessage im){
		messages.add(im);
	}
	
	public void view_messages(String client){
		for(InstantMessage im : messages){
			if(im.get_sender_nfo().get_name().equals(client)){
				out.printf("%s (%s): %s\n", im.get_sender_nfo().get_name(), im.get_timestamp(), im.get_message());
			}
		}
	}
	
	public void view_messages(){
		for(InstantMessage im : messages){
			out.printf("%s (%s): %s\n", im.get_sender_nfo().get_name(), im.get_timestamp(), im.get_message());
		}
	}
	
	private void handle_message(DatagramPacket packet) throws JAXBException{
		InstantMessage msg = (InstantMessage) UtilityBelt.bytes_2_java(packet.getData(), InstantMessage.class);
		add_message(msg);
	}
	
	public void close(){
		working.set(false);
	}
	
	public void run(){
		DatagramPacket packet;
		while(working.get()){
			if((packet = socket.non_blocking_accept()) != null){
				try {
					handle_message(packet);
					out.println("New message received");
				} catch (JAXBException e) {
					// Drop message...
				}
			}
		}
		out.println("Done listening... closing");	
	}
	
	protected AbstractMessageArbiter(String handle, int port) throws SocketException, InvalidRecordException, UnknownHostException{
		this.port_num = port;
		socket = SpecialSocket.create_socket(port);
		this.me = ServerInfo.get_nfo(handle, socket.get_inet().getHostAddress(), port);
		working = new AtomicBoolean(true);
		setDaemon(true);
	}

}
