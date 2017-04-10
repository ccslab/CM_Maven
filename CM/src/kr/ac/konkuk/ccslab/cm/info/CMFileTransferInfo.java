package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMFileRequestInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;

import java.io.*;

public class CMFileTransferInfo {
	private String m_strFilePath;
	private Vector<CMFileRequestInfo> m_requestList;
	private Vector<CMRecvFileInfo> m_recvList;
	
	public CMFileTransferInfo()
	{
		m_strFilePath = null;
		m_requestList = new Vector<CMFileRequestInfo>();
		//m_pushList = new Vector<CMFilePushInfo>();
		m_recvList = new Vector<CMRecvFileInfo>();
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

	public boolean addRecvFileInfo(String fName, long lSize, int nContentID, long lRecvSize, 
			FileOutputStream fos)
	{
		CMRecvFileInfo rInfo = findRecvFileInfo(fName, nContentID);
		if( rInfo != null )
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo(), already exists.");
			System.out.println("file name: "+fName);
			return false;
		}

		rInfo = new CMRecvFileInfo();
		rInfo.setFileName(fName);
		rInfo.setFileSize(lSize);
		rInfo.setContentID(nContentID);
		rInfo.setRecvSize(lRecvSize);
		rInfo.setFileOutputStream(fos);

		m_recvList.addElement(rInfo);
		return true;
	}

	public CMRecvFileInfo findRecvFileInfo(String fName, int nContentID)
	{
		CMRecvFileInfo rInfo = null;
		boolean bFound = false;
		Iterator<CMRecvFileInfo> iterPushList = m_recvList.iterator();

		while(iterPushList.hasNext() && !bFound)
		{
			rInfo = iterPushList.next();
			if(fName.equals(rInfo.getFileName()) && nContentID == rInfo.getContentID())
				bFound = true;
		}

		if(bFound)
			return rInfo;
		return null;
	}

	public boolean removeRecvFileInfo(String fName, int nContentID)
	{
		CMRecvFileInfo rInfo = null;
		boolean bFound = false;		
		Iterator<CMRecvFileInfo> iterRecvList = m_recvList.iterator();
		
		while(iterRecvList.hasNext() && !bFound)
		{
			rInfo = iterRecvList.next();
			if(fName.equals(rInfo.getFileName()) && nContentID == rInfo.getContentID())
			{
				iterRecvList.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public Vector<CMRecvFileInfo> getRecvFileList()
	{
		return m_recvList;
	}
}
