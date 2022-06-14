package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncMode;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncUpdateMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CMConfigurationInfo {
	private Path m_confFileHome;
	
	private int m_nMulticastPort;
	private int m_nServerPort;
	private int m_nMyPort;
	private String m_strMulticastAddress;
	private String m_strServerAddress;
	private List<String> m_myAddressList;
	private String m_strMyCurrentAddress;
	private String m_strSystemType;
	private String m_strCommArch;
	private int m_bLoginScheme;
	private int m_nMaxLoginFailure;
	private int m_nKeepAliveTime;
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
	// automatic permission of file-transfer request
	private int m_bPermitFileTransferRequest;
	
	// Simulation parameter for added transmission delay
	private int m_nSimTransDelay;
	// log level
	private int m_nLogLevel;

	////////// file-sync (4 client)
	// file-sync mode
	private CMFileSyncMode fileSyncMode;
	// monitoring period of directory activation ratio (DAMP)
	private long dirActivationMonitoringPeriod;
	// monitoring period unit of directory activation ratio (TimeUnit string)
	private TimeUnit dirActivationMonitoringPeriodUnit;
	// duration-since-last-access threshold (DSLAT)
	private long durationSinceLastAccessThreshold;
	// duration-since-last-access threshold unit (TimeUnit string)
	private TimeUnit durationSinceLastAccessThresholdUnit;
	// threshold of proactive online-mode (0~1)
	private double onlineModeThreshold;
	// threshold of proactive local-mode (0~1)
	private double localModeThreshold;
	// file-sync storage (FSS) (MB)
	private long fileSyncStorage;
	// used storage ratio threshold (USRT) (0~1)
	private double usedStorageRatioThreshold;
	// max access delay threshold (MADT) (milliseconds)
	private long maxAccessDelayThreshold;
	// file-sync update mode (DELTA, FILE, HYBRID)
	private CMFileSyncUpdateMode fileSyncUpdateMode;
	// file size threshold (for HYBRID mode of file-sync update) (Bytes)
	private long fileSizeThreshold;
	// file modification ratio threshold (for HYBRID mode of file-sync update) (0~1)
	private double fileModRatioThreshold;
	
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
		m_myAddressList = null;
		m_strMyCurrentAddress = "";
		m_strMulticastAddress = "";
		m_nSessionNumber = 0;
		m_bLoginScheme = 0;
		m_nMaxLoginFailure = 0;
		m_nKeepAliveTime = 0;
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
		m_bPermitFileTransferRequest = 0;
		
		m_nSimTransDelay = 0;
		m_nLogLevel = 1;

		fileSyncMode = CMFileSyncMode.OFF;
		dirActivationMonitoringPeriod = 0;
		dirActivationMonitoringPeriodUnit = TimeUnit.DAYS;
		durationSinceLastAccessThreshold = 0;
		durationSinceLastAccessThresholdUnit = TimeUnit.DAYS;
		onlineModeThreshold = 0;
		localModeThreshold = 0;
		fileSyncStorage = 0;
		usedStorageRatioThreshold = 0;
		maxAccessDelayThreshold = 0;
		fileSyncUpdateMode = CMFileSyncUpdateMode.DELTA;
		fileSizeThreshold = 0;
		fileModRatioThreshold = 1;	// always DELTA
	}

	// set/get methods
	public synchronized void setConfFileHome(Path filePath)
	{
		m_confFileHome = filePath;
	}
	
	public synchronized Path getConfFileHome()
	{
		return m_confFileHome;
	}
	
	public synchronized void setServerAddress(String addr)
	{
		m_strServerAddress = addr;
	}
	
	public synchronized String getServerAddress()
	{
		return m_strServerAddress;
	}
	
	public synchronized void setServerPort(int port)
	{
		m_nServerPort = port;
	}
	
	public synchronized int getServerPort()
	{
		return m_nServerPort;
	}
	
	public synchronized void setMyAddressList(List<String> addrList)
	{
		m_myAddressList = addrList;
	}
	
	public synchronized List<String> getMyAddressList()
	{
		return m_myAddressList;
	}
	
	public synchronized void setMyCurrentAddress(String addr)
	{
		m_strMyCurrentAddress = addr;
	}
	
	public synchronized String getMyCurrentAddress()
	{
		return m_strMyCurrentAddress;
	}
	
	public synchronized void setMyPort(int port)
	{
		m_nMyPort = port;
	}
	
	public synchronized int getMyPort()
	{
		return m_nMyPort;
	}
	
	public synchronized void setMulticastAddress(String addr)
	{
		m_strMulticastAddress = addr;
	}
	
	public synchronized String getMulticastAddress()
	{
		return m_strMulticastAddress;
	}
	
	public synchronized void setMulticastPort(int port)
	{
		m_nMulticastPort = port;
	}
	
	public synchronized int getMulticastPort()
	{
		return m_nMulticastPort;
	}
	
	public synchronized void setSystemType(String type)
	{
		m_strSystemType = type;
	}
	
	public synchronized String getSystemType()
	{
		return m_strSystemType;
	}
	
	public synchronized void setCommArch(String commArch)
	{
		m_strCommArch = commArch;
	}
	
	public synchronized String getCommArch()
	{
		return m_strCommArch;
	}
	
	public synchronized void setLoginScheme(int bScheme)
	{
		m_bLoginScheme = bScheme;
	}
	
	public synchronized void setLoginScheme(boolean bScheme)
	{
		if(bScheme) 
			m_bLoginScheme = 1;
		else
			m_bLoginScheme = 0;
	}
	
	public synchronized boolean isLoginScheme()
	{
		boolean bScheme = false;
		if(m_bLoginScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public synchronized void setMaxLoginFailure(int nCount)
	{
		m_nMaxLoginFailure = nCount;
	}
	
	public synchronized int getMaxLoginFailure()
	{
		return m_nMaxLoginFailure;
	}
	
	public synchronized void setKeepAliveTime(int second)
	{
		m_nKeepAliveTime = second;
	}
	
	public synchronized int getKeepAliveTime()
	{
		return m_nKeepAliveTime;
	}
	
	public synchronized void setSessionScheme(int bScheme)
	{
		m_bSessionScheme = bScheme;
	}
	
	public synchronized void setSessionScheme(boolean bScheme)
	{
		if(bScheme)
			m_bSessionScheme = 1;
		else
			m_bSessionScheme = 0;
	}
	
	public synchronized boolean isSessionScheme()
	{
		boolean bScheme = false;
		if(m_bSessionScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public synchronized void setDownloadScheme(int bScheme)
	{
		m_bDownloadScheme = bScheme;
	}
	
	public synchronized void setDownloadScheme(boolean bScheme)
	{
		if(bScheme)
			m_bDownloadScheme = 1;
		else
			m_bDownloadScheme = 0;
	}
	
	public synchronized boolean isDownloadScheme()
	{
		boolean bScheme = false;
		
		if(m_bDownloadScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public synchronized void setDownloadNum(int num)
	{
		m_nDownloadNum = num;
	}
	
	public synchronized int getDownloadNum()
	{
		return m_nDownloadNum;
	}
	
	public synchronized void setThumbnailHorSize(int nHorizon)
	{
		m_nThumbnailHorSize = nHorizon;
	}
	
	public synchronized int getThumbnailHorSize()
	{
		return m_nThumbnailHorSize;
	}
	
	public synchronized void setThumbnailVerSize(int nVertical)
	{
		m_nThumbnailVerSize = nVertical;
	}
	
	public synchronized int getThumbnailVerSize()
	{
		return m_nThumbnailVerSize;
	}
	
	public synchronized void setAttachDownloadScheme(int nScheme)
	{
		m_nAttachDownloadScheme = nScheme;
	}
	
	public synchronized int getAttachDownloadScheme()
	{
		return m_nAttachDownloadScheme;
	}
	
	public synchronized void setAttachAccessInterval(int nInterval)
	{
		m_nAttachAccessInterval = nInterval;
	}
	
	public synchronized int getAttachAccessInterval()
	{
		return m_nAttachAccessInterval;
	}
	
	public synchronized void setAttachPrefetchThreshold(double dThreshold)
	{
		m_dAttachPrefetchThreshold = dThreshold;
	}
	
	public synchronized double getAttachPrefetchThreshold()
	{
		return m_dAttachPrefetchThreshold;
	}
	
	public synchronized void setUDPPort(int port)
	{
		m_nUDPPort = port;
	}
	
	public synchronized int getUDPPort()
	{
		return m_nUDPPort;
	}
	
	public synchronized void setSessionNumber(int num)
	{
		m_nSessionNumber = num;
	}
	
	public synchronized int getSessionNumber()
	{
		return m_nSessionNumber;
	}
	
	public synchronized Vector<String> getSessionConfFileList()
	{
		return m_sessionConfFileList;
	}
	
	////////////////////////////////////////////
	//	DB information
	public synchronized void setDBUse(int bUse)
	{
		m_bDBUse = bUse;
	}
	
	public synchronized void setDBUse(boolean bUse)
	{
		if(bUse)
			m_bDBUse = 1;
		else
			m_bDBUse = 0;
	}
	
	public synchronized boolean isDBUse()
	{
		boolean bUse = false;
		
		if(m_bDBUse == 0)
			bUse = false;
		else
			bUse = true;
		
		return bUse;
	}
	
	public synchronized void setDBHost(String host)
	{
		m_strDBHost = host;
	}
	
	public synchronized String getDBHost()
	{
		return m_strDBHost;
	}
	
	public synchronized void setDBUser(String user)
	{
		m_strDBUser = user;
	}
	
	public synchronized String getDBUser()
	{
		return m_strDBUser;
	}
	
	public synchronized void setDBPass(String pass)
	{
		m_strDBPass = pass;
	}
	
	public synchronized String getDBPass()
	{
		return m_strDBPass;
	}
	
	public synchronized void setDBPort(int port)
	{
		m_nDBPort = port;
	}
	
	public synchronized int getDBPort()
	{
		return m_nDBPort;
	}
	
	public synchronized void setDBName(String name)
	{
		m_strDBName = name;
	}
	
	public synchronized String getDBName()
	{
		return m_strDBName;
	}

	/////////////////////////////////////////////////////////////////////
	
	public synchronized void setTransferedFileHome(Path filePath)
	{
		m_transFileHome = filePath;

		return;
	}
	
	public synchronized Path getTransferedFileHome()
	{
		return m_transFileHome;
	}
	
	public synchronized void setFileTransferScheme(int bScheme)
	{
		m_bFileTransferScheme = bScheme;
	}
	
	public synchronized boolean isFileTransferScheme()
	{
		boolean bScheme = false;
		
		if(m_bFileTransferScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;		
	}
	
	public synchronized void setFileAppendScheme(byte bScheme)
	{
		m_bFileAppendScheme = bScheme;
	}
	
	public synchronized void setFileAppendScheme(boolean bScheme)
	{
		if(bScheme)
			m_bFileAppendScheme = 1;
		else
			m_bFileAppendScheme = 0;
		
		return;
	}
	
	public synchronized boolean isFileAppendScheme()
	{
		boolean bScheme = false;
		
		if(m_bFileAppendScheme == 0)
			bScheme = false;
		else
			bScheme = true;
		
		return bScheme;
	}
	
	public synchronized void setPermitFileTransferRequest(int bPermit)
	{
		m_bPermitFileTransferRequest = bPermit;
		
		return;
	}
	
	public synchronized boolean isPermitFileTransferRequest()
	{
		boolean bPermit = false;
		
		if(m_bPermitFileTransferRequest == 0)
			bPermit = false;
		else
			bPermit = true;
		
		return bPermit;
	}
	
	/////////////////////////////////////////////////////////////////////
	
	public synchronized void setSimTransDelay(int nDelay)
	{
		m_nSimTransDelay = nDelay;
	}
	
	public synchronized int getSimTransDelay()
	{
		return m_nSimTransDelay;
	}
	
	public synchronized void setLogLevel(int nLevel)
	{
		m_nLogLevel = nLevel;
	}
	
	public synchronized int getLogLevel()
	{
		return m_nLogLevel;
	}

	/////////////////////////////////////////////////////////////////////

	public synchronized CMFileSyncMode getFileSyncMode() {
		return fileSyncMode;
	}

	public synchronized void setFileSyncMode(CMFileSyncMode fileSyncMode) {
		this.fileSyncMode = fileSyncMode;
	}

	public synchronized long getDirActivationMonitoringPeriod() {
		return dirActivationMonitoringPeriod;
	}

	public synchronized void setDirActivationMonitoringPeriod(long dirActivationMonitoringPeriod) {
		this.dirActivationMonitoringPeriod = dirActivationMonitoringPeriod;
	}

	public synchronized TimeUnit getDirActivationMonitoringPeriodUnit() {
		return dirActivationMonitoringPeriodUnit;
	}

	public synchronized void setDirActivationMonitoringPeriodUnit(TimeUnit dirActivationMonitoringPeriodUnit) {
		this.dirActivationMonitoringPeriodUnit = dirActivationMonitoringPeriodUnit;
	}

	public synchronized long getDurationSinceLastAccessThreshold() {
		return durationSinceLastAccessThreshold;
	}

	public synchronized void setDurationSinceLastAccessThreshold(long durationSinceLastAccessThreshold) {
		this.durationSinceLastAccessThreshold = durationSinceLastAccessThreshold;
	}

	public synchronized TimeUnit getDurationSinceLastAccessThresholdUnit() {
		return durationSinceLastAccessThresholdUnit;
	}

	public synchronized void setDurationSinceLastAccessThresholdUnit(TimeUnit durationSinceLastAccessThresholdUnit) {
		this.durationSinceLastAccessThresholdUnit = durationSinceLastAccessThresholdUnit;
	}

	public synchronized double getOnlineModeThreshold() {
		return onlineModeThreshold;
	}

	public synchronized void setOnlineModeThreshold(double onlineModeThreshold) {
		this.onlineModeThreshold = onlineModeThreshold;
	}

	public synchronized double getLocalModeThreshold() {
		return localModeThreshold;
	}

	public synchronized void setLocalModeThreshold(double localModeThreshold) {
		this.localModeThreshold = localModeThreshold;
	}

	public synchronized long getFileSyncStorage() {
		return fileSyncStorage;
	}

	public synchronized void setFileSyncStorage(long fileSyncStorage) {
		this.fileSyncStorage = fileSyncStorage;
	}

	public synchronized double getUsedStorageRatioThreshold() {
		return usedStorageRatioThreshold;
	}

	public synchronized void setUsedStorageRatioThreshold(double usedStorageRatioThreshold) {
		this.usedStorageRatioThreshold = usedStorageRatioThreshold;
	}

	public synchronized long getMaxAccessDelayThreshold() {
		return maxAccessDelayThreshold;
	}

	public synchronized void setMaxAccessDelayThreshold(long maxAccessDelayThreshold) {
		this.maxAccessDelayThreshold = maxAccessDelayThreshold;
	}

	public CMFileSyncUpdateMode getFileSyncUpdateMode() {
		return fileSyncUpdateMode;
	}

	public void setFileSyncUpdateMode(CMFileSyncUpdateMode fileSyncUpdateMode) {
		this.fileSyncUpdateMode = fileSyncUpdateMode;
	}

	public long getFileSizeThreshold() {
		return fileSizeThreshold;
	}

	public void setFileSizeThreshold(long fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

	public double getFileModRatioThreshold() {
		return fileModRatioThreshold;
	}

	public void setFileModRatioThreshold(double fileModRatioThreshold) {
		this.fileModRatioThreshold = fileModRatioThreshold;
	}
}
