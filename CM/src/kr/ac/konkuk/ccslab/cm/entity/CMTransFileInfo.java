package kr.ac.konkuk.ccslab.cm.entity;

public class CMTransFileInfo {
	private String m_strFileName; // the name of the transferred file
	private long m_lFileSize;	  // the size of the transferred file
	private int m_nContentID;	  // the identifier of content to which the transferred file belongs
	
	public CMTransFileInfo()
	{
		m_strFileName = null;
		m_lFileSize = -1;
		m_nContentID = -1;
	}
	
	// get/set methods
	
	void setFileName(String strName)
	{
		m_strFileName = strName;
		return;
	}
	
	String getFileName()
	{
		return m_strFileName;
	}
	
	void setFileSize(long lSize)
	{
		m_lFileSize = lSize;
		return;
	}
	
	long getFileSize()
	{
		return m_lFileSize;
	}
	
	void setContentID(int nID)
	{
		m_nContentID = nID;
		return;
	}
	
	int getContentID()
	{
		return m_nContentID;
	}
}
