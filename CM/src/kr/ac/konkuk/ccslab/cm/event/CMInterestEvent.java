package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.entity.CMPoint3f;
import kr.ac.konkuk.ccslab.cm.entity.CMPosition;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMInterestEvent extends CMEvent{
	public static final int USER_ENTER = 1;
	public static final int USER_LEAVE = 2;
	public static final int USER_MOVE = 3;
	public static final int USER_COLLIDE = 4;
	public static final int USER_TALK = 5;

	private String m_strUserName;
	private String m_strPasswd;
	private String m_strHostAddr;
	private int m_nUDPPort;
	private String m_strCurrentGroup;
	private String m_strTalk;
	private CMPosition m_pq;
	private String m_strCollideObj;	

	public CMInterestEvent()
	{
		m_nType = CMInfo.CM_INTEREST_EVENT;
		m_strUserName = "";
		m_strPasswd = "";
		m_strHostAddr = "";
		m_nUDPPort = -1;
		m_strCurrentGroup = "";
		m_strTalk = "";
		m_strCollideObj = "";
	
		m_pq = new CMPosition();
		m_pq.m_p.m_x = 0.0f;
		m_pq.m_p.m_y = 0.0f;
		m_pq.m_p.m_z = 0.0f;
		m_pq.m_q.m_w = 0.0f;
		m_pq.m_q.m_x = 0.0f;
		m_pq.m_q.m_y = 0.0f;
		m_pq.m_q.m_z = 0.0f;
	}
	
	public CMInterestEvent(ByteBuffer msg)
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
	
	public void setCurrentGroup(String gName)
	{
		m_strCurrentGroup = gName;
	}
	
	public String getCurrentGroup()
	{
		return m_strCurrentGroup;
	}
	
	public void setTalk(String talk)
	{
		m_strTalk = talk;
	}
	
	public String getTalk()
	{
		return m_strTalk;
	}
	
	public void setPoint3f(CMPoint3f p)
	{
		m_pq.m_p.setPoint(p.m_x, p.m_y, p.m_z);
	}
	
	public CMPoint3f getPoint3f()
	{
		return m_pq.m_p;
	}
	
	public void setPosition(CMPosition pq)
	{
		m_pq.m_p.setPoint(pq.m_p.m_x, pq.m_p.m_y, pq.m_p.m_z);
		m_pq.m_q.setQuat(pq.m_q.m_w, pq.m_q.m_x, pq.m_q.m_y, pq.m_q.m_z);
	}
	
	public CMPosition getPosition()
	{
		return m_pq;
	}
	
	public void setCollideObj(String oName)
	{
		m_strCollideObj = oName;
	}
	
	public String getCollideObj()
	{
		return m_strCollideObj;
	}
	
	///////////////////////////////////////////////////////////////////////
	protected int getByteNum()
	{
		/*
		typedef struct _userEntered {
			int userID; //--> deleted
			char userName[NAME_NUM];
			char hostAddr[EVENT_FIELD_LEN];
			int nUDPPort;
			char currentRegion[EVENT_FIELD_LEN];
			float pos[3];
			float quat[4];
		} userEntered;

		typedef struct _userLeaved {
			char userName[NAME_NUM];
		} userLeaved;

		typedef struct _userMoved {
			char userName[NAME_NUM];
			float pos[3];
			float quat[4];
		} userMoved;

		typedef struct _collision {
			char userName[NAME_NUM];
			char collideObj[EVENT_FIELD_LEN];
		} collision;	

		typedef struct _talk {
			char userName[NAME_NUM];
			char talk[TALK_LEN];
		} talk;
		*/
		
		int nByteNum = 0;
		nByteNum = super.getByteNum();
		
		switch(m_nID)
		{
		case CMInterestEvent.USER_ENTER:
			nByteNum += 4*Integer.BYTES + m_strUserName.getBytes().length + m_strHostAddr.getBytes().length
					+ m_strCurrentGroup.getBytes().length + 7*Float.BYTES;
			break;
		case CMInterestEvent.USER_LEAVE:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case CMInterestEvent.USER_MOVE:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length + 7*Float.BYTES;
			break;
		case CMInterestEvent.USER_COLLIDE:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strCollideObj.getBytes().length;
			break;
		case CMInterestEvent.USER_TALK:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strTalk.getBytes().length;
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
		case CMInterestEvent.USER_ENTER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strHostAddr.getBytes().length);
			m_bytes.put(m_strHostAddr.getBytes());
			m_bytes.putInt(m_nUDPPort);
			m_bytes.putInt(m_strCurrentGroup.getBytes().length);
			m_bytes.put(m_strCurrentGroup.getBytes());
			m_bytes.putFloat(m_pq.m_p.m_x);
			m_bytes.putFloat(m_pq.m_p.m_y);
			m_bytes.putFloat(m_pq.m_p.m_z);
			m_bytes.putFloat(m_pq.m_q.m_w);
			m_bytes.putFloat(m_pq.m_q.m_x);
			m_bytes.putFloat(m_pq.m_q.m_y);
			m_bytes.putFloat(m_pq.m_q.m_z);
			break;
		case CMInterestEvent.USER_LEAVE:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		case CMInterestEvent.USER_MOVE:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putFloat(m_pq.m_p.m_x);
			m_bytes.putFloat(m_pq.m_p.m_y);
			m_bytes.putFloat(m_pq.m_p.m_z);
			m_bytes.putFloat(m_pq.m_q.m_w);
			m_bytes.putFloat(m_pq.m_q.m_x);
			m_bytes.putFloat(m_pq.m_q.m_y);
			m_bytes.putFloat(m_pq.m_q.m_z);
			break;
		case CMInterestEvent.USER_COLLIDE:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strCollideObj.getBytes().length);
			m_bytes.put(m_strCollideObj.getBytes());
			break;
		case CMInterestEvent.USER_TALK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strTalk.getBytes().length);
			m_bytes.put(m_strTalk.getBytes());
			break;
		default:
			System.out.println("CMInterestEvent.marshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		switch(m_nID)
		{
		case CMInterestEvent.USER_ENTER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strHostAddr = getStringFromByteBuffer(msg);
			m_nUDPPort = msg.getInt();
			m_strCurrentGroup = getStringFromByteBuffer(msg);
			
			m_pq.m_p.m_x = msg.getFloat();
			m_pq.m_p.m_y = msg.getFloat();
			m_pq.m_p.m_z = msg.getFloat();
			m_pq.m_q.m_w = msg.getFloat();
			m_pq.m_q.m_x = msg.getFloat();
			m_pq.m_q.m_y = msg.getFloat();
			m_pq.m_q.m_z = msg.getFloat();
			
			break;
		case CMInterestEvent.USER_LEAVE:
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		case CMInterestEvent.USER_MOVE:
			m_strUserName = getStringFromByteBuffer(msg);

			m_pq.m_p.m_x = msg.getFloat();
			m_pq.m_p.m_y = msg.getFloat();
			m_pq.m_p.m_z = msg.getFloat();
			m_pq.m_q.m_w = msg.getFloat();
			m_pq.m_q.m_x = msg.getFloat();
			m_pq.m_q.m_y = msg.getFloat();
			m_pq.m_q.m_z = msg.getFloat();
			
			break;
		case CMInterestEvent.USER_COLLIDE:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strCollideObj = getStringFromByteBuffer(msg);
			break;
		case CMInterestEvent.USER_TALK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strTalk = getStringFromByteBuffer(msg);
			break;
		default:
			System.out.println("CMInterestEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}
	}
}
