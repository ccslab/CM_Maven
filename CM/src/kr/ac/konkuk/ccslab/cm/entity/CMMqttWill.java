package kr.ac.konkuk.ccslab.cm.entity;

/**
 * The CMMqttWill class represents information about MQTT will message.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttWill {

	private String m_strWillMessage;
	private String m_strWillTopic;
	private byte m_willQoS;
	private boolean m_bWillRetain;
	
	public CMMqttWill()
	{
		m_strWillMessage = "";
		m_strWillTopic = "";
		m_willQoS = 0;
		m_bWillRetain = false;
	}
	
	public CMMqttWill(String strMsg, String strTopic, byte qos, boolean bRetain)
	{
		m_strWillMessage = strMsg;
		m_strWillTopic = strTopic;
		m_willQoS = qos;
		m_bWillRetain = bRetain;
	}
	
	// setter/getter
	public void setWillMessage(String strMsg)
	{
		m_strWillMessage = strMsg;
	}
	
	public String getWillMessage()
	{
		return m_strWillMessage;
	}
	
	public void setWillTopic(String strTopic)
	{
		m_strWillTopic = strTopic;
	}
	
	public String getWillTopic()
	{
		return m_strWillTopic;
	}
	
	public void setWillQoS(byte qos)
	{
		m_willQoS = qos;
	}
	
	public byte getWillQoS()
	{
		return m_willQoS;
	}
	
	public void setWillRetain(boolean bRetain)
	{
		m_bWillRetain = bRetain;
	}
	
	public boolean isWillRetain()
	{
		return m_bWillRetain;
	}
	
	// overridden methods
	@Override
	public String toString()
	{
		String str = "CMMqttWill{ \"willMessage\": "+m_strWillMessage+", \"willTopic\": "+m_strWillTopic
				+", \"willQoS\": "+m_willQoS+", \"willRetain\": "+m_bWillRetain+"}";
		return str;
	}
}
