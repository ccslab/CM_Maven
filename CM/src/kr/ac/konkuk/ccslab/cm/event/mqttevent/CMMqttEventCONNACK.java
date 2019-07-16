package kr.ac.konkuk.ccslab.cm.event.mqttevent;

import java.nio.ByteBuffer;

/**
 * This class represents a CM event that the variable header and payload of 
 * MQTT CONNACK packet.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttEventCONNACK extends CMMqttEventFixedHeader {

	//////////////////////////////////////////////////
	// member variables (variable header)
	byte m_connAckFlag;	// 1 byte, 0000000X
	byte m_connReturnCode;	// 1 byte, 0 ~ 5
	
	//////////////////////////////////////////////////
	// constructors

	public CMMqttEventCONNACK()
	{
		// initialize CM event ID
		m_nID = CMMqttEvent.CONNACK;	// 2
		// initialize fixed header
		m_packetType = CMMqttEvent.CONNACK;	// 2
		m_flag = 0;
		
		// initialize variable header
		m_connAckFlag = 0;
		m_connReturnCode = 0;
	}
	
	public CMMqttEventCONNACK(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
	
	//////////////////////////////////////////////////
	// setter/getter (variable header)

	public void setVarHeader(boolean bConnAckFlag, byte returnCode)
	{
		if(bConnAckFlag) m_connAckFlag = 0x01;
		else m_connAckFlag = 0x00;
		
		m_connReturnCode = returnCode;
	}
	
	public void setConnAckFlag(boolean bFlag)
	{
		if(bFlag) m_connAckFlag = 0x01;
		else m_connAckFlag = 0x00;
	}
	
	public boolean isConnAckFlag()
	{
		if((m_connAckFlag & 0x01) == 0) return false;
		else return true;
	}
	
	public void setReturnCode(byte code)
	{
		m_connReturnCode = code;
	}
	
	public byte getReturnCode()
	{
		return m_connReturnCode;
	}
	
	//////////////////////////////////////////////////
	// overridden methods (variable header)
	
	@Override
	protected int getVarHeaderByteNum()
	{
		int nByteNum = 2;	// conn ack flag (1 byte) + return code (1 byte)
		return nByteNum;
	}

	@Override
	protected void marshallVarHeader()
	{
		m_bytes.put(m_connAckFlag);
		m_bytes.put(m_connReturnCode);
	}

	@Override
	protected void unmarshallVarHeader(ByteBuffer buf)
	{
		m_connAckFlag = buf.get();
		m_connReturnCode = buf.get();
	}

	//////////////////////////////////////////////////
	// overridden methods (payload)

	// The CONNACK packet has no payload.
	@Override
	protected int getPayloadByteNum()
	{
		return 0;
	}

	@Override
	protected void marshallPayload(){}

	@Override
	protected void unmarshallPayload(ByteBuffer buf){}
	
	//////////////////////////////////////////////////
	// overridden methods

	@Override
	public String toString()
	{
		StringBuffer strBufVarHeader = new StringBuffer();
		strBufVarHeader.append("CMMqttEventCONNACK {");
		strBufVarHeader.append(super.toString()+", ");
		strBufVarHeader.append("\"connAckFlag\": "+m_connAckFlag+", ");
		strBufVarHeader.append("\"connReturnCode\": "+m_connReturnCode);
		strBufVarHeader.append("}");
		return strBufVarHeader.toString();
	}

}
