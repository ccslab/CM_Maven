package kr.ac.konkuk.ccslab.cm.event.mqttevent;

import java.nio.ByteBuffer;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that belong to the MQTT control packets.
 * @author mlim
 *
 */
public abstract class CMMqttEvent extends CMEvent {

	// definition of MQTT event IDs
	public static final int CONNECT = 1;
	public static final int CONNACK = 2;
	public static final int PUBLISH = 3;
	public static final int PUBACK = 4;
	public static final int PUBREC = 5;
	public static final int PUBREL = 6;
	public static final int PUBCOMP = 7;
	public static final int SUBSCRIBE = 8;
	public static final int SUBACK = 9;
	public static final int UNSUBSCRIBE = 10;
	public static final int UNSUBACK = 11;
	public static final int PINGREQ = 12;
	public static final int PINGRESP = 13;
	public static final int DISCONNECT = 14;
	
	// abstract methods
	protected abstract int getFixedHeaderByteNum();
	protected abstract void marshallFixedHeader();
	protected abstract void unmarshallFixedHeader(ByteBuffer buf);
	protected abstract int getVarHeaderByteNum();
	protected abstract void marshallVarHeader();
	protected abstract void unmarshallVarHeader(ByteBuffer buf);
	protected abstract int getPayloadByteNum();
	protected abstract void marshallPayload();
	protected abstract void unmarshallPayload(ByteBuffer buf);
	
	public CMMqttEvent()
	{
		m_nType = CMInfo.CM_MQTT_EVENT;
	}
	
	public CMMqttEvent(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
		
	/**
	 * returns the string representation of this CMMqttEvent object.
	 * @return string of this object.
	 */
	@Override
	public String toString()
	{
		return super.toString();
	}
	
	@Override
	protected int getByteNum()
	{
		int nByteNum = 0;
		int nFixedHeaderByteNum = 0;
		int nVarHeaderByteNum = 0;
		int nPayloadByteNum = 0;
		
		nByteNum = super.getByteNum();
		
		nVarHeaderByteNum = getVarHeaderByteNum();
		nPayloadByteNum = getPayloadByteNum();
		nByteNum += nVarHeaderByteNum + nPayloadByteNum;

		// m_nRemainLength of the fixed header is determined after getVarHeaderByteNum() and 
		// getPayloadByteNum() are completed.
		nFixedHeaderByteNum = getFixedHeaderByteNum();
		nByteNum += nFixedHeaderByteNum;

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEvent.getByteNum(): fixed header("+nFixedHeaderByteNum
					+")+ var header("+nVarHeaderByteNum+")+ payload("+nPayloadByteNum
					+") = "+nByteNum);
		}
		
		return nByteNum;
	}
	
	@Override
	protected void marshallBody() 
	{
		// TODO Auto-generated method stub
		marshallFixedHeader();
		marshallVarHeader();
		marshallPayload();
		
		return;
	}

	@Override
	protected void unmarshallBody(ByteBuffer buf) 
	{
		// TODO Auto-generated method stub
		unmarshallFixedHeader(buf);
		unmarshallVarHeader(buf);
		unmarshallPayload(buf);
		
		return;
	}
	
	@Override
	protected void putStringToByteBuffer(String str)
	{
		int nStrLength = str.getBytes().length;
		putInt2BytesToByteBuffer(nStrLength);
		m_bytes.put(str.getBytes());
	}
	
	@Override
	protected String getStringFromByteBuffer(ByteBuffer buf)
	{
		int nStrLength = 0;
		byte[] strBytes;
		String str = null;
		
		nStrLength = getInt2BytesFromByteBuffer(buf);
		strBytes = new byte[nStrLength];
		buf.get(strBytes);
		str = new String(strBytes);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEvent.getStringFromByteBuffer(), str(\""+str+"\")");
		}
		
		return str;
	}

	// put 2 bytes number to ByteBuffer
	protected void putInt2BytesToByteBuffer(int num)
	{
		byte[] numBytes = new byte[2];
		numBytes[0] = 0;
		numBytes[1] = 0;
		
		numBytes[0] = (byte)((num & 0x0000ff00) >> 8);
		numBytes[1] = (byte)(num & 0x000000ff);
		
		m_bytes.put(numBytes);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEvent.putInt2BytesToByteBuffer(), num("+num
					+"), numBytes[0]("+numBytes[0]+"), numBytes[1]("+numBytes[1]+")");
		}
	}	
	
	// get integer with 2 bytes from ByteBuffer
	protected int getInt2BytesFromByteBuffer(ByteBuffer buf)
	{
		int num = 0;
		byte[] numBytes = new byte[2];
		buf.get(numBytes);
		
		num = (num | numBytes[0]) << 8;
		num = num | numBytes[1];
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEvent.getInt2BytesFromByteBuffer(), numBytes[0]("
					+numBytes[0]+"), numBytes[1]("+numBytes[1]+"), num("+num+")");
		}
		
		return num;
	}
	
}
