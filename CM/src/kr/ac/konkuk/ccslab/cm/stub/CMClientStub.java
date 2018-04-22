package kr.ac.konkuk.ccslab.cm.stub;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMPosition;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMServerInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.entity.CMSessionInfo;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.CMDataEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMEventInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMSNSInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMGroupManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttach;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContent;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContentList;

/**
 * This class provides APIs, through which a client developer can access most of the communication 
 * services of CM.
 * A client application can use this class in order to request client-specific communication services.
 * 
 * @author mlim
 * @see CMStub 
 * @see CMServerStub
 */
public class CMClientStub extends CMStub {

	/**
	 * Creates an instance of the CMClientStub class.
	 * 
	 * <p> This method just called the default constructor of the super class, CMStub. 
	 */
	public CMClientStub()
	{
		super();
	}

	/**
	 * Initializes and starts the client CM.
	 * 
	 * <p> Before the server CM starts, it initializes the configuration and the interaction manager. Then, 
	 * it starts two separate threads for receiving and processing CM events.
	 * <br> After the initialization process, the client CM also establishes a stream(TCP) connection to 
	 * the default server and makes a default datagram(UDP) channel.
	 * 
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
		CMCommManager.startSendingMessage(m_cmInfo);
		
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
	 * 
	 * <p> When a client application calls this method, the client CM opens a default stream(TCP)
	 * channel and connects to the server CM used by the default server application.
	 * <br> When the CM client starts by calling the {@link CMClientStub#startCM()} method, it connects 
	 * to the default server ("SERVER") as one of the initialization tasks.
	 * Before the client logs in to the default CM server, it must be connected to the server by calling this method.
	 * The connection to the default server is made with the default TCP channel.
	 * 
	 * @return true if the connection is established successfully, or false otherwise.
	 * @see CMClientStub#connectToServer(String)
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
	 * @see CMClientStub#disconnectFromServer(String)
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
	 * {@link CMSessionEvent#isSessionScheme()}. The detailed information of the LOGIN_ACK event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.LOGIN_ACK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.LOGIN_ACK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>User validity</td><td>int</td><td>1:valid user, 0:invalid user</td><td>isValidUser()</td>
	 * </tr>
	 * <tr>
	 * <td>Communication architecture</td><td>String</td>
	 * <td>
	 * Specified communication architecture
	 * <br>CM_CS: client-server model
	 * <br>CM_PS: client-server with multicast model
	 * </td>
	 * <td>getCommArch()</td>
	 * </tr>
	 * <tr>
	 * <td>Login scheme</td><td>int</td><td>1: user authentication used, 0: no user authentication</td><td>isLoginScheme()</td>
	 * </tr>
	 * <tr>
	 * <td>Session scheme</td><td>int</td><td>1: multiple sessions used, 0: single session used</td><td>isSessionScheme()</td>
	 * </tr>
	 * </table>
	 * 
	 * <p> When the server CM accepts the login request from a client, the server CM also notifies other 
	 * participating clients of the information of the login user with the SESSION_ADD_USER event. 
	 * A client application can catch this event in the event handler routine if it wants to use such 
	 * information. The login user information is the user name and the host address that can be retrieved 
	 * by {@link CMSessionEvent#getUserName()} and {@link CMSessionEvent#getHostAddress()} methods, respectively.
	 * The detailed information of the SESSION_ADD_USER event is shown below.
	 * 
	 * <table border=1>
	 * <caption>CMsessionEvent.SESSION_ADD_USER event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.SESSION_ADD_USER</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Name of the login user</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Host address</td><td>String</td><td>Host address of the login user</td><td>getHostAddress()</td>
	 * </tr>
	 * </table>
	 * 
	 * @param strUserName - the user name
	 * @param strPassword - the password
	 * @return true if the request is successfully sent to the server; false otherwise.
	 * @see CMClientStub#syncLoginCM(String, String)
	 * @see CMClientStub#loginCM(String, String, String)
	 * @see CMClientStub#logoutCM()
	 * @see CMClientStub#registerUser(String, String)
	 * 
	 */
	public boolean loginCM(String strUserName, String strPassword)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		boolean bRequestResult = false;
		
		// check local state
		int nUserState = getMyself().getState();
		
		// If the user is not connected to the default server, he/she connects to it first.
		if(nUserState == CMInfo.CM_INIT)
		{
			CMInteractionManager.connectDefaultServer(m_cmInfo);
		}
		
		switch( nUserState )
		{
		case CMInfo.CM_LOGIN:
		case CMInfo.CM_SESSION_JOIN:
			System.out.println("You already logged in to the default server."); 
			return false;
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
		CMUser myself = getMyself();
		myself.setName(strUserName);
		myself.setPasswd(strPassword);
		myself.setHost(strMyAddr);
		myself.setUDPPort(nMyUDPPort);
		
		// send the event
		bRequestResult = send(se, "SERVER");
		se = null;
		
		return bRequestResult;
	}
	
	/**
	 * Logs in to the default server synchronously.
	 * 
	 * <p> Unlike the asynchronous login method ({@link CMClientStub#loginCM(String, String)}), 
	 * this method makes the main thread of the client block its execution until it receives and 
	 * returns the reply event (CMSessionEvent.LOGIN_ACK) from the default server.
	 * <br> For the other detailed information of the login process, please refer to 
	 * the asynchronous login method.
	 * 
	 * @param strUserName - the user name
	 * @param strPassword - the password
	 * @return the reply event (CMSessionEvent.LOGIN_ACK) from the default server.
	 * @see CMClientStub#loginCM(String, String)
	 */
	public CMSessionEvent syncLoginCM(String strUserName, String strPassword)
	{
		CMEventSynchronizer eventSync = m_cmInfo.getEventInfo().getEventSynchronizer();
		CMSessionEvent loginAckEvent = null;
		boolean bRequestResult = false;
		
		bRequestResult = loginCM(strUserName, strPassword);
		if(!bRequestResult) return null;
		
		eventSync.setWaitingEvent(CMInfo.CM_SESSION_EVENT, CMSessionEvent.LOGIN_ACK);
		synchronized(eventSync)
		{
			while(loginAckEvent == null)
			{
				try {
					eventSync.wait(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				loginAckEvent = (CMSessionEvent) eventSync.getReplyEvent();
			}
		}
		eventSync.init();
		
		return loginAckEvent;
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
	 * {@link CMSessionEvent#getUserName()} method. The detailed information of the SESSION_REMOVE_USER event 
	 * is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.SESSION_REMOVE_USER event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.SESSION_REMOVE_USER</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>user name</td><td>String</td><td>Name of the logout user</td><td>getUserName()</td>
	 * </tr>
	 * </table>
	 * 
	 * @return true if successfully sent the logout request, false otherwise.
	 * @see CMClientStub#loginCM(String, String)
	 * @see CMClientStub#deregisterUser(String, String)
	 */
	public boolean logoutCM()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		boolean bRequestResult = false;
		
		// check state of the local user
		CMUser myself = getMyself();
		switch(myself.getState())
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect and log in to the default server."); return false;
		case CMInfo.CM_CONNECT:
			System.out.println("You should log in to the default server."); return false;
		}
		
		// terminate current group info (multicast channel, group member, Membership key)
		CMGroupManager.terminate(myself.getCurrentSession(), myself.getCurrentGroup(), m_cmInfo);

		// close and remove all additional channels to the default server
		interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo().removeAllAddedChannels(0);
		interInfo.getDefaultServerInfo().getBlockSocketChannelInfo().removeAllChannels();
		
		// make and send an LOGOUT event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.LOGOUT);
		se.setUserName(myself.getName());
		bRequestResult = send(se, "SERVER");
		
		// update local state
		myself.setState(CMInfo.CM_CONNECT);
		if(bRequestResult)
			System.out.println("["+myself.getName()+"] successfully sent the logout request to the default server.");
		else
			System.err.println("["+myself.getName()+"] failed the logout request!");
		
		se = null;
		return bRequestResult;
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
	 * <br> The detailed information of the RESPONSE_SESSION_INFO event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.RESPONSE_SESSION_INFO event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.RESPONSE_SESSION_INFO</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Number of sessions</td><td>int</td><td>Number of sessions</td><td>getSessionNum()</td>
	 * </tr>
	 * <tr>
	 * <td>Vector of sessions</td><td>Vector&lt;CMSessionInfo&gt;</td><td>List of session information</td>
	 * <td>getSessionInfoList()</td>
	 * </tr>
	 * </table>
	 * 
	 * @return true if the request is successfully sent to the server; false otherwise.
	 * @see CMClientStub#syncRequestSessionInfo()
	 * @see CMClientStub#joinSession(String)
	 * @see CMClientStub#joinSession(String, String)
	 */
	// request available session information from the default server
	public boolean requestSessionInfo()
	{
		boolean bRequestResult = false;
		
		// check local state
		int nUserState = getMyself().getState();
		if(nUserState == CMInfo.CM_INIT || nUserState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestSessionInfo(), you should log in to the default server.");
			return false;
		}
		
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.REQUEST_SESSION_INFO);
		se.setUserName(getMyself().getName());
		bRequestResult = send(se, "SERVER");
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMClientStub.requestSessionInfo(), end.");
		se = null;
		return bRequestResult;
	}
	
	/**
	 * Requests available session information from the default server synchronously.
	 * 
	 * <p> Unlike the asynchronous method ({@link CMClientStub#requestSessionInfo()}), this method makes 
	 * the main thread of the client block its execution until it receives and returns the reply event 
	 * (CMSessionEvent.RESPONSE_SESSION_INFO) from the default server.
	 * <br> For the other detailed information of the session-information-request process, 
	 * please refer to the asynchronous version of the request. 
	 * 
	 * @return the reply event (CMSessionEvent.RESPONSE_SESSION_INFO) from the default server.
	 * @see CMClientStub#requestSessionInfo()
	 */
	public CMSessionEvent syncRequestSessionInfo()
	{
		CMEventSynchronizer eventSync = m_cmInfo.getEventInfo().getEventSynchronizer();
		CMSessionEvent replyEvent = null;
		boolean bRequestResult = false;
		
		bRequestResult = requestSessionInfo();
		if(!bRequestResult) return null;
		
		eventSync.setWaitingEvent(CMInfo.CM_SESSION_EVENT, CMSessionEvent.RESPONSE_SESSION_INFO);
		synchronized(eventSync)
		{
			while(replyEvent == null)
			{
				try {
					eventSync.wait(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				replyEvent = (CMSessionEvent) eventSync.getReplyEvent();
			}
		}
		eventSync.init();
		
		return replyEvent;
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
	 * group of the session. For example, if the client joins &quot;session1&quot;, it also enters the group, &quot;g1&quot;
	 * that is the first group of the session, &quot;session1&quot;.
	 * 
	 * <p> When the server CM completes the session joining request from a client, the server CM also 
	 * notifies other participating clients of the information of the new session user with 
	 * the CHANGE_SESSION event of the {@link CMSessionEvent}. A client application can catch this event 
	 * in the event handler routine if it wants to use such information. The CHANGE_SESSION event includes 
	 * fields such as the user name and the session name, which can be returned by calling 
	 * the {@link CMSessionEvent#getUserName()} and the {@link CMSessionEvent#getSessionName()} methods, 
	 * respectively. The detailed information of the CHANGE_SESSION event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.CHANGE_SESSION event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.CHANGE_SESSION</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Name of a user who joins a session</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Session name</td><td>String</td><td>Name of a session which the user joins</td><td>getSessionName()</td>
	 * </tr>
	 * </table>
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
	 * and {@link CMDataEvent#getUDPPort()} methods, respectively. The detailed information of the NEW_USER event 
	 * is described below.
	 * 
	 * <table border=1>
	 * <caption>CMDataEvent.NEW_USER event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_DATA_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMDataEvent.NEW_USER</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>current session</td><td>String</td><td>current session name of the user</td><td>getHandlerSession()</td>
	 * </tr>
	 * <tr>
	 * <td>current group</td><td>String</td><td>current group name of the user</td><td>getHandlerGroup()</td>
	 * </tr>
	 * <tr>
	 * <td>user name</td><td>String</td><td>name of the new group user</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>host address</td><td>String</td><td>host address of the new group user</td><td>getHostAddress()</td>
	 * </tr>
	 * <tr>
	 * <td>UDP port number</td><td>int</td><td>UDP port number of the new group user</td><td>getUDPPort()</td>
	 * </tr>
	 * </table>
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
	 * and {@link CMDataEvent#getUDPPort()} methods, respectively. The detailed information of the INHABITANT event is 
	 * described below.
	 * 
	 * <table border=1>
	 * <caption>CMDataEvent.INHABITANT event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_DATA_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMDataEvent.INHABITANT</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>current session</td><td>String</td><td>current session name of the user</td><td>getHandlerSession()</td>
	 * </tr>
	 * <tr>
	 * <td>current group</td><td>String</td><td>current group name of the user</td><td>getHandlerGroup()</td>
	 * </tr>
	 * <tr>
	 * <td>user name</td><td>String</td><td>name of the new group user</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>host address</td><td>String</td><td>host address of the new group user</td><td>getHostAddress()</td>
	 * </tr>
	 * <tr>
	 * <td>UDP port number</td><td>int</td><td>UDP port number of the new group user</td><td>getUDPPort()</td>
	 * </tr>
	 * </table>
	 * 
	 * 
	 * @param sname - the session name that a client requests to join
	 * @return true if the request is successful; false otherwise.
	 * @see CMClientStub#syncJoinSession(String)
	 * @see CMClientStub#joinSession(String, String)
	 * @see CMClientStub#leaveSession()
	 * @see CMClientStub#leaveSession(String)
	 */
	public boolean joinSession(String sname)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		boolean bResult = false;
		
		// check local state
		switch( getMyself().getState() )
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect and login server before session join.\n"); return false;
		case CMInfo.CM_CONNECT:
			System.out.println("You should login server before session join..\n"); return false;
		case CMInfo.CM_SESSION_JOIN:
			System.out.println("You have already joined a session.\n"); return false;
		}
		
		// check selected session
		if( !interInfo.isMember(sname) )
		{
			System.out.println("session("+sname+") not found. You can request session information"
					+" from the default server.");
			return false;
		}

		// make and send an event
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.JOIN_SESSION);
		se.setHandlerSession(sname);
		se.setUserName(getMyself().getName());
		se.setSessionName(sname);
		bResult = send(se, "SERVER");
		getMyself().setCurrentSession(sname);
		
		se = null;
		return bResult;
	}
	
	/**
	 * Joins a session in the default server synchronously.
	 * 
	 * <p> Unlike the asynchronous method ({@link CMClientStub#joinSession(String)}), this method makes 
	 * the main thread of the client block its execution until it receives and returns the reply event 
	 * (CMSessionEvent.JOIN_SESSION_ACK) from the default server.
	 * <br> For the other detailed information of the session-join process, please refer to the asynchronous 
	 * version of the request.  
	 * 
	 * @param sname - the session name that a client requests to join
	 * @return the reply event (CMSessionEvent.JOIN_SESSION_ACK) from the default server, null if the request fails.
	 * @see CMClientStub#joinSession(String)
	 */
	public CMSessionEvent syncJoinSession(String sname)
	{
		CMEventSynchronizer eventSync = m_cmInfo.getEventInfo().getEventSynchronizer();
		CMSessionEvent replyEvent = null;
		boolean bRequestResult = false;
		
		bRequestResult = joinSession(sname);
		if(!bRequestResult) return null;
		
		eventSync.setWaitingEvent(CMInfo.CM_SESSION_EVENT, CMSessionEvent.JOIN_SESSION_ACK);
		synchronized(eventSync)
		{
			while(replyEvent == null)
			{
				try {
					eventSync.wait(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				replyEvent = (CMSessionEvent) eventSync.getReplyEvent();
			}
		}
		eventSync.init();
		
		return replyEvent;		
	}
	
	/**
	 * Leaves the current session in the default server.
	 * 
	 * <p> There is no result from the server about the session-leave request.
	 * 
	 * <p> Before leaving the current session, the server first remove the client from its current group. 
	 * The server notifies group members of the user leave by sending the REMOVE_USER event of 
	 * the {@link CMDataEvent}. The REMOVE_USER event includes the user name field, which can be returned 
	 * by the {@link CMDataEvent#getUserName()} method. The detailed information of the REMOvE_USER event 
	 * is described below.
	 * 
	 * <table border=1>
	 * <caption>CMDataEvent.REMOVE_USER event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event field</td><td>CMInfo.CM_DATA_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMDataEvent.REMOVE_USER</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>current session</td><td>String</td><td>current session name of the user</td><td>getHandlerSession()</td>
	 * </tr>
	 * <tr>
	 * <td>current group</td><td>String</td><td>current group name of the user</td><td>getHandlerGroup()</td>
	 * </tr>
	 * <tr>
	 * <td>user name</td><td>String</td><td>name of the leaving group user</td><td>getUserName()</td>
	 * </tr>
	 * </table>
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
	 * @return true if successfully sent the leave-session request, false otherwise.
	 * @see CMClientStub#leaveSession(String)
	 * @see CMClientStub#joinSession(String)
	 * @see CMClientStub#joinSession(String, String)
	 */
	public boolean leaveSession()
	{
		boolean bRequestResult = false;
		CMUser myself = getMyself();
		// check local state
		switch(myself.getState())
		{
		case CMInfo.CM_INIT:
			System.out.println("You should connect, log in to the default server, and join a session."); 
			return false;
		case CMInfo.CM_CONNECT:
			System.out.println("You should log in to the default server and join a session.");
			return false;
		case CMInfo.CM_LOGIN:
			System.out.println("You should join a session."); return false;
		}
		
		// terminate current group info (multicast channel, group member, Membership key)
		CMGroupManager.terminate(myself.getCurrentSession(), myself.getCurrentGroup(), m_cmInfo);
		
		// send the leave request to the default server
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.LEAVE_SESSION);
		se.setHandlerSession(myself.getCurrentSession());
		se.setUserName(myself.getName());
		se.setSessionName(myself.getCurrentSession());
		bRequestResult = send(se, "SERVER");
		
		// update the local state
		myself.setState(CMInfo.CM_LOGIN);
		
		if(bRequestResult)
			System.out.println("["+myself.getName()+"] successfully requested to leave session("+myself.getCurrentSession()+").");
		else
			System.err.println("["+myself.getName()+"] failed the leave-session request!");
		
		se = null;
		return bRequestResult;
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
		CMUser myself = getMyself();
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
	 * The detailed information of the SESSION_TALK event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.SESSION_TALK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_Event</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.SESSION_TALK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>user name</td><td>String</td><td>name of the sending user</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>text message</td><td>String</td><td>a chat message</td><td>getTalk()</td>
	 * </tr>
	 * <tr>
	 * <td>session name</td><td>String</td><td>current session of the sending user</td><td>getHandlerSession()</td>
	 * </tr>
	 * </table>
	 * 
	 * <br>The other event is the USER_TALK event of the {@link CMInterestEvent} class. A client can 
	 * receive this event only if it enters a group. The USER_TALK event includes fields such as the sender 
	 * name, the text message, the session name of the sender, and the group name of the sender, which can 
	 * be returned by calling {@link CMInterestEvent#getUserName()}, {@link CMInterestEvent#getTalk()}, 
	 * {@link CMInterestEvent#getHandlerSession()}, and {@link CMInterestEvent#getHandlerGroup()} methods, 
	 * respectively. The detailed information of the USER_TAlK event is descrbied below.
	 * 
	 * <table border=1>
	 * <caption>CMInterestEvent.USER_TALK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_INTEREST_Event</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMInterestEvent.USER_TALK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>user name</td><td>String</td><td>name of the sending user</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>text message</td><td>String</td><td>a chat message</td><td>getTalk()</td>
	 * </tr>
	 * <tr>
	 * <td>session name</td><td>String</td><td>current session of the sending user</td><td>getHandlerSession()</td>
	 * </tr>
	 * <tr>
	 * <td>group name</td><td>String</td><td>current group of the sending user</td><td>getHandlerGroup()</td>
	 * </tr>
	 * </table> 
	 * 
	 * @param strTarget - the receiver name.
	 * <br>This parameter must start with &quot;/&quot; character and it specifies the range of recipients of the chat 
	 * message as described below:
	 * <br> /b - The chat message is sent to the all login users.
	 * <br> /s - The chat message is sent to the all session members of the sending user.
	 * <br> /g - The chat message is sent to the all group members of the sending user.
	 * <br> /name - The chat message is sent to a specific CM node of which name is &quot;name&quot;. The name can be 
	 * another user name or a server name. If &quot;name&quot; is SERVER, the message is sent to the default server.
	 * @param strMessage - the chat message.
	 */
	public void chat(String strTarget, String strMessage)
	{
		CMUser myself = getMyself();
		
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
	 * Adds asynchronously a nonblocking (TCP) socket channel to a server.
	 * <br> Only the client can add an additional stream socket (TCP) channel. In the case of the datagram 
	 * and multicast channels, both the client and the server can add an additional channel 
	 * with the {@link CMStub#addDatagramChannel(int)} and {@link CMStub#addMulticastChannel(String, String, String, int)} 
	 * methods in the CMStub class.
	 * 
	 * <p> Although this method returns the reference to the valid socket channel at the client, it is unsafe 
	 * for the client to use the socket before the server also adds the relevant channel information.
	 * The establishment of a new nonblocking socket channel at both sides (the client and the server) completes 
	 * only when the client receives the ack event (CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK) from the server 
	 * and the return code in the event is 1.
	 * The client event handler can catch the ack event, and the detailed event fields are described below:
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SESSION_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> Channel name (server name) </td> <td> {@link CMSessionEvent#getChannelName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> Channel key </td> <td> {@link CMSessionEvent#getChannelNum()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> Return code </td> <td> {@link CMSessionEvent#getReturnCode()} </td>
	 *   </tr>
	 * </table>
	 * 
	 * @param nChKey - the channel key that must be greater than 0.
	 * The key 0 is occupied by the default TCP channel.
	 * @param strServer - the server name to which the client adds a TCP channel. The name of the default 
	 * server is 'SERVER'.
	 * @return true if the socket channel is successfully created at the client and requested to add 
	 * the (key, socket) pair to the server. 
	 * <br> False, otherwise.
	 * 
	 * @see CMClientStub#syncAddNonBlockSocketChannel(int, String)
	 * @see CMClientStub#addBlockSocketChannel(int, String)
	 * @see CMClientStub#syncAddBlockSocketChannel(int, String)
	 * @see CMClientStub#removeNonBlockSocketChannel(int, String)
	 */
	public boolean addNonBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		SocketChannel sc = null;
		CMChannelInfo<Integer> scInfo = null;
		
		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.addNonBlockSocketChannel(), you must log in to the default server!");
			return false;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.addNonBlockSocketChannel(), server("+strServer+") not found.");
				return false;
			}			
		}
		
		try {
			scInfo = serverInfo.getNonBlockSocketChannelInfo();
			sc = (SocketChannel) scInfo.findChannel(nChKey);
			if(sc != null)
			{
				System.err.println("CMClientStub.addNonBlockSocketChannel(), channel key("+nChKey
						+") already exists.");
				return false;
			}
			
			sc = (SocketChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
					serverInfo.getServerAddress(), serverInfo.getServerPort(), m_cmInfo);
			if(sc == null)
			{
				System.err.println("CMClientStub.addNonBlockSocketChannel(), failed!: key("+nChKey+"), server("
						+strServer+")");
				return false;
			}
			scInfo.addChannel(nChKey, sc);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL);
		se.setChannelName(getMyself().getName());
		se.setChannelNum(nChKey);
		send(se, strServer, CMInfo.CM_STREAM, nChKey);
		
		se = null;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMClientStub.addNonBlockSocketChannel(),successfully requested to add the channel "
					+ "with the key("+nChKey+") to the server("+strServer+")");
		}
		
		return true;
	}
	
	/**
	 * Adds synchronously a nonblocking (TCP) socket channel to a server.
	 * <br> Only the client can add an additional stream socket (TCP) channel. In the case of the datagram 
	 * and multicast channels, both the client and the server can add an additional channel 
	 * with the {@link CMStub#addDatagramChannel(int)} and {@link CMStub#addMulticastChannel(String, String, String, int)} 
	 * methods in the CMStub class.
	 * 
	 * @param nChKey - the channel key which must be greater than 0.
	 * The key 0 is occupied by the default TCP channel.
	 * @param strServer - the server name to which the client adds a TCP channel. The name of the default 
	 * server is 'SERVER'.
	 * @return true if the socket channel is successfully created both at the client and the server. 
	 * <br> False, otherwise.
	 * 
	 * @see CMClientStub#addNonBlockSocketChannel(int, String)
	 * @see CMClientStub#addBlockSocketChannel(int, String)
	 * @see CMClientStub#syncAddBlockSocketChannel(int, String)
	 * @see CMClientStub#removeNonBlockSocketChannel(int, String)
	 */
	public SocketChannel syncAddNonBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		SocketChannel sc = null;
		CMChannelInfo<Integer> scInfo = null;
		CMEventInfo eInfo = m_cmInfo.getEventInfo();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMSessionEvent replyEvent = null;
		int nReturnCode = -1;
		
		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.syncAddNonBlockSocketChannel(), you must log in to the default server!");
			return null;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.syncAddNonBlockSocketChannel(), server("+strServer+") not found.");
				return null;
			}			
		}
		
		try {
			scInfo = serverInfo.getNonBlockSocketChannelInfo();
			sc = (SocketChannel) scInfo.findChannel(nChKey);
			if(sc != null)
			{
				System.err.println("CMClientStub.syncAddNonBlockSocketChannel(), channel key("+nChKey
						+") already exists.");
				return null;
			}
			
			sc = (SocketChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
					serverInfo.getServerAddress(), serverInfo.getServerPort(), m_cmInfo);
			if(sc == null)
			{
				System.err.println("CMClientStub.syncAddNonBlockSocketChannel(), failed!: key("+nChKey+"), server("
						+strServer+")");
				return null;
			}
			scInfo.addChannel(nChKey, sc);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		eventSync.setWaitingEvent(CMInfo.CM_SESSION_EVENT, CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK);

		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL);
		se.setChannelName(getMyself().getName());
		se.setChannelNum(nChKey);
		boolean bRequestResult = send(se, strServer, CMInfo.CM_STREAM, nChKey);
		if(!bRequestResult)
			return null;
		
		se = null;
		
		synchronized(eventSync)
		{
			while(replyEvent == null)
			{
				try {
					eventSync.wait(30000);  // timeout 30s
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				replyEvent = (CMSessionEvent) eventSync.getReplyEvent();
			}	
		}
		eventSync.init();

		nReturnCode = replyEvent.getReturnCode();
		if(nReturnCode == 1) // successfully add the new channel info (key, channel) at the server
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMClientStub.syncAddNonBlockSocketChannel(), successfully add the channel "
						+ "info at the server: key("+nChKey+"), server("+strServer+")");
			}
		}
		else if(nReturnCode == 0) // failed to add the new channel info (key, channel) at the server
		{
			System.err.println("CMClientStub.syncAddNonBlockSocketChannel(),failed to add the channel info "
					+ "at the server: key("+nChKey+"), server("+strServer+")");
			sc = null;	// the new socket channel is closed and removed at the CMInteractionManager
		}
		else
		{
			System.err.println("CMClientStub.syncAddNonBlockSocketChannel(), failed: return code("+nReturnCode+")");
			scInfo.removeChannel(nChKey);
			sc = null;
		}
		
		return sc;		
	}
	
	/**
	 * Removes a nonblocking (TCP) socket channel from a server.
	 * 
	 * @param nChKey - the key of the channel that is to be removed. The key must be greater than 0. 
	 * If the default channel (0) is removed, the result is undefined. 
	 * @param strServer - the server name from which the additional channel is removed.
	 * @return true if the client successfully closes and removes the channel, or false otherwise.
	 * <br> If the client removes the nonblocking socket channel, the server CM detects the disconnection and 
	 * removes the channel at the server side as well. 
	 * @see CMClientStub#addNonBlockSocketChannel(int, String)
	 * @see CMClientStub#removeBlockSocketChannel(int, String)
	 */
	public boolean removeNonBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMChannelInfo<Integer> scInfo = null;
		boolean result = false;

		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.addNonBlockSocketChannel(), you must log in to the default server!");
			return false;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.removeNonBlockSocketChannel(), server("+strServer+") not found.");
				return false;
			}			
		}
		
		scInfo = serverInfo.getNonBlockSocketChannelInfo();
		result = scInfo.removeChannel(nChKey);
		if(result)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMClientStub.removeNonBlockSocketChannel(), succeeded. key("+nChKey+"), server ("
					+strServer+").");
			}
		}
		else
		{
			System.err.println("CMClientStub.removeNonBlockSocketChannel(), failed! key("+nChKey+"), server ("
					+strServer+").");			
		}
		
		return result;
	}

	/**
	 * Adds asynchronously a blocking (TCP) socket channel to a server.
	 * <br> Only the client can add an additional stream socket (TCP) channel. In the case of the datagram 
	 * and multicast channels, both the client and the server can add an additional channel 
	 * with the {@link CMStub#addDatagramChannel(int)} and {@link CMStub#addMulticastChannel(String, String, String, int)} 
	 * methods in the CMStub class.
	 * 
	 * <p> Although this method returns the reference to the valid socket channel, the server side socket channel is 
	 * always created as a nonblocking mode first due to the intrinsic CM architecture of event-driven asynchronous 
	 * communication. The server sends the acknowledgement message after the nonblocking channel is changed 
	 * to the blocking channel. It is unsafe for the client use its socket channel before the channel is changed to 
	 * the blocking mode at the server.
	 * The establishment of a new blocking socket channel at both sides (the client and the server) completes 
	 * only when the client receives the ack event (CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK) from the server 
	 * and the return code in the event is 1. 
	 * The client event handler can catch the ack event, and the detailed event fields are described below:
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SESSION_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> Channel name (server name) </td> <td> {@link CMSessionEvent#getChannelName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> Channel key </td> <td> {@link CMSessionEvent#getChannelNum()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> Return code </td> <td> {@link CMSessionEvent#getReturnCode()} </td>
	 *   </tr>
	 * </table>
	 * 
	 * @param nChKey - the channel key. It should be a positive integer (greater than or equal to 0).
	 * @param strServer - the name of a server to which the client creates a connection. The default server name is 
	 * "SERVER".
	 * @return a reference to the socket channel if it is successfully created at the client, or null otherwise. 
	 * 
	 * @see CMClientStub#syncAddBlockSocketChannel(int, String)
	 * @see CMClientStub#addNonBlockSocketChannel(int, String)
	 * @see CMClientStub#syncAddNonBlockSocketChannel(int, String)
	 * @see CMClientStub#removeBlockSocketChannel(int, String)
	 * 
	 */
	public boolean addBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		SocketChannel sc = null;
		CMChannelInfo<Integer> scInfo = null;

		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.addNonBlockSocketChannel(), you must log in to the default server!");
			return false;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.addBlockSocketChannel(), server("+strServer+") not found.");
				return false;
			}			
		}
		
		try {
			scInfo = serverInfo.getBlockSocketChannelInfo();
			sc = (SocketChannel) scInfo.findChannel(nChKey);
			if(sc != null)
			{
				System.err.println("CMClientStub.addBlockSocketChannel(), channel key("+nChKey+") already exists.");
				return false;
			}
			
			sc = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
					serverInfo.getServerAddress(), serverInfo.getServerPort(), m_cmInfo);
			if(sc == null)
			{
				System.err.println("CMClientStub.addBlockSocketChannel(), failed!: key("+nChKey+"), server("+strServer+")");
				return false;
			}
			scInfo.addChannel(nChKey, sc);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
		se.setChannelName(getMyself().getName());
		se.setChannelNum(nChKey);
		send(se, strServer, CMInfo.CM_STREAM, nChKey, true);
		se = null;

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMClientStub.addBlockSocketChannel(),successfully requested to add the channel "
					+ "with the key("+nChKey+") to the server("+strServer+")");
		}
				
		return true;				
	}
	
	/**
	 * Adds synchronously a blocking (TCP) socket channel to a server.
	 * <br> Only the client can add an additional stream socket (TCP) channel. In the case of the datagram 
	 * and multicast channels, both the client and the server can add an additional channel 
	 * with the {@link CMStub#addDatagramChannel(int)} and {@link CMStub#addMulticastChannel(String, String, String, int)} 
	 * methods in the CMStub class.
	 * 
	 * @param nChKey - the channel key. It should be a positive integer (greater than or equal to 0).
	 * @param strServer - the name of a server to which the client creates a connection. The default server name is 
	 * "SERVER".
	 * @return a reference to the socket channel if it is successfully created both at the client and the server, 
	 * or null otherwise. 
	 * 
	 * @see CMClientStub#addBlockSocketChannel(int, String)
	 * @see CMClientStub#addNonBlockSocketChannel(int, String)
	 * @see CMClientStub#syncAddNonBlockSocketChannel(int, String)
	 * @see CMClientStub#removeBlockSocketChannel(int, String)
	 */
	public SocketChannel syncAddBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		SocketChannel sc = null;
		CMChannelInfo<Integer> scInfo = null;
		CMEventInfo eInfo = m_cmInfo.getEventInfo();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMSessionEvent replyEvent = null;
		int nReturnCode = -1;

		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.syncAddNonBlockSocketChannel(), you must log in to the default server!");
			return null;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.syncAddBlockSocketChannel(), server("+strServer+") not found.");
				return null;
			}			
		}
		
		try {
			scInfo = serverInfo.getBlockSocketChannelInfo();
			sc = (SocketChannel) scInfo.findChannel(nChKey);
			if(sc != null)
			{
				System.err.println("CMClientStub.syncAddBlockSocketChannel(), channel key("+nChKey+") already exists.");
				return null;
			}
			
			sc = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
					serverInfo.getServerAddress(), serverInfo.getServerPort(), m_cmInfo);
			if(sc == null)
			{
				System.err.println("CMClientStub.syncAddBlockSocketChannel(), failed!: key("+nChKey+"), server("
						+strServer+")");
				return null;
			}
			scInfo.addChannel(nChKey, sc);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		eventSync.setWaitingEvent(CMInfo.CM_SESSION_EVENT, CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK);

		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
		se.setChannelName(getMyself().getName());
		se.setChannelNum(nChKey);
		boolean bRequestResult = send(se, strServer, CMInfo.CM_STREAM, nChKey, true);
		if(!bRequestResult)
			return null;
		
		se = null;

		synchronized(eventSync)
		{
			while(replyEvent == null)
			{
				try {
					eventSync.wait(30000);  // timeout 30s
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				replyEvent = (CMSessionEvent) eventSync.getReplyEvent();
			}			
		}
		eventSync.init();

		nReturnCode = replyEvent.getReturnCode();
		if(nReturnCode == 1) // successfully add the new channel info (key, channel) at the server
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMClientStub.syncAddBlockSocketChannel(), successfully add the channel "
						+ "info at the server: "+"key("+nChKey+"), server("+strServer+")");
			}
		}
		else if(nReturnCode == 0) // failed to add the new channel info (key, channel) at the server
		{
			System.err.println("CMClientStub.syncAddBlockSocketChannel(),failed to add the channel info at the server: "
					+"key("+nChKey+"), server("+strServer+")");
			sc = null;	// the new socket channel is closed and removed at the CMInteractionManager
		}
		else
		{
			System.err.println("CMClientStub.syncAddBlockSocketChannel(), failed: return code("+nReturnCode+")");
			scInfo.removeChannel(nChKey);
			sc = null;
		}

		return sc;		
	}
	
	/**
	 * Removes asynchronously the blocking socket (TCP) channel.
	 * 
	 * <p> This method does not immediately remove the requested channel for safe and smooth close procedure 
	 * between the client and the server. Before the removal of the client socket channel, the client first sends 
	 * a request CM event to the server that then prepares the channel disconnection and sends the ack event 
	 * (CMSessionEvent.REMOVE_SOCKET_CHANNEL_ACK) back to the client.
	 * <br> The client closes and removes the target channel only if it receives the ack event and the return code 
	 * is 1. 
	 * The client event handler can catch the event in order to figure out the result of the removal request.
	 * The detailed event fields are described below:
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL_ACK event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SESSION_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL_ACK </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event field </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Channel name (server name) </td> <td> {@link CMSessionEvent#getChannelName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Channel key </td> <td> {@link CMSessionEvent#getChannelNum()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Return code </td> <td> {@link CMSessionEvent#getReturnCode()} </td>
	 *   </tr>
	 * </table>
	 * 
	 * @param nChKey - the key of a socket channel that is to be deleted.
	 * @param strServer - the name of a server to which the target socket channel is connected.
	 * @return true if the client successfully requests the removal of the channel from the server, or false otherwise.
	 * <br> The blocking socket channel is closed and removed only when the client receives the ack event from the server.
	 * 
	 * @see CMClientStub#syncRemoveBlockSocketChannel(int, String)
	 * @see CMClientStub#addBlockSocketChannel(int, String)
	 * @see CMClientStub#syncAddBlockSocketChannel(int, String)
	 */
	public boolean removeBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMChannelInfo<Integer> scInfo = null;
		boolean result = false;
		SocketChannel sc = null;
		CMSessionEvent se = null;

		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.removeBlockSocketChannel(), you must log in to the default server!");
			return false;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.removeBlockSocketChannel(), server("+strServer+") not found.");
				return false;
			}			
		}
		
		scInfo = serverInfo.getBlockSocketChannelInfo();
		sc = (SocketChannel) scInfo.findChannel(nChKey);
		if(sc == null)
		{
			System.err.println("CMClientStub.removeBlockSocketChannel(), socket channel not found! key("
					+nChKey+"), server ("+strServer+").");
			return false;
		}
		
		se = new CMSessionEvent();
		se.setID(CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL);
		se.setChannelNum(nChKey);
		se.setChannelName(interInfo.getMyself().getName());
		result = send(se, strServer);	// send the event with the default nonblocking socket channel
		se = null;
		
		// The channel will be closed and removed after the client receives the ACK event at the event handler.
		
		return result;
	}
	
	/**
	 * Removes synchronously the blocking socket (TCP) channel.
	 * 
	 * <p> This method does not immediately remove the requested channel for safe and smooth close procedure 
	 * between the client and the server. Before the removal of the client socket channel, the client first sends 
	 * a request CM event to the server that then prepares the channel disconnection and sends the ack event 
	 * (CMSessionEvent.REMOVE_SOCKET_CHANNEL_ACK) back to the client.
	 * <br> The client closes and removes the target channel only if it receives the ack event and the return code 
	 * is 1. 
	 * 
	 * @param nChKey - the key of a socket channel that is to be deleted.
	 * @param strServer - the name of a server to which the target socket channel is connected.
	 * @return true if the client successfully closed and removed the channel, false otherwise.
	 * <br> The blocking socket channel is closed and removed only when the client receives the ack event from the server.
	 * 
	 * @see CMClientStub#removeBlockSocketChannel(int, String)
	 * @see CMClientStub#addBlockSocketChannel(int, String)
	 * @see CMClientStub#syncAddBlockSocketChannel(int, String)
	 */
	public boolean syncRemoveBlockSocketChannel(int nChKey, String strServer)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMChannelInfo<Integer> scInfo = null;
		boolean result = false;
		SocketChannel sc = null;
		CMSessionEvent se = null;
		CMEventInfo eInfo = m_cmInfo.getEventInfo();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMSessionEvent replyEvent = null;
		int nReturnCode = -1;

		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.addNonBlockSocketChannel(), you must log in to the default server!");
			return false;
		}
		
		if(strServer.equals("SERVER"))
		{
			serverInfo = interInfo.getDefaultServerInfo();
		}
		else
		{
			serverInfo = interInfo.findAddServer(strServer);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.removeBlockSocketChannel(), server("+strServer+") not found.");
				return false;
			}			
		}
		
		scInfo = serverInfo.getBlockSocketChannelInfo();
		sc = (SocketChannel) scInfo.findChannel(nChKey);
		if(sc == null)
		{
			System.err.println("CMClientStub.removeBlockSocketChannel(), socket channel not found! key("
					+nChKey+"), server ("+strServer+").");
			return false;
		}
		
		eventSync.setWaitingEvent(CMInfo.CM_SESSION_EVENT, CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL_ACK);
		
		se = new CMSessionEvent();
		se.setID(CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL);
		se.setChannelNum(nChKey);
		se.setChannelName(interInfo.getMyself().getName());
		result = send(se, strServer);	// send the event with the default nonblocking socket channel
		if(!result)
			return false;
		
		se = null;
		
		synchronized(eventSync)
		{
			while(replyEvent == null)
			{
				try {
					eventSync.wait(30000);  // timeout 30s
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				replyEvent = (CMSessionEvent) eventSync.getReplyEvent();
			}
		}
		eventSync.init();

		nReturnCode = replyEvent.getReturnCode();
		if(nReturnCode == 1) // successfully remove the new channel info (key, channel) at the server
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMClientStub.syncRemoveBlockSocketChannel(), successfully removed the channel "
						+ "info at the server: "+"key("+nChKey+"), server("+strServer+")");
			}
		}
		else if(nReturnCode == 0) // failed to remove the new channel info (key, channel) at the server
		{
			System.err.println("CMClientStub.syncRemoveBlockSocketChannel(),failed to remove the channel info "
					+ "at the server: key("+nChKey+"), server("+strServer+")");
			result = false;
		}
		else
		{
			System.err.println("CMClientStub.syncRemoveBlockSocketChannel(), failed: return code("+nReturnCode+")");
			result = false;
		}
		
		return result;
	}

	/**
	 * Returns a blocking socket (TCP) channel.
	 * 
	 * @param nChKey - the channel key.
	 * @param strServerName - the name of a server to which the socket channel is connected.
	 * <br> If strServerName is null, it implies the socket channel to the default server.
	 * @return the blocking socket channel, or null if the channel is not found.
	 */
	public SocketChannel getBlockSocketChannel(int nChKey, String strServerName)
	{
		SocketChannel sc = null;
		CMServer serverInfo = null;
		CMChannelInfo<Integer> chInfo = null;

		if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		{
			System.err.println("CMClientStub.addNonBlockSocketChannel(), you must log in to the default server!");
			return null;
		}
		
		if(strServerName.equals("SERVER"))
		{
			serverInfo = m_cmInfo.getInteractionInfo().getDefaultServerInfo();
		}
		else
		{
			serverInfo = m_cmInfo.getInteractionInfo().findAddServer(strServerName);
			if(serverInfo == null)
			{
				System.err.println("CMClientStub.getBlockSocketChannel(), additional server info not found! : "
						+"server ("+strServerName+"), key ("+nChKey+")");
				return null;
			}
		}
		
		chInfo = serverInfo.getBlockSocketChannelInfo();
		sc = (SocketChannel) chInfo.findChannel(nChKey);

		if(sc == null)
		{
			System.err.println("CMClientStub.getBlockSocketChannel(), not found! : key ("+nChKey+"), server ("
					+strServerName+")");
			return null;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMClientStub.getBlockSocketChannel(), channel found ("+sc.hashCode()+") : "
					+"key ("+nChKey+"), server ("+strServerName+")");
		}
		
		return sc;
	}

	/**
	 * Requests to download the list of SNS content from the default server.
	 * <p> The number of downloaded content items is determined by the server. In the configuration file of 
	 * the server CM (cm-server.conf), the DOWNLOAD_NUM field specifies the number of downloaded content items.
	 * 
	 * <p> Each of the requested SNS content item is then sent to the requesting client as 
	 * the CONTENT_DOWNLOAD event that belongs to the {@link CMSNSEvent} class, and that can be caught in 
	 * the client event handler. The CONTENT_DOWNLOAD event includes fields as below:
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.CONTENT_DOWNLOAD event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SNS_EVENT </td>
	 *   </tr>
	 *   <tr> 
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMSNEEvent.CONTENT_DOWNLOAD </td> 
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event field </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Requester name </td> <td> {@link CMSNSEvent#getUserName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Requested content offset </td> <td> {@link CMSNSEvent#getContentOffset()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Content ID </td> <td> {@link CMSNSEvent#getContentID()} </td>
	 *   </tr>
	 *   <tr> 
	 *     <td bgcolor="lightgrey"> Written date and time of the content </td> <td> {@link CMSNSEvent#getDate()} </td> 
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Writer name of the content </td> <td> {@link CMSNSEvent#getWriterName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Text message of the content </td> <td> {@link CMSNSEvent#getMessage()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Number of attachments </td> <td> {@link CMSNSEvent#getNumAttachedFiles()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Content ID to which this message replies (0 for no reply) </td>
	 *     <td> {@link CMSNSEvent#getReplyOf()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Level of disclosure of the content
	 *          <br> 0: open to public <br> 1: open only to friends <br> 2: open only to bi-friends 
	 *          <br> 3: private 
	 *     </td> 
	 *     <td> {@link CMSNSEvent#getLevelOfDisclosure()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> List of attached file names </td> <td> {@link CMSNSEvent#getFileNameList()} </td>
	 *   </tr>
	 * </table>
	 * 
	 * <p> In most cases, the server sends multiple CONTENT_DOWNLOAD events due to the corresponding number of SNS messages, 
	 * and it sends the CONTENT_DOWNLOAD_END event of {@link CMSNSEvent} as the end signal of current download. 
	 * This event contains a field that is the number of downloaded messages. A client event handler can catch this event, 
	 * and the client can send another download request by updating the offset parameter with the number of previously 
	 * downloaded messages. 
	 * The detailed event fields of the CONTENT_DOWNLOAD_END event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.CONTENT_DOWNLOAD_END event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SNS_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMSNSEvent.CONTENT_DOWNLOAD_END </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event field </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> User name </td> <td> {@link CMSNSEvent#getUserName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Offset </td> <td> {@link CMSNSEvent#getContentOffset()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Content ID </td> <td> {@link CMSNSEvent#getContentID()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Number of downloaded items </td> <td> {@link CMSNSEvent#getNumContents()} </td>
	 *   <tr>
	 * </table>
	 * 
	 * @param strWriter - the name of the writer whose content list will be downloaded.
	 * <br> The client can designate a specific writer name or a friend group. If the parameter value is 
	 * a specific user name, the client downloads only content that was uploaded by the specified name 
	 * and that is accessible by the requester. If the parameter value is &quot;CM_MY_FRIEND&quot;, the client 
	 * downloads content that was uploaded by the requester&#39;s friends. If the parameter is &quot;CM_BI_FRIEND&quot;, 
	 * the client downloads content that was uploaded by the requester&#39;s bi-friends. If the &quot;strWriter&quot; 
	 * parameter is an empty string (&quot;&quot;), the client does not specify a writer name and it downloads all 
	 * content that the requester is eligible to access.
	 * @param nOffset - the offset from the beginning of the requested content list.
	 * <br> The client can request to download some number of SNS messages starting from the nOffset-th 
	 * most recent content. The nOffset value is greater than or equal to 0. The requested content list is 
	 * sorted in reverse chronological order (in reverse order of uploading time). If the searched content 
	 * list has 5 items, they have index number starting with 0. The first item (index 0) is the most recent content, 
	 * the second item (index 1) is the second most recent one, and so on.
	 * 
	 * @see CMClientStub#requestSNSContentUpload(String, String, int, int, int, ArrayList)
	 * @see CMClientStub#requestAttachedFileOfSNSContent(String)
	 * @see CMClientStub#requestAttachedFileOfSNSContent(int, String, String)
	 */
	public void requestSNSContent(String strWriter, int nOffset)
	{
		CMUser user = getMyself();
		int nState = user.getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT )
		{
			System.out.println("CCMClientStub::requestSNSContents(), you must log in to the default server!");
			return;
		}
		
		String strUser = user.getName();
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.CONTENT_DOWNLOAD_REQUEST);
		se.setUserName(strUser);
		se.setWriterName(strWriter);
		se.setContentOffset(nOffset);
		send(se, "SERVER");

		se = null;
		return;
	}

	/**
	 * Requests to download the next list of SNS content.
	 * 
	 * <p> This method requests the next list after the last download request of SNS content.
	 * If this method is called without any previous download request, it requests the most recent list of SNS content, 
	 * which is the same as the result of {@link CMClientStub#requestSNSContent(String, int)}.
	 * <br> If there is no more next list of SNS content, the server sends the CONTENT_DOWNLOAD_END event of 
	 * {@link CMSNSEvent} without sending the CONTENT_DOWNLOAD event.
	 * 
	 * @see CMClientStub#requestPreviousSNSContent()
	 * @see CMClientStub#requestSNSContent(String, int)
	 */
	public void requestNextSNSContent()
	{
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		// get the saved data
		String strWriter = snsInfo.getLastlyReqWriter();
		int nOffset = snsInfo.getLastlyReqOffset();
		int nDownContentNum = snsInfo.getLastlyDownContentNum();
		
		// update next content offset
		nOffset = nOffset+nDownContentNum;
		
		// request SNS content
		requestSNSContent(strWriter, nOffset);
		
		return;
	}
	
	/**
	 * Requests to download the previous list of SNS content.
	 * 
	 * <p> This method requests the previous list before the last download request of SNS content.
	 * If this method is called without any previous download request, it requests the most recent list of SNS content, 
	 * which is the same as the result of {@link CMClientStub#requestSNSContent(String, int)}.
	 * <br> If there is no more previous list of SNS content, the server sends the CONTENT_DOWNLOAD_END event of 
	 * {@link CMSNSEvent} without sending the CONTENT_DOWNLOAD event.
	 * 
	 * @see CMClientStub#requestNextSNSContent()
	 * @see CMClientStub#requestSNSContent(String, int)
	 */
	public void requestPreviousSNSContent()
	{
		CMSNSInfo snsInfo = m_cmInfo.getSNSInfo();
		// get the saved data
		String strWriter = snsInfo.getLastlyReqWriter();
		int nOffset = snsInfo.getLastlyReqOffset();
		int nDownContentNum = snsInfo.getLastlyDownContentNum();
		
		// update next content offset
		nOffset = nOffset-nDownContentNum;
		//if(nOffset < 0) nOffset = 0;
		
		// request SNS content
		requestSNSContent(strWriter, nOffset);
		
		return;
	}
	
	/**
	 * Uploads SNS content.
	 * <br> A client can call this method to upload a message to the default server.
	 * 
	 * <p> If the server receives the content upload request, it stores the requested message with the user name, 
	 * the index of the content, the upload time, the number of attachments, the reply ID, and the level of disclosure. 
	 * If the content has attached files, the client separately transfers them to the server. After the upload task is 
	 * completed, the server sends the CONTENT_UPLOAD_RESPONSE event of {@link CMSNSEvent} to the requesting client 
	 * so that the client handler can catch the result of the request. 
	 * The detailed event fields of the CONTENT_UPLOAD_RESPONSE event is described below:
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.CONTENT_UPLOAD_RESPONSE event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SNS_EVENT </td>
	 *   </tr>
	 *   <tr> 
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMSNEEvent.CONTENT_UPLOAD_RESPONSE </td> 
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event field </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Return code </td> <td> {@link CMSNSEvent#getReturnCode()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Content ID </td> <td> {@link CMSNSEvent#getContentID()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Date and time </td> <td> {@link CMSNSEvent#getDate()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> User name </td> <td> {@link CMSNSEvent#getUserName()} </td>
	 *   </tr>
	 * </table>  
	 * 
	 * @param user - the name of a user who uploads a message
	 * @param message - the text message
	 * @param nNumAttachedFiles - the number of attached files in this message
	 * <br> The value of nNumAttachedFiles must be the same as the number of elements in a given file path list 
	 * as the last parameter, filePathList.
	 * @param nReplyOf - an ID (greater than 0) of content to which this message replies
	 * <br> If the value is 0, it means that the uploaded content is not a reply but an original one.
	 * @param nLevelOfDisclosure - the level of disclosure (LoD) of the uploaded content
	 * <br> CM provides four levels of disclosure of content from 0 to 3. LoD 0 is to open the uploaded content to public. 
	 * LoD 1 allows only users who added the uploading user as friends to access the uploaded content. 
	 * LoD 2 allows only bi-friends of the uploading user to access the uploaded content. (Refer to the friend management 
	 * section for details of different friend concepts.) 
	 * LoD 3 does not open the uploaded content and makes it private.
	 * @param filePathList - the list of attached files
	 * 
	 * @see CMClientStub#requestSNSContent(String, int) 
	 */
	public void requestSNSContentUpload(String user, String message, int nNumAttachedFiles, 
			int nReplyOf, int nLevelOfDisclosure, ArrayList<String> filePathList)
	{
		ArrayList<String> fileNameList = null;
		int i = -1;
		int nState = getMyself().getState();
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
			CMSNSAttach sendAttach = sInfo.getSendSNSAttach();
			sendAttach.setFilePathList(filePathList);
			
			// create a file name list with the given file path list
			fileNameList = new ArrayList<String>();
			for(i = 0; i < filePathList.size(); i++)
			{
				String strFilePath = filePathList.get(i);
				int startIndex = strFilePath.lastIndexOf(File.separator);
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
	
	/**
	 * Requests an attached file of SNS content.
	 * 
	 * <p> The client can request to download a file that is attached to a downloaded SNS content item from the server.
	 * <br> After the client request to download SNS content by the {@link CMClientStub#requestSNSContent(String, int)} 
	 * method, all attached files are automatically downloaded from the server. If an attached file is not available at 
	 * the server, only the file name is downloaded so that the client can separately request such a file from 
	 * the server later.
	 * 
	 * <p> If the server is requested to download an attached file, it sends the RESPONSE_ATTACHED_FILE event of 
	 * the {@link CMSNSEvent}. The client event handler can catch this event, and can figure out the result of 
	 * the request by getting the return code. If the return code is 1, the requested file is available at the server, 
	 * and the server separately sends the requested file to the client using the {@link CMStub#pushFile(String, String)} 
	 * method. If the return code is 0, the requested file does not exist at the server.
	 * <br> The detailed event fields of the RESPONSE_ATTACHED_FILE event are described below:
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.RESPONSE_ATTACHED_FILE event</caption>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SNS_EVENT </td> 
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Event ID </td> <td> CMSNSEvent.RESPONSE_ATTACHED_FILE </td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 *   <td> Event field </td> <td> Get method </td> <td> Description </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> User name </td> <td> {@link CMSNSEvent#getUserName()} </td> 
	 *   <td> The requesting user name </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Content ID </td> <td> {@link CMSNSEvent#getContentID()} </td>
	 *   <td> The ID of the SNS content that attached the requested file </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Writer name </td> <td> {@link CMSNSEvent#getWriterName()} </td>
	 *   <td> The writer name of the SNS content </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> File name </td> <td> {@link CMSNSEvent#getFileName()} </td>
	 *   <td> The name of the attached file </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Return code </td> <td> {@link CMSNSEvent#getReturnCode()} </td>
	 *   <td> The request result. If the value is 1, the requested file will be delivered to the client. If the value 
	 *   is 0, the server does nothing further for the request. </td>
	 * </tr>
	 * </table>
	 * 
	 * @param strFileName - the requested file name
	 * <br> The list of attached file names can be got in the CONTENT_DOWNLOAD event of {@link CMSNSEvent} that is sent from 
	 * the server. After the client event handler catch this event and get the list of attached file names, it can choose 
	 * a file name that needs to separately download from the server.
	 * 
	 * @return true if the request is successfully sent, or false otherwise
	 * @see CMClientStub#requestAttachedFileOfSNSContent(int, String, String)
	 */
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
	
	/**
	 * Requests an attached file of SNS content.
	 * 
	 * <p> The detailed information about the request for an attached file can be found in 
	 * the {@link CMClientStub#requestAttachedFileOfSNSContent(String)} method.
	 * 
	 * @param nContentID - the ID of SNS content to which the requested file is attached
	 * @param strWriterName - the name of a requesting user
	 * @param strFileName - the requested file name
	 * 
	 * @see CMClientStub#requestAttachedFileOfSNSContent(String)
	 */
	public void requestAttachedFileOfSNSContent(int nContentID, String strWriterName, String strFileName)
	{
		String strUserName = getMyself().getName();
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

	/**
	 * Informs the server that the attached file has been accessed by the client.
	 * 
	 * <p> The client can call this method to report its access history of an attached file to the server. The access 
	 * report is sent to the server as the ACCESS_ATTACHED_FILE event of the {@link CMSNSEvent}.  
	 * If the server receives the event, it can use the access information for the analysis of the history of 
	 * client behavior. The server event handler can catch the event.
	 * 
	 * <p> The detailed event fields of the ACCESS_ATTACHED_FILE event are described below:
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.ACCESS_ATTACHED_FILE event</caption>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_SNS_EVENT </td> 
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Event ID </td> <td> CMSNSEvent.ACCESS_ATTACHED_FILE </td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 *   <td> Event field </td> <td> Get method </td> <td> Description </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> User name </td> <td> {@link CMSNSEvent#getUserName()} </td> 
	 *   <td> The name of the file-accessing user </td> 
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Content ID </td> <td> {@link CMSNSEvent#getContentID()} </td>
	 *   <td> ID of the SNS content of which attached file is accessed </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Writer name </td> <td> {@link CMSNSEvent#getWriterName()} </td>
	 *   <td> The writer name of the SNS content of which attached file is accessed </td>
	 * </tr>
	 * <tr>
	 *   <td bgcolor="lightgrey"> Attached file name </td> <td> {@link CMSNSEvent#getFileName()} </td>
	 *   <td> The name of an attached file that the user accessed </td>
	 * </tr>
	 * </table>
	 * 
	 * @param strFileName - the name of an attached file that the user accessed
	 * @return true if the file access information is successfully sent to the server and if the corresponding 
	 * SNS content is found at the client. Otherwise, the return value is false.
	 * 
	 * @see CMClientStub#accessAttachedFileOfSNSContent(int, String, String)
	 */
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

	/**
	 * Informs the server that the attached file has been accessed by the client.
	 * 
	 * <p>	The detailed information about the access report of an attached file to the server can be found in 
	 * the {@link CMClientStub#accessAttachedFileOfSNSContent(String)} method.
	 * 
	 * @param nContentID - the ID of the SNS content of which attached file is accessed
	 * @param strWriterName - the writer name of the SNS content of which attached file is accessed
	 * @param strFileName - the name of an attached file that the user accessed
	 * 
	 * @see CMClientStub#accessAttachedFileOfSNSContent(String)
	 */
	public void accessAttachedFileOfSNSContent(int nContentID, String strWriterName, String strFileName)
	{
		String strUserName = getMyself().getName();
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

	/**
	 * Requests new additional server information from the default server.
	 * 
	 * <p> When the default server registers an additional server, it then notifies clients of 
	 * the new server information. If a client is a late comer to the CM network, it can also 
	 * explicitly request the information of additional servers from the default server.
	 * <br> In any of the above two cases, the default server sends the NOTIFY_SERVER_INFO event 
	 * of the {@link CMMultiServerEvent} class. This event contains the list of additional server information 
	 * such as a server name, address, port number, and UDP port number. The detailed event fields of 
	 * the NOTIFY_SERVER_INFO event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMMultiServerEvent.NOTIFY_SERVER_INFO event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_MULTI_SERVER_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMMultiServerEvent.NOTIFY_SERVER_INFO</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Number of servers</td><td>int</td><td>Number of additional servers</td><td>getServerNum()</td>
	 * </tr>
	 * <tr>
	 * <td>Server list</td><td>Vector&lt;{@link CMServerInfo}&gt;</td><td>List of additional server information</td>
	 * <td>getServerInfoList()</td>
	 * </tr>
	 * </table>
	 * 
	 * <p> When the default server deletes an additional server by the deregistration request, it then sends 
	 * the NOTIFY_SERVER_LEAVE event to clients. The event fields of the event are described below.
	 * 
	 * <table border=1>
	 * <caption>CMMultiServerEvent.NOTIFY_SERVER_LEAVE event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_MULTI_SERVER_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMMultiServerEvent.NOTIFY_SERVER_LEAVE</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Server name</td><td>String</td><td>Name of an additional server that leaves the CM network</td>
	 * <td>getServerName()</td>
	 * </tr>
	 * </table>
	 * 
	 */
	public void requestServerInfo()
	{
		CMUser myself = getMyself();
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
	
	/**
	 * Connects to a CM server.
	 * 
	 * <p>If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can connect to these servers by specifying a server name as the parameter. 
	 * Connection to an additional server is made with an additional TCP channel created.
	 * 
	 * @param strServerName - the server name
	 * @return true if the connection is successfully established; or false otherwise.
	 * @see CMClientStub#connectToServer()
	 * @see CMServerStub#connectToServer()
	 */
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
	
	/**
	 * Disconnects from a CM server.
	 * 
	 * <p> If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can disconnect from these servers by specifying a server name as the parameter. 
	 * 
	 * @param strServerName - the server name
	 * @return true if the connection is successfully disconnected; or false otherwise.
	 * @see CMClientStub#disconnectFromServer()
	 * @see CMServerStub#disconnectFromServer()
	 */
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
	
	/**
	 * Logs in to a CM server.
	 * 
	 * <p> If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can log in to these servers by specifying a server name.  
	 * <br> The login process to an additional CM server is the almost same as that to the default server 
	 * with the {@link CMClientStub#loginCM(String, String)} method.
	 * Different part is that the login to an additional server requires the target server name, and that 
	 * the multiple-server-related CM event is the {@link CMMultiServerEvent} class instead of the {@link CMSessionEvent}. 
	 * Each event ID of the CMMultiServerEvent is preceded by the "ADD_" and the remaining ID word and its 
	 * role is the same as that of the CMSessionEvent class. Event fields of the CMMultiServerEvent event is also 
	 * the same as those of the CMSessionEvent except an additional field, the server name.
	 * 
	 * @param strServer - the server name
	 * @param strUser - the user name
	 * @param strPasswd - the password
	 * @see CMClientStub#loginCM(String, String)
	 */
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
	
	/**
	 * Logs out from a CM server.
	 * 
	 * <p> If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can log from one of these servers by specifying a server name.  
	 * <br> The logout process from an additional CM server is the almost same as that from the default server 
	 * with the {@link CMClientStub#logoutCM()} method.
	 * Different part is that the logout from an additional server requires the target server name, and that 
	 * the multiple-server-related CM event is the {@link CMMultiServerEvent} class instead of the {@link CMSessionEvent}. 
	 * Each event ID of the CMMultiServerEvent is preceded by the "ADD_" and the remaining ID word and its 
	 * role is the same as that of the CMSessionEvent class. Event fields of the CMMultiServerEvent event is also 
	 * the same as those of the CMSessionEvent except an additional field, the server name.
	 * 
	 * @param strServer - the server name
	 * @see CMClientStub#logoutCM()
	 */
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
		CMChannelInfo<Integer> chInfo = tserver.getNonBlockSocketChannelInfo();
		chInfo.removeAllAddedChannels(0);
		chInfo = tserver.getBlockSocketChannelInfo();
		chInfo.removeAllChannels();

		// make and send event
		CMMultiServerEvent tmse = new CMMultiServerEvent();
		tmse.setID(CMMultiServerEvent.ADD_LOGOUT);
		tmse.setServerName(strServer);
		tmse.setUserName(getMyself().getName());

		send(tmse, strServer);

		// update the local state of the server
		tserver.setClientState(CMInfo.CM_CONNECT);

		tmse = null;
		return;
	}
	
	/**
	 * Requests available session information from a CM server.
	 * 
	 * <p> If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can request session information from one of these servers by specifying a server name.  
	 * <br> The session-information-request process with an additional CM server is the almost same as that 
	 * with the default server using the {@link CMClientStub#requestSessionInfo()} method.
	 * Different part is that the request from an additional server requires the target server name, and that 
	 * the multiple-server-related CM event is the {@link CMMultiServerEvent} class instead of the {@link CMSessionEvent}. 
	 * Each event ID of the CMMultiServerEvent is preceded by the "ADD_" and the remaining ID word and its 
	 * role is the same as that of the CMSessionEvent class. Event fields of the CMMultiServerEvent event is also 
	 * the same as those of the CMSessionEvent except an additional field, the server name.
	 * 
	 * @param strServerName - the server name
	 * @see CMClientStub#requestSessionInfo()
	 */
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
		mse.setUserName(getMyself().getName());
		send(mse, strServerName);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMClientStub.requestSessionInfo(..) server("+strServerName+"), Ok");
		}
	
		mse = null;
		return;
	}
	
	/**
	 * Joins a session in a CM server.
	 * 
	 * <p> If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can join a session in one of these servers by specifying a server name.  
	 * <br> The session-join process with an additional CM server is the almost same as that 
	 * with the default server using the {@link CMClientStub#joinSession(String)} method.
	 * Different part is that the request from an additional server requires the target server name, and that 
	 * the multiple-server-related CM event is the {@link CMMultiServerEvent} class instead of the {@link CMSessionEvent}. 
	 * Each event ID of the CMMultiServerEvent is preceded by the "ADD_" and the remaining ID word and its 
	 * role is the same as that of the CMSessionEvent class. Event fields of the CMMultiServerEvent event is also 
	 * the same as those of the CMSessionEvent except an additional field, the server name.
	 * 
	 * @param strServer - the server name
	 * @param strSession - the session name
	 * @see CMClientStub#joinSession(String)
	 */
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
		mse.setUserName(getMyself().getName());
		mse.setSessionName(strSession);

		// send the event
		send(mse, strServer);
		// set current session of the client in the server info element
		tserver.setCurrentSessionName(strSession);

		mse = null;
		return;
	}
	
	/**
	 * Leaves the current session in a CM server.
	 * 
	 * <p> If the CM network has multiple servers (the default server and additional servers), 
	 * a CM client can leave the current session in one of these servers by specifying a server name.  
	 * <br> The session-leave process in an additional CM server is the almost same as that 
	 * in the default server using the {@link CMClientStub#leaveSession()} method.
	 * Different part is that the request from an additional server requires the target server name, and that 
	 * the multiple-server-related CM event is the {@link CMMultiServerEvent} class instead of the {@link CMSessionEvent}. 
	 * Each event ID of the CMMultiServerEvent is preceded by the "ADD_" and the remaining ID word and its 
	 * role is the same as that of the CMSessionEvent class. Event fields of the CMMultiServerEvent event is also 
	 * the same as those of the CMSessionEvent except an additional field, the server name.
	 * 
	 * @param strServer - the server name
	 * @see CMClientStub#leaveSession()
	 */
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
		mse.setUserName(getMyself().getName());
		mse.setSessionName( tserver.getCurrentSessionName() );

		send(mse, strServer);

		// update the client state of the server
		tserver.setClientState(CMInfo.CM_LOGIN);

		mse = null;
		return;
	}
	
	/**
	 * Registers a user to the default server.
	 * 
	 * <p> A user can be registered to CM by the registerUser method of the CM client stub. 
	 * If a CM client is connected to the default server, it can call this method. 
	 * CM uses the registered user information for the user authentication when a user logs in to the default server.
	 * 
	 * <p> Whether the registration request is successful or not is set to a return code of a reply session event, 
	 * REGISTER_USER_ACK. If the request is successful, the reply event also contains the registration time at the server. 
	 * The details of the REGISTER_USER_ACK event are described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.REGISTER_USER_ACK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.REGISTER_USER_ACK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Return code</td><td>int</td>
	 * <td>Result of the request <br>1: succeeded<br>0: failed
	 * </td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Requester user name</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Creation time</td><td>String</td><td>Time to register the user at the DB</td><td>getCreationTime()</td>
	 * </tr>
	 * </table>
	 * 
	 * @param strName - the user name
	 * @param strPasswd - the password
	 */
	public void registerUser(String strName, String strPasswd)
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
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
	
	/**
	 * Deregisters a user from the default server.
	 * 
	 * <p> A user can cancel his/her registration from CM by the deregisterUser method of 
	 * the CM client stub. If a client is connected to the default server, it can call this method. 
	 * When requested, CM removes the registered user information from the CM DB.
	 * <br> Whether the deregistration request is successful or not is set to a return code of 
	 * a reply session event, DEREGISTER_USER_ACK as described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.DEREGISTER_USER_ACK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.DEREGISTER_USER_ACK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Return code</td><td>int</td>
	 * <td>Result of the request <br>1: succeeded<br>0: failed
	 * </td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Requester user name</td><td>getUserName()</td>
	 * </tr>
	 * </table>
	 * 
	 * @param strName - the user name
	 * @param strPasswd - the password
	 */
	public void deregisterUser(String strName, String strPasswd)
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
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
	
	/**
	 * Finds a registered user.
	 * 
	 * <p> A user can search for another user by the findRegisteredUser method of the CM client stub. 
	 * If a client is connected to the default server, it can call this method. When requested, 
	 * CM provides the basic profile of the target user such as a name and registration time.
	 * <br> Whether the requested user is found or not is set to a return code of a reply session event, 
	 * FIND_REGISTERED_USER_ACK as described below.
	 * 
	 * <table border=1>
	 * <caption>CMSessionEvent.FIND_REGISTERED_USER_ACK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SESSION_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSessionEvent.FIND_REGISTERED_USER_ACK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Return code</td><td>int</td>
	 * <td>Result of the request <br>1: succeeded<br>0: failed
	 * </td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Requester user name</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Creation time</td><td>String</td><td>Time to create the user at DB</td><td>getCreationTime()</td>
	 * </tr>
	 * </table>
	 * 
	 * @param strName - the user name
	 */
	public void findRegisteredUser(String strName)
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
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
	
	/**
	 * Adds a new friend user.
	 * 
	 * <p> A client can add a user as its friend only if the user name has been registered to CM. 
	 * When the default server receives the request for adding a new friend, it first checks 
	 * if the friend is a registered user or not. If the friend is a registered user, the server 
	 * adds it to the friend table of the CM DB as a friend of the requesting user. Otherwise, 
	 * the request fails. In any case, the server sends the ADD_NEW_FRIEND_ACK event with a result 
	 * code to the requesting client so that it can figure out the request result.
	 * The detailed information of the ADD_NEW_FRIEND_ACK event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.ADD_NEW_FRIEND_ACK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SNS_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSNSEvent.ADD_NEW_FRIEND_ACK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Return code</td><td>int</td>
	 * <td>Result of the request <br>1: succeeded<br>0: failed
	 * </td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Requester user name</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Friend name</td><td>String</td><td>Friend name</td><td>getFriendName()</td>
	 * </tr>
	 * </table>
	 * 
	 * @param strFriendName - the friend name
	 * @see CMClientStub#removeFriend(String)
	 */
	public void addNewFriend(String strFriendName)
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.addNewFriend(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.ADD_NEW_FRIEND);
		se.setUserName(getMyself().getName());
		se.setFriendName(strFriendName);
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	/**
	 * Removes a friend.
	 * 
	 * <p> When the default server receives the request for deleting a friend, it searches for 
	 * the friend of the requesting user. If the friend is found, the server deletes 
	 * the corresponding entry from the friend table. Otherwise, the request fails. 
	 * The result of the request is sent to the requesting client as the REMOVE_FRIEND_ACK event 
	 * with a result code.
	 * The detailed information of the REMOVE_FRIEND_ACK event is described below.
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.REMOVE_FRIEND_ACK event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SNS_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMSNSEvent.REMOVE_FRIEND_ACK</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>Return code</td><td>int</td>
	 * <td>Result of the request <br>1: succeeded<br>0: failed
	 * </td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Requester user name</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Friend name</td><td>String</td><td>Friend name</td><td>getFriendName()</td>
	 * </tr>
	 * </table>
	 * 
	 * @param strFriendName - the friend name
	 * @see CMClientStub#addNewFriend(String)
	 */
	public void removeFriend(String strFriendName)
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.removeFriend(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REMOVE_FRIEND);
		se.setUserName(getMyself().getName());
		se.setFriendName(strFriendName);
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	/**
	 * Requests to retrieve current friends list of this client.
	 * 
	 * <p> Different SNS applications use the concept of a friend in different ways. 
	 * In some applications, a user can add another user in his/her friend list without 
	 * the agreement of the target user. In other applications, a user can add a friend 
	 * only if the other user accepts the friend request. CM supports such different policies 
	 * of the friend management by methods that request different user lists. 
	 * <br>The requestFriendsList method requests the list of users whom the requesting user adds 
	 * as his/her friends regardless of the acceptance of the others. 
	 * <br>The {@link CMClientStub#requestFriendRequestersList()} method requests the list of users 
	 * who add the requesting user as a friend, but whom the requesting user has not added 
	 * as friends yet. 
	 * <br>The {@link CMClientStub#requestBiFriendsList()} method requests the list 
	 * of users who add the requesting user as a friend and whom the requesting user adds as friends. 
	 * 
	 * <p> When the default server receives the request for friends, requesters, or bi-friends 
	 * from a client, it sends corresponding user list as the RESPONSE_FRIEND_LIST, 
	 * RESPONSE_FRIEND_REQUESTER_LIST, or RESPONSE_BI_FRIEND_LIST event to the requesting client. 
	 * The three events have the same event fields as described below. 
	 * One of the event fields is the friend list, but the meaning of the list is different 
	 * according to an event ID. The friend list contains a maximum of 50 user names. 
	 * If the total number exceeds 50, the server then sends the event more than once.
	 * 
	 * <table border=1>
	 * <caption>CMSNSEvent.RESPONSE_FRIEND_LIST, RESPONSE_FRIEND_REQUESTER_LIST, 
	 * RESPONSE_BI_FRIEND_LIST events</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_SNS_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td>
	 * <td>CMSNSEvent.RESPONSE_FRIEND_LIST<br>CMSNSEvent.RESPONSE_FRIEND_REQUESTER_LIST
	 * <br>CMSNSEvent.RESPONSE_BI_FRIEND_LIST</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>User name</td><td>String</td><td>Requester user name</td><td>getUserName()</td>
	 * </tr>
	 * <tr>
	 * <td>Total number of friends</td><td>int</td><td>Total number of requested friends</td>
	 * <td>getTotalNumFriends()</td>
	 * </tr>
	 * <tr>
	 * <td>Number of friends</td><td>int</td><td>Number of requested friends in this event</td>
	 * <td>getNumFriends()</td>
	 * </tr>
	 * <tr>
	 * <td>Friend list</td><td>ArrayList&lt;String&gt;</td><td>List of requested friend names</td>
	 * <td>getFriendList()</td>
	 * </tr>
	 * </table>
	 * 
	 * @see CMClientStub#requestFriendRequestersList()
	 * @see CMClientStub#requestBiFriendsList()
	 */
	public void requestFriendsList()
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestFriendsList(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_FRIEND_LIST);
		se.setUserName(getMyself().getName());
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	/**
	 * Requests to retrieve current friend-requesters list of this client.
	 * 
	 * <p> The detailed information is described in the {@link CMClientStub#requestFriendsList()} method.
	 * 
	 * @see CMClientStub#requestFriendsList()
	 * @see CMClientStub#requestBiFriendsList()
	 */
	public void requestFriendRequestersList()
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestFriendRequestersList(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_FRIEND_REQUESTER_LIST);
		se.setUserName(getMyself().getName());
		send(se, "SERVER");
		
		se = null;
		return;
	}
	
	/**
	 * Requests to retrieve current bi-friends list of this client.
	 * 
	 * <p> The detailed information is described in the {@link CMClientStub#requestFriendsList()} method.
	 * 
	 * @see CMClientStub#requestFriendsList()
	 * @see CMClientStub#requestFriendRequestersList()
	 */
	public void requestBiFriendsList()
	{
		int nState = -1;

		// check if the user is connected to a default server
		nState = getMyself().getState();
		if( nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.out.println("CMClientStub.requestBiFriendsList(), you should log in to "
					+ "the default server!");
			return;
		}
		
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.REQUEST_BI_FRIEND_LIST);
		se.setUserName(getMyself().getName());
		send(se, "SERVER");
		
		se = null;
		return;
	}
}
