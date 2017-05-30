package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttach;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttachHashtable;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttachList;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContentList;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSPrefetchHashMap;

public class CMSNSInfo {
	private CMSNSContentList m_contentList;	// downloaded content for client, global content list for server
											// if DB is not used
	private CMSNSAttach m_sendAttach;	// by client for content upload
	private CMSNSAttachHashtable m_recvAttachHashtable;	// by server for content upload
	private CMSNSAttachHashtable m_sendAttachHashtable;	// by server for content download
	private CMSNSAttachList m_recvAttachList;	// by client for content download	
	private CMSNSPrefetchHashMap m_prefetchMap;		// by server for prefetching attached files
	
	private String m_strLastlyReqWriter;	// by client for saving the information on the last content download request
	private int m_nLastlyReqOffset;			// by client for saving the information on the last content download request
	private int m_nLastlyDownContentNum;	// by client for saving the information on the last content download request
	
	public CMSNSInfo()
	{
		m_contentList = new CMSNSContentList();
		m_sendAttach = new CMSNSAttach();
		m_recvAttachHashtable = new CMSNSAttachHashtable();
		m_sendAttachHashtable = new CMSNSAttachHashtable();
		m_recvAttachList = new CMSNSAttachList();
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
	
	public void setSendSNSAttach(CMSNSAttach attach)
	{
		m_sendAttach = attach;
	}
	
	public CMSNSAttach getSendSNSAttach()
	{
		return m_sendAttach;
	}
	
	public void setRecvSNSAttachHashtable(CMSNSAttachHashtable attachTable)
	{
		m_recvAttachHashtable = attachTable;
	}
	
	public CMSNSAttachHashtable getRecvSNSAttachHashtable()
	{
		return m_recvAttachHashtable;
	}
	
	public void setSendSNSAttachHashtable(CMSNSAttachHashtable attachTable)
	{
		m_sendAttachHashtable = attachTable;
	}
	
	public CMSNSAttachHashtable getSendSNSAttachHashtable()
	{
		return m_sendAttachHashtable;
	}
	
	public void setRecvSNSAttachList(CMSNSAttachList attachList)
	{
		m_recvAttachList = attachList;
	}
	
	public CMSNSAttachList getRecvSNSAttachList()
	{
		return m_recvAttachList;
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
