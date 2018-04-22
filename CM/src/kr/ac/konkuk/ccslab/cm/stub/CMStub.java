package kr.ac.konkuk.ccslab.cm.stub;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEventHandler;
import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
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
import kr.ac.konkuk.ccslab.cm.thread.CMByteSender;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

import java.io.File;
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
 * @see CMClientStub
 * @see CMServerStub
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
		CMByteReceiver byteReceiver = commInfo.getByteReceiver();
		if(byteReceiver != null)
			byteReceiver.interrupt();
		CMByteSender byteSender = commInfo.getByteSender();
		if(byteSender != null)
			byteSender.interrupt();
		
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
	
	/**
	 * Adds an additional datagram (UDP) channel to this CM node.
	 * 
	 * <p> A developer must note that the given port number is unique and different from 
	 * that of the default channel in the configuration file. A UDP channel is identified 
	 * by the port number as an index.
	 * 
	 * @param nChPort - the port number for the new datagram (UDP) channel
	 * @return true if the channel is successfully open, or false otherwise.
	 * @see CMStub#removeAdditionalDatagramChannel(int)
	 */
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
	
	/**
	 * Removes an additional datagram (UDP) channel from this CM node.
	 * 
	 * <p> Like the stream (TCP) channel case, a developer should be careful 
	 * not to remove the default channel.
	 * 
	 * @param nChPort - the port number of the channel to be removed
	 * @return true if the channel is successfully removed, or false otherwise.
	 * @see CMStub#addDatagramChannel(int)
	 */
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

	/**
	 * Adds an additional multicast channel to this CM node.
	 * 
	 * <p> The first and second parameters specify a session and group to which a new channel is assigned, 
	 * because a multicast channel is always mapped to a specific group in a session. The third and fourth 
	 * parameters are the multicast address and the port number of the new channel. A developer must note 
	 * that the given address and the port number are unique and different from that of the default channel 
	 * in a corresponding session configuration file. A multicast channel is identified by the four-tuple: 
	 * a session name, a group name, a multicast address, and a port number.
	 * 
	 * @param strSession - the session name which the new channel belongs to
	 * @param strGroup - the group name which the new channel belongs to
	 * @param strChAddress - the multicast address
	 * @param nChPort - the port number
	 * @return true if the channel is successfully open, or false otherwise.
	 * @see CMStub#removeAdditionalMulticastChannel(String, String, String, int)
	 */
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
	
	/**
	 * Removes an additional multicast channel from this CM node.
	 * 
	 * <p> Like the other additional channel cases, a developer should be careful not to remove the default channel.
	 * 
	 * @param strSession - the session name which the target channel belongs to
	 * @param strGroup - the group name which the target channel belongs to
	 * @param strChAddress - the multicast address
	 * @param nChPort - the port number
	 * @return true if the channel is successfully removed, or false otherwise.
	 * @see CMStub#addMulticastChannel(String, String, String, int)
	 */
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
	 * @see CMStub#send(CMEvent, String, int)
	 * @see CMStub#send(CMEvent, String, int, int)
	 * @see CMStub#send(CMEvent, String, int, int, boolean)
	 * 
	 * @see CMStub#send(CMEvent, String, String)
	 * @see CMStub#send(CMEvent, String, String, int)
	 * @see CMStub#send(CMEvent, String, String, int, int)
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
	 * @see CMStub#send(CMEvent, String)
	 * @see CMStub#send(CMEvent, String, int, int)
	 * @see CMStub#send(CMEvent, String, int, int, boolean)
	 * 
	 * @see CMStub#send(CMEvent, String, String)
	 * @see CMStub#send(CMEvent, String, String, int)
	 * @see CMStub#send(CMEvent, String, String, int, int)
	 */
	public boolean send(CMEvent cme, String strTarget, int opt)
	{
		boolean bReturn = false;
		
		if(opt == CMInfo.CM_STREAM)
			bReturn = send(cme, strTarget, opt, 0, false);
		else if(opt == CMInfo.CM_DATAGRAM)
			bReturn = send(cme, strTarget, opt, m_cmInfo.getConfigurationInfo().getUDPPort(), false);
		else
		{
			System.err.println("CMStub.send(), invalid option !");
			return false;
		}
		
		return bReturn;
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
	 * @see CMStub#send(CMEvent, String)
	 * @see CMStub#send(CMEvent, String, int)
	 * @see CMStub#send(CMEvent, String, int, int, boolean)
	 * 
	 * @see CMStub#send(CMEvent, String, String)
	 * @see CMStub#send(CMEvent, String, String, int)
	 * @see CMStub#send(CMEvent, String, String, int, int) 
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
	 * @see CMStub#send(CMEvent, String)
	 * @see CMStub#send(CMEvent, String, int)
	 * @see CMStub#send(CMEvent, String, int, int)
	 * 
	 * @see CMStub#send(CMEvent, String, String)
	 * @see CMStub#send(CMEvent, String, String, int) 
	 * @see CMStub#send(CMEvent, String, String, int, int)
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
	 * Sends a CM event to a node group
	 * 
	 * <p> This method is the same as calling send(cme, sessionName, groupName, CMInfo.CM_STREAM, 0)
	 * of the {@link CMStub#send(CMEvent, String, String, int, int)} method.
	 * 
	 * @param cme - the CM event
	 * @param sessionName - the target session name
	 * <br> If sessionName is null (not specified), the event is sent to all sessions. In this case, 
	 * the groupName parameter must be also null.
	 * @param groupName - the target group name
	 * <br> If groupName is null (not specified), the event is sent to all groups in a session.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, int, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String, String)
	 * @see CMStub#cast(CMEvent, String, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, String, int, int)
	 */
	public boolean cast(CMEvent cme, String sessionName, String groupName)
	{
		return cast(cme, sessionName, groupName, CMInfo.CM_STREAM, 0);
	}

	/**
	 * Sends a CM event to a node group
	 * 
	 * <p> This method is the same as calling send(cme, sessionName, groupName, opt, 0)
	 * of the {@link CMStub#send(CMEvent, String, String, int, int)} method.
	 * 
	 * @param cme - the CM event
	 * @param sessionName - the target session name
	 * <br> If sessionName is null (not specified), the event is sent to all sessions. In this case, 
	 * the groupName parameter must be also null.
	 * @param groupName - the target group name
	 * <br> If groupName is null (not specified), the event is sent to all groups in a session.
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String)
	 * @see CMStub#cast(CMEvent, String, String, int, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String, String)
	 * @see CMStub#cast(CMEvent, String, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, String, int, int)
	 */
	public boolean cast(CMEvent cme, String sessionName, String groupName, int opt)
	{
		boolean bReturn = false;
		
		if(opt == CMInfo.CM_STREAM)
			cast(cme, sessionName, groupName, opt, 0);
		else if(opt == CMInfo.CM_DATAGRAM)
			cast(cme, sessionName, groupName, opt, m_cmInfo.getConfigurationInfo().getUDPPort());
		else
		{
			System.err.println("CMStub.cast(), invalid option!");
			return false;
		}
		
		return bReturn;
	}

	/**
	 * Sends a CM event to a node group
	 * 
	 * <p> The cast method sends a CM event to a receivers group using multiple one-to-one transmission. 
	 * The range of receivers can be set with a session and a group. That is, this method sends an event 
	 * to a specific session members or group members.
	 * With the different combination of the sessionName and groupName parameters, the range of receivers is 
	 * different as described below.
	 * 
	 * <table border=1>
	 * <caption>Receiver ranges of the chat() method</caption>
	 * <tr bgcolor="lightgrey">
	 * <td>sessionName</td><td>groupName</td><td>receiver range</td>
	 * </tr>
	 * <tr>
	 * <td>not null</td><td>not null</td><td>all members of a group (groupName) in a session (sessionName)</td>
	 * </tr>
	 * <tr>
	 * <td>not null</td><td>null</td><td>all members of all groups in a session (sessionName)</td>
	 * </tr>
	 * <tr>
	 * <td>null</td><td>null</td><td>all members of all groups in all sessions (= all login users in the default server)</td>
	 * </tr>
	 * </table>
	 * 
	 * @param cme - the CM event
	 * @param sessionName - the target session name
	 * <br> If sessionName is null (not specified), the event is sent to all sessions. In this case, 
	 * the groupName parameter must be also null.
	 * @param groupName - the target group name
	 * <br> If groupName is null (not specified), the event is sent to all groups in a session.
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @param nChNum - the channel key. 
	 * <br> If the application adds additional TCP or UDP channels, they are identified 
	 * by the channel key. The key of the TCP channel should be greater than 1 (0 for the default channel), 
	 * and the key of the UDP channel is the locally bound port number.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String)
	 * @see CMStub#cast(CMEvent, String, String, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String, String)
	 * @see CMStub#cast(CMEvent, String, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, String, int, int)
	 * 
	 * @see CMStub#multicast(CMEvent, String, String)
	 * @see CMStub#broadcast(CMEvent, int, int)
	 */
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
	
	/**
	 * Sends a CM event to a node group via multicast
	 * 
	 * <p> If a CM server application sets the communication architecture as the CM_PS in the server configuration file, 
	 * it creates a multicast channel assigned to each group. In this case, the server and client applications can send 
	 * an event using a multicast channel.
	 * <br> Unlike the cast method, both the session and group name values must not be null, 
	 * because the multicast is enabled only for a specific group.
	 * 
	 * @param cme - the CM event
	 * @param sessionName - the target session name
	 * @param groupName - the target group name
	 * @return true if the event is successfully multicasted; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String, int, int)
	 * @see CMStub#broadcast(CMEvent, int, int)
	 */
	public boolean multicast(CMEvent cme, String sessionName, String groupName)
	{
		boolean ret = false;
		ret = CMEventManager.multicastEvent(cme, sessionName, groupName, m_cmInfo);
		return ret;
	}
	
	/**
	 * Sends a CM event to all connected nodes
	 * 
	 * <p> This method is the same as calling broadcast(cme, CMInfo.CM_STREAM, 0)
	 * of the {@link CMStub#broadcast(CMEvent, int, int)} method.
	 * 
	 * @param cme - the CM event
	 * @return true if the event is successfully broadcasted; false otherwise.
	 * 
	 * @see CMStub#broadcast(CMEvent, int)
	 * @see CMStub#broadcast(CMEvent, int, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String, int, int)
	 * @see CMStub#multicast(CMEvent, String, String)
	 */
	public boolean broadcast(CMEvent cme)
	{
		return broadcast(cme, CMInfo.CM_STREAM, 0);
	}
	
	/**
	 * Sends a CM event to all connected nodes
	 * 
	 * <p> This method is the same as calling broadcast(cme, opt, 0)
	 * of the {@link CMStub#broadcast(CMEvent, int, int)} method.
	 * 
	 * @param cme the CM event
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @return true if the event is successfully broadcasted; false otherwise.
	 * 
	 * @see CMStub#broadcast(CMEvent)
	 * @see CMStub#broadcast(CMEvent, int, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String, int, int)
	 * @see CMStub#multicast(CMEvent, String, String) 
	 */
	public boolean broadcast(CMEvent cme, int opt)
	{
		boolean bReturn = false;
		
		if(opt == CMInfo.CM_STREAM)
			broadcast(cme, opt, 0);
		else if(opt == CMInfo.CM_DATAGRAM)
			broadcast(cme, opt, m_cmInfo.getConfigurationInfo().getUDPPort());
		else
		{
			System.err.println("CMStub.broadcast(), invalid option!");
			return false;
		}
		
		return bReturn;
	}

	/**
	 * Sends a CM event to all connected nodes
	 * 
	 * <p> This method is the same as calling broadcast(cme, opt, 0)
	 * of the {@link CMStub#broadcast(CMEvent, int, int)} method.
	 * <br> The broadcast method sends a CM event to all users who currently log in to the default server via multiple 
	 * one-to-one transmissions. 
	 * 
	 * @param cme the CM event
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @param nChNum - the channel key. 
	 * <br> If the application adds additional TCP or UDP channels, they are identified 
	 * by the channel key. The key of the TCP channel should be greater than 1 (0 for the default channel), 
	 * and the key of the UDP channel is the locally bound port number.
	 * @return true if the event is successfully broadcasted; false otherwise.
	 * 
	 * @see CMStub#broadcast(CMEvent)
	 * @see CMStub#broadcast(CMEvent, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String, int, int)
	 * @see CMStub#multicast(CMEvent, String, String) 
	 */
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

	/**
	 * Sends a CM event to a single node via a designated server.
	 * 
	 * <p> This method is the same as calling send(cme, serverName, userName, CMInfo.CM_STREAM, 0)
	 * of the {@link CMStub#send(CMEvent, String, String, int, int)} method.
	 * 
	 * @param cme - the CM event
	 * @param serverName - the server name
	 * @param userName - the target user name
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#send(CMEvent, String, String, int) 
	 * @see CMStub#send(CMEvent, String, String, int, int)
	 * 
	 * @see CMStub#send(CMEvent, String)
	 * @see CMStub#send(CMEvent, String, int)
	 * @see CMStub#send(CMEvent, String, int, int)
	 * @see CMStub#send(CMEvent, String, int, int, boolean)  
	 */
	public boolean send(CMEvent cme, String serverName, String userName)
	{
		return send(cme, serverName, userName, CMInfo.CM_STREAM, 0);
	}

	/**
	 * Sends a CM event to a single node via a designated server.
	 * 
	 * <p> This method is the same as calling send(cme, serverName, userName, opt, 0)
	 * of the {@link CMStub#send(CMEvent, String, String, int, int)} method.
	 * 
	 * @param cme - the CM event
	 * @param serverName - the server name
	 * @param userName - the target user name
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#send(CMEvent, String, String) 
	 * @see CMStub#send(CMEvent, String, String, int, int)
	 * 
	 * @see CMStub#send(CMEvent, String)
	 * @see CMStub#send(CMEvent, String, int)
	 * @see CMStub#send(CMEvent, String, int, int)
	 * @see CMStub#send(CMEvent, String, int, int, boolean)  
	 */
	public 	boolean send(CMEvent cme, String serverName, String userName, int opt)
	{
		boolean bReturn = false;
		
		if(opt == CMInfo.CM_STREAM)
			send(cme, serverName, userName, opt, 0);
		else if(opt == CMInfo.CM_DATAGRAM)
			send(cme, serverName, userName, opt, m_cmInfo.getConfigurationInfo().getUDPPort());
		else
		{
			System.err.println("CMStub.send(), invalid option!");
			return false;
		}
		
		return bReturn;
	}
	
	/**
	 * Sends a CM event to a single node via a designated server.
	 * 
	 * <p> This method is usually used to send an event to a participating user node through the designated server.
	 * This method is the same as calling send(cme, userName, opt, 0, false)
	 * of the {@link CMStub#send(CMEvent, String, int, int, boolean)} method except that this method uses the designated server.
	 * <br> This method is used especially if the application uses multiple servers and the client wants to send an event to 
	 * another client. The target client (user) is connected to a server which is not the default server.
	 * 
	 * @param cme - the CM event
	 * @param serverName - the server name
	 * @param userName - the target user name
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @param nChNum - the channel key. 
	 * <br> If the application adds additional TCP or UDP channels, they are identified 
	 * by the channel key. The key of the TCP channel should be greater than 1 (0 for the default channel), 
	 * and the key of the UDP channel is the locally bound port number.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#send(CMEvent, String, String) 
	 * @see CMStub#send(CMEvent, String, String, int)
	 * 
	 * @see CMStub#send(CMEvent, String)
	 * @see CMStub#send(CMEvent, String, int)
	 * @see CMStub#send(CMEvent, String, int, int)
	 * @see CMStub#send(CMEvent, String, int, int, boolean)  
	 */
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

	/**
	 * Sends a CM event to a node group via a designated server
	 * 
	 * <p> This method is the same as calling send(cme, serverName, sessionName, groupName, CMInfo.CM_STREAM, 0)
	 * of the {@link CMStub#send(CMEvent, String, String, int, int)} method.
	 *  
	 * @param cme - the CM event
	 * @param serverName - the server name
	 * @param sessionName - the target session name
	 * <br> If sessionName is null (not specified), the event is sent to all sessions. In this case, 
	 * the groupName parameter must be also null.
	 * @param groupName - the target group name
	 * <br> If groupName is null (not specified), the event is sent to all groups in a session.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, String, int, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String)
	 * @see CMStub#cast(CMEvent, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, int, int) 
	 */
	public 	boolean cast(CMEvent cme, String serverName, String sessionName, String groupName)
	{
		return cast(cme, serverName, sessionName, groupName, CMInfo.CM_STREAM, 0);
	}
	
	/**
	 * Sends a CM event to a node group via a designated server
	 * 
	 * <p> This method is the same as calling send(cme, serverName, sessionName, groupName, opt, 0)
	 * of the {@link CMStub#send(CMEvent, String, String, int, int)} method.
	 *   
	 * @param cme - the CM event
	 * @param serverName - the server name
	 * @param sessionName - the target session name
	 * <br> If sessionName is null (not specified), the event is sent to all sessions. In this case, 
	 * the groupName parameter must be also null.
	 * @param groupName - the target group name
	 * <br> If groupName is null (not specified), the event is sent to all groups in a session.
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String, String)
	 * @see CMStub#cast(CMEvent, String, String, String, int, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String)
	 * @see CMStub#cast(CMEvent, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, int, int)  
	 */
	public 	boolean cast(CMEvent cme, String serverName, String sessionName, String groupName, int opt)
	{
		boolean bReturn = false;
		
		if(opt == CMInfo.CM_STREAM)
			cast(cme, serverName, sessionName, groupName, opt, 0);
		else if(opt == CMInfo.CM_DATAGRAM)
			cast(cme, serverName, sessionName, groupName, opt, m_cmInfo.getConfigurationInfo().getUDPPort());
		else
		{
			System.err.println("CMStub.cast(), invalid option!");
			return false;
		}
		
		return bReturn;
	}
	
	/**
	 * Sends a CM event to a node group via a designated server
	 * 
	 * <p> This method is usually used to send an event to a participating user node group through the designated server.
	 * This method is the same as calling cast(cme, sessionName, groupName, opt, nChNum)
	 * of the {@link CMStub#cast(CMEvent, String, String, int, int)} method except that this method uses the designated server.
	 * <br> This method is used especially if the application uses multiple servers and the client wants to send an event to 
	 * a client group. The target client (user) group is connected to a server which is not the default server. 
	 * 
	 * @param cme - the CM event
	 * @param serverName - the server name
	 * @param sessionName - the target session name
	 * <br> If sessionName is null (not specified), the event is sent to all sessions. In this case, 
	 * the groupName parameter must be also null.
	 * @param groupName - the target group name
	 * <br> If groupName is null (not specified), the event is sent to all groups in a session.
	 * @param opt - the reliability option. 
	 * <br> If opt is CMInfo.CM_STREAM (or 0), CM uses TCP socket channel to send 
	 * the event. If opt is CMInfo.CM_DATAGRAM (or 1), CM uses UDP datagram socket channel.
	 * @param nChNum - the channel key. 
	 * <br> If the application adds additional TCP or UDP channels, they are identified 
	 * by the channel key. The key of the TCP channel should be greater than 1 (0 for the default channel), 
	 * and the key of the UDP channel is the locally bound port number.
	 * @return true if the event is successfully sent; false otherwise.
	 * 
	 * @see CMStub#cast(CMEvent, String, String, String)
	 * @see CMStub#cast(CMEvent, String, String, String, int)
	 * 
	 * @see CMStub#cast(CMEvent, String, String)
	 * @see CMStub#cast(CMEvent, String, String, int)
	 * @see CMStub#cast(CMEvent, String, String, int, int)   
	 */
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

	/**
	 * Sets the default file path for file transfer.
	 * 
	 * <p> CM applications that directly connect to each other can exchange a file with the CMStub class 
	 * that is the parent class of the CMClientStub and the CMServerStub classes. In the client-server architecture, 
	 * a client can push or pull a file to/from a server and vice versa. When CM is initialized by an application, 
	 * the default directory is configured by the path information that is set in the configuration file 
	 * (the FILE_PATH field). If the default directory does not exist, CM creates it. If the FILE_PATH field is not set, 
	 * the default path is set to the current working directory (".").
	 * <p> If the file transfer is requested, a sender (the server or the client) searches for the file 
	 * in the default file path. If a client receives a file, CM stores the file in this file path. 
	 * If a server receives a file, CM stores the file in a sub-directory of the default path. 
	 * The sub-directory name is a sender (client) name.
	 * 
	 * @param filePath - the file path
	 * @see CMStub#getFilePath()
	 */
	public void setFilePath(String filePath)
	{
		CMFileTransferManager.setFilePath(filePath, m_cmInfo);
		return;
	}
	
	/**
	 * Gets the default file path for file transfer.
	 * 
	 * @return the default file path for file transfer
	 * @see CMStub#setFilePath(String)
	 */
	public String getFilePath()
	{
		CMFileTransferInfo fInfo = m_cmInfo.getFileTransferInfo();
		return fInfo.getFilePath();
	}
	
	/**
	 * Requests to transfer a file from a owner (push mode).
	 * 
	 * <p> This method is the same as calling requestFile(strFileName, strFileOwner, CMInfo.FILE_DEFAULT) 
	 * of the {@link CMStub#requestFile(String, String, byte)}.
	 * 
	 * @param strFileName - the requested file name
	 * @param strFileOwner - the file owner name
	 * @return true if the file transfer is successfully requested, or false otherwise.
	 * @see CMStub#requestFile(String, String, byte)
	 * @see CMStub#pushFile(String, String)
	 */
	public boolean requestFile(String strFileName, String strFileOwner)
	{
		boolean bReturn = false;
		bReturn = CMFileTransferManager.requestFile(strFileName, strFileOwner, m_cmInfo);		
		return bReturn;
	}
	
	/**
	 * Requests to transfer a file from a owner (push mode).
	 * 
	 * <p> If a client requests a file, the file owner can be a server. If a server requests a file, 
	 * the file owner can be a client.
	 * <p> If a client requests a file to the default server and the server has the requested file 
	 * in its file path, the client successfully receives the file and locates it in its file path. 
	 * If the requested file does not exist in the requested server, the server sends a pre-defined 
	 * file event as the reply to the request and finishes the file transfer protocol. 
	 * <br> By catching the reply file event, REPLY_FILE_TRANSFER, the client can figure out 
	 * whether the requested file exists or not in the server. The following codes are the part of 
	 * the client event handler which handles the REPLY_FILE_TRANSFER event. If the return code of the event is 1, 
	 * the requested file exists. If the return code is 0, the requested file does not exist in the requested server.
	 * The detailed event fields of the REPLY_FILE_TRANSFER event are described below.
	 * 
	 * <table border=1>
	 * <caption>CMFileEvent.REPLY_FILE_TRANSFER event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_FILE_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMFileEvent.REPLY_FILE_TRANSFER </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Field data type </td> <td> Field definition </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> file name </td> <td> String </td> <td> file name </td> <td> {@link CMFileEvent#getFileName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> return code </td> <td> int </td> 
	 *     <td> 1: ok <br> 0: the requested file does not exist
	 *     </td>
	 *     <td> {@link CMFileEvent#getReturnCode()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> attaching SNS content ID </td> <td> int </td> 
	 *     <td> If this file is an attachment of an SNS content, the content ID (&gt;= 0) is set. The default value is 
	 *     -1 (if this file is not an attachment). 
	 *     <td> {@link CMFileEvent#getContentID()} </td>
	 *   </tr>
	 * </table>
	 * 
	 * <p> When the requested file is completely transferred, the sender CM sends the END_FILE_TRANSFER event 
	 * to the requester. The requester can catch this event in the event handler if it needs to be notified 
	 * when the entire file is transferred. The detailed information of the END_FILE_TRANSFER event is described below.
	 *
	 * <table border=1>
	 * <caption>CMFileEvent.END_FILE_TRANSFER event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_FILE_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMFileEvent.END_FILE_TRANSFER </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Field data type </td> <td> Field definition </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> receiver name </td> <td> String </td> <td> file receiver name </td> 
	 *     <td> {@link CMFileEvent#getReceiverName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> file name </td> <td> String </td> <td> file name </td> <td> {@link CMFileEvent#getFileName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> return code </td> <td> int </td> 
	 *     <td> 1: transfer succeeded <br> 0: transfer failed
	 *     </td>
	 *     <td> {@link CMFileEvent#getReturnCode()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> attaching SNS content ID </td> <td> int </td> 
	 *     <td> If this file is an attachment of an SNS content, the content ID (&gt;= 0) is set. The default value is 
	 *     -1 (if this file is not an attachment). 
	 *     <td> {@link CMFileEvent#getContentID()} </td>
	 *   </tr>
	 * </table>
	 * 
	 * @param strFileName - the requested file name
	 * @param strFileOwner - the file owner name
	 * @param byteFileAppend - the file reception mode
	 * <br> The file reception mode specifies the behavior of the receiver if it already has the entire 
	 * or part of the requested file. CM provides three file reception modes which are default, overwrite, 
	 * and append mode. The byteFileAppend parameter can be one of these three modes which are CMInfo.FILE_DEFAULT(-1), 
	 * CMInfo.FILE_OVERWRITE(0), and CMInfo.FILE_APPEND(1). If the byteFileAppend parameter is CMInfo.FILE_DEFAULT, 
	 * the file reception mode is determined by the FILE_APPEND_SCHEME field of the CM configuration file. 
	 * If the byteFileAppend parameter is set to one of the other two values, the reception mode of this requested file 
	 * does not follow the CM configuration file, but this parameter value. The CMInfo.FILE_OVERWRITE is the overwrite mode 
	 * where the receiver always receives the entire file even if it already has the same file. 
	 * The CMInfo.FILE_APPEND is the append mode where the receiver skips existing file blocks 
	 * and receives only remaining blocks.
	 * @return true if the file transfer is successfully requested, or false otherwise.
	 * @see CMStub#requestFile(String, String)
	 * @see CMStub#pushFile(String, String)
	 */
	public boolean requestFile(String strFileName, String strFileOwner, byte byteFileAppend)
	{
		boolean bReturn = false;
		bReturn = CMFileTransferManager.requestFile(strFileName, strFileOwner, byteFileAppend, m_cmInfo);
		return bReturn;
	}
	
	/**
	 * Sends a file to a receiver (push mode).
	 * 
	 * <p> Unlike the pull mode, in the push mode, a CM application can send a file to another remote CM application.
	 * 
	 * <p> When the file is entirely transferred to the receiver, the receiver CM sends the END_FILE_TRANSFER_ACK event 
	 * to the sender. The sender can catch this event in the event handler if it needs to be notified when the entire file 
	 * is transferred. The detailed information of the END_FILE_TRANSFER_ACK event is the same as that of 
	 * the END_FILE_TRANSFER event as follows.
	 * 
	 * <table border=1>
	 * <caption>CMFileEvent.END_FILE_TRANSFER_ACK event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_FILE_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> <td> CMFileEvent.END_FILE_TRANSFER_ACK </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Field data type </td> <td> Field definition </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> receiver name </td> <td> String </td> <td> file receiver name </td> 
	 *     <td> {@link CMFileEvent#getReceiverName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> file name </td> <td> String </td> <td> file name </td> <td> {@link CMFileEvent#getFileName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> return code </td> <td> int </td> 
	 *     <td> 1: the entire file successfully received <br> 0: reception error at the receiver
	 *     </td>
	 *     <td> {@link CMFileEvent#getReturnCode()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> attaching SNS content ID </td> <td> int </td> 
	 *     <td> If this file is an attachment of an SNS content, the content ID (&gt;= 0) is set. The default value is 
	 *     -1 (if this file is not an attachment). 
	 *     <td> {@link CMFileEvent#getContentID()} </td>
	 *   </tr>
	 * </table>
	 *  
	 * @param strFilePath - the path name of a file to be sent
	 * @param strReceiver - the receiver name
	 * @return true if the file push is successfully notified to the receiver, or false otherwise.
	 * @see CMStub#requestFile(String, String)
	 */
	public boolean pushFile(String strFilePath, String strReceiver)
	{
		boolean bReturn = false;
		bReturn = CMFileTransferManager.pushFile(strFilePath, strReceiver, m_cmInfo);		
		return bReturn;
	}
	
	/**
	 * Cancels sending (or pushing) a file.
	 * 
	 * <p> A sender can cancel all of its sending tasks to the receiver by calling this method. 
	 * The file pushing task can be cancelled regardless of the file transfer scheme that is determined 
	 * by the FILE_TRANSFER_SCHEME field of the server CM configuration file. The cancellation is also 
	 * notified to the receiver. The result of the receiver's cancellation is sent to the sender 
	 * as the CANCEL_FILE_SEND_ACK or the CANCEL_FILE_SEND_CHAN_ACK event. The former event is sent 
	 * if the file transfer service uses the default channel (,that is, if the FILE_TRANSFER_SCHEME field 
	 * is 0 in the server CM configuration file), and the latter event is sent if the file transfer service 
	 * uses the separate channel (, that is, if the FILE_TRANSFER_SCHEME field is 1 in the server CM 
	 * configuration file). The detailed information of these events is described below.
	 * 
	 * <table border=1>
	 * <caption>CMFileEvent.CANCEL_FILE_SEND_CHAN_ACK event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_FILE_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> 
	 *     <td> CMFileEvent.CANCEL_FILE_SEND_ACK <br> CMFileEvent.CANCEL_FILE_SEND_CHAN_ACK </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Field data type </td> <td> Field definition </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> sender name </td> <td> String </td> <td> file sender name </td>
	 *     <td> {@link CMFileEvent#getSenderName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> receiver name </td> <td> String </td> <td> file receiver name </td> 
	 *     <td> {@link CMFileEvent#getReceiverName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> return code </td> <td> int </td> 
	 *     <td> 1: successfully cancelled at the receiver <br> 0: cancellation error at the receiver
	 *     </td>
	 *     <td> {@link CMFileEvent#getReturnCode()} </td>
	 *   </tr>
	 * </table> 
	 * 
	 * @param strReceiver - the receiver name
	 * @return true if the cancellation is succeeded, or false otherwise.
	 * @see CMStub#cancelRequestFile(String)
	 */
	public boolean cancelPushFile(String strReceiver)
	{
		boolean bReturn = false;
		bReturn = CMFileTransferManager.cancelPushFile(strReceiver, m_cmInfo);
		return bReturn;
	}
	
	/**
	 * Cancels receiving (or pulling) a file.
	 * 
	 * <p> A receiver can cancel all of its receiving tasks from the sender by calling this method. 
	 * Unlike the cancellation of the file pushing task, the file pulling task can be cancelled 
	 * only if the file transfer scheme is on using the separate channel (, that is, 
	 * if the FILE_TRANSFER_SCHEME field is 1 in the server CM configuration file). 
	 * The cancellation is also notified to the sender. The result of the sender's cancellation 
	 * is sent to the receiver as the CANCEL_FILE_RECV_CHAN_ACK event. The detailed information of 
	 * this events is described below.
	 * 
	 * <table border=1>
	 * <caption>CMFileEvent.CANCEL_FILE_RECV_CHAN_ACK event</caption>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event type </td> <td> CMInfo.CM_FILE_EVENT </td>
	 *   </tr>
	 *   <tr>
	 *     <td bgcolor="lightgrey"> Event ID </td> 
	 *     <td> CMFileEvent.CANCEL_FILE_RECV_CHAN_ACK </td>
	 *   </tr>
	 *   <tr bgcolor="lightgrey">
	 *     <td> Event field </td> <td> Field data type </td> <td> Field definition </td> <td> Get method </td>
	 *   </tr>
	 *   <tr>
	 *     <td> sender name </td> <td> String </td> <td> file sender name </td>
	 *     <td> {@link CMFileEvent#getSenderName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> receiver name </td> <td> String </td> <td> file receiver name </td> 
	 *     <td> {@link CMFileEvent#getReceiverName()} </td>
	 *   </tr>
	 *   <tr>
	 *     <td> return code </td> <td> int </td> 
	 *     <td> 1: successfully cancelled at the sender <br> 0: cancellation error at the sender
	 *     </td>
	 *     <td> {@link CMFileEvent#getReturnCode()} </td>
	 *   </tr>
	 * </table>  
	 * 
	 * @param strSender - the sender name
	 * @return true if the cancellation is succeeded, or false otherwise.
	 * @see CMStub#cancelPushFile(String) 
	 */
	public boolean cancelRequestFile(String strSender)
	{
		boolean bReturn = false;
		bReturn = CMFileTransferManager.cancelRequestFile(strSender, m_cmInfo);
		return bReturn;
	}

	/////////////////////////////////////////////////////////////////////
	// network service
	
	// measure synchronously the end-to-end input throughput from the target node to this node
	/**
	 * measures the incoming network throughput from a CM node.
	 * 
	 * <p> A CM application can measure the incoming/outgoing network throughput from/to another CM application 
	 * that is directly connected to the application. For example, a client can measure the network throughput 
	 * from/to the server. 
	 * The incoming network throughput of a CM node A from B measures how many bytes A can receive from B in a second. 
	 * The outgoing network through of a CM node A to B measures how many bytes A can send to B.
	 * 
	 * @param strTarget - the target CM node
	 * <br> The target CM node should be directly connected to the calling node.
	 * @return the network throughput value by the unit of Megabytes per second (MBps) 
	 * if successfully measured, or -1 otherwise.
	 * @see CMStub#measureOutputThroughput(String)
	 */
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
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMFileEvent replyEvent = null;
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		
		fInfo.setStartTime(lStartTime);
		bReturn = CMFileTransferManager.requestFile("throughput-test.jpg", strTarget, CMInfo.FILE_OVERWRITE, m_cmInfo);
		
		if(!bReturn)
			return -1;

		if(confInfo.isFileTransferScheme())
			eventSync.setWaitingEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER_CHAN);
		else
			eventSync.setWaitingEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER);
		
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
				replyEvent = (CMFileEvent) eventSync.getReplyEvent();
			}
			lFileSize = replyEvent.getFileSize();
		}
		eventSync.init();
		
		if(replyEvent.getID() == CMFileEvent.REPLY_FILE_TRANSFER || replyEvent.getID() == CMFileEvent.REPLY_FILE_TRANSFER_CHAN)
		{
			return -1;
		}
				
		lEndTime = System.currentTimeMillis();
		lTransDelay = lEndTime - lStartTime;	// millisecond
		fSpeed = ((float)lFileSize / 1000000) / ((float)lTransDelay / 1000);	// MBps
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMStub.measureInputThroughput(); received file size("+lFileSize+"), delay("
					+lTransDelay+" ms), speed("+fSpeed+" MBps)");
		}

		return fSpeed;
	}
	
	/**
	 * measures the incoming network throughput from a CM node.
	 * 
	 * @param strTarget - the target CM node
	 * @return the network throughput value by the unit of Megabytes per second (MBps) 
	 * if successfully measured, or -1 otherwise.
	 * @see CMStub#measureInputThroughput(String)
	 */
	// measure synchronously the end-to-end output throughput from this node to the target node
	public float measureOutputThroughput(String strTarget)
	{
		boolean bReturn = false;
		float fSpeed = -1;
		long lFileSize = -1;	// the size of a file to measure the transmission delay
		long lStartTime = System.currentTimeMillis();
		long lEndTime = -1;
		long lTransDelay = -1;
		CMFileTransferInfo fInfo = m_cmInfo.getFileTransferInfo();
		CMEventInfo eInfo = m_cmInfo.getEventInfo();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMFileEvent replyEvent = null;
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		String strFilePath = fInfo.getFilePath() + File.separator + "throughput-test.jpg";
		
		fInfo.setStartTime(lStartTime);
		bReturn = CMFileTransferManager.pushFile(strFilePath, strTarget, CMInfo.FILE_OVERWRITE, m_cmInfo);
		
		if(!bReturn)
			return -1;
		
		if(confInfo.isFileTransferScheme())
			eventSync.setWaitingEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER_CHAN_ACK);
		else
			eventSync.setWaitingEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER_ACK);
		
		synchronized(eventSync)
		{
			while(replyEvent == null)
			{
				try {
					eventSync.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				replyEvent = (CMFileEvent) eventSync.getReplyEvent();
			}
			lFileSize = replyEvent.getFileSize();
		}
		eventSync.init();
				
		lEndTime = System.currentTimeMillis();
		lTransDelay = lEndTime - lStartTime;	// millisecond
		fSpeed = ((float)lFileSize / 1000000) / ((float)lTransDelay / 1000);	// MBps
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMStub.measureOutputThroughput(); received file size("+lFileSize+"), delay("
					+lTransDelay+" ms), speed("+fSpeed+" MBps)");
		}

		return fSpeed;
	}
	
}
