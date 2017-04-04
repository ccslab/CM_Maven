package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.CMObject;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMEvent extends CMObject {
	
	protected String m_strHandlerSession;
	protected String m_strHandlerGroup;
	protected String m_strDistributionSession;
	protected String m_strDistributionGroup;
	protected int m_nID;
	protected int m_nByteNum;	// total number of bytes in the event
	ByteBuffer m_bytes;

	public CMEvent()
	{
		m_nType = CMInfo.CM_EVENT;
		m_nID = -1;
		m_strHandlerSession = "";
		m_strHandlerGroup = "";
		m_strDistributionSession = "";
		m_strDistributionGroup = "";
		m_nByteNum = -1;
		m_bytes = null;
	}
	
	public CMEvent(ByteBuffer msg)
	{
		m_nType = CMInfo.CM_EVENT;
		m_nID = -1;
		m_strHandlerSession = "";
		m_strHandlerGroup = "";
		m_strDistributionSession = "";
		m_strDistributionGroup = "";
		m_nByteNum = -1;
		m_bytes = null;
		
		unmarshallHeader(msg);
		unmarshallBody(msg);
	}

	// marshalling of an event
	public ByteBuffer marshall()
	{
		allocate();
		marshallHeader();
		marshallBody();
		return m_bytes;
	}
	
	// unmarshalling of an event
	public CMEvent unmarshall(ByteBuffer msg)
	{
		// should be implemented in sub-classes
		unmarshallHeader(msg);
		unmarshallBody(msg);
		return this;
	}

	// get/set methods
	public void setID(int id)
	{
		m_nID = id;
	}
	
	public int getID()
	{
		return m_nID;
	}

	public void setHandlerSession(String sName)
	{
		m_strHandlerSession = sName;
	}
	
	public void setHandlerGroup(String gName)
	{
		m_strHandlerGroup = gName;
	}
	
	public void setDistributionSession(String sName)
	{
		m_strDistributionSession = sName;
	}
	
	public void setDistributionGroup(String gName)
	{
		m_strDistributionGroup = gName;
	}

	public String getHandlerSession()
	{
		return m_strHandlerSession;
	}
	
	public String getHandlerGroup()
	{
		return m_strHandlerGroup;
	}
	
	public String getDistributionSession()
	{
		return m_strDistributionSession;
	}
	
	public String getDistributionGroup()
	{
		return m_strDistributionGroup;
	}
	
	/////////////////////////////////////////////////
	
	protected void allocate()
	{
		m_nByteNum = getByteNum();
		m_bytes = ByteBuffer.allocate(m_nByteNum);
		
		// this allocated object should be deallocated after the event is sent by a sending method.
	}
	
	protected void marshallHeader()
	{
		/*
		typedef struct _cmEvent {
			int byteNum;
			int type;
			unsigned int id;
			char handlerSession[EVENT_FIELD_LEN];
			char handlerRegion[EVENT_FIELD_LEN];
			char distributionSession[EVENT_FIELD_LEN];
			char distributionRegion[EVENT_FIELD_LEN];
			unsigned char body[1];
		} cmEvent;
		*/
		
		//if( !CMEndianness.isBigEndian() )
		//	m_bytes.order(ByteOrder.BIG_ENDIAN);
		
		m_bytes.putInt(m_nByteNum);
		m_bytes.putInt(m_nType);
		m_bytes.putInt(m_nID);
		m_bytes.putInt(m_strHandlerSession.getBytes().length);
		m_bytes.put(m_strHandlerSession.getBytes());
		m_bytes.putInt(m_strHandlerGroup.getBytes().length);
		m_bytes.put(m_strHandlerGroup.getBytes());
		m_bytes.putInt(m_strDistributionSession.getBytes().length);
		m_bytes.put(m_strDistributionSession.getBytes());
		m_bytes.putInt(m_strDistributionGroup.getBytes().length);
		m_bytes.put(m_strDistributionGroup.getBytes());
		//m_bytes.rewind();
		
		//if( !CMEndianness.isBigEndian() )
		//	m_bytes.order(ByteOrder.LITTLE_ENDIAN);

	}
	
	protected void unmarshallHeader(ByteBuffer msg)
	{
		int nStrNum;
		
		/*
		typedef struct _cmEvent {
			int byteNum;
			int type;
			unsigned int id;
			char handlerSession[EVENT_FIELD_LEN];
			char handlerRegion[EVENT_FIELD_LEN];
			char distributionSession[EVENT_FIELD_LEN];
			char distributionRegion[EVENT_FIELD_LEN];
			unsigned char body[1];
		} cmEvent;
		*/

		// add endian test
		
		m_nByteNum = msg.getInt();
		m_nType = msg.getInt();
		m_nID = msg.getInt();
		
		nStrNum = msg.getInt();
		byte[] strBytes = new byte[nStrNum];
		msg.get(strBytes);
		m_strHandlerSession = new String(strBytes);
		
		nStrNum = msg.getInt();
		strBytes = new byte[nStrNum];
		msg.get(strBytes);
		m_strHandlerGroup = new String(strBytes);
		
		nStrNum = msg.getInt();
		strBytes = new byte[nStrNum];
		msg.get(strBytes);
		m_strDistributionSession = new String(strBytes);
		
		nStrNum = msg.getInt();
		strBytes = new byte[nStrNum];
		msg.get(strBytes);
		m_strDistributionGroup = new String(strBytes);
		
		//msg.rewind();
		
		/*
		if( !CMEndianness.isBigEndian() )
		{
			msg.order(ByteOrder.BIG_ENDIAN);
			msg.put(msg);
			msg.rewind();
		}
		*/

	}
	
	protected void marshallBody()
	{
		m_bytes.clear();
		// should be implemented in sub-classes
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		msg.clear();
		// should be implemented in sub-classes
	}

	protected void setByteNum(int nByteNum)
	{
		m_nByteNum = nByteNum;
	}
	
	protected int getByteNum()
	{
		// can be re-implemented by sub-class
		int nSize;
		nSize = (Integer.BYTES)*7 + m_strHandlerSession.getBytes().length + m_strHandlerGroup.getBytes().length
				+ m_strDistributionSession.getBytes().length + m_strDistributionGroup.getBytes().length;
		return nSize;
	}
	
	protected String getStringFromByteBuffer(ByteBuffer msg)
	{
		int nStrNum;
		byte[] strBytes;
		
		nStrNum = msg.getInt();
		strBytes = new byte[nStrNum];
		msg.get(strBytes);
		
		return new String(strBytes);
	}
	
}
