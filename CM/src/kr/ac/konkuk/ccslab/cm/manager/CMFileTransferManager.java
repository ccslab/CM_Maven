package kr.ac.konkuk.ccslab.cm.manager;
import java.io.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSNSEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMSNSInfo;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttach;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttachHashMap;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSAttachList;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContent;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSContentList;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;

public class CMFileTransferManager {

	public static void init(CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		String strPath = confInfo.getFilePath();
		
		// if the default directory does not exist, create it.
		File defaultPath = new File(strPath);
		if(!defaultPath.exists() || !defaultPath.isDirectory())
		{
			boolean ret = defaultPath.mkdirs();
			if(ret)
			{
				if(CMInfo._CM_DEBUG)
					System.out.println("A default path is created!");
			}
			else
			{
				System.out.println("A default path cannot be created!");
				return;
			}
		}
		
		fInfo.setFilePath(strPath);
		if(CMInfo._CM_DEBUG)
			System.out.println("A default path for the file transfer: "+strPath);
				
		return;
	}
	
	public static void setFilePath(String strFilePath, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		fInfo.setFilePath(strFilePath);
		return;
	}
	
	public static void requestFile(String strFileName, String strFileOwner, int nContentID, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN && myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMFileTransferManager.requestFile(), Client must log in to the default server.");
			return;
		}
		
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_FILE_TRANSFER);
		fe.setUserName(myself.getName());	// requester name
		fe.setFileName(strFileName);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strFileOwner, cmInfo);
		
		fe = null;
		return;
	}
	
	public static void requestFile(String strFileName, String strFileOwner, CMInfo cmInfo)
	{
		requestFile(strFileName, strFileOwner, -1, cmInfo);
		return;
	}
	
	
	// strFilePath: absolute or relative path to a target file
	public static void pushFile(String strFilePath, String strReceiver, int nContentID, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		// get file information (size)
		File file = new File(strFilePath);
		if(!file.exists())
		{
			System.err.println("CMFileTransferManager.pushFile(), file("+strFilePath+") does not exists.");
			return;
		}
		long lFileSize = file.length();
		
		// add send file information
		// target(requester) name, file path, size
		// thread handle will be added when it starts to transmit file (not yet)
		fInfo.addSendFileInfo(strReceiver, strFilePath, lFileSize, nContentID, null);

		// get my name
		String strMyName = interInfo.getMyself().getName();

		// get file name
		String strFileName = getFileNameFromPath(strFilePath);
		System.out.println("file name: "+strFileName);
		
		// start file transfer process
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.START_FILE_TRANSFER);
		fe.setSenderName(strMyName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);

		file = null;
		fe = null;
		return;
	}
	
	public static void pushFile(String strFilePath, String strReceiver, CMInfo cmInfo)
	{
		pushFile(strFilePath, strReceiver, -1, cmInfo);
		return;
	}

	// srcFile: reference of RandomAccessFile of source file
	// bos: reference of BufferedOutputStream of split file
	public static void splitFile(RandomAccessFile srcFile, long lOffset, long lSplitSize, String strSplitFile)
	{
		long lRemainBytes = lSplitSize;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		int readBytes;
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(strSplitFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			// set file position
			srcFile.seek(lOffset);

			// read and write
			while( lRemainBytes > 0 )
			{
				if(lRemainBytes >= CMInfo.FILE_BLOCK_LEN)
					readBytes = srcFile.read(fileBlock);
				else
					readBytes = srcFile.read(fileBlock, 0, (int)lRemainBytes);

				if( readBytes >= CMInfo.FILE_BLOCK_LEN )
					bos.write(fileBlock);
				else
					bos.write(fileBlock, 0, readBytes);

				lRemainBytes -= readBytes;
			}
			
			bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public static long mergeFiles(String[] strSplitFiles, int nSplitNum, String strMergeFile)
	{
		long lMergeSize = -1;
		long lSrcSize = 0;
		FileInputStream srcfis = null;
		BufferedOutputStream bos = null;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		int readBytes = 0;

		if(nSplitNum != strSplitFiles.length)
		{
			System.err.println("CMFileTransferManager.mergeFiles(), the number of members in the "
					+"first parameter is different from the given second parameter!");
			return -1;
		}
		
		// open a target file
		try {
			
			bos = new BufferedOutputStream(new FileOutputStream(strMergeFile));
			
			for(int i = 0; i < nSplitNum; i++)
			{
				// open a source file
				File srcFile = new File(strSplitFiles[i]);
				srcfis = new FileInputStream(srcFile);

				// get source file size
				lSrcSize = srcFile.length();

				// concatenate a source file to a target file
				while( lSrcSize > 0 )
				{
					if( lSrcSize >= CMInfo.FILE_BLOCK_LEN )
					{
						readBytes = srcfis.read(fileBlock);
						bos.write(fileBlock, 0, readBytes);
					}
					else
					{
						readBytes = srcfis.read(fileBlock, 0, (int)lSrcSize);
						bos.write(fileBlock, 0, readBytes);
					}

					lSrcSize -= readBytes;
				}

				// close a source file
				srcfis.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(srcfis != null)
				{
					srcfis.close();
				}
				if(bos != null){
					bos.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		File targetFile = new File(strMergeFile);
		lMergeSize = targetFile.length();
		
		return lMergeSize;
	}
	
	public static String getFileNameFromPath(String strPath)
	{
		String strName = null;
		int index;
		String sep = File.separator;
		index = strPath.lastIndexOf("/");
		if(index == -1)
		{
			index = strPath.lastIndexOf(sep);
			if(index == -1)
				strName = strPath;
			else
				strName = strPath.substring(index+1);
		}
		else
		{
			strName = strPath.substring(index+1);
		}
		
		return strName;
	}
	
	//////////////////////////////////////////////////////////////////
	// process file event
	
	public static void processEvent(CMMessage msg, CMInfo cmInfo)
	{
		CMFileEvent fe = new CMFileEvent(msg.m_buf);
		
		switch(fe.getID())
		{
		case CMFileEvent.REQUEST_FILE_TRANSFER:
			processREQUEST_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.REPLY_FILE_TRANSFER:
			processREPLY_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER:
			processSTART_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER_ACK:
			processSTART_FILE_TRANSFER_ACK(fe, cmInfo);
			break;
		case CMFileEvent.CONTINUE_FILE_TRANSFER:
			processCONTINUE_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER:
			processEND_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER_ACK:
			processEND_FILE_TRANSFER_ACK(fe, cmInfo);
			break;
		case CMFileEvent.REQUEST_DIST_FILE_PROC:
			processREQUEST_DIST_FILE_PROC(fe, cmInfo);
			break;
		default:
			System.err.println("CMFileTransferManager.processEvent(), unknown event id("+fe.getID()+").");
			fe = null;
			return;
		}
		
		fe = null;
		return;
	}
	
	private static void processREQUEST_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_FILE_TRANSFER(), requester("
					+fe.getUserName()+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()
					+").");
		}

		String strFileName = fe.getFileName();
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.REPLY_FILE_TRANSFER);
		feAck.setFileName(strFileName);

		// get the full path of the requested file
		String strFullPath = fInfo.getFilePath() + "/" + strFileName; 
		// check the file existence
		File file = new File(strFullPath);
		if(!file.exists())
		{
			feAck.setReturnCode(0);	// file not found
			CMEventManager.unicastEvent(feAck, fe.getUserName(), cmInfo);
			feAck = null;
			return;
		}
		
		feAck.setReturnCode(1);	// file found
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getUserName(), cmInfo);
		
		// get the file size
		long lFileSize = file.length();
		
		// add send file information
		// requester name, file path, size
		// thread reference will be added when it starts to transmit file (not yet)
		fInfo.addSendFileInfo(fe.getUserName(), strFullPath, lFileSize, fe.getContentID(), null);

		// start file transfer process
		CMFileEvent feStart = new CMFileEvent();
		feStart.setID(CMFileEvent.START_FILE_TRANSFER);
		feStart.setSenderName(myself.getName());
		feStart.setFileName(fe.getFileName());
		feStart.setFileSize(lFileSize);
		feStart.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feStart, fe.getUserName(), cmInfo);

		feAck = null;
		feStart = null;
		file = null;
		return;
	}
	
	private static void processREPLY_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileManager.processREPLY_FILE_TRANSFER(), file("+fe.getFileName()
					+"), return code("+fe.getReturnCode()+"), contentID("+fe.getContentID()+").");
		}
		return;
	}
	
	private static void processSTART_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER(),");
			System.out.println("sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), size("
					+fe.getFileSize()+"), contentID("+fe.getContentID()+").");
		}
		
		// set file size
		long lFileSize = fe.getFileSize();
		
		// set a path of the received file
		String strFullPath = fInfo.getFilePath();
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			strFullPath = strFullPath + "/" + fe.getFileName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + "/" + fe.getSenderName();
			File subDir = new File(strFullPath);
			if(!subDir.exists() || !subDir.isDirectory())
			{
				boolean ret = subDir.mkdirs();
				if(ret)
				{
					if(CMInfo._CM_DEBUG)
						System.out.println("A sub-directory is created.");
				}
				else
				{
					System.out.println("A sub-directory cannot be created!");
					return;
				}
			}
			
			strFullPath = strFullPath + "/" + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
		}
		
		
		// open a file output stream
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(strFullPath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		// init received size
		long lRecvSize = 0;
		// add the received file info in the push list
		fInfo.addRecvFileInfo(fe.getFileName(), lFileSize, fe.getContentID(), lRecvSize, fos);
		
		// send ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.START_FILE_TRANSFER_ACK);
		feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
		feAck.setFileName(fe.getFileName());
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);

		feAck = null;
		return;
	}
	
	private static void processSTART_FILE_TRANSFER_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		sendFile(cmInfo); // need to be processed by a separate thread (not yet)
	}
	
	private static void sendFile(CMInfo cmInfo)
	{
		String strRequester = null;
		String strFileName = null;
		String strFullFileName = null;
		long lFileSize = -1;
		int nContentID = -1;
		String strSenderName = null;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();

		CMSendFileInfo sInfo = null;
		boolean bFound = false;
		Iterator<CMSendFileInfo> iter = fInfo.getSendFileList().iterator();
		while(iter.hasNext() && !bFound)
		{
			sInfo = iter.next();
			if(!sInfo.isStartedToSend())
			{
				bFound = true;
				sInfo.setStartedToSend(true);
				strRequester = sInfo.getRequesterName();
				strFullFileName = sInfo.getFilePath();
				strFileName = getFileNameFromPath(strFullFileName);
				lFileSize = sInfo.getFileSize();
				nContentID = sInfo.getContentID();
			}
		}
		
		System.out.println("Sending file("+strFileName+") to target("+strRequester+").");

		// open the file
		try {
			sInfo.setFileInputStream(new FileInputStream(strFullFileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// set sender name
		strSenderName = cmInfo.getInteractionInfo().getMyself().getName();

		// send blocks
		long lRemainBytes = lFileSize;
		long lSentBytes = 0;
		int nReadBytes = 0;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		CMFileEvent fe = new CMFileEvent();
		
		while(lRemainBytes > 0)
		{
			try {
				nReadBytes = sInfo.getFileInputStream().read(fileBlock);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// send file block
			fe = new CMFileEvent();
			fe.setID(CMFileEvent.CONTINUE_FILE_TRANSFER);
			fe.setSenderName(strSenderName);
			fe.setFileName(strFileName);
			fe.setFileBlock(fileBlock);
			fe.setBlockSize(nReadBytes);
			fe.setContentID(nContentID);
			CMEventManager.unicastEvent(fe, strRequester, cmInfo);
			
			lRemainBytes -= nReadBytes;
			lSentBytes += nReadBytes;
		}
		
		if(lSentBytes == lFileSize)
		{
			sInfo.setSentAll(true);
		}
		else
		{
			System.err.println("CMFileTransferManager.sendFile(), All file bytes are not sent.");
			System.err.println("file size("+lFileSize+"), sent bytes("+lSentBytes+").");
			return;
		}
		
		// close fis
		try {
			sInfo.getFileInputStream().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Ending transfer of file("+strFileName+") to target("+strRequester
				+"), size("+lFileSize+") Bytes.");

		// send the end of file transfer
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.END_FILE_TRANSFER);
		fe.setSenderName(strSenderName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strRequester, cmInfo);
		
		fe = null;
		return;
	}
	
	private static void processCONTINUE_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		
		/*
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileManager.processCONTINUE_FILE_TRANSFER(), sender("
					+fe.getSenderName()+"), file("+fe.getFileName()+"), "+fe.getBlockSize()
					+" Bytes, contentID("+fe.getContentID()+").");
		}
		*/

		// find info in the recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getFileName(), fe.getContentID());
		if( recvInfo == null )
		{
			System.err.println("CMFileTransferManager.processCONTINUE_FILE_TRANSFER(), "
					+ "recv file info for file("+fe.getFileName()+") not found.");
			return;
		}

		try {
			recvInfo.getFileOutputStream().write(fe.getFileBlock(), 0, fe.getBlockSize());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		recvInfo.setRecvSize(recvInfo.getRecvSize()+fe.getBlockSize());

		/*
		if(CMInfo._CM_DEBUG)
			System.out.println("Cumulative written file size: "+pushInfo.m_lRecvSize+" Bytes.");
		*/
		
		return;
	}
	
	private static void processEND_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMSNSInfo snsInfo = cmInfo.getSNSInfo();
		
		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER(), recv file info "
					+"for file("+fe.getFileName()+") not found.");
			return;
		}
		// close received file descriptor
		try {
			recvInfo.getFileOutputStream().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER(), sender("+fe.getSenderName()
					+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()+"), file size("
					+recvInfo.getFileSize()+"), received size("+recvInfo.getRecvSize()+").");
		}

		// remove info from push file list
		fInfo.removeRecvFileInfo(fe.getFileName(), fe.getContentID());
		
		// send ack
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_ACK);
		feAck.setUserName(interInfo.getMyself().getName());
		feAck.setFileName(fe.getFileName());
		feAck.setReturnCode(1);	// success
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;
		
		//////////////////// check the completion of receiving attached file of SNS content
		CMSNSAttachList attachList = null;
		CMSNSAttach attach = null;
		int nContentID = -1;
		String strFileName = null;
		int nCompleted = 0;
		CMSNSEvent se = null;
		
		if(confInfo.getSystemType().equals("SERVER"))
		{
			// find attachment info to be received
			CMSNSAttachHashMap attachMap = snsInfo.getSNSAttachMapToBeRecv();
			attachList = attachMap.findSNSAttachList(fe.getSenderName());
			if(attachList == null) return;
			attach = attachList.findSNSAttach(fe.getContentID());
			if(attach == null) return;
			if(!attach.containsFileName(fe.getFileName())) return;
			// add attached file info in attached_file_table of DB
			String strFilePath = fInfo.getFilePath() + "/" + fe.getSenderName();
			nContentID = attach.getContentID();
			strFileName = fe.getFileName();
			
			// create a thumbnail image
			String strInputPath = strFilePath + "/" + strFileName;
			if(CMUtil.isImageFile(strInputPath))
			{
				int index = strFileName.lastIndexOf(".");
				String strName = strFileName.substring(0, index)+"-thumbnail";
				String strExt = strFileName.substring(index+1, strFileName.length());
				strName = strName + "." + strExt;
				String strOutPath = strFilePath + "/" + strName;
				
				int nWidth = confInfo.getThumbnailHorSize();
				int nHeight = confInfo.getThumbnailVerSize();
				CMUtil.createScaledImage(strInputPath, nWidth, nHeight, strOutPath);
			}
			
			if(confInfo.isDBUse())
			{
				CMDBManager.queryInsertSNSAttachedFile(nContentID, strFilePath, strFileName, cmInfo);
			}
			else
			{
				CMSNSContentList contentList = snsInfo.getSNSContentList();
				CMSNSContent content = contentList.findSNSContent(nContentID);
				if(content == null)
				{
					System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER(), content("+nContentID
							+") not found!");
					return;
				}
				content.getFilePathList().add(strFilePath+"/"+strFileName);
			}
			
			// increase the number of completed attached files
			nCompleted = attach.getNumCompleted() + 1;
			attach.setNumCompleted(nCompleted);
			// check if all attached files of the content have been transfered or not
			if(nCompleted < attach.getFilePathList().size()) return;
			// send the response event to the content upload request
			se = new CMSNSEvent();
			se.setID(CMSNSEvent.CONTENT_UPLOAD_RESPONSE);
			se.setReturnCode(attach.getReturnCode());
			se.setContentID(attach.getContentID());
			se.setDate(attach.getCreationTime());
			se.setUserName(attach.getUserName());
			CMEventManager.unicastEvent(se, fe.getSenderName(), cmInfo);
			se = null;
			
			// remove the completed attachment info
			attachList.removeSNSAttach(nContentID);
			attach = null;
			if(attachList.getSNSAttachList().isEmpty())
			{
				attachMap.removeSNSAttachList(fe.getSenderName());
				attachList = null;
			}

		}
		else if(confInfo.getSystemType().equals("CLIENT"))
		{
			// find attachment info to be received
			attachList = snsInfo.getSNSAttachListToBeRecv();
			nContentID = fe.getContentID();
			attach = attachList.findSNSAttach(nContentID);
			if(attach == null) return;
			if(!attach.containsFileName(fe.getFileName())) return;
			// increase the number of completed attached files
			nCompleted = attach.getNumCompleted() + 1;
			attach.setNumCompleted(nCompleted);
			// check if all attached files of the content have been transfered or not
			if(nCompleted < attach.getFilePathList().size()) return;

			// remove the completed attachment info
			attachList.removeSNSAttach(nContentID);
			attach = null;
		}
		else
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER(), wrong system type!");
		}
		////////////////////

		return;
	}
	
	private static void processEND_FILE_TRANSFER_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		String strUserName = fe.getUserName();
		String strFileName = fe.getFileName();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strUserName, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), send info not found");
			System.err.println("requester("+strUserName+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeFileRequestInfo(strUserName, strFileName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), receiver("
					+strUserName+"), file("+strFileName+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		/*
		CMSNSInfo snsInfo = cmInfo.getSNSInfo();
		CMSNSAttach attach = null;
		int nCompleted = 0;
		*/
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMSNSManager.checkClientAttachmentCompletion(nContentID, strFileName, cmInfo);
			/*
			attach = snsInfo.getSNSAttachToBeSent();
			if(attach.getContentID() != nContentID) return;
			if(!attach.containsFileName(strFileName)) return;
			nCompleted = attach.getNumCompleted() + 1;
			attach.setNumCompleted(nCompleted);
			if(nCompleted < attach.getFilePathList().size()) return;
			attach.init();
			*/
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			CMSNSManager.checkServerAttachmentCompletion(strUserName, nContentID, strFileName, cmInfo);
			/*
			// find and update the completed attachment info
			CMSNSAttachHashMap attachMap = snsInfo.getSNSAttachMapToBeSent();
			CMSNSAttachList attachList = attachMap.findSNSAttachList(strUserName);
			if(attachList == null) return;
			attach = attachList.findSNSAttach(nContentID);
			if(attach == null) return;
			if(!attach.containsFileName(strFileName)) return;
			nCompleted = attach.getNumCompleted() + 1;
			attach.setNumCompleted(nCompleted);
			if(nCompleted < attach.getFilePathList().size()) return;
			
			// remove the completed attachment info
			attachList.removeSNSAttach(nContentID);
			attach = null;
			if(attachList.getSNSAttachList().isEmpty())
			{
				// send CONTENT_DOWNLOAD_END event to the client
				CMSNSEvent sevent = new CMSNSEvent();
				sevent.setID(CMSNSEvent.CONTENT_DOWNLOAD_END);
				sevent.setUserName( attachList.getUserName() );
				sevent.setContentOffset( attachList.getContentOffset() );
				sevent.setNumContents( attachList.getNumContents() );

				// send the end event
				CMEventManager.unicastEvent(sevent, strUserName, cmInfo);
				sevent = null;
				
				// remove the completed attachment list info
				attachMap.removeSNSAttachList(fe.getUserName());
				attachList = null;
			}
			*/
			
			// check the completion of prefetching process
			CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
			CMUser user = interInfo.getLoginUsers().findMember(strUserName);
			if(user != null && user.getAttachDownloadScheme() == CMInfo.SNS_ATTACH_PREFETCH)
			{
				CMSNSManager.checkServerPrefetchCompletion(strUserName, strFileName, cmInfo);
				/*
				CMSNSPrefetchHashMap prefetchMap = snsInfo.getPrefetchMap();
				CMSNSPrefetchList prefetchList = prefetchMap.findPrefetchList(strUserName);
				if(prefetchList == null) return;
				String strPath = prefetchList.findFilePath(strFileName);
				if(strPath == null) return;
				prefetchList.removeFilePath(strPath);
				if(!prefetchList.getFilePathList().isEmpty()) return;
				prefetchMap.removePrefetchList(strUserName);
				if(CMInfo._CM_DEBUG)
					System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), "
							+"prefetching for user("+strUserName+") completes.");
				// notify the user of the prefetching completion??
				*/
			}
			
		}
		else
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), wrong system type!");
		}
		
		return;
	}
	
	private static void processREQUEST_DIST_FILE_PROC(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_DIST_FILE_PROC(), user("
						+fe.getUserName()+") requests the distributed file processing.");
		}
		return;
	}
	
	/////////////////////////////////////////////////////////////////////////////
}
