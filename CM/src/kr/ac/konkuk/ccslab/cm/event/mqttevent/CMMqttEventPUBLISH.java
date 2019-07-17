package kr.ac.konkuk.ccslab.cm.event.mqttevent;

import java.nio.ByteBuffer;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents a CM event that the variable header and payload of 
 * MQTT PUBLISH packet.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttEventPUBLISH extends CMMqttEventFixedHeader {

	//////////////////////////////////////////////////
	// member variables (fixed header)
	boolean m_bDupFlag;	// 1 bit of m_flag of CMMqttEventFixedHeader
	byte m_qos;			// 2 bits of m_flag of CMMqttEventFixedHeader
	boolean m_bRetainFlag;	// 1 bit of m_flag of CMMqttEventFixedHeader
	
	//////////////////////////////////////////////////
	// member variables (variable header)
	String m_strTopicName;
	// int m_nPacketID that is declared in CMMqttEvent is used in this packet.
	
	//////////////////////////////////////////////////
	// member variables (variable header)
	String m_strAppMessage;
	
	//////////////////////////////////////////////////
	// constructors

	public CMMqttEventPUBLISH()
	{
		// initialize CM event ID
		m_nID = CMMqttEvent.PUBLISH;	// 3
		// initialize fixed header
		m_packetType = CMMqttEvent.PUBLISH;	// 3
		m_flag = 0;	// no flag (dup flag, qos, retain flag)
		m_bDupFlag = false;
		m_qos = 0;
		m_bRetainFlag = false;
		
		// initialize varaible header
		m_strTopicName = "";
		// initialize payload
		m_strAppMessage = "";
	}
	
	public CMMqttEventPUBLISH(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
	
	//////////////////////////////////////////////////
	// setter/getter (fixed header)
	
	public void setDupFlag(boolean bFlag)
	{
		// print current m_flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventPUBLISH.setDupFlag(): ");
			System.out.println("packet type + flag (before): "+getBinaryStringOfByte(m_flag));
		}
		
		// set m_flag
		if(bFlag)
			m_flag |= 0x08;	// 0b0000 1000
		else
			m_flag &= 0xf7;	// 0b1111 0111
		
		// print modified m_flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("packet type + flag (after): "+getBinaryStringOfByte(m_flag));
		}
	}
	
	public boolean isDupFlag()
	{
		if((m_flag & 0x08) == 0) return false;
		else return true;
	}

	public void setQoS(byte qos)
	{
		// print current m_flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventPUBLISH.setQoS(): ");
			System.out.println("packet type + flag (before): "+getBinaryStringOfByte(m_flag));
		}
		
		// set m_flag
		m_flag &= 0xf9;	// initialize qos bits (0b1111 1001)
		m_flag |= qos << 1;
		
		// print modified m_flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("packet type + flag (after): "+getBinaryStringOfByte(m_flag));
		}
	}
	
	public byte getQoS()
	{
		byte qos = 0;
		qos = (byte)((m_flag & 0x06) >> 1);	// (0b0000 0110)
		return qos;
	}
	
	public void setRetainFlag(boolean bFlag)
	{
		// print current m_flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventPUBLISH().setRetainFlag(): ");
			System.out.println("packet type + flag (before): "+getBinaryStringOfByte(m_flag));
		}
		
		// set m_flag
		if(bFlag)
			m_flag |= 0x01;	// 0b0000 0001
		else
			m_flag &= 0xfe;	// 0b1111 1110
		
		// print modified m_flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("packet type + flag (after): "+getBinaryStringOfByte(m_flag));
		}
	}
	
	public boolean isRetainFlag()
	{
		if((m_flag & 0x01) == 0) return false;
		else return true;
	}
	
	//////////////////////////////////////////////////
	// setter/getter (variable header)

	public void setTopicName(String strTopic)
	{
		m_strTopicName = strTopic;
	}
	
	public String getTopicName()
	{
		return m_strTopicName;
	}
		
	//////////////////////////////////////////////////
	// overridden methods (variable header)
	
	@Override
	protected int getVarHeaderByteNum()
	{
		int nByteNum = 0;
		nByteNum += 2 + m_strTopicName.getBytes().length;	// topic name
		nByteNum += 2;	// packet identifier

		return nByteNum;
	}

	@Override
	protected void marshallVarHeader()
	{
		putStringToByteBuffer(m_strTopicName);
		putInt2BytesToByteBuffer(m_nPacketID);
	}

	@Override
	protected void unmarshallVarHeader(ByteBuffer buf)
	{
		m_strTopicName = getStringFromByteBuffer(buf);
		m_nPacketID = getInt2BytesFromByteBuffer(buf);
	}

	//////////////////////////////////////////////////
	// setter/getter (payload)

	public void setAppMessage(String strMsg)
	{
		m_strAppMessage = strMsg;
	}
	
	public String getAppMessage()
	{
		return m_strAppMessage;
	}

	//////////////////////////////////////////////////
	// overridden methods (payload)

	@Override
	protected int getPayloadByteNum()
	{
		int nByteNum = m_strAppMessage.getBytes().length;	// app message
		// this byte number does not contain the length of the string 
		// because the length will be calculated separately.
		return nByteNum;
	}

	@Override
	protected void marshallPayload()
	{
		// The string length will be calculated separately.
		m_bytes.put(m_strAppMessage.getBytes());
	}

	@Override
	protected void unmarshallPayload(ByteBuffer buf)
	{
		int nAppMsgLength = m_nRemainingLength - getVarHeaderByteNum();
		// m_nRemainingLength is determined after unmarshallFixedHeader() 
		byte[] appMsgBytes = new byte[nAppMsgLength];
		buf.get(appMsgBytes);
		m_strAppMessage = new String(appMsgBytes);
	}

	//////////////////////////////////////////////////
	// overridden methods

	@Override
	public String toString()
	{
		StringBuffer strBufVarHeader = new StringBuffer();
		strBufVarHeader.append("CMMqttEventPUBLISH {");
		strBufVarHeader.append(super.toString()+", ");
		strBufVarHeader.append("\"topicName\": \""+m_strTopicName+"\", ");
		strBufVarHeader.append("\"packetID\": "+m_nPacketID);
		strBufVarHeader.append("}");
		
		StringBuffer strBufPayload = new StringBuffer();
		strBufPayload.append(strBufVarHeader);
		strBufPayload.append("{");
		strBufPayload.append("\"appMessage\": \""+m_strAppMessage+"\"");
		strBufPayload.append("}");
		
		return strBufPayload.toString();
	}
}
