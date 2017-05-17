package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;

import java.io.*;

public class CMFileTransferInfo {
	private String m_strFilePath;
	private Hashtable<String, CMList<CMSendFileInfo>> m_sendFileHashtable; // key is the receiver name
	private Hashtable<String, CMList<CMRecvFileInfo>> m_recvFileHashtable; // key is the sender name
	
	public CMFileTransferInfo()
	{
		m_strFilePath = null;
		m_sendFileHashtable = new Hashtable<String, CMList<CMSendFileInfo>>();
		m_recvFileHashtable = new Hashtable<String, CMList<CMRecvFileInfo>>();
	}
	
	////////// set/get methods
	
	public void setFilePath(String path)
	{
		m_strFilePath = path;
	}
	
	public String getFilePath()
	{
		return m_strFilePath;
	}
	
	////////// add/remove/find sending file info
	
	public boolean addSendFileInfo(String uName, String fPath, long lSize, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		String strFileName = null;
		CMList<CMSendFileInfo> sInfoList = null;
		boolean bResult = false;
		
		strFileName = fPath.substring(fPath.lastIndexOf(File.separator)+1);
		sInfo = new CMSendFileInfo();
		sInfo.setReceiverName(uName);
		sInfo.setFileName(strFileName);
		sInfo.setFilePath(fPath);
		sInfo.setFileSize(lSize);
		sInfo.setContentID(nContentID);
		
		sInfoList = m_sendFileHashtable.get(uName);
		if(sInfoList == null)
		{
			sInfoList = new CMList<CMSendFileInfo>();
			m_sendFileHashtable.put(uName, sInfoList);
		}
		
		bResult = sInfoList.addElement(sInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addSendFileInfo() failed: "+sInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addSendFileInfo() done: "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}

		return true;
	}

	public CMSendFileInfo findSendFileInfo(String uName, String fName, int nContentID)
	{
		CMSendFileInfo sInfo = null;
		CMList<CMSendFileInfo> sInfoList = null;
		CMSendFileInfo tInfo = null;
		
		sInfoList = m_sendFileHashtable.get(uName);
		if(sInfoList == null)
		{
			System.err.println("CMFileTransferInfo.findSendFileInfo(), list not found for receiver("
					+uName+")");
			return null;
		}
		
		tInfo = new CMSendFileInfo();
		tInfo.setReceiverName(uName);
		tInfo.setFileName(fName);
		tInfo.setContentID(nContentID);
		
		sInfo = sInfoList.findElement(tInfo);
		
		if(sInfo == null)
		{
			System.err.println("CMFileTransferInfo.findSendFileInfo(), not found!: "+tInfo.toString());
			return null;
		}
		
		return sInfo;
	}

	public boolean removeSendFileInfo(String uName, String fName, int nContentID)
	{
		CMList<CMSendFileInfo> sInfoList = null;
		CMSendFileInfo sInfo = null;
		boolean bResult = false;

		sInfoList = m_sendFileHashtable.get(uName);
		if(sInfoList == null)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo(), list not found for receiver("
					+uName+")");
			return false;
		}
		
		sInfo = new CMSendFileInfo();
		sInfo.setReceiverName(uName);
		sInfo.setFileName(fName);
		sInfo.setContentID(nContentID);
		bResult = sInfoList.removeElement(sInfo);

		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo() error! : "+sInfo.toString());
			return false;
		}
		
		if(sInfoList.isEmpty())
		{
			m_sendFileHashtable.remove(uName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done : "+sInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;
		
	}
	
	public Hashtable<String, CMList<CMSendFileInfo>> getSendFileHashtable()
	{
		return m_sendFileHashtable;
	}

	////////// add/remove/find receiving file info

	public boolean addRecvFileInfo(String senderName, String fName, long lSize, int nContentID, 
			long lRecvSize,	FileOutputStream fos)
	{
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = null;
		boolean bResult = false;

		rInfo = null;
		rInfo = new CMRecvFileInfo();
		rInfo.setSenderName(senderName);
		rInfo.setFileName(fName);
		rInfo.setFileSize(lSize);
		rInfo.setContentID(nContentID);
		rInfo.setRecvSize(lRecvSize);
		rInfo.setFileOutputStream(fos);
		
		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			rInfoList = new CMList<CMRecvFileInfo>();
			m_recvFileHashtable.put(senderName, rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo() failed: "+rInfo.toString());
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo() done: "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;
	}

	public CMRecvFileInfo findRecvFileInfo(String senderName, String fName, int nContentID)
	{	
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = null;
		CMRecvFileInfo tInfo = null;
		
		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			System.err.println("CMFileTransferInfo.findRecvFileInfo(), list not found for sender("
					+senderName+")");
			return null;
		}
		
		tInfo = new CMRecvFileInfo();
		tInfo.setSenderName(senderName);
		tInfo.setFileName(fName);
		tInfo.setContentID(nContentID);
		
		rInfo = rInfoList.findElement(tInfo);
		
		if(rInfo == null)
		{
			System.err.println("CMFileTransferInfo.findRecvFileInfo(), not found!: "+tInfo.toString());
			return null;
		}
				
		return rInfo;

	}

	public boolean removeRecvFileInfo(String senderName, String fName, int nContentID)
	{
		CMList<CMRecvFileInfo> rInfoList = null;
		CMRecvFileInfo rInfo = null;
		boolean bResult = false;

		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			System.err.println("CMFileTransferInfo.removeRecvFileInfo(), list not found for sender("
					+senderName+")");
			return false;
		}
		
		rInfo = new CMRecvFileInfo();
		rInfo.setSenderName(senderName);
		rInfo.setFileName(fName);
		rInfo.setContentID(nContentID);
		bResult = rInfoList.removeElement(rInfo);

		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.removeRecvFileInfo() error! : "+rInfo.toString());
			return false;
		}
		
		if(rInfoList.isEmpty())
		{
			m_sendFileHashtable.remove(senderName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done : "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;
		
	}
	
	public Hashtable<String, CMList<CMRecvFileInfo>> getRecvFileHashtable()
	{
		return m_recvFileHashtable;
	}
}
