package com.ushareit.core.io.sfile;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import com.ushareit.core.io.FileUtils;
import com.ushareit.core.Assert;
import com.ushareit.core.lang.ObjectStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * We must use DocumentFile class to access the external storage in AndroidL, 
 * SDK should apply tow ways: traditional File object and DocumentFile object.
 * This is utility class designed to emulate the File interface. 
 * inherit class implement the traditional File and DocumentFile in AndroidL.
 */
public abstract class SFile {

    public enum OpenMode {
        Read, Write, RW;
    }

    public interface Filter {
        boolean accept(SFile fs);
    }

    public static void setSupportRenameTo(SFile root, boolean support) {
        if (root instanceof SFileDocumentImpl)
            SFileDocumentImpl.setSupportRenameTo(support);
    }

    /**
     * create instance with File object
     * @param f file object
     * @return
     */
    public static SFile create(File f) {
        return new SFileOriginalImpl(f);
    }

    /**
     * create instance with DocumentFile object
     * @param doc DocumentFile object
     * @return
     */
    public static SFile create(DocumentFile doc) {
        return new SFileDocumentImpl(doc);
    }

    /**
     * create instance with path
     * @param path path or uri string
     * @return
     */
    public static SFile create(String path) {
        Context context = ObjectStore.getContext();
        Uri uri = Uri.parse(path);
        return isDocumentUriSafety(context, uri) ? new SFileDocumentImpl(uri, false) : new SFileOriginalImpl(path);
    }

    /**
     * create instance with path, path represent folder path
     * @param path path or uri string
     * @return
     */
    public static SFile createFolder(String path) {
        Context context = ObjectStore.getContext();
        Uri uri = Uri.parse(path);
        return isDocumentUriSafety(context, uri) ? new SFileDocumentImpl(uri, true) : new SFileOriginalImpl(path);

    }

    /**
     * create instance with parent and name
     * @param fs parent instance
     * @param name
     * @return
     */
    public static SFile create(SFile fs, String name) {
        if (fs instanceof SFileOriginalImpl)
            return new SFileOriginalImpl((SFileOriginalImpl)fs, name);
        else if (fs instanceof SFileDocumentImpl)
            return new SFileDocumentImpl((SFileDocumentImpl)fs, name);
        return null;
    }

    /**
     * automatically add postfix in file name, make it unique in specified parent folder.
     * @param fs the folder
     * @param name the initial file name.
     * @return the unique file name, may have added some postfix, like "fileName (2).ext"
     */
    public static SFile createUnique(SFile fs, String name) {
        String ext = FileUtils.getExtension(name);
        String baseName = FileUtils.getBaseName(name);

        SFile uniquePath = null;
        String uniqueFileName = name;
        int postfix = 0;
        while (true) {
            uniquePath = SFile.create(fs, uniqueFileName);
            if (!uniquePath.exists())
                break;

            postfix++;
            uniqueFileName = baseName + " (" + postfix + ")" + (ext.length() > 0 ? "." + ext : ext);
        }
        return uniquePath;
    }


    /**
     * automatically add postfix in file name, make it unique in specified parent folder.
     * @param fs the folder
     * @param name the initial file name.
     * @return the unique file name, may have added some postfix, like "fileName_1"
     */
    public static SFile createUniqueFolder(SFile fs, String name) {
        SFile uniquePath = null;
        String uniqueFileName = name;
        int postfix = 0;
        while (true) {
            uniquePath = SFile.create(fs, uniqueFileName);
            if (!uniquePath.exists())
                break;

            postfix++;
            uniqueFileName = name + "_" + postfix;
        }
        return uniquePath;
    }

    /**
     * Indicates whether the current context is allowed to write to this file
     * @return
     */
    abstract public boolean canWrite();

    /**
     * Indicates whether the current context is allowed to read from this file.
     * @return
     */
    abstract public boolean canRead();

    /**
     * Returns a boolean indicating whether this file can be found.
     * @return
     */
    abstract public boolean exists();

    /**
     * Returns a boolean indicating whether this file can be found.
     * @return
     */
    abstract public boolean isDirectory();

    /**
     * Returns a boolean indicating whether this file is hidden
     * @return
     */
    abstract public boolean isHidden();

    /**
     * Returns an array of files contained in the directory represented by this file.
     * @return
     */
    abstract public SFile[] listFiles();

    /**
     * Returns an array of file name without path contained in the directory represented by this file.
     * @return
     */
    abstract public String[] list();

    /**
     * Returns an array of files contained in the directory represented by this file and filter by specified.
     * @param filter specified filter
     * @return
     */
    abstract public SFile[] listFiles(Filter filter);

    /**
     * Return the parent file of this document.
     * @return
     */
    abstract public SFile getParent();

    /**
     * Return the absolute path or uri string
     * @return
     */
    abstract public String getAbsolutePath();

    /**
     * Return the display name of this document.
     * @return
     */
    abstract public String getName();

    /**
     * Returns the length of this file in bytes.
     * @return
     */
    abstract public long length();

    /**
     * set last modified time
     * @param last
     */
    abstract public void setLastModified(long last);

    /**
     * Returns the time when this file was last modified, measured in milliseconds since January 1st, 1970, midnight.
     * @return
     */
    abstract public long lastModified();

    /**
     * create new directory
     * @return
     */
    abstract public boolean mkdir();

    /**
     * create directory
     * @return
     */
    abstract public boolean mkdirs();

    /**
     * create new file
     * @return
     */
    abstract public boolean createFile();

    /**
     * delete file
     * @return
     */
    abstract public boolean delete();

    /**
     * rename file to target
     * @param target
     * @return
     */
    abstract public boolean renameTo(SFile target);

    /**
     * get File object
     * @return
     */
    abstract public File toFile();

    /**
     * open file, if mode contains WRITE, will create file if not exists
     * @param mode
     * @throws FileNotFoundException
     */
    abstract public void open(OpenMode mode) throws FileNotFoundException;

    /**
     * seek position
     * @param mode
     * @param offset
     * @throws IOException
     */
    abstract public void seek(OpenMode mode, long offset) throws IOException;

    /**
     * read bytes to buffer from file
     * @param buffer
     * @return
     * @throws IOException
     */
    abstract public int read(byte[] buffer) throws IOException;

    /**
     * read bytes to buffer from file
     * @param buffer
     * @return
     * @throws IOException
     */
    abstract public int read(byte[] buffer, int start, int length) throws IOException;

    /**
     * write bytes to file
     * @param buffer
     * @param offset
     * @param count
     * @throws IOException
     */
    abstract public void write(byte[] buffer, int offset, int count) throws IOException;

    /**
     * close file
     */
    abstract public void close();

    /**
     * get input stream from file
     */
    abstract public InputStream getInputStream() throws IOException;

    /**
     * get output stram from file
     * @return
     */
    abstract public OutputStream getOutputStream() throws IOException;

    /**
     * indicating the SFile instance whether support rename method
     */
    abstract public boolean isSupportRename();

    /**
     * change to uri
     */
    abstract public Uri toUri();

    /**
     * for check DocumentFile class API
     */
    public boolean checkRenameTo(SFile dst) {
        return (dst instanceof SFileOriginalImpl);
    }

    public boolean renameTo(String displayName) {
        throw new IllegalArgumentException("only document support rename(display) method!");
    }

    public static boolean isDocument(SFile file) {
        return (file instanceof SFileDocumentImpl);
    }

    public static boolean isDocumentUri(String uriString) {
        Assert.notNull(uriString);
        Uri uri = Uri.parse(uriString);
        return "content".equals(uri.getScheme());
    }

    private static boolean isDocumentUriSafety(Context context, Uri uri) {
        try {
            return DocumentFile.isDocumentUri(context, uri);
        } catch (NoClassDefFoundError error) {
        }
        return false;
    }
}
