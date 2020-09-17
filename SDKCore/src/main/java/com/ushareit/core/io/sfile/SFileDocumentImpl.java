package com.ushareit.core.io.sfile;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.documentfile.provider.DocumentFile;
import android.text.TextUtils;

import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.io.StorageVolumeHelper;
import com.ushareit.core.io.StorageVolumeHelper.Volume;
import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

class SFileDocumentImpl extends SFile {
    private final static String TAG = "FSDocument";

    private DocumentFile mDocument;
    private String mName;

    private DocumentFile mParent;

    private ParcelFileDescriptor mFd;
    private OutputStream mOutput;
    private InputStream mInput;

    private static boolean mSupportRenameTo = false;

    public SFileDocumentImpl(DocumentFile document) {
        Assert.notNull(document);
        mDocument = document;
    }

    public SFileDocumentImpl(Uri uri, boolean isFolder) {
        Context context = ObjectStore.getContext();
        Assert.isTrue(DocumentFile.isDocumentUri(context, uri));
        if (!isFolder)
            mDocument = DocumentFile.fromSingleUri(context, uri);
        else {
            mDocument = DocumentFile.fromTreeUri(context, uri);
            String dstPath = uri.getLastPathSegment();
            String rootPath = mDocument.getUri().getLastPathSegment();
            String subPath = dstPath.substring(rootPath.length());
            String[] subs = subPath.split(File.separator);
            DocumentFile document = mDocument;
            for (String sub : subs) {
                if (TextUtils.isEmpty(sub))
                    continue;
                document = document.findFile(sub);
                if (document == null) {
                    Assert.fail("This uri can not create document!");
                    return;
                }
            }
            if (document != null)
                mDocument = document;
        }
    }

    public SFileDocumentImpl(SFileDocumentImpl parent, String name) {
        mParent = parent.mDocument;
        mName = name.startsWith(File.separator) ? name.substring(1) : name;
    }

    public static void setSupportRenameTo(boolean support) {
        mSupportRenameTo = support;
    }

    @Override
    public boolean canWrite() {
        if (mDocument == null && mParent != null && mName != null)
            mDocument = mParent.findFile(mName);
        return (mDocument == null) ? false : mDocument.canWrite();
    }

    @Override
    public boolean canRead() {
        if (mDocument == null && mParent != null && mName != null)
            mDocument = mParent.findFile(mName);
        return (mDocument == null) ? false : mDocument.canRead();
    }

    @Override
    public boolean exists() {
        if (mDocument != null)
            return mDocument.exists();

        if (mParent == null || mName == null)
            return false;

        String[] subs = mName.split(File.separator);
        DocumentFile document = mParent;
        for (String sub : subs) {
            if (TextUtils.isEmpty(sub))
                continue;
            document = document.findFile(sub);
            if (document == null)
                return false;
        }
        mDocument = document;
        return true;
    }

    @Override
    public boolean isDirectory() {
        if (mDocument != null)
            return mDocument.isDirectory();

        if (mParent != null && mName != null) {
            String[] subs = mName.split(File.separator);
            DocumentFile document = mParent;
            for (String sub : subs) {
                if (TextUtils.isEmpty(sub))
                    continue;
                document = document.findFile(sub);
                if (document == null)
                    return false;
            }
            mDocument = document;
        }
        return (mDocument == null) ? false : mDocument.isDirectory();
    }

    @Override
    public boolean isHidden() {
        if (mDocument != null)
            return mDocument.getName().startsWith(".");

        if (!TextUtils.isEmpty(mName)) {
            String[] subs = mName.split(File.separator);
            if (subs.length == 0)
                return false;

            for (int i = subs.length - 1; i >= 0; i--) {
                if (TextUtils.isEmpty(subs[i]))
                    continue;
                return subs[i].startsWith(".");
            }
        }
        return false;
    }

    @Override
    public SFile[] listFiles() {
        if (mDocument == null)
            return null;

        DocumentFile[] files = mDocument.listFiles();
        if (files == null)
            return null;

        List<SFile> results = new ArrayList<SFile>();
        for (DocumentFile file : files)
            results.add(new SFileDocumentImpl(file));
        return results.toArray(new SFile[results.size()]);
    }

    @Override
    public String[] list() {
        if (mDocument == null)
            return null;

        DocumentFile[] files = mDocument.listFiles();
        if (files == null)
            return null;

        List<String> results = new ArrayList<>();
        for (DocumentFile file : files)
            results.add(file.getName());
        return results.toArray(new String[results.size()]);
    }

    @Override
    public SFile[] listFiles(Filter filter) {
        if (mDocument == null)
            return null;

        DocumentFile[] files = mDocument.listFiles();
        if (files == null)
            return null;

        List<SFile> results = new ArrayList<SFile>();
        for (DocumentFile file : files) {
            SFileDocumentImpl doc = new SFileDocumentImpl(file);
            if (filter.accept(doc))
                results.add(doc);
        }
        return results.toArray(new SFile[results.size()]);
    }

    @Override
    public SFile getParent() {
        if (mParent != null)
            return new SFileDocumentImpl(mParent);

        DocumentFile parent = mDocument.getParentFile();
        return (parent == null) ? null : new SFileDocumentImpl(parent);
    }

    @Override
    public String getAbsolutePath() {
        if (mDocument != null)
            return mDocument.getUri().toString();
        if (mParent != null && mName != null) {
            String[] subs = mName.split(File.separator);
            DocumentFile document = mParent;
            for (String sub : subs) {
                if (TextUtils.isEmpty(sub))
                    continue;
                document = document.findFile(sub);
                if (document == null)
                    return "";
            }
            mDocument = document;
            return mDocument.getUri().toString();
        }
        return "";
    }

    @Override
    public String getName() {
        if (mDocument != null)
            return mDocument.getName();
        if (mParent != null && !TextUtils.isEmpty(mName)) {
            String[] subs = mName.split(File.separator);
            if (subs.length == 0)
                return mName;

            for (int i = subs.length - 1; i >= 0; i--) {
                if (TextUtils.isEmpty(subs[i]))
                    continue;
                return subs[i];
            }
        }
        return "";
    }

    @Override
    public long length() {
        if (mDocument == null && mParent != null && mName != null) {
            String[] subs = mName.split(File.separator);
            DocumentFile document = mParent;
            for (String sub : subs) {
                if (TextUtils.isEmpty(sub))
                    continue;
                document = document.findFile(sub);
                if (document == null)
                    return 0;
            }
            mDocument = document;
        }
        return (mDocument != null) ? mDocument.length() : 0;
    }

    @Override
    public void setLastModified(long time) {
        // NOTE: documentfile do not support this method
    }

    @Override
    public long lastModified() {
        if (mDocument == null && mParent != null && mName != null) {
            String[] subs = mName.split(File.separator);
            DocumentFile document = mParent;
            for (String sub : subs) {
                if (TextUtils.isEmpty(sub))
                    continue;
                document = document.findFile(sub);
                if (document == null)
                    return 0;
            }
            mDocument = document;
        }

        return (mDocument == null) ? 0 : mDocument.lastModified();
    }

    @Override
    public boolean mkdir() {
        if (mParent == null || mName == null)
            return false;

        try {
            mDocument = mParent.createDirectory(mName);
        } catch (SecurityException se) {
            Logger.w(TAG, "can not create directory, need authority!");
        }
        return mDocument != null;
    }

    @Override
    public boolean mkdirs() {
        if (mParent == null || mName == null)
            return false;

        String[] subs = mName.split(File.separator);
        DocumentFile document = mParent;
        for (String sub : subs) {
            DocumentFile existDoc = document.findFile(sub);
            if (existDoc != null) {
                document = existDoc;
                continue;
            }

            try {
                document = document.createDirectory(sub);
            } catch (SecurityException se) {
                Logger.w(TAG, "can not create directory, need authority!");
            }
            if (document == null || !document.exists())
                return false;
        }
        mDocument = document;
        return true;
    }

    @Override
    public boolean createFile() {
        if (mParent == null || mName == null)
            return false;

        try {
            mDocument = mParent.createFile("", mName);
        } catch (SecurityException se) {
            Logger.w(TAG, "can not create file, need authority!");
        }
        return mDocument != null;
    }

    @Override
    public boolean delete() {
        try {
            if (mDocument != null)
                return mDocument.delete();
            if (mParent == null || mName == null)
                return false;
            mDocument = mParent.findFile(mName);
            return (mDocument == null) ? false : mDocument.delete();
        } catch (SecurityException se) {
            Logger.w(TAG, "can not delete file, need authority!");
            return false;
        } finally {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public boolean renameTo(SFile target) {
        if (mDocument == null || !mDocument.exists())
            return false;

        if (mSupportRenameTo) {
            String dstPath = target.getAbsolutePath();
            String srcPath = mDocument.getUri().getLastPathSegment();
            String[] dstElements = dstPath.split(File.separator);
            String[] srcElements = srcPath.split(File.separator);
            int dstPathCnt = dstElements.length - 1;
            int srcPathCnt = srcElements.length - 1;
            int position = 0;
            for (; position < dstPathCnt; position++) {
                if (position >= srcPathCnt || !dstElements[position].equals(srcElements[position]))
                    break;
            }
            String displayName = "";
            int backCount = srcPathCnt - position;
            for (int i = 0; i < backCount; i++)
                displayName += (".." + File.separator);
            for (; position < dstElements.length; position++)
                displayName += (dstElements[position] + ((position == dstElements.length - 1) ? "" : File.separator));

            try {
                return mDocument.renameTo(displayName);
            } catch (SecurityException se) {
                Logger.w(TAG, "can not renameto file, need authority!");
                return false;
            }
        } else {
            try {
                FileUtils.move(this, target);
            } catch (IOException e) {
                return false;
            }
            return true;
        }
    }

    @Override
    public File toFile() {
        if (mDocument == null)
            mDocument = mParent.findFile(mName);
        if (mDocument == null)
            return new File("");

        // 0 means storage uuid, 1 means path
        String[] opers = mDocument.getUri().getLastPathSegment().split(":");
        if (opers.length == 0)
            return new File("");

        String sdPath = null;
        List<Volume> volumes = StorageVolumeHelper.getVolumeList(ObjectStore.getContext());
        for (Volume v : volumes) {
            String uuid = TextUtils.isEmpty(v.mUuid) ? (v.mIsPrimary ? "primary" : "") : v.mUuid;
            // NOTE: some device like p1ma40 lenovo, do it step like below:
            // 1. set external storage SD as default storage in system setting;
            // 2. choose internal storage as app directory and get auth
            // 3. storage tag is primary in document uri
            // 4. but only external storage volume is primary
            if (uuid.equals(opers[0]) || ("primary".equals(opers[0]) && TextUtils.isEmpty(v.mUuid) && !v.mIsPrimary)) {
                sdPath = v.mPath;
                break;
            }
        }
        if (sdPath == null)
            return new File("");
        return (opers.length < 2) ? new File(sdPath) : new File(sdPath, opers[1]);
    }

    @Override
    public void open(OpenMode mode) throws FileNotFoundException {
        Context context = ObjectStore.getContext();
        if (mDocument == null && mParent != null && mName != null)
            mDocument = mParent.createFile("", mName);
        if (mDocument == null)
            throw new IllegalArgumentException("Can not create file!");

        // Must keep the fd life circle same with out/in stream.
        // Otherwise, fd may be closed by VM suddenly, write/read will throw exception.
        mFd = context.getContentResolver().openFileDescriptor(mDocument.getUri(), "rw");
        if (mode == OpenMode.RW || mode == OpenMode.Write)
            mOutput = new FileOutputStream(mFd.getFileDescriptor());
        else if (mode == OpenMode.Read)
            mInput = new FileInputStream(mFd.getFileDescriptor());
    }

    @Override
    public void seek(OpenMode mode, long offset) throws IOException {
        FileChannel channel = null;
        if (mode == OpenMode.RW || mode == OpenMode.Write)
            channel = ((FileOutputStream)mOutput).getChannel();
        else if (mode == OpenMode.Read)
            channel = ((FileInputStream)mInput).getChannel();
        channel.position(offset);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if (mOutput == null)
            throw new IOException("Target file do not opened!");
        mOutput.write(buffer, offset, count);
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        if (mInput == null)
            throw new IOException("Target file do not opened!");
        return mInput.read(buffer);
    }

    @Override
    public int read(byte[] buffer, int start, int length) throws IOException {
        if (mInput == null)
            throw new IOException("Target file do not opened!");
        return mInput.read(buffer, start, length);
    }

    @Override
    public void close() {
        if (mOutput != null) {
            Utils.close(mOutput);
            mOutput = null;
        }
        if (mInput != null) {
            Utils.close(mInput);
            mInput = null;
        }
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        if (mInput == null) {
            Context context = ObjectStore.getContext();
            if (mDocument == null && mParent != null && mName != null)
                mDocument = mParent.createFile("", mName);
            if (mDocument == null)
                throw new IllegalArgumentException("Can not create file!");

            mFd = context.getContentResolver().openFileDescriptor(mDocument.getUri(), "rw");
            mInput = new FileInputStream(mFd.getFileDescriptor());
        }
        return mInput;

    }

    @Override
    public OutputStream getOutputStream() throws FileNotFoundException {
        if (mOutput == null) {
            Context context = ObjectStore.getContext();
            if (mDocument == null && mParent != null && mName != null)
                mDocument = mParent.createFile("", mName);
            if (mDocument == null)
                throw new IllegalArgumentException("Can not create file!");

            mFd = context.getContentResolver().openFileDescriptor(mDocument.getUri(), "rw");
            mOutput = new FileOutputStream(mFd.getFileDescriptor());
        }
        return mOutput;
    }

    @Override
    public boolean isSupportRename() {
        return mSupportRenameTo;
    }

    @Override
    public boolean checkRenameTo(SFile dst) {
        String dstPath = dst.getAbsolutePath();
        String srcPath = mDocument.getUri().getLastPathSegment();
        String[] dstElements = dstPath.split(File.separator);
        String[] srcElements = srcPath.split(File.separator);
        int dstPathCnt = dstElements.length - 1;
        int srcPathCnt = srcElements.length - 1;
        int position = 0;
        for (; position < dstPathCnt; position++) {
            if (position >= srcPathCnt || !dstElements[position].equals(srcElements[position]))
                break;
        }
        String displayName = "";
        int backCount = srcPathCnt - position;
        for (int i = 0; i < backCount; i++)
            displayName += (".." + File.separator);
        for (; position < dstElements.length; position++)
            displayName += (dstElements[position] + ((position == dstElements.length - 1) ? "" : File.separator));

        try {
            return mDocument.renameTo(displayName);
        } catch (SecurityException se) {
            Logger.w(TAG, "can not check renameto file, need authority!");
            return false;
        }
    }

    @Override
    public boolean renameTo(String displayName) {
        if (mDocument == null || !mDocument.exists())
            return false;

        try {
            return mDocument.renameTo(displayName);
        } catch (SecurityException se) {
            Logger.w(TAG, "can not renameto file, need authority!");
            return false;
        }
    }

    @Override
    public Uri toUri() {
        if (mDocument == null)
            return null;
        return mDocument.getUri();
    }
}
