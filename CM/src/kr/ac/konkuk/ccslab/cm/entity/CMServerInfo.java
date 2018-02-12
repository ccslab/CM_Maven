package kr.ac.konkuk.ccslab.cm.entity;
import kr.ac.konkuk.ccslab.cm.stub.*;
////////////////////////////////////////////////////////
//CMServerInfo (server info to be transfered in an event)

/**
 * This class represents CM server information that needs to be transferred to a remote CM node.
 * 
 * @author mlim
 * @see {@link CMStub#requestServerInfo()}
 */
public class CMServerInfo {
	protected String m_strServerName;
	protected String m_strServerAddress;
	protected int m_nServerPort;
	protected int m_nServerUDPPort;
	
	public CMServerInfo()
	{
		m_strServerName = "";
		m_strServerAddress = "";
		m_nServerPort = -1;
		m_nServerUDPPort = -1;
	}
	
	public CMServerInfo(String sname, String saddr, int sport, int sudpport)
	{
		m_strServerName = sname;
		m_strServerAddress = saddr;
		m_nServerPort = sport;
		m_nServerUDPPort = sudpport;
	}

	public void setServerName(String sname)
	{
		m_strServerName = sname;
	}
	
	public void setServerAddress(String saddr)
	{
		m_strServerAddress = saddr;
	}
	
	public void setServerPort(int port)
	{
		m_nServerPort = port;
	}
	
	public void setServerUDPPort(int port)
	{
		m_nServerUDPPort = port;
	}

	public String getServerName()
	{
		return m_strServerName;
	}
	
	public String getServerAddress()
	{
		return m_strServerAddress;
	}
	
	public int getServerPort()
	{
		return m_nServerPort;
	}
	
	public int getServerUDPPort()
	{
		return m_nServerUDPPort;
	}
}
