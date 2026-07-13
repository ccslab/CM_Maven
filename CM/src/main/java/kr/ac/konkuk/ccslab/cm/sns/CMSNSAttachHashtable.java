package kr.ac.konkuk.ccslab.cm.sns;

import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMObject;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMSNSAttachHashtable extends CMObject {

	// [Modification] Key: CMUserLoginKey (User Name + UUID), Value: CMSNSAttachList
	private Hashtable<CMUserLoginKey, CMSNSAttachList> m_attachHashtable;
	//private Hashtable<String, CMSNSAttachList> m_attachHashtable; // key is the requester name
	
	public CMSNSAttachHashtable()
	{
		//m_attachHashtable = new Hashtable<String, CMSNSAttachList>();
		m_attachHashtable = new Hashtable<>();
	}
	
	public CMSNSAttachList findSNSAttachList(String strUserName, UUID uuid)
	{
		if(strUserName == null) {
			System.err.println("CMSNSAttachHashtable.findSNSAttachList(), the user name is null!");
			return null;
		}

		CMUserLoginKey key = new CMUserLoginKey(strUserName, uuid);
		return m_attachHashtable.get(key);
	}

	public boolean addSNSAttachList(String strUserName, UUID uuid, CMSNSAttachList attachList)
	{
		// check if parameters are null or not
		if(strUserName == null)
		{
			System.err.println("CMSNSAttachHashtable.addSNSAttachList(), the user name is null!");
			return false;
		}
		if(attachList == null)
		{
			System.err.println("CMSNSAttachHashtable.addSNSAttachList(), the attach list is null!");
			return false;
		}

		CMUserLoginKey key = new CMUserLoginKey(strUserName, uuid);

		// check if the key already exists or not
		if(m_attachHashtable.containsKey(key))
		{
			System.err.println("CMSNSAttachHashtable.addSNSAttachList(), the key("+key+") already exists!");
			return false;
		}
		
		m_attachHashtable.put(key, attachList);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMSNSAttachHashtable.addSNSAttachList(), succeeded for key("+key+").");
		}
		
		return true;
	}
	
	public boolean removeSNSAttachList(String strUserName, UUID uuid)
	{
		if(strUserName == null) {
			System.err.println("CMSNSAttachHashtable.removeSNSAttachList(), the user name is null!");
			return false;
		}

		CMUserLoginKey key = new CMUserLoginKey(strUserName, uuid);

		// check if the key already exists or not
		if(!m_attachHashtable.containsKey(key))
		{
			if(CMInfo._CM_DEBUG)
				System.err.println("CMSNSAttachHashtable.removeSNSAttachList(), key("+key+") not found!");
			return false;
		}
		
		//m_attachMap.remove(strUserName);
		m_attachHashtable.remove(key);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMSNSAttachHashtable.removeSNSAttachList(), succeeded for key("+key+").");
		}
		
		return true;
	}
	
	public void removeAllSNSAttachList()
	{
		//m_attachMap.clear();
		m_attachHashtable.clear();
		return;
	}
}
