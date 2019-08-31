package kr.ac.konkuk.ccslab.cm.entity;

import java.nio.channels.SelectableChannel;

public class CMTransFileInfo extends Object {
	protected String m_strSenderName;	// the sender name
	protected String m_strReceiverName;// the receiver name
	protected String m_strFileName; // the name of the transferred file
	protected String m_strFilePath;	// the local full path to the sent or received file
	protected long m_lFileSize;	  // the size of the transferred file
	protected int m_nContentID;	  // the identifier of content to which the transferred file belongs
	protected SelectableChannel m_defaultChannel;	// default socket channel (used for multiple channels)
	
	public CMTransFileInfo()
	{
		m_strSenderName = "?";
		m_strReceiverName = "?";
		m_strFileName = "?";
		m_strFilePath = "?";
		m_lFileSize = -1;
		m_nContentID = -1;
		m_defaultChannel = null;
	}
	
	public CMTransFileInfo(String strFile, long lSize, int nID)
	{
		m_strSenderName = "?";
		m_strReceiverName = "?";
		m_strFileName = strFile;
		m_strFilePath = "?";
		m_lFileSize = lSize;
		m_nContentID = nID;
		m_defaultChannel = null;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null) return false;
		if(!this.getClass().equals(o.getClass())) return false;
		if(this == o) return true;

		CMTransFileInfo tfInfo = (CMTransFileInfo) o;
		String strFileName = tfInfo.getFileName();
		int nContentID = tfInfo.getContentID();
		
		if(strFileName.equals(m_strFileName) && nContentID == m_nContentID)
			return true;
		
		return false;	
	}
	
	@Override
	public String toString()
	{
		String strInfo = "CMTransFileInfo: file("+m_strFileName+"), content ID("+m_nContentID+")";
		return strInfo;
	}
	
	// get/set methods
	
	public synchronized void setSenderName(String strName)
	{
		m_strSenderName = strName;
		return;
	}
	
	public synchronized String getSenderName()
	{
		return m_strSenderName;
	}
	
	public synchronized void setReceiverName(String strName)
	{
		m_strReceiverName = strName;
		return;
	}
	
	public synchronized String getReceiverName()
	{
		return m_strReceiverName;
	}

	public synchronized void setFileName(String strName)
	{
		m_strFileName = strName;
		return;
	}
	
	public synchronized String getFileName()
	{
		return m_strFileName;
	}
	
	public synchronized void setFilePath(String strPath)
	{
		m_strFilePath = strPath;
		return;
	}
	
	public synchronized String getFilePath()
	{
		return m_strFilePath;
	}
	
	public synchronized void setFileSize(long lSize)
	{
		m_lFileSize = lSize;
		return;
	}
	
	public synchronized long getFileSize()
	{
		return m_lFileSize;
	}
	
	public synchronized void setContentID(int nID)
	{
		m_nContentID = nID;
		return;
	}
	
	public synchronized int getContentID()
	{
		return m_nContentID;
	}
	
	public synchronized void setDefaultChannel(SelectableChannel channel)
	{
		m_defaultChannel = channel;
		return;
	}
	
	public synchronized SelectableChannel getDefaultChannel()
	{
		return m_defaultChannel;
	}
		
}
