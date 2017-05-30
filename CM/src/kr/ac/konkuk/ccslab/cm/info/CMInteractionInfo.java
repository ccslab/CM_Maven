package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;

public class CMInteractionInfo {
	private CMMember m_loginUsers;
	private Vector<CMSession> m_sessionList;
	private CMUser m_myself;
	private CMServer m_defaultServerInfo;		// default server info (for client)
	private Vector<CMServer> m_addServerList;	// additional server info (for client)
	
	public CMInteractionInfo()
	{
		m_loginUsers = new CMMember();
		m_sessionList = new Vector<CMSession>();
		m_myself = new CMUser();
		m_defaultServerInfo = new CMServer();
		m_addServerList = new Vector<CMServer>();
/*
		CMUser tuser = new CMUser();
		tuser.setName("mlim");
		m_loginUsers.addMember(tuser);
*/		
	}
	
	// get methods
	public CMMember getLoginUsers()
	{
		return m_loginUsers;
	}
	
	public Vector<CMSession> getSessionList()
	{
		return m_sessionList;
	}
	
	public CMUser getMyself()
	{
		return m_myself;
	}
	
	public CMServer getDefaultServerInfo()
	{
		return m_defaultServerInfo;
	}
	
	public Vector<CMServer> getAddServerList()
	{
		return m_addServerList;
	}
	
	//////////////////////////////////////////////////////////
	// session membership management
	
	// check if a session is member or not
	public boolean isMember(String strSessionName)
	{
		CMSession tSession = null;
		boolean bFound = false;
		Iterator<CMSession> iter = m_sessionList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tSession = iter.next();
			String tname = tSession.getSessionName();
			if(strSessionName.equals(tname))
			{
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	// create a session and add it to the session list
	// strAddr: address of a server which manages the session
	// nPort: port of a server which manages the session
	public CMSession createSession(String strSessionName, String strAddr, int nPort)
	{
		CMSession cmSession = null;
		if( isMember(strSessionName) )
		{
			System.out.println("CMInteractionInfo.createSession(), session("+strSessionName+") already exists.");
			return null;
		}
		
		cmSession = new CMSession(strSessionName, strAddr, nPort);
		addSession(cmSession);
		
		return cmSession;
	}

	public boolean addSession(CMSession session)
	{
		String sname = session.getSessionName();
		
		if(isMember(sname))
		{
			System.out.println("CMInteractionInfo.addSession(), session("+sname+") already exists.");
			return false;
		}
		
		m_sessionList.addElement(session);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMIntearctionInfo.addSession(), session("+sname+") added.");
		return true;
	}

	public boolean removeSession(String strSessionName)
	{
		CMSession tSession = null;
		boolean bFound = false;
		Iterator<CMSession> iter = m_sessionList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tSession = iter.next();
			String tname = tSession.getSessionName();
			if(strSessionName.equals(tname))
			{
				iter.remove();
				bFound = true;
			}
		}
		
		if(bFound)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMInteractionInfo.removeSession(), session("+strSessionName+") removed.");
		}
		else
		{
			System.out.println("CMInteractionInfo.removeSession(), session("+strSessionName+") not found.");
		}
		
		return bFound;
	}

	public CMSession findSession(String strSessionName)
	{
		CMSession tSession = null;
		boolean bFound = false;
		Iterator<CMSession> iter = m_sessionList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tSession = iter.next();
			String tname = tSession.getSessionName();
			if(strSessionName.equals(tname))
			{
				bFound = true;
			}
		}
		
		if(!bFound)
			tSession = null;
		
		return tSession;
	}
	
	public CMSession findSessionWithUserName(String strUserName)
	{
		CMSession tSession = null;
		boolean bFound = false;
		Iterator<CMSession> iter = m_sessionList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tSession = iter.next();
			if(tSession.getSessionUsers().isMember(strUserName))
			{
				bFound = true;
			}
		}
		
		if(!bFound)
			tSession = null;
		
		return tSession;
	}
	
	//////////////////////////////////////////////////////////
	// membership management of additional server info

	// check if a server is member or not
	public boolean isAddServer(String strServerName)
	{
		CMServer tServer = null;
		boolean bFound = false;
		Iterator<CMServer> iter = m_addServerList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tServer = iter.next();
			String tname = tServer.getServerName();
			if(strServerName.equals(tname))
			{
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	// create additional server info and add it to the add-server list
	public CMServer createAddServer(String strServerName, String strAddr, int nPort, int nUDPPort)
	{
		CMServer cmServer = null;
		if( isMember(strServerName) )
		{
			System.out.println("CMInteractionInfo.createAddserver(), server("+strServerName+") already exists.");
			return null;
		}
		
		cmServer = new CMServer(strServerName, strAddr, nPort, nUDPPort);
		addAddServer(cmServer);
		
		return cmServer;
	}

	public boolean addAddServer(CMServer server)
	{
		String sname = server.getServerName();
		
		if(isMember(sname))
		{
			System.out.println("CMInteractionInfo.addAddServer(), server("+sname+") already exists.");
			return false;
		}
		
		m_addServerList.addElement(server);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMIntearctionInfo.addAddServer(), server("+sname+") added.");
		return true;
	}

	public boolean removeAddServer(String strServerName)
	{
		CMServer tServer = null;
		boolean bFound = false;
		Iterator<CMServer> iter = m_addServerList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tServer = iter.next();
			String tname = tServer.getServerName();
			if(strServerName.equals(tname))
			{
				iter.remove();
				bFound = true;
			}
		}
		
		if(bFound)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMInteractionInfo.removeAddServer(), server("+strServerName+") removed.");
		}
		else
		{
			System.out.println("CMInteractionInfo.removeAddServer(), server("+strServerName+") not found.");
		}
		
		return bFound;
	}

	public CMServer findAddServer(String strServerName)
	{
		CMServer tServer = null;
		boolean bFound = false;
		Iterator<CMServer> iter = m_addServerList.iterator();
		
		while(iter.hasNext() && !bFound)
		{
			tServer = iter.next();
			String tname = tServer.getServerName();
			if(strServerName.equals(tname))
			{
				bFound = true;
			}
		}
		
		if(!bFound)
			tServer = null;
		
		return tServer;
	}

}
