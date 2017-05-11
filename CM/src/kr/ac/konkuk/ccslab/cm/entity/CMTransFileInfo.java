package kr.ac.konkuk.ccslab.cm.entity;

import java.nio.channels.SelectableChannel;

public class CMTransFileInfo {
	protected String m_strSenderName; 	// the sender name
	protected String m_strFileName; // the name of the transferred file
	protected long m_lFileSize;	  // the size of the transferred file
	protected int m_nContentID;	  // the identifier of content to which the transferred file belongs
	protected SelectableChannel m_defaultChannel;	// default socket channel (used for multiple channels)
	
	public CMTransFileInfo()
	{
		m_strSenderName = null;
		m_strFileName = null;
		m_lFileSize = -1;
		m_nContentID = -1;
		m_defaultChannel = null;
	}
	
	public CMTransFileInfo(String strFile, long lSize, int nID)
	{
		m_strSenderName = null;
		m_strFileName = strFile;
		m_lFileSize = lSize;
		m_nContentID = nID;
		m_defaultChannel = null;
	}
	
	// get/set methods

	public void setSenderName(String strName)
	{
		m_strSenderName = strName;
		return;
	}
	
	public String getSenderName()
	{
		return m_strSenderName;
	}
	
	public void setFileName(String strName)
	{
		m_strFileName = strName;
		return;
	}
	
	public String getFileName()
	{
		return m_strFileName;
	}
	
	public void setFileSize(long lSize)
	{
		m_lFileSize = lSize;
		return;
	}
	
	public long getFileSize()
	{
		return m_lFileSize;
	}
	
	public void setContentID(int nID)
	{
		m_nContentID = nID;
		return;
	}
	
	public int getContentID()
	{
		return m_nContentID;
	}
	
	public void setDefaultChannel(SelectableChannel channel)
	{
		m_defaultChannel = channel;
		return;
	}
	
	public SelectableChannel getDefaultChannel()
	{
		return m_defaultChannel;
	}
		
}
