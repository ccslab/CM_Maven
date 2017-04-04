package kr.ac.konkuk.ccslab.cm.sns;

import java.util.*;

import kr.ac.konkuk.ccslab.cm.CMObject;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMSNSAttachHashMap extends CMObject {
	private HashMap<String, CMSNSAttachList> m_attachMap;
	
	public CMSNSAttachHashMap()
	{
		m_attachMap = new HashMap<String, CMSNSAttachList>();
	}
	
	public CMSNSAttachList findSNSAttachList(String strUserName)
	{
		CMSNSAttachList attachList = m_attachMap.get(strUserName);
		return attachList;
	}
	
	public boolean addSNSAttachList(String strUserName, CMSNSAttachList attachList)
	{
		// check if parameters are null or not
		if(strUserName == null)
		{
			System.err.println("CMSNSAttachHashMap.addSNSAttachList(), the user name is null!");
			return false;
		}
		if(attachList == null)
		{
			System.err.println("CMSNSAttachHashMap.addSNSAttachList(), the attach list is null!");
			return false;
		}
		
		// check if the key already exists or not
		if(m_attachMap.containsKey(strUserName))
		{
			System.err.println("CMSNSAttachHashMap.addSNSAttachList(), the key("+strUserName+") already exists!");
			return false;
		}
		
		m_attachMap.put(strUserName, attachList);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMSNSAttachHashMap.addSNSAttachList(), succeeded for user("+strUserName+").");
		}
		
		return true;
	}
	
	public boolean removeSNSAttachList(String strUserName)
	{
		//CMSNSAttachList attachList = null;
		
		if(!m_attachMap.containsKey(strUserName))
		{
			System.err.println("CMSNSAttachHashMap.removeSNSAttachList(), key("+strUserName+") not found!");
			return false;
		}
		
		//attachList = m_attachMap.get(strUserName);
		m_attachMap.remove(strUserName);
		//attachList = null;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMSNSAttachHashMap.removeSNSAttachList(), succeeded for key("+strUserName+").");
		}
		
		return true;
	}
	
	public void removeAllSNSAttachList()
	{
		/*
		CMSNSAttachList attachList = null;
		Iterator<Entry<String, CMSNSAttachList>> iter = m_attachMap.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry<String, CMSNSAttachList> e = (Map.Entry<String, CMSNSAttachList>) iter.next();
			attachList = e.getValue();
			attachList = null;
		}
		*/
		m_attachMap.clear();
		return;
	}
}
