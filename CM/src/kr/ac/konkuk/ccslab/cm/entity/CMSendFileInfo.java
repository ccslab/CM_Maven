package kr.ac.konkuk.ccslab.cm.entity;

import java.io.FileInputStream;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.Future;

public class CMSendFileInfo extends CMTransFileInfo {
	private String m_strReceiverName;	// the receiver name
	private SelectableChannel m_sendChannel; // the dedicated channel for sending the file
	private String m_strFilePath;	// the path of the sent file
	private FileInputStream m_fis;	// the file input stream that is used to read data of the sent file
	private Future m_sendTaskResult;	// the result of the submitted sending task to the thread pool
	
	private boolean m_bStarted;

	public CMSendFileInfo()
	{
		super();
		m_strReceiverName = "?";
		m_sendChannel = null;
		m_strFilePath = "?";
		m_fis = null;
		m_sendTaskResult = null;
		
		m_bStarted = false;
	}
	
	public CMSendFileInfo(String strFile, long lSize)
	{
		super(strFile, lSize, -1);
		m_strReceiverName = "?";
		m_sendChannel = null;
		m_strFilePath = "?";
		m_fis = null;
		m_sendTaskResult = null;
		
		m_bStarted = false;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!super.equals(o)) return false;
		
		CMSendFileInfo sfInfo = (CMSendFileInfo) o;
		String strReceiverName = sfInfo.getReceiverName();
		
		if(strReceiverName.equals(m_strReceiverName))
			return true;
		return false;
	}
	
	@Override
	public String toString()
	{
		String strInfo = super.toString();
		strInfo += ", receiver("+m_strReceiverName+")";
		return strInfo;
	}
	
	// set/get method
		
	public void setReceiverName(String strName)
	{
		m_strReceiverName = strName;
		return;
	}
	
	public String getReceiverName()
	{
		return m_strReceiverName;
	}
	
	public void setFilePath(String strPath)
	{
		m_strFilePath = strPath;
		return;
	}
	
	public void setSendChannel(SelectableChannel channel)
	{
		m_sendChannel = channel;
		return;
	}
	
	public SelectableChannel getSendChannel()
	{
		return m_sendChannel;
	}
	
	public String getFilePath()
	{
		return m_strFilePath;
	}
		
	public void setFileInputStream(FileInputStream fis)
	{
		m_fis = fis;
		return;
	}
	
	public FileInputStream getFileInputStream()
	{
		return m_fis;
	}
	
	public void setSendTaskResult(Future result)
	{
		m_sendTaskResult = result;
		return;
	}
	
	public Future getSendTaskResult()
	{
		return m_sendTaskResult;
	}
	
	public void setStartedToSend(boolean bStarted)
	{
		m_bStarted = bStarted;
		return;
	}
	
	public boolean isStartedToSend()
	{
		return m_bStarted;
	}
	
}
