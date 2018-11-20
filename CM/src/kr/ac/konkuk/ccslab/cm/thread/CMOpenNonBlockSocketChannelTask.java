package kr.ac.konkuk.ccslab.cm.thread;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;

public class CMOpenNonBlockSocketChannelTask implements Callable<SocketChannel> {
	private int m_nChType;
	private String m_strServerAddress;
	private int m_nServerPort;
	private CMInfo m_cmInfo;
	
	public CMOpenNonBlockSocketChannelTask(int nChType, String strServerAddress, int nServerPort, CMInfo cmInfo)
	{
		m_nChType = nChType;
		m_strServerAddress = strServerAddress;
		m_nServerPort = nServerPort;
		m_cmInfo = cmInfo;
	}
	
	@Override
	public SocketChannel call()
	{
		SocketChannel sc = null;
		try {
			sc = (SocketChannel) CMCommManager.openNonBlockChannel(m_nChType, 
					m_strServerAddress, m_nServerPort, m_cmInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sc;
	}
	
	// set/get methods
	public void setChannelType(int nChType)
	{
		m_nChType = nChType;
	}
	
	public int getChannelType()
	{
		return m_nChType;
	}
	
	public void setServerAddress(String strServerAddress)
	{
		m_strServerAddress = strServerAddress;
	}
	
	public String getServerAddress()
	{
		return m_strServerAddress;
	}
	
	public void setServerPort(int nServerPort)
	{
		m_nServerPort = nServerPort;
	}
	
	public int getServerPort()
	{
		return m_nServerPort;
	}
	
	public void setCMInfo(CMInfo cmInfo)
	{
		m_cmInfo = cmInfo;
	}
	
	public CMInfo getCMInfo()
	{
		return m_cmInfo;
	}
}
