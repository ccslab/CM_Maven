package kr.ac.konkuk.ccslab.cm;
import java.util.*;
import java.io.IOException;
import java.nio.channels.*;

public class CMStub {
	protected CMInfo m_cmInfo;
	
	public CMStub()
	{
		m_cmInfo = new CMInfo();
	}

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

	public CMInfo getCMInfo()
	{
		return m_cmInfo;
	}
	
	public void setEventHandler(CMEventHandler handler)
	{
		m_cmInfo.setEventHandler(handler);
	}
	
	public CMEventHandler getEventHandler()
	{
		return m_cmInfo.getEventHandler();
	}
	
	////////////////////////////////////////////////////////////////////////
	// add/remove channel (DatagramChannel or MulticastChannel)
	// SocketChannel is available only in the ClientStub module
	
	public void addDatagramChannel(int nChIndex, int nChPort)
	{
		CMCommInfo commInfo = m_cmInfo.getCommInfo();
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMChannelInfo dcInfo = commInfo.getDatagramChannelInfo();
		DatagramChannel dc = null;
		if(dcInfo.findChannel(nChIndex) != null)
		{
			System.out.println("CMStub.addDatagramChannel(), channel index("+nChIndex+") already exists.");
			return;
		}
		try {
			dc = (DatagramChannel) CMCommManager.openChannel(CMInfo.CM_DATAGRAM_CHANNEL, confInfo.getMyAddress()
					, nChPort, m_cmInfo);
			if(dc == null)
			{
				System.out.println("CMStub.addDatagramChannel(), failed.");
				return;
			}
			dcInfo.addChannel(dc, nChIndex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public void removeAdditionalDatagramChannel(int nChIndex)
	{
		CMCommInfo commInfo = m_cmInfo.getCommInfo();
		CMChannelInfo dcInfo = commInfo.getDatagramChannelInfo();

		dcInfo.removeChannel(nChIndex);
		return;
	}

	public void addMulticastChannel(int nChIndex, String strSession, String strGroup, String strChAddress, int nChPort)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		DatagramChannel mc = null;
		
		CMSession session = interInfo.findSession(strSession);
		if(session == null)
		{
			System.out.println("CMStub.addMulticastChannel(), session("+strSession+") not found.");
			return;
		}
		CMGroup group = session.findGroup(strGroup);
		if(group == null)
		{
			System.out.println("CMStub.addMulticastChannel(), group("+strGroup+") not found.");
			return;
		}
		CMChannelInfo mcInfo = group.getMulticastChannelInfo();
		if(mcInfo.findChannel(nChIndex) != null)
		{
			System.out.println("CMStub.addMulticastChannel(), channel index("+nChIndex+") already exists.");
			return;
		}
		
		try {
			mc = (DatagramChannel) CMCommManager.openChannel(CMInfo.CM_MULTICAST_CHANNEL, strChAddress
															, nChPort, m_cmInfo);
			if(mc == null)
			{
				System.out.println("CMStub.addMulticastChannel() failed.");
				return;
			}
			mcInfo.addChannel(mc, nChIndex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public void removeAdditionalMulticastChannel(int nChIndex, String strSession, String strGroup)
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		
		CMSession session = interInfo.findSession(strSession);
		if(session == null)
		{
			System.out.println("CMStub.removeAdditionalMulticastChannel(), session("+strSession+") not found.");
			return;
		}
		CMGroup group = session.findGroup(strGroup);
		if(group == null)
		{
			System.out.println("CMStub.removeAdditionalMulticastChannel(), group("+strGroup+") not found.");
			return;
		}
		CMChannelInfo mcInfo = group.getMulticastChannelInfo();
		mcInfo.removeChannel(nChIndex);
		return;
	}

	////////////////////////////////////////////////////////////////////////
	// event transmission methods (if required, the default server is used.)
	
	public boolean send(CMEvent cme, String strTarget)
	{
		return send(cme, strTarget, CMInfo.CM_STREAM, 0);
	}
	
	public boolean send(CMEvent cme, String strTarget, int opt)
	{
		return send(cme, strTarget, opt, 0);
	}
	
	public boolean send(CMEvent cme, String strTarget, int opt, int nChNum)
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
			ret = CMEventManager.unicastEvent(cme, strTarget, opt, nChNum, m_cmInfo);
		}

		return ret;
	}
	
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
		return multicast(cme, sessionName, groupName, 0);
	}
	
	public boolean multicast(CMEvent cme, String sessionName, String groupName, int nChNum)
	{
		boolean ret = false;
		ret = CMEventManager.multicastEvent(cme, sessionName, groupName, nChNum, m_cmInfo);
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

}
