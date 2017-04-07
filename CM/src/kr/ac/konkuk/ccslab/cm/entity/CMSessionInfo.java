package kr.ac.konkuk.ccslab.cm.entity;

public class CMSessionInfo {
	protected String m_strSessionName;
	protected String m_strAddress;
	protected int m_nPort;
	protected int m_nUserNum;		// 세션 내 사용자 수
	
	public CMSessionInfo()
	{
		m_strSessionName = "";
		m_strAddress = "";
		m_nPort = -1;
		m_nUserNum = -1;
	}
	
	public CMSessionInfo(String sname, String address, int port)
	{
		m_strSessionName = sname;
		m_strAddress = address;
		m_nPort = port;
		m_nUserNum = -1;
	}

	public CMSessionInfo(String sname, String address, int port, int nUserNum)
	{
		m_strSessionName = sname;
		m_strAddress = address;
		m_nPort = port;
		m_nUserNum = nUserNum;
	}

	public void setSessionName(String name)
	{
		m_strSessionName = name;
	}
	
	public void setAddress(String addr)
	{
		m_strAddress = addr;
	}
	
	public void setPort(int port)
	{
		m_nPort = port;
	}

	public void setUserNum(int num)
	{
		m_nUserNum = num;
	}

	public String getSessionName()
	{
		return m_strSessionName;
	}
	
	public String getAddress()
	{
		return m_strAddress;
	}
	
	public int getPort()
	{
		return m_nPort;
	}

	public int getUserNum()
	{
		return m_nUserNum;
	}
}
