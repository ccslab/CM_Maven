package kr.ac.konkuk.ccslab.cm.event;

public class CMEventSynchronizer {

	int m_nWaitedEventType;
	int m_nWaitedEventID;
	String m_strWaitedReceiver;
	CMEvent m_replyEvent;
	
	public CMEventSynchronizer()
	{
		m_nWaitedEventType = -1;
		m_nWaitedEventID = -1;
		m_strWaitedReceiver = "";
		m_replyEvent = null;
	}
	
	public synchronized void init()
	{
		m_nWaitedEventType = -1;
		m_nWaitedEventID = -1;
		m_strWaitedReceiver = "";
		m_replyEvent = null;
	}
	
	public synchronized boolean isWaiting()
	{
		boolean bReturn = false;
		if(m_nWaitedEventType != -1 && m_nWaitedEventID != -1)
			bReturn = true;
		
		return bReturn;
	}
	
	public synchronized void setWaitedEvent(int nType, int nID, String strReceiver)
	{
		m_nWaitedEventType = nType;
		m_nWaitedEventID = nID;
		m_strWaitedReceiver = strReceiver;
	}
	
	public synchronized void setReplyEvent(CMEvent event)
	{
		m_replyEvent = event;
	}
	
	public synchronized int getWaitedEventType()
	{
		return m_nWaitedEventType;
	}
	
	public synchronized int getWaitedEventID()
	{
		return m_nWaitedEventID;
	}
	
	public synchronized CMEvent getReplyEvent()
	{
		return m_replyEvent;
	}
	
	public synchronized String getWaitedReceiver()
	{
		return m_strWaitedReceiver;
	}
}
