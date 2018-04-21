package kr.ac.konkuk.ccslab.cm.event;

public class CMEventSynchronizer {

	int m_nWaitingEventType;
	int m_nWaitingEventID;
	CMEvent m_replyEvent;
	
	public CMEventSynchronizer()
	{
		m_nWaitingEventType = -1;
		m_nWaitingEventID = -1;
		m_replyEvent = null;
	}
	
	public synchronized void init()
	{
		m_nWaitingEventType = -1;
		m_nWaitingEventID = -1;
		m_replyEvent = null;
	}
	
	public synchronized boolean isWaiting()
	{
		boolean bReturn = false;
		if(m_nWaitingEventType != -1 && m_nWaitingEventID != -1)
			bReturn = true;
		
		return bReturn;
	}
	
	public synchronized void setWaitingEvent(int nType, int nID)
	{
		m_nWaitingEventType = nType;
		m_nWaitingEventID = nID;
	}
	
	public synchronized void setReplyEvent(CMEvent event)
	{
		m_replyEvent = event;
	}
	
	public synchronized int getWaitingEventType()
	{
		return m_nWaitingEventType;
	}
	
	public synchronized int getWaitingEventID()
	{
		return m_nWaitingEventID;
	}
	
	public synchronized CMEvent getReplyEvent()
	{
		return m_replyEvent;
	}
}
