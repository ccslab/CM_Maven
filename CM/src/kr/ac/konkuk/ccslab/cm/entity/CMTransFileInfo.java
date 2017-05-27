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
	
	public void setSenderName(String strName)
	{
		m_strSenderName = strName;
		return;
	}
	
	public String getSenderName()
	{
		return m_strSenderName;
	}
	
	public void setReceiverName(String strName)
	{
		m_strReceiverName = strName;
		return;
	}
	
	public String getReceiverName()
	{
		return m_strReceiverName;
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
	
	public void setFilePath(String strPath)
	{
		m_strFilePath = strPath;
		return;
	}
	
	public String getFilePath()
	{
		return m_strFilePath;
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
