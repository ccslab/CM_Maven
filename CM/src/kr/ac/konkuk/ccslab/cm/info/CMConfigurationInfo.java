package kr.ac.konkuk.ccslab.cm.info;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CMConfigurationInfo {
	private Path m_confFileHome;
	
	private int m_nMulticastPort;
	private int m_nServerPort;
	private int m_nMyPort;
	private String m_strMulticastAddress;
	private String m_strServerAddress;
	private String m_strMyAddress;
	private String m_strSystemType;
	private String m_strCommArch;
	private int m_bLoginScheme;
	private int m_bSessionScheme;
	private int m_nUDPPort;
	private int m_nSessionNumber;
	private int m_bDownloadScheme;
	private int m_nDownloadNum;
	private int m_nThumbnailHorSize;
	private int m_nThumbnailVerSize;
	private int m_nAttachDownloadScheme;
	private int m_nAttachAccessInterval;
	private double m_dAttachPrefetchThreshold;
	private Vector<String> m_sessionConfFileList;

	// DB configuration
	private int m_bDBUse;
	private String m_strDBHost;
	private String m_strDBUser;
	private String m_strDBPass;
	private int m_nDBPort;
	private String m_strDBName;
	
	// Default path for file transfer
	private Path m_transFileHome;
	// File transfer scheme
	private int m_bFileTransferScheme;
	// File append scheme
	private byte m_bFileAppendScheme;	
	
	// Simulation parameter for added transmission delay
	private int m_nSimTransDelay;
	
	public CMConfigurationInfo()
	{
		m_confFileHome = Paths.get(".");
		m_strSystemType = "";
		m_strCommArch = "";
		m_nServerPort = -1;
		m_nMyPort = -1;
		m_nUDPPort = -1;
		m_nMulticastPort = -1;
		m_strServerAddress = "";
		m_strMyAddress = null;
		m_strMulticastAddress = "";
		m_nSessionNumber = 0;
		m_bLoginScheme = 0;
		m_bSessionScheme = 0;
		m_bDownloadScheme = 0;
		m_nDownloadNum = 0;
		m_nThumbnailHorSize = 0;
		m_nThumbnailVerSize = 0;
		m_nAttachDownloadScheme = 0;
		m_nAttachAccessInterval = 0;
		m_dAttachPrefetchThreshold = 0.0;
		m_sessionConfFileList = new Vector<String>();

		m_bDBUse = 0;
		m_strDBHost = "";
		m_strDBUser = "";
		m_strDBPass = "";
		m_nDBPort = -1;
		m_strDBName = "";
		
		m_transFileHome = Paths.get(".");
		m_bFileTransferScheme = 0;
		m_bFileAppendScheme = 0;
		
		m_nSimTransDelay = 0;
	}

	// set/get methods
	public void setConfFileHome(Path filePath)
	{
		m_confFileHome = filePath;
	}
	
	public Path getConfFileHome()
	{
		return m_confFileHome;
	}
	
	public void setServerAddress(String addr)
	{
		m_strServerAddress = addr;
	}
	
	public String getServerAddress()
	{
		return m_strServerAddress;
	}
	
	public void setServerPort(int port)
	{
		m_nServerPort = port;
	}
	
	public int getServerPort()
	{
		return m_nServerPort;
	}
	
	public void setMyAddress(String addr)
	{
		m_strMyAddress = addr;
	}
	
	public String getMyAddress()
	{
		return m_strMyAddress;
	}
	
	public void setMyPort(int port)
	{
		m_nMyPort = port;
	}
	
	public int getMyPort()
	{
		return m_nMyPort;
	}
	
	public void setMulticastAddress(String addr)
	{
		m_strMulticastAddress = addr;
	}
	
	public String getMulticastAddress()
	{
		return m_strMulticastAddress;
	}
	
	public void setMulticastPort(int port)
	{
		m_nMulticastPort = port;
	}
	
	public int getMulticastPort()
	{
		return m_nMulticastPort;
	}
	
	public void setSystemType(String type)
	{
		m_strSystemType = type;
	}
	
	public String getSystemType()
	{
		return m_strSystemType;
	}
	
	public void setCommArch(String commArch)
	{
		m_strCommArch = commArch;
	}
	
	public String getCommArch()
	{
		return m_strCommArch;
	}
	
	public void setLoginScheme(int bScheme)
	{
		m_bLoginScheme = bScheme;
	}
	
	public void setLoginScheme(boolean bScheme)
	{
		if(bScheme) 
			m_bLoginScheme = 1;
		else
			m_bLoginScheme = 0;
	}
	
	public boolean isLoginScheme()
	{
		boolean bScheme = false;
		if(m_bLoginScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public void setSessionScheme(int bScheme)
	{
		m_bSessionScheme = bScheme;
	}
	
	public void setSessionScheme(boolean bScheme)
	{
		if(bScheme)
			m_bSessionScheme = 1;
		else
			m_bSessionScheme = 0;
	}
	
	public boolean isSessionScheme()
	{
		boolean bScheme = false;
		if(m_bSessionScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public void setDownloadScheme(int bScheme)
	{
		m_bDownloadScheme = bScheme;
	}
	
	public void setDownloadScheme(boolean bScheme)
	{
		if(bScheme)
			m_bDownloadScheme = 1;
		else
			m_bDownloadScheme = 0;
	}
	
	public boolean isDownloadScheme()
	{
		boolean bScheme = false;
		
		if(m_bDownloadScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public void setDownloadNum(int num)
	{
		m_nDownloadNum = num;
	}
	
	public int getDownloadNum()
	{
		return m_nDownloadNum;
	}
	
	public void setThumbnailHorSize(int nHorizon)
	{
		m_nThumbnailHorSize = nHorizon;
	}
	
	public int getThumbnailHorSize()
	{
		return m_nThumbnailHorSize;
	}
	
	public void setThumbnailVerSize(int nVertical)
	{
		m_nThumbnailVerSize = nVertical;
	}
	
	public int getThumbnailVerSize()
	{
		return m_nThumbnailVerSize;
	}
	
	public void setAttachDownloadScheme(int nScheme)
	{
		m_nAttachDownloadScheme = nScheme;
	}
	
	public int getAttachDownloadScheme()
	{
		return m_nAttachDownloadScheme;
	}
	
	public void setAttachAccessInterval(int nInterval)
	{
		m_nAttachAccessInterval = nInterval;
	}
	
	public int getAttachAccessInterval()
	{
		return m_nAttachAccessInterval;
	}
	
	public void setAttachPrefetchThreshold(double dThreshold)
	{
		m_dAttachPrefetchThreshold = dThreshold;
	}
	
	public double getAttachPrefetchThreshold()
	{
		return m_dAttachPrefetchThreshold;
	}
	
	public void setUDPPort(int port)
	{
		m_nUDPPort = port;
	}
	
	public int getUDPPort()
	{
		return m_nUDPPort;
	}
	
	public void setSessionNumber(int num)
	{
		m_nSessionNumber = num;
	}
	
	public int getSessionNumber()
	{
		return m_nSessionNumber;
	}
	
	public Vector<String> getSessionConfFileList()
	{
		return m_sessionConfFileList;
	}
	
	////////////////////////////////////////////
	//	DB information
	public void setDBUse(int bUse)
	{
		m_bDBUse = bUse;
	}
	
	public void setDBUse(boolean bUse)
	{
		if(bUse)
			m_bDBUse = 1;
		else
			m_bDBUse = 0;
	}
	
	public boolean isDBUse()
	{
		boolean bUse = false;
		
		if(m_bDBUse == 0)
			bUse = false;
		else
			bUse = true;
		
		return bUse;
	}
	
	public void setDBHost(String host)
	{
		m_strDBHost = host;
	}
	
	public String getDBHost()
	{
		return m_strDBHost;
	}
	
	public void setDBUser(String user)
	{
		m_strDBUser = user;
	}
	
	public String getDBUser()
	{
		return m_strDBUser;
	}
	
	public void setDBPass(String pass)
	{
		m_strDBPass = pass;
	}
	
	public String getDBPass()
	{
		return m_strDBPass;
	}
	
	public void setDBPort(int port)
	{
		m_nDBPort = port;
	}
	
	public int getDBPort()
	{
		return m_nDBPort;
	}
	
	public void setDBName(String name)
	{
		m_strDBName = name;
	}
	
	public String getDBName()
	{
		return m_strDBName;
	}

	/////////////////////////////////////////////////////////////////////
	
	public void setTransferedFileHome(Path filePath)
	{
		m_transFileHome = filePath;
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMConfigurationInfo.setTransferedFileHome(): "+m_transFileHome.toString());
		
		return;
	}
	
	public Path getTransferedFileHome()
	{
		return m_transFileHome;
	}
	
	public void setFileTransferScheme(int bScheme)
	{
		m_bFileTransferScheme = bScheme;
	}
	
	public boolean isFileTransferScheme()
	{
		boolean bScheme = false;
		
		if(m_bFileTransferScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;		
	}
	
	public void setFileAppendScheme(byte bScheme)
	{
		m_bFileAppendScheme = bScheme;
	}
	
	public void setFileAppendScheme(boolean bScheme)
	{
		if(bScheme)
			m_bFileAppendScheme = 1;
		else
			m_bFileAppendScheme = 0;
		
		return;
	}
	
	public boolean isFileAppendScheme()
	{
		boolean bScheme = false;
		
		if(m_bFileAppendScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	/////////////////////////////////////////////////////////////////////
	
	public void setSimTransDelay(int nDelay)
	{
		m_nSimTransDelay = nDelay;
	}
	
	public int getSimTransDelay()
	{
		return m_nSimTransDelay;
	}
}
