package kr.ac.konkuk.ccslab.cm.event;

/**
 * This class represents CM events that belong to the fixed header of 
 * MQTT control packets.
 * @author mlim
 *
 */
public abstract class CMMqttEventFixedHeader extends CMMqttEvent {

	private byte m_packetType; // 4 bits, 1~14
	private byte m_flag; //	4 bits
	private int m_nRemainingLength;	// 1~4 bytes
	
	public CMMqttEventFixedHeader()
	{
		m_packetType = -1;
		m_flag = -1;
		m_nRemainingLength = -1;
	}
	
	// setter/getter
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

		// from here
	}

	@Override
	protected void unmarshallFixedHeader() {
		// TODO Auto-generated method stub

	}

	/*
	// abstract methods that must be implemented by a sub-class
	protected abstract int getVarHeaderByteNum();
	protected abstract int getPayloadByteNum();
	protected abstract void marshallVarHeader();
	protected abstract void unmarshallVarHeader();
	protected abstract void marshallPayload();
	protected abstract void unmarshallPayload();
	*/
}
