package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

public class CMEventInfo {
	private CMEventReceiver m_eventReceiver;
	private CMEventSynchronizer m_eventSynchronizer;
	
	public CMEventInfo()
	{
		m_eventReceiver = null;
		m_eventSynchronizer = new CMEventSynchronizer();		
	}
	
	// set/get methods

	public void setEventReceiver(CMEventReceiver receiver)
	{
		m_eventReceiver = receiver;
	}
	
	public CMEventReceiver getEventReceiver()
	{
		return m_eventReceiver;
	}
	
	public void setEventSynchronizer(CMEventSynchronizer sync)
	{
		m_eventSynchronizer = sync;
	}
	
	public CMEventSynchronizer getEventSynchronizer()
	{
		return m_eventSynchronizer;
	}
	
}
