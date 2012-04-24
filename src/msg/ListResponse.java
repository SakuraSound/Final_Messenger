package msg;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import data.Record;
import data.ServerInfo;

@XmlRootElement(name="ReadListMessage")
@XmlSeeAlso(value={Record.class, ServerInfo.class})
public class ListResponse<T> extends GenericResponse {
    
    @XmlElement(name="records")
    private List<T> record_list;
    @XmlElement(name="num_recs")
    private int num_recs;
    
    
    public List<T> get_records(){ return record_list; }
    public int get_numrecs(){ return num_recs; }
    
    
    public static <T> ListResponse<T> create_message(List<T> records){
        return new ListResponse<T>(records);
    }

    private ListResponse(List<T> records){
    	super("List of records received.");
        this.record_list = records;
        this.num_recs = records.size();
    }
    
    private ListResponse(){}
}
