package kr.ac.konkuk.ccslab.cm.sns;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMObject;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMSNSContentList extends CMObject {
	private Vector<CMSNSContent> m_contentList;
	
	public CMSNSContentList()
	{
		m_nType = CMInfo.CM_SNS_CONTENT_LIST;
		m_contentList = new Vector<CMSNSContent>();
	}
	
	// add content at the end of the list
	public boolean addSNSContent(CMSNSContent cont)
	{
		if( findSNSContent(cont.getContentID()) != null )
		{
			System.out.println("CMSNSContentList.addSNSContent(), content("+cont.getContentID()+") already exists.");
			return false;
		}
		
		m_contentList.addElement(cont);
		if( CMInfo._CM_DEBUG )
		{
			System.out.println("CMSNSContentList.addSNSContent(), Ok id="+cont.getContentID());
		}
		
		return true;

	}
	
	// add content at the end of the list
	public boolean addSNSContent(int id, String date, String writer, String msg, int nAttachment, 
			int replyID, int lod, ArrayList<String> fPathList)
	{
		if( findSNSContent(id) != null )
		{
			System.out.println("CMSNSContentList.addSNSContent(), content("+id+") already exists.");
			return false;
		}
		
		CMSNSContent cont = new CMSNSContent(id, date, writer, msg, nAttachment, replyID, lod);
		if(fPathList != null)
			cont.setFilePathList(fPathList);
		m_contentList.addElement(cont);
		if( CMInfo._CM_DEBUG )
		{
			System.out.println("CMSNSContentList.addSNSContent(), Ok id="+cont.getContentID());
		}
		
		return true;
	}
	
	public boolean removeSNSContent(int id)
	{
		Iterator<CMSNSContent> iter = m_contentList.iterator();
		
		while(iter.hasNext())
		{
			CMSNSContent sc = iter.next();
			if( id == sc.getContentID() )
			{
				iter.remove();
				if( CMInfo._CM_DEBUG )
				{
					System.out.println("CMSNSContentList.removeSNSContent(), Ok id="+id);
				}
				return true;
			}
		}
		
		if( CMInfo._CM_DEBUG )
		{
			System.out.println("CMSNSContentList.removeSNSContent(), content("+id+") not found.");
		}
		return false;
	}
	
	public boolean removeAllSNSContents()
	{
		if( m_contentList.isEmpty() )
		{
			if( CMInfo._CM_DEBUG )
			{
				System.out.println("CMSNSContentList.removeAllSNSContents(), already empty.");
			}
			return false;
		}
		
		m_contentList.removeAllElements();
		
		if( CMInfo._CM_DEBUG )
		{
			System.out.println("CMSNSContentList.removeAllContents(), Ok.");
		}
		
		return true;
	}
	
	public CMSNSContent findSNSContent(int id)
	{
		Iterator<CMSNSContent> iter = m_contentList.iterator();
		CMSNSContent cont;
		
		while(iter.hasNext())
		{
			cont = iter.next();
			if( cont.getContentID() == id )
				return cont;
		}
		
		return null;
	}
	
	// get number of contents in this list
	public int getSNSContentNum()
	{
		return m_contentList.size();
	}
	
	public Vector<CMSNSContent> getContentList()
	{
		return m_contentList;
	}
}
