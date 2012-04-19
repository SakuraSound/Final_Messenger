package msg;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="Error")
@XmlEnum
public enum Error{
    @XmlEnumValue("invalid query")
    INVALID_QUERY("This query is invalid. Please format appropriately.", "invalid query"),
    @XmlEnumValue("invalid record")
    INVALID_RECORD("An invalid record was sent/detected. Rejecting.", "invalid record"),
    @XmlEnumValue("overwrite error")
    OVERWRITE_ERROR("Similar record exists. Remove that before inserting this one.", "overwrite error"),
    @XmlEnumValue("communication error")
    COMMUNICATION_ERROR("Having problems communicating with recordstore", "communication error"),
    @XmlEnumValue("server side error")
    SERVER_SIDE_ERROR("Issues with the recordstore server.", "server side error"),
    @XmlEnumValue("closed db")
    CLOSED_DB("The database you are accessing is closed", "closed db"),
    @XmlEnumValue("record non existant")
    RECORD_NOT_FOUND("The record you are trying to delete was not found.", "record non existant"),
    @XmlEnumValue("unknown request")
    UNKNOWN_REQUEST("Unknown request received.", "unknown request"),
    @XmlEnumValue("linkage error")
    LINKAGE_ERROR("Unable to perform linkage...", "linkage error"),
    @XmlEnumValue("duplicate link")
    DUPLICATE_LINK_ERROR("Link already exists", "duplicate link"),
    @XmlEnumValue("user offline")
    USER_OFFLINE("The user does not seem to be online...", "user offline");
    
    
    private final String message;
    private final String value;
    
    public String get_message(){ return message; }
    public String get_value(){ return value; }
        
    
    Error(final String message, String value){
        this.message = message;
        this.value = value;
    }
    
    public static Error from_string(String value){
        for(Error e : Error.values()){
            if(e.get_value().equals(value)){
                return e;
            }
        }
        throw new IllegalArgumentException(value);
    }
}