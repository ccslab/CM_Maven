package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.info.CMGroupInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMSessionInfo;

public class CMSessionEvent extends CMEvent {
	public static final int LOGIN = 1;						// 로긴 요청 (c->s)
	public static final int LOGOUT = 2;						// 로그아웃 (c->s)
	public static final int LOGIN_ACK = 3;					// 로긴 응답 (s->c)
	public static final int REQUEST_SESSION_INFO = 4;		// 세션 정보 요청 (c->s)
	public static final int RESPONSE_SESSION_INFO = 5;		// 세션 정보 응답 (s->c)
	public static final int JOIN_SESSION = 6;				// 세션 가입 요청 (c->s) sm에서 처리
	public static final int JOIN_SESSION_ACK = 7;			// 세션 가입 응답 (s->c) sm에서 처리
	public static final int LEAVE_SESSION = 8;				// 세션 탈퇴 요청 (c->s) sm에서 처리
	public static final int LEAVE_SESSION_ACK = 9;			// 세션 탈퇴 응답 (s->c) sm에서 처리
	public static final int SESSION_TALK = 10;				// 로긴상태에서의 채팅 (c<->s)
	public static final int SESSION_ADD_USER = 11;			// 로긴 사용자 추가 (s->c)
	public static final int SESSION_REMOVE_USER = 12;		// 로그아웃 사용자 삭제 (s->c)
	public static final int CHANGE_SESSION = 13;			// 사용자의 세션 변경 (s->c)
	public static final int ADD_CHANNEL = 14;				// add channel info (c->s)
	public static final int ADD_CHANNEL_ACK = 15;			// ack for added channel (s->c)

	public static final int REGISTER_USER = 16;				// 사용자 등록 요청 (c->s)
	public static final int REGISTER_USER_ACK = 17;			// 사용자 등록 요청 응답 (s->c)
	public static final int DEREGISTER_USER = 18;			// 사용자 탈퇴 요청 (c->s)
	public static final int DEREGISTER_USER_ACK = 19;		// 사용자 탈퇴 요청 응답 (s->c)
	public static final int FIND_REGISTERED_USER = 20;		// 사용자 검색 (c->s)
	public static final int FIND_REGISTERED_USER_ACK = 21;	// 사용자 검색 응답 (s->c)

	String m_strUserName;						// login or out user name
	String m_strPasswd;							// password of user
	String m_strHostAddr;						// host address of the login user
	int m_nUDPPort;

	int m_bValidUser;							// for LOGINACK
	String m_strSessionName;					// 세션 이름

	String m_strCurrentGroupName;				// for LOGINACK (group name)
	String m_strCurrentAddress;					// for LOGINACK (group multicast address)
	int m_nCurrentPort;							// for LOGINACK (group port)
	
	int m_nSessionNum;
	Vector< CMSessionInfo > m_sessionList;
	int m_nGroupNum;
	Vector< CMGroupInfo > m_groupList;

	String m_strCommArch;						// communication architecture (CM_PS/CM_CS)
	int m_bLoginScheme;
	int m_bSessionScheme;
	int m_nAttachDownloadScheme;
	int m_nReturnCode;
	String m_strTalk;
	
	String m_strChannelName;
	int m_nChannelNum;

	String m_strCreationTime;			// for user registration time

	public CMSessionEvent()
	{
		m_nType = CMInfo.CM_SESSION_EVENT;
		m_strHostAddr = "?";
		m_strPasswd = "?";
		m_strUserName = "?";
		m_nUDPPort = -1;
		m_bValidUser = -1;
		m_strSessionName = "?";
		m_strCurrentGroupName = "?";
		m_strCurrentAddress = "?";
		m_nCurrentPort = -1;
		m_nSessionNum = -1;
		m_nGroupNum = -1;
		m_strCommArch = "?";
		m_bLoginScheme = -1;
		m_bSessionScheme = -1;
		m_nAttachDownloadScheme = -1;
		m_nReturnCode = -1;
		m_strTalk = "?";
		m_strChannelName = "?";
		m_nChannelNum = -1;
		m_strCreationTime = "?";
		
		m_sessionList = new Vector<CMSessionInfo>();
		m_groupList = new Vector<CMGroupInfo>();
	}
	
	public CMSessionEvent(ByteBuffer msg)
	{
		m_nType = CMInfo.CM_SESSION_EVENT;
		m_strHostAddr = "?";
		m_strPasswd = "?";
		m_strUserName = "?";
		m_nUDPPort = -1;
		m_bValidUser = -1;
		m_strSessionName = "?";
		m_strCurrentGroupName = "?";
		m_strCurrentAddress = "?";
		m_nCurrentPort = -1;
		m_nSessionNum = -1;
		m_nGroupNum = -1;
		m_strCommArch = "?";
		m_bLoginScheme = -1;
		m_bSessionScheme = -1;
		m_nAttachDownloadScheme = -1;
		m_nReturnCode = -1;
		m_strTalk = "?";
		m_strChannelName = "?";
		m_nChannelNum = -1;
		m_strCreationTime = "?";
		
		m_sessionList = new Vector<CMSessionInfo>();
		m_groupList = new Vector<CMGroupInfo>();

		unmarshallHeader(msg);
		unmarshallBody(msg);
	}
	
	public CMSessionEvent unmarshall(ByteBuffer msg)
	{
		unmarshallHeader(msg);
		unmarshallBody(msg);
		
		return this;
	}
	
	// set/get methods
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
	
	public void setPassword(String passwd)
	{
		m_strPasswd = passwd;
	}
	
	public String getPassword()
	{
		return m_strPasswd;
	}
	
	public void setUserName(String uname)
	{
		m_strUserName = uname;
	}
	
	public String getUserName()
	{
		return m_strUserName;
	}
	
	public void setValidUser(int bValid)
	{
		m_bValidUser = bValid;
	}
	
	public int isValidUser()
	{
		return m_bValidUser;
	}
	
	public void setSessionName(String sname)
	{
		m_strSessionName = sname;
	}
	
	public String getSessionName()
	{
		return m_strSessionName;
	}
	
	public void setCommArch(String commArch)
	{
		m_strCommArch = commArch;
	}
	
	public String getCommArch()
	{
		return m_strCommArch;
	}
	
	public void setLoginScheme(int bLoginScheme)
	{
		m_bLoginScheme = bLoginScheme;
	}
	
	public int isLoginScheme()
	{
		return m_bLoginScheme;
	}
	
	public void setSessionScheme(int bSessionScheme)
	{
		m_bSessionScheme = bSessionScheme;
	}
	
	public int isSessionScheme()
	{
		return m_bSessionScheme;
	}
	
	public void setAttachDownloadScheme(int nScheme)
	{
		m_nAttachDownloadScheme = nScheme;
	}
	
	public int getAttachDownloadScheme()
	{
		return m_nAttachDownloadScheme;
	}
	
	public void setReturnCode(int code)
	{
		m_nReturnCode = code;
	}
	
	public int getReturnCode()
	{
		return m_nReturnCode;
	}
	
	public void setTalk(String talk)
	{
		m_strTalk = talk;
	}
	
	public String getTalk()
	{
		return m_strTalk;
	}
	
	public void setCurrentGroupName(String gname)
	{
		m_strCurrentGroupName = gname;
	}
	
	public String getCurrentGroupName()
	{
		return m_strCurrentGroupName;
	}
	
	public void setCurrentAddress(String addr)
	{
		m_strCurrentAddress = addr;
	}
	
	public String getCurrentAddress()
	{
		return m_strCurrentAddress;
	}
	
	public void setCurrentPort(int port)
	{
		m_nCurrentPort = port;
	}
	
	public int getCurrentPort()
	{
		return m_nCurrentPort;
	}
	
	public void setChannelName(String name)
	{
		m_strChannelName = name;
	}
	
	public String getChannelName()
	{
		return m_strChannelName;
	}
	
	public void setChannelNum(int num)
	{
		m_nChannelNum = num;
	}
	
	public int getChannelNum()
	{
		return m_nChannelNum;
	}
	
	public void setSessionNum(int num)
	{
		m_nSessionNum = num;
	}
	
	public int getSessionNum()
	{
		return m_nSessionNum;
	}
	
	public void setGroupNum(int num)
	{
		m_nGroupNum = num;
	}
	
	public int getGroupNum()
	{
		return m_nGroupNum;
	}
	
	public void setCreationTime(String time)
	{
		m_strCreationTime = time;
	}
	
	public String getCreationTime()
	{
		return m_strCreationTime;
	}
	
	public boolean addSessionInfo(CMSessionInfo si)
	{
		CMSessionInfo tsi = findSessionInfo(si.getSessionName());
		
		if( tsi != null )
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMSessionEvent.addSessionInfo(), already exists: "+si.getSessionName());
			return false;
		}
		
		m_sessionList.addElement(si);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMSessionEvent.addSessionInfo(), ok session: "+si.getSessionName());

		return true;
	}
	
	public boolean removeSessionInfo(String sname)
	{
		boolean found = false;
		CMSessionInfo tsi;
		Iterator<CMSessionInfo> iter = m_sessionList.iterator();
		
		while(iter.hasNext() && !found)
		{
			tsi = iter.next();
			if( sname.equals(tsi.getSessionName()) )
			{
				iter.remove();
				found = true;
			}
		}

		if(!found)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMSessionEvent.removeSessionInfo(), not found: "+sname);
			
			return false;
		}

		if(CMInfo._CM_DEBUG)
			System.out.println("CMSessionEvent.removeSessionInfo(), Ok session: "+sname);

		return true;		
	}
	
	public void removeAllSessionInfoObjects()
	{
		/*
		Iterator<CMSessionInfo> iter = m_sessionList.iterator();
		while(iter.hasNext())
		{
			CMSessionInfo si = iter.next();
			si = null;
		}
		*/
		m_sessionList.removeAllElements();
		return;
	}
	
	public CMSessionInfo findSessionInfo(String sname)
	{
		boolean found = false;
		CMSessionInfo tsi = null;
		Iterator<CMSessionInfo> iter = m_sessionList.iterator();
		
		while(iter.hasNext() && !found)
		{
			tsi = iter.next();
			if( sname.equals(tsi.getSessionName()) )
			{
				found = true;
			}
		}

		if(!found)
		{
			//if(CMInfo._CM_DEBUG)
			//	System.out.println("CMSessionEvent.findSessionInfo(), not found: "+sname);

			return null;
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMSessionEvent.findSessionInfo(), Ok session: "+sname);

		return tsi;
	}
	
	public Vector<CMSessionInfo> getSessionInfoList()
	{
		return m_sessionList;
	}
	
	public boolean addGroupInfo(CMGroupInfo gi)
	{
		CMGroupInfo tgi = findGroupInfo(gi.getGroupName());
		
		if( tgi != null )
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMSessionEvent.addGroupInfo(), already exists: "+gi.getGroupName());
			return false;
		}
		
		m_groupList.addElement(gi);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMSessionEvent.addGroupInfo(), ok group: "+gi.getGroupName());

		return true;
	}
	
	public boolean removeGroupInfo(String gname)
	{
		boolean found = false;
		CMGroupInfo tgi;
		Iterator<CMGroupInfo> iter = m_groupList.iterator();
		
		while(iter.hasNext() && !found)
		{
			tgi = iter.next();
			if( gname.equals(tgi.getGroupName()) )
			{
				iter.remove();
				found = true;
			}
		}

		if(!found)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMSessionEvent.removeGroupInfo(), not found: "+gname);
			
			return false;
		}

		if(CMInfo._CM_DEBUG)
			System.out.println("CMSessionEvent.removeGroupInfo(), Ok session: "+gname);

		return true;		
	}
	
	public void removeAllGroupInfoObjects()
	{
		/*
		Iterator<CMGroupInfo> iter = m_groupList.iterator();
		while(iter.hasNext())
		{
			CMGroupInfo gi = iter.next();
			gi = null;
		}
		*/
		m_groupList.removeAllElements();
		return;
	}
	
	public CMGroupInfo findGroupInfo(String gname)
	{
		boolean found = false;
		CMGroupInfo tgi = null;
		Iterator<CMGroupInfo> iter = m_groupList.iterator();
		
		while(iter.hasNext() && !found)
		{
			tgi = iter.next();
			if( gname.equals(tgi.getGroupName()) )
			{
				found = true;
			}
		}

		if(!found)
		{
			//if(CMInfo._CM_DEBUG)
			//	System.out.println("CMSessionEvent.findGroupInfo(), not found: "+gname);

			return null;
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMSessionEvent.findGroupInfo(), Ok session: "+gname);

		return tgi;
	}
	
	public Vector<CMGroupInfo> getGroupInfoList()
	{
		return m_groupList;
	}

	/////////////////////////////////////////////
	
	protected int getByteNum()
	{
		/*
		typedef struct _logIn {
			char userName[NAME_NUM];
			char passwd[EVENT_FIELD_LEN];
			char hostAddr[EVENT_FIELD_LEN];
			int nUDPPort;
		} logIn;

		typedef struct _logOut {
			char userName[NAME_NUM];
		} logOut;

		typedef struct _loginAck {
			bool bValidUser;
			int userID;		// deleted
			char commArch[EVENT_FIELD_LEN];
			bool bLoginScheme;
			bool bSessionScheme;
			int nServerUDPPort;
		} loginAck;

		typedef struct _responseSessionInfo {
			int sessionNum;
			unsigned char session[1];
		} responseSessionInfo;

		typedef struct _sInfo {
			char sessionName[EVENT_FIELD_LEN];
			char address[EVENT_FIELD_LEN];
			int port;
			int userNum;
			unsigned char session[1];
		} sInfo;

		typedef struct _joinSessin {
			char userName[NAME_NUM];
			char sessionName[EVENT_FIELD_LEN];
		} joinSession;

		typedef struct _joinSessionAck {
			int nRegionNum;
			unsigned char next[1];
		} joinSessionAck;

		typedef struct _rInfo {
			char strRegionName[EVENT_FIELD_LEN];
			char strRegionAddress[EVENT_FIELD_LEN];
			int nRegionPort;
			unsigned char next[1];
		} rInfo;

		typedef struct _leaveSession {
			char userName[NAME_NUM];
			char sessionName[EVENT_FIELD_LEN];
		} leaveSession;

		typedef struct _leaveSessionAck {
			int returnCode;
		} leaveSessionAck;

		typedef struct _sessionTalk {
			char userName[NAME_NUM];
			char talk[TALK_LEN];
		} sessionTalk;

		typedef struct _sessionAddUser {
			char userName[NAME_NUM];
			char hostAddr[EVENT_FIELD_LEN];
			char sessionName[EVENT_FIELD_LEN];
		} sessionAddUser;

		typedef struct _sessionRemoveUser {
			char userName[NAME_NUM];
		} sessionRemoveUser;

		typedef struct _changeSession {
			char userName[NAME_NUM];
			char sessionName[EVENT_FIELD_LEN];
		} changeSession;

		typedef struct _addChannel {
			char strChannelName[NAME_NUM];
			int nChannelNum;
		} addChannel;

		typedef struct _addChannelAck {
			char strChannelName[NAME_NUM];
			int nChannelNum;
			int nReturnCode;
		} addChannelAck;
		*/
		
		Iterator<CMSessionInfo> iterSessionList;
		Iterator<CMGroupInfo> iterGroupList;
		int nElementByteNum = 0;
		
		int nByteNum = 0;
		nByteNum = super.getByteNum();
		
		switch(m_nID)
		{
		case LOGIN:
			nByteNum += 4*Integer.BYTES + m_strUserName.getBytes().length + m_strPasswd.getBytes().length
						+ m_strHostAddr.getBytes().length;
			break;
		case LOGOUT:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case LOGIN_ACK:
			nByteNum += 6*Integer.BYTES + m_strCommArch.getBytes().length;
			break;
		case REQUEST_SESSION_INFO:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case RESPONSE_SESSION_INFO:
			nByteNum += Integer.BYTES;
			nElementByteNum = 0;
			iterSessionList = m_sessionList.iterator();
			while(iterSessionList.hasNext())
			{
				CMSessionInfo tsi = iterSessionList.next();
				nElementByteNum += 4*Integer.BYTES + tsi.getSessionName().getBytes().length
								+ tsi.getAddress().getBytes().length;
			}
			nByteNum += nElementByteNum;
			break;
		case JOIN_SESSION:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strSessionName.getBytes().length;
			break;
		case JOIN_SESSION_ACK:
			nByteNum += Integer.BYTES; // group num
			nElementByteNum = 0;
			iterGroupList = m_groupList.iterator();
			while(iterGroupList.hasNext())
			{
				CMGroupInfo tgi = iterGroupList.next();
				nElementByteNum += 3*Integer.BYTES + tgi.getGroupName().getBytes().length 
								+ tgi.getGroupAddress().getBytes().length;
			}
			nByteNum += nElementByteNum;
			break;
		case LEAVE_SESSION:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strSessionName.getBytes().length;
			break;
		case LEAVE_SESSION_ACK:
			nByteNum += Integer.BYTES;
			break;
		case SESSION_TALK:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strTalk.getBytes().length;
			break;
		case SESSION_ADD_USER:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strHostAddr.getBytes().length
						+ m_strSessionName.getBytes().length;
			break;
		case SESSION_REMOVE_USER:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case CHANGE_SESSION:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strSessionName.getBytes().length;
			break;
		case ADD_CHANNEL:
			nByteNum += 2*Integer.BYTES + m_strChannelName.getBytes().length;
			break;
		case ADD_CHANNEL_ACK:
			nByteNum += 3*Integer.BYTES + m_strChannelName.getBytes().length;
			break;
		case REGISTER_USER:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strPasswd.getBytes().length;
			break;
		case REGISTER_USER_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length 
					+ m_strCreationTime.getBytes().length;
			break;
		case DEREGISTER_USER:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length + m_strPasswd.getBytes().length;
			break;
		case DEREGISTER_USER_ACK:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case FIND_REGISTERED_USER:
			nByteNum += Integer.BYTES + m_strUserName.getBytes().length;
			break;
		case FIND_REGISTERED_USER_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length 
					+ m_strCreationTime.getBytes().length;
			break;
		default:
			nByteNum = -1;
			break;
		}
		
		return nByteNum;
	}
	
	protected void marshallBody()
	{
		Iterator<CMSessionInfo> iterSessionList;
		Iterator<CMGroupInfo> iterGroupList;
		
		switch(m_nID)
		{
		case LOGIN:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strPasswd.getBytes().length);
			m_bytes.put(m_strPasswd.getBytes());
			m_bytes.putInt(m_strHostAddr.getBytes().length);
			m_bytes.put(m_strHostAddr.getBytes());
			m_bytes.putInt(m_nUDPPort);
			m_bytes.clear();
			break;
		case LOGOUT:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.clear();
			break;
		case LOGIN_ACK:
			m_bytes.putInt(m_bValidUser);
			m_bytes.putInt(m_strCommArch.getBytes().length);
			m_bytes.put(m_strCommArch.getBytes());
			m_bytes.putInt(m_bLoginScheme);
			m_bytes.putInt(m_bSessionScheme);
			m_bytes.putInt(m_nAttachDownloadScheme);
			m_bytes.putInt(m_nUDPPort);			// server udp port
			m_bytes.clear();
			break;
		case REQUEST_SESSION_INFO:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.clear();
			break;
		case RESPONSE_SESSION_INFO:
			if(m_nSessionNum != m_sessionList.size())
			{
				System.out.println("CMSessionEvent.marshallBody(), incorrect number of session info.");
				m_bytes = null;
				return;
			}
			
			m_bytes.putInt(m_nSessionNum);
			
			iterSessionList = m_sessionList.iterator();
			while(iterSessionList.hasNext())
			{
				CMSessionInfo tsi = iterSessionList.next();
				m_bytes.putInt(tsi.getSessionName().getBytes().length);
				m_bytes.put(tsi.getSessionName().getBytes());
				m_bytes.putInt(tsi.getAddress().getBytes().length);
				m_bytes.put(tsi.getAddress().getBytes());
				m_bytes.putInt(tsi.getPort());
				m_bytes.putInt(tsi.getUserNum());
			}
			m_bytes.clear();
			break;
		case JOIN_SESSION:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			m_bytes.clear();
			break;
		case JOIN_SESSION_ACK:
			if(m_nGroupNum != m_groupList.size())
			{
				System.out.println("CMSessionEvent.marshallBody(), incorrect number of group info.");
				m_bytes = null;
				return;
			}

			m_bytes.putInt(m_nGroupNum);
			
			iterGroupList = m_groupList.iterator();
			while(iterGroupList.hasNext())
			{
				CMGroupInfo tgi = iterGroupList.next();
				m_bytes.putInt(tgi.getGroupName().getBytes().length);
				m_bytes.put(tgi.getGroupName().getBytes());
				m_bytes.putInt(tgi.getGroupAddress().getBytes().length);
				m_bytes.put(tgi.getGroupAddress().getBytes());
				m_bytes.putInt(tgi.getGroupPort());
			}
			m_bytes.clear();
			break;
		case LEAVE_SESSION:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			m_bytes.clear();
			break;
		case LEAVE_SESSION_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.clear();
			break;
		case SESSION_TALK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strTalk.getBytes().length);
			m_bytes.put(m_strTalk.getBytes());
			m_bytes.clear();
			break;
		case SESSION_ADD_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strHostAddr.getBytes().length);
			m_bytes.put(m_strHostAddr.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			m_bytes.clear();
			break;
		case SESSION_REMOVE_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.clear();
			break;
		case CHANGE_SESSION:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			m_bytes.clear();
			break;
		case ADD_CHANNEL:
			m_bytes.putInt(m_strChannelName.getBytes().length);
			m_bytes.put(m_strChannelName.getBytes());
			m_bytes.putInt(m_nChannelNum);
			m_bytes.clear();
			break;
		case ADD_CHANNEL_ACK:
			m_bytes.putInt(m_strChannelName.getBytes().length);
			m_bytes.put(m_strChannelName.getBytes());
			m_bytes.putInt(m_nChannelNum);
			m_bytes.putInt(m_nReturnCode);
			m_bytes.clear();
			break;
		case REGISTER_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strPasswd.getBytes().length);
			m_bytes.put(m_strPasswd.getBytes());
			m_bytes.clear();
			break;
		case REGISTER_USER_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strCreationTime.getBytes().length);
			m_bytes.put(m_strCreationTime.getBytes());
			m_bytes.clear();
			break;
		case DEREGISTER_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strPasswd.getBytes().length);
			m_bytes.put(m_strPasswd.getBytes());
			m_bytes.clear();
			break;
		case DEREGISTER_USER_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.clear();
			break;
		case FIND_REGISTERED_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.clear();
			break;
		case FIND_REGISTERED_USER_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strCreationTime.getBytes().length);
			m_bytes.put(m_strCreationTime.getBytes());
			m_bytes.clear();
			break;
		default:
			System.out.println("CMSessionEvent.marshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		int i;
		
		switch(m_nID)
		{
		case LOGIN:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strPasswd = getStringFromByteBuffer(msg);
			m_strHostAddr = getStringFromByteBuffer(msg);
			m_nUDPPort = msg.getInt();
			msg.clear();
			break;
		case LOGOUT:
			m_strUserName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case LOGIN_ACK:
			m_bValidUser = msg.getInt();
			m_strCommArch = getStringFromByteBuffer(msg);
			m_bLoginScheme = msg.getInt();
			m_bSessionScheme = msg.getInt();
			m_nAttachDownloadScheme = msg.getInt();
			m_nUDPPort = msg.getInt();
			msg.clear();
			break;
		case REQUEST_SESSION_INFO:
			m_strUserName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case RESPONSE_SESSION_INFO:
			m_sessionList.removeAllElements();
			m_nSessionNum = msg.getInt();
			for(i = 0; i < m_nSessionNum; i++)
			{
				CMSessionInfo tsi = new CMSessionInfo();
				tsi.setSessionName(getStringFromByteBuffer(msg));
				tsi.setAddress(getStringFromByteBuffer(msg));
				tsi.setPort(msg.getInt());
				tsi.setUserNum(msg.getInt());
				addSessionInfo(tsi);
			}
			msg.clear();
			break;
		case JOIN_SESSION:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case JOIN_SESSION_ACK:
			m_groupList.removeAllElements();
			m_nGroupNum = msg.getInt();
			for(i = 0; i < m_nGroupNum; i++)
			{
				CMGroupInfo tgi = new CMGroupInfo();
				tgi.setGroupName(getStringFromByteBuffer(msg));
				tgi.setGroupAddress(getStringFromByteBuffer(msg));
				tgi.setGroupPort(msg.getInt());
				addGroupInfo(tgi);
			}
			msg.clear();
			break;
		case LEAVE_SESSION:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case LEAVE_SESSION_ACK:
			m_nReturnCode = msg.getInt();
			msg.clear();
			break;
		case SESSION_TALK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strTalk = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case SESSION_ADD_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strHostAddr = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case SESSION_REMOVE_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case CHANGE_SESSION:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case ADD_CHANNEL:
			m_strChannelName = getStringFromByteBuffer(msg);
			m_nChannelNum = msg.getInt();
			msg.clear();
			break;
		case ADD_CHANNEL_ACK:
			m_strChannelName = getStringFromByteBuffer(msg);
			m_nChannelNum = msg.getInt();
			m_nReturnCode = msg.getInt();
			msg.clear();
			break;
		case REGISTER_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strPasswd = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case REGISTER_USER_ACK:
			m_nReturnCode = msg.getInt();
			m_strUserName = getStringFromByteBuffer(msg);
			m_strCreationTime = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case DEREGISTER_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strPasswd = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case DEREGISTER_USER_ACK:
			m_nReturnCode = msg.getInt();
			m_strUserName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case FIND_REGISTERED_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		case FIND_REGISTERED_USER_ACK:
			m_nReturnCode = msg.getInt();
			m_strUserName = getStringFromByteBuffer(msg);
			m_strCreationTime = getStringFromByteBuffer(msg);
			msg.clear();
			break;
		default:
			System.out.println("CMSessionEvent.unmarshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}
	}
}
