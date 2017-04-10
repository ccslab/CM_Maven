package kr.ac.konkuk.ccslab.cm.entity;

import java.io.FileInputStream;

public class CMSendFileInfo extends CMTransFileInfo {
	private String m_strUserName;	// the name of a sender who is sending the file
	private String m_strFilePath;	// the path of the sent file
	private Thread m_fileSendThread;
	private FileInputStream m_fis;	// the file input stream that is used to read data of the sent file
	private boolean m_bStarted;
	private boolean m_bSentAll;
	private boolean m_bReceivedAck;

	public CMSendFileInfo()
	{
		m_strUserName = null;
		m_strFilePath = null;
		m_fileSendThread = null;
		m_fis = null;
		m_bStarted = false;
		m_bSentAll = false;
		m_bReceivedAck = false;
	}
	
	// set/get method
	
	void setUserName(String strName)
	{
		m_strUserName = strName;
		return;
	}
	
	String getUserName()
	{
		return m_strUserName;
	}
	
	void setFilePath(String strPath)
	{
		m_strFilePath = strPath;
		return;
	}
	
	String getFilePath()
	{
		return m_strFilePath;
	}
	
	void setFileSendThread(Thread thread)
	{
		m_fileSendThread = thread;
		return;
	}
	
	Thread getFileSendThread()
	{
		return m_fileSendThread;
	}
	
	void setFileInputStream(FileInputStream fis)
	{
		m_fis = fis;
		return;
	}
	
	FileInputStream getFileInputStream()
	{
		return m_fis;
	}
	
	void setStartedToSend(boolean bStarted)
	{
		m_bStarted = bStarted;
		return;
	}
	
	boolean isStartedToSend()
	{
		return m_bStarted;
	}
	
	void setSentAll(boolean bSentAll)
	{
		m_bSentAll = bSentAll;
		return;
	}
	
	boolean isSentAll()
	{
		return m_bSentAll;
	}
	
	void setReceivedAck(boolean bAck)
	{
		m_bReceivedAck = bAck;
		return;
	}
	
	boolean isReceivedAck()
	{
		return m_bReceivedAck;
	}
	
}
