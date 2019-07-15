package kr.ac.konkuk.ccslab.cm.event.mqttevent;

import java.nio.ByteBuffer;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents a CM event that the variable header and payload of 
 * MQTT CONNECT packet.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttEventCONNECT extends CMMqttEventFixedHeader {

	//////////////////////////////////////////////////
	// member variables (variable header)
	private String m_strProtocolName; // encoding with 2 bytes length and the string length
	private byte m_protocolLevel;	// 1 byte
	private byte m_connectFlag;		// 1 byte
	private int m_nKeepAlive;		// 2 bytes
	
	//////////////////////////////////////////////////
	// member variables (payload)
	private String m_strClientID;	// encoding with 2 bytes length and the string length
	private String m_strWillTopic;	// encoding with 2 bytes length and the string length
	private String m_strWillMessage;// encoding with 2 bytes length and the string length
	private String m_strUserName;	// encoding with 2 bytes length and the string length
	private String m_strPassword;	// encoding with 2 bytes length and the string length
	
	//////////////////////////////////////////////////
	// constructors

	public CMMqttEventCONNECT() {
		// initialize CM event ID
		m_nID = CMMqttEvent.CONNECT;
		// initialize fixed header
		m_packetType = CMMqttEvent.CONNECT;
		m_flag = 0;
		// m_nRemainingLength is determined at getFixedHeaderByteNum()
		
		// initialize variable header
		m_strProtocolName = "MQTT";
		m_protocolLevel = 4;	// MQTT 3.1.1
		m_connectFlag = 0;	// flag bits (user, passwd, will retain, will flag. clean session) and will qos
		m_nKeepAlive = 0;
		
		// initialize payload
		m_strClientID = "";
		m_strWillTopic = "";
		m_strWillMessage = "";
		m_strUserName = "";
		m_strPassword = "";
	}
	
	public CMMqttEventCONNECT(ByteBuffer msg) {
		this();
		unmarshall(msg);
	}
	
	//////////////////////////////////////////////////
	// setter/getter (variable header)
	public void set(byte connectFlag, int nKeepAlive)
	{
		m_connectFlag = connectFlag;
		m_nKeepAlive = nKeepAlive;
	}
	
	public void setProtocolName(String strName)
	{
		m_strProtocolName = strName;
	}
	
	public String getProtocolName()
	{
		return m_strProtocolName;
	}
	
	public void setProtocolLevel(byte level)
	{
		m_protocolLevel = level;
	}
	
	public byte getProtocolLevel()
	{
		return m_protocolLevel;
	}
	
	public void setConnFlag(byte flag)
	{
		m_connectFlag = flag;
	}
	
	public byte getConnFlag()
	{
		return m_connectFlag;
	}
	
	public void setUserNameFlag(boolean bUser)
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.setUserNameFlag(): "+bUser);
			System.out.println("connect flag (before): "+ getConnectFlagString());
		}
		
		// set the user name bit
		if(bUser)
			m_connectFlag |= 0x80;	// 0b1000 0000
		else
			m_connectFlag &= 0x7f;	// 0b0111 1111
		
		// print modified connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("connect flag (after): "+ getConnectFlagString());
		}
	}
	
	public boolean isUserNameFlag()
	{
		if((m_connectFlag & 0x80) == 0) return false;
		else return true;
	}
	
	public void setPasswordFlag(boolean bPassword)
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.setPasswordFlag(): "+bPassword);
			System.out.println("connect flag (before): "+getConnectFlagString());
		}
		
		// set the password bit
		if(bPassword)
			m_connectFlag |= 0x40;	// 0b0100 0000
		else
			m_connectFlag &= 0xbf;	// 0b1011 1111
		
		// print modified connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("connect flag (after): "+getConnectFlagString());
		}
	}
	
	public boolean isPasswordFlag()
	{
		if((m_connectFlag & 0x40) == 0) return false;
		else return true;
	}
	
	public void setWillRetainFlag(boolean bWillRetain)
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.setWillRetainFlag(): "+bWillRetain);
			System.out.println("connect flag (before): "+getConnectFlagString());
		}
		
		// set the will-retain flag
		if(bWillRetain)
			m_connectFlag |= 0x20;	// 0b0010 0000
		else
			m_connectFlag &= 0xdf;	// 0b1101 1111
		
		// print modified connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("connect flag (after): "+getConnectFlagString());
		}
	}
	
	public boolean isWillRetainFlag()
	{
		if((m_connectFlag & 0x20) == 0) return false;
		else return true;
	}
	
	public void setWillQoS(byte qos)
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.setWillQoS(): "+qos);
			System.out.println("connect flag (before): "+getConnectFlagString());
		}
		
		// set will-QoS flag
		m_connectFlag &= 0xe7;	// 0b1110 0111
		m_connectFlag |= (qos << 3);
		
		// print modified connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("connect flag (after): "+getConnectFlagString());
		}
	}
	
	public byte getWillQoS()
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.getWillQoS(): ");
			System.out.println("connect flag: "+getConnectFlagString());
		}
		// get will qos
		byte willQoS = 0;
		willQoS = (byte)((m_connectFlag & 0x18) >> 3);
		
		// print will qos
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("will QoS: "+willQoS);
		}
		
		return willQoS;
	}
	
	public void setWillFlag(boolean bWill)
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.setWillFlag(): "+bWill);
			System.out.println("connect flag (before): "+getConnectFlagString());
		}
		
		// set will flag
		if(bWill)
			m_connectFlag |= 0x04;	// 0b0000 0100
		else
			m_connectFlag &= 0xfb;	// 0b1111 1011
		
		// print modified connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("connect flag (after): "+getConnectFlagString());
		}
	}
	
	public boolean isWillFlag()
	{
		if((m_connectFlag & 0x04) == 0) return false;
		else return true;
	}
	
	public void setCleanSessionFlag(boolean bCleanSession)
	{
		// print current connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventCONNECT.setCleanSessionFlag(): "+bCleanSession);
			System.out.println("connect flag (before): "+getConnectFlagString());
		}
		
		// set clean-session flag
		if(bCleanSession)
			m_connectFlag |= 0x02;	// 0b0000 0010
		else
			m_connectFlag &= 0xfd;	// 0b1111 1101
				
		// print modified connect flag
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("connect flag (after): "+getConnectFlagString());
		}
	}
	
	public boolean isCleanSessionFlag()
	{
		if((m_connectFlag & 0x02) == 0) return false;
		else return true;
	}
	
	public void setKeepAlive(int seconds)
	{
		m_nKeepAlive = seconds;
	}
	
	public int getKeepAlive()
	{
		return m_nKeepAlive;
	}
		
	private String getConnectFlagString()
	{
		String strConnFlag = String.format("%8s", Integer.toBinaryString(m_connectFlag & 0xff))
				.replace(' ', '0');
		return strConnFlag;
	}
	
	//////////////////////////////////////////////////
	// overridden methods (variable header)

	@Override
	protected int getVarHeaderByteNum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void marshallVarHeader() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void unmarshallVarHeader(ByteBuffer buf) {
		// TODO Auto-generated method stub

	}

	//////////////////////////////////////////////////
	// setter/getter (payload)

	
	//////////////////////////////////////////////////
	// overridden methods (payload)

	@Override
	protected int getPayloadByteNum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void marshallPayload() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void unmarshallPayload(ByteBuffer buf) {
		// TODO Auto-generated method stub

	}

}
