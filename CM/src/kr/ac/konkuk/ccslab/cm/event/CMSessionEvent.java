package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMGroupInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSessionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that are used for session related tasks.
 * 
 * @author mlim
 * @see CMEvent
 */
public class CMSessionEvent extends CMEvent {

	public static final int LOGIN = 1;	// login request (from client to server)
	public static final int LOGOUT = 2;	// logout request (from client to server)
	public static final int LOGIN_ACK = 3;	// response to the login request (from server to client)
	public static final int REQUEST_SESSION_INFO = 4;		// request session information (from client to server)
	public static final int RESPONSE_SESSION_INFO = 5;	// response to the session-information request (from server to client)
	public static final int JOIN_SESSION = 6;	// request to join a session (from client to server)
	public static final int JOIN_SESSION_ACK = 7;	// response to the join-session request (from server to client)
	public static final int LEAVE_SESSION = 8;		// request to leave the current session (from client to server)
	public static final int LEAVE_SESSION_ACK = 9;	// response to the leave-session request (from server to client)
	public static final int SESSION_TALK = 10;		// chat in a session
	public static final int SESSION_ADD_USER = 11;	// notify to add a login user (from server to client)
	public static final int SESSION_REMOVE_USER = 12;	// notify to remove a logout user (from server to client)
	public static final int CHANGE_SESSION = 13;		// notify to change a session of a user (from server to client)
	// request to add a non-blocking socket channel information (from client to server)
	public static final int ADD_NONBLOCK_SOCKET_CHANNEL = 14;	
	// response to the add-non-blocking-socket-channel request (from server to client)
	public static final int ADD_NONBLOCK_SOCKET_CHANNEL_ACK = 15;
	// request to add a blocking socket channel information (from client to server)
	public static final int ADD_BLOCK_SOCKET_CHANNEL = 22;
	// response to the add-blocking-socket-channel request (from server to client)
	public static final int ADD_BLOCK_SOCKET_CHANNEL_ACK = 23;
	// request to remove a blocking socket channel information (from client to server)
	public static final int REMOVE_BLOCK_SOCKET_CHANNEL = 24;	
	// response to the remove-blocking-socket-channel request (from server to client)
	public static final int REMOVE_BLOCK_SOCKET_CHANNEL_ACK = 25;	

	public static final int REGISTER_USER = 16;			// request to register a user (from client to server)
	public static final int REGISTER_USER_ACK = 17;		// response to the register-user request (from server to client)
	public static final int DEREGISTER_USER = 18;		// request to deregister a user (from client to server)
	public static final int DEREGISTER_USER_ACK = 19;		// response to the deregister-user request (from server to client)
	public static final int FIND_REGISTERED_USER = 20;	// request to find a user (from client to server)
	public static final int FIND_REGISTERED_USER_ACK = 21;	// response to the find-user request (from server to client)
	
	// local CM event from CM to notify the application of the unexpected disconnection (from client CM to application)
	public static final int UNEXPECTED_SERVER_DISCONNECTION = 99;	

	private String m_strUserName;
	private String m_strPasswd;
	private String m_strHostAddr;
	private int m_nUDPPort;

	private int m_bValidUser;
	private String m_strSessionName;

	private String m_strCurrentGroupName;
	private String m_strCurrentAddress;
	private int m_nCurrentPort;
	
	private int m_nSessionNum;
	private Vector< CMSessionInfo > m_sessionList;
	private int m_nGroupNum;
	private Vector< CMGroupInfo > m_groupList;

	private String m_strCommArch;
	private int m_bFileTransferScheme;
	private int m_bLoginScheme;
	private int m_bSessionScheme;
	private int m_nAttachDownloadScheme;
	private int m_nReturnCode;
	private String m_strTalk;
	
	private String m_strChannelName;
	private int m_nChannelNum;

	private String m_strCreationTime;

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
		m_bFileTransferScheme = -1;
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
		this();
		unmarshall(msg);
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
	
	public void setFileTransferScheme(int bFileTransferScheme)
	{
		m_bFileTransferScheme = bFileTransferScheme;
	}
	
	public int isFileTransferScheme()
	{
		return m_bFileTransferScheme;
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
			nByteNum += 7*Integer.BYTES + m_strCommArch.getBytes().length;
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
		case ADD_NONBLOCK_SOCKET_CHANNEL:
		case ADD_BLOCK_SOCKET_CHANNEL:
		case REMOVE_BLOCK_SOCKET_CHANNEL:
			nByteNum += 2*Integer.BYTES + m_strChannelName.getBytes().length;
			break;
		case ADD_NONBLOCK_SOCKET_CHANNEL_ACK:
		case ADD_BLOCK_SOCKET_CHANNEL_ACK:
		case REMOVE_BLOCK_SOCKET_CHANNEL_ACK:
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
			break;
		case LOGOUT:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		case LOGIN_ACK:
			m_bytes.putInt(m_bValidUser);
			m_bytes.putInt(m_strCommArch.getBytes().length);
			m_bytes.put(m_strCommArch.getBytes());
			m_bytes.putInt(m_bFileTransferScheme);
			m_bytes.putInt(m_bLoginScheme);
			m_bytes.putInt(m_bSessionScheme);
			m_bytes.putInt(m_nAttachDownloadScheme);
			m_bytes.putInt(m_nUDPPort);			// server udp port
			break;
		case REQUEST_SESSION_INFO:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
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
			break;
		case JOIN_SESSION:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
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
			break;
		case LEAVE_SESSION:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			break;
		case LEAVE_SESSION_ACK:
			m_bytes.putInt(m_nReturnCode);
			break;
		case SESSION_TALK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strTalk.getBytes().length);
			m_bytes.put(m_strTalk.getBytes());
			break;
		case SESSION_ADD_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strHostAddr.getBytes().length);
			m_bytes.put(m_strHostAddr.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			break;
		case SESSION_REMOVE_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		case CHANGE_SESSION:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strSessionName.getBytes().length);
			m_bytes.put(m_strSessionName.getBytes());
			break;
		case ADD_NONBLOCK_SOCKET_CHANNEL:
		case ADD_BLOCK_SOCKET_CHANNEL:
		case REMOVE_BLOCK_SOCKET_CHANNEL:
			m_bytes.putInt(m_strChannelName.getBytes().length);
			m_bytes.put(m_strChannelName.getBytes());
			m_bytes.putInt(m_nChannelNum);
			break;
		case ADD_NONBLOCK_SOCKET_CHANNEL_ACK:
		case ADD_BLOCK_SOCKET_CHANNEL_ACK:
		case REMOVE_BLOCK_SOCKET_CHANNEL_ACK:
			m_bytes.putInt(m_strChannelName.getBytes().length);
			m_bytes.put(m_strChannelName.getBytes());
			m_bytes.putInt(m_nChannelNum);
			m_bytes.putInt(m_nReturnCode);
			break;
		case REGISTER_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strPasswd.getBytes().length);
			m_bytes.put(m_strPasswd.getBytes());
			break;
		case REGISTER_USER_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strCreationTime.getBytes().length);
			m_bytes.put(m_strCreationTime.getBytes());
			break;
		case DEREGISTER_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strPasswd.getBytes().length);
			m_bytes.put(m_strPasswd.getBytes());
			break;
		case DEREGISTER_USER_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		case FIND_REGISTERED_USER:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			break;
		case FIND_REGISTERED_USER_ACK:
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strCreationTime.getBytes().length);
			m_bytes.put(m_strCreationTime.getBytes());
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
			break;
		case LOGOUT:
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		case LOGIN_ACK:
			m_bValidUser = msg.getInt();
			m_strCommArch = getStringFromByteBuffer(msg);
			m_bFileTransferScheme = msg.getInt();
			m_bLoginScheme = msg.getInt();
			m_bSessionScheme = msg.getInt();
			m_nAttachDownloadScheme = msg.getInt();
			m_nUDPPort = msg.getInt();
			break;
		case REQUEST_SESSION_INFO:
			m_strUserName = getStringFromByteBuffer(msg);
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
			break;
		case JOIN_SESSION:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
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
			break;
		case LEAVE_SESSION:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			break;
		case LEAVE_SESSION_ACK:
			m_nReturnCode = msg.getInt();
			break;
		case SESSION_TALK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strTalk = getStringFromByteBuffer(msg);
			break;
		case SESSION_ADD_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strHostAddr = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			break;
		case SESSION_REMOVE_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		case CHANGE_SESSION:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strSessionName = getStringFromByteBuffer(msg);
			break;
		case ADD_NONBLOCK_SOCKET_CHANNEL:
		case ADD_BLOCK_SOCKET_CHANNEL:
		case REMOVE_BLOCK_SOCKET_CHANNEL:
			m_strChannelName = getStringFromByteBuffer(msg);
			m_nChannelNum = msg.getInt();
			break;
		case ADD_NONBLOCK_SOCKET_CHANNEL_ACK:
		case ADD_BLOCK_SOCKET_CHANNEL_ACK:
		case REMOVE_BLOCK_SOCKET_CHANNEL_ACK:
			m_strChannelName = getStringFromByteBuffer(msg);
			m_nChannelNum = msg.getInt();
			m_nReturnCode = msg.getInt();
			break;
		case REGISTER_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strPasswd = getStringFromByteBuffer(msg);
			break;
		case REGISTER_USER_ACK:
			m_nReturnCode = msg.getInt();
			m_strUserName = getStringFromByteBuffer(msg);
			m_strCreationTime = getStringFromByteBuffer(msg);
			break;
		case DEREGISTER_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strPasswd = getStringFromByteBuffer(msg);
			break;
		case DEREGISTER_USER_ACK:
			m_nReturnCode = msg.getInt();
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		case FIND_REGISTERED_USER:
			m_strUserName = getStringFromByteBuffer(msg);
			break;
		case FIND_REGISTERED_USER_ACK:
			m_nReturnCode = msg.getInt();
			m_strUserName = getStringFromByteBuffer(msg);
			m_strCreationTime = getStringFromByteBuffer(msg);
			break;
		default:
			System.out.println("CMSessionEvent.unmarshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}
	}
}
