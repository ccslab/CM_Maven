package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;

import java.io.*;

public class CMFileTransferInfo {
	private String m_strFilePath;
	private Vector<CMSendFileInfo> m_sendList;
	private Vector<CMRecvFileInfo> m_recvList;
	
	// from here (linked list? hash map? for sending/receiving file info)
	
	public CMFileTransferInfo()
	{
		m_strFilePath = null;
		m_sendList = new Vector<CMSendFileInfo>();
		m_recvList = new Vector<CMRecvFileInfo>();
	}
	
	// set/get methods
	
	// need to use File.separator in order to adapt to different OSs ('/' or '\\') not yet
	public void setFilePath(String path)
	{
		m_strFilePath = path;
	}
	
	public String getFilePath()
	{
		return m_strFilePath;
	}
	
	// add/remove/find request info
	
	public boolean addSendFileInfo(String uName, String fPath, long lSize, int nContentID)
	{
		/*
		CMSendFileInfo sInfo = findSendFileInfo(uName, fPath, nContentID);

		if( sInfo != null )
		{
			System.err.println("CMFileTransferInfo.addSendFileInfo(), already exists.");
			System.err.println("receiver name: "+uName+", file path: "+fPath+", content ID: "+nContentID);
			return false;
		}
		*/
		CMSendFileInfo sInfo = null;
		String strFileName = fPath.substring(fPath.lastIndexOf("/")+1);
		
		sInfo = new CMSendFileInfo();
		sInfo.setReceiverName(uName);
		sInfo.setFileName(strFileName);
		sInfo.setFilePath(fPath);
		sInfo.setFileSize(lSize);
		sInfo.setContentID(nContentID);
		
		if(m_sendList.contains(sInfo))
		{
			System.err.println("CMFileTransferInfo.addSendFileInfo(), already exists.");
			System.err.println("receiver name: "+uName+", file path: "+fPath+", content ID: "+nContentID);
			return false;			
		}
		
		m_sendList.addElement(sInfo);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addSendFileInfo() done, for "+
					"receiver name: "+uName+", file path: "+fPath+", content ID: "+nContentID);
			System.out.println("# current element: "+m_sendList.size());
		}

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
			/*
			if(uName.equals(sInfo.getReceiverName()) && sInfo.getFilePath().endsWith(fName) && 
					nContentID == sInfo.getContentID())	// not sure
			*/
			if(uName.equals(sInfo.getReceiverName()) && fName.equals(sInfo.getFileName()) && 
					nContentID == sInfo.getContentID())
				bFound = true;
		}

		if(bFound)
			return sInfo;
		return null;
	}

	public boolean removeSendFileInfo(String uName, String fName, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		boolean bFound = false;
		/*
		Iterator<CMSendFileInfo> iterSendList = m_sendList.iterator();
		
		while(iterSendList.hasNext() && !bFound)
		{
			sInfo = iterSendList.next();
			//if(uName.equals(rInfo.m_strUserName) && fName.equals(rInfo.m_strFileName))
			if(uName.equals(sInfo.getReceiverName()) && sInfo.getFilePath().endsWith(fName) &&
					nContentID == sInfo.getContentID())	// not sure
			{
				iterSendList.remove();
				bFound = true;
			}
		}
		*/
		
		sInfo = new CMSendFileInfo();
		sInfo.setReceiverName(uName);
		sInfo.setFileName(fName);
		sInfo.setContentID(nContentID);
		bFound = m_sendList.removeElement(sInfo);
		
		if(!bFound)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo() error! for receiver("+uName
					+"), file("+fName+"), contentID("+nContentID+")");
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done, for "+
					"receiver name: "+uName+", file name: "+fName+", content ID: "+nContentID);
			System.out.println("# current element: "+m_sendList.size());
		}
		
		return bFound;
	}
	
	public Vector<CMSendFileInfo> getSendFileList()
	{
		return m_sendList;
	}

	// add/remove/find recv file info

	public boolean addRecvFileInfo(String senderName, String fName, long lSize, int nContentID, 
			long lRecvSize,	FileOutputStream fos)
	{
		/*
		CMRecvFileInfo rInfo = findRecvFileInfo(fName, nContentID);
		if( rInfo != null )
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo(), already exists.");
			System.err.println("file name: "+fName+", content ID: "+nContentID);
			return false;
		}
		*/

		CMRecvFileInfo rInfo = null;
		rInfo = new CMRecvFileInfo();
		rInfo.setSenderName(senderName);
		rInfo.setFileName(fName);
		rInfo.setFileSize(lSize);
		rInfo.setContentID(nContentID);
		rInfo.setRecvSize(lRecvSize);
		rInfo.setFileOutputStream(fos);
		
		if(m_recvList.contains(rInfo))
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo(), already exists.");
			System.err.println("sender: "+senderName+", file name: "+fName+", content ID: "+nContentID);
			return false;
		}

		m_recvList.addElement(rInfo);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo() done, for "+
					"sender name: "+senderName+", file name: "+fName+", content ID: "+nContentID);
			System.out.println("# current element: "+m_recvList.size());
		}
		
		return true;
	}

	public CMRecvFileInfo findRecvFileInfo(String senderName, String fName, int nContentID)
	{
		CMRecvFileInfo rInfo = null;
		boolean bFound = false;
		Iterator<CMRecvFileInfo> iterPushList = m_recvList.iterator();

		while(iterPushList.hasNext() && !bFound)
		{
			rInfo = iterPushList.next();
			if(senderName.equals(rInfo.getSenderName()) && fName.equals(rInfo.getFileName()) && 
					nContentID == rInfo.getContentID())
				bFound = true;
		}

		if(bFound)
			return rInfo;
		return null;
	}

	public boolean removeRecvFileInfo(String senderName, String fName, int nContentID)
	{
		CMRecvFileInfo rInfo = null;
		boolean bFound = false;

		/*
		Iterator<CMRecvFileInfo> iterRecvList = m_recvList.iterator();
		while(iterRecvList.hasNext() && !bFound)
		{
			rInfo = iterRecvList.next();
			if(senderName.equals(rInfo.getSenderName()) && fName.equals(rInfo.getFileName()) 
					&& nContentID == rInfo.getContentID())
			{
				iterRecvList.remove();
				bFound = true;
			}
		}
		*/
		
		rInfo = new CMRecvFileInfo();
		rInfo.setSenderName(senderName);
		rInfo.setFileName(fName);
		rInfo.setContentID(nContentID);
		bFound = m_recvList.remove(rInfo);

		if(!bFound)
		{
			System.err.println("CMFileTransferInfo.removeRecvFileInfo() error! for sender("+senderName
					+"), file("+fName+"), contentID("+nContentID+")");
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeRecvFileInfo() done, for "+
					"sender name: "+senderName+", file name: "+fName+", content ID: "+nContentID);
			System.out.println("# current element: "+m_recvList.size());
		}

		return bFound;
	}
	
	public Vector<CMRecvFileInfo> getRecvFileList()
	{
		return m_recvList;
	}
}
