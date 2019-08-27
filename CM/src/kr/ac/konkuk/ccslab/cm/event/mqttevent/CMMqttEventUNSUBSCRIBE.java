package kr.ac.konkuk.ccslab.cm.event.mqttevent;

import java.nio.ByteBuffer;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttTopicQoS;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents a CM event that the variable header and payload of 
 * MQTT UNSUBSCRIBE packet.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttEventUNSUBSCRIBE extends CMMqttEventFixedHeader {

	//////////////////////////////////////////////////
	// member variables (variable header)
	int m_nPacketID;	// 2 bytes

	//////////////////////////////////////////////////
	// member variables (payload)
	CMList<String> m_topicList; 
	
	//////////////////////////////////////////////////
	// constructors

	public CMMqttEventUNSUBSCRIBE()
	{
		// initialize CM event ID
		m_nID = CMMqttEvent.UNSUBSCRIBE;
		// initialize fixed header
		m_packetType = CMMqttEvent.UNSUBSCRIBE;
		m_flag = 2;
		// initialize variable header
		m_nPacketID = -1;
		// initialize payload
		m_topicList = new CMList<String>();
	}
	
	public CMMqttEventUNSUBSCRIBE(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}

	//////////////////////////////////////////////////
	// setter/getter (variable header)

	/**
	 * sets MQTT packet ID.
	 * @param nID - packet ID.
	 */
	public void setPacketID(int nID)
	{
		m_nPacketID = nID;
	}
	
	/**
	 * gets MQTT packet ID.
	 * @return packet ID.
	 */
	@Override
	public int getPacketID()
	{
		return m_nPacketID;
	}
	
	//////////////////////////////////////////////////
	// overridden methods (variable header)
	
	@Override
	protected int getVarHeaderByteNum()
	{
		return 2;	// packet ID
	}

	@Override
	protected void marshallVarHeader()
	{
		putInt2BytesToByteBuffer(m_nPacketID);
	}

	@Override
	protected void unmarshallVarHeader(ByteBuffer buf)
	{
		m_nPacketID = getInt2BytesFromByteBuffer(buf);
	}

	//////////////////////////////////////////////////
	// setter/getter (payload)
	
	public void setTopicList(CMList<String> topicList)
	{
		if(topicList != null)
			m_topicList = topicList;
	}
	
	public CMList<String> getTopicList()
	{
		return m_topicList;
	}
	
	public boolean addTopic(String strTopic)
	{
		return m_topicList.addElement(strTopic);
	}
	
	public boolean removeTopic(String strTopic)
	{
		return m_topicList.removeElement(strTopic);
	}
	
	public void removeAllTopic()
	{
		m_topicList.removeAllElements();
	}
	
	//////////////////////////////////////////////////
	// overridden methods (payload)

	@Override
	protected int getPayloadByteNum()
	{
		int nByteNum = 0;
		for(String strTopic : m_topicList.getList())
		{
			nByteNum += CMInfo.STRING_LEN_BYTES_LEN + strTopic.getBytes().length;
		}
		return nByteNum;
	}

	@Override
	protected void marshallPayload()
	{
		for(String strTopic : m_topicList.getList())
		{
			putStringToByteBuffer(strTopic);
		}
	}

	@Override
	protected void unmarshallPayload(ByteBuffer buf)
	{
		m_topicList.removeAllElements();
		String strTopic = null;
		while(buf.hasRemaining())
		{
			strTopic = getStringFromByteBuffer(buf);
			m_topicList.addElement(strTopic);
		}
	}
	
	//////////////////////////////////////////////////
	// overridden methods

	@Override
	public String toString()
	{
		StringBuffer strBuf = new StringBuffer();
		strBuf.append("CMMqttEventUNSUBSCRIBE {");
		strBuf.append(super.toString()+", ");
		strBuf.append("\"packetID\": "+m_nPacketID+"}");
		strBuf.append("{ \"topic\": [");
		
		int nCount = 0;
		for(String strTopic : m_topicList.getList())
		{
			nCount++;
			strBuf.append("\""+strTopic+"\"");
			if(nCount < m_topicList.getSize())
				strBuf.append(", ");
		}
		
		strBuf.append("]");
		strBuf.append("}");

		return strBuf.toString();
	}

}
