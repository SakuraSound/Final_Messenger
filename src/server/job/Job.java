package server.job;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum Job {
	@XmlEnumValue("TEST") TEST,
	@XmlEnumValue("READ")  READ,
	@XmlEnumValue("WRITE")  WRITE,
	@XmlEnumValue("DELETE")  DELETE,
	@XmlEnumValue("REGISTER")  REGISTER,
	@XmlEnumValue("UNREGISTER")  UNREGISTER,
	@XmlEnumValue("CLIENT_SEARCH")  CLIENT_SEARCH,
	@XmlEnumValue("LINK")  LINK,
	@XmlEnumValue("UNLINK")  UNLINK,
	@XmlEnumValue("VIEW_LINK") VIEW_LINK,
	@XmlEnumValue("SERVER_LINK") SERVER_LINK,
	@XmlEnumValue("SERVER_UNLINK") SERVER_UNLINK,
	@XmlEnumValue("VIEW_FORWARDING")  VIEW_FORWARDING,
	@XmlEnumValue("SHUT_DOWN")  SHUT_DOWN,
	@XmlEnumValue("SEND_MESSAGE")  SEND_MESSAGE,
	@XmlEnumValue("UPDATE_REGISTER") UPDATE_REGISTER,
	@XmlEnumValue("UPDATE_UNREGISTER") UPDATE_UNREGISTER,
	@XmlEnumValue("INITIAL_REGISTRATION_LOAD") INITIAL_REGISTRATION_LOAD,
	@XmlEnumValue("LINK_STATE_PULSE") LINK_STATE_PULSE;
}
