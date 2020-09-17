package com.ushareit.core.io.sfile;

import android.net.Uri;

import com.ushareit.core.Assert;
import com.ushareit.core.utils.Utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

class SFileOriginalImpl extends SFile {

    private File mFile;
    private RandomAccessFile mRandomAccessFile;

    public SFileOriginalImpl(File file) {
        Assert.notNull(file);
        mFile = file;
    }

    public SFileOriginalImpl(String path) {
        mFile = new File(path);
    }

    public SFileOriginalImpl(SFileOriginalImpl parent, String name) {
        mFile = new File(parent.mFile, name);
    }

    @Override
    public boolean canWrite() {
        return mFile.canWrite();
    }

    @Override
    public boolean canRead() {
        return mFile.canRead();
    }

    @Override
    public boolean exists() {
        return mFile.exists();
    }

    @Override
    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    @Override
    public boolean isHidden() {
        return mFile.isHidden();
    }

    @Override
    public SFile[] listFiles() {
        File[] files = mFile.listFiles();
        if (files == null)
            return null;

        List<SFile> results = new ArrayList<SFile>();
        for (File file : files)
            results.add(new SFileOriginalImpl(file));
        return results.toArray(new SFile[results.size()]);
    }

    @Override
    public String[] list() {
        return mFile == null ? null : mFile.list();
    }

    @Override
    public SFile[] listFiles(final Filter filter) {
        File[] files = mFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return filter.accept(new SFileOriginalImpl(pathname));
            }
        });
        if (files == null)
            return null;

        List<SFile> results = new ArrayList<SFile>();
        for (File file : files)
            results.add(new SFileOriginalImpl(file));
        return results.toArray(new SFile[results.size()]);
    }



    @Override
    public SFile getParent() {
        File parent = mFile.getParentFile();
        return (parent != null) ? new SFileOriginalImpl(parent) : null;
    }

    @Override
    public String getAbsolutePath() {
        return mFile.getAbsolutePath();
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public long length() {
        return mFile.length();
    }

    @Override
    public void setLastModified(long time) {
        mFile.setLastModified(time);
    }

    @Override
    public long lastModified() {
        return mFile.lastModified();
    }

    @Override
    public boolean mkdir() {
        return mFile.mkdir();
    }

    @Override
    public boolean mkdirs() {
        return mFile.mkdirs();
    }

    @Override
    public boolean createFile() {
        try {
            return mFile.createNewFile();
        } catch (IOException e) {}
        return false;
    }

    @Override
    public boolean delete() {
        return mFile.delete();
    }

    @Override
    public boolean renameTo(SFile target) {
        return mFile.renameTo(((SFileOriginalImpl)target).mFile);
    }

    @Override
    public File toFile() {
        return mFile;
    }

    @Override
    public void open(OpenMode mode) throws FileNotFoundException {
        String smode = "rw";
        if (mode == OpenMode.Read)
            smode = "r";
        mRandomAccessFile = new RandomAccessFile(mFile, smode);
    }

    @Override
    public void seek(OpenMode mode, long offset) throws IOException {
        mRandomAccessFile.seek(offset);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if (mRandomAccessFile == null)
            throw new IOException("Target file do not opened!");
        mRandomAccessFile.write(buffer, offset, count);
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        if (mRandomAccessFile == null)
            throw new IOException("Target file do not opened!");
        return mRandomAccessFile.read(buffer);
    }

    @Override
    public int read(byte[] buffer, int start, int length) throws IOException {
        if (mRandomAccessFile == null)
            throw new IOException("Target file do not opened!");
        return mRandomAccessFile.read(buffer, start, length);
    }

    @Override
    public void close() {
        Utils.close(mRandomAccessFile);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(mFile);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(mFile);
    }

    @Override
    public boolean isSupportRename() {
        return true;
    }

    @Override
    public Uri toUri() {
        return Uri.fromFile(mFile);
    }
}
