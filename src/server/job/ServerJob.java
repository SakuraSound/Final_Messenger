package server.job;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerJob implements Comparable<ServerJob>{

	private long timestamp;
	private String ip_addr;
	private InetAddress inet;
	private int port_num;
	private Job job;
	private byte[] data;
	
	
	public final long get_timestamp(){ return timestamp; }
	public InetAddress get_inet(){ return inet; }
	public int get_port(){ return port_num; }
	public Job get_job(){ return job; }
	public byte[] get_data(){ return data; }
	public String get_ip(){ return ip_addr; }
	
	
	
	public int compareTo(ServerJob job) {
		/* answer = sign(time_diff - 150*priority_diff)
		 * If job is younger, than we already start with a negative number
		 * then we subtract the priority difference * 150ms and take the sign
		 */
		return (int) ( this.timestamp- job.get_timestamp());
	}

	private ServerJob(InetAddress inet, int port_num, Job type, byte[] data) throws UnknownHostException{
		this.timestamp = System.currentTimeMillis();
		this.inet = inet;
		this.ip_addr = inet.getHostAddress();
		this.data = data;
		this.job = type;
		this.port_num = port_num;
	}
	
	public static ServerJob make_job(InetAddress inet, int port_num, Job type, byte[] data) throws UnknownHostException{
		return new ServerJob(inet, port_num, type, data);
	}
	
}
