package kr.ac.konkuk.ccslab.cm.stub;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMEventInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;
import kr.ac.konkuk.ccslab.cm.thread.CMByteReceiver;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

/**
 * This class provides CM APIs of the common communication services that can be called by both the server and the client 
 * applications.
 * This class is the super class of the CMClientStub and the CMServerStub classes.
 * Application developers should generate an instance of the two sub-classes instead of the CMStub class.
 * 
 * @author mlim
 * @see {@link CMClientStub}, {@link CMServerStub}
 */
public class CMStub {
	protected CMInfo m_cmInfo;
	
	/**
	 * Creates an instance of the CMStub class.
	 * 
	 * <p> This method is called first when the sub-classes of the CMStub class call the default constructor. 
	 */
	public CMStub()
	{
		m_cmInfo = new CMInfo();
	}

	/**
	 * Terminates CM.
	 * 
	 * <p> This method is called first when the sub-classes of the CMStub class call the overridden methods.
	 * All the termination procedure is processed by this method. 
	 */
	public void terminateCM()
	{
		CMInteractionManager.terminate(m_cmInfo);
		
		// terminate threads
		CMEventInfo eventInfo = m_cmInfo.getEventInfo();
		CMEventReceiver er = eventInfo.getEventReceiver();
		if(er != null)
			er.interrupt();
		CMCommInfo commInfo = m_cmInfo.getCommInfo();
		CMByteReceiver br = commInfo.getByteReceiver();
		if(br != null)
			br.interrupt();
		
		// close all channels
		CMCommManager.terminate(m_cmInfo);
		
		// deregister all cancelled keys in the selector
		Selector sel = commInfo.getSelector();
		try {
			sel.select(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// set/get methods

	/**
	 * Returns a reference to the CMInfo object that the CMStub object has created.
	 * 
	 * <p> The CMInfo object includes all kinds of state information on the current CM.
	 * 
	 * @return a reference to the CMInfo object.
	 * @see CMInfo
	 */
	public CMInfo getCMInfo()
	{
		return m_cmInfo;
	}
	
	/**
	 * Registers the event handler of the application.
	 * 
	 * <p> An event handler has a role of receiving a CM event whenever it is received. A developer can define an event 
	 * handler class which includes application codes so that the application can do any task when a CM event is received.
	 * The event handler class must implement the {@link CMEventHandler} interface which defines an event processing 
	 * method, {@link CMEventHandler#processEvent(CMEvent)}. 
	 *
	 * @param handler - the event handler of the application.
	 */
	public void setEventHandler(CMEventHandler handler)
	{
		m_cmInfo.setEventHandler(handler);
	}
	
	/**
	 * Returns the registered event handler of the application.
	 * 
	 * @return the registered event handler of the application.
	 */
	public CMEventHandler getEventHandler()
	{
		return m_cmInfo.getEventHandler();
	}
	
	/**
	 * Returns the current client or server information.
	 * 
	 * <p> The {@link CMUser} class includes the user information such as the name, the host address, 
	 * the UDP port number, the current session and group names, and so on.
	 *  
	 * @return the current client or the current server information.
	 */
	public CMUser getMyself()
	{
		CMUser user = m_cmInfo.getInteractionInfo().getMyself();
		return user;
	}
	
	////////////////////////////////////////////////////////////////////////
	// add/remove channel (DatagramChannel or MulticastChannel)
	// SocketChannel is available only in the ClientStub module
	
	public boolean addDatagramChannel(int nChPort)
	{
		CMCommInfo commInfo = m_cmInfo.getCommInfo();
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMChannelInfo<Integer> nonBlockDCInfo = commInfo.getNonBlockDatagramChannelInfo();
		DatagramChannel dc = null;
		boolean result = false;
		
		if(nonBlockDCInfo.findChannel(nChPort) != null)
		{
			System.err.println("CMStub.addDatagramChannel(), channel key("+nChPort+") already exists.");
			return false;
		}
		try {
			dc = (DatagramChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_DATAGRAM_CHANNEL, 
					confInfo.getMyAddress(), nChPort, m_cmInfo);
			if(dc == null)
			{
				System.err.println("CMStub.addDatagramChannel(), failed.");
				return false;
			}
			
			result = nonBlockDCInfo.addChannel(nChPort, dc);
			if(result)
			{
				if(CMInfo._CM_DEBUG)
					System.out.println("CMStub.addDatagramChannel(), succeeded. port("+nChPort+")");
			}
			else
			{
				System.err.println("CMStub.addDatagramChannel(), failed! port("+nChPort+")");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean removeAdditionalDatagramChannel(int nChPort)
	{
		CMCommInfo commInfo = m_cmInfo.getCommInfo();
		CMChannelInfo<Integer> dcInfo = commInfo.getNonBlockDatagramChannelInfo();
		boolean result = false;

		result = dcInfo.removeChannel(nChPort); 
		if(result)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMStub.removeAdditionalDatagramChannel(), succeeded. port("+nChPort+")");
		}
		else
		{
			System.err.println("CMStub.removeAdditionalDatagramChannel(), failed! port("+nChPort+")");
		}
		return result;
	}

	public boolean addMulticastChannel(String strSession, String strGroup, String strChAddress, int nChPort)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		DatagramChannel mc = null;
		InetSocketAddress sockAddress = new InetSocketAddress(strChAddress, nChPort);
		boolean result = false;
		
		CMSession session = interInfo.findSession(strSession);
		if(session == null)
		{
			System.err.println("CMStub.addMulticastChannel(), session("+strSession+") not found.");
			return false;
		}
		CMGroup group = session.findGroup(strGroup);
		if(group == null)
		{
			System.err.println("CMStub.addMulticastChannel(), group("+strGroup+") not found.");
			return false;
		}
		CMChannelInfo<InetSocketAddress> mcInfo = group.getMulticastChannelInfo();
		if(mcInfo.findChannel(sockAddress) != null)
		{
			System.err.println("CMStub.addMulticastChannel(), channel index("+sockAddress.toString()+") already exists.");
			return false;
		}
		
		try {
			mc = (DatagramChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_MULTICAST_CHANNEL, strChAddress, 
					nChPort, m_cmInfo);
			if(mc == null)
			{
				System.err.println("CMStub.addMulticastChannel() failed.");
				return false;
			}
			
			result = mcInfo.addChannel(sockAddress, mc); 
			if(result)
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMStub.addMulticastChannel(), succeeded. session("+strSession+"), group("
							+strGroup+"), address("+strChAddress+"), port("+nChPort+")");					
				}
			}
			else
			{
				System.err.println("CMStub.addMulticastChannel(), failed! session("+strSession+"), group("
						+strGroup+"), address("+strChAddress+"), port("+nChPort+")");				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean removeAdditionalMulticastChannel(String strSession, String strGroup, String strChAddress, int nChPort)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		InetSocketAddress sockAddress = null;
		boolean result = false;
		
		CMSession session = interInfo.findSession(strSession);
		if(session == null)
		{
			System.err.println("CMStub.removeAdditionalMulticastChannel(), session("+strSession+") not found.");
			return false;
		}
		CMGroup group = session.findGroup(strGroup);
		if(group == null)
		{
			System.err.println("CMStub.removeAdditionalMulticastChannel(), group("+strGroup+") not found.");
			return false;
		}
		CMChannelInfo<InetSocketAddress> mcInfo = group.getMulticastChannelInfo();
		sockAddress = new InetSocketAddress(strChAddress, nChPort);
		
		result = mcInfo.removeChannel(sockAddress);
		if(result)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMStub.removeAdditionalMulticastChannel(), succeeded. session("+strSession+"), group("
						+strGroup+"), address("+strChAddress+"), port("+nChPort+")");				
			}
		}
		else
		{
			System.out.println("CMStub.removeAdditionalMulticastChannel(), failed! session("+strSession+"), group("
					+strGroup+"), address("+strChAddress+"), port("+nChPort+")");			
		}
		
		return result;
	}

	////////////////////////////////////////////////////////////////////////
	// event transmission methods (if required, the default server is used.)
	
	/**
	 * Sends a CM event to a single node.
	 * 
	 * <p> This method is the same as calling send(cme, strTarget, CMInfo.CM_STREAM, 0, false) 
	 * of the {@link CMStub#send(CMEvent, String, int, int, boolean)} method.
	 * 
	 * @param cme - the CM event
	 * @param strTarget - the receiver name. 
	 * <br> The receiver can be either the server or the client. In the CM network, 
	 * all the participating nodes (servers and clients) have a string name that can be the value of this parameter. 
	 * For example, the name of the default server is "SERVER".
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see {@link CMStub#send(CMEvent, String, int)}
	 * @see {@link CMStub#send(CMEvent, String, int, int)}
	 * @see {@link CMStub#send(CMEvent, String, int, int, boolean)}
	 * 
	 * @see {@link CMStub#send(CMEvent, String, String)}
	 * @see {@link CMStub#send(CMEvent, String, String, int)}
	 * @see {@link CMStub#send(CMEvent, String, String, int, int)}
	 */
	public boolean send(CMEvent cme, String strTarget)
	{
		return send(cme, strTarget, CMInfo.CM_STREAM, 0, false);
	}
	
	/**
	 * Sends a CM event to a single node.
	 * 
	 * <p> This method is the same as calling send(cme, strTarget, opt, 0, false)
	 * of the {@link CMStub#send(CMEvent, String, int, int, boolean)} method.
	 *  
	 * @param cme - the CM event
	 * @param strTarget - the receiver name. 
	 * <br> The receiver can be either the server or the client. In the CM network, 
	 * all the participating nodes (servers and clients) have a string name that can be the value of this parameter. 
	 * For example, the name of the default server is "SERVER".
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see {@link CMStub#send(CMEvent, String)}
	 * @see {@link CMStub#send(CMEvent, String, int, int)}
	 * @see {@link CMStub#send(CMEvent, String, int, int, boolean)}
	 * 
	 * @see {@link CMStub#send(CMEvent, String, String)}
	 * @see {@link CMStub#send(CMEvent, String, String, int)}
	 * @see {@link CMStub#send(CMEvent, String, String, int, int)}
	 */
	public boolean send(CMEvent cme, String strTarget, int opt)
	{
		return send(cme, strTarget, opt, 0, false);
	}
	
	/**
	 * Sends a CM event to a single node.
	 * 
	 * <p> This method is the same as calling send(cme, strTarget, opt, nChNum, false)
	 * of the {@link CMStub#send(CMEvent, String, int, int, boolean)} method.
	 *  
	 * @param cme - the CM event
	 * @param strTarget - the receiver name. 
	 * <br> The receiver can be either the server or the client. In the CM network, 
	 * all the participating nodes (servers and clients) have a string name that can be the value of this parameter. 
	 * For example, the name of the default server is "SERVER".
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @param nChNum - the channel key. 
	 * <br> If the application adds additional TCP or UDP channels, they are identified 
	 * by the channel key. The key of the TCP channel should be greater than 1 (0 for the default channel), 
	 * and the key of the UDP channel is the locally bound port number.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see {@link CMStub#send(CMEvent, String)}
	 * @see {@link CMStub#send(CMEvent, String, int)}
	 * @see {@link CMStub#send(CMEvent, String, int, int, boolean)}
	 * 
	 * @see {@link CMStub#send(CMEvent, String, String)}
	 * @see {@link CMStub#send(CMEvent, String, String, int)}
	 * @see {@link CMStub#send(CMEvent, String, String, int, int)} 
	 */
	public boolean send(CMEvent cme, String strTarget, int opt, int nChNum)
	{
		return send(cme, strTarget, opt, nChNum, false);
	}
	
	/**
	 * Sends a CM event to a single node.
	 * 
	 * <p> This method and the other shortened forms ({@link CMStub#send(CMEvent, String)}, 
	 * {@link CMStub#send(CMEvent, String, int)}, and {@link CMStub#send(CMEvent, String, int, int)}) use 
	 * the default server if the receiver is a client, because they assume that the receiver belongs to 
	 * the default server. (If the receiver is a server, these send methods can be called without a problem.) 
	 * If the client wants to send the event to a client that belongs to another server, the client should call 
	 * the {@link CMStub#send(CMEvent, String, String, int, int)} method. (CM applications can add multiple servers 
	 * in addition to the default server.) 
	 *  
	 * @param cme - the CM event
	 * @param strTarget - the receiver name. 
	 * <br> The receiver can be either the server or the client. In the CM network, 
	 * all the participating nodes (servers and clients) have a string name that can be the value of this parameter. 
	 * For example, the name of the default server is "SERVER". 
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @param nChNum - the channel key. 
	 * <br> If the application adds additional TCP or UDP channels, they are identified 
	 * by the channel key. The key of the TCP channel should be greater than 1 (0 for the default channel), 
	 * and the key of the UDP channel is the locally bound port number.
	 * @param isBlock - the blocking option. 
	 * <br> If isBlock is true, this method uses a blocking channel to send the event. 
	 * If isBlock is false, this method uses a nonblocking channel to send the event. The CM uses nonblocking channels 
	 * by default, but the application can also add blocking channels if required.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see {@link CMStub#send(CMEvent, String)}
	 * @see {@link CMStub#send(CMEvent, String, int)}
	 * @see {@link CMStub#send(CMEvent, String, int, int)}
	 * 
	 * @see {@link CMStub#send(CMEvent, String, String)}
	 * @see {@link CMStub#send(CMEvent, String, String, int)} 
	 * @see {@link CMStub#send(CMEvent, String, String, int, int)}
	 */
	public boolean send(CMEvent cme, String strTarget, int opt, int nChNum, boolean isBlock)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		boolean ret = false;
		
		// if a client in the c/s model, use internal forwarding by a server
		if(confInfo.getCommArch().equals("CM_CS") && confInfo.getSystemType().equals("CLIENT")
				&& !strTarget.equals("SERVER") && !interInfo.isAddServer(strTarget))
		{
			cme.setDistributionSession("CM_ONE_USER");
			cme.setDistributionGroup(strTarget);
			ret = CMEventManager.unicastEvent(cme, "SERVER", opt, nChNum, m_cmInfo);
			cme.setDistributionSession("");
			cme.setDistributionGroup("");
		}
		else
		{
			ret = CMEventManager.unicastEvent(cme, strTarget, opt, nChNum, isBlock, m_cmInfo);
		}

		return ret;
	}
	
	/**
	 * (from here)
	 * 
	 * @param cme
	 * @param sessionName
	 * @param groupName
	 * @return
	 */
	public boolean cast(CMEvent cme, String sessionName, String groupName)
	{
		return cast(cme, sessionName, groupName, CMInfo.CM_STREAM, 0);
	}
	
	public boolean cast(CMEvent cme, String sessionName, String groupName, int opt)
	{
		return cast(cme, sessionName, groupName, opt, 0);
	}
	
	public boolean cast(CMEvent cme, String sessionName, String groupName, int opt, int nChNum)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMSession session = null;
		CMGroup group = null;
		CMMember member = null;
		boolean ret = false;
		
		// if a client in the c/s model, use internal forwarding by a server
		//if(confInfo.getCommArch().equals("CM_CS") && confInfo.getSystemType().equals("CLIENT"))
		// if a client, use the internal forwarding by the server
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			// if the sessionName is null, broadcast
			if(sessionName == null)
			{
				cme.setDistributionSession("CM_ALL_SESSION");
				cme.setDistributionGroup("CM_ALL_GROUP");
			}
			// if the sessionName is not null, and the groupName is null, cast to all session members
			else if(groupName == null)
			{
				cme.setDistributionSession(sessionName);
				cme.setDistributionGroup("CM_ALL_GROUP");
			}
			// if both the sessionName and groupName is not null, cast to group members
			else
			{
				cme.setDistributionSession(sessionName);
				cme.setDistributionGroup(groupName);
			}
			
			ret = CMEventManager.unicastEvent(cme, "SERVER", opt, nChNum, m_cmInfo);
		}
		else	// if a server,
		{
			// if the sessionName is null, broadcast
			if(sessionName == null)
			{
				ret = CMEventManager.broadcastEvent(cme, opt, nChNum, m_cmInfo);
			}
			// if the sessionName is not null, and the groupName is null, cast to all session members
			else if(groupName == null)
			{
				session = interInfo.findSession(sessionName);
				if(session == null)
				{
					System.out.println("CMStub.cast(), session("+sessionName+") not found.");
					return false;
				}
				member = session.getSessionUsers();
				if(member == null)
				{
					System.out.println("CCMStub.cast(), session("+sessionName+") member is null.");
					return false;
				}
				ret = CMEventManager.castEvent(cme, member, opt, nChNum, m_cmInfo);
			}
			// if both the sessionName and groupName is not null, cast to group members
			else
			{
				session = interInfo.findSession(sessionName);
				if(session == null)
				{
					System.out.println("CCMStub.cast(), session("+sessionName+") not found.");
					return false;
				}
				group = session.findGroup(groupName);
				if( group == null )
				{
					System.out.println("CCMStub.cast(), grouop("+groupName+") not foudn.");
					return false;
				}
				member = group.getGroupUsers();
				if(member == null)
				{
					System.out.println("CCMStub.cast(), session("+sessionName+"), group("
										+groupName+") member is null");
					return false;
				}
				ret = CMEventManager.castEvent(cme, member, opt, nChNum, m_cmInfo);
			}
			
		}

		return ret;
	}
	
	public boolean multicast(CMEvent cme, String sessionName, String groupName)
	{
		boolean ret = false;
		ret = CMEventManager.multicastEvent(cme, sessionName, groupName, m_cmInfo);
		return ret;
	}
	
	public boolean broadcast(CMEvent cme)
	{
		return broadcast(cme, CMInfo.CM_STREAM, 0);
	}
	
	public boolean broadcast(CMEvent cme, int opt)
	{
		return broadcast(cme, opt, 0);
	}

	public boolean broadcast(CMEvent cme, int opt, int nChNum)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		boolean ret = false;
		// in the case of a client in the C/S model, use internal forwarding by all servers
		//if(confInfo.getCommArch().equals("CM_CS") && confInfo.getSystemType().equals("CLIENT"))
		// if a client, use internal forwarding by all servers
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			cme.setDistributionSession("CM_ALL_SESSION");
			cme.setDistributionGroup("CM_ALL_GROUP");
			ret = CMEventManager.unicastEvent(cme, "SERVER", opt, nChNum, m_cmInfo);
			
			// if there are additional servers connected
			Iterator<CMServer> iterAddServer = interInfo.getAddServerList().iterator();
			for(int i = 0; i < interInfo.getAddServerList().size(); i++)
			{
				CMServer tServer = iterAddServer.next();
				CMEventManager.unicastEvent(cme, tServer.getServerName(), opt, nChNum, m_cmInfo);
			}
		}
		else	// if a server
		{
			ret = CMEventManager.broadcastEvent(cme, opt, nChNum, m_cmInfo);
		}

		return ret;
	}

	////////////////////////////////////////////////////////////////////////
	// event transmission methods with a designated server

	public boolean send(CMEvent cme, String serverName, String userName)
	{
		return send(cme, serverName, userName, CMInfo.CM_STREAM, 0);
	}
	
	public 	boolean send(CMEvent cme, String serverName, String userName, int opt)
	{
		return send(cme, serverName, userName, opt, 0);
	}
	
	public 	boolean send(CMEvent cme, String serverName, String userName, int opt, int nChNum)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		boolean ret = false;

		// if a server name cannot be found, error
		if(!serverName.equals("SERVER") && !interInfo.isAddServer(serverName))
		{
			System.out.println("CMStub::send(), server("+serverName+") not found.");
			return false;
		}

		// if a client in the c/s model and a user is not null, use internal forwarding by a server
		//if(confInfo.getCommArch().equals("CM_CS") && confInfo.getSystemType().equals("CLIENT")
		//		&& userName != null)
		// if the system type is a client and a user is not null, use internal forwarding by a server
		if(confInfo.getSystemType().equals("CLIENT") && userName != null)
		{
			cme.setDistributionSession("CM_ONE_USER");
			cme.setDistributionGroup(userName);
			ret = CMEventManager.unicastEvent(cme, serverName, opt, nChNum, m_cmInfo);
			cme.setDistributionSession("");
			cme.setDistributionGroup("");
		}
		else
		{
			ret = CMEventManager.unicastEvent(cme, serverName, opt, nChNum, m_cmInfo);
		}
		
		return ret;
	}

	public 	boolean cast(CMEvent cme, String serverName, String sessionName, String groupName)
	{
		return cast(cme, serverName, sessionName, groupName, CMInfo.CM_STREAM, 0);
	}
	
	public 	boolean cast(CMEvent cme, String serverName, String sessionName, String groupName, int opt)
	{
		return cast(cme, serverName, sessionName, groupName, opt, 0);
	}
	
	public 	boolean cast(CMEvent cme, String serverName, String sessionName, String groupName, int opt, int nChNum)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		boolean ret = false;

		// check server validity
		if(!confInfo.getSystemType().equals("SERVER") && !interInfo.isAddServer(serverName))
		{
			System.out.println("CMStub::cast(), server("+serverName+") not found.");
			return false;
		}

		// if a client in the c/s model, use internal forwarding by a server
		//if(confInfo.getCommArch().equals("CM_CS") && confInfo.getSystemType().equals("CLIENT"))
		// if a client, use internal forwarding by a server
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			// if the sessionName is null, broadcast
			if(sessionName == null)
			{
				cme.setDistributionSession("CM_ALL_SESSION");
				cme.setDistributionGroup("CM_ALL_GROUP");
			}
			// if the sessionName is not null, and the groupName is null, cast to all session members
			else if(groupName == null)
			{
				cme.setDistributionSession(sessionName);
				cme.setDistributionGroup("CM_ALL_GROUP");
			}
			// if both the sessionName and groupName is not null, cast to group members
			else
			{
				cme.setDistributionSession(sessionName);
				cme.setDistributionGroup(groupName);
			}

			ret = CMEventManager.unicastEvent(cme, serverName, opt, nChNum, m_cmInfo);
		}

		return ret;
	}
	
	/////////////////////////////////////////////////////////////////////
	// file transfer 
	
	public void setFilePath(String filePath)
	{
		CMFileTransferManager.setFilePath(filePath, m_cmInfo);
		return;
	}
	
	public String getFilePath()
	{
		CMFileTransferInfo fInfo = m_cmInfo.getFileTransferInfo();
		return fInfo.getFilePath();
	}
	
	public void requestFile(String strFileName, String strFileOwner)
	{
		CMFileTransferManager.requestFile(strFileName, strFileOwner, m_cmInfo);		
		return;
	}
	
	public void pushFile(String strFilePath, String strReceiver)
	{
		CMFileTransferManager.pushFile(strFilePath, strReceiver, m_cmInfo);		
		return;
	}

	/////////////////////////////////////////////////////////////////////
	// network service
	
	// measure synchronously the end-to-end input throughput from the target node to this node
	public float measureInputThroughput(String strTarget)
	{
		boolean bReturn = false;
		float fSpeed = -1;
		long lFileSize = -1;	// the size of a file to measure the transmission delay
		long lStartTime = System.currentTimeMillis();
		long lEndTime = -1;
		long lTransDelay = -1;
		CMFileTransferInfo fInfo = m_cmInfo.getFileTransferInfo();
		CMEventInfo eInfo = m_cmInfo.getEventInfo();
		
		fInfo.setStartTime(lStartTime);
		bReturn = CMFileTransferManager.requestFile("throughput-test.jpg", strTarget, m_cmInfo);
		
		if(!bReturn)
			return -1;
		
		synchronized(eInfo.getEFTObject())
		{
			try {
				eInfo.getEFTObject().wait();
				lFileSize = eInfo.getEFTFileSize();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}
		}
				
		lEndTime = System.currentTimeMillis();
		lTransDelay = lEndTime - lStartTime;	// millisecond
		fSpeed = ((float)lFileSize / 1000000) / ((float)lTransDelay / 1000);	// MBps
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMStub.measureReadThroughput(); received file size("+lFileSize+"), delay("
					+lTransDelay+" ms), speed("+fSpeed+" MBps)");
		}

		return fSpeed;
	}
	
	// measure synchronously the end-to-end output throughput from this node to the target node
	public float measureOutputThroughput(String strTarget)
	{
		float fSpeed = -1f;
		// from here
		
		return fSpeed;
	}
	
}
