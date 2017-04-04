package kr.ac.konkuk.ccslab.cm.stub;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import kr.ac.konkuk.ccslab.cm.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.CMInfo;
import kr.ac.konkuk.ccslab.cm.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.CMMember;
import kr.ac.konkuk.ccslab.cm.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.CMUser;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;
import kr.ac.konkuk.ccslab.cm.manager.CMSNSManager;

public class CMServerStub extends CMStub {
	
	public CMServerStub()
	{
		super();
	}
	
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
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMServerStub.startCM(), succeeded.");
		
		return true;
	}
	
	public void terminateCM()
	{
		super.terminateCM();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMServerStub.terminateCM(), succeeded.");
	}
	
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
	
	// connect to the default server
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
	
	// disconnect from the default server
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
