package utils;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public final class UtilityBelt {
	public static final String get_timestamp(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        return formatter.format(new Date());
    }
	
	
	/**
     * Converts a record object into xml representation
     * @param record the record we want to convert
     * @return xml representation of our record object
     * @throws JAXBException if corruption of object
     */
	public static final byte[] java_2_bytes(final Object xmlable, Class<?> clazz) throws JAXBException{
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(xmlable, writer);
        return writer.toString().getBytes();
    }
	
	
	/**
     * Converts the received xml data to a Record object
     * @param xml the sent schema (with information)
     * @return new Record object
     * @throws JAXBException if mangled xml data received.
     */
    public static final Object bytes_2_java(final byte[] bytes, Class<?> clazz) throws JAXBException, ClassCastException{
        //Trim the trailing zeros from the byte array
        String xml = new String(bytes).trim();
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller u = context.createUnmarshaller();
        return clazz.cast(u.unmarshal(new ByteArrayInputStream(xml.getBytes())));
    }
}
