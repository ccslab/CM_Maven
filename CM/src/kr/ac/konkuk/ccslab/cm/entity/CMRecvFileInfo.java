package kr.ac.konkuk.ccslab.cm.entity;

import java.io.RandomAccessFile;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.Future;

public class CMRecvFileInfo extends CMTransFileInfo {
	private long m_lRecvSize;
	private RandomAccessFile m_writeFile;		// for writing the received file block to the new file
	private SelectableChannel m_recvChannel;	// the dedicated channel for receiving the file
	private Future m_recvTaskResult;	// the result of the submitted receiving task to the thread pool 

	public CMRecvFileInfo()
	{
		super();
		m_lRecvSize = -1;
		m_writeFile = null;
		m_recvChannel = null;
		m_recvTaskResult = null;
	}
	
	public CMRecvFileInfo(String strFile, long lSize)
	{
		super(strFile, lSize, -1);
		m_lRecvSize = -1;
		m_writeFile = null;
		m_recvChannel = null;
		m_recvTaskResult = null;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!super.equals(o)) return false;
		
		CMRecvFileInfo rfInfo = (CMRecvFileInfo) o;
		String strSenderName = rfInfo.getSenderName();
		
		if(strSenderName.equals(m_strSenderName))
			return true;
		return false;
	}

	@Override
	public String toString()
	{
		String str = super.toString();
		str += "; CMRecvFileInfo: sender("+m_strSenderName+")";
		return str;
	}
	
	// set/get methods
	
	public void setRecvSize(long lSize)
	{
		m_lRecvSize = lSize;
		return;
	}
	
	public long getRecvSize()
	{
		return m_lRecvSize;
	}
	
	public void setWriteFile(RandomAccessFile acf)
	{
		m_writeFile = acf;
	}
	
	public RandomAccessFile getWriteFile()
	{
		return m_writeFile;
	}
	
	public void setRecvChannel(SelectableChannel channel)
	{
		m_recvChannel = channel;
		return;
	}
	
	public SelectableChannel getRecvChannel()
	{
		return m_recvChannel;
	}
	
	public void setRecvTaskResult(Future result)
	{
		m_recvTaskResult = result;
		return;
	}
	
	public Future getRecvTaskResult()
	{
		return m_recvTaskResult;
	}
	
}
