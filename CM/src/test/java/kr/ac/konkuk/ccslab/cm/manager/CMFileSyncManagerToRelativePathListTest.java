package kr.ac.konkuk.ccslab.cm.manager;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncManagerToRelativePathListTest {

    private CMFileSyncManager fileSyncManager;
    private Method toRelativePathList;
    private Path basePath;

    @Before
    public void setUp() throws Exception {
        System.out.println("===== called setUp()");
        fileSyncManager = new CMFileSyncManager();
        toRelativePathList = CMFileSyncManager.class
                .getDeclaredMethod("toRelativePathList", List.class, Path.class);
        toRelativePathList.setAccessible(true);
        basePath = Paths.get("/home/user/sync").toAbsolutePath().normalize();
    }

    @SuppressWarnings("unchecked")
    private List<Path> invoke(List<Path> paths, Path base) throws Exception {
        return (List<Path>) toRelativePathList.invoke(fileSyncManager, paths, base);
    }

    // -----------------------------------------------------------------------
    // 정상 케이스
    // -----------------------------------------------------------------------

    @Test
    public void absolutePath_convertedToRelative() throws Exception {
        System.out.println("===== called absolutePath_convertedToRelative()");
        Path absPath = basePath.resolve("a/b/c.txt");
        List<Path> result = invoke(Collections.singletonList(absPath), basePath);

        assertEquals(1, result.size());
        assertEquals(Paths.get("a/b/c.txt"), result.get(0));
        assertFalse(result.get(0).isAbsolute());
    }

    @Test
    public void relativePath_returnedAsIs() throws Exception {
        System.out.println("===== called relativePath_returnedAsIs()");
        Path relPath = Paths.get("a/b/c.txt");
        List<Path> result = invoke(Collections.singletonList(relPath), basePath);

        assertEquals(1, result.size());
        assertEquals(relPath, result.get(0));
    }

    @Test
    public void mixedPaths_convertedCorrectly() throws Exception {
        System.out.println("===== called mixedPaths_convertedCorrectly()");
        Path absPath = basePath.resolve("dir/file.txt");
        Path relPath = Paths.get("other/file.txt");
        List<Path> input = Arrays.asList(absPath, relPath);

        List<Path> result = invoke(input, basePath);

        assertEquals(2, result.size());
        assertEquals(Paths.get("dir/file.txt"), result.get(0));
        assertEquals(Paths.get("other/file.txt"), result.get(1));
        assertFalse(result.get(0).isAbsolute());
        assertFalse(result.get(1).isAbsolute());
    }

    @Test
    public void emptyList_returnsEmptyList() throws Exception {
        System.out.println("===== called emptyList_returnsEmptyList()");
        List<Path> result = invoke(Collections.emptyList(), basePath);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void absolutePath_directChildOfBase() throws Exception {
        System.out.println("===== called absolutePath_directChildOfBase()");
        Path absPath = basePath.resolve("file.txt");
        List<Path> result = invoke(Collections.singletonList(absPath), basePath);

        assertEquals(Paths.get("file.txt"), result.get(0));
    }

    @Test
    public void relativePath_withDotDot_normalized() throws Exception {
        System.out.println("===== called relativePath_withDotDot_normalized()");
        // 상대경로 a/../b/c.txt → 정규화되어 b/c.txt
        Path relPath = Paths.get("a/../b/c.txt");
        List<Path> result = invoke(Collections.singletonList(relPath), basePath);

        assertEquals(Paths.get("b/c.txt"), result.get(0));
    }

    @Test
    public void multipleAbsolutePaths_allConverted() throws Exception {
        System.out.println("===== called multipleAbsolutePaths_allConverted()");
        List<Path> input = Arrays.asList(
                basePath.resolve("a.txt"),
                basePath.resolve("sub/b.txt"),
                basePath.resolve("sub/deep/c.txt")
        );

        List<Path> result = invoke(input, basePath);

        assertEquals(3, result.size());
        assertEquals(Paths.get("a.txt"),           result.get(0));
        assertEquals(Paths.get("sub/b.txt"),        result.get(1));
        assertEquals(Paths.get("sub/deep/c.txt"),   result.get(2));
    }

    // -----------------------------------------------------------------------
    // 예외 케이스
    // -----------------------------------------------------------------------

    @Test
    public void nullPaths_throwsNullPointerException() {
        System.out.println("===== called nullPaths_throwsNullPointerException()");
        try {
            invoke(null, basePath);
            fail("NullPointerException expected");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void nullBasePath_throwsNullPointerException() {
        System.out.println("===== called nullBasePath_throwsNullPointerException()");
        try {
            invoke(Collections.emptyList(), null);
            fail("NullPointerException expected");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void nullElementInList_throwsNullPointerException() {
        System.out.println("===== called nullElementInList_throwsNullPointerException()");
        try {
            invoke(Collections.singletonList(null), basePath);
            fail("NullPointerException expected");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void absolutePathOutsideBase_throwsIllegalArgumentException() {
        System.out.println("===== called absolutePathOutsideBase_throwsIllegalArgumentException()");
        Path outsidePath = Paths.get("/other/location/file.txt").toAbsolutePath().normalize();
        try {
            invoke(Collections.singletonList(outsidePath), basePath);
            fail("IllegalArgumentException expected");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }
}
