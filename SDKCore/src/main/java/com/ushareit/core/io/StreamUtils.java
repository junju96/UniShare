package com.ushareit.core.io;

import com.ushareit.core.lang.ObjectStore;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.Assert;
import com.ushareit.core.Logger;
import com.ushareit.core.io.sfile.SFile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO should refactoring with FileUtils.java
public final class StreamUtils {
    private static final String TAG = "StreamUtils";

    private static final int BUFFER_SIZE = 1024 * 64;

    private StreamUtils() {}

    public static void inputStreamToOutputStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int r;
        while ((r = input.read(buffer)) != -1) {
            output.write(buffer, 0, r);
        }
    }

    // read everything in an input stream and return as string (trim-ed, and may apply optional utf8 conversion)
    public static String inputStreamToString(final InputStream is, final boolean sourceIsUTF8) throws IOException {
        InputStreamReader isr = sourceIsUTF8 ? new InputStreamReader(is, Charset.forName("UTF-8")) : new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line);
        br.close();
        return sb.toString().trim();
    }

    public static int readBuffer(InputStream input, byte[] buffer) throws IOException {
        return readBuffer(input, buffer, 0, buffer.length);
    }

    /**
     * inputstream 读取byte[] buffer不能保证�?��完整读取，使用本方法可以保证填满buffer
     * @param input
     * @param buffer
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static int readBuffer(InputStream input, byte[] buffer, int offset, int length) throws IOException {
        int sum = 0;
        int r;
        while (length > 0 && (r = input.read(buffer, offset, length)) != -1) {
            sum += r;
            offset += r;
            length -= r;
        }
        return sum;
    }

    public static void writeStringToFile(String str, SFile file) throws IOException {
        try {
            file.open(SFile.OpenMode.Write);
            byte[] buffer = str.getBytes("UTF-8");
            file.write(buffer, 0, buffer.length);
        } finally {
            file.close();
        }
    }

    public static String readStringFromFile(SFile file) throws IOException {
        return readStringFromFile(file, Integer.MAX_VALUE);
    }

    public static String readStringFromFile(SFile file, int count) throws IOException {
        try {
            file.open(SFile.OpenMode.Read);
            int bufferSize = Math.min((int)file.length(), count);
            byte[] buffer = new byte[bufferSize];
            file.read(buffer, 0, bufferSize);
            return new String(buffer);
        } finally {
            file.close();
        }
    }

    public static String readStringFromRaw(int rawId) {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = null;
        try {
            inputStream = ObjectStore.getContext().getResources().openRawResource(rawId);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                Logger.e(TAG, "read file error!", e);
            }
        } catch (Exception e) {
            Logger.e(TAG, "read file error!", e);
        } finally {
            Utils.close(inputStream);
        }
        return sb.toString();
    }

    public static byte[] readBufferFromFile(SFile file, long offset, int length) throws IOException {
        if (file == null || file.length() <= offset)
            return null;
        try {
            file.open(SFile.OpenMode.Read);
            file.seek(SFile.OpenMode.Read, offset);
            int size = Math.min((int) (file.length() - offset), length);
            byte[] buffer = new byte[size];
            file.read(buffer);
            return buffer;
        } finally {
            file.close();
        }
    }

    public static int[] readHashLinesFromFile(File file, boolean isSort) throws IOException {
        Assert.notNull(file);
        List<Integer> list = new ArrayList<Integer>();

        FileReader reader = null;
        BufferedReader br = null;
        try {
            reader = new FileReader(file);
            br = new BufferedReader(reader);
            String line = null;

            while ((line = br.readLine()) != null)
                list.add(line.hashCode());
        } catch (IOException e) {
            Logger.e(TAG, e);
            throw e;
        } finally {
            Utils.close(reader);
            Utils.close(br);
        }

        if (isSort)
            Collections.sort(list);

        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
            array[i] = list.get(i);

        return array;
    }

    public static int[] readIntArrayFromFile(File file) throws IOException {
        Assert.notNull(file);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            return readIntArrayFromInputStream(fis);
        } catch (IOException e) {
            Logger.e(TAG, e);
            throw e;
        } finally {
            Utils.close(fis);
        }
    }

    public static int[] readIntArrayFromInputStream(InputStream is) throws IOException {
        Assert.notNull(is);

        int length = is.available();
        length = (length % 4 == 0) ? (length / 4) : (length / 4 + 1);
        int[] array = new int[length];

        DataInputStream dis = null;
        try {
            dis = new DataInputStream(is);

            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readInt();
            }
        } catch (IOException e) {
            Logger.e(TAG, e);
            throw e;
        } finally {
            Utils.close(dis);
        }

        return array;
    }

    public static void writeIntArrayToFile(File file, int[] array, int len) throws IOException {
        Assert.notNull(file);
        Assert.notNull(array);

        FileOutputStream fos = null;
        DataOutputStream dos = null;
        try {
            fos = new FileOutputStream(file);
            dos = new DataOutputStream(fos);

            for (int i = 0; i < len; i++) {
                dos.writeInt(array[i]);
            }
        } catch (IOException e) {
            Logger.e(TAG, e);
            throw e;
        } finally {
            Utils.close(fos);
            Utils.close(dos);
        }
    }

    /**
     * read contents from specified input stream and write to specified target file.
     * @param in the input stream
     * @param file the target file
     * @throws IOException
     */
    public static void writeStreamToFile(InputStream in, SFile file) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(in);
            file.open(SFile.OpenMode.Write);

            byte[] buffer = new byte[1024 * 16];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1)
                file.write(buffer, 0, bytesRead);
        } finally {
            Utils.close(input);
            file.close();
        }
    }

    public static void writeFileToStream(SFile file, OutputStream output) throws IOException {
        try {
            file.open(SFile.OpenMode.Read);
            byte[] b = new byte[1024 * 4];
            int r;
            while ((r = file.read(b)) != -1) {
                output.write(b, 0, r);
            }
            output.flush();
        } finally {
            file.close();
        }
    }
}
