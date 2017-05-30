package kr.ac.konkuk.ccslab.cm.entity;

public class CMGroupInfo {
	protected String m_strGroupName;
	protected String m_strGroupAddress;
	protected int m_nGroupPort;

	public CMGroupInfo()
	{
		m_strGroupName = "";
		m_strGroupAddress = "";
		m_nGroupPort = -1;
	}
	
	public CMGroupInfo(String gname, String address, int port)
	{
		m_strGroupName = gname;
		m_strGroupAddress = address;
		m_nGroupPort = port;
	}

	public void setGroupName(String name)
	{
		m_strGroupName = name;
	}
	
	public void setGroupAddress(String addr)
	{
		m_strGroupAddress = addr;
	}
	
	public void setGroupPort(int port)
	{
		m_nGroupPort = port;
	}
	
	public String getGroupName()
	{
		return m_strGroupName;
	}
	
	public String getGroupAddress()
	{
		return m_strGroupAddress;
	}
	
	public int getGroupPort()
	{
		return m_nGroupPort;
	}
}
