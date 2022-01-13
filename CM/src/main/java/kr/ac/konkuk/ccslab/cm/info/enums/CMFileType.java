package kr.ac.konkuk.ccslab.cm.info.enums;

import java.nio.file.Files;
import java.nio.file.Path;

public enum CMFileType {
    FILE, LINK, DIR;

    public static CMFileType getType(Path path) {
        if(path == null) return null;
        if(Files.isDirectory(path)) return CMFileType.DIR;
        else if(Files.isSymbolicLink(path)) return CMFileType.LINK;
        else return CMFileType.FILE;
    }
}
