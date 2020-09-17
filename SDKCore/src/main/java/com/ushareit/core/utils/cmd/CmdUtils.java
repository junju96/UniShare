package com.ushareit.core.utils.cmd;

import android.text.TextUtils;

import com.ushareit.core.utils.Utils;
import com.ushareit.core.Logger;
import com.ushareit.core.utils.i18n.LocaleUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CmdUtils {
    private final static String TAG = "CmdUtils";
    public static class CmdOutput {
        List<String> errors = new ArrayList<String>();
        List<String> contents = new ArrayList<String>();
        boolean succeed = false;

        public boolean isSucceed() {
            return succeed && errors.isEmpty();
        }

        public String getContents() {
            return contents.toString();
        }
        @Override
        public String toString() {
            return "CmdOutput{" +
                    "errors=" + errors +
                    ", contents=" + contents +
                    ", succeed=" + succeed +
                    '}';
        }
    }

    public static CmdOutput execCommand(String command) {
        return execCommand(command, 2 * 1000);
    }

    public static CmdOutput execCommand(String command, long timeout) {
        Logger.v(TAG, "execute command:" + command);

        CmdOutput output = new CmdOutput();
        String[] args = command.split(" ");
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].replaceAll("\"", "");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        try {
            process = processBuilder.start();
            CmdWorker worker = new CmdWorker(process, output);
            worker.start();
            try {
                worker.join(timeout);
                if (worker.mRunning)
                    worker.interrupt();
            } catch (Exception e) {}
        }catch (Exception e) {
            Logger.e(TAG, TAG + " system " + e.getMessage());
        } finally {
            if (process != null)
                process.destroy();
        }
        Logger.v(TAG, "execute:" + command + ", " + output.toString());
        return output;
    }

    private static class CmdWorker extends Thread {
        private final Process mProcess;
        private final CmdOutput mOutput;
        private boolean mRunning = true;

        private CmdWorker(Process process, CmdOutput output) {
            mProcess = process;
            mOutput = output;
        }

        @Override
        public void run() {
            BufferedReader in = null;
            BufferedReader er = null;
            try {
                Logger.d(TAG, "CmdWorker Run!");
                mProcess.waitFor();
                in = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                er = new BufferedReader(new InputStreamReader(mProcess.getErrorStream()));
                String line = null;
                while ((line = er.readLine()) != null) {
                    mOutput.errors.add(line);
                }
                while ((line = in.readLine()) != null) {
                    mOutput.contents.add(line);
                }
                mOutput.succeed = mOutput.errors.isEmpty();
            } catch (InterruptedException ie) {
                Logger.e(TAG, TAG + " interrupted exception " + ie.getMessage());
                mOutput.errors.add("exception: shareit InterruptedException , " + ie.getMessage());
            } catch (IOException ioe) {
                Logger.e(TAG, TAG + " io exception " + ioe.getMessage());
                mOutput.errors.add("exception: shareit IOException， " + ioe.getMessage());
            } catch (RuntimeException e) {
                Logger.e(TAG, TAG + " runtime exception " + e.getMessage());
                mOutput.errors.add("exception: shareit RuntimeException， " + e.getMessage());
            } finally {
                Utils.close(in);
                Utils.close(er);
                mRunning = false;
                Logger.d(TAG, "CmdWorker Run Completed!");
            }
        }
    }

    public static class PingResult {
        public int avgTime = 0;
        public int recvPackagePercent = -1;
        public boolean succeed = false;
        public String cmdOut;
        public String errMsg;

        @Override
        public String toString() {
            return "PingResult{" +
                    "avgTime=" + avgTime +
                    ", recvPackagePercent=" + recvPackagePercent +
                    ", succeed=" + succeed +
                    '}';
        }
    }

    public static PingResult execPing(int count, String address) {
        return execPing(count, address, 2000);
    }

    public static PingResult execPing(int count, String address, int timeOut) {
        String cmd = LocaleUtils.formatStringIgnoreLocale("ping -c %d -i 0.2 %s", count, address);
        CmdOutput output = execCommand(cmd, timeOut);
        PingResult result = new PingResult();
        result.cmdOut = output.getContents();
        if (!output.isSucceed()) {
            for (String error : output.errors)
                result.errMsg += (error + ",");
            return result;
        }
        List<String> items = output.contents;
        int succeedCnt = 0;
        float totalTime = .0f;
        for (String item : items) {
            Logger.v(TAG, "PING RESULT ITEM:" + item);
            String lowItem = item.toLowerCase();
            int position = lowItem.indexOf("time=");
            if (position < 0) {
                continue;
            }
            String timeString = lowItem.substring(position + "time=".length());
            String[] parts = timeString.split(" ");
            if (parts.length == 0 || TextUtils.isEmpty(parts[0])) {
                Logger.d(TAG, "parse ping item failed:" + item);
                continue;
            }
            try {
                float time = Float.parseFloat(parts[0]);
                totalTime += time;
                succeedCnt ++;
            } catch (Exception e) {
                Logger.d(TAG, "parse ping item failed,parse time err:" + item);
                continue;
            }

        }
        result.recvPackagePercent = (int)((1.0f * succeedCnt / count) * 100);
        result.avgTime = succeedCnt > 0 ?  (int)(totalTime / succeedCnt): 10000;

        boolean unexpectedValue = result.recvPackagePercent > 100;
        if (succeedCnt == 0 || unexpectedValue) {
            if (unexpectedValue) {
                Logger.d(TAG, "expected recv package percent:" + result.recvPackagePercent);
                result.errMsg += "Recv package percent unexpected:";

                result.recvPackagePercent = 100;
            }
            for (String item : items)
                result.errMsg += (item + ",");
        }

        result.succeed = TextUtils.isEmpty(result.errMsg) || (!result.errMsg.contains("100% packet loss") && !result.errMsg.contains("0 received"));
        Logger.v(TAG, "ping result:" + result);
        return result;

    }

}
