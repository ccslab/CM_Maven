package kr.ac.konkuk.ccslab.cm.entity;

import java.util.Calendar;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttachAccessHistoryList;

/**
 * This class represents the information of a CM user (client).
 * 
 * @author mlim
 *
 */
public class CMUser extends CMObject {
	
	private int m_nID;
	private String m_strName;
	private String m_strPasswd;
	private String m_strHost;
	private int m_nUDPPort;
	private CMPosition m_pq;
	private String m_strCurrentSession;
	private String m_strCurrentGroup;
	private int m_nState;
	private CMChannelInfo<Integer> m_nonBlockSocketChannelInfo;
	private CMChannelInfo<Integer> m_blockSocketChannelInfo;
	private int m_nAttachDownloadScheme;	// used at SERVER
	private CMSNSAttachAccessHistoryList m_historyList;	// used at SERVER
	private Calendar m_lastLoginDate;		// used at SERVER
	
	public CMUser()
	{
		m_nType = CMInfo.CM_USER;
		m_nID = -1;
		m_strName = "";
		m_strPasswd = "?";
		m_strHost = "?";
		m_nUDPPort = -1;
		m_strCurrentSession = "?";
		m_strCurrentGroup = "?";
		m_nState = CMInfo.CM_INIT;
		m_nonBlockSocketChannelInfo = new CMChannelInfo<Integer>();
		m_blockSocketChannelInfo = new CMChannelInfo<Integer>();
		m_nAttachDownloadScheme = -1;
		m_historyList = new CMSNSAttachAccessHistoryList();
		m_lastLoginDate = null;
	}
	
	public CMUser(String name, String passwd, String host)
	{
		m_nType = CMInfo.CM_USER;
		m_nID = -1;
		m_strName = name;
		m_strPasswd = passwd;
		m_strHost = host;
		m_nUDPPort = -1;
		m_strCurrentSession = "";
		m_strCurrentGroup = "";
		m_nState = CMInfo.CM_INIT;
		m_nonBlockSocketChannelInfo = new CMChannelInfo<Integer>();
		m_blockSocketChannelInfo = new CMChannelInfo<Integer>();
		m_nAttachDownloadScheme = -1;
		m_historyList = new CMSNSAttachAccessHistoryList();
		m_lastLoginDate = null;
	}
	
	// set methods
	public void setID(int id)
	{
		m_nID = id;
	}
	
	public void setName(String name)
	{
		m_strName = name;
	}
	
	public void setPasswd(String passwd)
	{
		m_strPasswd = passwd;
	}
	
	public void setHost(String host)
	{
		m_strHost = host;
	}
	
	public void setUDPPort(int port)
	{
		m_nUDPPort = port;
	}
	
	public void setPosition(CMPosition pq)
	{
		m_pq = pq;
	}
	
	public void setCurrentSession(String sName)
	{
		m_strCurrentSession = sName;
	}
	
	public void setCurrentGroup(String gName)
	{
		m_strCurrentGroup = gName;
	}

	public void setState(int state)
	{
		m_nState = state;
	}
	
	public void setAttachDownloadScheme(int scheme)
	{
		m_nAttachDownloadScheme = scheme;
	}
	
	public void setAttachAccessHistoryList(CMSNSAttachAccessHistoryList list)
	{
		m_historyList = list;
	}
	
	public void setLastLoginDate(Calendar date)
	{
		m_lastLoginDate = date;
	}
	
	// get methods
	public int getID()
	{
		return m_nID;
	}
	
	public String getName()
	{
		return m_strName;
	}
	
	public String getPasswd()
	{
		return m_strPasswd;
	}
	
	public String getHost()
	{
		return m_strHost;
	}
	
	public int getUDPPort()
	{
		return m_nUDPPort;
	}
	
	public CMPosition getPosition()
	{
		return m_pq;
	}
	
	public String getCurrentSession()
	{
		return m_strCurrentSession;
	}
	
	public String getCurrentGroup()
	{
		return m_strCurrentGroup;
	}
	
	public int getState()
	{
		return m_nState;
	}
	
	public CMChannelInfo<Integer> getNonBlockSocketChannelInfo()
	{
		return m_nonBlockSocketChannelInfo;
	}
	
	public CMChannelInfo<Integer> getBlockSocketChannelInfo()
	{
		return m_blockSocketChannelInfo;
	}
	
	public int getAttachDownloadScheme()
	{
		return m_nAttachDownloadScheme;
	}
	
	public CMSNSAttachAccessHistoryList getAttachAccessHistoryList()
	{
		return m_historyList;
	}
	
	public Calendar getLastLoginDate()
	{
		return m_lastLoginDate;
	}
}
