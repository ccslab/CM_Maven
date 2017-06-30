import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.sns.CMSNSUserAccessSimulator;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.*;
import java.util.*;

public class CMServerApp {
	private CMServerStub m_serverStub;
	private CMServerEventHandler m_eventHandler;
	private boolean m_bRun;
	private CMSNSUserAccessSimulator m_uaSim;
	private Scanner m_scan = null;
	
	public CMServerApp()
	{
		m_serverStub = new CMServerStub();
		m_eventHandler = new CMServerEventHandler(m_serverStub);
		m_bRun = true;
		m_uaSim = new CMSNSUserAccessSimulator();
	}
	
	public CMServerStub getServerStub()
	{
		return m_serverStub;
	}
	
	public CMServerEventHandler getServerEventHandler()
	{
		return m_eventHandler;
	}
	
	///////////////////////////////////////////////////////////////
	// test methods
	public void startTest()
	{
		System.out.println("Server application starts.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		m_scan = new Scanner(System.in);
		String strInput = null;
		int nCommand = -1;
		while(m_bRun)
		{
			System.out.println("Type \"0\" for menu.");
			System.out.print("> ");
			try {
				strInput = br.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			
			try {
				nCommand = Integer.parseInt(strInput);
			} catch (NumberFormatException e) {
				System.out.println("Incorrect command number!");
				continue;
			}
			
			switch(nCommand)
			{
			case 0:
				System.out.println("0: help, 1: session info, 2: group info");
				System.out.println("3: set file path, 4: request file, 5: push file");
				System.out.println("17: cancel receiving file, 18: cancel sending file");
				System.out.println("6: request registration to the default server");
				System.out.println("7: request deregistration from the default server");
				System.out.println("8: connect to the default server, 9: disconnect from the default server");
				System.out.println("10: set a scheme for attachment download of SNS content");
				System.out.println("11: config user access simulation, 12: start user access simulation");
				System.out.println("13: start user access simulation and calculate prefetch precision and recall");
				System.out.println("14: configure, simulate, and write recent history to CMDB");
				System.out.println("15: test input network throughput, 16: test output network throughput");
				System.out.println("99: terminate CM");
				break;
			case 1: // print session information
				printSessionInfo();
				break;
			case 2: // print selected group information
				printGroupInfo();
				break;
			case 3: // set file path
				setFilePath();
				break;
			case 4: // request a file
				requestFile();
				break;
			case 5: // push a file
				pushFile();
				break;
			case 6: // request registration to the default server
				requestServerReg();
				break;
			case 7: // request deregistration from the default server
				requestServerDereg();
				break;
			case 8: // connect to the default server
				connectToDefaultServer();
				break;
			case 9: // disconnect from the default server
				disconnectFromDefaultServer();
				break;
			case 10:	// set a scheme for attachment download of SNS content
				setAttachDownloadScheme();
				break;
			case 11:	// configure variables of user access simulation
				configureUserAccessSimulation();
				break;
			case 12: 	// start user access simulation
				startUserAccessSimulation();
				break;
			case 13:	// start user access simulation and calculate prefetch precision and recall
				startUserAccessSimulationAndCalPrecRecall();
				break;
			case 14: 	// configure, simulate and write recent history to CMDB
				writeRecentAccHistoryToDB();
				break;
			case 15:	// test input network throughput
				measureInputThroughput();
				break;
			case 16:	// test output network throughput
				measureOutputThroughput();
				break;
			case 17:	// test cancel receiving a file
				cancelRecvFile();
				break;
			case 18:	// test cancel sending a file
				cancelSendFile();
				break;
			case 99:
				testTermination();
				return;
			default:
				System.out.println("Unknown command.");
				break;
			}
		}
		
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		m_scan.close();
		
	}
	
	public void testTermination()
	{
		m_serverStub.terminateCM();
		m_bRun = false;
	}
	
	public void printSessionInfo()
	{
		System.out.println("------------------------------------------------------");
		System.out.format("%-20s%-20s%-10s%-10s%n", "session name", "session addr", "port", "#users");
		System.out.println("------------------------------------------------------");
		
		CMInteractionInfo interInfo = m_serverStub.getCMInfo().getInteractionInfo();
		Iterator<CMSession> iter = interInfo.getSessionList().iterator();
		while(iter.hasNext())
		{
			CMSession session = iter.next();
			System.out.format("%-20s%-20s%-10d%-10d%n", session.getSessionName(), session.getAddress()
					, session.getPort(), session.getSessionUsers().getMemberNum());
		}
		return;
	}
	
	public void printGroupInfo()
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String strSessionName = null;
		
		System.out.println("====== print group information");
		System.out.print("Session name: ");
		try {
			strSessionName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CMInteractionInfo interInfo = m_serverStub.getCMInfo().getInteractionInfo();
		CMSession session = interInfo.findSession(strSessionName);
		if(session == null)
		{
			System.out.println("Session("+strSessionName+") not found.");
			return;
		}
		
		System.out.println("------------------------------------------------------------------");
		System.out.format("%-20s%-20s%-10s%-10s%n", "group name", "multicast addr", "port", "#users");
		System.out.println("------------------------------------------------------------------");

		Iterator<CMGroup> iter = session.getGroupList().iterator();
		while(iter.hasNext())
		{
			CMGroup gInfo = iter.next();
			System.out.format("%-20s%-20s%-10d%-10d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
					, gInfo.getGroupPort(), gInfo.getGroupUsers().getMemberNum());
		}

		System.out.println("======");
		return;
	}
	
	public void setFilePath()
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== set file path");
		String strPath = null;
		System.out.print("file path: ");
		try {
			strPath = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		if(!strPath.endsWith("/"))
		{
			System.out.println("Invalid file path!");
			return;
		}
		*/
		
		//CMFileTransferManager.setFilePath(strPath, m_serverStub.getCMInfo());
		m_serverStub.setFilePath(strPath);
		
		System.out.println("======");
	}
	
	public void requestFile()
	{
		boolean bReturn = false;
		String strFileName = null;
		String strFileOwner = null;
		String strFileAppend = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== request a file");
		try {
			System.out.print("File name: ");
			strFileName = br.readLine();
			System.out.print("File owner(user name): ");
			strFileOwner = br.readLine();
			System.out.print("File append mode('y'(append);'n'(overwrite);''(empty for the default configuration): ");
			strFileAppend = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(strFileAppend.isEmpty())
			bReturn = m_serverStub.requestFile(strFileName, strFileOwner);
		else if(strFileAppend.equals("y"))
			bReturn = m_serverStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_APPEND);
		else if(strFileAppend.equals("n"))
			bReturn = m_serverStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_OVERWRITE);
		else
			System.err.println("wrong input for the file append mode!");
		
		if(!bReturn)
			System.err.println("Request file error! file("+strFileName+"), owner("+strFileOwner+").");

		System.out.println("======");
	}
	
	public void pushFile()
	{
		boolean bReturn = false;
		String strFilePath = null;
		String strReceiver = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== push a file");
		
		try {
			System.out.print("File path name: ");
			strFilePath = br.readLine();
			System.out.print("File receiver (user name): ");
			strReceiver = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		bReturn = m_serverStub.pushFile(strFilePath, strReceiver);
		
		if(!bReturn)
			System.err.println("Push file error! file("+strFilePath+"), receiver("+strReceiver+")");

		System.out.println("======");
	}
	
	public void cancelRecvFile()
	{
		String strSender = null;
		boolean bReturn = false;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== cancel receiving a file");
		
		System.out.print("Input sender name (enter for all senders): ");
		try {
			strSender = br.readLine();
			if(strSender.isEmpty())
				strSender = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		bReturn = m_serverStub.cancelRequestFile(strSender);
		
		if(bReturn)
		{
			if(strSender == null)
				strSender = "all senders";
			System.out.println("Successfully requested to cancel receiving a file to ["+strSender+"].");
		}
		else
			System.err.println("Request failed to cancel receiving a file to ["+strSender+"]!");
		
		return;
		
	}
	
	public void cancelSendFile()
	{
		String strReceiver = null;
		boolean bReturn = false;
		System.out.println("====== cancel sending a file");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input receiver name (enter for all receivers): ");
		
		try {
			strReceiver = br.readLine();
			if(strReceiver.isEmpty())
				strReceiver = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		bReturn = m_serverStub.cancelPushFile(strReceiver);
		
		if(bReturn)
			System.out.println("Successfully requested to cancel sending a file to ["+strReceiver+"]");
		else
			System.err.println("Request failed to cancel sending a file to ["+strReceiver+"]!");
		
		return;
	}
	
	public void requestServerReg()
	{
		String strServerName = null;
		System.out.println("====== request registration to the default server");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Enter registered server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_serverStub.requestServerReg(strServerName);
		System.out.println("======");
		return;
	}
	
	public void requestServerDereg()
	{
		System.out.println("====== request deregistration from the default server");
		m_serverStub.requestServerDereg();
		System.out.println("======");
		return;
	}
	
	public void connectToDefaultServer()
	{
		System.out.println("====== connect to the default server");
		m_serverStub.connectToServer();
		System.out.println("======");
		return;
	}
	
	public void disconnectFromDefaultServer()
	{
		System.out.println("====== disconnect from the default server");
		m_serverStub.disconnectFromServer();
		System.out.println("======");
		return;
	}
	
	public void setAttachDownloadScheme()
	{
		String strUserName = null;
		int nScheme;
		System.out.println("====== set a scheme for attachement download of SNS content");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Input target user name(Enter for all users): ");
			strUserName = br.readLine();
			if(strUserName.isEmpty())
				strUserName = null;
			System.out.println("0: full download, 1: partial(thumbnail file) download, "
					+ "2: prefetching download, 3: none (only file name)");
			System.out.print("Enter scheme number: ");
			nScheme = Integer.parseInt(br.readLine());
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		m_serverStub.setAttachDownloadScheme(strUserName, nScheme);
		return;
	}
	
	public void configureUserAccessSimulation()
	{
		int nUserNum = -1;
		int nAvgDayAccCount = -1;
		int nTotalSimDays = -1;
		int nAccPattern = -1;
		double dNormalMean = -1.0;
		double dNormalSD = -1.0;
		String strInput = null;
		
		// retrieve current values
		nUserNum = m_uaSim.getUserNum();
		nAvgDayAccCount = m_uaSim.getAvgDayAccCount();
		nTotalSimDays = m_uaSim.getTotalSimDays();
		nAccPattern = m_uaSim.getAccPattern();
		dNormalMean = m_uaSim.getNormalMean();
		dNormalSD = m_uaSim.getNormalSD();
		
		System.out.println("====== Configure variables of user access simulation");
		System.out.println("The value in () is the current value.");
		System.out.println("Enter in each variable to keep the current value.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("Number of users("+nUserNum+"): ");
			strInput = br.readLine();
			if(!strInput.isEmpty())
			{
				nUserNum = Integer.parseInt(strInput);
				m_uaSim.setUserNum(nUserNum);
			}
			System.out.print("Average daily access count("+nAvgDayAccCount+"): ");
			strInput = br.readLine();
			if(!strInput.isEmpty())
			{
				nAvgDayAccCount = Integer.parseInt(strInput);
				m_uaSim.setAvgDayAccCount(nAvgDayAccCount);
			}
			System.out.print("Total number of simulation days("+nTotalSimDays+"): ");
			strInput = br.readLine();
			if(!strInput.isEmpty())
			{
				nTotalSimDays = Integer.parseInt(strInput);
				m_uaSim.setTotalSimDays(nTotalSimDays);
			}
			System.out.print("Access pattern("+nAccPattern+") (0: random, 1: skewed): ");
			strInput = br.readLine();
			if(!strInput.isEmpty())
			{
				nAccPattern = Integer.parseInt(strInput);
				if(nAccPattern < 0 || nAccPattern > 1)
				{
					System.err.println("Invalid access pattern!");
					return;
				}
				m_uaSim.setAccPattern(nAccPattern);
			}
			
			if(nAccPattern == 1) // skewed access pattern
			{
				System.out.print("Mean value("+dNormalMean+"): ");
				strInput = br.readLine();
				if(!strInput.isEmpty())
				{
					dNormalMean = Double.parseDouble(strInput);
					m_uaSim.setNormalMean(dNormalMean);
				}
				System.out.println("Standard deviation("+dNormalSD+"): ");
				strInput = br.readLine();
				if(!strInput.isEmpty())
				{
					dNormalSD = Double.parseDouble(strInput);
					m_uaSim.setNormalSD(dNormalSD);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}
	
	// simulate user access history according to previous configuration
	public void startUserAccessSimulation()
	{
		System.out.println("====== Start user access simulation");
		m_uaSim.start();
		return;
	}
	
	// simulate user access history and calculate prefetch precision and recall
	public void startUserAccessSimulationAndCalPrecRecall()
	{
		int nUserNum = 0;
		int nAvgDayAccCount = 0;
		int nTotalSimDays = 0;
		int nAccPattern = 0;
		double dNormalMean = 0.0;
		double dNormalSD = 0.0;
		double dPrefThreshold;
		int nPrefInterval;
		double[] dAvgPrecRecall; //[0]: precision, [1]: recall
		FileOutputStream fo = null;
		PrintWriter pw = null;


		System.out.println("====== Start user access simulation");
		
		//////// execute simulation
		nUserNum = 10;
		nAvgDayAccCount = 10;
		nTotalSimDays = 100;
		nAccPattern = 0;
		dNormalMean = 5.0;
		dNormalSD = 1.0;
		
		for(nAccPattern = 0; nAccPattern <= 1; nAccPattern++)
		{
			m_uaSim.start(nUserNum, nAvgDayAccCount, nTotalSimDays, nAccPattern, dNormalMean, dNormalSD);

			///// calculate the prefetch precision and recall varying the prefetch threshold
			try {
				fo = new FileOutputStream("precision-recall-int7-ap"+nAccPattern+".txt");
				pw = new PrintWriter(fo);
				pw.println("Number of users: "+nUserNum);
				pw.println("Average daily access count: "+nAvgDayAccCount);
				pw.println("Total simulation days: "+nTotalSimDays);
				pw.println("Access pattern: "+nAccPattern);
				if(nAccPattern == 1)
				{
					pw.println("Normal mean: "+dNormalMean);
					pw.println("Normal SD: "+dNormalSD);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			nPrefInterval = 7;
			pw.println("Prefetch Interval: "+nPrefInterval);
			dPrefThreshold = 0.0;
			while(dPrefThreshold < 1.0)
			{
				dAvgPrecRecall = m_uaSim.calPrecisionRecall(dPrefThreshold, nPrefInterval);
				pw.format("%.2f\t%.4f\t%.4f\n", dPrefThreshold, dAvgPrecRecall[0], dAvgPrecRecall[1]);
				dPrefThreshold += 0.1;
			}

			pw.close();
			try {
				fo.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			///// calculate the prefetch precision and recall varying the prefetch interval
			try {
				if(nAccPattern == 0)
					fo = new FileOutputStream("precision-recall-thr0.1-ap"+nAccPattern+".txt");
				else
					fo = new FileOutputStream("precision-recall-thr0.2-ap"+nAccPattern+".txt");
				pw = new PrintWriter(fo);
				pw.println("Number of users: "+nUserNum);
				pw.println("Average daily access count: "+nAvgDayAccCount);
				pw.println("Total simulation days: "+nTotalSimDays);
				pw.println("Access pattern: "+nAccPattern);
				if(nAccPattern == 1)
				{
					pw.println("Normal mean: "+dNormalMean);
					pw.println("Normal SD: "+dNormalSD);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(nAccPattern == 0)
				dPrefThreshold = 0.1;
			else
				dPrefThreshold = 0.2;
			pw.println("Prefetch Threshold: "+dPrefThreshold);
			for(nPrefInterval = 7; nPrefInterval <= nTotalSimDays; nPrefInterval+=7)
			{
				dAvgPrecRecall = m_uaSim.calPrecisionRecall(dPrefThreshold, nPrefInterval);
				pw.format("%d\t%.4f\t%.4f\n", nPrefInterval, dAvgPrecRecall[0], dAvgPrecRecall[1]);			
			}

			pw.close();
			try {
				fo.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		
		//////// execute simulation of skewed access pattern with varying standard deviation of normal distribution
		nUserNum = 10;
		nAvgDayAccCount = 10;
		nTotalSimDays = 100;
		nAccPattern = 1;
		dNormalMean = 5.0;
		dNormalSD = 1.0;
		
		for(dNormalSD = 0.1; dNormalSD <= 2.0; dNormalSD += 0.2)
		{
			m_uaSim.start(nUserNum, nAvgDayAccCount, nTotalSimDays, nAccPattern, dNormalMean, dNormalSD);

			///// calculate the prefetch precision and recall varying the prefetch threshold
			try {
				fo = new FileOutputStream("precision-recall-int7-ap1-mean"+dNormalMean+"-sd"+dNormalSD+".txt");
				pw = new PrintWriter(fo);
				pw.println("Number of users: "+nUserNum);
				pw.println("Average daily access count: "+nAvgDayAccCount);
				pw.println("Total simulation days: "+nTotalSimDays);
				pw.println("Access pattern: "+nAccPattern);
				pw.println("Normal mean: "+dNormalMean);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			nPrefInterval = 7;
			pw.println("Prefetch Interval: "+nPrefInterval);
			dPrefThreshold = 0.0;
			while(dPrefThreshold < 1.0)
			{
				dAvgPrecRecall = m_uaSim.calPrecisionRecall(dPrefThreshold, nPrefInterval);
				pw.format("%.2f\t%.4f\t%.4f\n", dPrefThreshold, dAvgPrecRecall[0], dAvgPrecRecall[1]);
				dPrefThreshold += 0.1;
			}

			pw.close();
			try {
				fo.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		return;
	}
	
	public void writeRecentAccHistoryToDB()
	{
		CMInfo cmInfo = m_serverStub.getCMInfo();
		boolean bRet = false;
		
		// configure user access simulation
		configureUserAccessSimulation();
		// start simulation
		startUserAccessSimulation();
		// wrtie recent access history to DB
		bRet = m_uaSim.writeRecentAccHistoryToDB(cmInfo);
		if(bRet)
			System.out.println("Successful update of user access table of CMDB");
		else
			System.err.println("Error for update of user access table of CMDB!");
		
		return;
	}
	
	public void measureInputThroughput()
	{
		String strTarget = null;
		float fSpeed = -1; // MBps
		System.out.println("========== test input network throughput");
		System.out.print("target user: ");
		strTarget = m_scan.next();
		fSpeed = m_serverStub.measureInputThroughput(strTarget);
		if(fSpeed == -1)
			System.err.println("Test failed!");
		else
			System.out.format("Input network throughput from [%s] : %.2f%n", strTarget, fSpeed);		
	}
	
	public void measureOutputThroughput()
	{
		String strTarget = null;
		float fSpeed = -1; // MBps
		System.out.println("========== test output network throughput");
		System.out.print("target user: ");
		strTarget = m_scan.next();
		fSpeed = m_serverStub.measureOutputThroughput(strTarget);
		if(fSpeed == -1)
			System.err.println("Test failed!");
		else
			System.out.format("Output network throughput to [%s] : %.2f%n", strTarget, fSpeed);		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CMServerApp server = new CMServerApp();
		CMServerStub cmStub = server.getServerStub();
		cmStub.setEventHandler(server.getServerEventHandler());
		boolean bRet = cmStub.startCM();
		if(!bRet)
		{
			System.err.println("CM initialization error!");
			return;
		}
		server.startTest();
		
		System.out.println("Server application is terminated.");
	}

}
