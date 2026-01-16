package kr.ac.konkuk.ccslab.cm.event;

import java.util.Objects;
import java.util.UUID;
import java.util.Vector;

public class CMEventSynchronizer {

	private int m_nWaitedEventType;
	private int m_nWaitedEventID;
	private String m_strWaitedReceiver;
	private UUID m_waitedReceiverUuid;
	private CMEvent m_replyEvent;
	
	private Vector<CMEvent> m_replyEventList;
	private int m_nMinNumWaitedEvents;
	
	public CMEventSynchronizer()
	{
		m_nWaitedEventType = -1;
		m_nWaitedEventID = -1;
		m_strWaitedReceiver = "";
		m_waitedReceiverUuid = null;
		m_replyEvent = null;
		m_replyEventList = new Vector<CMEvent>();
		m_nMinNumWaitedEvents = 0;
	}
	
	public synchronized void init()
	{
		m_nWaitedEventType = -1;
		m_nWaitedEventID = -1;
		m_strWaitedReceiver = "";
		m_waitedReceiverUuid = null;
		m_replyEvent = null;
		m_replyEventList.clear();
		m_nMinNumWaitedEvents = 0;
	}
	
	public synchronized boolean isWaiting()
	{
		boolean bReturn = false;
		if(m_nWaitedEventType != -1 && m_nWaitedEventID != -1)
			bReturn = true;
		
		return bReturn;
	}
	
	//////////////////////////////////////////////////////
	// event list methods
	
	public synchronized boolean addReplyEvent(CMEvent event)
	{
		boolean ret = false;
		
		if(findReplyEvent(event)) return false;
		ret = m_replyEventList.add(event);
		return ret;
	}
	
	private synchronized boolean findReplyEvent(CMEvent event)
	{
		boolean bFound = false;
		CMEvent tmpEvent;
		
		for(int i = 0; i < m_replyEventList.size() && !bFound; i++)
		{
			tmpEvent = m_replyEventList.get(i);
			if( tmpEvent.getSender().equals(event.getSender()) &&
					Objects.equals(tmpEvent.getSenderUuid(), event.getSenderUuid()) &&
					tmpEvent.getType() == m_nWaitedEventType && tmpEvent.getID() == m_nWaitedEventID )
				bFound = true;
		}
		
		return bFound;
	}
	
	public synchronized boolean isCompleteReplyEvents()
	{
		if( m_replyEventList.size() >= m_nMinNumWaitedEvents )
		{
			return true;
		}
		return false;
	}
	
	//////////////////////////////////////////////////////
	// get/set methods
	
	public synchronized void setWaitedEvent(int nType, int nID, String strReceiver, UUID receiverUuid)
	{
		m_nWaitedEventType = nType;
		m_nWaitedEventID = nID;
		m_strWaitedReceiver = strReceiver;
		m_waitedReceiverUuid = receiverUuid;
	}
	
	public synchronized void setWaitedEventType(int nType)
	{
		m_nWaitedEventType = nType;
	}

	public synchronized int getWaitedEventType()
	{
		return m_nWaitedEventType;
	}

	public synchronized void setWaitedEventID(int nID)
	{
		m_nWaitedEventID = nID;
	}

	public synchronized int getWaitedEventID()
	{
		return m_nWaitedEventID;
	}

	public synchronized void setReplyEvent(CMEvent event)
	{
		m_replyEvent = event;
	}

	public synchronized CMEvent getReplyEvent()
	{
		return m_replyEvent;
	}

	public synchronized String getWaitedReceiver()
	{
		return m_strWaitedReceiver;
	}

	public synchronized UUID getWaitedReceiverUuid() { return m_waitedReceiverUuid; }
	
	public synchronized void setMinNumWaitedEvents(int num)
	{
		m_nMinNumWaitedEvents = num;
	}
	
	public synchronized int getMinNumWaitedEvents()
	{
		return m_nMinNumWaitedEvents;
	}
	
	public synchronized CMEvent[] getReplyEventListAsArray()
	{
		if(m_replyEventList.isEmpty()) return null;
		CMEvent[] eventArray = new CMEvent[m_replyEventList.size()];
		eventArray = m_replyEventList.toArray(eventArray);
		
		return eventArray;
	}
	
}
