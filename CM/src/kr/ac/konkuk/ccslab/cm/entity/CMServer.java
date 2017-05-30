package kr.ac.konkuk.ccslab.cm.entity;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

// server information managed by a client
public class CMServer extends CMServerInfo {
	private CMChannelInfo<Integer> m_nonBlockSocketChannelInfo;
	private CMChannelInfo<Integer> m_blockSocketChannelInfo;
	
	private String m_strCurrentSessionName;		// only 4 client
	private String m_strCurrentGroupName;		// only 4 client
	private int m_nClientState;					// only 4 client
	private String m_strCommArch;				// only 4 client
	private boolean m_bLoginScheme;				// only 4 client
	private boolean m_bSessionScheme;			// only 4 client
	private Vector<CMSession> m_sessionList;	// only 4 client

	
	public CMServer()
	{
		super();
		m_nonBlockSocketChannelInfo = new CMChannelInfo<Integer>();
		m_blockSocketChannelInfo = new CMChannelInfo<Integer>();
		
		m_strCurrentSessionName = "";
		m_strCurrentGroupName = "";
		m_nClientState = CMInfo.CM_INIT;
		m_strCommArch = "";
		m_bLoginScheme = false;
		m_bSessionScheme = false;
		m_sessionList = new Vector<CMSession>();
	}
	
	public CMServer(String sname, String saddr, int nPort, int nUDPPort)
	{
		super(sname, saddr, nPort, nUDPPort);
		m_nonBlockSocketChannelInfo = new CMChannelInfo<Integer>();
		m_blockSocketChannelInfo = new CMChannelInfo<Integer>();
		
		m_strCurrentSessionName = "";
		m_strCurrentGroupName = "";
		m_nClientState = CMInfo.CM_INIT;
		m_strCommArch = "";
		m_bLoginScheme = false;
		m_bSessionScheme = false;
		m_sessionList = new Vector<CMSession>();
	}
	
	// set/get methods
	
	public CMChannelInfo<Integer> getNonBlockSocketChannelInfo()
	{
		return m_nonBlockSocketChannelInfo;
	}
	
	public CMChannelInfo<Integer> getBlockSocketChannelInfo()
	{
		return m_blockSocketChannelInfo;
	}
	
	public void setCurrentSessionName(String name)
	{
		m_strCurrentSessionName = name;
	}
	
	public String getCurrentSessionName()
	{
		return m_strCurrentSessionName;
	}
	
	public void setCurrentGroupName(String name)
	{
		m_strCurrentGroupName = name;
	}
	
	public String getCurrentGroupName()
	{
		return m_strCurrentGroupName;
	}
	
	public void setClientState(int state)
	{
		m_nClientState = state;
	}
	
	public int getClientState()
	{
		return m_nClientState;
	}
	
	public void setCommArch(String ca)
	{
		m_strCommArch = ca;
	}
	
	public String getCommArch()
	{
		return m_strCommArch;
	}
	
	public void setLoginScheme(boolean bLogin)
	{
		m_bLoginScheme = bLogin;
	}
	
	public boolean isLoginScheme()
	{
		return m_bLoginScheme;
	}
	
	public void setSessionScheme(boolean bSession)
	{
		m_bSessionScheme = bSession;
	}
	
	public boolean isSessionScheme()
	{
		return m_bSessionScheme;
	}
	
	// session management
	public int getSessionNum()
	{
		return m_sessionList.size();
	}
	
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

	public boolean addSession(CMSession session)
	{
		String sname = session.getSessionName();
		
		if(isMember(sname))
		{
			System.out.println("CMServer.addSession(), session("+sname+") already exists in server("
					+m_strServerName+").");
			return false;
		}
		
		m_sessionList.addElement(session);
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMServer.addSession(), session("+sname+") added in server("
						+m_strServerName+").");
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
				System.out.println("CMServer.removeSession(), session("+strSessionName+") removed in server("
						+m_strServerName+").");
		}
		else
		{
			System.out.println("CMServer.removeSession(), session("+strSessionName+" not found in server("
					+m_strServerName+").");
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
	
	public Vector<CMSession> getSessionList()
	{
		return m_sessionList;
	}

}
