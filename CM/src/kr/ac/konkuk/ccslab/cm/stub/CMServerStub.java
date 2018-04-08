package kr.ac.konkuk.ccslab.cm.stub;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;
import kr.ac.konkuk.ccslab.cm.manager.CMSNSManager;

/**
 * This class provides APIs, through which a server developer can access the communication services of CM.
 * A server application can use this class in order to request service-specific communication services.
 * 
 * @author mlim
 * @see CMClientStub
 * @see CMStub
 */
public class CMServerStub extends CMStub {

	/**
	 * Creates an instance of the CMServerStub class.
	 * 
	 * <p> This method just called the default constructor of the super class, CMStub. 
	 */
	public CMServerStub()
	{
		super();
	}
	
	/**
	 * Initializes and starts the server CM.
	 * <p> Before the server CM starts, it initializes the configuration and the interaction manager. Then, 
	 * it starts two separate threads for receiving and processing CM events.
	 *  
	 * @return true if the initialization of CM succeeds, or false if the initialization of CM fails.
	 * @see CMServerStub#terminateCM()
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
			CMConfigurator.init("cm-server.conf", m_cmInfo);
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
			System.out.println("CMServerStub.startCM(), succeeded.");
		
		return true;
	}
	
	/**
	 * Terminates the server CM.
	 * <br>A server application calls this method when it does not need to use CM any more. 
	 * The server releases all the resources that are used by CM.
	 * 
	 * @see CMServerStub#startCM()
	 */
	public void terminateCM()
	{
		super.terminateCM();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMServerStub.terminateCM(), succeeded.");
	}
	
	/**
	 * Registers an additional server to the default server.
	 * 
	 * <p> When an additional server starts, it automatically connects to the default server. 
	 * The additional server then needs to request for registration to the default server in order to 
	 * participate in current CM network.
	 * <br> Only an additional server should call the requestServerReg method with a desired server name. 
	 * Because the default server has the reserved name, "SERVER", the additional server must specify 
	 * a different name as the parameter of this method.
	 * <br> In order for a requesting server to check the result of the registration request, 
	 * the server can catch the RES_SERVER_REG event of the CMMultiServerEvent class in its event handler routine. 
	 * The event fields of this event are described below.
	 * 
	 * <table border=1>
	 * <caption>CMMultiServerEvent.RES_SERVER_REG event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_MULTI_SERVER_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMMultiServerEvent.RES_SERVER_REG</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>server name</td><td>String</td><td>the requester server name</td><td>getServerName()</td>
	 * </tr>
	 * <tr>
	 * <td>return code</td><td>int</td>
	 * <td>result code of the registration request
	 * <br>1: succeeded<br>0: failed</td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * </table> 
	 * 
	 * @param server - the server name
	 * @see CMServerStub#requestServerDereg()
	 */
	public void requestServerReg(String server)
	{
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		
		if(server == null)
		{
			System.out.println("CMServerStub.requestServerReg(), the requesting server name is null.");
			return;
		}
		if(CMConfigurator.isDServer(m_cmInfo))
		{
			System.out.println("CMServerStub.requestServerReg(), This is the default server!");
			return;
		}

		CMMultiServerEvent mse = new CMMultiServerEvent();

		mse.setID(CMMultiServerEvent.REQ_SERVER_REG);
		mse.setServerName(server);
		mse.setServerAddress( confInfo.getMyAddress() );
		mse.setServerPort( confInfo.getMyPort() );
		mse.setServerUDPPort( confInfo.getUDPPort() );

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMServerStub.reqServerReg(), server("+server+"), addr("+confInfo.getMyAddress()
					+"), port("+confInfo.getMyPort()+"), udp port("+confInfo.getUDPPort()+").");
		}

		CMEventManager.unicastEvent(mse, "SERVER", m_cmInfo);
		interInfo.getMyself().setName(server);	// to set my server name

		mse = null;
		return;
	}
	
	/**
	 * Deregisters an additional server from the default server.
	 * 
	 * <p> If an additional server leaves current CM network, it can request to deregister from the default server.
	 * Although it leaves the CM network, the additional server still maintains the connection with the default server. 
	 * If required, this connection can also be managed by the {@link CMServerStub#connectToServer()} and 
	 * the {@link CMServerStub#disconnectFromServer()} methods.
	 * 
	 * <br> In order for a requesting server to check the result of the deregistration request, 
	 * the server can catch the RES_SERVER_DEREG event of the CMMultiServerEvent class in its event handler routine. 
	 * The event fields of this event are described below.
	 * 
	 * <table border=1>
	 * <caption>CMMultiServerEvent.RES_SERVER_DEREG event</caption>
	 * <tr>
	 * <td bgcolor="lightgrey">Event type</td><td>CMInfo.CM_MULTI_SERVER_EVENT</td>
	 * </tr>
	 * <tr>
	 * <td bgcolor="lightgrey">Event ID</td><td>CMMultiServerEvent.RES_SERVER_DEREG</td>
	 * </tr>
	 * <tr bgcolor="lightgrey">
	 * <td>Event field</td><td>Field data type</td><td>Field definition</td><td>Get method</td>
	 * </tr>
	 * <tr>
	 * <td>server name</td><td>String</td><td>the requester server name</td><td>getServerName()</td>
	 * </tr>
	 * <tr>
	 * <td>return code</td><td>int</td>
	 * <td>result code of the deregistration request
	 * <br>1: succeeded<br>0: failed</td>
	 * <td>getReturnCode()</td>
	 * </tr>
	 * </table> 
	 * 
	 * @see CMServerStub#requestServerReg(String)
	 */
	public void requestServerDereg()
	{
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();

		if( CMConfigurator.isDServer(m_cmInfo) )
		{
			System.out.println("CMServerStub.requestServerDereg(), this server is the default "
					+ "server!");
			return;
		}

		CMUser myself = interInfo.getMyself();
		if( myself.getName().equals("") )
		{
			System.out.println("CMServerStub.requestServerDereg(), the requesting server name "
					+ "is not defined!");
			return;
		}

		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.REQ_SERVER_DEREG);
		mse.setServerName( myself.getName() );

		CMEventManager.unicastEvent(mse, "SERVER", m_cmInfo);

		mse = null;
		return;
	}
	
	/**
	 * Connects to the default server.
	 * 
	 * <p> An additional server can call this method to establish a connection to 
	 * the default server.
	 * 
	 * @return true if the connection is successfully established, or false otherwise.
	 * @see CMServerStub#disconnectFromServer()
	 */
	public boolean connectToServer()
	{
		boolean result = false;
		if( CMConfigurator.isDServer(m_cmInfo) )
		{
			System.out.println("CMServerStub.connectToServer(), this is the default server!");
			return false;
		}
		result = CMInteractionManager.connectDefaultServer(m_cmInfo);
		return result;
	}

	/**
	 * Disconnects from the default server.
	 * 
	 * <p> An additional server can call this method to disconnect the connection from 
	 * the default server.
	 * 
	 * @return true if the connection is successfully disconnected, or false otherwise.
	 * @see CMServerStub#connectToServer()
	 */
	public boolean disconnectFromServer()
	{
		boolean result = false;
		if( CMConfigurator.isDServer(m_cmInfo) )
		{
			System.out.println("CMServerStub.disconnectFromServer(), this is the default server!");
			return false;
		}
		result = CMInteractionManager.disconnectFromDefaultServer(m_cmInfo);
		return result;
	}
	
	/**
	 * Sets the download scheme for attached images of SNS content.
	 *
	 * <p> The detailed information about the attachment download scheme can be found 
	 * in the following reference: 
	 * <br> <i>Mingyu Lim, "Multi-level Content Transmission Mechanism for Intelligent Quality of Service 
	 * in Social Networking Services," The Transactions on the Korean Institute of Electrical Engineers, 
	 * Vol. 65, No. 8, August 2016, pp.1407-1417.</i>
	 * 
	 * @param strUserName - the target user name
	 * <br> The attachment download scheme is applied to 'strUserName'. If the value is null, 
	 * the download scheme is applied to all users.
	 * @param nScheme - the download scheme
	 * <br> The possible value is CMInfo.SNS_ATTACH_FULL(or 0), CMInfo.SNS_ATTACH_PARTIAL(or 1), 
	 * CMInfo.SNS_ATTACH_PREFETCH(or 2) and CMInfo.SNS_ATTACH_NONE(or 3).
	 * <table border=1>
	 * <caption>Download scheme of attached images of SNS content</caption>
	 * 	<tr bgcolor=lightgrey>
	 * 		<td>download scheme</td><td>description</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>CMInfo.SNS_ATTACH_FULL</td>
	 * 		<td>
	 * 			The CM server sends images with the original quality to the client.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>CMInfo.SNS_ATTACH_PARTIAL</td>
	 * 		<td>
	 * 			The server sends thumbnail images instead of the original images.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>CMInfo.SNS_ATTACH_PREFETCH</td>
	 * 		<td>
	 * 			The server sends thumbnail images to the client, and sends also original 
	 * 			images that the client is interested in.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>CMInfo.SNS_ATTACH_NONE</td>
	 * 		<td>
	 * 			The server sends only text links to images.
	 * 		</td>
	 * 	</tr>
	 * </table>
	 * @see CMClientStub#requestSNSContent(String, int)
	 */
	// change the download scheme for the attachment of SNS content
	public void setAttachDownloadScheme(String strUserName, int nScheme)
	{
		// set the scheme for the user
		CMInteractionInfo interInfo = m_cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		CMMember loginUsers = interInfo.getLoginUsers();
		CMUser tuser = null;
		int nPrevScheme = -1;
				
		// make an event
		CMSNSEvent se = new CMSNSEvent();
		se.setID(CMSNSEvent.CHANGE_ATTACH_DOWNLOAD_SCHEME);
		se.setAttachDownloadScheme(nScheme);

		if(strUserName == null) // change all users
		{
			// change current scheme in the configuration info for late comers
			confInfo.setAttachDownloadScheme(nScheme);

			for(int i=0; i<loginUsers.getMemberNum(); i++)
			{
				tuser = loginUsers.getAllMembers().elementAt(i);
				nPrevScheme = tuser.getAttachDownloadScheme();
				tuser.setAttachDownloadScheme(nScheme);
				if(nPrevScheme != CMInfo.SNS_ATTACH_PREFETCH && nScheme == CMInfo.SNS_ATTACH_PREFETCH)
				{
					// load history info for attachment access of this user
					CMSNSManager.loadAccessHistory(tuser, m_cmInfo);
				}
				else if(nPrevScheme == CMInfo.SNS_ATTACH_PREFETCH && nScheme != CMInfo.SNS_ATTACH_PREFETCH)
				{
					// save the updated or newly added history info for attachment access of this user
					CMSNSManager.saveAccessHistory(tuser, m_cmInfo);
				}

			}
			broadcast(se);
		}
		else
		{
			tuser = loginUsers.findMember(strUserName);
			if(tuser == null)
			{
				System.err.println("CMServerStub.setAttachDownloadScheme(), user("+strUserName+") not found!");
				se = null;
				return;
			}
			
			nPrevScheme = tuser.getAttachDownloadScheme();
			tuser.setAttachDownloadScheme(nScheme);
			if(nPrevScheme != CMInfo.SNS_ATTACH_PREFETCH && nScheme == CMInfo.SNS_ATTACH_PREFETCH)
			{
				// load history info for attachment access of this user
				CMSNSManager.loadAccessHistory(tuser, m_cmInfo);
			}
			else if(nPrevScheme == CMInfo.SNS_ATTACH_PREFETCH && nScheme != CMInfo.SNS_ATTACH_PREFETCH)
			{
				// save the updated or newly added history info for attachment access of this user
				CMSNSManager.saveAccessHistory(tuser, m_cmInfo);
			}

			send(se, strUserName);
		}

		se = null;
		return;
	}
}
