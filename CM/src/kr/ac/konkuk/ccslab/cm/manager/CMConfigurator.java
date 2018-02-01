package kr.ac.konkuk.ccslab.cm.manager;
import java.io.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMConfigurator {
	
	// initialize field values of server configuration or client configuration
	public static void init(String fName, CMInfo cmInfo) throws IOException
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		
		confInfo.setConfFileName(fName);

		confInfo.setSystemType(CMConfigurator.getConfiguration(fName, "SYS_TYPE"));
		confInfo.setServerAddress(CMConfigurator.getConfiguration(fName, "SERVER_ADDR"));
		confInfo.setServerPort(Integer.parseInt(CMConfigurator.getConfiguration(fName, "SERVER_PORT")));
		confInfo.setUDPPort(Integer.parseInt(CMConfigurator.getConfiguration(fName, "UDP_PORT")));
		confInfo.setMulticastAddress(CMConfigurator.getConfiguration(fName, "MULTICAST_ADDR"));
		confInfo.setMulticastPort(Integer.parseInt(CMConfigurator.getConfiguration(fName, "MULTICAST_PORT")));
		//confInfo.setMyAddress(InetAddress.getLocalHost().getHostAddress());
		if(confInfo.getServerAddress().equals("localhost"))
		{
			confInfo.setMyAddress("localhost");
		}
		else if(confInfo.getServerAddress().equals("127.0.0.1"))
		{
			confInfo.setMyAddress("127.0.0.1");
		}
		else
		{
			confInfo.setMyAddress(CMCommManager.getLocalIP());
		}
				
		// default download directory
		String strFilePath = CMConfigurator.getConfiguration(fName, "FILE_PATH");
		if(strFilePath == null) strFilePath = "."; // if no default path is set, it is set to the current working directory
		confInfo.setFilePath(strFilePath);
		
		// default append mode for the file transfer
		confInfo.setFileAppendScheme(Byte.parseByte(CMConfigurator.getConfiguration(fName, "FILE_APPEND_SCHEME")));
		
		// added delay value for the simulation of transmission delay
		confInfo.setSimTransDelay(Integer.parseInt(CMConfigurator.getConfiguration(fName, "SIM_TRANS_DELAY")));

		if( confInfo.getSystemType().equals("SERVER") )
		{
			//confInfo.setMyAddress(CMConfigurator.getConfiguration(fName, "MY_ADDR"));
			confInfo.setMyPort(Integer.parseInt(CMConfigurator.getConfiguration(fName, "MY_PORT")));
			confInfo.setCommArch(CMConfigurator.getConfiguration(fName, "COMM_ARCH"));
			confInfo.setFileTransferScheme(Integer.parseInt(CMConfigurator.getConfiguration(fName, "FILE_TRANSFER_SCHEME")));
			confInfo.setLoginScheme(Integer.parseInt(CMConfigurator.getConfiguration(fName, "LOGIN_SCHEME")));
			confInfo.setSessionScheme(Integer.parseInt(CMConfigurator.getConfiguration(fName, "SESSION_SCHEME")));
			confInfo.setDownloadScheme(Integer.parseInt(CMConfigurator.getConfiguration(fName, "DOWNLOAD_SCHEME")));
			confInfo.setDownloadNum(Integer.parseInt(CMConfigurator.getConfiguration(fName, "DOWNLOAD_NUM")));
			confInfo.setThumbnailHorSize(Integer.parseInt(CMConfigurator.getConfiguration(fName, "THUMBNAIL_HOR_SIZE")));
			confInfo.setThumbnailVerSize(Integer.parseInt(CMConfigurator.getConfiguration(fName, "THUMBNAIL_VER_SIZE")));
			confInfo.setAttachDownloadScheme(Integer.parseInt(CMConfigurator.getConfiguration(fName, "ATTACH_DOWNLOAD_SCHEME")));
			confInfo.setAttachAccessInterval(Integer.parseInt(CMConfigurator.getConfiguration(fName, "ATTACH_ACCESS_INTERVAL")));
			confInfo.setAttachPrefetchThreshold(Double.parseDouble(CMConfigurator.getConfiguration(fName, "ATTACH_PREFETCH_THRESHOLD")));
			confInfo.setSessionNumber(Integer.parseInt(CMConfigurator.getConfiguration(fName, "SESSION_NUM")));
			
			// store session configuration file names
			if(confInfo.getSessionNumber() > 0)
			{
				confInfo.getSessionConfFileList().clear();
				
				for(int i = 1; i <= confInfo.getSessionNumber(); i++)
				{
					String strFileField = "SESSION_FILE"+i;
					String strFileName = CMConfigurator.getConfiguration(fName, strFileField);
					confInfo.getSessionConfFileList().addElement(strFileName);
				}
			}
			
			// DB configuration
			confInfo.setDBUse(Integer.parseInt(CMConfigurator.getConfiguration(fName, "DB_USE")));
			confInfo.setDBHost(CMConfigurator.getConfiguration(fName, "DB_HOST"));
			confInfo.setDBUser(CMConfigurator.getConfiguration(fName, "DB_USER"));
			confInfo.setDBPass(CMConfigurator.getConfiguration(fName, "DB_PASS"));
			confInfo.setDBPort(Integer.parseInt(CMConfigurator.getConfiguration(fName, "DB_PORT")));
			confInfo.setDBName(CMConfigurator.getConfiguration(fName, "DB_NAME"));
		}
		else if( confInfo.getSystemType().equals("CLIENT") )
		{
			//confInfo.setMyAddress(InetAddress.getLocalHost().getHostAddress());
		}


		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMConfigurator.init(), Ok");
			System.out.println("SYS_TYPE: "+confInfo.getSystemType());
			System.out.println("SERVER_ADDR: "+confInfo.getServerAddress());
			System.out.println("SERVER_PORT: "+confInfo.getServerPort());
			System.out.println("UDP_PORT: "+confInfo.getUDPPort());
			System.out.println("MULTICAST_ADDR: "+confInfo.getMulticastAddress());
			System.out.println("MULTICAST_PORT: "+confInfo.getMulticastPort());
			System.out.println("MY_ADDR: "+confInfo.getMyAddress());
			System.out.println("FILE_PATH: "+confInfo.getFilePath());
			System.out.println("FILE_APPEND_SCHEME: "+confInfo.isFileAppendScheme());
			System.out.println("SIM_TRANS_DELAY: "+confInfo.getSimTransDelay());
			if( confInfo.getSystemType().equals("SERVER") )
			{
				//System.out.println("MY_ADDR: "+confInfo.getMyAddress());
				System.out.println("MY_PORT: "+confInfo.getMyPort());
				System.out.println("COMM_ARCH: "+confInfo.getCommArch());
				System.out.println("FILE_TRANSFER_SCHEME: "+confInfo.isFileTransferScheme());
				System.out.println("LOGIN_SCHEME: "+confInfo.isLoginScheme());
				System.out.println("SESSION_SCHEME: "+confInfo.isSessionScheme());
				System.out.println("DOWNLOAD_SCHEME: "+confInfo.isDownloadScheme());
				System.out.println("DOWNLOAD_NUM: "+confInfo.getDownloadNum());
				System.out.println("THUMBNAIL_HOR_SIZE: "+confInfo.getThumbnailHorSize());
				System.out.println("THUMBNAIL_VER_SIZE: "+confInfo.getThumbnailVerSize());
				System.out.println("ATTACH_DOWNLOAD_SCHEME: "+confInfo.getAttachDownloadScheme());
				System.out.println("ATTACH_ACCESS_INTERVAL: "+confInfo.getAttachAccessInterval());
				System.out.println("ATTACH_PREFETCH_THRESHOLD: "+confInfo.getAttachPrefetchThreshold());
				System.out.println("SESSION_NUM: "+confInfo.getSessionNumber());
				
				// session configuration file list
				System.out.print("session conf files: ");
				if(!confInfo.getSessionConfFileList().isEmpty())
				{
					Iterator<String> iter = confInfo.getSessionConfFileList().iterator();
					while(iter.hasNext())
					{
						System.out.print(iter.next()+" ");
					}
				}
				System.out.println();

				// DB configuration
				System.out.println("DB_USE: "+confInfo.isDBUse());
				System.out.println("DB_HOST: "+confInfo.getDBHost());
				System.out.println("DB_USER: "+confInfo.getDBUser());
				System.out.println("DB_PASS: "+confInfo.getDBPass());
				System.out.println("DB_PORT: "+confInfo.getDBPort());
				System.out.println("DB_NAME: "+confInfo.getDBName());

				if( CMConfigurator.isDServer(cmInfo) )
					System.out.println("This server is a default server!!");
				else
					System.out.println("This server is NOT a default server!!");
			}
			else if(confInfo.getSystemType().equals("CLIENT"))
			{
				//System.out.println("MY_ADDR: "+confInfo.getMyAddress());
			}
		}
	}
	
	public static String getConfiguration(String fileName, String fieldName) throws IOException
	{
		FileInputStream fis = new FileInputStream(fileName);
		String strLine = null;
		String strValue = null;
		String[] strToken = null;
		String delim = "\\s+";
		Scanner scan = new Scanner(fis);

		while(scan.hasNextLine() && strValue == null)
		{
			strLine = scan.nextLine();
			if(strLine.equals("") || strLine.charAt(0) == '#' || strLine.charAt(0) == '!')
				continue;
			
			strToken = strLine.split(delim);
			if(strToken[0].equals(fieldName))
				strValue = strToken[1];
		}

		fis.close();
		scan.close();
		
		return strValue;
	}

	public static boolean isDServer(CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		boolean ret = false;
		String strServerAddress = confInfo.getServerAddress();
		int nServerPort = confInfo.getServerPort();
		String strMyAddress = confInfo.getMyAddress();
		int nMyPort = confInfo.getMyPort();
		
		// if server info is initialized and two server info is the same

		if( strServerAddress.compareTo("") != 0 && nServerPort != -1	
				&& strServerAddress.equals(strMyAddress) && nServerPort == nMyPort )
			ret = true;
		
		return ret;
	}
	
}
