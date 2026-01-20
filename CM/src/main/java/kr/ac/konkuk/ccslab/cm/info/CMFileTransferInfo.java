package kr.ac.konkuk.ccslab.cm.info;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;

import java.io.*;
import java.util.stream.Collectors;

public class CMFileTransferInfo {
	private static CMFileTransferInfo instance;

	// [Modification]: Changed key from String to CMUserLoginKey for multi-device support
	// key is the receiver (name, uuid) pair
	private Hashtable<CMUserLoginKey, CMList<CMSendFileInfo>> m_sendFileHashtable;
	// key is the sender (name, uuid) pair
	private Hashtable<CMUserLoginKey, CMList<CMRecvFileInfo>> m_recvFileHashtable;
	private boolean m_bCancelSend;	// flag for canceling file push with the default channel
	
	// starting time (to send/receive request pushing/pulling a file)
	private long m_lStartRequestTime;
	// starting time of sending a file after the permit granted
	private long m_lStartSendTime;
	// ending time of sending a file (receiving completion ack event)
	private long m_lEndSendTime;
	// starting time of receiving a file after the permit granted
	private long m_lStartRecvTime;
	// ending time of receiving a file (receiving completion event)
	private long m_lEndRecvTime;
	
	private CMFileTransferInfo()
	{
		m_sendFileHashtable = new Hashtable<>();
		m_recvFileHashtable = new Hashtable<>();
		m_bCancelSend = false;
		m_lStartRequestTime = 0;
		m_lStartSendTime = 0;
		m_lEndSendTime = 0;
		m_lStartRecvTime = 0;
		m_lEndRecvTime = 0;
	}

	// getInstance()
	public static synchronized CMFileTransferInfo getInstance() {
		if(instance == null) {
			instance = new CMFileTransferInfo();
		}
		return instance;
	}
	
	////////// set/get methods
		
	public synchronized void setStartRequestTime(long time)
	{
		m_lStartRequestTime = time;
	}
	
	public synchronized long getStartRequestTime()
	{
		return m_lStartRequestTime;
	}
	
	public synchronized void setStartSendTime(long time)
	{
		m_lStartSendTime = time;
	}
	
	public synchronized long getStartSendTime()
	{
		return m_lStartSendTime;
	}
	
	public synchronized void setEndSendTime(long time)
	{
		m_lEndSendTime = time;
	}
	
	public synchronized long getEndSendTime()
	{
		return m_lEndSendTime;
	}
	
	public synchronized void setStartRecvTime(long time)
	{
		m_lStartRecvTime = time;
	}
	
	public synchronized long getStartRecvTime()
	{
		return m_lStartRecvTime;
	}
	
	public synchronized void setEndRecvTime(long time)
	{
		m_lEndRecvTime = time;
	}
	
	public synchronized long getEndRecvTime()
	{
		return m_lEndRecvTime;
	}
	
	public synchronized void setCancelSend(boolean bCancel)
	{
		m_bCancelSend = bCancel;
		return;
	}
	
	public synchronized boolean isCancelSend()
	{
		return m_bCancelSend;
	}
		
	
	////////// add/remove/find sending file info

	public synchronized boolean addSendFileInfo(CMSendFileInfo sInfo)
	{
		String strFileName = null;
		CMList<CMSendFileInfo> sInfoList = null;
		boolean bResult = false;
		
		strFileName = sInfo.getFilePath().substring(sInfo.getFilePath().lastIndexOf(File.separator)+1);
		sInfo.setFileName(strFileName);

		// [Modification]: Create key using receiver name and UUID
		CMUserLoginKey key = new CMUserLoginKey(sInfo.getFileReceiver(), sInfo.getFileReceiverUuid());
		sInfoList = m_sendFileHashtable.get(key);
		if(sInfoList == null)
		{
			sInfoList = new CMList<>();
			m_sendFileHashtable.put(key, sInfoList);
		}
		
		bResult = sInfoList.addElement(sInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addSendFileInfo() failed: "+sInfo);
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addSendFileInfo() done: "+sInfo);
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}

		return true;		
	}

	public synchronized CMSendFileInfo findSendFileInfo(String fileReceiver, UUID fileReceiverUuid, String fName,
														int nContentID)
	{
		CMSendFileInfo sInfo = null;
		CMList<CMSendFileInfo> sInfoList = null;
		CMSendFileInfo tInfo = null;

		// [Modification]: Direct lookup with key
		CMUserLoginKey key = new CMUserLoginKey(fileReceiver, fileReceiverUuid);
		sInfoList = m_sendFileHashtable.get(key);

		if(sInfoList == null)
		{
			System.err.println("CMFileTransferInfo.findSendFileInfo(), list not found for receiver("
					+fileReceiver+"), uuid("+fileReceiverUuid+")!");
			return null;
		}
		
		tInfo = new CMSendFileInfo();
		tInfo.setFileReceiver(fileReceiver);
		tInfo.setFileReceiverUuid(fileReceiverUuid); // Set UUID
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

	public synchronized boolean removeSendFileInfo(String fileReceiver, UUID fileReceiverUuid, String fName,
												   int nContentID)
	{
		CMList<CMSendFileInfo> sInfoList = null;
		CMSendFileInfo sInfo = null;
		boolean bResult = false;

		// [Modification]: Key lookup
		CMUserLoginKey key = new CMUserLoginKey(fileReceiver, fileReceiverUuid);
		sInfoList = m_sendFileHashtable.get(key);
		if(sInfoList == null)
		{
			//System.err.println("CMFileTransferInfo.removeSendFileInfo(), list not found for receiver("
			//		+uName+")");
			return false;
		}
		
		sInfo = new CMSendFileInfo();
		sInfo.setFileReceiver(fileReceiver);
		sInfo.setFileReceiverUuid(fileReceiverUuid); // Set UUID
		sInfo.setFileName(fName);
		sInfo.setContentID(nContentID);
		bResult = sInfoList.removeElement(sInfo);

		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo() error! : "+sInfo);
			return false;
		}
		
		if(sInfoList.isEmpty())
		{
			m_sendFileHashtable.remove(key);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done : "+sInfo);
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;
	}
	
	public synchronized boolean removeSendFileInfo(CMSendFileInfo sfInfo)
	{
		CMList<CMSendFileInfo> sInfoList = null;
		String strFileReceiver = sfInfo.getFileReceiver();
		UUID fileReceiverUuid = sfInfo.getFileReceiverUuid();
		boolean bResult = false;

		CMUserLoginKey key = new CMUserLoginKey(strFileReceiver, fileReceiverUuid);
		sInfoList = m_sendFileHashtable.get(key);
		if(sInfoList == null)
		{
			//System.err.println("CMFileTransferInfo.removeSendFileInfo(), list not found for receiver("
			//		+uName+")");
			return false;
		}
		
		bResult = sInfoList.removeElement(sfInfo);

		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.removeSendFileInfo() error! : "+sfInfo);
			return false;
		}
		
		if(sInfoList.isEmpty())
		{
			m_sendFileHashtable.remove(key);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileInfo() done : "+sfInfo);
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;		
	}
	
	public synchronized boolean removeSendFileList(String strReceiver, UUID receiverUuid)
	{
		// [Modification]: Direct removal by key
		CMUserLoginKey key = new CMUserLoginKey(strReceiver, receiverUuid);
		CMList<CMSendFileInfo> sInfoList = null;

		sInfoList = m_sendFileHashtable.remove(key);

		if(sInfoList == null)
		{
			//System.err.println("CMFileTransferInfo.removeSendFileList(); list not found for receiver("
			//		+strReceiver+")!");
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeSendFileList() done : receiver("+strReceiver
					+"), uuid("+receiverUuid+").");
			System.out.println("# current hashtable elements: "+m_sendFileHashtable.size());
		}
		
		return true;
	}
	
	public synchronized boolean clearSendFileHashtable()
	{
		m_sendFileHashtable.clear();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferInfo.clearSendFileHashtable(); current size("
					+m_sendFileHashtable.size()+").");
		return true;
	}
	
	public synchronized List<CMSendFileInfo> getSendFileList(String strReceiver, UUID receiverUuid)
	{
		CMList<CMSendFileInfo> sendFileList = null;
		CMUserLoginKey key = new CMUserLoginKey(strReceiver, receiverUuid);
		sendFileList = m_sendFileHashtable.get(key);

		if( sendFileList == null ) {
			System.err.println("CMFileTransferInfo.getSendFileList(), list for receiver("+strReceiver
					+"), uuid("+receiverUuid+") not found!");
			return null;
		}

		return sendFileList.getList();
	}
	
	public synchronized Hashtable<CMUserLoginKey, CMList<CMSendFileInfo>> getSendFileHashtable()
	{
		return m_sendFileHashtable;
	}
	
	//////////////////// methods for the investigation of current state of the sending file info
	//////////////////// (used by the file transfer with separate channels and threads)

	// find the CMSendFileInfo of which file is currently being sent to the receiver
	public synchronized CMSendFileInfo findSendFileInfoOngoing(String strReceiver, UUID receiverUuid)
	{
		CMSendFileInfo sInfo = null;
		// [Modification] Create a key with the receiver name and UUID
		CMUserLoginKey key = new CMUserLoginKey(strReceiver, receiverUuid);

		CMList<CMSendFileInfo> sInfoList = m_sendFileHashtable.get(key);
		if(sInfoList == null) return null;
		
		Iterator<CMSendFileInfo> iter = sInfoList.getList().iterator();
		while(iter.hasNext())
		{
			sInfo = iter.next();
			if(sInfo.getSendTaskResult() != null)
			{
				if(CMInfo._CM_DEBUG) {
					System.out.println("CMFileTransferInfo.findSendFileInfoOngoing(); ongoing send info found: "
							+ sInfo);
				}
				return sInfo;
			}
		}
		
		return null;
	}
	
	// find the sending file info that is not yet started by the thread pool
	public synchronized CMSendFileInfo findSendFileInfoNotStarted(String strReceiver, UUID receiverUuid)
	{
		CMSendFileInfo sfInfo = null;
		CMUserLoginKey key = new CMUserLoginKey(strReceiver, receiverUuid);
		CMList<CMSendFileInfo> sfInfoList = m_sendFileHashtable.get(key);
		boolean bFound = false;
		
		if(sfInfoList == null) return null;
		
		Iterator<CMSendFileInfo> iter = sfInfoList.getList().iterator();
		while(iter.hasNext())
		{
			sfInfo = iter.next();
			if(sfInfo.getSendTaskResult() == null)
			{
				if(CMInfo._CM_DEBUG) {
					System.out.println("CMFileTransferInfo.findSendFileInfoNotStarted(); found: " + sfInfo);
				}
				return sfInfo;
			}
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferInfo.findSendFileInfoNotStarted(); not found!");
		return null;
	}

	////////// add/remove/find receiving file info

	public synchronized boolean addRecvFileInfo(String senderName, UUID senderUuid, String fName,
			long lSize, int nContentID,	long lRecvSize, RandomAccessFile writeFile)
	{
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = null;
		boolean bResult = false;

		rInfo = new CMRecvFileInfo();
		rInfo.setFileSender(senderName);
		rInfo.setFileSenderUuid(senderUuid);
		rInfo.setFileName(fName);
		rInfo.setFileSize(lSize);
		rInfo.setContentID(nContentID);
		rInfo.setRecvSize(lRecvSize);
		rInfo.setWriteFile(writeFile);

		// [Modification] Changed the key of hashtable from String to CMUserLoginKey
		CMUserLoginKey key = new CMUserLoginKey(senderName, senderUuid);
		rInfoList = m_recvFileHashtable.get(key);
		if(rInfoList == null)
		{
			rInfoList = new CMList<>();
			m_recvFileHashtable.put(key, rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo() failed: "+rInfo);
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo() done: "+rInfo);
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;
	}
	
	public synchronized boolean addRecvFileInfo(CMRecvFileInfo rInfo)
	{
		CMList<CMRecvFileInfo> rInfoList = null;
		boolean bResult = false;

		CMUserLoginKey key = new CMUserLoginKey(rInfo.getFileSender(), rInfo.getFileSenderUuid());
		rInfoList = m_recvFileHashtable.get(key);
		if(rInfoList == null)
		{
			rInfoList = new CMList<>();
			m_recvFileHashtable.put(key, rInfoList);
		}
		
		bResult = rInfoList.addElement(rInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferInfo.addRecvFileInfo() failed: "+rInfo);
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.addRecvFileInfo() done: "+rInfo);
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;		
	}

	public synchronized CMRecvFileInfo findRecvFileInfo(String senderName, UUID senderUuid, String fName,
														int nContentID)
	{	
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = null;
		CMRecvFileInfo tInfo = null;

		// [Modification] Use CMUserLoginKey with sender name and uuid as the key for the hashtable
		CMUserLoginKey key = new CMUserLoginKey(senderName, senderUuid);
		rInfoList = m_recvFileHashtable.get(key);
		if(rInfoList == null)
		{
			System.err.println("CMFileTransferInfo.findRecvFileInfo(), list not found for sender("
					+senderName+"), uuid("+senderUuid+")!");
			return null;
		}
		
		tInfo = new CMRecvFileInfo();
		tInfo.setFileSender(senderName);
		// [Modification] Set sender UUID to the temporary object for search
		tInfo.setFileSenderUuid(senderUuid);
		tInfo.setFileName(fName);
		tInfo.setContentID(nContentID);
		
		rInfo = rInfoList.findElement(tInfo);
		
		if(rInfo == null)
		{
			System.err.println("CMFileTransferInfo.findRecvFileInfo(), not found!: "+tInfo);
			return null;
		}
				
		return rInfo;

	}

	public synchronized boolean removeRecvFileInfo(String senderName, String fName, 
			int nContentID)
	{
		CMList<CMRecvFileInfo> rInfoList = null;
		CMRecvFileInfo rInfo = null;
		boolean bResult = false;

		rInfoList = m_recvFileHashtable.get(senderName);
		if(rInfoList == null)
		{
			//System.err.println("CMFileTransferInfo.removeRecvFileInfo(), list not found for sender("
			//		+senderName+")");
			return false;
		}
		
		rInfo = new CMRecvFileInfo();
		rInfo.setFileSender(senderName);
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
			m_recvFileHashtable.remove(senderName);
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeRecvFileInfo() done : "+rInfo.toString());
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;
		
	}

	public synchronized boolean removeRecvFileList(String strSender)
	{
		CMList<CMRecvFileInfo> rInfoList = null;
		rInfoList = m_recvFileHashtable.remove(strSender);
		if(rInfoList == null)
		{
			//System.err.println("CMFileTransferInfo.removeRecvFileList(); list not found for sender("
			//		+strSender+")!");
			return false;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferInfo.removeRecvFileList() done : sender("+strSender+").");
			System.out.println("# current hashtable elements: "+m_recvFileHashtable.size());
		}
		
		return true;
	}

	public synchronized boolean clearRecvFileHashtable()
	{
		m_recvFileHashtable.clear();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferInfo.clearRecvFileHashtable(); current size("
					+m_sendFileHashtable.size()+").");
		return true;
	}
	

	public synchronized CMList<CMRecvFileInfo> getRecvFileList(String strSender)
	{
		CMList<CMRecvFileInfo> recvFileList = null;
		recvFileList = m_recvFileHashtable.get(strSender);
		
		return recvFileList;
	}

	public synchronized Hashtable<CMUserLoginKey, CMList<CMRecvFileInfo>> getRecvFileHashtable()
	{
		return m_recvFileHashtable;
	}
	
	//////////////////// methods for the investigation of current state of the receiving file info
	//////////////////// (used by the file transfer with separate channels and threads)
	
	// find the receiving file info that is not yet started by the thread pool 
	public synchronized CMRecvFileInfo findRecvFileInfoNotStarted(String strSender)
	{
		CMRecvFileInfo rfInfo = null;
		CMList<CMRecvFileInfo> rfInfoList = m_recvFileHashtable.get(strSender);
		boolean bFound = false;
		
		if(rfInfoList == null) return null;
		
		Iterator<CMRecvFileInfo> iter = rfInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rfInfo = iter.next();
			if(rfInfo.getRecvTaskResult() == null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMFileTransferInfo.findRecvFileInfoNotStarted(); found: "+rfInfo.toString());
			}
		}
		
		if(bFound)
			return rfInfo;
		else
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMFileTransferInfo.findRecvFileInfoNotStarted(); not found!");
			return null;
		}
	}

	// check whether there is the receiving file info that is being used 
	public synchronized boolean isRecvOngoing(String strSender)
	{
		CMRecvFileInfo rfInfo = null;
		CMList<CMRecvFileInfo> rfInfoList = m_recvFileHashtable.get(strSender);
		boolean bFound = false;
		
		if(rfInfoList == null) return false;
		
		Iterator<CMRecvFileInfo> iter = rfInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rfInfo = iter.next();
			if(rfInfo.getRecvTaskResult() != null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMFileTransferInfo.isRecvOngoing(); ongoing recv info found: "+rfInfo.toString());
			}
		}
		
		return bFound;
	}
	
	// find the CMRecvFileInfo of which file is currently being received from the sender
	public synchronized CMRecvFileInfo findRecvFileInfoOngoing(String strSender)
	{
		CMRecvFileInfo rInfo = null;
		CMList<CMRecvFileInfo> rInfoList = m_recvFileHashtable.get(strSender);
		boolean bFound = false;
		
		if(rInfoList == null) return null;
		
		Iterator<CMRecvFileInfo> iter = rInfoList.getList().iterator();
		while(iter.hasNext() && !bFound)
		{
			rInfo = iter.next();
			if(rInfo.getRecvTaskResult() != null)
			{
				bFound = true;
				if(CMInfo._CM_DEBUG)
					System.out.println("CMFileTransferInfo.findRecvFileInfoOngoing(); ongoing recv info found: "
							+rInfo.toString());
			}
		}
		
		if(!bFound)
			rInfo = null;
		
		return rInfo;
	}	
}
