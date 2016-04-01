import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import kr.ac.konkuk.ccslab.cm.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.CMGroup;
import kr.ac.konkuk.ccslab.cm.CMInfo;
import kr.ac.konkuk.ccslab.cm.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.CMSNSUserAccessSimulator;
import kr.ac.konkuk.ccslab.cm.CMServerStub;
import kr.ac.konkuk.ccslab.cm.CMSession;

public class CMWinServer extends JFrame {
	//private JTextArea m_outTextArea;
	private JTextPane m_outTextPane;
	private JTextField m_inTextField;
	private JButton m_startStopButton;
	private CMServerStub m_serverStub;
	private CMWinServerEventHandler m_eventHandler;
	private CMSNSUserAccessSimulator m_uaSim;
	
	CMWinServer()
	{
		
		MyKeyListener cmKeyListener = new MyKeyListener();
		MyActionListener cmActionListener = new MyActionListener();
		setTitle("CM Server");
		setSize(500, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setLayout(new BorderLayout());
		
		m_outTextPane = new JTextPane();
		m_outTextPane.setEditable(false);

		StyledDocument doc = m_outTextPane.getStyledDocument();
		addStylesToDocument(doc);

		add(m_outTextPane, BorderLayout.CENTER);
		JScrollPane scroll = new JScrollPane (m_outTextPane, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		add(scroll);
		
		m_inTextField = new JTextField();
		m_inTextField.addKeyListener(cmKeyListener);
		add(m_inTextField, BorderLayout.SOUTH);
		
		JPanel topButtonPanel = new JPanel();
		topButtonPanel.setLayout(new FlowLayout());
		add(topButtonPanel, BorderLayout.NORTH);
		
		m_startStopButton = new JButton("Start Server CM");
		m_startStopButton.addActionListener(cmActionListener);
		//add(startStopButton, BorderLayout.NORTH);
		topButtonPanel.add(m_startStopButton);
		
		setVisible(true);

		m_serverStub = new CMServerStub();
		m_eventHandler = new CMWinServerEventHandler(m_serverStub, this);
		m_uaSim = new CMSNSUserAccessSimulator();

	}
	
	private void addStylesToDocument(StyledDocument doc)
	{
		Style defStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		Style regularStyle = doc.addStyle("regular", defStyle);
		StyleConstants.setFontFamily(regularStyle, "SansSerif");
		
		Style boldStyle = doc.addStyle("bold", defStyle);
		StyleConstants.setBold(boldStyle, true);
	}
	
	public CMServerStub getServerStub()
	{
		return m_serverStub;
	}
	
	public CMWinServerEventHandler getServerEventHandler()
	{
		return m_eventHandler;
	}
	
	public void processInput(String strInput)
	{
		int nCommand = -1;
		try {
			nCommand = Integer.parseInt(strInput);
		} catch (NumberFormatException e) {
			//System.out.println("Incorrect command number!");
			printMessage("Incorrect command number!\n");
			return;
		}
		
		switch(nCommand)
		{
		case 0:
			//System.out.println("0: help, 1: session info, 2: group info");
			//System.out.println("3: set file path, 4: request file, 5: push file");
			//System.out.println("6: request registration to the default server");
			//System.out.println("7: request deregistration from the default server");
			//System.out.println("8: connect to the default server, 9: disconnect from the default server");
			//System.out.println("99: terminate CM");
			printMessage("0: help, 1: session info, 2: group info\n");
			printMessage("3: set file path, 4: request file, 5: push file\n");
			printMessage("6: request registration to the default server\n");
			printMessage("7: request deregistration from the default server\n");
			printMessage("8: connect to the default server, 9: disconnect from the default server\n");
			printMessage("10: set a scheme for attachment download of SNS content\n");
			printMessage("11: config user access simulation, 12: start user access simulation\n");
			printMessage("13: start user access simulation and calculate prefetch precision and recall\n");
			printMessage("14: configure, simulate, and write recent history to CMDB\n");
			printMessage("99: terminate CM\n");
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
		case 10: // set a scheme for attachement download of SNS content
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
		case 99:
			testTermination();
			return;
		default:
			//System.out.println("Unknown command.");
			printMessage("Unknown command.\n");
			break;
		}
	}
	
	public void testTermination()
	{
		m_serverStub.terminateCM();
		printMessage("Server CM terminates.\n");
		m_startStopButton.setText("Start Server CM");

	}

	public void printSessionInfo()
	{
		//System.out.println("------------------------------------------------------");
		//System.out.format("%-20s%-20s%-10s%-10s%n", "session name", "session addr", "port", "#users");
		//System.out.println("------------------------------------------------------");
		printMessage("------------------------------------------------------\n");
		printMessage(String.format("%-20s%-20s%-10s%-10s%n", "session name", "session addr", "port", "#users"));
		printMessage("------------------------------------------------------\n");
		
		CMInteractionInfo interInfo = m_serverStub.getCMInfo().getInteractionInfo();
		Iterator<CMSession> iter = interInfo.getSessionList().iterator();
		while(iter.hasNext())
		{
			CMSession session = iter.next();
			//System.out.format("%-20s%-20s%-10d%-10d%n", session.getSessionName(), session.getAddress()
			//		, session.getPort(), session.getSessionUsers().getMemberNum());
			printMessage(String.format("%-20s%-20s%-10d%-10d%n", session.getSessionName(), session.getAddress()
					, session.getPort(), session.getSessionUsers().getMemberNum()));
		}
		return;
	}
	
	public void printGroupInfo()
	{
		// JOptionPane.showInputDialog for single input, showConfirmDialog for multiple input fields
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String strSessionName = null;
		
		//System.out.println("====== print group information");
		printMessage("====== print group information\n");
		/*
		System.out.print("Session name: ");
		try {
			strSessionName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		strSessionName = JOptionPane.showInputDialog("Session Name");
		if(strSessionName == null)
		{
			return;
		}
		
		CMInteractionInfo interInfo = m_serverStub.getCMInfo().getInteractionInfo();
		CMSession session = interInfo.findSession(strSessionName);
		if(session == null)
		{
			//System.out.println("Session("+strSessionName+") not found.");
			printMessage("Session("+strSessionName+") not found.\n");
			return;
		}
		
		//System.out.println("------------------------------------------------------------------");
		//System.out.format("%-20s%-20s%-10s%-10s%n", "group name", "multicast addr", "port", "#users");
		//System.out.println("------------------------------------------------------------------");
		printMessage("------------------------------------------------------------------\n");
		printMessage(String.format("%-20s%-20s%-10s%-10s%n", "group name", "multicast addr", "port", "#users"));
		printMessage("------------------------------------------------------------------\n");

		Iterator<CMGroup> iter = session.getGroupList().iterator();
		while(iter.hasNext())
		{
			CMGroup gInfo = iter.next();
			//System.out.format("%-20s%-20s%-10d%-10d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
			//		, gInfo.getGroupPort(), gInfo.getGroupUsers().getMemberNum());
			printMessage(String.format("%-20s%-20s%-10d%-10d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
					, gInfo.getGroupPort(), gInfo.getGroupUsers().getMemberNum()));
		}

		//System.out.println("======");
		printMessage("======\n");
		return;
	}

	public void setFilePath()
	{
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//System.out.println("====== set file path");
		printMessage("====== set file path\n");
		String strPath = null;
		/*
		System.out.print("file path (must end with \'/\'): ");
		try {
			strPath = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		strPath = JOptionPane.showInputDialog("file path (must end with \'/\')");
		if(strPath == null)
		{
			return;
		}
		
		if(!strPath.endsWith("/"))
		{
			//System.out.println("Invalid file path!");
			printMessage("Invalid file path!\n");
			return;
		}
		
		//CMFileTransferManager.setFilePath(strPath, m_serverStub.getCMInfo());
		m_serverStub.setFilePath(strPath);
		
		//System.out.println("======");
		printMessage("======\n");
	}
	
	public void requestFile()
	{
		String strFileName = null;
		String strFileOwner = null;
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== request a file");
		try {
			System.out.print("File name: ");
			strFileName = br.readLine();
			System.out.print("File owner(user name): ");
			strFileOwner = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		printMessage("====== request a file\n");
		JTextField fileNameField = new JTextField();
		JTextField fileOwnerField = new JTextField();
		Object[] message = {
		    "File Name:", fileNameField,
		    "File Owner:", fileOwnerField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "File Request Input", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) 
		{
			strFileName = fileNameField.getText();
			strFileOwner = fileOwnerField.getText();
			//CMFileTransferManager.requestFile(strFileName, strFileOwner, m_serverStub.getCMInfo());
			m_serverStub.requestFile(strFileName, strFileOwner);
		}
		
		//System.out.println("======");
		printMessage("======\n");
	}

	public void pushFile()
	{
		String strFilePath = null;
		File[] files;
		String strReceiver = null;

		/*
		printMessage("====== push a file\n");
		JTextField fileNameField = new JTextField();
		JTextField receiverField = new JTextField();
		Object[] message = {
		    "File Path:", fileNameField,
		    "File Receiver:", receiverField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "File Push Input", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			strFilePath = fileNameField.getText();
			strReceiver = receiverField.getText();
			CMFileTransferManager.pushFile(strFilePath, strReceiver, m_serverStub.getCMInfo());
		}
		*/
		
		strReceiver = JOptionPane.showInputDialog("Receiver Name: ");
		if(strReceiver == null) return;
		JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(true);
		CMFileTransferInfo fInfo = m_serverStub.getCMInfo().getFileTransferInfo();
		File curDir = new File(fInfo.getFilePath());
		fc.setCurrentDirectory(curDir);
		int fcRet = fc.showOpenDialog(this);
		if(fcRet != JFileChooser.APPROVE_OPTION) return;
		files = fc.getSelectedFiles();
		if(files.length < 1) return;
		for(int i=0; i < files.length; i++)
		{
			strFilePath = files[i].getPath();
			//CMFileTransferManager.pushFile(strFilePath, strReceiver, m_serverStub.getCMInfo());
			m_serverStub.pushFile(strFilePath, strReceiver);
		}
	
		printMessage("======\n");
	}

	public void requestServerReg()
	{
		String strServerName = null;
		//System.out.println("====== request registration to the default server");
		printMessage("====== request registration to the default server\n");
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Enter registered server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		strServerName = JOptionPane.showInputDialog("Enter registered server name");
		if(strServerName != null)
		{
			m_serverStub.requestServerReg(strServerName);
		}
		//System.out.println("======");
		printMessage("======\n");
		return;
	}

	public void requestServerDereg()
	{
		//System.out.println("====== request deregistration from the default server");
		printMessage("====== request deregistration from the default server\n");
		m_serverStub.requestServerDereg();
		//System.out.println("======");
		printMessage("======\n");
		return;
	}

	public void connectToDefaultServer()
	{
		//System.out.println("====== connect to the default server");
		printMessage("====== connect to the default server\n");
		m_serverStub.connectToServer();
		//System.out.println("======");
		printMessage("======\n");
		return;
	}

	public void disconnectFromDefaultServer()
	{
		//System.out.println("====== disconnect from the default server");
		printMessage("====== disconnect from the default server\n");
		m_serverStub.disconnectFromServer();
		//System.out.println("======");
		printMessage("======\n");
		return;
	}
	
	public void setAttachDownloadScheme()
	{
		String strUserName = null;
		int nScheme = -1;
		JTextField userField = new JTextField();
		String[] attachLod = {"Full", "Thumbnail", "Prefetching", "None"};
		JComboBox<String> lodBox = new JComboBox<String>(attachLod);
		Object[] message = {
				"Target user name (Enter for all users)", userField,
				"Image QoS: ", lodBox
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Attachement Download Scheme", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strUserName = userField.getText();
			nScheme = lodBox.getSelectedIndex();
			printMessage("The attachment download scheme of user["+strUserName
					+"] is set to ["+lodBox.getItemAt(nScheme)+"].\n");
			if(strUserName.isEmpty())
				strUserName = null;
			m_serverStub.setAttachDownloadScheme(strUserName, nScheme);
		}
		
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
		
		// retrieve current values
		nUserNum = m_uaSim.getUserNum();
		nAvgDayAccCount = m_uaSim.getAvgDayAccCount();
		nTotalSimDays = m_uaSim.getTotalSimDays();
		nAccPattern = m_uaSim.getAccPattern();
		dNormalMean = m_uaSim.getNormalMean();
		dNormalSD = m_uaSim.getNormalSD();
		
		printMessage("====== Configure variables of user access simulation\n");
		JTextField userNumField = new JTextField();
		userNumField.setText(Integer.toString(nUserNum));
		JTextField avgDayAccCountField = new JTextField();
		avgDayAccCountField.setText(Integer.toString(nAvgDayAccCount));
		JTextField totalSimDaysField = new JTextField();
		totalSimDaysField.setText(Integer.toString(nTotalSimDays));
		String[] accPattern = {"Random", "Skewed"};
		JComboBox<String> accPatternBox = new JComboBox<String>(accPattern);
		accPatternBox.setSelectedIndex(0);

		Object[] message = {
		    "Number of users:", userNumField,
		    "Average daily access count:", avgDayAccCountField,
		    "Total number of simulation days:", totalSimDaysField,
		    "Access pattern:", accPatternBox
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Configuration of simulation variables", 
				JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION)
		{
			int nInput;
			nInput = Integer.parseInt(userNumField.getText());
			if(nInput != nUserNum)
				m_uaSim.setUserNum(nInput);
			nInput = Integer.parseInt(avgDayAccCountField.getText());
			if(nInput != nAvgDayAccCount)
				m_uaSim.setAvgDayAccCount(nInput);
			nInput = Integer.parseInt(totalSimDaysField.getText());
			if(nInput != nTotalSimDays)
				m_uaSim.setTotalSimDays(nInput);
			nInput = accPatternBox.getSelectedIndex();
			if(nInput != nAccPattern)
				m_uaSim.setAccPattern(nInput);
		}
		
		if(accPatternBox.getSelectedIndex() == 1) // skewed access pattern
		{
			JTextField normalMeanField = new JTextField();
			normalMeanField.setText(Double.toString(dNormalMean));
			JTextField normalSDField = new JTextField();
			normalSDField.setText(Double.toString(dNormalSD));
			Object[] messageNormal = {
					"Mean value:", normalMeanField,
					"Standard deviation:", normalSDField 
			};
			option = JOptionPane.showConfirmDialog(null, messageNormal, "Config for normal distribution",
					JOptionPane.OK_CANCEL_OPTION);
			if(option == JOptionPane.OK_OPTION)
			{
				double dInput;
				dInput = Double.parseDouble(normalMeanField.getText());
				if(dInput != dNormalMean)
					m_uaSim.setNormalMean(dInput);
				dInput = Double.parseDouble(normalSDField.getText());
				if(dInput != dNormalSD)
					m_uaSim.setNormalSD(dInput);
			}
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
			printMessage("Successful update of user access table of CMDB\n");
		else
			printMessage("Error for update of user access table of CMDB!\n");
		
		return;
	}


	public void printMessage(String strText)
	{
		/*
		m_outTextArea.append(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
		*/
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, null);
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public void printStyledMessage(String strText, String strStyleName)
	{
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, doc.getStyle(strStyleName));
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
		
	public void printImage(String strPath)
	{
		int nTextPaneWidth = m_outTextPane.getWidth();
		int nImageWidth;
		int nImageHeight;
		int nNewWidth;
		int nNewHeight;
		ImageIcon icon = new ImageIcon(strPath);
		Image image = icon.getImage();
		nImageWidth = image.getWidth(m_outTextPane);
		nImageHeight = image.getHeight(m_outTextPane);
		
		if(nImageWidth > nTextPaneWidth/2)
		{
			nNewWidth = nTextPaneWidth / 2;
			float fRate = (float)nNewWidth/(float)nImageWidth;
			nNewHeight = (int)(nImageHeight * fRate);
			Image newImage = image.getScaledInstance(nNewWidth, nNewHeight, java.awt.Image.SCALE_SMOOTH);
			icon = new ImageIcon(newImage);
		}
		
		m_outTextPane.insertIcon ( icon );
		printMessage("\n");
	}

	
	/*
	private void setMessage(String strText)
	{
		m_outTextArea.setText(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
	}
	*/
	
	public class MyKeyListener implements KeyListener {
		public void keyPressed(KeyEvent e)
		{
			int key = e.getKeyCode();
			if(key == KeyEvent.VK_ENTER)
			{
				JTextField input = (JTextField)e.getSource();
				String strText = input.getText();
				printMessage(strText+"\n");
				// parse and call CM API
				processInput(strText);
				input.setText("");
				input.requestFocus();
			}
		}
		
		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
	}
	
	public class MyActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			JButton button = (JButton) e.getSource();
			if(button.getText().equals("Start Server CM"))
			{
				// start cm
				boolean bRet = m_serverStub.startCM();
				if(!bRet)
				{
					printStyledMessage("CM initialization error!\n", "bold");
				}
				else
				{
					printStyledMessage("Server CM starts.\n", "bold");
					printMessage("Type \"0\" for menu.\n");					
				}
				// change button to "stop CM"
				button.setText("Stop Server CM");
				// check if default server or not
				if(CMConfigurator.isDServer(m_serverStub.getCMInfo()))
				{
					setTitle("CM Default Server (\"SERVER\")");
				}
				else
				{
					setTitle("CM Additional Server (\"?\")");
				}					
				m_inTextField.requestFocus();
			}
			else if(button.getText().equals("Stop Server CM"))
			{
				// stop cm
				m_serverStub.terminateCM();
				printMessage("Server CM terminates.\n");
				// change button to "start CM"
				button.setText("Start Server CM");
			}
		}
	}
			
	public static void main(String[] args)
	{
		CMWinServer server = new CMWinServer();
		CMServerStub cmStub = server.getServerStub();
		cmStub.setEventHandler(server.getServerEventHandler());
	}
}
