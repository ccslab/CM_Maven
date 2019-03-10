package kr.ac.konkuk.ccslab.cm.manager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMGroupInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMServerInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMSessionInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMEventInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMSNSInfo;
import kr.ac.konkuk.ccslab.cm.info.CMThreadInfo;

import java.sql.*;

public class CMInteractionManager {

	// initialize the interaction info object in cmInfo
	public static boolean init(CMInfo cmInfo)
	{
		CMCommInfo commInfo = cmInfo.getCommInfo();
		
		// initialize DB
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(confInfo.isDBUse() && CMConfigurator.isDServer(cmInfo))
		{
			CMDBManager.init(cmInfo);
		}
		
		// check the system type
		String strSysType = confInfo.getSystemType();
		if(!strSysType.equals("SERVER") && !strSysType.equals("CLIENT"))
		{
			System.out.println("CMInteractionInfo.init(), wrong system type ("+strSysType+").");
			return false;
		}
		
		// open a server socket channel in the case of a server
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		if(strSysType.equals("SERVER"))
		{
			myself.setName("SERVER");
			ServerSocketChannel ssc = null;
			try {
				ssc = (ServerSocketChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_SERVER_CHANNEL, 
						confInfo.getMyAddress(), confInfo.getMyPort(), cmInfo);
				commInfo.setNonBlockServerSocketChannel(ssc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		
		// open a stream socket channel in the case of a client or a server which is not a default server
		if(strSysType.equals("CLIENT") || (strSysType.equals("SERVER") && !CMConfigurator.isDServer(cmInfo)))
		{
			boolean ret = connectDefaultServer(cmInfo);
			if(!ret)
			{
				System.out.println("CMInteractionManager.init(), connection to the default server FAILED.");
				return false;
			}
		}
		
		// open a datagram channel
		DatagramChannel dc = null;
		try {
			dc = (DatagramChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_DATAGRAM_CHANNEL, 
					confInfo.getMyAddress(), confInfo.getUDPPort(), cmInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		// store the datagram channel
		commInfo.getNonBlockDatagramChannelInfo().addChannel(confInfo.getUDPPort(), dc);
		
		// set session info
		createSession(cmInfo);
	
		// initialize sessions
		CMSessionManager.init(cmInfo);
		
		// initialize file manager
		CMFileTransferManager.init(cmInfo);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMInteractionManager.init(), succeeded.");
		
		return true;
	}
	
	public static void terminate(CMInfo cmInfo)
	{
		CMFileTransferManager.terminate(cmInfo);		
	}
	
	public static boolean connectDefaultServer(CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = interInfo.getMyself();
		SocketChannel sc = null;
		CMServer dServer = null;
		
		// check the user state
		if(myself.getState() != CMInfo.CM_INIT)
		{
			System.out.println("CMInteractionManager.connectDefaultServer(), already connected to the default server.");
			return false;
		}
		
		// connection establishment to the default server
		try {
			sc = (SocketChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
					confInfo.getServerAddress(), confInfo.getServerPort(), cmInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		// store server info and channel
		dServer = interInfo.getDefaultServerInfo();
		dServer.setServerName("SERVER");	// name of the default server
		dServer.setServerAddress(confInfo.getServerAddress());
		dServer.setServerPort(confInfo.getServerPort());
		dServer.getNonBlockSocketChannelInfo().addChannel(0, sc);	// default channel number: 0
		
		// update the user's state
		myself.setState(CMInfo.CM_CONNECT);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMInteractionManager.connectDefaultServer(), succeeded.");
		
		return true;
	}
	
	public static boolean disconnectFromDefaultServer(CMInfo cmInfo)
	{
		// check user's state
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		if(myself.getState() == CMInfo.CM_INIT)
		{
			System.out.println("Not connected to default server yet.");
			return false;
		}
		
		// remove all channels to the default server
		CMServer dsInfo = interInfo.getDefaultServerInfo();
		dsInfo.getNonBlockSocketChannelInfo().removeAllChannels();
		dsInfo.getBlockSocketChannelInfo().removeAllChannels();
		
		// update user's state
		myself.setState(CMInfo.CM_INIT);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.disconnectFromDefautServer(), succeeded.");
		}
		
		return true;
	}
	
	// connect to an additional server
	public static boolean connectAddServer(String strName, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer tserver = null;

		tserver = interInfo.findAddServer(strName);
		if( tserver == null )
		{
			System.out.println("CMInteractionManager.connectAddServer(), server("+strName
					+") info not found in the add-server info list.");
			return false;
		}
		
		if( tserver.getClientState() != CMInfo.CM_INIT )
		{
			System.out.println("Already connected to the server("+strName+").");
			return false;
		}

		SelectableChannel sc = null;
		try {
			sc = CMCommManager.openNonBlockChannel(CMInfo.CM_SOCKET_CHANNEL, tserver.getServerAddress(), 
										tserver.getServerPort(), cmInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if( sc == null )
		{
			System.out.println("CMInteractionManager.connectAddServer(), fail to connect to "
					+ "the server("+strName+").");
			return false;
		}

		// add channel info
		tserver.getNonBlockSocketChannelInfo().addChannel(0, sc);
		
		// update peer's state
		tserver.setClientState(CMInfo.CM_CONNECT);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.connectAddServer() successfully connect to "
					+ "server("+strName+").");
		}

		return true;
	}
	
	// disconnect from an additional server
	public static boolean disconnectFromAddServer(String strName, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer tserver = interInfo.findAddServer(strName);
		if( tserver == null )
		{
			System.out.println("CMInteractionManager.disconnectFromAddServer(), server("
					+strName+") info not found in the add-server info list.");
			return false;
		}

		if( tserver.getClientState() == CMInfo.CM_INIT )
		{
			System.out.println("CMInteractionManager.disconnectFromAddServer(), not yet connected "
					+ "to server("+strName+")!");
			return false;
		}

		// close and delete all channels of the server
		tserver.getNonBlockSocketChannelInfo().removeAllChannels();
		tserver.getBlockSocketChannelInfo().removeAllChannels();

		// update peer's state
		tserver.setClientState(CMInfo.CM_INIT);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.disconnectFromAddServer(), "
					+ "successfully disconnected from server("+strName+").");
		}
		
		return true;
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	
	public static boolean processEvent(CMMessage msg, CMInfo cmInfo)
	{
		boolean bReturn = true;	// the flag of whether the event will be forwarded to the application or not
		CMEvent cmEvent = null;

		// unmarshall an event
		cmEvent = CMEventManager.unmarshallEvent(msg.m_buf);
		if(cmEvent == null)
		{
			System.err.println("CMInteractionManager.processEvent(), unmarshalled event is null.");
			return false;
		}

		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("---- CMInteractionManager.processEvent() starts. event(type: "
					+cmEvent.getType()+", id: "+cmEvent.getID()+").");
		}

		// check session handler
		String strHandlerSession = cmEvent.getHandlerSession();
		String strHandlerGroup = cmEvent.getHandlerGroup();
		if(strHandlerSession != null && !strHandlerSession.equals(""))
		{
			// deliver msg to session manager

			if(strHandlerGroup != null && !strHandlerGroup.equals(""))
				CMGroupManager.processEvent(msg, cmInfo);
			else
				CMSessionManager.processEvent(msg, cmInfo);
		}
		else
		{
			int nEventType = cmEvent.getType();
			switch(nEventType)
			{
			case CMInfo.CM_FILE_EVENT:
				CMFileTransferManager.processEvent(msg, cmInfo);
				break;
			case CMInfo.CM_SNS_EVENT:
				CMSNSManager.processEvent(msg, cmInfo);
				break;
			case CMInfo.CM_SESSION_EVENT:
				bReturn = processSessionEvent(msg, cmInfo);
				break;
			case CMInfo.CM_MULTI_SERVER_EVENT:
				processMultiServerEvent(msg, cmInfo);
				break;
			case CMInfo.CM_USER_EVENT:
				if(CMInfo._CM_DEBUG)
					System.out.println("CMInteractionManager.processEvent(), user event, nothing to do.");
				break;
			default:
				System.err.println("CMInteractionManager.processEvent(), unknown event type: "
						+nEventType);
				cmEvent = null;
				return true;
			}
		}
		
		// distribution to other session members or group members, if required
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			if(msg.m_ch instanceof SocketChannel)
			{
				distributeEvent(cmEvent.getDistributionSession(), cmEvent.getDistributionGroup(), 
						cmEvent, CMInfo.CM_STREAM, cmInfo);
			}
			else if(msg.m_ch instanceof DatagramChannel)
			{
				distributeEvent(cmEvent.getDistributionSession(), cmEvent.getDistributionGroup(),
						cmEvent, CMInfo.CM_DATAGRAM, cmInfo);
			}
		}
		
		if(CMInfo._CM_DEBUG_2)
			System.out.println("CMInteractionManager.processEvent() ends.");
		
		// clear event object (message object is cleared at the EventReceiver)
		cmEvent = null;
		
		return bReturn;
	}
	
	/////////////////////////////////////////////////////////////////////
	// private methods
	private static void createSession(CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		if(confInfo.getSystemType().equals("SERVER"))
		{
			int nSessionNum = confInfo.getSessionNumber();
			if(nSessionNum <= 0)
			{
				System.out.println("CMInteractionManager.createSession(), incorrect number of sessions: "
						+nSessionNum);
				return;
			}
			
			String strConfPath = confInfo.getConfFileHome().resolve("cm-server.conf").toString();
			
			for(int i=1; i <= nSessionNum; i++)
			{
				CMSession session = new CMSession();
				String strSessionName = null;
				String strSessionConfFileName = null;
				strSessionName = CMConfigurator.getConfiguration(strConfPath, "SESSION_NAME"+i);
				strSessionConfFileName = CMConfigurator.getConfiguration(strConfPath, "SESSION_FILE"+i);

					session.setSessionName(strSessionName);
				session.setAddress(confInfo.getMyAddress());
				session.setPort(confInfo.getMyPort());
				session.setSessionConfFileName(strSessionConfFileName);
				interInfo.addSession(session);
			}
		}
		else if(confInfo.getSystemType().equals("CLIENT"))
		{
			// default session (without name) is added
			//CMSession session = new CMSession();
			//interInfo.addSession(session);
			
			// a session will be created when this client receives JOIN_SESSION_ACK event
		}
	}

	private static boolean processSessionEvent(CMMessage msg, CMInfo cmInfo)
	{
		boolean bForward = true;
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		int nEventID = se.getID();
		switch(nEventID)
		{
		case CMSessionEvent.LOGIN:
			bForward = processLOGIN(msg, cmInfo);
			break;
		case CMSessionEvent.LOGIN_ACK:
			processLOGIN_ACK(msg, cmInfo);
			break;
		case CMSessionEvent.LOGOUT:
			processLOGOUT(msg, cmInfo);
			break;
		case CMSessionEvent.SESSION_ADD_USER:
			processSESSION_ADD_USER(msg, cmInfo);
			break;
		case CMSessionEvent.SESSION_REMOVE_USER:
			processSESSION_REMOVE_USER(msg, cmInfo);
			break;
		case CMSessionEvent.CHANGE_SESSION:
			processCHANGE_SESSION(msg, cmInfo);
			break;
		case CMSessionEvent.REQUEST_SESSION_INFO:
			processREQUEST_SESSION_INFO(msg, cmInfo);
			break;
		case CMSessionEvent.RESPONSE_SESSION_INFO:
			processRESPONSE_SESSION_INFO(msg, cmInfo);
			break;
		case CMSessionEvent.SESSION_TALK:
			processSESSION_TALK(msg, cmInfo);
			break;
		case CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL:
			processADD_NONBLOCK_SOCKET_CHANNEL(msg, cmInfo);
			break;
		case CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK:
			processADD_NONBLOCK_SOCKET_CHANNEL_ACK(msg, cmInfo);
			break;
		case CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL:
			processADD_BLOCK_SOCKET_CHANNEL(msg, cmInfo);
			break;
		case CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK:
			processADD_BLOCK_SOCKET_CHANNEL_ACK(msg, cmInfo);
			break;
		case CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL:
			processREMOVE_BLOCK_SOCKET_CHANNEL(msg, cmInfo);
			break;
		case CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL_ACK:
			processREMOVE_BLOCK_SOCKET_CHANNEL_ACK(msg, cmInfo);
			break;
		case CMSessionEvent.REGISTER_USER:
			processREGISTER_USER(msg, cmInfo);
			break;
		case CMSessionEvent.REGISTER_USER_ACK:
			processREGISTER_USER_ACK(se, cmInfo);
			break;
		case CMSessionEvent.DEREGISTER_USER:
			processDEREGISTER_USER(msg, cmInfo);
			break;
		case CMSessionEvent.DEREGISTER_USER_ACK:
			processDEREGISTER_USER_ACK(se, cmInfo);
			break;
		case CMSessionEvent.FIND_REGISTERED_USER:
			processFIND_REGISTERED_USER(msg, cmInfo);
			break;
		case CMSessionEvent.FIND_REGISTERED_USER_ACK:
			processFIND_REGISTERED_USER_ACK(se, cmInfo);
			break;
		default:
			System.out.println("CMInteractionManager.processSessionEvent(), unknown event ID: "
					+nEventID);
			se = null;
			return false;
		}
		
		se = null;
		return bForward;
	}

	private static boolean processLOGIN(CMMessage msg, CMInfo cmInfo)
	{
		boolean bForward = true;
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMMember loginUsers = null;
		
		if(confInfo.getSystemType().equals("SERVER"))
		{
			// check if the user already has logged in or not
			CMSessionEvent se = new CMSessionEvent(msg.m_buf);
			loginUsers = interInfo.getLoginUsers();
			if(loginUsers.isMember(se.getUserName()))
			{
				// send LOGIN_ACK event saying that the user already has logged into the server
				bForward = false;
				if(CMInfo._CM_DEBUG)
				{
					System.err.println("CMInteractionManager.processLOGIN(), user("+
							se.getUserName()+") already has logged into the server!");
				}
				
				CMSessionEvent seAck = new CMSessionEvent();
				seAck.setID(CMSessionEvent.LOGIN_ACK);
				seAck.setValidUser(-1);	// already-logged user
				CMEventManager.unicastEvent(seAck, (SocketChannel)msg.m_ch, cmInfo);
			}
			else
			{
				CMUser tuser = new CMUser();

				tuser.setName(se.getUserName());
				tuser.setPasswd(se.getPassword());
				tuser.setHost(se.getHostAddress());
				tuser.setUDPPort(se.getUDPPort());
				
				tuser.getNonBlockSocketChannelInfo().addChannel(0, msg.m_ch);
				loginUsers.addMember(tuser);
				
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMInteractionManager.processLOGIN(), add new user("+
							se.getUserName()+"), # longin users("+loginUsers.getMemberNum()+").");
				}

				if(!confInfo.isLoginScheme())
					replyToLOGIN(se, true, cmInfo);				
			}

			se = null;
			return bForward;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.err.println("CMInteractionManager.processLOGIN(); system type is not SERVER!");
		}
		return false;
	}

	public static void replyToLOGIN(CMSessionEvent se, boolean bValidUser, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMSessionEvent seAck = new CMSessionEvent();
		CMUser user = interInfo.getLoginUsers().findMember(se.getUserName());
		
		if(user == null)
		{
			System.out.println("CMInteractionManager.replyToLOGIN(), user("+se.getUserName()
					+") not found.");
			return;
		}

		seAck.setID(CMSessionEvent.LOGIN_ACK);
		seAck.setSender(interInfo.getMyself().getName());
		seAck.setReceiver(se.getSender());
		if(bValidUser)
			seAck.setValidUser(1);
		else
			seAck.setValidUser(0);

		seAck.setCommArch(confInfo.getCommArch());
		
		if(confInfo.isFileTransferScheme())
			seAck.setFileTransferScheme(1);
		else
			seAck.setFileTransferScheme(0);
		
		if(confInfo.isLoginScheme())
			seAck.setLoginScheme(1);
		else
			seAck.setLoginScheme(0);
		
		if(confInfo.isSessionScheme())
			seAck.setSessionScheme(1);
		else
			seAck.setSessionScheme(0);
		
		seAck.setAttachDownloadScheme(confInfo.getAttachDownloadScheme());	// default value
		
		seAck.setUDPPort(confInfo.getUDPPort());
		
		CMEventManager.unicastEvent(seAck, user.getName(), cmInfo);
		seAck = null;
		
		if(bValidUser)
		{
			// set default scheme for attachment download
			user.setAttachDownloadScheme(confInfo.getAttachDownloadScheme());
			// set login date
			user.setLastLoginDate(Calendar.getInstance());
			if(confInfo.getAttachDownloadScheme() == CMInfo.SNS_ATTACH_PREFETCH && confInfo.isDBUse())
			{
				// load history info for attachment access of this user
				CMSNSManager.loadAccessHistory(user, cmInfo);
			}

			// send inhabitants who already logged on the system to the new user
			distributeLoginUsers(user.getName(), cmInfo);
			
			// notify info. on new user who logged in
			CMSessionEvent tse = new CMSessionEvent();
			tse.setID(CMSessionEvent.SESSION_ADD_USER);
			tse.setUserName(user.getName());
			tse.setHostAddress(user.getHost());
			tse.setSessionName(user.getCurrentSession());
			CMEventManager.broadcastEvent(tse, cmInfo);
		}
		else
		{
			user.getNonBlockSocketChannelInfo().removeAllAddedChannels(0);
			user.getBlockSocketChannelInfo().removeAllChannels();
			interInfo.getLoginUsers().removeMember(user.getName());
		}

		return;
	}
	
	private static void distributeLoginUsers(String targetUser, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMSessionEvent tse = null;
		CMUser loginUser = null;
		
		Iterator<CMUser> iterUser = interInfo.getLoginUsers().getAllMembers().iterator();
		while(iterUser.hasNext())
		{
			loginUser = iterUser.next();
			if(!targetUser.equals(loginUser.getName()))
			{
				tse = new CMSessionEvent();
				tse.setID(CMSessionEvent.SESSION_ADD_USER);
				tse.setUserName(loginUser.getName());
				tse.setHostAddress(loginUser.getHost());
				tse.setSessionName(loginUser.getCurrentSession());
				CMEventManager.unicastEvent(tse, targetUser, cmInfo);
				tse = null;
			}
		}
		
	}
	
	private static void processLOGIN_ACK(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		SocketChannel sc = null;
		CMServer serverInfo = null;
		CMChannelInfo<Integer> scInfo = null;
		
		if(!confInfo.getSystemType().equals("CLIENT"))
			return;
		
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		confInfo.setCommArch(se.getCommArch());
		confInfo.setFileTransferScheme(se.isFileTransferScheme());
		confInfo.setLoginScheme(se.isLoginScheme());
		confInfo.setSessionScheme(se.isSessionScheme());
		confInfo.setAttachDownloadScheme(se.getAttachDownloadScheme());
		interInfo.getDefaultServerInfo().setServerUDPPort(se.getUDPPort());
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processLOGIN_ACK(), received.");
			System.out.println("bValidUser("+se.isValidUser()+"), comm arch("+se.getCommArch()
					+"), bFileTransferScheme("+se.isFileTransferScheme()+"), bLoginScheme("
					+se.isLoginScheme()+"), bSessionScheme("+se.isSessionScheme()
					+"), nAttachDwonloadScheme("+se.getAttachDownloadScheme()+"), server udp port("
					+se.getUDPPort()+").");
		}
		
		if(se.isValidUser() == 1)
		{
			// update client's state
			interInfo.getMyself().setState(CMInfo.CM_LOGIN);
			// set client's attachment download scheme
			interInfo.getMyself().setAttachDownloadScheme(se.getAttachDownloadScheme());
			// if the file trasnfer scheme is set, create a blocking TCP socket channel
			if(confInfo.isFileTransferScheme())
			{
				serverInfo = interInfo.getDefaultServerInfo();
				scInfo = serverInfo.getBlockSocketChannelInfo();
				try {
					sc = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
							serverInfo.getServerAddress(), serverInfo.getServerPort(), cmInfo);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
				
				if(sc == null)
				{
					System.err.println("CMInteractionMaanger.processLOGIN_ACK(), failed to create a blocking "
							+ "TCP socket channel to the default server!");
					return;
				}
				scInfo.addChannel(0, sc); // key for the default blocking TCP socket channel is 1

				CMSessionEvent tse = new CMSessionEvent();
				tse.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				tse.setChannelName(interInfo.getMyself().getName());
				tse.setChannelNum(0);
				CMEventManager.unicastEvent(tse, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true, cmInfo);
				se = null;

				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMInteractionManager.processLOGIN_ACK(),successfully requested to add "
							+ "the channel with the key(0) to the default server.");
				}
				
			}
			// request session information if session scheme is not used.
			if(!confInfo.isSessionScheme())
			{
				// set session name as default name (session1)
				//CMSession tsession = interInfo.getSessionList().elementAt(0);
				//tsession.setSessionName("session1");
				
				CMSessionEvent tse = new CMSessionEvent();
				tse.setID(CMSessionEvent.JOIN_SESSION);
				tse.setHandlerSession("session1");
				tse.setUserName(interInfo.getMyself().getName());
				tse.setSessionName("session1");
				
				CMEventManager.unicastEvent(tse, "SERVER", cmInfo);
				interInfo.getMyself().setCurrentSession("session1");
				tse = null;
			}
		}
		else
		{
			System.out.println("CMInteractionManager.processLOGIN_ACK(), invalid user.");
		}
		
		se = null;
		return;
	}
	
	private static void processSESSION_ADD_USER(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processSESSION_ADD_USER(), user("+se.getUserName()
						+"), host("+se.getHostAddress()+"), session("+se.getSessionName()+").");
			}
		}
		se = null;
		return;
	}
	
	private static void processSESSION_REMOVE_USER(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processSESSION_REMOVE_USER(), user("
						+se.getUserName()+").");
			}
		}
		se = null;
		return;
	}
	
	private static void processCHANGE_SESSION(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processCHANGE_SESSION(), user("+se.getUserName()
					+"), session("+se.getSessionName()+").");
		}

		if(confInfo.getSystemType().equals("SERVER"))	// Currently, this event is sent only to users (not servers) (not clear)
		{
			CMEventManager.broadcastEvent(se, cmInfo);
		}
		
		se = null;
		return;
	}
	
	private static void processLOGOUT(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMSNSInfo snsInfo = cmInfo.getSNSInfo();
		
		if(!confInfo.getSystemType().equals("SERVER"))
			return;
		
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		// find the user in the login user list
		CMUser user = interInfo.getLoginUsers().findMember(se.getUserName());
		if(user == null)
		{
			System.out.println("CMInteractionManager.processLOGOUT, user("+se.getUserName()
					+") not found.");
			return;
		}
		
		// stop all the file-transfer threads
		//List<Runnable> ftList = fInfo.getExecutorService().shutdownNow();	// wrong
		//if(CMInfo._CM_DEBUG)
		//	System.out.println("CMInteractionManager.processLOGOUT(); # shutdown threads: "+ftList.size());
		// remove all the ongoing file-transfer info with the user
		fInfo.removeRecvFileList(user.getName());
		fInfo.removeSendFileList(user.getName());
		// remove all the ongoing sns related file-transfer info about the user
		snsInfo.getPrefetchMap().removePrefetchList(user.getName());
		snsInfo.getRecvSNSAttachHashtable().removeSNSAttachList(user.getName());
		snsInfo.getSendSNSAttachHashtable().removeSNSAttachList(user.getName());
		
		if(confInfo.getAttachDownloadScheme() == CMInfo.SNS_ATTACH_PREFETCH && confInfo.isDBUse())
		{
			// save newly added or updated access history for the attachment of SNS content
			CMSNSManager.saveAccessHistory(user, cmInfo);
		}
		
		// leave session and group
		CMSessionManager.leaveSession(user, cmInfo);
		
		// close and remove all additional nonblocking socket channels of the user
		user.getNonBlockSocketChannelInfo().removeAllAddedChannels(0);
		// close and remove all blocking socket channels of the user
		user.getBlockSocketChannelInfo().removeAllChannels();
		// remove the user from login user list
		//interInfo.getLoginUsers().removeMember(user);
		//user = null;
		interInfo.getLoginUsers().removeMemberObject(user);
		
		// notify login users of the logout user
		CMSessionEvent tse = new CMSessionEvent();
		tse.setID(CMSessionEvent.SESSION_REMOVE_USER);
		tse.setUserName(se.getUserName());
		CMEventManager.broadcastEvent(tse, cmInfo);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processLOGOUT(), user("+se.getUserName()
					+"), # login users("+interInfo.getLoginUsers().getMemberNum()+").");
		}
		
		se = null;
		tse = null;
		return;
	}
	
	private static void processREQUEST_SESSION_INFO(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		if(!confInfo.getSystemType().equals("SERVER"))
		{
			return;
		}
		
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		if(!interInfo.getLoginUsers().isMember(se.getUserName()))
		{
			System.out.println("CMInteractinManager.processREQUEST_SESSION_INFO(), user("+se.getUserName()
					+") not found in the login user list.");
			se = null;
			return;
		}
		
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.RESPONSE_SESSION_INFO);
		seAck.setSender(interInfo.getMyself().getName());
		seAck.setReceiver(se.getSender());
		seAck.setSessionNum(confInfo.getSessionNumber());
		Iterator<CMSession> iterSession = interInfo.getSessionList().iterator();
		while(iterSession.hasNext())
		{
			CMSession tsession = iterSession.next();
			CMSessionInfo tInfo = new CMSessionInfo(tsession.getSessionName(), confInfo.getServerAddress(),
									confInfo.getServerPort(), tsession.getSessionUsers().getMemberNum());
			seAck.addSessionInfo(tInfo);
		}
		CMEventManager.unicastEvent(seAck, se.getUserName(), cmInfo);

		seAck.removeAllGroupInfoObjects();
		seAck = null;
		se = null;
		return;
	}
	
	private static void processRESPONSE_SESSION_INFO(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();

		if(!confInfo.getSystemType().equals("CLIENT"))
		{
			return;
		}
		
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		Iterator<CMSessionInfo> iter = se.getSessionInfoList().iterator();

		if(CMInfo._CM_DEBUG)
		{
			System.out.format("%-60s%n", "------------------------------------------------------------");
			System.out.format("%-20s%-20s%-10s%-10s%n", "name", "address", "port", "user num");
			System.out.format("%-60s%n", "------------------------------------------------------------");
		}

		while(iter.hasNext())
		{
			CMSessionInfo tInfo = iter.next();
			CMSession tSession = interInfo.findSession(tInfo.getSessionName());
			if(tSession == null)
			{
				tSession = new CMSession(tInfo.getSessionName(), tInfo.getAddress(), tInfo.getPort(),
						tInfo.getUserNum());
				interInfo.addSession(tSession);
			}
			else
			{
				tSession.setAddress(tInfo.getAddress());
				tSession.setPort(tInfo.getPort());
				tSession.setUserNum(tInfo.getUserNum());
			}
			
			if(CMInfo._CM_DEBUG)
			{
				System.out.format("%-20s%-20s%-10d%-10d%n", tInfo.getSessionName(), tInfo.getAddress(), tInfo.getPort(), tInfo.getUserNum());
			}
		}
		
		se.removeAllSessionInfoObjects();
		se = null;
		return;
	}
	
	private static void processSESSION_TALK(CMMessage msg, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			CMSessionEvent se = new CMSessionEvent(msg.m_buf);
			System.out.println("CMInteractionManager.processSESSION_TALK(), broadcasted by user("
					+se.getUserName()+")");
			System.out.println("chat: "+se.getTalk());
			se = null;
		}
		
		return;
	}
	
	private static void processADD_NONBLOCK_SOCKET_CHANNEL(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		String strChannelName = se.getChannelName();
		int nChIndex = se.getChannelNum();
		
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.ADD_NONBLOCK_SOCKET_CHANNEL_ACK);
		seAck.setSender(interInfo.getMyself().getName());
		seAck.setReceiver(se.getSender());
		seAck.setChannelName(interInfo.getMyself().getName());
		seAck.setChannelNum(nChIndex);

		CMUser user = interInfo.getLoginUsers().findMember(strChannelName);
		if(user == null)
		{
			System.out.println("CMInteractionManager.processADD_NONBLOCK_SOCKET_CHANNEL(), user("+strChannelName
					+") not found in the login user list.");
			seAck.setReturnCode(0);
			CMEventManager.unicastEvent(seAck, (SocketChannel) msg.m_ch, cmInfo);
			seAck = null;
			se = null;
			return;
		}
		boolean ret = user.getNonBlockSocketChannelInfo().addChannel(nChIndex, msg.m_ch);
		if(ret)
			seAck.setReturnCode(1);
		else
			seAck.setReturnCode(0);
		
		CMEventManager.unicastEvent(seAck, user.getName(), cmInfo);
		
		se = null;
		seAck = null;
		return;
	}
	
	private static void processADD_NONBLOCK_SOCKET_CHANNEL_ACK(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		
		if(se.getReturnCode() == 0)
		{
			String strServer = se.getChannelName();
			int nChIndex = se.getChannelNum();
			System.out.println("CMInteractionManager.processADD_NONBLOCK_SOCKET_CHANNEL_ACK() failed to add channel,"
					+"server("+strServer+"), channel key("+nChIndex+").");
			
			if(strServer.equals("SERVER"))
			{
				serverInfo = interInfo.getDefaultServerInfo();
			}
			else
			{
				serverInfo = interInfo.findAddServer(strServer);
			}
			serverInfo.getNonBlockSocketChannelInfo().removeChannel(nChIndex);
		}
		else
		{
			System.out.println("CMInteractionManager.processADD_NONBLOCK_SOCKET_CHANNEL_ACK(), succeeded for server("
					+se.getChannelName()+") channel key("+se.getChannelNum()+").");
		}
				
		se = null;
		return;
	}
	
	private static void processADD_BLOCK_SOCKET_CHANNEL(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMCommInfo commInfo = cmInfo.getCommInfo();
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		String strChannelName = se.getChannelName();
		int nChKey = se.getChannelNum();

		CMUser user = interInfo.getLoginUsers().findMember(strChannelName);
		if(user == null)
		{
			System.err.println("CMInteractionManager.processADD_BLOCK_SOCKET_CHANNEL(), user("+strChannelName
					+") not found in the login user list.");
			
			return;
		}
		
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL_ACK);
		seAck.setSender(interInfo.getMyself().getName());
		seAck.setReceiver(se.getSender());
		seAck.setChannelName(interInfo.getMyself().getName());
		seAck.setChannelNum(nChKey);

		// The receiving channel is included in the Selector with the nonblocking mode.
		// This channel must be taken out from the Selector and changed to the blocking mode.
		synchronized(Selector.class){
			SelectionKey selKey = msg.m_ch.keyFor(commInfo.getSelector());
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processADD_BLOCK_SOCKET_CHANNEL(); # registered ky in "
						+ "the selector before the cancel request of the key: "
						+ commInfo.getSelector().keys().size());
			}
			selKey.cancel();
			while(msg.m_ch.isRegistered())
			{
				try {
					Selector.class.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processADD_BLOCK_SOCKET_CHANNEL(); # registered key in "
						+ "the selector after the completion of the key cancellation: "
						+ commInfo.getSelector().keys().size());
			}
		}
		
		try {
			msg.m_ch.configureBlocking(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			seAck.setReturnCode(0);
			CMEventManager.unicastEvent(seAck, user.getName(), cmInfo);
			seAck = null;
			se = null;
			return;
		}
		
		boolean ret = user.getBlockSocketChannelInfo().addChannel(nChKey, msg.m_ch);
		if(ret)
			seAck.setReturnCode(1);
		else
			seAck.setReturnCode(0);
		
		CMEventManager.unicastEvent(seAck, user.getName(), cmInfo);
		
		se = null;
		seAck = null;
		return;
	}
	
	private static void processADD_BLOCK_SOCKET_CHANNEL_ACK(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		
		if(se.getReturnCode() == 0)
		{
			String strServer = se.getChannelName();
			int nChKey = se.getChannelNum();
			System.out.println("CMInteractionManager.processADD_BLOCK_SOCKET_CHANNEL_ACK() failed to add channel,"
					+"server("+strServer+"), channel key("+nChKey+").");
			
			if(strServer.equals("SERVER"))
			{
				serverInfo = interInfo.getDefaultServerInfo();
			}
			else
			{
				serverInfo = interInfo.findAddServer(strServer);
			}
			serverInfo.getBlockSocketChannelInfo().removeChannel(nChKey);
		}
		else
		{
			System.out.println("CMInteractionManager.processADD_BLOCK_SOCKET_CHANNEL_ACK(), succeeded for server("
					+se.getChannelName()+") channel key("+se.getChannelNum()+").");
		}
				
		se = null;
		return;
	}
	
	private static void processREMOVE_BLOCK_SOCKET_CHANNEL(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMUser user = null;
		CMChannelInfo<Integer> scInfo = null;
		SocketChannel sc = null;
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		String strChannelName = se.getChannelName();
		int nChKey = se.getChannelNum();
		ByteBuffer recvBuf = null;
		int nRecvBytes = -1;
		
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL_ACK);
		seAck.setSender(interInfo.getMyself().getName());
		seAck.setReceiver(se.getSender());
		seAck.setChannelName(interInfo.getMyself().getName());
		seAck.setChannelNum(nChKey);
		
		user = interInfo.getLoginUsers().findMember(strChannelName);
		if( user == null )
		{
			System.err.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL(), user("+strChannelName
					+") not found!");
			seAck.setReturnCode(0);
			CMEventManager.unicastEvent(seAck, (SocketChannel)msg.m_ch, cmInfo);
			seAck = null;
			se = null;
			return;
		}
		scInfo = user.getBlockSocketChannelInfo();
		sc = (SocketChannel) scInfo.findChannel(nChKey);
		if(sc == null)
		{
			System.err.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL(), channel not found! "
					+"user("+strChannelName+"), channel key("+nChKey+")");
			seAck.setReturnCode(0);
			CMEventManager.unicastEvent(seAck,  (SocketChannel)msg.m_ch, cmInfo);
			seAck = null;
			se = null;
			return;
		}
		
		// found the blocking channel that will be disconnected
		seAck.setReturnCode(1);	// ok
		CMEventManager.unicastEvent(seAck, (SocketChannel)msg.m_ch, cmInfo);
		seAck = null;
		se = null;
		
		try {
			recvBuf = ByteBuffer.allocate(Integer.BYTES);
			System.out.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL(),waiting for disconnection "
					+"from the client.");
			nRecvBytes = sc.read(recvBuf);	// wait for detecting the disconnection of this channel from the client
			if(CMInfo._CM_DEBUG)
				System.out.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL(), the number "
						+"of received bytes: "+nRecvBytes+" Bytes.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.err.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL(), disconnection detected "
					+"by the IOException!");
			
		} 

		// close the channel and remove the channel info
		try {
			sc.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scInfo.removeChannel(nChKey);
		
		return;
	}
	
	private static void processREMOVE_BLOCK_SOCKET_CHANNEL_ACK(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer serverInfo = null;
		CMChannelInfo<Integer> scInfo = null;
		SocketChannel sc = null;
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		int nChKey = se.getChannelNum();
		String strServer = se.getChannelName();
		boolean result = false;
		
		if(se.getReturnCode() == 1)
		{
			if(strServer.equals("SERVER"))
				serverInfo = interInfo.getDefaultServerInfo();
			else
				serverInfo = interInfo.findAddServer(strServer);
				
			if(serverInfo == null)
			{
				System.err.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL_ACK(), "
						+"server information not found: server("+strServer+"), channel key("+nChKey+")");
					
				return;
			}
				
			scInfo = serverInfo.getBlockSocketChannelInfo();
			sc = (SocketChannel) scInfo.findChannel(nChKey);
				
			if(sc == null)
			{
				System.err.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL_ACK(), "
						+"the socket channel not found: channel key("+nChKey+"), server("+strServer+")");
				
				return;
			}
							
			result = scInfo.removeChannel(nChKey);
			if(result)
			{
				if(CMInfo._CM_DEBUG)
					System.out.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL_ACK(), "
							+"succeeded : channel key("+nChKey+"), server("+strServer+")");
			}
			else
			{
				System.err.println("CMInteractionManager.processREMOVE_BLOCK_SOCKET_CHANNEL_ACK(), "
						+"failed to remove the channel : channel key("+nChKey+"), server("+strServer+")");
			}
			
			return;
		}
		else
		{
			System.err.println("CMInteractionManager.processREMOVE_BLOCK_CHANNEL_ACK(), the server fails to accept "
					+" the removal request of the channel: key("+nChKey+"), server("+strServer+")");
			return;			
		}
			
	}
	
	private static void processREGISTER_USER(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		String strQuery = null;
		ResultSet rs = null;
		int ret = -1;
		int nReturnCode = 0;
		String strCreationTime = "";
		
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);
		
		// process the request only if CM DB is configured to be used
		if( confInfo.isDBUse() )
		{
			// find if the user name already exists or not
			strQuery = "select * from  user_table where userName='"+se.getUserName()+"';";
			rs = CMDBManager.sendSelectQuery(strQuery, cmInfo);
			try {
				if( rs != null && rs.next() )
				{
					// the requested user already exists
					System.out.println("CMInteractionManager.processREGISTER_USER(), user("
							+se.getUserName()+") already exists in DB!");
				}
				else
				{
					// insert a new user
					ret = CMDBManager.queryInsertUser(se.getUserName(), se.getPassword(), cmInfo);
					if( ret == 1 )	// not clear
					{
						// get the inserted creationTime from DB
						strQuery = "select creationTime from user_table where userName='"
								+se.getUserName()+"';";
						rs = CMDBManager.sendSelectQuery(strQuery, cmInfo);
						rs.next();
						strCreationTime = rs.getString("creationTime");
						nReturnCode = 1;
					}
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				CMDBManager.closeDB(cmInfo);
				CMDBManager.closeRS(rs);
			}

		}
		else
		{
			System.out.println("CMInteractionManager.processREGISTER_USER(), CM DB not used!");
		}

		// send back an ack event
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.REGISTER_USER_ACK);
		seAck.setReturnCode(nReturnCode);
		seAck.setUserName(se.getUserName());
		seAck.setCreationTime(strCreationTime);
		CMEventManager.unicastEvent(seAck, (SocketChannel) msg.m_ch, cmInfo);

		se = null;
		seAck = null;
		return;
	}

	private static void processREGISTER_USER_ACK(CMSessionEvent se, CMInfo cmInfo)
	{

		if( se.getReturnCode() == 1 )
		{
			// user registration succeeded
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processREGISTER_USER_ACK(), user("
						+se.getUserName()+") registered at time("+se.getCreationTime()+").");
			}
		}
		else
		{
			// user registration failed
			System.out.println("CMInteractionManager.processREGISTER_USER_ACK(), FAILED for user("
					+se.getUserName()+")!");
		}
		
		return;
	}
	
	private static void processDEREGISTER_USER(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		String strQuery = null;
		ResultSet rs = null;
		int ret = -1;
		int nReturnCode = 0;	// 0 is error code

		CMSessionEvent se = new CMSessionEvent(msg.m_buf);

		// process the request only if CM DB is configured to be used
		if( confInfo.isDBUse() )
		{
			// check the user authentication (if user name and password are correct or not)
			strQuery = "select * from user_table where userName='"+se.getUserName()+"' and "
					+"password=PASSWORD('"+se.getPassword()+"');";
			rs = CMDBManager.sendSelectQuery(strQuery, cmInfo);
			try {
				if( rs != null && !rs.next() )
				{
					// authentication failed
					System.out.println("CMInteractionManager.processDEREGISTER_USER(), user name or "
							+ "password not correct! user("+se.getUserName()+").");
				}
				else
				{
					// delete a user from DB
					ret = CMDBManager.queryDeleteUser(se.getUserName(), cmInfo);
					if( ret == 1 ) // not clear
					{
						nReturnCode = 1;
					}
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				CMDBManager.closeDB(cmInfo);
				CMDBManager.closeRS(rs);
			}

		}
		else
		{
			System.out.println("CMInteractionManager.processDEREGISTER_USER(), CM DB not used!");
		}

		// send back an ack event
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.DEREGISTER_USER_ACK);
		seAck.setReturnCode(nReturnCode);
		seAck.setUserName(se.getUserName());
		CMEventManager.unicastEvent(seAck, (SocketChannel) msg.m_ch, cmInfo);
		
		se = null;
		seAck = null;
		return;
	}

	private static void processDEREGISTER_USER_ACK(CMSessionEvent se, CMInfo cmInfo)
	{
		if( se.getReturnCode() == 1 )
		{
			// user deregistration succeeded
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processDEREGISTER_USER_ACK(), user("
						+se.getUserName()+") deregistered.");
			}
		}
		else
		{
			// user registration failed
			System.out.println("CMInteractionManager.processDEREGISTER_USER_ACK(), FAILED for user("
					+se.getUserName()+")!");
		}
		
		return;
	}
	
	private static void processFIND_REGISTERED_USER(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		String strQuery = null;
		ResultSet rs = null;
		int nReturnCode = 0;
		String strCreationTime = "";
		
		CMSessionEvent se = new CMSessionEvent(msg.m_buf);

		// process the request only if CM DB is configured to be used
		if( confInfo.isDBUse() )
		{

			// make a search query
			strQuery = "select * from user_table where userName='"+se.getUserName()+"';";
			rs = CMDBManager.sendSelectQuery(strQuery, cmInfo);

			try {
				if( rs != null && !rs.next() )
				{
					// search failed
					System.out.println("CMInteractionManager.processFIND_REGISTERED_USER(), user("
							+se.getUserName()+") not found!");
				}
				else
				{
					// found the user
					System.out.println("CMInteractionManager.processFIND_REGISTERED_USER(), succeeded "
							+ "for user("+se.getUserName()+").");
					nReturnCode = 1;
					strCreationTime = rs.getString("creationTime");
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				CMDBManager.closeDB(cmInfo);
				CMDBManager.closeRS(rs);
			}
		}
		else
		{
			System.out.println("CMInteractionManager.processFIND_REGISTERED_USER(), CM DB is not "
					+ "used!");
		}

		// send back an ack event
		CMSessionEvent seAck = new CMSessionEvent();
		seAck.setID(CMSessionEvent.FIND_REGISTERED_USER_ACK);
		seAck.setReturnCode(nReturnCode);
		seAck.setUserName(se.getUserName());
		seAck.setCreationTime(strCreationTime);
		CMEventManager.unicastEvent(seAck, (SocketChannel) msg.m_ch, cmInfo);

		se = null;
		seAck = null;
		return;
	}
	
	private static void processFIND_REGISTERED_USER_ACK(CMSessionEvent se, CMInfo cmInfo)
	{
		if( se.getReturnCode() == 1 )
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processFIND_REGISTERED_USER_ACK(),"
						+ "succeeded: user("+se.getUserName()+"), registration time("
						+se.getCreationTime()+").");
			}
		}
		else
		{
			System.out.println("CMInteractionManager.processFIND_REGISTERED_USER_ACK(), "
					+ "failed for user("+se.getUserName()+")!");
		}
		return;
	}
	
	private static void processMultiServerEvent(CMMessage msg, CMInfo cmInfo)
	{
		CMMultiServerEvent mse = new CMMultiServerEvent(msg.m_buf);
		int nEventID = mse.getID();
		switch(nEventID)
		{
		case CMMultiServerEvent.REQ_SERVER_REG:
			processREQ_SERVER_REG(msg, cmInfo);
			break;
		case CMMultiServerEvent.RES_SERVER_REG:
			processRES_SERVER_REG(mse, cmInfo);
			break;
		case CMMultiServerEvent.REQ_SERVER_DEREG:
			processREQ_SERVER_DEREG(mse, cmInfo);
			break;
		case CMMultiServerEvent.RES_SERVER_DEREG:
			processRES_SERVER_DEREG(mse, cmInfo);
			break;
		case CMMultiServerEvent.NOTIFY_SERVER_INFO:
			processNOTIFY_SERVER_INFO(mse, cmInfo);
			break;
		case CMMultiServerEvent.NOTIFY_SERVER_LEAVE:
			processNOTIFY_SERVER_LEAVE(mse, cmInfo);
			break;
		case CMMultiServerEvent.REQ_SERVER_INFO:
			processREQ_SERVER_INFO(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_LOGIN:
			processADD_LOGIN(msg, cmInfo);
			break;
		case CMMultiServerEvent.ADD_LOGIN_ACK:
			processADD_LOGIN_ACK(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_LOGOUT:
			processADD_LOGOUT(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_SESSION_ADD_USER:
			processADD_SESSION_ADD_USER(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_SESSION_REMOVE_USER:
			processADD_SESSION_REMOVE_USER(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_REQUEST_SESSION_INFO:
			processADD_REQUEST_SESSION_INFO(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_RESPONSE_SESSION_INFO:
			processADD_RESPONSE_SESSION_INFO(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_JOIN_SESSION:
			processADD_JOIN_SESSION(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_JOIN_SESSION_ACK:
			processADD_JOIN_SESSION_ACK(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_LEAVE_SESSION:
			processADD_LEAVE_SESSION(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_CHANGE_SESSION:
			processADD_CHANGE_SESSION(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_JOIN_GROUP:
			processADD_JOIN_GROUP(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_GROUP_INHABITANT:
			processADD_GROUP_INHABITANT(mse, cmInfo);
			break;
		case CMMultiServerEvent.ADD_NEW_GROUP_USER:
			processADD_NEW_GROUP_USER(mse, cmInfo);
			break;
		default:
			System.out.println("CMInteractionManager.processMultiServerEvent(), unknown event ID: "
					+nEventID);
			mse = null;
			return;
		}
		
		mse = null;
		return;
	}
	
	private static void processREQ_SERVER_REG(CMMessage msg, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		String strServerName = null;
		String strServerAddress = null;
		int nServerPort = -1;
		int nServerUDPPort = -1;
		boolean bRet = false;
		
		CMMultiServerEvent mse = new CMMultiServerEvent(msg.m_buf);

		strServerName = mse.getServerName();
		strServerAddress = mse.getServerAddress();
		nServerPort = mse.getServerPort();
		nServerUDPPort = mse.getServerUDPPort();

		// add a new server info
		CMServer server = new CMServer(strServerName, strServerAddress, nServerPort, nServerUDPPort);
		server.getNonBlockSocketChannelInfo().addChannel(0, msg.m_ch);	// add default channel to the new server
		bRet = interInfo.addAddServer(server);

		// send response event
		CMMultiServerEvent mseAck = new CMMultiServerEvent();
		mseAck.setID( CMMultiServerEvent.RES_SERVER_REG );
		mseAck.setServerName( strServerName );
		if(bRet)
			mseAck.setReturnCode(1);
		else
			mseAck.setReturnCode(0);
		CMEventManager.unicastEvent(mseAck, (SocketChannel) msg.m_ch, cmInfo);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processREQ_SERVER_REG(), server("+strServerName
					+"), return code("+mseAck.getReturnCode()+").");
		}

		// notify it to existing users of the default server
		CMServerInfo sinfo = new CMServerInfo();
		sinfo.setServerName(strServerName);
		sinfo.setServerAddress(strServerAddress);
		sinfo.setServerPort(nServerPort);
		sinfo.setServerUDPPort(nServerUDPPort);

		mseAck = new CMMultiServerEvent();
		mseAck.setID( CMMultiServerEvent.NOTIFY_SERVER_INFO );
		mseAck.setServerNum(1);
		mseAck.addServerInfo(sinfo);
		CMEventManager.broadcastEvent(mseAck, cmInfo);

		mse = null;
		sinfo = null;
		mseAck = null;
		return;
	}
	
	private static void processRES_SERVER_REG(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		if(mse.getReturnCode() == 1)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processRES_SERVER_REG(), server("
						+mse.getServerName()+") is successfully registered to the default server.");
			}
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processRES_SERVER_REG(), server("
						+mse.getServerName()+") was not registered to the default server.");
			}
		}

		return;
	}
	
	private static void processREQ_SERVER_DEREG(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		boolean bRet = false;

		// delete a server info
		String serverName = mse.getServerName();
		if( !interInfo.isAddServer(serverName) )
			bRet = false;
		else
			bRet = true;

		// send a response event
		CMMultiServerEvent mseAck = new CMMultiServerEvent();
		mseAck.setID(CMMultiServerEvent.RES_SERVER_DEREG);
		mseAck.setServerName(serverName);
		if(bRet)
			mseAck.setReturnCode(1);
		else
			mseAck.setReturnCode(0);
		CMEventManager.unicastEvent(mseAck, serverName, cmInfo);
		
		if(bRet)
		{
			interInfo.removeAddServer(serverName);	// remove the requested server
		
			// notify a client of the deregistration
			mseAck = new CMMultiServerEvent();
			mseAck.setID(CMMultiServerEvent.NOTIFY_SERVER_LEAVE);
			mseAck.setServerName(serverName);
			CMEventManager.broadcastEvent(mseAck, cmInfo);
		}

		mseAck = null;
		return;
	}

	private static void processRES_SERVER_DEREG(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		if(mse.getReturnCode() == 1)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processRES_SERVER_DEREG(), server("
						+mse.getServerName()+") is successfully deregistered from the default server.");
			}
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processRES_SERVER_DEREG(), server("
						+mse.getServerName()+") was not deregistered from the default server.");
			}
		}

		return;
	}
	
	private static void processNOTIFY_SERVER_INFO(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		//A client must receive this event
		if( confInfo.getSystemType().equals("SERVER") )
		{
			System.out.println("CMInteractionManager.processNOTIFY_SERVER_INFO(), "
					+ "SERVER type does not need this event.");
			return;
		}

		Iterator<CMServerInfo> iter = mse.getServerInfoList().iterator();
		if(mse.getServerNum() != mse.getServerInfoList().size())
		{
			System.out.println("CMInteractionManager::processNOTIFY_SERVER_INFO(), "
					+ "server num field("+mse.getServerNum()+") and # list member("+mse.getServerInfoList().size()
					+") are different!");
			return;
		}

		//A client adds new server info (# server can be more than one)
		while(iter.hasNext())
		{
			CMServerInfo si = iter.next();
			if(interInfo.isAddServer(si.getServerName()))
			{
				System.out.println("CMInteractionManager.processNOTIFY_SERVER_INFO(), additional"
						+"server ("+si.getServerName()+") already exists!");
				continue;
			}
			else
			{
				CMServer addServer = new CMServer(si.getServerName(), si.getServerAddress(),
						si.getServerPort(), si.getServerUDPPort());
				interInfo.addAddServer(addServer);
			}
		}

		mse.removeAllServerInfoObjects();
		return;
	}
	
	private static void processNOTIFY_SERVER_LEAVE(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		//A client must receive this event
		if( confInfo.getSystemType().equals("SERVER") )
		{
			System.out.println("CMInteractionManager.processNOTIFY_SERVER_LEAVE(), SERVER type "
					+ "does not need this event.");
			return;
		}

		//A client removes the server info
		String serverName = mse.getServerName();
		if(!interInfo.isAddServer(serverName))
		{
			System.out.println("CMInteractionManager.processNOTIFY_SERVER_LEAVE(), additional "
					+"server ("+serverName+") not found.");
			return;
		}
		
		interInfo.removeAddServer(serverName);

		return;
	}
	
	private static void processREQ_SERVER_INFO(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		// send the list of server info
		CMMultiServerEvent mseAck = new CMMultiServerEvent();
		mseAck.setID(CMMultiServerEvent.NOTIFY_SERVER_INFO);
		mseAck.setServerNum( interInfo.getAddServerList().size() );
	
		Iterator<CMServer> iterAddServer = interInfo.getAddServerList().iterator();
		while(iterAddServer.hasNext())
		{
			CMServer tserver = iterAddServer.next();
			CMServerInfo si = new CMServerInfo(tserver.getServerName(), tserver.getServerAddress(),
					tserver.getServerPort(), tserver.getServerUDPPort());
			mseAck.addServerInfo(si);
		}
		CMEventManager.unicastEvent(mseAck, mse.getUserName(), cmInfo);

		mseAck.removeAllServerInfoObjects();
		mseAck = null;
		return;
	}
	
	private static void processADD_LOGIN(CMMessage msg, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();

		if(!confInfo.getSystemType().equals("SERVER"))
		{
			return;
		}
		
		CMMultiServerEvent mse = new CMMultiServerEvent(msg.m_buf);

		// omit the authentication process of the user name and password

		CMUser user = new CMUser();
		user.setName(mse.getUserName());
		user.setPasswd(mse.getPassword());
		user.setHost(mse.getHostAddress());
		user.setUDPPort(mse.getUDPPort());

		user.getNonBlockSocketChannelInfo().addChannel(0, msg.m_ch);
		interInfo.getLoginUsers().addMember(user);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_LOGIN(), add new user("
								+user.getName()+").");
		}

		if( !confInfo.isLoginScheme() )
			replyToADD_LOGIN(mse, true, cmInfo);

		mse = null;
		return;
	}
	
	public static void replyToADD_LOGIN(CMMultiServerEvent mse, boolean bValidUser, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser user = interInfo.getLoginUsers().findMember(mse.getUserName());

		CMMultiServerEvent mseAck = new CMMultiServerEvent();
		mseAck.setID(CMMultiServerEvent.ADD_LOGIN_ACK);
		mseAck.setServerName(mse.getServerName());
		if(bValidUser)
		{
			mseAck.setValidUser(1);
			mseAck.setCommArch(confInfo.getCommArch());
			if(confInfo.isLoginScheme())
				mseAck.setLoginScheme(1);
			else
				mseAck.setLoginScheme(0);
			if(confInfo.isSessionScheme())
				mseAck.setSessionScheme(1);
			else
				mseAck.setSessionScheme(0);
			mseAck.setServerUDPPort(confInfo.getUDPPort());
		}
		else
		{
			mseAck.setValidUser(0);
			mseAck.setCommArch("");
			mseAck.setLoginScheme(-1);
			mseAck.setSessionScheme(-1);
			mseAck.setUDPPort(-1);
		}
		
		CMEventManager.unicastEvent(mseAck, mse.getUserName(), cmInfo);

		if(bValidUser)
		{
			// send inhabitants who already logged on the system
			distributeAddLoginUsers(mse.getUserName(), cmInfo);

			// notify info. on new user who logged in
			CMMultiServerEvent tmse = new CMMultiServerEvent();
			tmse.setID(CMMultiServerEvent.ADD_SESSION_ADD_USER);
			tmse.setServerName(mse.getServerName());
			tmse.setUserName( mse.getUserName() );
			tmse.setHostAddress( mse.getHostAddress() );
			tmse.setSessionName("?");
			CMEventManager.broadcastEvent(tmse, cmInfo);
			tmse = null;
		}
		else
		{
			user.getNonBlockSocketChannelInfo().removeAllChannels();
			user.getBlockSocketChannelInfo().removeAllChannels();
			interInfo.getLoginUsers().removeMember(mse.getUserName());
		}
		
		mseAck = null;
		return;
	}
	
	private static void distributeAddLoginUsers(String strUser, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		Iterator<CMUser> iter = interInfo.getLoginUsers().getAllMembers().iterator();
		CMMultiServerEvent tmse = null;
		
		while(iter.hasNext())
		{
			CMUser tuser = iter.next();
			if(!strUser.equals(tuser.getName()))
			{
				tmse = new CMMultiServerEvent();
				tmse.setID(CMMultiServerEvent.ADD_SESSION_ADD_USER);
				tmse.setServerName(interInfo.getMyself().getName());
				tmse.setUserName(tuser.getName());
				tmse.setHostAddress(tuser.getHost());
				tmse.setSessionName(tuser.getCurrentSession());
				CMEventManager.unicastEvent(tmse, strUser, cmInfo);
			}
		}

		tmse = null;
		return;
	}
	
	private static void processADD_LOGIN_ACK(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer tserver = null;
		CMMultiServerEvent tmse = null;
		
		if(!confInfo.getSystemType().equals("CLIENT"))
		{
			return;
		}
		
		// get a corresponding server info
		tserver = interInfo.findAddServer(mse.getServerName());
		if( tserver == null )
		{
			System.out.println("CMInteractionManager.processADD_LOGIN_ACK(), "
					+ "server("+mse.getServerName()+") info not found!");
			return;
		}

		// set other info on the server in the server info instance
		tserver.setCommArch(mse.getCommArch());
		if(mse.isLoginScheme() == 1)
			tserver.setLoginScheme(true);
		else
			tserver.setLoginScheme(false);
		if(mse.isSessionScheme() == 1)
			tserver.setSessionScheme(true);
		else
			tserver.setSessionScheme(false);
		tserver.setServerUDPPort(mse.getServerUDPPort());

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_LOGIN_ACK(),");
			System.out.println("bValidUser("+mse.isValidUser()+"), commArch("+mse.getCommArch()
					+"), bLoginScheme("+mse.isLoginScheme()+"), bSessionScheme("+mse.isSessionScheme()
					+"), server udp port("+mse.getServerUDPPort()+").");
		}

		// update peer's state in the server info instance
		if( mse.isValidUser() == 1 )
		{
			tserver.setClientState(CMInfo.CM_LOGIN);
			// request session info. if no session scheme
			if( !tserver.isSessionScheme() )
			{
				tmse = new CMMultiServerEvent();
				// send the event
				tmse.setID(CMMultiServerEvent.ADD_JOIN_SESSION);
				tmse.setServerName( mse.getServerName() );
				tmse.setUserName( interInfo.getMyself().getName() );
				tmse.setSessionName("session1");	// default session name

				CMEventManager.unicastEvent(tmse, mse.getServerName(), cmInfo);
				tserver.setCurrentSessionName("session1");
				tmse = null;
			}
		}
		else
		{
			System.out.println("CMInteractionManager.processADD_LOGIN_ACK(), invalid user.");
		}

		return;
	}
	
	private static void processADD_SESSION_ADD_USER(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		
		if(!confInfo.getSystemType().equals("CLIENT"))
			return;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_SESSION_ADD_USER(), nothing to do at CM");
			System.out.println("server("+mse.getServerName()+"), user("+mse.getUserName()+"), host("
					+mse.getHostAddress()+"), session("+mse.getSessionName()+").");
		}
		return;
	}
	
	private static void processADD_LOGOUT(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		if(!confInfo.getSystemType().equals("SERVER"))
			return;
		
		CMUser user = interInfo.getLoginUsers().findMember(mse.getUserName());
		if(user == null)
		{
			System.out.println("CMInteractionManager.processADD_LOGOUT(), user("
					+mse.getUserName()+") not found in the login user list!");
			return;
		}
		
		CMSession session = interInfo.findSession(user.getCurrentSession());
		if(session != null)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMInteractionManager.processADD_LOGOUT(), user("
						+mse.getUserName()+") should leave session("+user.getCurrentSession()+").");
			}
			CMSessionManager.leaveSession(user, cmInfo);
		}
		
		user.getNonBlockSocketChannelInfo().removeAllAddedChannels(0); // main channel remained
		user.getBlockSocketChannelInfo().removeAllChannels();
		interInfo.getLoginUsers().removeMemberObject(mse.getUserName());
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_LOGOUT(), user("
					+mse.getUserName()+") removed from the login user list, member num(" 
					+interInfo.getLoginUsers().getMemberNum()+").");
		}

		// notify that a user logged out
		CMMultiServerEvent tmse = new CMMultiServerEvent();
		tmse.setID(CMMultiServerEvent.ADD_SESSION_REMOVE_USER);
		tmse.setServerName(mse.getServerName());
		tmse.setUserName( mse.getUserName() );

		CMEventManager.broadcastEvent(tmse, cmInfo);

		tmse = null;
		return;
	}
	
	private static void processADD_SESSION_REMOVE_USER(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		
		if(!confInfo.getSystemType().equals("CLIENT"))
			return;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_SESSION_REMOVE_USER(), nothing to do at CM");
			System.out.println("server("+mse.getServerName()+"), user("+mse.getUserName()+").");
		}
		return;
	}
	
	private static void processADD_REQUEST_SESSION_INFO(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		if(!confInfo.getSystemType().equals("SERVER"))
			return;
		
		CMUser user = interInfo.getLoginUsers().findMember(mse.getUserName()); 
		if( user == null )
		{
			System.out.println("CMInteractionManager.processADD_REQUEST_SESSION_INFO(), "
					+ "user("+mse.getUserName()+") not found in the login user list!");
			return;
		}
		
		CMMultiServerEvent tmse = new CMMultiServerEvent();

		tmse.setID(CMMultiServerEvent.ADD_RESPONSE_SESSION_INFO);
		tmse.setServerName(interInfo.getMyself().getName());
		tmse.setSessionNum(interInfo.getSessionList().size());
		Iterator<CMSession> iter = interInfo.getSessionList().iterator();
		while(iter.hasNext())
		{
			CMSession session = iter.next();
			CMSessionInfo si = new CMSessionInfo();
			si.setSessionName(session.getSessionName());
			si.setAddress(session.getAddress());
			si.setPort(session.getPort());
			si.setUserNum(session.getSessionUsers().getMemberNum());
			tmse.addSessionInfo(si);
		}
		CMEventManager.unicastEvent(tmse, mse.getUserName(), cmInfo);

		tmse.removeAllSessionInfoObjects();
		tmse = null;
		return;
	}
	
	private static void processADD_RESPONSE_SESSION_INFO(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer server = null;
		
		if(!confInfo.getSystemType().equals("CLIENT"))
			return;
		
		// find server info of the client
		server = interInfo.findAddServer(mse.getServerName());
		if( server == null )
		{
			System.out.println("CMInteractionManager.processADD_RESPONSE_SESSION_INFO(), "
					+ "server("+mse.getServerName()+") info not found in the add-server list!");
			return;
		}

		Iterator<CMSessionInfo> iter = mse.getSessionInfoList().iterator();

		if(CMInfo._CM_DEBUG)
		{
			System.out.format("%-60s%n", "------------------------------------------------------------");
			System.out.format("%-20s%-20s%-10s%-10s%n", "name", "address", "port", "user num");
			System.out.format("%-60s%n", "------------------------------------------------------------");
		}

		while(iter.hasNext())
		{
			CMSessionInfo tInfo = iter.next();
			CMSession tSession = server.findSession(tInfo.getSessionName());
			if(tSession == null)
			{
				tSession = new CMSession(tInfo.getSessionName(), tInfo.getAddress(), tInfo.getPort(),
						tInfo.getUserNum());
				server.addSession(tSession);
			}
			else
			{
				tSession.setAddress(tInfo.getAddress());
				tSession.setPort(tInfo.getPort());
				tSession.setUserNum(tInfo.getUserNum());
			}
			
			if(CMInfo._CM_DEBUG)
			{
				System.out.format("%-20s%-20s%-10d%-10d%n", tInfo.getSessionName(), tInfo.getAddress(), tInfo.getPort(), tInfo.getUserNum());
			}
		}
		
		mse.removeAllSessionInfoObjects();
		return;
	}
	
	private static void processADD_JOIN_SESSION(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMUser user = null;
		CMSession session = null;
		CMGroup group = null;
		
		if(!confInfo.getSystemType().equals("SERVER"))
			return;
		
		// find login user info
		user = interInfo.getLoginUsers().findMember(mse.getUserName());
		if(user == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_SESSION(), user("
					+mse.getUserName()+") not found in the login user list of server("
					+mse.getServerName()+").");
			return;
		}

		session = interInfo.findSession(mse.getSessionName());
		if(session == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_SESSION(), session("
					+mse.getSessionName()+") not found, user("+mse.getUserName()+"), server("
					+mse.getServerName()+").");
			return;
		}
		
		user.setCurrentSession( mse.getSessionName() );
		
		group = session.getGroupList().elementAt(0);	// first group
		if(group == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_SESSION(), no group info"
					+" in session("+session.getSessionName()+"), server("+mse.getServerName()+").");
			return;
		}

		user.setCurrentGroup(group.getGroupName());
		
		// request join process to the SM
		CMSessionManager.addJoinSession(user, cmInfo);

		// notify that a user changes session to all other users
		CMMultiServerEvent tmse = new CMMultiServerEvent();
		tmse.setID(CMMultiServerEvent.ADD_CHANGE_SESSION);
		tmse.setServerName( mse.getServerName() );
		tmse.setUserName( mse.getUserName() );
		tmse.setSessionName( mse.getSessionName() );

		CMEventManager.broadcastEvent(tmse, cmInfo);

		tmse = null;
		return;
	}
	
	private static void processADD_JOIN_SESSION_ACK(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		CMServer server = null;
		CMSession session = null;
		CMGroup group = null;
		
		if(!confInfo.getSystemType().equals("CLIENT"))
			return;
		
		server = interInfo.findAddServer(mse.getServerName());
		if(server == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_SESSION_ACK(), server("
					+mse.getServerName()+") not found in the add-server list.");
			return;
		}
		
		if( mse.getGroupInfoList().size() < 1 )
		{
			System.out.println("CMInteractionManager.processADD_JOIN_SESSION_ACK(), group info "
					+ "empty.");
			return;
		}
		
		// find a current session of the add-server
		session = server.findSession(server.getCurrentSessionName());
		if(session == null)
		{
			// create a new session
			session = new CMSession();
			session.setSessionName(server.getCurrentSessionName());
			server.addSession(session);
		}

		Iterator<CMGroupInfo> iter = mse.getGroupInfoList().iterator();
		while(iter.hasNext())
		{
			CMGroupInfo gi = iter.next();
			CMGroup tgroup = new CMGroup(gi.getGroupName(), gi.getGroupAddress(), gi.getGroupPort());
			session.addGroup(tgroup);
		}
		
		group = session.getGroupList().elementAt(0);	// first group
		// set current group name of the server
		server.setCurrentGroupName( group.getGroupName() );
		// initialize current group
		CMGroupManager.init(session.getSessionName(), group.getGroupName(), cmInfo);
		// update client state of the server
		server.setClientState(CMInfo.CM_SESSION_JOIN);

		// enter the current group
		CMMultiServerEvent tmse = new CMMultiServerEvent();
		tmse.setID(CMMultiServerEvent.ADD_JOIN_GROUP);
		tmse.setServerName( mse.getServerName() );
		tmse.setUserName( myself.getName() );
		tmse.setHostAddress( myself.getHost() );
		tmse.setUDPPort( myself.getUDPPort() );
		tmse.setSessionName( server.getCurrentSessionName() );
		tmse.setGroupName( server.getCurrentGroupName() );

		CMEventManager.unicastEvent(tmse, mse.getServerName(), cmInfo);

		tmse = null;
		return;
	}
	
	private static void processADD_LEAVE_SESSION(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMSession session = null;
		CMUser user = null;
		
		if(!confInfo.getSystemType().equals("SERVER"))
			return;
		
		// find a session
		session = interInfo.findSession(mse.getSessionName());
		if(session == null)
		{
			System.out.println("CMInteractionManager.processADD_LEAVE_SESSION(), session("
					+mse.getSessionName()+") not found in this server("+interInfo.getMyself().getName()
					+")!");
			return;
		}
		
		// find a session user
		user = session.getSessionUsers().findMember(mse.getUserName());
		if(user == null)
		{
			System.out.println("CMIntractionManager.processADD_LEAVE_SESSION(), user("
					+mse.getUserName()+") not found in session("+session.getSessionName()
					+") of this server("+interInfo.getMyself().getName()+")!");
			return;
		}
		
		CMSessionManager.addLeaveSession(user, cmInfo);
		
		// notify login users of the session leave
		CMMultiServerEvent tmse = new CMMultiServerEvent();
		tmse.setID(CMMultiServerEvent.ADD_CHANGE_SESSION);
		tmse.setServerName(interInfo.getMyself().getName());
		tmse.setUserName(user.getName());
		tmse.setSessionName("");
		CMEventManager.broadcastEvent(tmse, cmInfo);
		
		//// do not send LEAVE_SESSION_ACK (?)

		tmse = null;
		return;
	}
	
	private static void processADD_CHANGE_SESSION(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		// nothing to do with this event here
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_CHANGE_SESSION(), nothing to do "
					+"in the CM.");
			System.out.println("server("+mse.getServerName()+"), user("+mse.getUserName()+
					"), session("+mse.getSessionName()+").");
		}
		return;
	}
	
	private static void processADD_JOIN_GROUP(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMSession session = null;
		CMGroup group = null;
		CMUser user = null;
		
		// find a session
		session = interInfo.findSession(mse.getSessionName());
		if(session == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_GROUP(), session("
					+mse.getSessionName()+") not found!");
			return;
		}
		// find a group
		group = session.findGroup(mse.getGroupName());
		if(group == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_GROUP(), session("
					+mse.getSessionName()+") found, but group("+mse.getGroupName()+") not found!");
			return;
		}
		// find a user
		user = interInfo.getLoginUsers().findMember(mse.getUserName());
		if(user == null)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_GROUP(), user("
					+mse.getUserName()+") not found in the login user list.");
			return;
		}
		user.setCurrentGroup(mse.getGroupName());
		boolean ret = group.getGroupUsers().addMember(user);
		if(!ret)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_GROUP(), fail to add user("
					+user.getName()+") to group("+group.getGroupName()+") of session("
					+session.getSessionName()+").");
			return;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_JOIN_GROUP(), add user("
					+mse.getUserName()+") to group("+group.getGroupName()+") of session("
					+session.getSessionName()+"), # group users("
					+group.getGroupUsers().getMemberNum()+").");
		}

		// send the new user existing group member information
		CMGroupManager.addDistributeGroupUsers(user, cmInfo);
		
		// send group members the new user information
		CMGroupManager.addNotifyGroupUsersOfNewUser(user, cmInfo);

		return;
	}
	
	private static void processADD_GROUP_INHABITANT(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer tserver = null;
		CMSession tsession = null;
		CMGroup tgroup = null;
		
		if( confInfo.getSystemType().equals("SERVER") )
		{
			// If a server receives this event
			System.out.println("CMInteractionManager.processADD_GROUP_INHABITANT(), a server "
					+ "does not need this event.");
			return;
		}

		// find server info of the client
		tserver = interInfo.findAddServer(mse.getServerName());
		if(tserver == null)
		{
			System.out.println("CMInteractionManager.processADD_GROUP_INHABITANT(), server("
					+mse.getServerName()+") info not found!");
			return;
		}

		// check if the session name is the same as that of event
		tsession = tserver.findSession(mse.getSessionName());
		if(tsession == null)
		{
			System.out.println("CMInteractionManager.processADD_GROUP_INHABITANT(), session("
					+mse.getSessionName()+") not found in server("+tserver.getServerName()+").");
			return;
		}

		// find group
		tgroup = tsession.findGroup(mse.getGroupName());
		if(tgroup == null)
		{
			System.out.println("CMInteractionManager.processADD_GROUP_INHABITANT(), group("
					+mse.getGroupName()+" not found in session("+tsession.getSessionName()
					+"), server("+tserver.getServerName()+").");
			return;
		}

		CMUser myself = interInfo.getMyself();
		if(myself.getName().equals(mse.getUserName()))
		{
			System.out.println("CMInteractionManager.processADD_GROUP_INHABITANT(), user("
					+mse.getUserName()+") is myself. group("+tgroup.getGroupName()+"), session("
					+tsession.getSessionName()+"), server("+tserver.getServerName()+").");
			return;
		}
		
		// add the existing group member to the group of session of the server
		CMUser user = new CMUser();
		user.setName(mse.getUserName());
		user.setHost(mse.getHostAddress());
		user.setUDPPort(mse.getUDPPort());
		user.setCurrentSession(mse.getSessionName());
		user.setCurrentGroup(mse.getGroupName());

		tgroup.getGroupUsers().addMember(user);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_GROUP_INHABITANT(), user("
					+user.getName()+"), host("+user.getHost()+"), udpport("+user.getUDPPort()
					+"), current session("+user.getCurrentSession()+"), current group("
					+user.getCurrentGroup()+").");
		}
		
		return;
	}
	
	private static void processADD_NEW_GROUP_USER(CMMultiServerEvent mse, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMServer tserver = null;
		CMSession tsession = null;
		CMGroup tgroup = null;
		
		if( confInfo.getSystemType().equals("SERVER") )
		{
			// If a server receives this event
			System.out.println("CMInteractionManager.processADD_NEW_GROUP_USER(), a server "
					+ "does not need this event.");
			return;
		}

		// find server info of the client
		tserver = interInfo.findAddServer(mse.getServerName());
		if(tserver == null)
		{
			System.out.println("CMInteractionManager.processADD_NEW_GROUP_USER(), server("
					+mse.getServerName()+") info not found!");
			return;
		}

		// check if the session name is the same as that of event
		tsession = tserver.findSession(mse.getSessionName());
		if(tsession == null)
		{
			System.out.println("CMInteractionManager.processADD_NEW_GROUP_USER(), session("
					+mse.getSessionName()+") not found in server("+tserver.getServerName()+").");
			return;
		}

		// find group
		tgroup = tsession.findGroup(mse.getGroupName());
		if(tgroup == null)
		{
			System.out.println("CMInteractionManager.processADD_NEW_GROUP_USER(), group("
					+mse.getGroupName()+" not found in session("+tsession.getSessionName()
					+"), server("+tserver.getServerName()+").");
			return;
		}

		CMUser myself = interInfo.getMyself();
		if(myself.getName().equals(mse.getUserName()))
		{
			System.out.println("CMInteractionManager.processADD_NEW_GROUP_USER, user("
					+mse.getUserName()+") is myself. group("+tgroup.getGroupName()+"), session("
					+tsession.getSessionName()+"), server("+tserver.getServerName()+").");
			return;
		}
		
		// add the existing group member to the group of session of the server
		CMUser user = new CMUser();
		user.setName(mse.getUserName());
		user.setHost(mse.getHostAddress());
		user.setUDPPort(mse.getUDPPort());
		user.setCurrentSession(mse.getSessionName());
		user.setCurrentGroup(mse.getGroupName());

		tgroup.getGroupUsers().addMember(user);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMInteractionManager.processADD_NEW_GROUP_USER(), user("
					+user.getName()+"), host("+user.getHost()+"), udpport("+user.getUDPPort()
					+"), current session("+user.getCurrentSession()+"), current group("
					+user.getCurrentGroup()+").");
		}
		
		return;
	}

	// distribute an event to members according to session/group specifier in the event header
	private static void distributeEvent(String strDistSession, String strDistGroup, CMEvent cme, int opt, CMInfo cmInfo)
	{
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMUser tuser = null;
		CMSession tSession = null;
		CMMember tMember = null;
		CMGroup tGroup = null;
		
		if(strDistSession != null && !strDistSession.equals(""))
		{
			if(strDistSession.equals("CM_ONE_USER"))	// distribute to one user
			{
				tuser = interInfo.getLoginUsers().findMember(strDistGroup);
				if(tuser == null)
				{
					System.out.println("CMInteractionManager.distributeEvent(), target user("
							+strDistGroup+") not found.");
					return;
				}
				CMEventManager.unicastEvent(cme, strDistGroup, opt, cmInfo);
			}
			else if(strDistSession.equals("CM_ALL_SESSION")) // distribute to all session members
			{
				Iterator<CMSession> iterSession = interInfo.getSessionList().iterator();
				while(iterSession.hasNext())
				{
					tSession = iterSession.next();
					tMember = tSession.getSessionUsers();
					CMEventManager.castEvent(cme, tMember, opt, cmInfo);
				}
			}
			else
			{
				tSession = interInfo.findSession(strDistSession);
				if(tSession == null)
				{
					System.out.println("CMInteractionManager.distributeEvent(), session("
							+strDistSession+") not found.");
					return;
				}

				if(strDistGroup.equals("CM_ALL_GROUP"))	// distribute to all group members of a session
				{
					Iterator<CMGroup> iterGroup = tSession.getGroupList().iterator();
					while(iterGroup.hasNext())
					{
						tGroup = iterGroup.next();
						tMember = tGroup.getGroupUsers();
						CMEventManager.castEvent(cme, tMember, opt, cmInfo);
					}
				}
				else	// distribute to specific group members
				{
					tGroup = tSession.findGroup(strDistGroup);
					if(tGroup == null)
					{
						System.out.println("CMInteractionManager.distributeEvent(), group("
								+strDistGroup+") not found.");
						return;
					}
					tMember = tGroup.getGroupUsers();
					CMEventManager.castEvent(cme, tMember, opt, cmInfo);
				}
			}
		}
		
		return;
	}
	
}
