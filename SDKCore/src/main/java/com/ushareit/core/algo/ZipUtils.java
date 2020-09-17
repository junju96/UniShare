package com.ushareit.core.algo;

import android.util.Pair;

import com.ushareit.core.utils.Utils;
import com.ushareit.core.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private static final String TAG = "ZipUtils";

    private static final int BUFFER_SIZE = 1024 * 4;

    private ZipUtils() {}

    public static boolean zip(String srcFilePath, String zipFilePath) {
        ZipOutputStream zos = null;
        try {
            // Make Zip File
            zos = new ZipOutputStream(new FileOutputStream(zipFilePath));
            // Open Output Files
            File file = new File(srcFilePath);
            // Compress
            zipFiles(file.getParent() + File.separator, file.getName(), zos);
            return true;
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return false;
        } finally {
            Utils.close(zos);
        }
    }

    public static void zip(List<String> srcPaths, OutputStream out) throws IOException {
        ZipOutputStream zipOut = null;

        try {
            zipOut = new ZipOutputStream(out);
            for (String filePath : srcPaths) {
                FileInputStream fis = null;
                try {
                    File file = new File(filePath);
                    fis = new FileInputStream(file);
                    zipOut.putNextEntry(new ZipEntry(file.getName()));
                    int length;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((length = fis.read(buffer)) >= 0)
                        zipOut.write(buffer, 0, length);
                    zipOut.closeEntry();
                } finally {
                    Utils.close(fis);
                }
            }
        } finally {
            Utils.close(zipOut);
        }
    }

    public static Pair<Boolean, String> unzip(String zipFilePath, String folderPath) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipFilePath);
            @SuppressWarnings("rawtypes")
            Enumeration en = zipFile.entries();
            while (en.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry)en.nextElement();
                String fileName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    fileName = fileName.substring(0, fileName.length() - 1);
                    new File(folderPath + File.separator + fileName).mkdirs();
                } else {
                    File file = new File(folderPath + File.separator + fileName);
                    if (!file.getParentFile().exists())
                        file.getParentFile().mkdirs();
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    int length;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    InputStream zis = zipFile.getInputStream(zipEntry);
                    if (zis != null) {
                        while ((length = zis.read(buffer)) != -1) {
                            // write (len) byte from buffer at the position 0
                            fos.write(buffer, 0, length);
                            fos.flush();
                        }
                        Utils.close(zis);
                    }
                    Utils.close(fos);
                }
            }
            return new Pair<Boolean, String>(true, null);
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return new Pair<Boolean, String>(false, e.getMessage());
        } finally {
            close(zipFile);
        }
    }

    public static boolean checkZip(String zipFilePath) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipFilePath);
            @SuppressWarnings("rawtypes")
            Enumeration en = zipFile.entries();
            while (en.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry)en.nextElement();
                if (!zipEntry.isDirectory()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    InputStream zis = zipFile.getInputStream(zipEntry);
                    if (zis != null) {
                        while (zis.read(buffer) != -1) {
                        }
                        Utils.close(zis);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
            return false;
        } finally {
            close(zipFile);
        }
    }

    public static List<String> getFileListFromZip(String zipFilePath, boolean containFolder, boolean containFile) {
        List<String> fileList = new ArrayList<String>();
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipFilePath);
            @SuppressWarnings("rawtypes")
            Enumeration en = zipFile.entries();
            while (en.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry)en.nextElement();
                String fileName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    // get the folder name of the widget
                    fileName = fileName.substring(0, fileName.length() - 1);
                    if (containFolder) {
                        fileList.add(fileName);
                    }
                } else {
                    if (containFile) {
                        fileList.add(fileName);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            close(zipFile);
        }
        return fileList;
    }

    private static void zipFiles(String folderPath, String filePath, ZipOutputStream zipOutputStream) {
        if (zipOutputStream == null)
            return;
        File file = new File(folderPath + filePath);
        FileInputStream fis = null;
        try {
            if (file.isFile()) {
                fis = new FileInputStream(file);
                zipOutputStream.putNextEntry(new ZipEntry(filePath));
                int length;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((length = fis.read(buffer)) != -1)
                    zipOutputStream.write(buffer, 0, length);
                zipOutputStream.closeEntry();
            } else {
                String fileList[] = file.list();
                if (fileList == null)
                    return;
                else if (fileList.length <= 0) {
                    zipOutputStream.putNextEntry(new ZipEntry(filePath + File.separator));
                    zipOutputStream.closeEntry();
                }
                for (int i = 0; i < fileList.length; i++) {
                    zipFiles(folderPath, filePath + File.separator + fileList[i], zipOutputStream);
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            Utils.close(fis);
        }
    }

    public static void close(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {}
        }
    }


    public interface ZipWriteListener {
        void onWriteBytes(long bytes);
        void onFinish();
        void onError();
    }

    public static void zip(String name, List<String> srcPaths, OutputStream out, ZipWriteListener listener) throws IOException {
        ZipOutputStream zipOut = null;

        try {
            zipOut = new ZipOutputStream(out);
            for (String filePath : srcPaths) {
                FileInputStream fis = null;
                try {
                    File file = new File(filePath);
                    fis = new FileInputStream(file);
                    String entryName = file.getName();
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    int length;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((length = fis.read(buffer)) >= 0) {
                        zipOut.write(buffer, 0, length);
                        if (listener != null) {
                            try {
                                listener.onWriteBytes(length);
                            } catch (Exception e) {}
                        }
                    }
                    zipOut.closeEntry();
                } finally {
                    Utils.close(fis);
                }
            }
            if (listener != null) {
                listener.onFinish();
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onError();
            }
            throw new IOException("zip file failed!", e);
        } finally {
            Utils.close(zipOut);
        }
    }

    public static void zipFiles(File file, String relativePath, ZipOutputStream zipOutputStream, ZipWriteListener listener, boolean isRoot) throws IOException{
        if (zipOutputStream == null)
            return;
        FileInputStream fis = null;
        try {
            if (file.isFile()) {
                fis = new FileInputStream(file);
                zipOutputStream.putNextEntry(new ZipEntry(relativePath));
                int length;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((length = fis.read(buffer)) != -1) {
                    zipOutputStream.write(buffer, 0, length);
                    if (listener != null) {
                        try {
                            listener.onWriteBytes(length);
                        } catch (Exception e) {}
                    }
                }
                zipOutputStream.closeEntry();
            } else {
                File[] fileList = file.listFiles();
                if (fileList == null)
                    return;
                else if (fileList.length <= 0) {
                    zipOutputStream.putNextEntry(new ZipEntry(relativePath + File.separator));
                    zipOutputStream.closeEntry();
                }

                for (File subFile : fileList) {
                    String tempPath = relativePath + subFile.getName();
                    if (subFile.isDirectory())
                        tempPath += File.separator;

                    zipFiles(subFile, tempPath, zipOutputStream, listener, false);
                }
            }
            if (isRoot && listener != null) {
                listener.onFinish();
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onError();
            }
            throw new IOException("zip file failed!", e);
        } finally {
            Utils.close(fis);
        }
    }
}
