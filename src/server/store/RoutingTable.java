package server.store;

import static java.lang.System.out;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import msg.Error;
import msg.ErrorMessage;
import msg.GenericResponse;
import msg.ServerMessage;
import net.SpecialSocket;
import server.job.Job;
import utils.UtilityBelt;
import data.ForwardingData;
import data.InvalidRecordException;
import data.Record;
import data.ServerInfo;


public class RoutingTable {
	private Timer status_pulsor;
	private ServerInfo me;
	private AdjacencyMatrix table;
	private HashSet<String> update_filter;
	private SpecialSocket out_socket;
	protected ConcurrentHashMap<ServerInfo, Integer> link_status;
	private AtomicBoolean is_active;
	private SpecialSocket pulse_socket;
	
	public static RoutingTable build_table(ServerInfo me) throws SocketException{
		return new RoutingTable(me);
	}
	
	private RoutingTable(ServerInfo me) throws SocketException{
		this.me = me;
		this.table = new AdjacencyMatrix();
		this.out_socket = SpecialSocket.create_socket();
		this.update_filter = new HashSet<String>();
		this.status_pulsor = new Timer();
		this.pulse_socket = SpecialSocket.create_socket();
		this.is_active = new AtomicBoolean(true);
		this.link_status = new ConcurrentHashMap<ServerInfo, Integer>();
		status_pulsor.schedule(new StatusPulse(), 0, 15000L);
		
	}
	
	private boolean ping(ServerInfo link_to) throws IOException, JAXBException{
		out_socket.send(ServerMessage.write_msg(Job.TEST, null), link_to.inet, link_to.port_num);
		return out_socket.accept(1024, 5000) != null;
	}
	
	public void kill() throws IOException, JAXBException{
		is_active.set(false);
		UpdateInfo update = new UpdateInfo(me, table.get_direct_links_from(me), false);
		update_filter.add(update.get_id());
		byte[] data = UtilityBelt.java_2_bytes(update, UpdateInfo.class);
		ServerMessage msg = ServerMessage.write_msg(Job.LINK_STATE_PULSE, data);
		
		propagate(msg, out_socket);
		table.remove_server(me);
	}
	
	
	public void forwarding_task(ServerInfo from, InetAddress inet, int port_num) throws SocketException{
		new ForwardingTask(from, inet, port_num).start();
	}
	
	public void link(ServerInfo nfo, InetAddress inet, int port_num, RecordStore clients) throws IOException{
		new LinkTask(Job.LINK, nfo, clients, inet, port_num).start();
	}
	
	public void be_linked(Job type, ServerInfo nfo, InetAddress inet, int port_num, RecordStore clients) throws IOException{
		new LinkTask(type, nfo, clients,inet, port_num).start();
	}
	
	public void be_unlinked(ServerInfo nfo, InetAddress inet, int port_num) throws SocketException{
		new LinkTask(Job.SERVER_UNLINK, nfo, null, inet, port_num).start();
	}
	
	public void unlink(ServerInfo nfo, InetAddress inet, int port_num) throws SocketException{
		new LinkTask(Job.UNLINK, nfo, null, inet, port_num).start();
	}
	
	public List<Record>  get_links(ServerInfo from) throws InvalidRecordException{
		ArrayList<Record> links =new ArrayList<Record>();
		for(ServerInfo nfo : table.get_direct_links_from(from)){
			links.add(nfo.to_record());
		}
		return links;
	}
	
	public Map<ServerInfo, List<ServerInfo>> get_routing_table(){
		return table.get_table();
	}
	
	

	
	public ServerInfo get_next_hop(final ServerInfo dest){
		if(!link_status.containsKey(dest)){
			ServerInfo best = null;
			PriorityQueue<QueueItem> level_order = new PriorityQueue<QueueItem>();
			for(ServerInfo nfo : link_status.keySet()){
				if(link_status.get(nfo) > 0){ // If we trust them (i.e. they answer our pings...)
					level_order.add(new QueueItem(nfo));
				}
			}
			while(best == null && !level_order.isEmpty()){
				QueueItem item = level_order.poll();
				List<ServerInfo> current_links = table.get_direct_links_from(item.current);
				for(ServerInfo nfo : current_links){
					if(nfo == dest){
						best = item.traversed.iterator().next();
						break;
					}else if(!item.traversed.contains(nfo)){
						level_order.add(new QueueItem(item.traversed, nfo));
					}
				}
			}
			return best;
		}else return dest;	
	}
	
	
	public void update_table(byte[] bytes) throws IOException{
		try{
			UpdateInfo update = (UpdateInfo) UtilityBelt.bytes_2_java(bytes, UpdateInfo.class);
			if(!update_filter.contains(update.get_id())){
				synchronized(update_filter){
					update_filter.add(update.get_id());
				}
				if(update.still_alive()){
					
					out.println("Starting living update");
					out.printf("   From Server: %s\n", update.get_info());
					out.print("   With links: ");
					for(ServerInfo info : update.get_links()){
						out.printf("%s, ", info.toString());
					}
					out.println();
					table.update_links(update.get_info(), update.get_links());
				}else{
					table.remove_server(update.get_info());
				}
				propagate(ServerMessage.write_msg(Job.LINK_STATE_PULSE, bytes), out_socket);
			}
		}catch(ClassCastException cce){ cce.printStackTrace(); }
		 catch(JAXBException jaxbe){ jaxbe.printStackTrace(); }
	}
	
	private void propagate(ServerMessage msg, SpecialSocket skt) throws IOException, JAXBException{
		for(ServerInfo nfo : link_status.keySet()){
			skt.send(msg, nfo.inet, nfo.port_num);
		}
	}
	
	public Map<ServerInfo, List<ServerInfo>> get_table(){
		return table.get_table();
	}
	
	public boolean still_accessible(ServerInfo nfo){
		return table.get_direct_links_from(nfo).size() > 0;
	}
	
	private class StatusPulse extends TimerTask{
		
		
		private void send_updates_to_links() throws JAXBException, IOException{
			List<ServerInfo> links = Collections.list(link_status.keys());
			UpdateInfo update = UpdateInfo.create(me, links, is_active.get());
			byte[] data = UtilityBelt.java_2_bytes(update, UpdateInfo.class);
			for(ServerInfo info : link_status.keySet()){
				pulse_socket.send(ServerMessage.write_msg(Job.LINK_STATE_PULSE, data), info.inet, info.port_num);
			}
		}
		
		private void test_links(){
			for(ServerInfo info : link_status.keySet()){
				DatagramPacket pkt = null;
				try {
					pulse_socket.send(ServerMessage.write_msg(Job.TEST, null), info.inet, info.port_num);
					pkt = pulse_socket.accept(1024, 2000);
					if(pkt == null){
						link_status.replace(info, Math.max(link_status.get(info) - 1, 0));
					}else{
						link_status.replace(info, 3);
					}
				} catch (IOException e) { e.printStackTrace();
				} catch (JAXBException e) { e.printStackTrace(); }
			}
		}
		
		public void run(){
			test_links();
			try {
				send_updates_to_links();
			} catch (JAXBException e) { e.printStackTrace();
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	
	@XmlRootElement(name="network_update")
	static class UpdateInfo{
		@XmlAttribute(name="uuid")
		private String id;
		@XmlElement(name="my_links")
		private List<ServerInfo> links;
		@XmlElement(name="living")
		private boolean still_alive;
		@XmlElement(name="from_who")
		private ServerInfo about_me;
		
		public String get_id(){ return id; }
		public List<ServerInfo> get_links(){ return links; }
		public boolean still_alive(){ return still_alive; }
		public ServerInfo get_info(){ return about_me; }
		
		private UpdateInfo(){}
		
		private UpdateInfo(ServerInfo from, List<ServerInfo> links, boolean alive){
			this.id = UUID.randomUUID().toString();
			this.links = links;
			this.still_alive = alive;
			this.about_me = from;
		}
		
		public static UpdateInfo create(ServerInfo from, List<ServerInfo> links, boolean alive){
			return new UpdateInfo(from, links, alive);
		}
	}
	
	
	private class ForwardingTask extends Thread{
		SpecialSocket task_socket;
		ServerInfo from;
		InetAddress client_inet;
		int client_port;
		
		public int get_shortest_distance(final ServerInfo from, final ServerInfo dest){
			PriorityQueue<QueueItem> level_order = new PriorityQueue<QueueItem>();
			for(ServerInfo nfo : table.get_direct_links_from(from)){
				level_order.add(new QueueItem(nfo));
			}
			while(!level_order.isEmpty()){
				QueueItem item = level_order.poll();
				List<ServerInfo> current_links = table.get_direct_links_from(item.current);
				for(ServerInfo nfo : current_links){
					if(nfo == dest){
						return item.traversed.size();
					}else if(!item.traversed.contains(nfo)){
						level_order.add(new QueueItem(item.traversed, nfo));
					}
				}
			}
			return -1;
		}
		
		public void run(){
			HashMap<ServerInfo, Integer> forwarding_tbl = new HashMap<ServerInfo, Integer>();
			List<ServerInfo> links = table.get_direct_links_from(from);
			forwarding_tbl.put(from, 0);
			for(ServerInfo nfo : links){
				forwarding_tbl.put(nfo, 1);
			}
			for(ServerInfo dest : table.get_non_linked_keys(from)){
				forwarding_tbl.put(dest, get_shortest_distance(from, dest));
			}
			ForwardingData data = ForwardingData.make(from, forwarding_tbl);
			try{
				String xml = new String(UtilityBelt.java_2_bytes(data, ForwardingData.class));
				task_socket.send(GenericResponse.create_response(xml), client_inet, client_port);
			}catch(JAXBException jaxbe){ 
				jaxbe.printStackTrace();
				try {
					task_socket.send(ErrorMessage.create_message(Error.SERVER_SIDE_ERROR), client_inet, client_port);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JAXBException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		public ForwardingTask(ServerInfo from, InetAddress inet, int port_num) throws SocketException{
			this.from = from;
			this.client_inet = inet;
			this.client_port = port_num;
			this.task_socket = SpecialSocket.create_socket();
		}
	}
	
	private class LinkTask extends Thread{
		Job type;
		SpecialSocket task_socket;
		ServerInfo link_to;
		RecordStore clients;
		InetAddress client_inet;
		int client_port;
		
		
		public void be_linked() throws IOException, JAXBException{
			if(type == Job.SERVER_LINK){
				System.out.println("ip: "+ link_to.ip_addr+", port: "+link_to.port_num);
				table.add_link(me, link_to);
				link_status.put(link_to, 3);
				ServerMessage c_msg = ServerMessage.write_msg(Job.INITIAL_REGISTRATION_LOAD, clients.get_bootstrap(me));
				task_socket.send(c_msg, link_to.inet, link_to.port_num);
				System.out.println(client_inet);
				System.out.println(client_port);
				task_socket.send(GenericResponse.create_response("clear"), client_inet, client_port);
			}else{
				table.remove_link(me, link_to);
				link_status.remove(link_to);
				task_socket.send(GenericResponse.create_response("unlinked"), client_inet, client_port);
			}
		}
		
		public void link() throws IOException, JAXBException{
			if(table.add_link(me, link_to)){
				link_status.put(link_to, 3);
				byte[] bytes = UtilityBelt.java_2_bytes(me, ServerInfo.class);
				ServerMessage slink = ServerMessage.write_msg(Job.SERVER_LINK, bytes);
				System.out.println(link_to.inet);
				task_socket.send(slink, link_to.inet, link_to.port_num);
				System.out.println(link_to.inet);
				System.out.println(link_to.port_num);
				DatagramPacket pkt = task_socket.accept();
				if(pkt != null){
					//TODO: send updated record store and finish...
					ServerMessage c_msg = ServerMessage.write_msg(Job.INITIAL_REGISTRATION_LOAD, clients.get_bootstrap(me));
					task_socket.send(c_msg, link_to.inet, link_to.port_num);
					task_socket.accept();
					GenericResponse rsp = GenericResponse.create_response("Successfully linked");
					task_socket.send(rsp, client_inet, client_port);
				}else{ //unable to link up...
					ErrorMessage msg = ErrorMessage.create_message(Error.LINKAGE_ERROR);
					task_socket.send(msg, client_inet, client_port);
				}
			}else{ //Duplicate links....
				ErrorMessage msg = ErrorMessage.create_message(Error.DUPLICATE_LINK_ERROR);
				task_socket.send(msg, client_inet, client_port);
			}
		}
		
		public void unlink() throws JAXBException, IOException{
			if(table.remove_link(me, link_to)){
				link_status.remove(link_to);
				byte[] bytes = UtilityBelt.java_2_bytes(me, ServerInfo.class);
				ServerMessage slink = ServerMessage.write_msg(Job.SERVER_UNLINK, bytes);
				task_socket.send(slink, link_to.inet , link_to.port_num);
				DatagramPacket pkt = task_socket.accept();
				if(pkt != null){
					GenericResponse rsp = GenericResponse.create_response("Successfully unlinked");
					task_socket.send(rsp, client_inet, client_port);
				}else{
					ErrorMessage msg = ErrorMessage.create_message(Error.LINKAGE_ERROR);
					task_socket.send(msg, client_inet, client_port);
				}
			}else{
				ErrorMessage msg = ErrorMessage.create_message(Error.LINKAGE_ERROR);
				task_socket.send(msg, client_inet, client_port);
			}
		}
		
		public void run(){
			try {
				if(type != Job.SERVER_LINK && type != Job.SERVER_UNLINK){
					if(ping(link_to)){
						if(type == Job.LINK) link();
						else unlink();
					}else{
						ErrorMessage msg = ErrorMessage.create_message(Error.COMMUNICATION_ERROR);
						task_socket.send(msg, link_to.inet, link_to.port_num);
					}
				} else be_linked();
			}catch (SocketException e) { e.printStackTrace(); } 
			 catch (IOException e) { e.printStackTrace(); }
			 catch (JAXBException e) { e.printStackTrace(); }
		}
		
		public LinkTask(Job type, ServerInfo link_to, RecordStore clients, InetAddress client_inet, int client_port) throws SocketException{
			super();
			this.type = type;
			this.task_socket = SpecialSocket.create_socket();
			this.client_inet = client_inet;
			this.client_port = client_port;
			this.link_to = link_to;
			this.clients = clients;
		}
		
	}
	
	private class QueueItem implements Comparable<QueueItem>{
		public Set<ServerInfo> traversed;
		public ServerInfo current;
		
		public QueueItem(Set<ServerInfo> traversed, ServerInfo current){
			this.current = current;
			this.traversed = traversed;
			this.traversed.add(current);
		}
		public QueueItem(ServerInfo current){
			this.current = current;
			this.traversed = new LinkedHashSet<ServerInfo>();
			this.traversed.add(current);
		}
		
		public int compareTo(QueueItem item){
			return this.traversed.size() - item.traversed.size();
		}
	}
	
	private class AdjacencyMatrix{
		private ConcurrentHashMap<ServerInfo, CopyOnWriteArrayList<ServerInfo>> table;
		
		public List<ServerInfo> get_non_linked_keys(ServerInfo from){
			ArrayList<ServerInfo> lst = new ArrayList<ServerInfo>();
			for(ServerInfo nfo : table.keySet()){
				if(!table.get(from).contains(nfo) && !nfo.equals(from)) lst.add(nfo);
			}
			return lst;
		}
		
		public Map<ServerInfo, List<ServerInfo>> get_table(){
			Map<ServerInfo, List<ServerInfo>> ret_tbl = new HashMap<ServerInfo, List<ServerInfo>>();
			for(Entry<ServerInfo, CopyOnWriteArrayList<ServerInfo>> entry: table.entrySet()){
				ret_tbl.put(entry.getKey(), new ArrayList<ServerInfo>(entry.getValue()));
			}
			return ret_tbl;
		}
		
		public AdjacencyMatrix(){
			this.table = new ConcurrentHashMap<ServerInfo, CopyOnWriteArrayList<ServerInfo>>();
		}
		
		public List<ServerInfo> get_direct_links_from(final ServerInfo nfo){
			if(table.containsKey(nfo)){
				return table.get(nfo);
			}else{ return new ArrayList<ServerInfo>(); }
		}
		
		public synchronized void update_links(ServerInfo from, List<ServerInfo> links){
			out.println("Updating links");
			out.println("Current Network: ");
			for(ServerInfo info : table.keySet()){
				out.printf(info+": ");
				for(ServerInfo lnk : table.get(info)){
					out.printf(lnk+", ");
				}
				out.println();
			}
			out.printf("Link to update: %s\n", from);
			out.println("Links to this server");
			for(ServerInfo info : links){
				out.printf("%s, ", info);
			}
			out.println();
			out.printf("table has From: %b\n", table.get(from) != null);
			out.println(table.keySet());
			out.println(table.get(from));
			if(table.containsKey(from)){
				out.println("Found match... updating");
				table.replace(from, new CopyOnWriteArrayList<ServerInfo>(links));
			}else{
				out.println("No match found... adding");
				table.put(from, new CopyOnWriteArrayList<ServerInfo>(links));
			}
		}
		
		
		public synchronized boolean add_link(ServerInfo from, ServerInfo dest){
			out.println("Adding link to routing table");
			if(table.containsKey(from)){
				if(table.get(from).contains(dest)){
					//Already linked...
					return false;
				}
				table.get(from).add(dest);
			}else{
				out.println("Link does not exist... creating link!");
				table.put(from, new CopyOnWriteArrayList<ServerInfo>());
				table.get(from).add(dest);
			}
			return true;
		}
		
		public boolean accessible(ServerInfo dest){
			return table.containsKey(dest)? table.get(dest).size() > 0: false;
		}
		
		public boolean appx_still_accessible(ServerInfo dest){
			for(ServerInfo nfo : table.keySet()){
				if(nfo.appx_equals(dest)){
					return table.get(nfo).size() > 0;
				}
			}
			return false;
		}
		
		public synchronized boolean remove_link(ServerInfo from, ServerInfo to){
			return table.get(from).remove(to);
		}
		
		public synchronized boolean remove_server(ServerInfo dead_server){
			if(table.containsKey(dead_server)){
				table.remove(dead_server);
				return true;
			}else{ return false; }
		}
		
	}
}
