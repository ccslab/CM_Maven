package kr.ac.konkuk.ccslab.cm.entity;

import java.nio.channels.SocketChannel;

public class CMUnknownChannelInfo {

	private SocketChannel m_unknownChannel;
	private long m_lLastEventTransTime;
	private int m_nNumLoginFailure;
	
	public CMUnknownChannelInfo(SocketChannel ch)
	{
		m_unknownChannel = ch;
		m_lLastEventTransTime = System.currentTimeMillis();
		m_nNumLoginFailure = 0;
	}
	
	public synchronized void setUnknownChannel(SocketChannel ch)
	{
		m_unknownChannel = ch;
	}
	
	public synchronized SocketChannel getUnknownChannel()
	{
		return m_unknownChannel;
	}
	
	public synchronized void setLastEventTransTime(long lTime)
	{
		m_lLastEventTransTime = lTime;
	}
	
	public synchronized long getLastEventTransTime()
	{
		return m_lLastEventTransTime;
	}
	
	public synchronized void setNumLoginFailure(int nCount)
	{
		m_nNumLoginFailure = nCount;
	}
	
	public synchronized int getNumLoginFailure()
	{
		return m_nNumLoginFailure;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		CMUnknownChannelInfo unch = (CMUnknownChannelInfo) obj;
		
		if( m_unknownChannel.equals(unch.getUnknownChannel()) )
			return true;
		
		return false;
	}

}
