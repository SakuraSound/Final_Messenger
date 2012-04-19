import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.JAXBException;

import msg.Error;
import msg.ErrorMessage;
import msg.GenericResponse;
import msg.InstantMessage;
import msg.ListResponse;
import msg.ServerMessage;
import net.SpecialSocket;
import server.job.Job;
import server.job.ServerJob;
import server.store.RecordStore;
import server.store.RoutingTable;
import utils.UtilityBelt;
import utils.ds.BloomFilter;
import data.ClientInfo;
import data.InvalidRecordException;
import data.Record;
import data.ServerInfo;


public class Server {
	
	private static RecordStore tables;
	private static PriorityBlockingQueue<ServerJob> queue;
	private static CopyOnWriteArrayList<ServerInfo> links;
	private static ConcurrentHashMap<ServerInfo, RecordStore> clients;
	private static BloomFilter msg_filter;
	private static RoutingTable router;
	private static ServerInfo meta;
	private static Arbiter arbiter;
	private static AtomicBoolean serving;
	private static SpecialSocket out_socket;

	
	
	
	private static final void check_args(String...args) throws UnknownHostException{
		if(args.length != 2){
			out.println("Call is Server <name> <port_num>");
			exit(0);
		}else{
			String name = args[0];
			try{
				int port_num = Integer.parseInt(args[1]);
				if(port_num < 1024 || port_num >65535){
					out.println("Second argument must be an integer between 1024 and 65535");
					exit(0);
				}
				meta = ServerInfo.get_nfo(name, port_num);
			}catch(NumberFormatException nfe){
				out.println("Second argument must be an integer between 1024 and 65535");
				exit(0);
			}
		}
	}
	
	private static final void init() throws SocketException{
		arbiter = Arbiter.get_arbiter(meta);
		queue = new PriorityBlockingQueue<ServerJob>();
		links = new CopyOnWriteArrayList<ServerInfo>();
		clients = new ConcurrentHashMap<ServerInfo, RecordStore>();
		clients.put(meta, RecordStore.make_temp_store(meta.name));
		msg_filter = BloomFilter.create_filter(125000, 500000);
		serving = new AtomicBoolean(true);
		tables = RecordStore.load_from_file(meta.name);
		out_socket = SpecialSocket.create_socket();
		arbiter.start();
	}
	
	private static void update(ServerJob job) throws SocketException, JAXBException{
		Job type = job.get_job();
		ClientInfo client = (ClientInfo) UtilityBelt.bytes_2_java(job.get_data(), ClientInfo.class);
		byte[] bytes = UtilityBelt.java_2_bytes(Record.from_client_info(client), Record.class);
		clients.get(client.get_server()).spawn_task(type, job.get_inet(), job.get_port(), bytes);
	}
	
	public static void main(String... args) throws JAXBException, IOException, InvalidRecordException{
		check_args(args);
		init();
		out.println("Server ONLINE!!!");
		while(serving.get() || !queue.isEmpty()){
			ServerJob job = queue.poll();
			if(job != null && serving.get()){
				Job type = job.get_job();
				if(type == Job.READ || type == Job.WRITE || type == Job.DELETE){
					tables.spawn_task(type, job.get_inet(), job.get_port(), job.get_data());
				}else if(type == Job.LINK_STATE_PULSE){
					router.update_table(job.get_data());
				}else if(type == Job.REGISTER || type == Job.UNREGISTER){
					//TODO: Pass linkage information for this spawn task to send updates 
					clients.get(meta.name).spawn_task(type, job.get_inet(), job.get_port(), job.get_data());
				}else if(type == Job.CLIENT_SEARCH){
					SearchTask.spawn(job.get_inet(), job.get_port(), job.get_data());
				}else if(type == Job.UPDATE_REGISTER || type == Job.UPDATE_UNREGISTER){
					update(job);
				}else if(type == Job.LINK){
					ServerInfo nfo = (ServerInfo) UtilityBelt.bytes_2_java(job.get_data(), ServerInfo.class);
					router.link(nfo, job.get_inet(), job.get_port(), clients.get(nfo.name));
				}else if(type == Job.UNLINK){
					ServerInfo nfo = (ServerInfo) UtilityBelt.bytes_2_java(job.get_data(), ServerInfo.class);
					router.unlink(nfo, job.get_inet(), job.get_port());
				}else if(type == Job.VIEW_LINK){
					out_socket.send(ListResponse.create_message(router.get_links()), job.get_inet(), job.get_port());
				}else if(type == Job.SERVER_LINK || type == Job.SERVER_UNLINK){
					//TODO: Add/remove data to the client registrar...
				}else if (type == Job.SEND_MESSAGE){
					//TODO: Implement code for this using the routing table...
					
				}else if(type == Job.SHUT_DOWN){
					serving.set(false);
					router.kill();
					//TODO: Implement code to handle this...
				}else if(type == Job.TEST){
					out_socket.send(GenericResponse.create_response("test"), job.get_inet(), job.get_port());
				}
			}else{
				//TODO: Add code to send an error message about this server being closed...
			}
		}
		out.println("*** WAITING FOR OTHER TASKS TO FINISH ***");
		
		out.println("*** SERVER SHUTDOWN ***");
		exit(0);
	}
	
	
	private static class MessageTask extends Thread{
		private SpecialSocket out_socket;
		private String final_hop;
		private InstantMessage im;
		
		public static final MessageTask spawn_task(InstantMessage im) throws SocketException{
			return new MessageTask(im);
		}
		
		private MessageTask(InstantMessage im) throws SocketException{
			super();
			this.im = im;
			this.out_socket = SpecialSocket.create_socket();
		}
		
		private ServerInfo get_info(){
			for(ServerInfo info : clients.keySet()){
				if(info.name.equals(final_hop)){
					return info;
				}
			}
			return null;
		}
		
		public void run(){
			ServerInfo final_dest;
			try{
				if((final_dest = get_info()) != null){
					ServerInfo next_hop = router.get_next_hop(final_dest);
					if(next_hop != null){
						//TODO: Send message to this next hop...
						out_socket.send(im, next_hop.inet, next_hop.port_num);
					}else{
						InetAddress sender_inet = InetAddress.getByName(im.get_sender_nfo().get_ip());
						out_socket.send(ErrorMessage.create_message(Error.COMMUNICATION_ERROR), sender_inet, im.get_sender_nfo().get_port());
					}
				}else{
					//TODO: send user a message saying we cannot send message...
					// We cant find the user... are they logged in?
					InetAddress sender_inet = InetAddress.getByName(im.get_sender_nfo().get_ip());
					out_socket.send(ErrorMessage.create_message(Error.USER_OFFLINE), sender_inet, im.get_sender_nfo().get_port());
				}
			}catch(SocketException se){ se.printStackTrace(); }
			 catch (JAXBException e) {
				e.printStackTrace();
			}catch(IOException ioe){
				
			}
			
		}
	}
	
	private static class SearchTask extends Thread{
		private SpecialSocket tmp_socket;
		private InetAddress inet;
		private int port_num;
		private byte[] data;
		
		
		public static SearchTask spawn(InetAddress inet, int port_num, byte[] data) throws SocketException{
			return new SearchTask(inet, port_num, data);
		}
		
		private SearchTask(InetAddress inet, int port_num, byte[] data) throws SocketException{
			this.inet = inet;
			this.port_num = port_num;
			this.data = data;
			this.tmp_socket = SpecialSocket.create_socket();
		}
		
		public void run(){
			try{
				List<ClientInfo> results = new ArrayList<ClientInfo>();
				for(Entry<ServerInfo, RecordStore> pair : clients.entrySet()){
					try{
						pair.getValue().spawn_task(Job.CLIENT_SEARCH, tmp_socket.get_inet(), tmp_socket.get_port(), data);
						DatagramPacket pkt = tmp_socket.accept();
						ListResponse<?> rsp = (ListResponse<?>) UtilityBelt.bytes_2_java(pkt.getData(), ListResponse.class);
						for(Object rec : rsp.get_records()){
							if(rec instanceof Record){
								Record tmp_rec = (Record) rec;
								results.add(ClientInfo.create_data(pair.getKey().name, tmp_rec));
							}
							
						}
					}catch(SocketException se){
						out.printf("Unable to search client list for server %s.\n", pair.getKey());
						se.printStackTrace();
					}catch(IOException ioe){
						out.printf("Unable to search client list for server %s.\n", pair.getKey());
						ioe.printStackTrace();
					}catch(JAXBException jaxbe){
						out.printf("Unable to search client list for server %s.\n", pair.getKey());
						jaxbe.printStackTrace();
					}catch(ClassCastException cce){
						out.printf("Unable to search client list for server %s.\n", pair.getKey());
						cce.printStackTrace();
					}
				}
				tmp_socket.send(ListResponse.create_message(results), inet, port_num);
			}catch (IOException e) {
				e.printStackTrace();
			} catch (JAXBException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private static class Arbiter extends Thread{
		private static SpecialSocket socket;
		
		private void handle_msg(DatagramPacket packet) throws JAXBException, UnknownHostException{
			out.printf("PACKET RECEIVED FROM %15s:%d at %s\n", packet.getAddress().getHostAddress(), 
																packet.getPort(),
															     UtilityBelt.get_timestamp());
			try{
				ServerMessage msg = (ServerMessage) UtilityBelt.bytes_2_java(packet.getData(), ServerMessage.class);
				queue.add(ServerJob.make_job(packet.getAddress(), packet.getPort(),msg.get_job() ,msg.get_data()));
			}catch(ClassCastException cce){}
			try{
				UtilityBelt.bytes_2_java(packet.getData(), InstantMessage.class);
				
			}catch(ClassCastException cce){}
		}
		
		
		public void run(){
			while(serving.get()){
				try {
					DatagramPacket packet;
					packet = socket.non_blocking_accept();
					if(packet != null){
						handle_msg(packet);
					}
				} 
				catch (IOException e) { e.printStackTrace(); }
				catch (JAXBException e) { e.printStackTrace(); }

			}
			out.println("No Longer Listening for requests");
		}
		
		public static Arbiter get_arbiter(ServerInfo nfo) throws SocketException{
			return new Arbiter(nfo.name, nfo.port_num);
		}
		
		private Arbiter(String name, int port_num) throws SocketException{
			socket = SpecialSocket.create_socket(port_num);
		}
		
	}
	
}
