package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMDummyEvent extends CMEvent{
	
	private String m_strDummyInfo;
	
	public CMDummyEvent()
	{
		m_nType = CMInfo.CM_DUMMY_EVENT;
		m_strDummyInfo = "";
	}
	
	public CMDummyEvent(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
	
	// set/get methods
	public void setDummyInfo(String info)
	{
		m_strDummyInfo = info;
	}
	
	public String getDummyInfo()
	{
		return m_strDummyInfo;
	}
	
	/////////////////////////////////////////////////////
	
	protected int getByteNum()
	{
		/*
		typedef struct _dummyEvent{
			char dummyInfo[DUMMY_LEN];
		} dummyEvent;
		*/
		
		int nByteNum = 0;
		nByteNum = super.getByteNum(); // get header length
		
		nByteNum += Integer.BYTES + m_strDummyInfo.getBytes().length; // get body length
		
		return nByteNum;
	}
	
	protected void marshallBody()
	{
		m_bytes.putInt(m_strDummyInfo.getBytes().length);
		m_bytes.put(m_strDummyInfo.getBytes());
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		m_strDummyInfo = getStringFromByteBuffer(msg);
	}
}
