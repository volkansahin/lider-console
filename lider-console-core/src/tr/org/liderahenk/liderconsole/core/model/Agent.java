package tr.org.liderahenk.liderconsole.core.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Agent implements Serializable {

	private static final long serialVersionUID = 2717258293731093402L;

	private Long id;

	private String jid; // XMPP JID = LDAP UID

	private Boolean deleted;

	private String dn;

	private String password;

	private String hostname;

	private String ipAddresses; // Comma-separated IP addresses

	private String macAddresses; // Comma-separated MAC addresses

	private Date createDate;

	private Date modifyDate;

	private List<AgentProperty> properties;

	private List<UserSession> sessions;

	public Agent() {
	}

	public Agent(Long id, String jid, Boolean deleted, String dn, String password, String hostname, String ipAddresses,
			String macAddresses, Date createDate, Date modifyDate, List<AgentProperty> properties,
			List<UserSession> sessions) {
		this.id = id;
		this.jid = jid;
		this.deleted = deleted;
		this.dn = dn;
		this.password = password;
		this.hostname = hostname;
		this.ipAddresses = ipAddresses;
		this.macAddresses = macAddresses;
		this.createDate = createDate;
		this.modifyDate = modifyDate;
		this.properties = properties;
		this.sessions = sessions;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public String getDn() {
		return dn;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getIpAddresses() {
		return ipAddresses;
	}

	public void setIpAddresses(String ipAddresses) {
		this.ipAddresses = ipAddresses;
	}

	public String getMacAddresses() {
		return macAddresses;
	}

	public void setMacAddresses(String macAddresses) {
		this.macAddresses = macAddresses;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public List<AgentProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<AgentProperty> properties) {
		this.properties = properties;
	}

	public List<UserSession> getSessions() {
		return sessions;
	}

	public void setSessions(List<UserSession> sessions) {
		this.sessions = sessions;
	}

}