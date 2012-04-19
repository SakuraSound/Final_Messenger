package net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.xml.bind.JAXBException;

import msg.Message;

public class SpecialSocket {

    private static final int DEFAULT_BITS = 4096;
    
    private final DatagramSocket dg_socket;
    
    
    /**
     * 
     * @param data
     * @param addr
     * @param port_num
     * @throws IOException
     * @throws JAXBException
     */
    public void send(final Message data, final InetAddress addr, final int port_num) throws IOException, JAXBException{
        byte[] buffer = data.to_bytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addr, port_num);
        dg_socket.send(packet);
    }
    
    /**
     * checks to see if socket is closed
     * @return
     */
    public boolean is_closed(){
        return dg_socket.isClosed();
    }
    
    /**
     * Close this special socket
     */
    public void close(){
        dg_socket.close();
    }
    
    /**
     * Get the inet address associated with this socket
     * @return
     */
    public InetAddress get_inet(){
        return dg_socket.getLocalAddress();
    }
    
    /**
     * Gets the port associated with this socket
     * @return
     */
    public int get_port(){
        return dg_socket.getLocalPort();
    }
    
    /**
     * 
     * @return
     * @throws IOException
     */
    public DatagramPacket accept() throws IOException{
        return accept(DEFAULT_BITS, 0);
    }
    
    public DatagramPacket non_blocking_accept(){
        try{
            return accept(DEFAULT_BITS, 50);
        }catch(IOException ste){
            return null;
         }
    }
    
    /**
     * 
     * @param bytes
     * @return
     * @throws IOException
     */
    public DatagramPacket accept(int bytes) throws IOException{
        return accept(bytes, 0);
    }
    
    public DatagramPacket accept(int bits, int timeout_ms) throws IOException {
        byte[] buffer = new byte[bits];
        DatagramPacket packet = new DatagramPacket(buffer, bits);
        dg_socket.setSoTimeout(timeout_ms);
        dg_socket.receive(packet);
        dg_socket.setSoTimeout(0);
        return packet;
    }
    
    /**
     * 
     * @param port_num
     * @return
     * @throws SocketException
     */
    public static final SpecialSocket create_socket(int port_num) throws SocketException{
        return new SpecialSocket(port_num);
    }
    
    public static final SpecialSocket create_socket() throws SocketException{
        return new SpecialSocket();
    }
    
    private SpecialSocket() throws SocketException{
        this.dg_socket = new DatagramSocket();
    }
    
    private SpecialSocket(int port_num) throws SocketException{
        this.dg_socket = new DatagramSocket(port_num);
        
    }
    
}
