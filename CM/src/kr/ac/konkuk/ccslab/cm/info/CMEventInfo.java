package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

public class CMEventInfo {
	private CMEventReceiver m_eventReceiver;
	private CMEventSynchronizer m_eventSynchronizer;
	
	// monitor object to wait for receiving the END_FILE_TRANSFER
	private Object m_EFTObject;
	// received file size that is needed to measure the end-to-end input network throughput
	private long m_lEFTFileSize;
	// monitor object to wait for receiving the END_FILE_TRANSFER_ACK
	private Object m_EFTAObject;
	// received file size that is needed to measure the end-to-end output network throughput
	private long m_lEFTAFileSize;
	
	public CMEventInfo()
	{
		m_eventReceiver = null;
		m_eventSynchronizer = new CMEventSynchronizer();
		
		m_EFTObject = new Object();
		m_lEFTFileSize = -1;
		m_EFTAObject = new Object();
		m_lEFTAFileSize = -1;
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
	
	public Object getEFTObject()
	{
		return m_EFTObject;
	}
	
	public void setEFTFileSize(long fSize)
	{
		m_lEFTFileSize = fSize;
	}
	
	public long getEFTFileSize()
	{
		return m_lEFTFileSize;
	}
	
	public Object getEFTAObject()
	{
		return m_EFTAObject;
	}
	
	public void setEFTAFileSize(long fSize)
	{
		m_lEFTAFileSize = fSize;
	}
	
	public long getEFTAFileSize()
	{
		return m_lEFTAFileSize;
	}
}
