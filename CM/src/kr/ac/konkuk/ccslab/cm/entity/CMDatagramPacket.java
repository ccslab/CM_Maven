package kr.ac.konkuk.ccslab.cm.entity;

public class CMDatagramPacket {
	public	int m_nMsgID;
	public	int m_nMsgBytes;
	public	int m_nTotalPacketNum;
	public	int m_nRecvPacketNum;
	public byte[]	msgPtr;		// not clear
	
	public CMDatagramPacket()
	{
		m_nMsgID = -1;
		m_nMsgBytes = -1;
		m_nTotalPacketNum = -1;
		m_nRecvPacketNum = 0;
		msgPtr = null;
	}
}
