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
}
