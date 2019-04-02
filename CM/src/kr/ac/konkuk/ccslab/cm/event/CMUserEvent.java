package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMUserEvent extends CMEvent {
	private String m_strID;	// instead of int ID inherited from CMEvent, use this for this event ID
	private Vector<CMUserEventField> m_eventFieldList;

	public CMUserEvent()
	{
		m_nType = CMInfo.CM_USER_EVENT;
		m_strID = "defaultID";
		m_eventFieldList = new Vector<CMUserEventField>();
	}
	
	public CMUserEvent(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}

	// set/get methods
	public Vector<CMUserEventField> getAllEventFields()
	{
		return m_eventFieldList;
	}
	
	public void setStringID(String strID)
	{
		m_strID = strID;
	}
	
	public String getStringID()
	{
		return m_strID;
	}
	
	// CM_INT, CM_LONG, CM_FLOAT, CM_DOUBLE, CM_STR values are set as String.
	public void setEventField(int nDataType, String strFieldName, String strFieldValue)
	{
		CMUserEventField uef = null;
		uef = findEventField(nDataType, strFieldName);
		if(uef == null)
		{
			uef = new CMUserEventField();
			uef.nDataType = nDataType;
			uef.strFieldName = strFieldName;
			uef.strFieldValue = strFieldValue;
			m_eventFieldList.addElement(uef);
			return;
		}
		uef.strFieldValue = strFieldValue;

		return;
	}
	
	// CM_BYTES sets nValueByteNum and valueBytes byte array
	public void setEventBytesField(String strFieldName, int nByteNum, byte[] bytes)
	{
		CMUserEventField uef = null;
		uef = findEventField(CMInfo.CM_BYTES, strFieldName);
		if(uef == null)
		{
			uef = new CMUserEventField();
			uef.nDataType = CMInfo.CM_BYTES;
			uef.strFieldName = strFieldName;
			uef.nValueByteNum = nByteNum;
			uef.valueBytes = new byte[nByteNum];
			uef.valueBytes = Arrays.copyOf(bytes, nByteNum);
			m_eventFieldList.addElement(uef);
			return;
		}
		uef.nValueByteNum = nByteNum;
		uef.valueBytes = new byte[nByteNum];
		uef.valueBytes = Arrays.copyOf(bytes, nByteNum);
		return;
	}

	// for CM_INT, CM_LONG, CM_FLOAT, CM_DOUBLE, CM_STR
	public String getEventField(int nDataType, String strFieldName)
	{
		CMUserEventField uef = null;
		uef = findEventField(nDataType, strFieldName);
		if(uef == null) return null;
		
		return uef.strFieldValue;
	}
	
	// for CM_BYTES
	public byte[] getEventBytesField(String strFieldName)
	{
		CMUserEventField uef = null;
		uef = findEventField(CMInfo.CM_BYTES, strFieldName);
		if(uef == null) return null;
		
		return uef.valueBytes;
	}
	
	public CMUserEventField findEventField(int nDataType, String strFieldName)
	{
		CMUserEventField uef = null;
		boolean bFound = false;
		Iterator<CMUserEventField> iterEventFieldList = m_eventFieldList.iterator();
		
		while(iterEventFieldList.hasNext() && !bFound)
		{
			uef = iterEventFieldList.next();
			if( nDataType == uef.nDataType && strFieldName.equals(uef.strFieldName) )
				bFound = true;
		}
		
		if(!bFound) return null;
		
		return uef;
	}
	
	public void removeEventField(int nDataType, String strFieldName)
	{
		CMUserEventField uef = null;
		boolean bFound = false;
		Iterator<CMUserEventField> iterEventFieldList = m_eventFieldList.iterator();
		
		while(iterEventFieldList.hasNext() && !bFound)
		{
			uef = iterEventFieldList.next();
			if( nDataType == uef.nDataType && strFieldName.equals(uef.strFieldName) )
			{
				if(nDataType == CMInfo.CM_BYTES)
					uef.valueBytes = null;
				iterEventFieldList.remove();
				bFound = true;
			}
		}
		
		return;
	}
	
	public void removeAllEventFields()
	{
		CMUserEventField uef = null;
		Iterator<CMUserEventField> iterEventFieldList = m_eventFieldList.iterator();
		
		while(iterEventFieldList.hasNext())
		{
			uef = iterEventFieldList.next();
			if( uef.nDataType == CMInfo.CM_BYTES )
			{
				uef.valueBytes = null;
			}
		}

		m_eventFieldList.removeAllElements();
		return;
	}
	
	///////////////////////////////////////////////////////
	
	protected int getByteNum()
	{
		int nByteNum = 0;
		CMUserEventField uef = null;
		Iterator<CMUserEventField> iterEventFieldList = m_eventFieldList.iterator();
		
		// header byte num
		nByteNum = super.getByteNum();
		
		// body byte num
		nByteNum += Integer.BYTES + m_strID.getBytes().length;
		while(iterEventFieldList.hasNext())
		{
			uef = iterEventFieldList.next();
			nByteNum += 3*Integer.BYTES; // nDataType, field name len, field value len
			if(uef.nDataType == CMInfo.CM_BYTES)
				nByteNum += uef.strFieldName.getBytes().length + uef.nValueByteNum;
			else
				nByteNum += uef.strFieldName.getBytes().length + uef.strFieldValue.getBytes().length;
		}
		
		return nByteNum;
	}
	
	protected void marshallBody()
	{
		CMUserEventField uef = null;
		Iterator<CMUserEventField> iterEventFieldList = m_eventFieldList.iterator();
		
		m_bytes.putInt(m_strID.getBytes().length);
		m_bytes.put(m_strID.getBytes());
		
		while(iterEventFieldList.hasNext())
		{
			uef = iterEventFieldList.next();
			
			m_bytes.putInt(uef.nDataType);
			m_bytes.putInt(uef.strFieldName.getBytes().length);
			m_bytes.put(uef.strFieldName.getBytes());
			if(uef.nDataType == CMInfo.CM_BYTES)
			{
				m_bytes.putInt(uef.nValueByteNum);
				m_bytes.put(uef.valueBytes);
			}
			else
			{
				m_bytes.putInt(uef.strFieldValue.getBytes().length);
				m_bytes.put(uef.strFieldValue.getBytes());
			}
		}
		
		return;
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		int nDataType;
		String strFieldName;
		String strFieldValue;
		int nValueByteNum;
		byte[] valueBytes;
		
		removeAllEventFields();
		
		m_strID = getStringFromByteBuffer(msg);
		
		while( msg.remaining() > 0 )
		{
			nDataType = msg.getInt();
			strFieldName = getStringFromByteBuffer(msg);
			
			if(nDataType == CMInfo.CM_BYTES)
			{
				nValueByteNum = msg.getInt();
				valueBytes = new byte[nValueByteNum];
				msg.get(valueBytes);
				setEventBytesField(strFieldName, nValueByteNum, valueBytes);
			}
			else
			{
				strFieldValue = getStringFromByteBuffer(msg);
				setEventField(nDataType, strFieldName, strFieldValue);
			}
		}
		
		return;
	}
}
