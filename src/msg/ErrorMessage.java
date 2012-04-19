package msg;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import utils.UtilityBelt;

/**
 * The Error message
 * Thrown when issues arise on the server or client.
 * @author Hatomi
 *
 */
@XmlRootElement(name="ErrorMessage")
public class ErrorMessage implements Comparable<ErrorMessage>, Message{
	@XmlAttribute(name="timestamp")
	private String timestamp;
	
    @XmlElement(name="Error_type")
    private Error Error_value;

    
    public Error get_Error(){ return Error_value; }
    public String get_timestamp(){ return timestamp; }
    
    public static ErrorMessage create_message(Error Error){
        return new ErrorMessage(Error);
    }
    
    private ErrorMessage(Error Error){
        this.Error_value = Error;
    }
    
    
    private ErrorMessage(){}
    
    public int compareTo(ErrorMessage msg){
        return timestamp.compareTo(msg.get_timestamp());
    }
    
    public boolean equals(ErrorMessage msg){
        return (this.Error_value == msg.get_Error()) &&
                (this.timestamp.equals(msg.get_timestamp()));
    }
    
	public byte[] to_bytes() throws JAXBException {
		// TODO Auto-generated method stub
		return UtilityBelt.java_2_bytes(this, getClass());
	}
    
}
