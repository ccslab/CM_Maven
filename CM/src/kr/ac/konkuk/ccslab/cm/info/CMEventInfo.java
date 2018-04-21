package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.thread.CMEventReceiver;

public class CMEventInfo {
	private CMEventReceiver m_eventReceiver;
	private CMEventSynchronizer m_eventSynchronizer;
	
	// monitor object to wait for receiving the ADD_BLOCK_SOCKET_CHANNEL_ACK
	private Object m_ABSCAObject;
	// return code of the ADD_BLOCK_SOCKET_CHANNEL_ACK event (-1: initial value, 0: false, 1: true)
	private int m_nABSCAReturnCode;
	// monitor object to wait for receiving the REMOVE_BLOCK_SOCKET_CHANNEL_ACK
	private Object m_RBSCAObject;
	// return code of the REMOVE_BLOCK_SOCKET_CHANNEL_ACK event (-1: initial value, 0: false, 1: true)
	private int m_nRBSCAReturnCode;
	// monitor object to wait for receiving the ADD_NONBLOCK_SOCKET_CHANNEL_ACK
	private Object m_ANBSCAObject;
	// return code of the ADD_NONBLOCK_SOCKET_CHANNEL_ACK event (-1: initial value, 0: false, 1: true)
	private int m_nANBSCAReturnCode;
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
		
		m_ABSCAObject = new Object();
		m_nABSCAReturnCode = -1;
		m_RBSCAObject = new Object();
		m_nRBSCAReturnCode = -1;
		m_ANBSCAObject = new Object();
		m_nANBSCAReturnCode = -1;
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
	
	public Object getABSCAObject()
	{
		return m_ABSCAObject;
	}
	
	public void setABSCAReturnCode(int nCode)
	{
		m_nABSCAReturnCode = nCode;
	}
	
	public int getABSCAReturnCode()
	{
		return m_nABSCAReturnCode;
	}
	
	public Object getRBSCAObject()
	{
		return m_RBSCAObject;
	}
	
	public void setRBSCAReturnCode(int nCode)
	{
		m_nRBSCAReturnCode = nCode;
	}
	
	public int getRBSCAReturnCode()
	{
		return m_nRBSCAReturnCode;
	}
	
	public Object getANBSCAObject()
	{
		return m_ANBSCAObject;
	}
	
	public void setANBSCAReturnCode(int nCode)
	{
		m_nANBSCAReturnCode = nCode;
	}
	
	public int getANBSCAReturnCode()
	{
		return m_nANBSCAReturnCode;
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
