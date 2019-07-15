package kr.ac.konkuk.ccslab.cm.event.mqttevent;

import java.nio.ByteBuffer;

import com.mysql.jdbc.Buffer;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that belong to the fixed header of 
 * MQTT control packets.
 * @author mlim
 *
 */
public abstract class CMMqttEventFixedHeader extends CMMqttEvent {

	protected byte m_packetType; // 4 bits, 1~14
	protected byte m_flag; //	4 bits
	protected int m_nRemainingLength;	// 1~4 bytes
	
	public CMMqttEventFixedHeader()
	{
		m_packetType = -1;
		m_flag = -1;
		m_nRemainingLength = -1;
	}
	
	// setter/getter
	
	public void setFixedHeader(byte packetType, byte flag)
	{
		m_packetType = packetType;
		m_flag = flag;
	}
	
	public void setPacketType(byte type)
	{
		m_packetType = type;
	}
	
	public byte getPacketType()
	{
		return m_packetType;
	}
	
	public void setFlag(byte flag)
	{
		m_flag = flag;
	}
	
	public byte getFlag()
	{
		return m_flag;
	}
	
	public void setRemainingLength(int len)
	{
		m_nRemainingLength = len;
	}
	
	public int getRemainingLength()
	{
		return m_nRemainingLength;
	}
	
	
	@Override
	protected int getFixedHeaderByteNum() {
		// TODO Auto-generated method stub
		
		int nByteNum = 1;	// packet type (4 bits) + flags (4 bits)
		m_nRemainingLength = getVarHeaderByteNum() + getPayloadByteNum();
		// according to MQTT v3.1.1, page 18, Table 2.4
		if(m_nRemainingLength >= 0 && m_nRemainingLength <= 127)
			nByteNum++;
		else if(m_nRemainingLength >= 128 && m_nRemainingLength <= 16383)
			nByteNum += 2;
		else if(m_nRemainingLength >= 16384 && m_nRemainingLength <= 2097151)
			nByteNum += 3;
		else if(m_nRemainingLength >= 2097152 && m_nRemainingLength <= 268435455)
			nByteNum += 4;
		else
		{
			System.err.print("CMMqttEventFixedHeader.getFixedHeaderByteNum(), ");
			System.err.print("out of bounds of the remaining length field of ");
			System.err.println("the fixed header: "+m_nRemainingLength);
			nByteNum = -1;
		}
		
		return nByteNum;
	}

	@Override
	protected void marshallFixedHeader() {
		// TODO Auto-generated method stub
		
		// encoding byte 1: packet type(4 bits) + flag (4 bits)
		byte byteTypeFlag = 0;
		byteTypeFlag = (byte)((byteTypeFlag | m_packetType) << 4);
		byteTypeFlag = (byte)(byteTypeFlag | m_flag);
		// print current value and the encoded byte
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventFixedHeader.marshallFixedHeader(): ");
			System.out.println("packet type: "+m_packetType+", flag: "+m_flag);
			System.out.println("encoded byte: "+String.format("%08x", byteTypeFlag));
		}
		// put the encoded type and flag to the ByteBuffer
		m_bytes.put(byteTypeFlag);
		
		// encoding byte 2: remaining length (1 ~ 4 bytes)
		// print current value
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("remaining length: "+m_nRemainingLength);
		}
		// The encoding rule conforms to MQTT v3.1.1 (page 19)
		int X = m_nRemainingLength;
		do {
			byte encodedByte = (byte)(X % 128);
			X = X / 128;
			// if there are more data to encode, set the top bit of this byte
			if( X > 0 )
				encodedByte = (byte)(encodedByte | 128);
			// put the encoded byte to the ByteBuffer
			m_bytes.put(encodedByte);
			// print the encoded byte
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("encoded reamining length byte: "+
						String.format("%08x", encodedByte));
			}
		} while( X > 0 );
		
		return;
	}

	@Override
	protected void unmarshallFixedHeader(ByteBuffer buf) {
		// TODO Auto-generated method stub

		// decoding byte 1: packet type (4 bits) + flag (4 bits)
		byte byteTypeFlag = buf.get();
		m_packetType &= 0x00; // initialize the variable
		m_packetType = (byte)((byteTypeFlag | 0xf0) >> 4);
		m_flag &= 0x00;	// initizliae the variable
		m_flag = (byte)(byteTypeFlag | 0x0f);
		// print current byte value and the decoded packet type and flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventFixedHeader.unmarshallFixedHeader(): ");
			System.out.println("byte 1: "+String.format("%08x",  byteTypeFlag));
			System.out.println("packet type: "+m_packetType+", flag: "+m_flag);
		}
		
		// decoding byte 2: remaining length (1 ~ 4 bytes)
		// The decoding rule conforms to MQTT v3.1.1 (page 19)
		int nMultiplier = 1;
		byte encodedByte = 0;
		m_nRemainingLength = 0;
		do {
			encodedByte = buf.get();
			// print the encoded byte
			if(CMInfo._CM_DEBUG)
				System.out.println("remaining length byte: "+String.format("%08x", encodedByte));
			m_nRemainingLength += (encodedByte & 127)*nMultiplier;
			nMultiplier *= 128;
			if(nMultiplier > 128*128*128)
			{
				System.err.println("malformed remaining length!");
				return;
			}
		} while( (encodedByte & 128) != 0 );
		
		// print the decoded remaining length
		if(CMInfo._CM_DEBUG)
			System.out.println("remaining length: "+m_nRemainingLength);
		
		return;
	}

	/*
	// abstract methods that must be implemented by a sub-class
	protected abstract int getVarHeaderByteNum();
	protected abstract int getPayloadByteNum();
	protected abstract void marshallVarHeader();
	protected abstract void unmarshallVarHeader(ByteBuffer buf);
	protected abstract void marshallPayload();
	protected abstract void unmarshallPayload(ByteBuffer buf);
	*/
}
