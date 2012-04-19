package data;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import utils.UtilityBelt;

@XmlRootElement(name="SearchQuery")
public class SearchQuery{
    @XmlElement(name="search_query")
    private  String name;
    @XmlElement(name="ip")
    private  String ip;
    @XmlElement(name="port_num")
    private int port_num;
    @XmlAttribute(name="timestamp")
    private  String timestamp;
    
    
    // *** ACCESSOR METHODS ***
    public String get_name(){ return name; }
    public String get_ip(){ return ip; }
    public int get_port(){ return port_num; }
    public String get_timestamp(){ return timestamp; }
    
   
    // ** STATIC FACTORY METHODS ***
    public static SearchQuery make_retrieve_query(String name, String ip_search){
        return make_query(name, ip_search, 0, null);
    }
    
    public static SearchQuery make_delete_query(String name_search, String ip_search, int port_num){
        return make_query(name_search, ip_search, port_num, null);
    }
    
    private static SearchQuery make_query(String name, String ip, int port_num, String time_search){
        //FIXME: Currently no support for time-range queries...
        return new SearchQuery(name, ip, port_num, null);
    }
    
    private SearchQuery(String name_search, String ip_search, int port_num, String time_search){
        this.name = name_search;
        this.ip = ip_search;
        this.port_num = port_num;
        timestamp = UtilityBelt.get_timestamp();
    }
    
    
    
    private SearchQuery(){}
    
}
