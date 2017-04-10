package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;

import java.io.*;

public class CMFileTransferInfo {
	private String m_strFilePath;
	private Vector<CMSendFileInfo> m_sendList;
	private Vector<CMRecvFileInfo> m_recvList;
	
	public CMFileTransferInfo()
	{
		m_strFilePath = null;
		m_sendList = new Vector<CMSendFileInfo>();
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
	
	public boolean addSendFileInfo(String uName, String fPath, long lSize, int nContentID, Thread fThread)
	{
		CMSendFileInfo sInfo = findSendFileInfo(uName, fPath, nContentID);

		if( sInfo != null )
		{
			System.out.println("CMFileTransferInfo.addSendFileInfo(), already exists.");
			System.out.println("requester name: "+uName+", file path: "+fPath+", content ID: "+nContentID);
			return false;
		}
		
		sInfo = new CMSendFileInfo();
		sInfo.setRequesterName(uName);
		sInfo.setFilePath(fPath);
		sInfo.setFileSize(lSize);
		sInfo.setContentID(nContentID);
		sInfo.setFileSendThread(fThread);
		
		m_sendList.addElement(sInfo);

		return true;
	}

	public CMSendFileInfo findSendFileInfo(String uName, String fName, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		boolean bFound = false;
		Iterator<CMSendFileInfo> iterSendList = m_sendList.iterator();
		
		while(iterSendList.hasNext() && !bFound)
		{
			sInfo = iterSendList.next();
			//if(uName.equals(rInfo.m_strUserName) && fName.equals(rInfo.m_strFileName))
			if(uName.equals(sInfo.getRequesterName()) && sInfo.getFilePath().endsWith(fName) && 
					nContentID == sInfo.getContentID())	// not sure
				bFound = true;
		}

		if(bFound)
			return sInfo;
		return null;
	}

	public boolean removeFileRequestInfo(String uName, String fName, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		boolean bFound = false;
		Iterator<CMSendFileInfo> iterSendList = m_sendList.iterator();
		
		while(iterSendList.hasNext() && !bFound)
		{
			sInfo = iterSendList.next();
			//if(uName.equals(rInfo.m_strUserName) && fName.equals(rInfo.m_strFileName))
			if(uName.equals(sInfo.getRequesterName()) && sInfo.getFilePath().endsWith(fName) &&
					nContentID == sInfo.getContentID())	// not sure
			{
				iterSendList.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public Vector<CMSendFileInfo> getSendFileList()
	{
		return m_sendList;
	}

	// add/remove/find recv file info

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
