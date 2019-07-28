package kr.ac.konkuk.ccslab.cm.entity;

/**
 * The CMMqttTopicQoS class represents a pair of a topic fileter and a requested QoS, 
 * and it is used in the topic subscription process of the MQTT protocol.
 * 
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttTopicQoS {

	private String m_strTopic;
	private byte m_qos;
	
	public CMMqttTopicQoS()
	{
		m_strTopic = "";
		m_qos = 0;
	}
	
	public CMMqttTopicQoS(String strTopic, byte qos)
	{
		m_strTopic = strTopic;
		m_qos = qos;
	}
	
	public void setTopic(String strTopic)
	{
		m_strTopic = strTopic;
	}
	
	public String getTopic()
	{
		return m_strTopic;
	}
	
	public void setQoS(byte qos)
	{
		m_qos = qos;
	}
	
	public byte getQoS()
	{
		return m_qos;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null) return false;
		if(!this.getClass().equals(obj.getClass())) return false;
		if(this == obj) return true;
		
		CMMqttTopicQoS topicQoS = (CMMqttTopicQoS) obj;
		if(m_strTopic.equals(topicQoS.getTopic()))
			return true;
		else
			return false;
	}
	
	@Override
	public String toString()
	{
		String strTopicQoS = "CMMqttTopicQoS{ \"topic\": \""+m_strTopic+"\", \"qos\": "+m_qos+" }";
		return strTopicQoS;
	}
}
