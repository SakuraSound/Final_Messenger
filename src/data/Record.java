package data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Record")
public class Record extends AbstractRecord implements Comparable<Record>{

	/**
	 * Factory method for creating Records
	 * @param name The name of the record being stored...
	 * @param ip   The ip address of the creator
	 * @param port_num The port number affiliated with this record
	 * @return a new record object
	 * @throws InvalidRecordException if any of the requirements of record is violated
	 */
	public static Record create_record(final String name, final String ip, final int port_num) throws InvalidRecordException{
		if(validate(name, ip, port_num))
			return new Record(name, ip, port_num);
		else throw new InvalidRecordException();
	}
	
	public static Record from_client_info(ClientInfo nfo){
		return new Record(nfo.get_name(), nfo.get_ip(), nfo.get_port());
	}
	
	/**
	 * Factory method to create a record object representing an existing record
	 * @param name The name of the record
	 * @param ip The ip address of the creator
	 * @param port_num The port number of the creator
	 * @param timestamp Datetime representing the original creation of this record
	 * @return new Record object representing existing record in database
	 * @throws InvalidRecordException if invalid parameters...
	 */
	public static Record recreate_record(final String name, final String ip, final int port_num, final String timestamp)
	        throws InvalidRecordException{
	    if(validate(name, ip, port_num) && timestamp != null)
	        return new Record(name, ip, port_num, timestamp);
	    else throw new InvalidRecordException();
	}
	
	private Record(String name, String ip, int port_num){
		super(name, ip, port_num);
	}
	
	private Record(String name, String ip, int port_num, String timestamp){
	    super(name, ip, port_num, timestamp);
	}
	
	private Record(){
		super();
	}

	public int compareTo(Record record) {
		int compare_val = this.name.compareTo(record.get_name());
		
		if(compare_val != 0) 
			return compare_val;

		compare_val = this.ip_addr.compareTo(record.get_ip());
		return (compare_val != 0)? compare_val : 
		       (record.get_port() != this.get_port())? record.get_port() - this.port_num: 
		                                               this.timestamp.compareTo(record.get_timestamp());
	}


}
