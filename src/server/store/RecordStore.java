package server.store;

import static java.lang.System.out;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
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
import msg.ListResponse;
import net.SpecialSocket;
import server.job.Job;
import utils.UtilityBelt;
import data.Record;
import data.SearchQuery;

@XmlRootElement(name="RecordStore")
public class RecordStore {
	@XmlAttribute(name="timestamp")
	protected String timestamp;
	@XmlElement(name="name")
	protected String name;
	@XmlElement(name="records")
	protected CopyOnWriteArrayList<Record> records;
	@XmlAttribute(name="persistent")
	protected boolean persistent;
	protected ConcurrentHashMap<String, Task> tasks;
	protected AtomicBoolean kill_switch;
	
	
	
	public boolean is_dead(){
		return kill_switch.get() && tasks.isEmpty();
	}
	
	public void kill_switch(){
		kill_switch.set(true);
	}
	
	
	/**
	 * Find records and return them to querier
	 * @param query
	 * @return
	 */
	protected List<Record> search_records(SearchQuery query){
		List<Record> found_recs = new ArrayList<Record>();
		for(Record rec : records){
			if(rec.get_name().matches(query.get_name())){
				if(rec.get_ip().matches(query.get_ip())){
					found_recs.add(rec);
				}
			}
		}
		return found_recs;
	}
	
	public void spawn_task(Job type, InetAddress inet, int port, byte[] data) throws SocketException{
		new Task(type, inet, port, data).start();
	}
	
	/**
	 * Find duplicate records in our data store
	 * @param record
	 * @return
	 */
	protected boolean find_duplicate(Record record){
		ListIterator<Record> iter = records.listIterator();
		while(iter.hasNext()){
			if(record.equals(iter.next())){
				return true;
			}
		}
		return false;
	}
	
	public List<Record> get_records(){
		ArrayList<Record> out = new ArrayList<Record>();
		Collections.copy(out, records);
		return out;
	}
	
	/**
	 * Add a record to the store
	 * @param record
	 * @return
	 */
	protected boolean add_data(Record record){
		boolean found_duplicate = find_duplicate(record);
		if(!found_duplicate) 
			records.add(record);
		return found_duplicate;
	}
	
	protected boolean delete_data(SearchQuery query){
		List<Integer> ptrs = new ArrayList<Integer>();
		ListIterator<Record> iter = records.listIterator();
		int current = 0;
		while(iter.hasNext()){
			Record this_rec = iter.next();
			if(query.get_ip().length() > 0){
				if(this_rec.get_ip().equals(query.get_ip())){
					if(query.get_port() == this_rec.get_port() || query.get_port() == 0){
						ptrs.add(current);
					}
				}
			}else if(query.get_port() != 0 && this_rec.get_port() == query.get_port()){
				ptrs.add(current);
			}
			current = iter.nextIndex();
		}
		if(ptrs.size() == 0) {
			return false;
		}
		else{
			for(int ptr : ptrs) records.remove(ptr);
			return true;
		}
	}
	
	protected RecordStore(){}
	
	protected RecordStore(String name, boolean persistent){
		this.name = name;
		this.kill_switch = new AtomicBoolean(false);
		this.records = new CopyOnWriteArrayList<Record>();
		this.tasks = new ConcurrentHashMap<String, Task>();
		this.timestamp = UtilityBelt.get_timestamp();
		this.persistent = persistent;
	}
	
	public static RecordStore make_temp_store(String name){
		return new RecordStore(name, false);
	}
	
	public static RecordStore load_from_file(String location){
		File file = new File(location);
		if(file.exists() && file.isFile()){
			try{
				InputStream is = new FileInputStream(file);
				long length = file.length();
				byte[] data = new byte[(int) length];
				int offset = 0;
			    int numRead = 0;
			    
			    while (offset < data.length
			           && (numRead=is.read(data, offset, data.length-offset)) >= 0) {
			        offset += numRead;
			    }
			    if (offset < data.length) {
			        throw new IOException("Could not completely read file "+file.getName());
			    }

			    is.close();
			    return (RecordStore) UtilityBelt.bytes_2_java(data, RecordStore.class);
			} catch (Exception e) {
				out.println("Unable to load data from file... will save new file on exit.");
				return new RecordStore(location, true);
			}
		}else{
			out.println("Unable to load data from file... will save new file on exit.");
			return new RecordStore(location, true);
		}
	}
	
	
	 private final class Task extends Thread{
		private Job type;
		private byte[] data;
		private SpecialSocket socket;
		private InetAddress inet;
		private int port_num;
		
		public void run(){
			try{
				if(!kill_switch.get()){
					tasks.put(getName(), this);
					if(type == Job.DELETE || type == Job.UNREGISTER)       run_delete();
					else if(type == Job.WRITE || type == Job.REGISTER)     run_write();
					else if(type == Job.READ || type == Job.CLIENT_SEARCH) run_read();
					tasks.remove(getName());
				}
			}catch(JAXBException jaxbe){ jaxbe.printStackTrace(); }
			 catch(IOException ioe){ ioe.printStackTrace(); }
			
		}
		
		private final void send_error(Error error) throws JAXBException, IOException{
			ErrorMessage msg = ErrorMessage.create_message(error);
			socket.send(msg, inet, port_num);
		}
		
		private void run_delete() throws JAXBException, IOException{
			try{
				SearchQuery query = (SearchQuery) UtilityBelt.bytes_2_java(data, SearchQuery.class);
				if(delete_data(query)){
					socket.send(GenericResponse.create_response("Deleted records"), inet, port_num);
				}else send_error(Error.RECORD_NOT_FOUND);
			}catch(ClassCastException cce){
				send_error(Error.INVALID_QUERY);
			}
		}
		
		private void run_write() throws JAXBException, IOException{
			try{
				Record record = (Record) UtilityBelt.bytes_2_java(data, Record.class);
				boolean okay = find_duplicate(record);
				if(okay){
					add_data(record);
					socket.send(GenericResponse.create_response("Added record"), inet, port_num);
				}else send_error(Error.OVERWRITE_ERROR);
			}catch(ClassCastException cce){
				send_error(Error.INVALID_RECORD);
			}
		}
		
		private void run_read() throws JAXBException, IOException{
			try{
				SearchQuery query = (SearchQuery) UtilityBelt.bytes_2_java(data, SearchQuery.class);
				ListResponse<Record> resp = ListResponse.create_message(search_records(query));
				socket.send(resp, inet, port_num);
			}catch(ClassCastException cce){
				send_error(Error.INVALID_QUERY);
			}
		}
		
		public Task(Job type, InetAddress inet, int port, byte[] data) throws SocketException{
			this.type = type;
			this.data = data;
			this.inet = inet;
			this.port_num = port;
			this.socket = SpecialSocket.create_socket();
			setName(type.toString() + ": "+UUID.randomUUID().toString());
		}
	}
	
}
