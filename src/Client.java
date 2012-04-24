import static java.lang.System.out;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import msg.ErrorMessage;
import msg.GenericResponse;
import msg.InstantMessage;
import msg.ListResponse;
import msg.ServerMessage;
import net.SpecialSocket;
import server.job.Job;
import utils.UtilityBelt;
import client.AbstractMessageArbiter;
import data.InvalidRecordException;
import data.Record;
import data.SearchQuery;
import data.ServerInfo;


public class Client {

    private static boolean running;
    private static SpecialSocket socket;
    private static MessageArbiter arbiter;
    private static int port_num;
    private static String ip_address;
    //TODO: Make sure to get the name of the server on response...
    private static String target_db;
    private static InetAddress inet;
    private ServerInfo me;
	
    private static List<Record> disconnected_servers;
    
    /** *************** START INPUT METHODS *************** **/
    
    private static String get_valid_name(boolean can_be_wild, boolean can_be_null){
        Scanner scan = new Scanner(System.in);
        boolean valid_name = false;
        String name;
        out.print("Record Name (max 80)"+(can_be_wild? "with wildcard *":"")+(can_be_null?" or enter to skip":"")+": ");
        do{
            name = scan.nextLine().trim();
            if(! (valid_name = Record.valid_name(name)) || (can_be_null && name.length() > 0)){
                out.println("Invalid record name. Name should be between " +(can_be_null?"0":"1")+" and 80 characters in length.");
            }
        }while(!valid_name || (can_be_null &&name.length() == 0));
        return name;
    }
    
    private static String get_valid_ip(boolean can_be_wild, boolean can_be_null){
        Scanner scan = new Scanner(System.in);
        boolean valid_ip = false;
        String ip_addr;
        out.print("Record IP (X.X.X.X with X [0-255]"+(can_be_wild? " and *":"")+(can_be_null?" or enter to skip":"") +"): ");
        do{
            ip_addr = scan.nextLine().trim();
            if(can_be_wild){
                if(! (valid_ip = Record.valid_wild_ip(ip_addr))){
                    out.println("Invalid IP address. Address in form of X.X.X.X, where X is number from 0-255 with wildcard * per digit.");
                }
            }else{
                if(! (valid_ip = Record.valid_ip(ip_addr))){
                    if(can_be_null){
                        if(ip_addr.length() == 0){
                            break;
                        }else
                            out.println("Invalid IP address. Address in form of X.X.X.X, where X is number from 0-255 (or hit enter to leave null).");
                    }else{
                        out.println("Invalid IP address. Address in form of X.X.X.X, where X is number from 0-255.");
                    }
                }
            }
        }while(!valid_ip);
        return ip_addr;
    }
    
    private static int get_valid_port(boolean can_be_0){
        Scanner scan = new Scanner(System.in);
        boolean valid_port = false;
        int port = 0;
        out.print("Record Port (1024-65535)"+(can_be_0?" or enter to skip":"")+": ");
        do{
            try{
                port = Integer.valueOf(scan.nextLine().trim());
                if(! (valid_port = Record.valid_port(port))){
                    out.println("Invalid port. Port is integer betwee 1024-65535.");
                }
            }catch(NumberFormatException nfe){
                if(can_be_0) break;
            }
        }while(!valid_port);
        return port;
    }
    
    /** *************** ENDING INPUT METHODS *************** **/
    /** *************** START SETUP METHOD *************** **/
    private static void setup(){
        ip_address = get_valid_ip(false, false);
        port_num = get_valid_port(false);
        out.println("Server information set... I hope you tested these first.");
        out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    /** *************** ENDING 
    
    
    /** *************** START SHUTDOWN METHODS *************** **/
    
    private static void disconnected(String srv) throws InvalidRecordException{
    	disconnected_servers.add(Record.create_record(srv, ip_address, port_num));
    	port_num = 0;
    	ip_address = null;
    	target_db = null;
    }
    
	private static void kill() throws UnknownHostException, IOException, JAXBException, InvalidRecordException{
        if(port_num != 0 && ip_address != null){
            ServerMessage msg = ServerMessage.write_msg(Job.SHUT_DOWN, null);
            socket.send(msg, inet, port_num);
            DatagramPacket pkt = socket.accept();
            if(pkt != null){
            	try{
                	GenericResponse rsp = (GenericResponse) UtilityBelt.bytes_2_java(pkt.getData(), msg.GenericResponse.class);
                	String srv = rsp.get_response();
                	disconnected(srv);
                	out.println("Server was shut down.");
                }catch(ClassCastException cce){
                	out.println("Error in shutting down...");
                }
            }else out.println("Timeout error experienced on this server...");
            
        }else out.println("You must provide a server address to run this command.");
        out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
	
	/** *************** ENDING SHUTDOWN METHOD ************** **/
	
	/** ************** TESTING METHOD START ************** **/
	
    private static void test(){
        String ip = get_valid_ip(false, false);
        int port = get_valid_port(false);
        ServerMessage msg = ServerMessage.write_msg(Job.TEST, new byte[1]);
        try{
            socket.send(msg, InetAddress.getByName(ip), port);
            DatagramPacket pkt = socket.accept(1024, 5000);
            UtilityBelt.bytes_2_java(pkt.getData(), GenericResponse.class);
            out.printf("Host %s:%d reached.\n", pkt.getAddress().getHostAddress(), 
                       pkt.getPort());
        }catch(UnknownHostException uhe){
            out.println("Unknown host...");
        } catch (IOException e) {
            out.println("Unable to reach the host... timed out");
        }catch(JAXBException jaxbe){
            out.println("Unable to reach the host... error at host");
        }
        out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
	
    /** *************** END TESTING METHOD *************** **/
    /** *************** START LINK METHODS *************** **/
    
    private static void link() throws UnknownHostException, IOException, JAXBException{
    	String server_name = get_valid_name(false, true);
    	String this_ip_addr = "";
    	int this_port_num = 0;
    	if(server_name.length() == 0){
    		this_ip_addr = get_valid_ip(false, false);
    		this_port_num = get_valid_port(false);
    	}
    	byte[] bytes = UtilityBelt.java_2_bytes(ServerInfo.get_nfo(server_name, this_ip_addr, this_port_num), ServerInfo.class);
    	ServerMessage msg = ServerMessage.write_msg(Job.LINK, bytes);
    	socket.send(msg, inet, port_num);
    	try{
    		UtilityBelt.bytes_2_java(socket.accept().getData(), GenericResponse.class);
    		out.println("Successfully linked servers.");
    	}catch(ClassCastException cce){ 
    		out.println("Unable to form the link... does the server know of it?");
    	}
    	out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    private static void unlink() throws UnknownHostException ,JAXBException, IOException{
    	String server_name = get_valid_name(false, true);
    	String this_ip_addr = "";
    	int this_port_num = 0;
    	if(server_name.length() == 0){
    		this_ip_addr = get_valid_ip(false, false);
    		this_port_num = get_valid_port(false);
    	}
    	byte[] bytes = UtilityBelt.java_2_bytes(ServerInfo.get_nfo(server_name, this_ip_addr, port_num), ServerInfo.class);
    	ServerMessage msg = ServerMessage.write_msg(Job.UNLINK, bytes);
    	socket.send(msg, inet, port_num);
    	try{
    		UtilityBelt.bytes_2_java(socket.accept().getData(), GenericResponse.class);
    		out.println("Successfully unlinked servers.");
    	}catch(ClassCastException cce){ 
    		out.println("Unable to form the link... does the server know of it?");
    	}
    	out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    private static void show_links() throws UnknownHostException, JAXBException, IOException{
    	String server_name = get_valid_name(false, true);
    	byte[] server_data = UtilityBelt.java_2_bytes(ServerInfo.get_nfo(server_name, 0), ServerInfo.class);
    	ServerMessage msg = ServerMessage.write_msg(Job.VIEW_LINK, server_data);
    	socket.send(msg,inet, port_num);
    	byte[] data = socket.accept().getData();
    	try{
    		ListResponse<?> rsp = (ListResponse<?>) UtilityBelt.bytes_2_java(data, ListResponse.class);
    		display_links(rsp.get_records(), rsp.get_timestamp());
    	}catch(ClassCastException cce){ out.println("Unable to view links..."); }
    	out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    private static void display_links(List<?> records, String timestamp){
        out.printf("%65s\n", "RETRIEVED LINKS");
        print_separator("_");
        String format = "%5s %80s %16s %05d\n";
        int counter = 1;
        out.printf("%5s %80s %16s %5s\n", "NUM.", "Record name", "IP Address", "Port");
        print_separator("_");
        if(records == null)
            records = new ArrayList<Record>();
        for(Object link : records){
            if(link instanceof Record){
            	Record item = (Record) link;
            	out.printf(format, counter++, item.get_name(), item.get_ip(), item.get_port());
            }
        }
        out.println();
        print_separator("_");
        out.printf("Retrieved %d links from %s:%d/%s at %s\n", records.size(), ip_address, port_num, target_db, timestamp);
    }
    	
    /** *************** END LINK METHODS *************** **/
    
	/** ******** START RECORD ALTERING METHODS *********** **/
    
    private static void add_record() throws UnknownHostException, IOException, JAXBException, InvalidRecordException{
        if(port_num != 0 && ip_address != null){
            Record record = Record.create_record(get_valid_name(false, false), get_valid_ip(false, false), get_valid_port(false));
            byte[] bytes = UtilityBelt.java_2_bytes(record, Record.class);
            ServerMessage msg = ServerMessage.write_msg(Job.WRITE, bytes);
            socket.send(msg, inet, port_num);
            try{
            	UtilityBelt.bytes_2_java(socket.accept().getData(), GenericResponse.class);
            	out.printf("Successfully added record to %s:%d/%s at %s\n", ip_address, port_num, target_db,msg.get_timestamp());
            }catch(UnmarshalException cce){ out.println("Unable to add record to server"); }
        }else out.println("You must provide a server address to run this command.");
        out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    private static void delete() throws JAXBException, IOException{
        if(port_num != 0 && ip_address != null){
            String name = get_valid_name(false, false);
            String ip = get_valid_ip(false, true);
            ip = (ip.length() > 0) ? ip : null;
            int port = get_valid_port(true);
            byte[] bytes = UtilityBelt.java_2_bytes(SearchQuery.make_delete_query(name, ip, port), SearchQuery.class);
            ServerMessage msg = ServerMessage.write_msg(Job.DELETE, bytes);
            socket.send(msg, inet, port_num);
            try{
            	UtilityBelt.bytes_2_java(socket.accept().getData(), GenericResponse.class);
            	out.printf("Successfully deleted record from %s:%d at %s\n", ip_address, port_num, msg.get_timestamp());
            }catch(UnmarshalException cce){ out.println("Unable to delete record from server"); }
             
        }else out.println("You must provide a server address to run this command.");
        out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    
    @SuppressWarnings("unchecked")
	private static void find() throws UnknownHostException, IOException, JAXBException{
        if(port_num != 0 && ip_address != null){
            String name = get_valid_name(true, false);
            String ip = get_valid_ip(true, false);
            ip = (ip.length() > 0) ? ip : null;
            byte[] bytes = UtilityBelt.java_2_bytes(SearchQuery.make_retrieve_query(name, ip), SearchQuery.class);
            ServerMessage msg = ServerMessage.write_msg(Job.READ, bytes);
            socket.send(msg, inet, port_num);
            DatagramPacket pkt = socket.accept();
            try{
            	ListResponse<Record> rsp = (ListResponse<Record>) UtilityBelt.bytes_2_java(pkt.getData(), ListResponse.class);
            	display_records(rsp.get_records(), rsp.get_timestamp());
            }catch(ClassCastException cce){
            	out.println("Unable to perform find command on this server.");
            }
        }else out.println("You must provide a server address to run this command.");
    }
    
    private static void display_records(List<Record> records, String timestamp){
        //TODO: When paging, we can change this accordingly
        out.printf("%65s\n", "RETRIEVED RECORDS");
        print_separator("_");
        String format = "%5s %80s %16s %05d\n";
        int counter = 1;
        out.printf("%5s %80s %16s %5s\n", "NUM.", "Record name", "IP Address", "Port");
        print_separator("_");
        if(records == null)
            records = new ArrayList<Record>();
        for(Record record : records){
            out.printf(format, counter++, record.get_name(), record.get_ip(), record.get_port());
        }
        out.println();
        print_separator("_");
        out.printf("Retrieved %d records from %s:%d/%s at %s\n", records.size(), ip_address, port_num, target_db, timestamp);
        out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    private static void print_separator(String point){
        for(int i=0;i<110;i++) out.print(point.charAt(0));
        out.println();
    }
    
    /** ************* END RECORD ALTERING METHODS *************** **/
	
    /** ************* START MESSAGING METHODS ****************** **/
    
    private static String get_message(){
    	String msg = "";
    	boolean valid_msg = false;
    	Scanner scanner = new Scanner(System.in);
    	do{
    		out.print("Message (must end with '.'): ");
    		if(scanner.hasNextLine()){
        		msg = scanner.nextLine().trim();
        		if(!msg.endsWith(".")){
        			valid_msg = true;
        		}
        	}
    	}while(!valid_msg);
    	return msg;
    }
    
    private static void view_all_msgs(){
    	if(arbiter != null){
    		arbiter.view_messages();
        	try {
    			Thread.sleep(1000);
    		} catch (InterruptedException e) { e.printStackTrace(); }
        	out.print("Press enter to continue");
            new Scanner(System.in).nextLine();
    	}else out.println("You have not registered to send/receive messages.");
    }
    
    private static void view_messages(){
    	if(arbiter != null){
    		String client = get_valid_name(false, false);
        	arbiter.view_messages(client);
        	try {
    			Thread.sleep(1000);
    		} catch (InterruptedException e) { e.printStackTrace(); }
        	out.print("Press enter to continue");
            new Scanner(System.in).nextLine();
    	}else out.println("You have not registered to send/receive messages.");
    }
    
//    private static void send() throws UnknownHostException, IOException, JAXBException{
//    	out.println("Client Name:");
//    	String client = get_valid_name(false, false);
//    	out.println("Server Name:");
//    	String server = get_valid_name(false, false);
//    	out.println("Message: ");
//    	String msg = get_message();
//    	
//    	InstantMessage im = InstantMessage.write_message(sender, receiver, msg)(client, server, ip_address, arbiter.get_port(), msg);
//    	IMMessage message = IMMessage.create_message(im, UUID.randomUUID().toString());
//    	socket.send(message, inet, port_num);
//    	DatagramPacket pkt = socket.accept();
//    	byte[] data = pkt.getData();
//    	if(handle_response(data, GenericResponse.class)){
//    		out.println("Message was sent.");
//    	}else out.println("Unable to send message...");
//    	out.print("Press enter to continue");
//        new Scanner(System.in).nextLine();
//    }
    
    
    private static void register() throws InvalidRecordException, UnknownHostException, IOException, JAXBException{
    	String client_name = get_valid_name(false, false);
    	int port = get_valid_port(false);
    	if(arbiter == null){
    		try{
    			arbiter = new MessageArbiter(client_name, port);
        		ServerMessage msg = ServerMessage.write_msg(Job.REGISTER, UtilityBelt.java_2_bytes(arbiter.get_profile(), ServerInfo.class));
            	socket.send(msg, inet, port_num);
            	byte[] data = socket.accept().getData();
            	try{
            		UtilityBelt.bytes_2_java(data, GenericResponse.class);
            		out.printf("Successfully registered %s to server.\n", client_name);
            		arbiter.start();
            	}catch(ClassCastException cce){
            		arbiter = null;
            		out.println("Unable to register handle to server...");
            	}
    		}catch(BindException be){ 
    			out.println("Port already in use"); 
    			arbiter = null;
    		}
    	}else{
    		out.printf("Already registered as %s:%d.\n", arbiter.get_profile().name, arbiter.get_profile().port_num);
    	}
    	out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    	
    }
    
    private static void unregister() throws IOException, JAXBException{
    	if(arbiter != null){
    		SearchQuery delete_query = SearchQuery.make_delete_query(arbiter.get_profile().name, 
    																	arbiter.get_profile().ip_addr, 
    																		arbiter.get_profile().port_num);
    		ServerMessage msg = ServerMessage.write_msg(Job.UNREGISTER, UtilityBelt.java_2_bytes(delete_query, SearchQuery.class));
    		socket.send(msg, inet, port_num);
        	byte[] data = socket.accept().getData();
        	try{
        		UtilityBelt.bytes_2_java(data, GenericResponse.class);
        		out.println("Successfully unregistered handle from server");
        		arbiter.close();
        		arbiter = null;
        	}catch(ClassCastException cce){
        		out.println("It seems there is a problem with unregistering. Server Error");
        	}
    	}else out.println("Unable to unregister... It seems like you havent registered yet....");
    	out.print("Press enter to continue");
        new Scanner(System.in).nextLine();
    }
    
    
    /** ************* END MESSAGING METHODS ****************** **/
    
    /** ************** START LIVE MENU METHODS *************** **/
    
    private static int valid_input(String value){
        try{
            return Integer.parseInt(value);
        }catch(NumberFormatException nfe){ return -1; }
        
    }
    
    private static void menu(){
        out.println();
        out.println("_____________________________________________________");
        out.println("List of commands. NOTE server_name is the port number");
        out.println("_____________________________________________________");
        out.println(" 0.  Server <ip_address> <port_num> [dbname]     ");
        out.println(" 1.  Test   <ip_address> <port_num>              ");
        out.println(" 2.  Insert <name> <ip_address> <port_num>       ");
        out.println(" 3.  Delete <name> [ip_address] [port_num]       ");
        out.println(" 4.  Find   <wild_name> <wild_ip>                ");
        out.println(" 5.  Link    <server_name>                       ");
        out.println(" 6.  Unlink    <server_name>                     ");
        out.println(" 7.  Show Links [<server_list>]                  ");
        out.println(" 8.  Forwarding [<server_list>]                  ");
        out.println(" 9.  Register <handle> <port_num>                ");
        out.println(" 10.  Unregister                                  ");
        out.println(" 11. Show Users <client_list> <server list>      ");
        out.println(" 12. Send <client_name> <server_name> <message>  ");
        out.println(" 13. View Message [client_name]                  ");
        out.println(" 14.  Kill                                       ");
        out.println(" 15.  Exit Program                               ");
        out.println("_____________________________________________________");
        out.print("Enter Choice (0-15): ");
    }
    
    private static void live_mode() throws InvalidRecordException, UnknownHostException, IOException, JAXBException{
    	init();
	    Scanner scanner = new Scanner(System.in);
	    while(running){
	        int choice;
	        menu();
	        if ((choice = valid_input(scanner.nextLine())) != -1){
	            switch(choice){
	                case 0: //Setup Command
	                	out.println("Setup command");
	                    setup();
	                    break;
	                case 1: //Test Command
	                	out.println("Test command");
	                	test();
	                	break;
	                case 2: //Insert Command
	                	out.println("Insert command");
	                	add_record();
	                	break;
	                case 3: //Delete Command
	                	out.println("Delete command");
	                	delete();
	                	break;
	                case 4: // Find Command
	                	out.println("Search command");
	                	find();
	                	break;
	                case 5: //Link command
	                	out.println("Link command");
	                	link();
	                	break;
	                case 6: //Unlink command
	                	out.println("Unlink command");
	                	unlink();
	                	break;
	                case 7: //Show Links command
	                	out.println("Show Links command");
	                	show_links();
	                	break;
	                case 8: //Forwarding command TODO: I AM HERE>>>
	                	out.println("Forwarding command");
	                	forwarding();
	                	break;
	                case 9: //Register command
	                	out.println("Register command");
	                	register();
	                	break;
	                case 10: //Unregister command
	                	out.println("Unregister command");
	                	unregister();
	                	break;
	                case 11: //Show Users command
	                	out.println("Show Users command");
	                	show_users();
	                	break;
	                case 12: //Send Message command
	                	out.println("Send command");
	                	send();
	                	break;
	                case 13: //View Message command
	                	out.println("Send command");
	                	view_msg();
	                	break;
	                case 14: //Shutdown command
	                	out.println("Shutdown command");
	                	kill();
	                	break;
	                case 15: //Exit command
	                default:
	                	out.println("Exit Program");
	                	running = false;
	                	break;
	                
	            }
	        }else continue;
	    }
	    out.println("Closing client...");
    }
    
    /** ************** ENDING MENU METHODS **************** **/
    /** ************** START STARTUP METHODS *************** **/
    private static void init() throws SocketException, UnknownHostException{
        running = true;
        socket = SpecialSocket.create_socket();
        inet = InetAddress.getByName(ip_address);
        disconnected_servers = new ArrayList<Record>();
    }
    
    
	public static void main(String[] args) throws InvalidRecordException, UnknownHostException, IOException, JAXBException {
	    //test_mode();
		live_mode();
	    
	}
	
	static class MessageArbiter extends AbstractMessageArbiter{

		protected MessageArbiter(String handle, int port) throws SocketException, InvalidRecordException, UnknownHostException {
			super(handle, port);
		}
		
	}
	
}
