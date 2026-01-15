package kr.ac.konkuk.ccslab.cm.sns;

import java.util.HashMap;
import java.util.UUID;

import kr.ac.konkuk.ccslab.cm.entity.CMObject;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMSNSPrefetchHashMap extends CMObject {
	private HashMap<CMUserLoginKey, CMSNSPrefetchList> m_prefetchMap;
	
	public CMSNSPrefetchHashMap()
	{
		m_prefetchMap = new HashMap<>();
	}

	public CMSNSPrefetchList findPrefetchList(String strUserName, UUID uuid)
	{
		if(strUserName == null) {
			System.err.println("CMSNSPrefetchHashMap.findPrefetchList(), the user name is null!");
			return null;
		}

		CMUserLoginKey key = new CMUserLoginKey(strUserName, uuid);
		return m_prefetchMap.get(key);
	}

	public boolean addPrefetchList(String strUserName, UUID uuid, CMSNSPrefetchList prefetchList)
	{
		// check if parameters are null or not
		if(strUserName == null)
		{
			System.err.println("CMSNSPrefetchHashMap.addPrefetchList(), user is null!");
			return false;
		}
		
		if(prefetchList == null)
		{
			System.err.println("CMSNSPrefetchHashMap.addPrefetchList(), prefetch list is null!");
			return false;
		}

		CMUserLoginKey key = new CMUserLoginKey(strUserName, uuid);

		// check if the key already exists or not
		if(m_prefetchMap.containsKey(key))
		{
			System.err.println("CMSNSPrefetchHashMap.addPrefetchList(), key("+strUserName+"), uuid("
					+uuid+") already exists!");
			return false;
		}
		
		m_prefetchMap.put(key, prefetchList);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMSNSPrefetchHashMap.addPrefetchList(), succeeded for user("+strUserName
					+"), uuid("+uuid+").");
		}
		return true;
	}
	
	public boolean removePrefetchList(String strUserName, UUID uuid)
	{
		if(strUserName == null) {
			System.err.println("CMSNSPrefetchHashMap.removePrefetchList(), the user name is null!");
			return false;
		}
		CMUserLoginKey key = new CMUserLoginKey(strUserName, uuid);

		if(!m_prefetchMap.containsKey(key))
		{
			if(CMInfo._CM_DEBUG)
				System.err.println("CMSNSPrefetchHashMap.removePrefetchList(), key("+key+") not found!");
			return false;
		}
		m_prefetchMap.remove(key);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMSNSPrefetchHashMap.removePrefetchList(), succeeded for key("+key+").");
		}
		return true;
	}
	
	public void removeAllPrefetchList()
	{
		m_prefetchMap.clear();
	}

}
