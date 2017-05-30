package kr.ac.konkuk.ccslab.cm.manager;
import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMRecvFileTask;
import kr.ac.konkuk.ccslab.cm.thread.CMSendFileTask;

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
		
		// create an executor service object
		ExecutorService es = fInfo.getExecutorService();
		int nAvailableProcessors = Runtime.getRuntime().availableProcessors();
		es = Executors.newFixedThreadPool(nAvailableProcessors);
		fInfo.setExecutorService(es);
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.init(), executor service created; # available processors("
					+nAvailableProcessors+").");
		}
				
		return;
	}
	
	public static void terminate(CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		ExecutorService es = fInfo.getExecutorService();
		es.shutdown();	// need to check
	}
	
	public static void setFilePath(String strFilePath, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		fInfo.setFilePath(strFilePath);
		return;
	}
	
	public static void requestFile(String strFileName, String strFileOwner, CMInfo cmInfo)
	{
		requestFile(strFileName, strFileOwner, -1, cmInfo);
		return;
	}
	
	public static void requestFile(String strFileName, String strFileOwner, int nContentID, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN 
				&& myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMFileTransferManager.requestFile(), Client must log in to the default server.");
			return;
		}
		
		if(confInfo.isFileTransferScheme())
			requestFileWithSepChannel(strFileName, strFileOwner, nContentID, cmInfo);
		else
			requestFileWithDefChannel(strFileName, strFileOwner, nContentID, cmInfo);
		return;
	}
	
	public static void requestFileWithDefChannel(String strFileName, String strFileOwner, int nContentID, CMInfo cmInfo)
	{
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_FILE_TRANSFER);
		fe.setUserName(myself.getName());	// requester name
		fe.setFileName(strFileName);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strFileOwner, cmInfo);
		
		fe = null;
		return;
	}
	
	public static void requestFileWithSepChannel(String strFileName, String strFileOwner, int nContentID, CMInfo cmInfo)
	{
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_FILE_TRANSFER_CHAN);
		fe.setUserName(myself.getName());	// requester name
		fe.setFileName(strFileName);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strFileOwner, cmInfo);
		
		fe = null;
		return;
	}
	
	
	public static void pushFile(String strFilePath, String strReceiver, CMInfo cmInfo)
	{
		pushFile(strFilePath, strReceiver, -1, cmInfo);
		return;
	}
	
	public static void pushFile(String strFilePath, String strReceiver, int nContentID, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN 
				&& myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMFileTransferManager.pushFile(), Client must log in to the default server.");
			return;
		}
		
		if(confInfo.isFileTransferScheme())
			pushFileWithSepChannel(strFilePath, strReceiver, nContentID, cmInfo);
		else
			pushFileWithDefChannel(strFilePath, strReceiver, nContentID, cmInfo);
		return;
	}

	// strFilePath: absolute or relative path to a target file
	public static void pushFileWithDefChannel(String strFilePath, String strReceiver, int nContentID, CMInfo cmInfo)
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
		// receiver name, file path, size
		fInfo.addSendFileInfo(strReceiver, strFilePath, lFileSize, nContentID);

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
	
	// strFilePath: absolute or relative path to a target file
	public static void pushFileWithSepChannel(String strFilePath, String strReceiver, int nContentID, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();

		// check the creation of the default blocking TCP socket channel
		CMChannelInfo<Integer> blockChannelList = null;
		CMChannelInfo<Integer> nonBlockChannelList = null;
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockChannelList = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
			nonBlockChannelList = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
			dsc = (SocketChannel) nonBlockChannelList.findChannel(0); // key for the default TCP socket channel is 0
		}
		else	// SERVER
		{
			CMUser user = interInfo.getLoginUsers().findMember(strReceiver);
			blockChannelList = user.getBlockSocketChannelInfo();
			sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
			nonBlockChannelList = user.getNonBlockSocketChannelInfo();
			dsc = (SocketChannel) nonBlockChannelList.findChannel(0);	// key for the default TCP socket channel is 0
		}

		if(sc == null)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default blocking TCP socket channel not found!");
			return;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default blocking TCP socket channel closed!");
			return;
		}
		
		if(dsc == null)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default TCP socket channel not found!");
			return;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default TCP socket channel closed!");
			return;
		}


		// get file information (size)
		File file = new File(strFilePath);
		if(!file.exists())
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(), file("+strFilePath+") does not exists.");
			return;
		}
		long lFileSize = file.length();
		
		// add send file information
		// sender name, receiver name, file path, size, content ID
		CMSendFileInfo sfInfo = new CMSendFileInfo();
		sfInfo.setSenderName(interInfo.getMyself().getName());
		sfInfo.setReceiverName(strReceiver);
		sfInfo.setFilePath(strFilePath);
		sfInfo.setFileSize(lFileSize);
		sfInfo.setContentID(nContentID);
		sfInfo.setSendChannel(sc);
		sfInfo.setDefaultChannel(dsc);
		//boolean bResult = fInfo.addSendFileInfo(strReceiver, strFilePath, lFileSize, nContentID);
		boolean bResult = fInfo.addSendFileInfo(sfInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); error for adding the sending file info: "
					+"receiver("+strReceiver+"), file("+strFilePath+"), size("+lFileSize+"), content ID("
					+nContentID+")!");
			return;
		}

		// get my name
		String strMyName = interInfo.getMyself().getName();

		// get file name
		String strFileName = getFileNameFromPath(strFilePath);
		
		// start file transfer process
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.START_FILE_TRANSFER_CHAN);
		fe.setSenderName(strMyName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);

		file = null;
		fe = null;
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
		/*
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
		*/
		index = strPath.lastIndexOf(sep);
		if(index == -1)
			strName = strPath;
		else
			strName = strPath.substring(index+1);
		
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
		case CMFileEvent.REQUEST_FILE_TRANSFER_CHAN:
			processREQUEST_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.REPLY_FILE_TRANSFER_CHAN:
			processREPLY_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER_CHAN:
			processSTART_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER_CHAN_ACK:
			processSTART_FILE_TRANSFER_CHAN_ACK(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER_CHAN:
			processEND_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER_CHAN_ACK:
			processEND_FILE_TRANSFER_CHAN_ACK(fe, cmInfo);
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
		String strFullPath = fInfo.getFilePath() + File.separator + strFileName; 
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
		// receiver name, file path, size
		fInfo.addSendFileInfo(fe.getUserName(), strFullPath, lFileSize, fe.getContentID());

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
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + File.separator + fe.getSenderName();
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
			
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
		}
		
		
		// open a file output stream
		RandomAccessFile writeFile;
		try {
			writeFile = new RandomAccessFile(strFullPath, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		
		// init received size
		long lRecvSize = 0;
		// add the received file info in the push list
		fInfo.addRecvFileInfo(fe.getSenderName(), fe.getFileName(), lFileSize, fe.getContentID(), lRecvSize, writeFile);
		
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
	
	private static void processSTART_FILE_TRANSFER_ACK(CMFileEvent recvFileEvent, CMInfo cmInfo)
	{
		String strReceiver = null;
		String strFileName = null;
		String strFullFileName = null;
		long lFileSize = -1;
		int nContentID = -1;
		String strSenderName = null;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMSendFileInfo sInfo = null;
		
		// find the CMSendFileInfo object 
		sInfo = fInfo.findSendFileInfo(recvFileEvent.getUserName(), recvFileEvent.getFileName(), 
				recvFileEvent.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), sendFileInfo not found! : "
					+"receiver("+recvFileEvent.getUserName()+"), file("+recvFileEvent.getFileName()
					+"), content ID("+recvFileEvent.getContentID()+")");
			return;
		}
		
		strReceiver = sInfo.getReceiverName();
		strFullFileName = sInfo.getFilePath();
		strFileName = getFileNameFromPath(strFullFileName);
		lFileSize = sInfo.getFileSize();
		nContentID = sInfo.getContentID();
					
		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "Sending file("+strFileName+") to target("+strReceiver+").");

		// open the file
		try {
			sInfo.setReadFile(new RandomAccessFile(strFullFileName, "rw"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// set sender name
		strSenderName = cmInfo.getInteractionInfo().getMyself().getName();

		// send blocks
		long lRemainBytes = lFileSize;
		int nReadBytes = 0;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		CMFileEvent fe = new CMFileEvent();
		
		while(lRemainBytes > 0)
		{
			try {
				nReadBytes = sInfo.getReadFile().read(fileBlock);
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
			CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
			
			lRemainBytes -= nReadBytes;
		}
		
		// close fis
		try {
			sInfo.getReadFile().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "Ending transfer of file("+strFileName+") to target("+strReceiver
					+"), size("+lFileSize+") Bytes.");

		// send the end of file transfer
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.END_FILE_TRANSFER);
		fe.setSenderName(strSenderName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
		
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
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		if( recvInfo == null )
		{
			System.err.println("CMFileTransferManager.processCONTINUE_FILE_TRANSFER(), "
					+ "recv file info for sender("+fe.getSenderName()+"), file("+fe.getFileName()
					+"), content ID("+fe.getContentID()+") not found.");
			return;
		}

		try {
			recvInfo.getWriteFile().write(fe.getFileBlock(), 0, fe.getBlockSize());
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
		
		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER(), recv file info "
					+"for sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), content ID("
					+fe.getContentID()+") not found.");
			return;
		}
		// close received file descriptor
		try {
			recvInfo.getWriteFile().close();
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
		fInfo.removeRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		
		// send ack
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_ACK);
		feAck.setUserName(interInfo.getMyself().getName());
		feAck.setFileName(fe.getFileName());
		feAck.setReturnCode(1);	// success
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;
		
		CMSNSManager.checkCompleteRecvAttachedFiles(fe, cmInfo);

		return;
	}
	
	private static void processEND_FILE_TRANSFER_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		String strReceiverName = fe.getUserName();
		String strFileName = fe.getFileName();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strReceiverName, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), send info not found");
			System.err.println("receiver("+strReceiverName+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendFileInfo(strReceiverName, strFileName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), receiver("
					+strReceiverName+"), file("+strFileName+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		CMSNSManager.checkCompleteSendAttachedFiles(fe, cmInfo);
		
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
	
	private static void processREQUEST_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_FILE_TRANSFER_CHAN(), requester("
					+fe.getUserName()+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()
					+").");
		}

		String strFileName = fe.getFileName();
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.REPLY_FILE_TRANSFER_CHAN);
		feAck.setFileName(strFileName);

		// get the full path of the requested file
		String strFullPath = fInfo.getFilePath() + File.separator + strFileName; 
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
		
		pushFileWithSepChannel(strFullPath, fe.getUserName(), fe.getContentID(), cmInfo);
		return;
	}
	
	private static void processREPLY_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileManager.processREPLY_FILE_TRANSFER_CHAN(), file("+fe.getFileName()
					+"), return code("+fe.getReturnCode()+"), contentID("+fe.getContentID()+").");
		}
		return;
	}
	
	private static void processSTART_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN(),");
			System.out.println("sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), size("
					+fe.getFileSize()+"), contentID("+fe.getContentID()+").");
		}
		
		// set file size
		long lFileSize = fe.getFileSize();
		
		// set a path of the received file
		String strFullPath = fInfo.getFilePath();
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + File.separator + fe.getSenderName();
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
					System.err.println("A sub-directory cannot be created!");
					return;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
		}		
		
		// get the default blocking TCP socket channel
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))	// CLIENT
		{
			CMServer serverInfo = cmInfo.getInteractionInfo().getDefaultServerInfo();
			sc = (SocketChannel) serverInfo.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) serverInfo.getNonBlockSocketChannelInfo().findChannel(0);
		}
		else	// SERVER
		{
			CMUser user = cmInfo.getInteractionInfo().getLoginUsers().findMember(fe.getSenderName());
			sc = (SocketChannel) user.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) user.getNonBlockSocketChannelInfo().findChannel(0);
		}
		
		if(sc == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default blocking TCP socket channel not found!");
			return;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default blocking TCP socket channel is closed!");
			return;
		}
		
		if(dsc == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default TCP socket channel not found!");
			return;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default TCP socket channel is closed!");
			return;
		}

		// check the existing file
		File file = new File(strFullPath);
		long lRecvSize = 0;
		if(file.exists())
		{
			// init received file size
			lRecvSize = file.length();
		}

		// add the received file info
		boolean bResult = false;
		CMRecvFileInfo rfInfo = new CMRecvFileInfo();
		rfInfo.setSenderName(fe.getSenderName());
		rfInfo.setReceiverName(cmInfo.getInteractionInfo().getMyself().getName());
		rfInfo.setFileName(fe.getFileName());
		rfInfo.setFilePath(strFullPath);
		rfInfo.setFileSize(lFileSize);
		rfInfo.setContentID(fe.getContentID());
		rfInfo.setRecvSize(lRecvSize);
		//rfInfo.setWriteFile(raf);
		rfInfo.setRecvChannel(sc);
		rfInfo.setDefaultChannel(dsc);
		
		bResult = fInfo.addRecvFileInfo(rfInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN(); failed to add "
					+ "the receiving file info!");
			return;
		}
		
		if(!fInfo.isRecvOngoing(fe.getSenderName()))
		{
			sendSTART_FILE_TRANSFER_CHAN_ACK(rfInfo, cmInfo);
			/*
			// start a dedicated thread to receive the file
			Future<CMRecvFileInfo> future = null;
			CMRecvFileTask recvFileTask = new CMRecvFileTask(rfInfo);
			future = fInfo.getExecutorService().submit(recvFileTask, rfInfo);
			rfInfo.setRecvTaskResult(future);
			
			// send ack event
			CMFileEvent feAck = new CMFileEvent();
			feAck.setID(CMFileEvent.START_FILE_TRANSFER_CHAN_ACK);
			feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
			feAck.setFileName(fe.getFileName());
			feAck.setContentID(fe.getContentID());
			feAck.setReceivedFileSize(lRecvSize);
			CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);

			feAck = null;
			*/
		}
				
		return;
	}
	
	private static void processSTART_FILE_TRANSFER_CHAN_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		String strReceiver = null;
		String strFileName = null;
		String strFullFileName = null;
		long lFileSize = -1;	// file size
		int nContentID = -1;
		long lRecvSize = -1;	// received size by the receiver
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMSendFileInfo sInfo = null;
		
		// find the CMSendFileInfo object 
		sInfo = fInfo.findSendFileInfo(fe.getUserName(), fe.getFileName(), fe.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN_ACK(), sendFileInfo "
					+ "not found! : receiver("+fe.getUserName()+"), file("+fe.getFileName()
					+"), content ID("+fe.getContentID()+")");
			return;
		}
		
		strReceiver = sInfo.getReceiverName();
		strFullFileName = sInfo.getFilePath();
		strFileName = getFileNameFromPath(strFullFileName);
		lFileSize = sInfo.getFileSize();
		nContentID = sInfo.getContentID();
		
		lRecvSize = fe.getReceivedFileSize();
		if(lRecvSize > 0)
			sInfo.setSentSize(lRecvSize);	// update the sent size 
					
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN_ACK(); "
					+ "receiver("+strReceiver+"), file path("+strFullFileName+"), file name("+strFileName
					+ "), file size("+lFileSize+"), content ID("+nContentID+").");
			System.out.println("already received file size by the receiver("+lRecvSize+").");
		}

		// start a dedicated sending thread
		Future<CMSendFileInfo> future = null;
		CMSendFileTask sendFileTask = new CMSendFileTask(sInfo);
		future = fInfo.getExecutorService().submit(sendFileTask, sInfo);
		sInfo.setSendTaskResult(future);		

		return;		
	}
	
	private static void processEND_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		boolean bResult = false;

		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), recv file info "
					+"for sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), content ID("
					+fe.getContentID()+") not found.");
			return;
		}

		// wait the receiving thread
		if(!recvInfo.getRecvTaskResult().isDone())
		{
			try {
				recvInfo.getRecvTaskResult().get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), sender("+fe.getSenderName()
					+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()+"), file size("
					+recvInfo.getFileSize()+"), received size("+recvInfo.getRecvSize()+").");
		}

		// make ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_CHAN_ACK);
		feAck.setUserName(interInfo.getMyself().getName());
		feAck.setFileName(fe.getFileName());
		feAck.setContentID(fe.getContentID());

		// check out whether the file is completely received
		if(recvInfo.getFileSize() == recvInfo.getRecvSize())
		{
			feAck.setReturnCode(1);	// success
			bResult = true;
		}
		else
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(); incompletely received!");
			feAck.setReturnCode(0); // failure
			bResult = false;
		}
		
		// remove info from push file list
		fInfo.removeRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		
		// send ack
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;

		if(bResult)
			CMSNSManager.checkCompleteRecvAttachedFiles(fe, cmInfo);

		// check whether there is a remaining receiving file info or not
		CMRecvFileInfo nextRecvInfo = fInfo.findRecvFileInfoNotStarted(fe.getSenderName());
		if(nextRecvInfo != null)
		{
			sendSTART_FILE_TRANSFER_CHAN_ACK(nextRecvInfo, cmInfo);
		}
		
		return;
	}
	
	private static void processEND_FILE_TRANSFER_CHAN_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		String strReceiverName = fe.getUserName();
		String strFileName = fe.getFileName();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strReceiverName, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN_ACK(), send info not found");
			System.err.println("receiver("+strReceiverName+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendFileInfo(strReceiverName, strFileName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN_ACK(), receiver("
					+strReceiverName+"), file("+strFileName+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		CMSNSManager.checkCompleteSendAttachedFiles(fe, cmInfo);

		return;	
	}
	
	private static void sendSTART_FILE_TRANSFER_CHAN_ACK(CMRecvFileInfo rfInfo, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();

		// start a dedicated thread to receive the file
		Future<CMRecvFileInfo> future = null;
		CMRecvFileTask recvFileTask = new CMRecvFileTask(rfInfo);
		future = fInfo.getExecutorService().submit(recvFileTask, rfInfo);
		rfInfo.setRecvTaskResult(future);
		
		// send ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.START_FILE_TRANSFER_CHAN_ACK);
		feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
		feAck.setFileName(rfInfo.getFileName());
		feAck.setContentID(rfInfo.getContentID());
		feAck.setReceivedFileSize(rfInfo.getRecvSize());
		CMEventManager.unicastEvent(feAck, rfInfo.getSenderName(), cmInfo);

		feAck = null;
	}
	
}
