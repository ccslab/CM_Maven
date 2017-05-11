package kr.ac.konkuk.ccslab.cm.entity;

import java.io.FileOutputStream;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.Future;

public class CMRecvFileInfo extends CMTransFileInfo {
	private long m_lRecvSize;
	private FileOutputStream m_fos;
	private SelectableChannel m_recvChannel;	// the dedicated channel for receiving the file
	private Future m_recvTaskResult;	// the result of the submitted receiving task to the thread pool 

	public CMRecvFileInfo()
	{
		super();
		m_lRecvSize = -1;
		m_fos = null;
		m_recvChannel = null;
		m_recvTaskResult = null;
	}
	
	public CMRecvFileInfo(String strFile, long lSize)
	{
		super(strFile, lSize, -1);
		m_lRecvSize = -1;
		m_fos = null;
		m_recvChannel = null;
		m_recvTaskResult = null;
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
	
	public void setFileOutputStream(FileOutputStream fos)
	{
		m_fos = fos;
		return;
	}
	
	public FileOutputStream getFileOutputStream()
	{
		return m_fos;
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
