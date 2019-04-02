package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.entity.CMPosition;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMDataEvent extends CMEvent{
	public static final int INHABITANT = 1;
	public static final int NEW_USER = 2;
	public static final int REMOVE_USER = 3;
	public static final int REQUEST_INHABITANT = 4;
	
	private String m_strUserName;
	private String m_strPasswd;
	private String m_strHostAddr;
	private int m_nUDPPort;
	private CMPosition m_pq;
	
	public CMDataEvent()
	{
		m_nType = CMInfo.CM_DATA_EVENT;
		m_strUserName = "?";
		m_strPasswd = "?";
		m_strHostAddr = "?";
		m_nUDPPort = -1;
		
		m_pq = new CMPosition();
	}
	
	public CMDataEvent(ByteBuffer msg)
	{
		this();
		unmarshall(msg);
	}
	
	// set/get methods
	public void setUserName(String name)
	{
		m_strUserName = name;
	}
	
	public String getUserName()
	{
		return m_strUserName;
	}
	
	public void setPassword(String passwd)
	{
		m_strPasswd = passwd;
	}
	
	public String getPassword()
	{
		return m_strPasswd;
	}
	
	public void setHostAddress(String host)
	{
		m_strHostAddr = host;
	}
	
	public String getHostAddress()
	{
		return m_strHostAddr;
	}
	
	public void setUDPPort(int port)
	{
		m_nUDPPort = port;
	}
	
	public int getUDPPort()
	{
		return m_nUDPPort;
	}
	
	public void setPosition(CMPosition pq)
	{
		m_pq.m_p.m_x = pq.m_p.m_x;
		m_pq.m_p.m_y = pq.m_p.m_y;
		m_pq.m_p.m_z = pq.m_p.m_z;
		m_pq.m_q.m_w = pq.m_q.m_w;
		m_pq.m_q.m_x = pq.m_q.m_x;
		m_pq.m_q.m_y = pq.m_q.m_y;
		m_pq.m_q.m_z = pq.m_q.m_z;
	}
	
	public CMPosition getPosition()
	{
		return m_pq;
	}
	
	//////////////////////////////////////////////////////////
	
	protected int getByteNum()
	{
		/*
		typedef struct _inhabitant {
			int userID; --> deleted
			char userName[NAME_NUM];
			char hostAddr[EVENT_FIELD_LEN];
			int nUDPPort;
			float pos[3];
			float quat[4];
		} inhabitant;

		typedef struct _newUser {
			int userID; --> deleted
			char userName[NAME_NUM];
			char hostAddr[EVENT_FIELD_LEN];
			int nUDPPort;
		} newUser;

		typedef struct _removeUser {
			int userID; --> deleted
			char userName[NAME_NUM];
		} removeUser;

		typedef struct _requestInhabitant {
			char userName[NAME_NUM];
		} requestInhabitant;
		*/
		int nByteNum = 0;
		nByteNum = super.getByteNum();
		
		switch(m_nID)
		{
		case CMDataEvent.INHABITANT:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strHostAddr.getBytes().length
					+ 7*Float.BYTES;
			break;
		case CMDataEvent.NEW_USER:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strHostAddr.getBytes().length;
			break;
		case CMDataEvent.REMOVE_USER:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case CMDataEvent.REQUEST_INHABITANT:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		default:
			nByteNum = -1;
			break;
		}
		
		return nByteNum;
	}
	
	protected void marshallBody()
	{
		switch(m_nID)
		{
		case CMDataEvent.INHABITANT:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strHostAddr.getBytes().length);
			m_bytes.put(m_strHostAddr.getBytes());
			m_bytes.putInt(m_nUDPPort);
			m_bytes.putFloat(m_pq.m_p.m_x);
			m_bytes.putFloat(m_pq.m_p.m_y);
			m_bytes.putFloat(m_pq.m_p.m_z);
			m_bytes.putFloat(m_pq.m_q.m_w);
			m_bytes.putFloat(m_pq.m_q.m_x);
			m_bytes.putFloat(m_pq.m_q.m_y);
			m_bytes.putFloat(m_pq.m_q.m_z);
			break;
		case CMDataEvent.NEW_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strHostAddr.getBytes().length);
			m_bytes.put(m_strHostAddr.getBytes());
			m_bytes.putInt(m_nUDPPort);
			break;
		case CMDataEvent.REMOVE_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		case CMDataEvent.REQUEST_INHABITANT:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		default:
			System.out.println("CMDataEvent.marshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}
		
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		switch(m_nID)
		{
		case CMDataEvent.INHABITANT:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strHostAddr = getStringFromByteBuffer(msg);
			m_nUDPPort = msg.getInt();
			m_pq.m_p.m_x = msg.getFloat();
			m_pq.m_p.m_y = msg.getFloat();
			m_pq.m_p.m_z = msg.getFloat();
			m_pq.m_q.m_w = msg.getFloat();
			m_pq.m_q.m_x = msg.getFloat();
			m_pq.m_q.m_y = msg.getFloat();
			m_pq.m_q.m_z = msg.getFloat();
			break;
		case CMDataEvent.NEW_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strHostAddr = getStringFromByteBuffer(msg);
			m_nUDPPort = msg.getInt();
			break;
		case CMDataEvent.REMOVE_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		case CMDataEvent.REQUEST_INHABITANT:
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		default:
			System.out.println("CMDataEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}
	}
	
}
