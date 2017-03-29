package kr.ac.konkuk.ccslab.cm;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * This class provides APIs, through which a client developer can access most of the communication 
 * services of CM.
 * A client application can use this class in order to request client-specific communication services.
 * 
 * @author mlim
 * @see CMStub, CMServerStub
 */
public class CMClientStub extends CMStub {

	public CMClientStub()
	{
		super();
	}

	/**
	 * Initializes and starts the client CM.
	 * <br> After the initialization process, the client CM also establishes a stream(TCP) connection to 
	 * the default server, makes a default datagram(UDP) channel.
	 *  
	 * @return true if the initialization of CM succeeds, or false if the initialization of CM fails.
	 * @see CMClientStub#terminateCM()
	 */
	public boolean startCM()
	{
		boolean bRet = false;
		
		// Korean encoding
		System.setProperty("file.encoding", "euc_kr");
		Field charset;
		try {
			charset = Charset.class.getDeclaredField("defaultCharset");
			charset.setAccessible(true);
			try {
				charset.set(null, null);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NoSuchFieldException | SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			CMConfigurator.init("cm-client.conf", m_cmInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		bRet = CMInteractionManager.init(m_cmInfo);
		if(!bRet)
		{
			return false;
		}
		CMEventManager.startReceivingEvent(m_cmInfo);
		CMCommManager.startReceivingMessage(m_cmInfo);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMClientStub.startCM(), succeeded.");
		
		return true;
	}
	
	/**
	 * Terminates the client CM.
	 * <br>A client application calls this method when it does not need to use CM. The client releases all 
	 * the resources, logs out from the server, and disconnects all communication channels.
	 * 
	 * @see CMClientStub#startCM()
	 */
	public void terminateCM()
	{
		super.terminateCM();

		if(CMInfo._CM_DEBUG)
			System.out.println("CMClientStub.terminateCM(), succeeded.");
	}
	
	/**
	 * Connects to the default server.
	 * <br> When a client application calls this method, the client CM opens a default stream(TCP)
	 * channel and connects to the server CM used by the default server application.
	 * 
	 * @return true if the connection is established successfully, or false otherwise.
	 * @see CMClientStub#disconnectFromServer()
	 */
	public boolean connectToServer()
	{
		return CMInteractionManager.connectDefaultServer(m_cmInfo);
	}
	
	/**
	 * Disconnects from the default server.
	 * <br> When a client application calls this method, the client CM tries to disconnect all the  
	 * stream(TCP) channels from the server CM used by the default server application.
	 * 
	 * @return true if the connection is successfully disconnected, or false otherwise.
	 * @see CMClientStub#connectToServer()
	 */
	public boolean disconnectFromServer()
	{
		return CMInteractionManager.disconnectFromDefaultServer(m_cmInfo);
	}
	
	/**
	 * Logs in to the default server.
	 * <br> For logging in to the server, the client first needs to register to the server by calling 
	 * registerUser() method.
	 * <p> The result of the login request can be caught asynchronously by the client event handler 
	 * that deals with all the incoming CM events from the server. To check whether the login request is 
	 * successful or not, the client event handler needs to catch the LOGIN_ACK event that belongs to 
	 * the {@link CMSessionEvent}. 
	 * In the LOGIN_ACK event, a result field of the Integer type is set, and the value can be retrieved by 
	 * the {@link CMSessionEvent#isValidUser()} method. If the value is 1, the login request successfully completes 
	 * and the requesting client is in the CM_LOGIN state. Otherwise, the login process fails.
	 * The LOGIN_ACK event also includes other CM information that can be returned by 
	 * {@link CMSessionEvent#getCommArch()}, {@link CMSessionEvent#isLoginScheme()}, and 
	 * {@link CMSessionEvent#isSessionScheme()}.
	 * <p> When the server CM accepts the login request from a client, the server CM also notifies other 
	 * participating clients of the information of the login user with the SESSION_ADD_USER event. 
	 * A client application can catch this event in the event handler routine if it wants to use such 
	 * information. The login user information is the user name and the host address that can be retrieved 
	 * by {@link CMSessionEvent#getUserName()} and {@link CMSessionEvent#getHostAddress()} methods, 
	 * respectively.
	 * 
	 * @param strUserName - the user name
	 * @param strPassword - the password
	 * @see {@link CMClientStub#logoutCM()}, {@link CMClientStub#registerUser(String, String)}
	 * 
	 */
	public void loginCM(String strUserName, String strPassword)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		
		// check local state
		int nUserState = interInfo.getMyself().getState();
		
		// If the user is not connected to the default server, he/she connects to it first.
		if(nUserState == CMInfo.CM_INIT)
		{
			CMInteractionManager.connectDefaultServer(m_cmInfo);
		}
		
		switch( nUserState )
		{
		//case CMInfo.CM_INIT:
			//System.out.println("You should connect to the default server before login.\n"); 
			//return;
		case CMInfo.CM_LOGIN:
		case CMInfo.CM_SESSION_JOIN:
			System.out.println("You already logged on the default server.\n"); 
			return;
		}
		
		String strMyAddr = confInfo.getMyAddress();		// client IP address
		int nMyUDPPort = confInfo.getUDPPort();			// client UDP port
		
		// make an event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.LOGIN);
		se.setUserName(strUserName);
		se.setPassword(strPassword);
		se.setHostAddress(strMyAddr);
		se.setUDPPort(nMyUDPPort);
		
		// set information on the local user
		CMUser myself = interInfo.getMyself();
		myself.setName(strUserName);
		myself.setPasswd(strPassword);
		myself.setHost(strMyAddr);
		myself.setUDPPort(nMyUDPPort);
		
		// send the event
		send(se, "SERVER");
		se = null;
		
		return;
	}
	
	/**
	 * Logs out from the default server.
	 * 
	 * <p> There is no result from the server about the logout request. 
	 * <p> When the server CM completes the logout request from a client, the server CM also notifies 
	 * other participating clients of the information of the logout user with the SESSION_REMOVE_USER event 
	 * of the {@link CMSessionEvent}. 
	 * A client application can catch this event in the event handler routine if it wants to use 
	 * such information. The logout user information is just the user name, which can be returned by 
	 * {@link CMSessionEvent#getUserName()} method.
	 * 
	 * @see {@link CMClientStub#loginCM()}, {@link CMClientStub#dereisterUser()}
	 */
	public void logoutCM()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		
		// check state of the local user
		CMUser myself = interInfo.getMyself();
		switch(myself.getState())
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect and log in to the default server."); return;
		case CMInfo.CM_CONNECT:
			System.out.println("You should log in to the default server."); return;
		}
		
		// terminate current group info (multicast channel, group member, Membership key)
		CMGroupManager.terminate(myself.getCurrentSession(), myself.getCurrentGroup(), m_cmInfo);

		// close and remove all additional channels to the default server
		interInfo.getDefaultServerInfo().getSocketChannelInfo().removeAllAddedChannels();
		
		// make and send an LOGOUT event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.LOGOUT);
		se.setUserName(myself.getName());
		send(se, "SERVER");
		
		// update local state
		myself.setState(CMInfo.CM_CONNECT);
		
		System.out.println("["+myself.getName()+"] logs out the default server.");
		
		se = null;
		return;
	}
	
	/**
	 * Requests available session information from the default server.
	 * <br> For requesting the session information, the client first needs to log in to the server by calling 
	 * loginCM() method. 
	 * 
	 * <p> The result of the session request can be caught asynchronously by the client event handler 
	 * that deals with all the incoming CM events from the server. To receive the available session 
	 * information, the client event handler needs to catch the RESPONSE_SESSION_INFO event that belongs to 
	 * the {@link CMSessionEvent}. 
	 * <br> The RESPONSE_SESSION_INFO event includes the number of available sessions and the vector of 
	 * the {@link CMSessionInfo}. Such event fields can be returned by 
	 * the {@link CMSessionEvent#getSessionNum()} and {@link CMSessionEvent#getSessionInfoList()}.
	 * <br> Each element of the CMSessionInfo object includes information of an available session such as 
	 * the session name, the session address and port number to which a client can join, and the current 
	 * number of session members who already joined the session.
	 * 
	 * @see {@link CMClientStub#joinSession(String)}, {@link CMClientStub#joinSession(String, String)}
	 */
	// request available session information from the default server
	public void requestSessionInfo()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		
		// check local state
		int nUserState = interInfo.getMyself().getState();
		if(nUserState == CMInfo.CM_INIT || nUserState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestSessionInfo(), you should log in to the default server.");
			return;
		}
		
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.REQUEST_SESSION_INFO);
		se.setUserName(interInfo.getMyself().getName());
		send(se, "SERVER");
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMClientStub.requestSessionInfo(), end.");
		se = null;
		return;
	}

	/**
	 * Joins a session in the default server.
	 * <br> For joining a session, the client first needs to log in to the server by calling 
	 * loginCM() method. The client can get available session information by calling 
	 * the requestSessionInfo() method.
	 * 
	 * <p> After the login process has completed, a client application must join a session and a group of 
	 * CM to finish entering the CM network. The session join process is different according to whether 
	 * the server CM adopts single session or multiple sessions in the CM server configuration file 
	 * (cm-server.conf).
	 * <br> After the client CM completes to join a session, it automatically proceeds to enter the first 
	 * group of the session. For example, if the client joins ¡°session1¡±, it also enters the group, ¡°g1¡±
	 * that is the first group of the session, ¡°session1¡±.
	 * 
	 * <p> When the server CM completes the session joining request from a client, the server CM also 
	 * notifies other participating clients of the information of the new session user with 
	 * the CHANGE_SESSION event of the {@link CMSessionEvent}. A client application can catch this event 
	 * in the event handler routine if it wants to use such information. The CHANGE_SESSION event includes 
	 * fields such as the user name and the session name, which can be returned by calling 
	 * the {@link CMSessionEvent#getUserName()} and the {@link CMSessionEvent#getSessionName()} methods, 
	 * respectively.
	 * 
	 * <p> When the server CM completes the group joining request from a client, the server CM also notifies 
	 * other participating clients of the information of the new group user with the NEW_USER event that 
	 * belongs to {@link CMDataEvent}.
	 * When the client CM receives this event, it stores the information of a new group user so that it 
	 * can figure out current group members later. A client application also can catch this event in 
	 * the event handler routine if it wants to use such information. The NEW_USER event includes fields 
	 * such as the current session name, the current group name, the name of the new group user, the host 
	 * address of the new group user, and the UDP port number of the new group user. Each event field can be 
	 * returned by calling the {@link CMDataEvent#getHandlerSession()}, {@link CMDataEvent#getHandlerGroup()}, 
	 * {@link CMDataEvent#getUserName()}, {@link CMDataEvent#getHostAddress()}, 
	 * and {@link CMDataEvent#getUDPPort()} methods, respectively.
	 * 
	 * <p> When the server CM completes the group joining request from a client, the server CM also notifies 
	 * the new user of the information of other existing group users with the series of INHABITANT events that 
	 * belong to {@link CMDataEvent}. 
	 * When the client CM receives this event, it stores the information of an existing group user so that 
	 * it can figure out current group members later. A client application also can catch this event 
	 * in the event handler routine if it wants to use such information. The INHABITANT event includes fields 
	 * such as the current session name, the current group name, the name of the new group user, the host 
	 * address of the new group user, and the UDP port number of the new group user. Each event field can be 
	 * returned by calling the {@link CMDataEvent#getHandlerSession()}, {@link CMDataEvent#getHandlerGroup()}, 
	 * {@link CMDataEvent#getUserName()}, {@link CMDataEvent#getHostAddress()}, 
	 * and {@link CMDataEvent#getUDPPort()} methods, respectively.
	 * 
	 * @param sname - the session name that a client requests to join
	 * @see {@link CMClientStub#joinSession(String, String)}
	 * @see {@link CMClientStub#leaveSession()}, {@link CMClientStub#leaveSession(String)}
	 */
	public void joinSession(String sname)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		
		// check local state
		switch( interInfo.getMyself().getState() )
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect and login server before session join.\n"); return;
		case CMInfo.CM_CONNECT:
			System.out.println("You should login server before session join..\n"); return;
		case CMInfo.CM_SESSION_JOIN:
			System.out.println("You have already joined a session.\n"); return;
		}
		
		// check selected session
		if( !interInfo.isMember(sname) )
		{
			System.out.println("session("+sname+") not found. You can request session information"
					+" from the default server.");
			return;
		}

		// make and send an event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.JOIN_SESSION);
		se.setHandlerSession(sname);
		se.setUserName(interInfo.getMyself().getName());
		se.setSessionName(sname);
		send(se, "SERVER");
		interInfo.getMyself().setCurrentSession(sname);
		
		se = null;
		return;
	}
	
	/**
	 * Leaves the current session in the default server.
	 * 
	 * <p> There is no result from the server about the session-leave request.
	 * 
	 * <p> Before leaving the current session, the server first remove the client from its current group. 
	 * The server notifies group members of the user leave by sending the REMOVE_USER event of 
	 * the {@link CMDataEvent}. The REMOVE_USER event includes the user name field, which can be returned 
	 * by the {@link CMDataEvent#getUserName()} method.
	 * 
	 * <p> When the server CM completes the session leaving request from a client, the server CM also 
	 * notifies other participating clients of the information of the leaving user with the CHANGE_SESSION 
	 * event of the {@link CMSessionEvent}. A client application can catch this event in the event handler 
	 * routine if it wants to use such information. The CHANGE_SESSION event includes 
	 * fields such as the user name and the session name, which can be returned by calling 
	 * the {@link CMSessionEvent#getUserName()} and the {@link CMSessionEvent#getSessionName()} methods, 
	 * respectively.
	 * If the session name field of this event is an empty space, a client can know that the user leaves 
	 * his/her current session. 
	 * 
	 * @see {@link CMClientStub#leaveSession(String)}
	 * @see {@link CMClientStub#joinSession(String)}, {@link CMClientStub#joinSession(String, String)}
	 */
	public void leaveSession()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		// check local state
		switch(myself.getState())
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect, log in to the default server, and join a session."); 
			return;
		case CMInfo.CM_CONNECT:
			System.out.println("You should log in to the default server and join a session.");
			return;
		case CMInfo.CM_LOGIN:
			System.out.println("You should join a session."); return;
		}
		
		// terminate current group info (multicast channel, group member, Membership key)
		CMGroupManager.terminate(myself.getCurrentSession(), myself.getCurrentGroup(), m_cmInfo);
		
		// send the leave request to the default server
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.LEAVE_SESSION);
		se.setHandlerSession(myself.getCurrentSession());
		se.setUserName(myself.getName());
		se.setSessionName(myself.getCurrentSession());
		send(se, "SERVER");
		
		// update the local state
		myself.setState(CMInfo.CM_LOGIN);
		
		System.out.println("["+myself.getName()+"] leaves session("+myself.getCurrentSession()+").");
		
		se = null;
		return;
	}
	
	/**
	 * Sends location information of the client to the group members.
	 * 
	 * <p> The location information consists of the position and orientation. The position is represented 
	 * by 3D coordinate (x,y,z). The orientation is represented by the quarternion (x,y,z,w) that includes 
	 * the rotation axis and the rotation angle.  
	 * @param pq - the new position and orientation of the client
	 * @see CMPosition
	 */
	// send position info to the group members
	public void sendUserPosition(CMPosition pq)
	{
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		// check user's local state
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.out.println("You should join a session and enter a group.");
			return;
		}
		
		// make and send an event
		CMInterestEvent ie = new CMInterestEvent();
		ie.setID(CMInterestEvent.USER_MOVE);
		ie.setHandlerSession(myself.getCurrentSession());
		ie.setHandlerGroup(myself.getCurrentGroup());
		ie.setDistributionSession(myself.getCurrentSession());
		ie.setDistributionGroup(myself.getCurrentGroup());
		ie.setUserName(myself.getName());
		ie.setPosition(pq);
		send(ie, "SERVER");
		
		// update user's current pq
		myself.setPosition(pq);
		return;
	}
	
	/**
	 * Sends a chat event.
	 * <p> A CM application can receive the chat event by catching a pre-defined CM event in the event 
	 * handler like other events. There are two types of CM chat events. One is the SESSION_TALK event of 
	 * the {@link CMSessionEvent} class. A client can receive this event if it at least logs in to the default 
	 * server. The SESSION_TALK event includes fields such as the sender name, the text message, and 
	 * the session name of the sender, which can be returned by calling {@link CMSessionEvent#getUserName()}, 
	 * {@link CMSessionEvent#getTalk()}, and {@link CMSessionEvent#getHandlerSession()} methods, respectively. 
	 * <br>The other event is the USER_TALK event of the {@link CMInterestEvent} class. A client can 
	 * receive this event only if it enters a group. The USER_TALK event includes fields such as the sender 
	 * name, the text message, the session name of the sender, and the group name of the sender, which can 
	 * be returned by calling {@link CMInterestEvent#getUserName()}, {@link CMInterestEvent#getTalk()}, 
	 * {@link CMInterestEvent#getHandlerSession()}, and {@link CMInterestEvent#getHandlerGroup()} methods, 
	 * respectively.
	 * 
	 * @param strTarget - the receiver name.
	 * <br>This parameter must start with ¡®/¡¯ character and it specifies the range of recipients of the chat 
	 * message as described below:
	 * <br> /b - The chat message is sent to the all login users.
	 * <br> /s - The chat message is sent to the all session members of the sending user.
	 * <br> /g - The chat message is sent to the all group members of the sending user.
	 * <br> /name - The chat message is sent to a specific CM node of which name is ¡®name¡¯. The name can be 
	 * another user name or a server name. If ¡®name¡¯ is SERVER, the message is sent to the default server.
	 * @param strMessage - the chat message.
	 */
	public void chat(String strTarget, String strMessage)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		
		// check target
		if(strTarget.equals("/b"))	// broadcast
		{
			if(myself.getState() == CMInfo.CM_CONNECT || myself.getState() == CMInfo.CM_INIT)
			{
				System.out.println("CMClientStub.chat(), You should log in to the default server"
						+" for broadcasting message.");
				return;
			}
			CMSessionEvent se = new CMSessionEvent();
			se.setID(CMSessionEvent.SESSION_TALK);
			se.setUserName(myself.getName());
			se.setTalk(strMessage);
			broadcast(se);
			se = null;
		}
		else if(strTarget.equals("/s"))	// cast to current session members
		{
			if(myself.getState() != CMInfo.CM_SESSION_JOIN)
			{
				System.out.println("CMClientStub.chat(), You should join a session.");
				return;
			}
			CMSessionEvent se = new CMSessionEvent();
			se.setID(CMSessionEvent.SESSION_TALK);
			se.setHandlerSession(myself.getCurrentSession());
			se.setUserName(myself.getName());
			se.setTalk(strMessage);
			cast(se, myself.getCurrentSession(), null);
			se = null;
		}
		else if(strTarget.equals("/g")) // cast to current group members
		{
			if(myself.getState() != CMInfo.CM_SESSION_JOIN)
			{
				System.out.println("CMClientStub.chat(), You should join a session.");
				return;
			}
			CMInterestEvent ie = new CMInterestEvent();
			ie.setID(CMInterestEvent.USER_TALK);
			ie.setHandlerSession(myself.getCurrentSession());
			ie.setHandlerGroup(myself.getCurrentGroup());
			ie.setUserName(myself.getName());
			ie.setTalk(strMessage);
			cast(ie, myself.getCurrentSession(), myself.getCurrentGroup());
			ie = null;
		}
		else	// send to a user of the current group
		{
			// check if the target user name field starts with '/'
			if(!strTarget.startsWith("/"))
			{
				System.out.println("CMClientStub.chat(), the name of target user must start with \"/\".");
				return;
			}
			
			strTarget = strTarget.substring(1); // without '/'
			CMInterestEvent ie = new CMInterestEvent();
			ie.setID(CMInterestEvent.USER_TALK);
			ie.setHandlerSession(myself.getCurrentSession());
			ie.setHandlerGroup(myself.getCurrentGroup());
			ie.setUserName(myself.getName());
			ie.setTalk(strMessage);
			send(ie, strTarget);
			ie = null;
		}
		
		return;
	}
	
	/**
	 * Changes the current group of the client.
	 * 
	 * <p> When a client calls this method, the client first leaves the current group and then requests to 
	 * enter a new group. The CM server notifies previous group members of the left user by sending 
	 * the REMOVE_USER event of the {@link CMDataEvent}, and the server also 
	 * notifies new group members of the new user by sending the NEW_USER event of the {@link CMDataEvent}. 
	 * The server also notifies the changing user of the existing member information of the new group by 
	 * sending the INHABITANT event of the {@link CMDataEvent}.
	 * 
	 * @param gName - the name of a group that the client wants to enter.
	 * @see CMClientStub#joinSession(String)
	 */
	public void changeGroup(String gName)
	{
		CMGroupManager.changeGroup(gName, m_cmInfo);
		return;
	}
	
	/**
	 * Adds a TCP channel to a server.
	 * 
	 * @param nChIndex - the channel index which must be greater than 0.
	 * The index 0 is occupied by the default TCP channel.
	 * @param strServer - the server name to which the client adds a TCP channel. The name of the default 
	 * server is 'SERVER'.
	 * 
	 * @see CMClientStub#removeAdditionalSocketChannel(int, String)
	 */
	public void addSocketChannel(int nChIndex, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		SocketChannel sc = null;
		CMChannelInfo scInfo = null;
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.out.println("CMClientStub.addSocketChannel(), server("+strServer+") not found.");
				return;
			}			
		}
		
		try {
			scInfo = serverInfo.getSocketChannelInfo();
			sc = (SocketChannel) scInfo.findChannel(nChIndex);
			if(sc != null)
			{
				System.out.println("CMClientStub.addSocketChannel(), channel index("+nChIndex
						+") already exists.");
				return;
			}
			
			sc = (SocketChannel) CMCommManager.openChannel(CMInfo.CM_SOCKET_CHANNEL, serverInfo.getServerAddress()
									, serverInfo.getServerPort(), m_cmInfo);
			if(sc == null)
			{
				System.out.println("CMClientStub.addSocketChannel(), failed.");
				return;
			}
			scInfo.addChannel(sc, nChIndex);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.ADD_CHANNEL);
		se.setChannelName(interInfo.getMyself().getName());
		se.setChannelNum(nChIndex);
		send(se, strServer, CMInfo.CM_STREAM, nChIndex);
		
		se = null;
		return;
	}
	
	/**
	 * Removes an additional TCP channel from a server.
	 * 
	 * @param nChIndex - the index of the channel that is to be removed. The index must be greater than 0. 
	 * If the default channel (0) is removed, the result is undefined. 
	 * @param strServer - the server name from which the additional channel is removed.
	 * @see CMClientStub#addSocketChannel(int, String)
	 */
	public void removeAdditionalSocketChannel(int nChIndex, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMChannelInfo scInfo = null;
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.out.println("CMClientStub.removeAdditionalSocketChannel(), server("+strServer+") not found.");
				return;
			}			
		}
		
		scInfo = serverInfo.getSocketChannelInfo();
		scInfo.removeChannel(nChIndex);
		return;
	}

	/**
	 * 
	 * (from here)
	 * @param strUser
	 * @param strWriter
	 * @param nOffset
	 */
	/* 
	 * To request to download the list of SNS content.
	 * strUser: requester name
	 * strWriter: writer name (empty string for any writer)
	 * nOffset: content offset (0 <= nOffset < total number of required content items), (0 for the most recent content) 
	 */
	public void requestSNSContent(String strUser, String strWriter, int nOffset)
	{
		int nState = m_cmInfo.getInteractionInfo().getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT )
		{
			System.out.println("CCMClientStub::requestSNSContents(), you must log in to the default server!");
			return;
		}
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.CONTENT_DOWNLOAD_REQUEST);
		se.setUserName(strUser);
		se.setWriterName(strWriter);
		se.setContentOffset(nOffset);
		send(se, "SERVER");

		se = null;
		return;
	}

	/*
	 * To request to download the next list of SNS content from the previous request.
	 */
	public void requestNextSNSContent()
	{
		String strUser = m_cmInfo.getInteractionInfo().getMyself().getName();
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		// get the saved data
		String strWriter = snsInfo.getLastlyReqWriter();
		int nOffset = snsInfo.getLastlyReqOffset();
		int nDownContentNum = snsInfo.getLastlyDownContentNum();
		
		// update next content offset
		nOffset = nOffset+nDownContentNum;
		
		// request SNS content
		requestSNSContent(strUser, strWriter, nOffset);
		
		return;
	}
	
	/*
	 * To request to download the previous list of SNS content from the previous request.
	 */
	public void requestPreviousSNSContent()
	{
		String strUser = m_cmInfo.getInteractionInfo().getMyself().getName();
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		// get the saved data
		String strWriter = snsInfo.getLastlyReqWriter();
		int nOffset = snsInfo.getLastlyReqOffset();
		int nDownContentNum = snsInfo.getLastlyDownContentNum();
		
		// update next content offset
		nOffset = nOffset-nDownContentNum;
		//if(nOffset < 0) nOffset = 0;
		
		// request SNS content
		requestSNSContent(strUser, strWriter, nOffset);
		
		return;
	}
	
	public void requestSNSContentUpload(String user, String message, int nNumAttachedFiles, 
			int nReplyOf, int nLevelOfDisclosure, ArrayList<String> filePathList)
	{
		ArrayList<String> fileNameList = null;
		int i = -1;
		int nState = m_cmInfo.getInteractionInfo().getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT )
		{
			System.out.println("CMClientStub.requestSNSContentUpload(), you must log in to the default server!");
			return;
		}
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.CONTENT_UPLOAD_REQUEST);
		se.setUserName(user);
		se.setMessage(message);
		se.setNumAttachedFiles(nNumAttachedFiles);
		se.setReplyOf(nReplyOf);
		se.setLevelOfDisclosure(nLevelOfDisclosure);
		if(nNumAttachedFiles > 0 && filePathList != null)
		{
			// check the number and the real number of elements
			if(nNumAttachedFiles != filePathList.size())
			{
				System.out.println("CMClientStub.requestSNSContentUpload(), the number of attached files "
						+ "are not consistent!");
				return;
			}
			
			// check if files exist or not
			for(i = 0; i < filePathList.size(); i++)
			{
				String strFile = filePathList.get(i);
				File attachFile = new File(strFile);
				if(!attachFile.exists())
				{
					System.out.println("CMClientStub.requestSNSContentUpload(), file("+strFile+") not found!");
					return;
				}
			}
			
			// store the file path list in the CMSNSInfo class (CMSNSAttach object)
			CMSNSInfo sInfo = m_cmInfo.getSNSInfo();
			CMSNSAttach attachToBeSent = sInfo.getSNSAttachToBeSent();
			attachToBeSent.setFilePathList(filePathList);
			
			// create a file name list with the given file path list
			fileNameList = new ArrayList<String>();
			for(i = 0; i < filePathList.size(); i++)
			{
				String strFilePath = filePathList.get(i);
				int startIndex = strFilePath.lastIndexOf("/");
				String strFileName = strFilePath.substring(startIndex+1);
				fileNameList.add(strFileName);
				System.out.println("attached file name: "+strFileName);
			}
			
			se.setFileNameList(fileNameList);
		}
		send(se, "SERVER");

		se = null;
		return;
	}
	
	public boolean requestAttachedFileOfSNSContent(String strFileName)
	{
		int nContentID = -1;
		String strWriterName = null;
		// A downloaded file name may be a thumbnail file name instead of original name
		int index = strFileName.lastIndexOf(".");
		String strThumbnail = strFileName.substring(0, index) + "-thumbnail"
				+ strFileName.substring(index, strFileName.length());
		// search for content ID and writer name
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		CMSNSContentList contentList = snsInfo.getSNSContentList();
		Vector<CMSNSContent> contentVector = contentList.getContentList();
		Iterator<CMSNSContent> iter = contentVector.iterator();
		boolean bFound = false;
		while(iter.hasNext() && !bFound)
		{
			CMSNSContent content = iter.next();
			if(content.containsFileName(strFileName) || content.containsFileName(strThumbnail))
			{
				nContentID = content.getContentID();
				strWriterName = content.getWriterName();
				bFound = true;
			}
		}		
		
		if(bFound)
		{
			// send request for the attachment download
			requestAttachedFileOfSNSContent(nContentID, strWriterName, strFileName);			
		}
		else
		{
			System.err.println("CMClientStub.requestAttachedFileOfSNSContent(), "
					+strFileName+" not found in the downloaded content list!\n");
			return false;
		}

		return true;
	}
	
	public void requestAttachedFileOfSNSContent(int nContentID, String strWriterName, String strFileName)
	{
		String strUserName = m_cmInfo.getInteractionInfo().getMyself().getName();
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_ATTACHED_FILE);
		se.setUserName(strUserName);
		se.setContentID(nContentID);
		se.setWriterName(strWriterName);
		se.setFileName(strFileName);
		send(se, "SERVER");
		
		se = null;
		return;
	}

	// find the downloaded content and inform the server that the attached file is accessed by the client
	public boolean accessAttachedFileOfSNSContent(String strFileName)
	{
		int nContentID = -1;
		String strWriterName = null;
		// A downloaded file name may be a thumbnail file name instead of original name
		int index = strFileName.lastIndexOf(".");
		String strThumbnail = strFileName.substring(0, index) + "-thumbnail"
				+ strFileName.substring(index, strFileName.length());
		// search for content ID and writer name
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		CMSNSContentList contentList = snsInfo.getSNSContentList();
		Vector<CMSNSContent> contentVector = contentList.getContentList();
		Iterator<CMSNSContent> iter = contentVector.iterator();
		boolean bFound = false;
		while(iter.hasNext() && !bFound)
		{
			CMSNSContent content = iter.next();
			if(content.containsFileName(strFileName) || content.containsFileName(strThumbnail))
			{
				nContentID = content.getContentID();
				strWriterName = content.getWriterName();
				bFound = true;
			}
		}		
		
		if(bFound)
		{
			// send request for the attachment download
			accessAttachedFileOfSNSContent(nContentID, strWriterName, strFileName);			
		}
		else
		{
			System.err.println("CMClientStub.accessAttachedFileOfSNSContent(), "
					+strFileName+" not found in the downloaded content list!\n");
			return false;
		}

		return true;
	}

	public void accessAttachedFileOfSNSContent(int nContentID, String strWriterName, String strFileName)
	{
		String strUserName = m_cmInfo.getInteractionInfo().getMyself().getName();
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.ACCESS_ATTACHED_FILE);
		se.setUserName(strUserName);
		se.setContentID(nContentID);
		se.setWriterName(strWriterName);
		se.setFileName(strFileName);
		send(se, "SERVER");
		
		se = null;
		return;
	}

	public void requestServerInfo()
	{
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		int state = myself.getState();

		if( state == CMInfo.CM_INIT || state == CMInfo.CM_CONNECT )
		{
			System.out.println("CMClientStub.requestServerInfo(), You should login the default server.");
			return;
		}

		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.REQ_SERVER_INFO);
		mse.setUserName( myself.getName() );
		send( mse, "SERVER" );

		if(CMInfo._CM_DEBUG)
			System.out.println("CMClientStub::requestServerInfo(), end.");
	
		mse = null;
		return;
	}
	
	public boolean connectToServer(String strServerName)
	{
		
		boolean ret = false;
		if( strServerName.equals("SERVER") )	// if a default server
		{
			ret = CMInteractionManager.connectDefaultServer(m_cmInfo);
			return ret;
		}
		
		ret = CMInteractionManager.connectAddServer(strServerName, m_cmInfo);
		return ret;
	}
	
	public boolean disconnectFromServer(String strServerName)
	{
		boolean ret = false;
		if( strServerName.equals("SERVER") )	// if a default server
		{
			ret = CMInteractionManager.disconnectFromDefaultServer(m_cmInfo);
			return ret;
		}

		ret = CMInteractionManager.disconnectFromAddServer(strServerName, m_cmInfo);
		return ret;
	}
	
	public void loginCM(String strServer, String strUser, String strPasswd)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMServer tserver = null;
		String myAddress = null;
		int myUDPPort = -1;
		
		// if a server is the default server, call the original function.
		if( strServer.equals("SERVER") )
		{
			loginCM(strUser, strPasswd);
			return;
		}

		// get a server info
		tserver = interInfo.findAddServer(strServer);
		if( tserver == null )
		{
			System.out.println("CMClientStub.loginCM(..), server("+strServer+") not found!");
			return;
		}

		// check local state
		
		// If the client is not connected to the server, he/she connects to it first.
		if(tserver.getClientState() == CMInfo.CM_INIT)
		{
			CMInteractionManager.connectAddServer(strServer, m_cmInfo);
		}
		
		switch( tserver.getClientState() )
		{
		//case CMInfo.CM_INIT:
			//System.out.println("You should connect to server("+strServer+") before login."); 
			//return;
		case CMInfo.CM_LOGIN:
		case CMInfo.CM_SESSION_JOIN:
			System.out.println("You already logged in to server("+strServer+").");
			return;
		}

		// get my ip address and port
		myAddress = confInfo.getMyAddress();
		myUDPPort = confInfo.getUDPPort();
		
		// make an event
		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.ADD_LOGIN);
		mse.setServerName(strServer);
		mse.setUserName(strUser);
		mse.setPassword(strPasswd);
		mse.setHostAddress(myAddress);
		mse.setUDPPort(myUDPPort);

		// send the event
		send(mse, strServer);

		mse = null;
		return;
	}
	
	public void logoutCM(String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		
		// if a server is the default server, call the original function.
		if(strServer.equals("SERVER"))
		{
			logoutCM();
			return;
		}

		CMServer tserver = interInfo.findAddServer(strServer);
		if( tserver == null )
		{
			System.out.println("CMClientStub.logoutCM(..), server("+strServer+") info not found "
					+ "in the add-server list!");
			return;
		}

		// check state of the client of the server
		switch( tserver.getClientState() )
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect and log in to the server("+strServer+")."); 
			return;
		case CMInfo.CM_CONNECT:
			System.out.println("You should log in to the server("+strServer+").");
			return;
		}

		// remove and close all additional channels in EM and CM
		CMChannelInfo chInfo = tserver.getSocketChannelInfo();
		chInfo.removeAllAddedChannels();

		// make and send event
		CMMultiServerEvent tmse = new CMMultiServerEvent();
		tmse.setID(CMMultiServerEvent.ADD_LOGOUT);
		tmse.setServerName(strServer);
		tmse.setUserName(interInfo.getMyself().getName());

		send(tmse, strServer);

		// update the local state of the server
		tserver.setClientState(CMInfo.CM_CONNECT);

		tmse = null;
		return;
	}
	
	// requests available session information of a designated server
	public void requestSessionInfo(String strServerName)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer tserver = null;

		// if a server is the default server, call the original function
		if( strServerName.equals("SERVER") )
		{
			requestSessionInfo();
			return;
		}

		// find a server info
		tserver = interInfo.findAddServer(strServerName);
		if( tserver == null )
		{
			System.out.println("CMClientStub.requestSessionInfo(..), server("+strServerName
					+") info not found in the add-server list.");
			return;
		}

		int	state = tserver.getClientState();

		if( state == CMInfo.CM_INIT || state == CMInfo.CM_CONNECT )
		{
			System.out.println("CMClientStub.requestSessionInfo(..), You should login the server("
					+strServerName+").");
			return;
		}

		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.ADD_REQUEST_SESSION_INFO);
		mse.setUserName(interInfo.getMyself().getName());
		send(mse, strServerName);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMClientStub.requestSessionInfo(..) server("+strServerName+"), Ok");
		}
	
		mse = null;
		return;
	}
	
	public void joinSession(String strServer, String strSession)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer tserver = null;
		CMSession tsession = null;
		
		// if a server is the default server, call the original function
		if( strServer.equals("SERVER") )
		{
			joinSession(strSession);
			return;
		}

		// find a server info
		tserver = interInfo.findAddServer(strServer);
		if( tserver == null )
		{
			System.out.println("CMClientStub.joinSession(..), server("+strServer+") info "
					+ "not found in the add-server list!");
			return;
		}
		
		// check local client state of the server
		switch( tserver.getClientState() )
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect and login server("+strServer+")."); 
			return;
		case CMInfo.CM_CONNECT:
			System.out.println("You should login server("+strServer+")."); 
			return;
		case CMInfo.CM_SESSION_JOIN:
			System.out.println("You have already joined a session of a server("+strServer+")."); 
			return;
		}

		// check selected session
		tsession = tserver.findSession(strSession);
		if(tsession == null)
		{
			System.out.println("CMClientStub.joinSession(..), session("+strSession+") info of server("
					+strServer+") not found! Request session info first!");
			return;
		}
		
		// make event
		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.ADD_JOIN_SESSION);
		mse.setServerName(strServer);
		mse.setUserName(interInfo.getMyself().getName());
		mse.setSessionName(strSession);

		// send the event
		send(mse, strServer);
		// set current session of the client in the server info element
		tserver.setCurrentSessionName(strSession);

		mse = null;
		return;
	}
	
	public void leaveSession(String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer tserver = null;
		
		// if a server is the default server, call the original function
		if( strServer.equals("SERVER") )
		{
			leaveSession();
			return;
		}

		// find a server info
		tserver = interInfo.findAddServer(strServer);
		if(tserver == null)
		{
			System.out.println("CMClientStub.leaveSession(..), server("+strServer+") info not found "
					+ "in the add-server list!");
			return;
		}

		// check the client state of the server
		switch( tserver.getClientState() )
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect, log in to server("+strServer+") and join a session.");
			return;
		case CMInfo.CM_CONNECT:
			System.out.println("You should log in to server("+strServer+") and join a session.");
			return;
		case CMInfo.CM_LOGIN:
			System.out.println("You should join a session of the server("+strServer+").");
			return;
		}

		// terminate current group info (multicast channel, group member, Membership key)
		CMGroupManager.terminate(tserver.getCurrentSessionName(), tserver.getCurrentGroupName(), m_cmInfo);

		// make and send event
		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.ADD_LEAVE_SESSION);
		mse.setServerName(tserver.getServerName());
		mse.setUserName(interInfo.getMyself().getName());
		mse.setSessionName( tserver.getCurrentSessionName() );

		send(mse, strServer);

		// update the client state of the server
		tserver.setClientState(CMInfo.CM_LOGIN);

		mse = null;
		return;
	}
	
	public void registerUser(String strName, String strPasswd)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT )
		{
			System.out.println("CMClientStub.registerUser(), client is not connected to "
					+ "the default server!");
			return;
		}

		// make a request event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.REGISTER_USER);
		se.setUserName(strName);
		se.setPassword(strPasswd);

		// send the request (a default server will send back REGISTER_USER_ACK event)
		send(se, "SERVER");

		if(CMInfo._CM_DEBUG)
			System.out.println("CMClientStub.registerUser(), user("+strName+") requested.");

		se = null;
		return;
	}
	
	public void deregisterUser(String strName, String strPasswd)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT )
		{
			System.out.println("CMClientStub.deregisterUser(), client is not connected to "
					+ "the default server!");
			return;
		}

		// make a request event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.DEREGISTER_USER);
		se.setUserName(strName);
		se.setPassword(strPasswd);

		// send the request (a default server will send back DEREGISTER_USER_ACK event)
		send(se, "SERVER");

		if(CMInfo._CM_DEBUG)
		{
			System.out.printf("CMClientStub.deregisterUser(), user("+strName+") requested.");
		}

		se = null;
		return;
	}
	
	public void findRegisteredUser(String strName)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT )
		{
			System.out.println("CMClientStub.findRegisteredUser(), client is not connected to "
					+ "the default server!");
			return;
		}

		// make a request event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.FIND_REGISTERED_USER);
		se.setUserName(strName);

		// send the request (a default server will send back FIND_REGISTERED_USER_ACK event)
		send(se, "SERVER");

		if(CMInfo._CM_DEBUG)
		{
			System.out.printf("CMClientStub.findRegisteredUser(), user("+strName+") requested.");
		}

		se = null;
		return;
	}
	
	public void addNewFriend(String strFriendName)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.addNewFriend(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.ADD_NEW_FRIEND);
		se.setUserName(interInfo.getMyself().getName());
		se.setFriendName(strFriendName);
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	public void removeFriend(String strFriendName)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.removeFriend(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REMOVE_FRIEND);
		se.setUserName(interInfo.getMyself().getName());
		se.setFriendName(strFriendName);
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	public void requestFriendsList()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestFriendsList(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_FRIEND_LIST);
		se.setUserName(interInfo.getMyself().getName());
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	public void requestFriendRequestersList()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestFriendRequestersList(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_FRIEND_REQUESTER_LIST);
		se.setUserName(interInfo.getMyself().getName());
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	public void requestBiFriendsList()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		int nState = -1;

		// check if the user is connected to a default server
		nState = interInfo.getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestBiFriendsList(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_BI_FRIEND_LIST);
		se.setUserName(interInfo.getMyself().getName());
		send(se, "SERVER");
		
		se = null;
		return;
	}
}
