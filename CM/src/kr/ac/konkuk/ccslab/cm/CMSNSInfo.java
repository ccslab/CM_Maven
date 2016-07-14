package kr.ac.konkuk.ccslab.cm;

public class CMSNSInfo {
	private CMSNSContentList m_contentList;	// downloaded content for client, global content list for server
											// if DB is not used
	private CMSNSAttach m_attachToBeSent;	// by client for content upload
	private CMSNSAttachHashMap m_attachMapToBeRecv;	// by server for content upload
	private CMSNSAttachHashMap m_attachMapToBeSent;	// by server for content download
	private CMSNSAttachList m_attachListToBeRecv;	// by client for content download	
	private CMSNSPrefetchHashMap m_prefetchMap;		// by server for prefetching attached files
	
	private String m_strLastlyReqWriter;	// by client for saving the information on the last content download request
	private int m_nLastlyReqOffset;			// by client for saving the information on the last content download request
	private int m_nLastlyDownContentNum;	// by client for saving the information on the last content download request
	
	public CMSNSInfo()
	{
		m_contentList = new CMSNSContentList();
		m_attachToBeSent = new CMSNSAttach();
		m_attachMapToBeRecv = new CMSNSAttachHashMap();
		m_attachMapToBeSent = new CMSNSAttachHashMap();
		m_attachListToBeRecv = new CMSNSAttachList();
		m_prefetchMap = new CMSNSPrefetchHashMap();
		m_strLastlyReqWriter = ""; // initial (or default) writer is an empty string
		m_nLastlyReqOffset = 0;		// initial (or default) offset is 0 (the most recent content)
		m_nLastlyDownContentNum = 0;
	}
	
	/////////////////////////////////////////////////////////////
	// get/set methods
	
	public void setSNSContentList(CMSNSContentList contList)
	{
		m_contentList = contList;
	}
	
	public CMSNSContentList getSNSContentList()
	{
		return m_contentList;
	}
	
	public void setSNSAttachToBeSent(CMSNSAttach attach)
	{
		m_attachToBeSent = attach;
	}
	
	public CMSNSAttach getSNSAttachToBeSent()
	{
		return m_attachToBeSent;
	}
	
	public void setSNSAttachMapToBeRecv(CMSNSAttachHashMap attachMap)
	{
		m_attachMapToBeRecv = attachMap;
	}
	
	public CMSNSAttachHashMap getSNSAttachMapToBeRecv()
	{
		return m_attachMapToBeRecv;
	}
	
	public void setSNSAttachMapToBeSent(CMSNSAttachHashMap attachMap)
	{
		m_attachMapToBeSent = attachMap;
	}
	
	public CMSNSAttachHashMap getSNSAttachMapToBeSent()
	{
		return m_attachMapToBeSent;
	}
	
	public void setSNSAttachListToBeRecv(CMSNSAttachList attachList)
	{
		m_attachListToBeRecv = attachList;
	}
	
	public CMSNSAttachList getSNSAttachListToBeRecv()
	{
		return m_attachListToBeRecv;
	}
	
	public void setPrefetchMap(CMSNSPrefetchHashMap prefetchMap)
	{
		m_prefetchMap = prefetchMap;
	}
	
	public CMSNSPrefetchHashMap getPrefetchMap()
	{
		return m_prefetchMap;
	}
	
	public void setLastlyReqWriter(String strWriter)
	{
		m_strLastlyReqWriter = strWriter;
	}
	
	public String getLastlyReqWriter()
	{
		return m_strLastlyReqWriter;
	}
	
	public void setLastlyReqOffset(int nOffset)
	{
		m_nLastlyReqOffset = nOffset;
	}
	
	public int getLastlyReqOffset()
	{
		return m_nLastlyReqOffset;
	}
	
	public void setLastlyDownContentNum(int nNum)
	{
		m_nLastlyDownContentNum = nNum;
	}
	
	public int getLastlyDownContentNum()
	{
		return m_nLastlyDownContentNum;
	}
}
