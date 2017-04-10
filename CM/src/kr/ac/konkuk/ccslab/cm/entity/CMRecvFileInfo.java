package kr.ac.konkuk.ccslab.cm.entity;

import java.io.FileOutputStream;

public class CMRecvFileInfo extends CMTransFileInfo {
	private long m_lRecvSize;
	private FileOutputStream m_fos;

	public CMRecvFileInfo()
	{
		m_lRecvSize = -1;
		m_fos = null;
	}
	
	// set/get methods
	
	void setRecvSize(long lSize)
	{
		m_lRecvSize = lSize;
		return;
	}
	
	long getRecvSize()
	{
		return m_lRecvSize;
	}
	
	void setFileOutputStream(FileOutputStream fos)
	{
		m_fos = fos;
		return;
	}
	
	FileOutputStream getFileOutputStream()
	{
		return m_fos;
	}
}
