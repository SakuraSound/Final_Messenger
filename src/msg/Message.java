package msg;

import javax.xml.bind.JAXBException;


//TODO: maybe convert this to an abstract class instead....
public interface Message{
    public abstract String get_timestamp();

	public abstract byte[] to_bytes() throws JAXBException;
}
