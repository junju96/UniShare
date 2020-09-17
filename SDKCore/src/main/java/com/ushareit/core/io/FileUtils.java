package com.ushareit.core.io;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.ushareit.ccf.config.IBasicKeys;
import com.ushareit.core.CloudConfig;
import com.ushareit.core.lang.StringUtils;
import com.ushareit.core.utils.ui.EmojiFilterUtils;
import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.sfile.SFile;
import com.ushareit.core.io.sfile.SFile.OpenMode;
import com.ushareit.core.io.StorageVolumeHelper.Volume;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.utils.i18n.LocaleUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *  SW split this file to several:
 *  PathUtils (path string manipulator related)
 *  StreamUtils (File Operation related)
 *  StorageUtils (Storage/PrivateExtAppDir related)
 *  FileIOUtils (read/write related)
 */
public final class FileUtils {
    private static final String TAG = "FileUtils";

    public static final String PREFIX_PHOTO = "image/";
    public static final String PREFIX_VIDEO = "video/";
    public static final String PREFIX_APK = "application/";
    public static final String PREFIX_MUSIC = "audio/";
    public static final String PREFIX_CONTACT = "text/x-vcard";

    private static final int MAX_LENGTH_FILE_NAME = 255;
    private static final int MAX_LENGTH_UNIQUE_FILE_NAME = 240;     // add hash code

    private FileUtils() {}

    // ---------------------- file path utilities

    public static String getMimeType(File file) {
        return getMimeType(file.getName());
    }

    public static String getMimeType(String fileName) {
        String ext = LocaleUtils.toLowerCaseIgnoreLocale(getExtension(fileName));
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    /**
     * make a full path by parent and child, add seperator between them if needed.
     * @param parent may or may not have ending /
     * @param child child name, maybe relative path or just file/folder name, shouldn't starts with /
     * @return the combined full path
     */
    public static String makePath(String parent, String child) {
        if (parent == null)
            return child;
        if (child == null)
            return parent;

        Assert.isFalse(child.startsWith(File.separator));
        return parent + (!parent.endsWith(File.separator) ? File.separator : "") + child;
    }

    /**
     * return file extension name, without .
     */
    public static String getExtension(String filename) {
        String extension = "";
        if (filename != null && filename.length() > 0) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                extension = filename.substring(dot + 1);
            }
        }
        return extension;
    }

    /**
     * get file name's base part, excluding ".ext"
     * @param path file name, may be full path.
     * @return
     */
    public static String getBaseName(String path) {
        if (path == null)
            return null;

        int index = path.lastIndexOf(File.separatorChar);
        if (index >= 0)
            path = path.substring(index + 1);

        index = path.lastIndexOf('.');
        if (index >= 0)
            path = path.substring(0, index);

        return path;
    }

    /**
     * get file name part, including extension
     * @param path
     * @return
     */
    public static String getFileName(String path) {
        if (path == null)
            return "";
        int index = path.lastIndexOf(File.separatorChar);
        if (index < 0) // compatible windows separator
            index = path.lastIndexOf('\\');
        return index < 0 ? path : path.substring(index + 1);
    }

    public static String getParentPath(String path) {
        File file = new File(path);
        return file.getParent();
    }

    private static List<Volume> sVolumeList;

    public static String getLocation(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return null;
        if (sVolumeList == null)
            sVolumeList = StorageVolumeHelper.getVolumeList(ObjectStore.getContext());

        String location = getParentPath(filePath);
        for (StorageVolumeHelper.Volume volume : sVolumeList) {
            if(location.startsWith(volume.mPath)) {
                location = "/SDCard" + location.substring(volume.mPath.length());
                return location;
            }
        }
        return location;
    }

    public static String getParentName(String path) {
        File file = new File(path);
        File parentFile = file.getParentFile();
        if (parentFile == null)
            return null;
        return parentFile.getName();
    }

    public static long getFileSize(File f) {
        if (f == null || !f.exists())
            return 0;

        if (f.isFile())
            return f.length();
        else
            return getFolderSize(f);
    }

    public static long getSpecialFileFolderSize(File root) {
        File flist[] = root.listFiles();
        if (flist == null || flist.length == 0)
            return 0;

        long size = 0;
        int fLen = flist.length;
        for (int i = 0; i < fLen; i++)
            size = size + flist[i].length();
        return size;
    }

    public static long getFolderSize(String path) {
        return getFolderSize(new File(path));
    }

    /**
     * get folder size recursively
     */
    public static long getFolderSize(File folder) {
        if (folder == null || !folder.isDirectory())
            return -1;

        long size = 0;
        try {
            File[] files = folder.listFiles();
            if (files != null)
                for (File file : files)
                    size += (file.isDirectory()) ? getFolderSize(file) : file.length();
        } catch (Exception e) { // maybe throw SecurityException, about files read and write permissions
            Logger.d(TAG, e.toString());
        }
        return size;
    }

    public static boolean isNoMediaFolder(String path) {
        if (StringUtils.isEmpty(path))
            return false;

        File folder = new File(path);
        if (!folder.isDirectory())
            folder = folder.getParentFile();

        return new File(folder, MediaStore.MEDIA_IGNORE_FILENAME).exists() || (folder.getParentFile() != null && isNoMediaFolder(folder.getParentFile().getPath()));
    }

    public static boolean isHideFile(String filePath) {
        if (StringUtils.isEmpty(filePath))
            return false;

        File file = new File(filePath);
        return file.isHidden() || (file.getParentFile() != null && isHideFile(file.getParentFile().getPath()));
    }

    public static String concatFilePaths(String... paths) {
        StringBuilder builder = new StringBuilder();
        boolean lastWithSeparator = false;
        for (String path : paths) {
            if (TextUtils.isEmpty(path.trim()))
                continue;

            if (builder.length() > 0) {
                boolean firstWithSeparator = path.indexOf(File.separatorChar) == 0;
                if (firstWithSeparator && lastWithSeparator)
                    path = path.substring(1);
                else if (!firstWithSeparator && !lastWithSeparator)
                    builder.append(File.separatorChar);
            }
            builder.append(path);
            lastWithSeparator = path.lastIndexOf(File.separatorChar) == path.length() - 1;
        }
        return builder.toString();
    }

    public static String extractFileName(String fileName) {
        // for CJK code, UTF length should be 80*3 < 255
        final int MAX_FILENAME_LEN = 80;

        if (fileName.length() < MAX_FILENAME_LEN)
            return fileName;

        String baseName = getBaseName(fileName);
        String ext = getExtension(fileName);
        if (ext.length() + 1 >= MAX_FILENAME_LEN)
            return fileName.substring(0, MAX_FILENAME_LEN);
        int end = MAX_FILENAME_LEN - (ext.length() + 1);
        return baseName.substring(0, end) + "." + ext;
    }

    // ---------------------- file operation utilities

    public static void move(SFile src, SFile dst) throws IOException {
        copy(src, dst);
        src.delete();
    }

    public static void fastCopy(SFile src, SFile target) throws Exception {
        FileChannel in = null, out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = (FileInputStream) src.getInputStream();
            outStream = (FileOutputStream) target.getOutputStream();
            in = inStream.getChannel();
            out = outStream.getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Throwable e) {
            throw new Exception("fastCopy failed!", e);
        } finally {
            Utils.close(inStream);
            Utils.close(in);
            Utils.close(outStream);
            Utils.close(out);
        }
    }

    public static void copy(SFile srcFile, SFile dstFile) throws IOException {
        if (srcFile == null)
            throw new RuntimeException("source file is null.");
        if (!srcFile.exists())
            throw new RuntimeException("source file[" + srcFile.getAbsolutePath() + "] is not exists.");

        try {
            srcFile.open(OpenMode.Read);
            dstFile.open(OpenMode.Write);

            byte[] buffer = new byte[1024 * 16];
            int bytesRead;
            while ((bytesRead = srcFile.read(buffer)) != -1)
                dstFile.write(buffer, 0, bytesRead);
        } finally {
            srcFile.close();
            dstFile.close();
        }
    }

    public static void moveFolder(SFile src, SFile dst) throws Exception {
        try {
            copyExt(src, dst);
            removeFolder(src);
        } catch (Exception e) {
            removeFolder(dst);
            throw e;
        }
    }

    public static void copyExt(SFile src, SFile dst) throws Exception {
        if (src.isDirectory()) {
            copyFolder(src, dst);
        } else {
            fastCopy(src, dst);
        }
    }

    public static void copyFolder(SFile src, SFile dst) throws Exception{
        if (!dst.exists() && !dst.mkdir())
            throw new IOException("dst mkdir failed! dst : " + dst.getAbsolutePath());


        for (String f : src.list()) {
            copyExt(SFile.create(src, f), SFile.create(dst, f));
        }
    }

    public static final void removeFolderDescents(SFile parent) {
        removeFolderDescents(parent, false);
    }

    public static final void removeMediaFolderDescents(SFile parent) {
        removeFolderDescents(parent, true);
    }
    /**
     * remove folder's descents recursively, DON't REMOVE root directory itself.
     * note: upon return, the contents of folder may not completed removed if errors occurs.
     * @param parent the root folder
     * @param scan del library db and scan
     */
    private static final void removeFolderDescents(SFile parent, boolean scan) {
        if (parent == null || !parent.exists())
            return;

        Assert.isTrue(parent.isDirectory());
        SFile[] files = parent.listFiles();
        if (files == null)
            return;

        for (SFile item : files) {
            boolean isDir = item.isDirectory();
            if (isDir)
                removeFolderDescents(item, scan);

            item.delete();
            if (!isDir && scan)
                notifyMediaFileScan(item);

        }
    }

    public static final void removeFolder(SFile parent) {
        removeFolder(parent, false);
    }

    public static final void removeMediaFolder(SFile parent) {
        removeFolder(parent, true);
    }

    /**
     * remove folder's descents recursively, AND REMOVE root directory itself.
     * @param parent the root folder
     * @param scan del library db and scan
     */
    private static final void removeFolder(SFile parent, boolean scan) {
        if (parent == null || !parent.exists())
            return;

        Assert.isTrue(parent.isDirectory());
        SFile[] files = parent.listFiles();
        if (files != null) {
            for (final SFile item : files) {
                if (item.isDirectory())
                    removeFolder(item, scan);
                else {
                    item.delete();
                    if (scan)
                        notifyMediaFileScan(item);
                }
            }
        }
        parent.delete();
    }

    public static final boolean removeFile(SFile file) {
        if (file == null || file.isDirectory() || !file.exists())
            return false;

        if (!file.delete())
            return false;

        notifyMediaFileScan(file);
        return true;
    }

    public static final void notifyMediaFileScan(final SFile file) {
        if (file == null)
            return;

        TaskHelper.execByIoThreadPoll(new TaskHelper.RunnableWithName("FileUtils#removeMedia") {
            @Override
            public void execute() {
                MediaUtils.scanFileForDel(ObjectStore.getContext(), file.toFile());
            }
        });
    }

    public static long getDataStorageAvailableSize() {
        File root = Environment.getDataDirectory();
        return getStorageAvailableSize(root.getAbsolutePath());
    }

    public static long getExternalStorageAvailableSize() {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED))
            return 0;

        File path = Environment.getExternalStorageDirectory();
        return getStorageAvailableSize(path.getAbsolutePath());
    }

    public static long getCurrentExternalStorageAvailableSize(Context context) {
        String path = getExternalStorage(context);
        return getStorageAvailableSize(path);
    }

    @SuppressWarnings("deprecation")
    public static long getStorageAvailableSize(String filePath) {
        // maybe happened IO or security exception
        try {
            StatFs stat = new StatFs(filePath);
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    public static long getStorageTotalSize(String filePath) {
        // maybe happened IO or security exception
        try {
            StatFs stat = new StatFs(filePath);
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getExternalStorage(Context context) {
        Volume currentVolume = StorageVolumeHelper.getVolume(context);
        return currentVolume.mPath;
    }

    public static String getCacheDirectory(Context context, String cacheFileName) {
        File cacheDir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            long size = 0;
            for (File file : context.getExternalCacheDirs()) {
                if (file == null)
                    continue;
                long availableSize = FileUtils.getStorageAvailableSize(file.getAbsolutePath());
                if (availableSize > size) {
                    size = availableSize;
                    cacheDir = file;
                }
            }
        } else {
            cacheDir = context.getExternalCacheDir();
        }
        if (cacheDir == null || !cacheDir.canWrite())
            cacheDir = context.getCacheDir();
        File cacheDirectory = new File(cacheDir, cacheFileName);
        if (!cacheDirectory.mkdirs() && (!cacheDirectory.exists() || !cacheDirectory.isDirectory())) {
            return cacheDir.getAbsolutePath();
        }
        return cacheDirectory.getAbsolutePath();
    }

    public static List<String> getAllExternalStorage(Context ctx) {
        List<String> storages = new ArrayList<String>();
        List<Volume> volumes = StorageVolumeHelper.getVolumeList(ctx);

        if (volumes.size() == 0)
            storages.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        else {
            for (int i = 0; i < volumes.size(); i++)
                storages.add(volumes.get(i).mPath);
        }

        return storages;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static File getPrivateExtAppDir(Context context, String root) {
        File f = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                File[] dirs = context.getExternalFilesDirs(null);
                for (File dir : dirs) {
                    if (dir != null && dir.getAbsolutePath().startsWith(root)) {
                        f = dir;
                        break;
                    }
                }
            } catch (NoSuchMethodError e) {
            } catch (SecurityException e) {
            } catch (NullPointerException e) {
            }
        }
        if (f == null) {
            // maybe, this method will return internal path, but, System will create all app's file dir
            // getExternalFilesDir also call getExternalFilesDirs on API >= KK, and maybe case ArrayIndexOutOfBoundsException
            // if getExternalFilesDirs's size is 0
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                    context.getExternalFilesDir(null);
            } catch (NoSuchMethodError e) {
            } catch (SecurityException e) {
            }

            f = getDefaultPrivateExtAppDir(context, root);
        }
        return f;
    }

    public static File getDefaultPrivateExtAppDir(Context context, String root) {
        return new File(root, "/Android/data/" + context.getPackageName());
    }

    public static String requestValidFileName(String parentFolder, String fileName) {
        String filePath = makePath(parentFolder, fileName);
        if (filePath.length() <= MAX_LENGTH_FILE_NAME)
            return fileName;

        String ext = getExtension(fileName);
        String baseName = getBaseName(fileName);
        int overLength = filePath.length() - MAX_LENGTH_UNIQUE_FILE_NAME;
        if (baseName.length() <= overLength)
            return fileName;

        baseName = baseName.substring(0, baseName.length() - overLength);
        return (baseName + fileName.hashCode() + (ext.length() > 0 ? "." + ext : ext));
    }

    public static boolean isSDCardMounted() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Logger.i(TAG, "MEDIA_MOUNTED+++");
            return true;
        } else {
            Logger.i(TAG, "MEDIA_UNMOUNTED---");
            return false;
        }
    }

    public static boolean isFileExist(String path) {
        if (path == null || path.length() <= 0)
            return false;

        File f = new File(path);
        return f.exists();
    }

    public static boolean isEmptyFolder(File folder) {
        if (folder == null || folder.isFile())
            return false;

        String[] list = folder.list();
        if (list == null || list.length == 0)
            return true;
        return false;
    }

    private static final String FILE_NOMEDIA = "." + "nomedia";
    public static void createNoMediaFile(SFile dir) {
        SFile noMedia = SFile.create(dir, FILE_NOMEDIA);
        if (!noMedia.exists())
            noMedia.createFile();
    }

    public static boolean removeNoMediaFile(final SFile dir) {
        SFile noMedia = SFile.create(dir, FILE_NOMEDIA);
        return noMedia.exists() ? noMedia.delete() : false;
    }

    public static boolean isAssetFile(String pathInAssetsDir) {
        return !TextUtils.isEmpty(pathInAssetsDir) && pathInAssetsDir.startsWith("file:///android_asset");
    }

    public static boolean isLocalFileUri(String uri) {
        return !TextUtils.isEmpty(uri) && uri.startsWith("file://");
    }

    public static List<File> findFileByName(final String dir, final String fileName){
        if(TextUtils.isEmpty(dir) || TextUtils.isEmpty(fileName)){
            return null;
        }
        File dirFile = new File(dir);
        if(!dirFile.exists()){
            return null;
        }
        final List<String> childDirs = new ArrayList<>();
        File[] files = dirFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(file.isDirectory()){
                    childDirs.add(file.getAbsolutePath());
                }
                return file.isFile() && file.getName().equals(fileName);
            }
        });
        List<File> results = new ArrayList<>(Arrays.asList(files));
        for (String childDir : childDirs){
            results.addAll(findFileByName(childDir, fileName));
        }
        return results;
    }

    public static List<File> findFileByName(List<String> dirs, String fileName){
        return findFileByName(dirs, fileName, false);
    }

    public static List<File> findFileByName(List<String> dirs, String fileName, boolean isSortDate){
        List<File> files = new ArrayList<>();
        for(String dir : dirs){
            List<File> result = findFileByName(dir, fileName);
            if(result!=null){
                files.addAll(result);
            }
        }

        Log.e(TAG, files.toString());

        //按最后修改时间排序
        if(isSortDate){
            Collections.sort(files, new FileDateComparator());
        }
        return files;
    }

    public static String escapeFileName(String fileName) {
        // invalid character in file name
        final String RegExp = CloudConfig.getStringConfig(ObjectStore.getContext(), IBasicKeys.KEY_CFG_ESCAPE_FILE_NAME_REGEXP, "[\\\\/:*#?\"<>|\r\n\\s+]");

        Pattern compiler = Pattern.compile(RegExp);
        Matcher matcher = compiler.matcher(fileName);
        return EmojiFilterUtils.filterEmoji(matcher.replaceAll("_"));
    }

    public static class FileDateComparator implements Comparator<File>{
        @Override
        public int compare(File f1, File f2) {
            return Long.valueOf(f1.lastModified()).intValue() - Long.valueOf(f2.lastModified()).intValue();
        }
    }
}
