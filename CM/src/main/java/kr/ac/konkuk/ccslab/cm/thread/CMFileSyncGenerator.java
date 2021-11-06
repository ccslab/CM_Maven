package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.file.Path;
import java.util.List;

public class CMFileSyncGenerator implements Runnable {
    private String userName;
    private CMInfo cmInfo;
    private List<CMFileSyncEntry> fileEntryList;
    private List<Path> basisFileList;

    public CMFileSyncGenerator(String userName, CMInfo cmInfo) {
        this.userName = userName;
        this.cmInfo = cmInfo;
    }

    @Override
    public void run() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncGenerator.run() called..");
        }

        // get client file-entry-list
        fileEntryList = cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
        System.out.println("fileEntryList = " + fileEntryList);
        if(fileEntryList == null) {
            System.err.println("CMFileSyncGenerator.run(), fileEntryList is null!");
            return;
        }

        // create a basis file-entry-list at the server
        basisFileList = createBasisFileList();

        // from here

        //// compare the client file-entry-list and the basis file-entry-list

        // create and delete a file-entry-list that exists only at the server

        // create a new file-entry-list that will be added to the server

        // request the files in the new file-entry-list from the client

        // update the files at the server by synchronizing with those at the client

    }

    private List<Path> createBasisFileList() {

        // from here
        return null;
    }
}
