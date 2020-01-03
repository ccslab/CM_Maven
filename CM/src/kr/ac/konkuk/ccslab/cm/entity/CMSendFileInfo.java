package kr.ac.konkuk.ccslab.cm.entity;

import java.io.RandomAccessFile;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Future;

public class CMSendFileInfo extends CMTransFileInfo {
	private long m_lSentSize;	// size already sent to the receiver
	private SocketChannel m_sendChannel; // the dedicated channel for sending the file
	private SocketChannel m_defaultChannel; // the default channel for sending the control event
	private RandomAccessFile m_readFile;// for reading file blocks of the sent file
	private Future<CMSendFileInfo> m_sendTaskResult;	// the result of the submitted sending task to the thread pool
	
	public CMSendFileInfo()
	{
		super();
		m_lSentSize = 0;
		m_sendChannel = null;
		m_defaultChannel = null;
		m_readFile = null;
		m_sendTaskResult = null;
	}
	
	public CMSendFileInfo(String strFile, long lSize)
	{
		super(strFile, lSize, -1);
		m_lSentSize = 0;
		m_sendChannel = null;
		m_defaultChannel = null;
		m_readFile = null;
		m_sendTaskResult = null;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!super.equals(o)) return false;
		
		CMSendFileInfo sfInfo = (CMSendFileInfo) o;
		String strReceiverName = sfInfo.getFileReceiver();
		
		if(strReceiverName.equals(m_strFileReceiver))
			return true;
		return false;
	}
	
	@Override
	public String toString()
	{
		String strInfo = super.toString();
		strInfo += "; CMSendFileInfo: receiver("+m_strFileReceiver+")";
		return strInfo;
	}
	
	// set/get method
		
	public synchronized void setSentSize(long lSize)
	{
		m_lSentSize = lSize;
		return;
	}
	
	public synchronized long getSentSize()
	{
		return m_lSentSize;
	}

	public synchronized void setSendChannel(SocketChannel channel)
	{
		m_sendChannel = channel;
		return;
	}
	
	public synchronized SocketChannel getSendChannel()
	{
		return m_sendChannel;
	}
	
	public synchronized void setDefaultChannel(SocketChannel channel)
	{
		m_defaultChannel = channel;
		return;
	}
	
	public synchronized SocketChannel getDefaultChannel()
	{
		return m_defaultChannel;
	}

	public synchronized void setReadFile(RandomAccessFile raf)
	{
		m_readFile = raf;
		return;
	}
	
	public synchronized RandomAccessFile getReadFile()
	{
		return m_readFile;
	}
	
	public synchronized void setSendTaskResult(Future<CMSendFileInfo> result)
	{
		m_sendTaskResult = result;
		return;
	}
	
	public synchronized Future<CMSendFileInfo> getSendTaskResult()
	{
		return m_sendTaskResult;
	}
	
}
