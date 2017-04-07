package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMFilePushInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMFileRequestInfo;

import java.io.*;

public class CMFileTransferInfo {
	private String m_strFilePath;
	private Vector<CMFileRequestInfo> m_requestList;
	private Vector<CMFilePushInfo> m_pushList;
	
	public CMFileTransferInfo()
	{
		m_strFilePath = null;
		m_requestList = new Vector<CMFileRequestInfo>();
		m_pushList = new Vector<CMFilePushInfo>();
	}
	
	// set/get methods
	public void setFilePath(String path)
	{
		m_strFilePath = path;
	}
	
	public String getFilePath()
	{
		return m_strFilePath;
	}
	
	// add/remove/find request info
	
	public boolean addFileRequestInfo(String uName, String fPath, long lSize, int nContentID, Thread fThread)
	{
		CMFileRequestInfo rInfo = findFileRequestInfo(uName, fPath, nContentID);

		if( rInfo != null )
		{
			System.out.println("CMFileTransferInfo.addFileRequestInfo(), already exists.");
			System.out.println("user name: "+uName+", file path: "+fPath+", content ID: "+nContentID);
			return false;
		}
		
		rInfo = new CMFileRequestInfo();
		rInfo.m_strUserName = uName;
		rInfo.m_strFilePath = fPath;
		rInfo.m_lFileSize = lSize;
		rInfo.m_nContentID = nContentID;
		rInfo.m_fileThread = fThread;
		
		m_requestList.addElement(rInfo);

		return true;
	}

	public CMFileRequestInfo findFileRequestInfo(String uName, String fName, int nContentID)
	{
		CMFileRequestInfo rInfo = null;
		boolean bFound = false;
		Iterator<CMFileRequestInfo> iterRequestList = m_requestList.iterator();
		
		while(iterRequestList.hasNext() && !bFound)
		{
			rInfo = iterRequestList.next();
			//if(uName.equals(rInfo.m_strUserName) && fName.equals(rInfo.m_strFileName))
			if(uName.equals(rInfo.m_strUserName) && rInfo.m_strFilePath.endsWith(fName) && 
					nContentID == rInfo.m_nContentID)	// not sure
				bFound = true;
		}

		if(bFound)
			return rInfo;
		return null;
	}

	public boolean removeFileRequestInfo(String uName, String fName, int nContentID)
	{
		CMFileRequestInfo rInfo = null;
		boolean bFound = false;
		Iterator<CMFileRequestInfo> iterRequestList = m_requestList.iterator();
		
		while(iterRequestList.hasNext() && !bFound)
		{
			rInfo = iterRequestList.next();
			//if(uName.equals(rInfo.m_strUserName) && fName.equals(rInfo.m_strFileName))
			if(uName.equals(rInfo.m_strUserName) && rInfo.m_strFilePath.endsWith(fName) &&
					nContentID == rInfo.m_nContentID)	// not sure
			{
				iterRequestList.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public Vector<CMFileRequestInfo> getFileRequestList()
	{
		return m_requestList;
	}

	// add/remove/find file push info

	public boolean addFilePushInfo(String fName, long lSize, int nContentID, long lRecvSize, 
			FileOutputStream fos)
	{
		CMFilePushInfo pInfo = findFilePushInfo(fName, nContentID);
		if( pInfo != null )
		{
			System.out.println("CMFileTransferInfo.addFilePushInfo(), already exists.");
			System.out.println("file name: "+fName);
			return false;
		}

		pInfo = new CMFilePushInfo();
		pInfo.m_strFileName = fName;
		pInfo.m_lFileSize = lSize;
		pInfo.m_nContentID = nContentID;
		pInfo.m_lRecvSize = lRecvSize;
		pInfo.m_fos = fos;

		m_pushList.addElement(pInfo);
		return true;
	}

	public CMFilePushInfo findFilePushInfo(String fName, int nContentID)
	{
		CMFilePushInfo pInfo = null;
		boolean bFound = false;
		Iterator<CMFilePushInfo> iterPushList = m_pushList.iterator();

		while(iterPushList.hasNext() && !bFound)
		{
			pInfo = iterPushList.next();
			if(fName.equals(pInfo.m_strFileName) && nContentID == pInfo.m_nContentID)
				bFound = true;
		}

		if(bFound)
			return pInfo;
		return null;
	}

	public boolean removeFilePushInfo(String fName, int nContentID)
	{
		CMFilePushInfo pInfo = null;
		boolean bFound = false;
		Iterator<CMFilePushInfo> iterPushList = m_pushList.iterator();
		
		while(iterPushList.hasNext() && !bFound)
		{
			pInfo = iterPushList.next();
			if(fName.equals(pInfo.m_strFileName) && nContentID == pInfo.m_nContentID)
			{
				iterPushList.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public Vector<CMFilePushInfo> getFilePushList()
	{
		return m_pushList;
	}
}
