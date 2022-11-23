package kr.ac.konkuk.ccslab.cm.info.enums;

/**
 * File synchronization mode.
 */
public enum CMFileSyncMode {
    /**
     *    {@link CMFileSyncMode#OFF} means that the client does not start the file sync.
     */
    OFF,

    /**
     *    {@link CMFileSyncMode#MANUAL} means that the client starts the file sync with
     *    the manual file mode change mechanism.*
     */
    MANUAL,

    /**
     *    {@link CMFileSyncMode#AUTO} means that the client starts the file sync with
     *    the active file mode change mechanism.
     */
    AUTO
}
