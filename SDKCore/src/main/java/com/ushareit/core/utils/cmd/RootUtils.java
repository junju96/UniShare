package com.ushareit.core.utils.cmd;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.SystemClock;

import com.ushareit.core.Logger;
import com.ushareit.core.io.FileUtils;
import com.ushareit.core.lang.thread.TaskHelper;
import com.ushareit.core.utils.InstallHelper;
import com.ushareit.core.utils.Utils;
import com.ushareit.core.utils.i18n.LocaleUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public final class RootUtils {
    private static final String TAG = "RootUtils";

    enum RootMask {
        UNLOAD, LOADING, NO_PERMISSION, HAS_PERMISSION
    };

    enum RootType {
        SYSTEM, NAC, ROOT, SECURITY
    };

    public static final int PERMISSION_MASK_NONE = 0x0000;
    public static final int PERMISSION_MASK_SYSTEM = 0x0001;
    public static final int PERMISSION_MASK_NAC = 0x0002;
    public static final int PERMISSION_MASK_ROOT = 0x0004;
    public static final int PERMISSION_MASK_SECURITY = 0x0008;

    public static final int LEROOT_MASK_NONE = 0x0000;
    public static final int LEROOT_MASK_NAC = 0x0001;
    public static final int LEROOT_MASK_SUPERCMD = 0x0002;
    public static final int LEROOT_MASK_NACSAFE = 0x0004;  // for lenovo.safe

    private static RootMask mNacPermissionMask = RootMask.UNLOAD;
    private static RootMask mSecurityPermissionMask = RootMask.UNLOAD;
    private static RootMask mSuPermissionMask = RootMask.UNLOAD;

    private static NACProcess sNACProcess = new NACProcess();                   // NAC server socket
    private static SecurityProcess sSecurityProcess = new SecurityProcess();    // security center provider silent installer
    private static RootProcess sRootProcess = new RootProcess();
    private static SystemProcess sSystemProcess = new SystemProcess();

    public static class ConsoleOutput {
        public List<String> contents = new ArrayList<String>();
        public List<String> error = new ArrayList<String>();
        public boolean isSuccess = false;
    }

    private RootUtils() {}

    public static void checkRoot(final Context context) {
        TaskHelper.exec(new Runnable() {
            @Override
            public void run() {
                /*
                 * As loadMask need cost lots of times in the first time, so do it now.
                 * AnyShare first time get ROOT Permission, cost long time.
                 */
                RootUtils.loadMask(context);
                int mask = RootUtils.getMaskNoWait();
                Logger.d(TAG, TAG + "_init:" + mask);
                if (mask == -1)
                    return;

                if ((mask & RootUtils.PERMISSION_MASK_NAC) != 0) {
                    BusyboxUtils.getInstance().initBusybox(context);
                }
            }
        });
    }

    /**
     * Get anyshare permission
     * @param
     * @return 0 = no permission; -1 = loading; other = mask
     */
    public static int getMaskNoWait() {
        int permissionMask = 0;
        if (mNacPermissionMask == RootMask.HAS_PERMISSION)
            permissionMask |= PERMISSION_MASK_NAC;
        if (mSuPermissionMask == RootMask.HAS_PERMISSION)
            permissionMask |= PERMISSION_MASK_ROOT;
        if (mSecurityPermissionMask == RootMask.HAS_PERMISSION)
            permissionMask |= PERMISSION_MASK_SECURITY;

        if (permissionMask != 0)
            return permissionMask;

        permissionMask = (mNacPermissionMask == RootMask.NO_PERMISSION ||
                mSuPermissionMask == RootMask.NO_PERMISSION ||
                mSecurityPermissionMask == RootMask.NO_PERMISSION) ? 0 : -1;
        return permissionMask;
    }

    public static void loadMask(Context context) {
        if (mNacPermissionMask != RootMask.UNLOAD)
            return;

        mNacPermissionMask = RootMask.LOADING;
        mSecurityPermissionMask = RootMask.LOADING;
        mNacPermissionMask = sNACProcess.loadPermission(context) ? RootMask.HAS_PERMISSION : RootMask.NO_PERMISSION;
        mSecurityPermissionMask = sSecurityProcess.loadPermission(context) ? RootMask.HAS_PERMISSION : RootMask.NO_PERMISSION;

        Logger.d(TAG, TAG + ", nac: " + mNacPermissionMask + ", security:" + mSecurityPermissionMask);
    }

    public static boolean getSuMask(Context context) {
        if (mSuPermissionMask != RootMask.UNLOAD)
            return false;

        mSuPermissionMask = RootMask.LOADING;
        boolean mask = sRootProcess.loadPermission(context);
        mSuPermissionMask = mask ? RootMask.HAS_PERMISSION : RootMask.NO_PERMISSION;

        Logger.d(TAG, TAG + ", su: " + mSuPermissionMask);
        return mask;
    }

    public static int getLeMask(Context context) {
        if (sNACProcess.sLeMask >= LEROOT_MASK_NONE)
            return sNACProcess.sLeMask;

        sNACProcess.sLeMask = sNACProcess.loadLePermission(context);
        return sNACProcess.sLeMask;
    }

    public static boolean canExecCommand(Context context) {
        int mask = getMaskNoWait();
        if ((mask & PERMISSION_MASK_NAC) != 0)
            return true;
        if ((mask & PERMISSION_MASK_ROOT) != 0)
            return true;

        return false;
    }

    public static int quietInstallPackage(Context context, String path) {
        int mask = getMaskNoWait(), rootFlag, securityFlag, nacFlag;
        securityFlag = rootFlag = nacFlag = InstallHelper.INSTALL_PERMISSION_INVALID;

        if (mask <= PERMISSION_MASK_NONE) {
            Logger.d(TAG, TAG + " quietInstallPackage(): Has no quiet install permission.");
            return InstallHelper.INSTALL_PERMISSION_INVALID;
        }

        File file = new File(path);
        if (file == null || !file.exists())
            return InstallHelper.INSTALL_FAILED_PACKAGE_INVALID;

        if ((mask & PERMISSION_MASK_NAC) != 0) {
            nacFlag = sNACProcess.quietInstallPackage(context, RootType.NAC, path);
            if (nacFlag == InstallHelper.INSTALL_SUCCESS)
                return InstallHelper.INSTALL_SUCCESS;
        }
        if ((mask & PERMISSION_MASK_SECURITY) != 0) {
            securityFlag = sSecurityProcess.quietInstallPackage(context, RootType.SECURITY, path);
            if (securityFlag == InstallHelper.INSTALL_SUCCESS)
                return InstallHelper.INSTALL_SUCCESS;
        }
        if ((mask & PERMISSION_MASK_ROOT) != 0) {
            rootFlag = sRootProcess.quietInstallPackage(context, RootType.ROOT, path);
            if (rootFlag == InstallHelper.INSTALL_SUCCESS)
                return InstallHelper.INSTALL_SUCCESS;
        }
        int tmp = (securityFlag >= rootFlag) ? securityFlag : rootFlag;
        tmp = ((tmp >= nacFlag) ? tmp : nacFlag);

        return tmp;
    }

    public static boolean quietUnInstallPackage(Context context, String packageName) {
        int mask = getMaskNoWait();
        if (mask <= PERMISSION_MASK_NONE) {
            Logger.d(TAG, TAG + " quietUnInstallPackage(): Has no quiet uninstall permission.");
            return false;
        }

        if ((mask & PERMISSION_MASK_SYSTEM) != 0) {
            if (sSystemProcess.quietUnInstallPackage(context, packageName))
                return true;
        }
        if ((mask & PERMISSION_MASK_NAC) != 0) {
            if (sNACProcess.quietUnInstallPackage(context, packageName))
                return true;
        }
        if ((mask & PERMISSION_MASK_ROOT) != 0) {
            if (sRootProcess.quietUnInstallPackage(context, packageName))
                return true;
        }
        return false;
    }

    /**
     * @param command
     * @return ConsoleOutput
     */
    public static ConsoleOutput executeCommand(Context context, String command) {
        int mask = getMaskNoWait();
        if (mask <= PERMISSION_MASK_SYSTEM) {
            return new ConsoleOutput();
        }
        if ((mask & PERMISSION_MASK_NAC) != 0) {
            return sNACProcess.executeCommand(command);
        }
        if ((mask & PERMISSION_MASK_ROOT) != 0) {
            return sRootProcess.executeCommand(command);
        }
        return new ConsoleOutput();
    }

    /**
     * @param commands : shell command
     * @return Success counts
     */
    public static boolean executeCommands(Context context, List<String> commands) {
        int mask = getMaskNoWait();
        if (mask <= PERMISSION_MASK_SYSTEM) {
            return false;
        }
        if ((mask & PERMISSION_MASK_NAC) != 0) {
            return sNACProcess.executeCommands(commands);
        }
        if ((mask & PERMISSION_MASK_ROOT) != 0) {
            return sRootProcess.executeCommands(commands);
        }
        return false;
    }

    protected static boolean isSuccess(ConsoleOutput result) {
        if (result.error.size() == 0 || (result.error.size() > 0 && result.error.get(0).equals("")))
            return true;
        if (result.contents.size() > 0 && isSimilar("Success", result.contents.get(0)))
            return true;
        return false;
    }

    protected static boolean isSimilar(String one, String another) {
        if (one == null || another == null)
            return false;

        int length = one.length();
        if (length > another.length()) {
            return false;
        }
        if (one.equalsIgnoreCase(another.substring(0, length))) {
            return true;
        } else {
            return false;
        }
    }

    private abstract static class CommandProcess {

        public int quietInstallPackage(Context context, RootType type, String apkPath) {
            int result = InstallHelper.INSTALL_FAILED_PACKAGE_INVALID;
            PackageInfo pi = context.getPackageManager().getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (pi == null)
                return InstallHelper.INSTALL_FAILED_PACKAGE_INVALID;

            String packageName = pi.packageName;
            int versionCode = pi.versionCode;

            String command = (type == RootType.SECURITY) ? apkPath : getInstallCmd(apkPath, true);

            long start = System.currentTimeMillis();
            Logger.d(TAG, "[AS.Nucleus] quietInstallPackage(1) " + packageName + ", start " + start);
            ConsoleOutput output = quietInstallPackage(context, command);
            long elapsed = System.currentTimeMillis() - start;
            Logger.d(TAG, "[AS.Nucleus] quietInstallPackage(2) " + packageName + ", elapsed " + elapsed + " ms.");

            result = parseConsoleOutput(context, packageName, versionCode, output);
            return result;
        }

        public boolean quietUnInstallPackage(Context context, String packageName) {
            String command = getUnInstallCmd(packageName);

            long start = System.currentTimeMillis();
            Logger.d(TAG, "[AS.Nucleus] quietUnInstallPackage(1) " + packageName + ", start " + start);
            ConsoleOutput output = executeCommand(command);
            long elapsed = System.currentTimeMillis() - start;
            Logger.d(TAG, "[AS.Nucleus] quietUnInstallPackage(2) " + packageName + ", elapsed " + elapsed + " ms." + output.isSuccess);

            return isAppUnInstalled(context, packageName);
        }

        private boolean isAppUnInstalled(Context context, String packageName) {
            try {
                context.getPackageManager().getApplicationInfo(packageName, 0);
                return false;
            } catch (NameNotFoundException e) {
                return true;
            }
        }

        private int parseConsoleOutput(Context context, String packageName, int versionCode, ConsoleOutput result) {
            try {
                if (result.isSuccess) {
                    PackageInfo info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                    if (info != null && info.versionCode == versionCode) {
                        return InstallHelper.INSTALL_SUCCESS;
                    }
                    return InstallHelper.INSTALL_FAILED_PACKAGE_UPDATE_ERROR;
                }
            } catch (Exception e) {}

            if (contains(result.error, "INSTALL_FAILED_UID_CHANGED")) {
                return InstallHelper.INSTALL_FAILED_UID_CHANGED;
            }
            if (contains(result.error, "INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_NO_CERTIFICA") ||
                    contains(result.error, "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE")) {
                return InstallHelper.INSTALL_FAILED_PACKAGE_CERTIFICATE_ERROR;
            }
            if (contains(result.error, "INSTALL_FAILED_CONFLICTING_PROVIDER") ||
                    contains(result.error, "INSTALL_FAILED_DEXOPT") ||
                    contains(result.error, "INSTALL_FAILED_OLDER_SDK") ||
                    contains(result.error, "INSTALL_FAILED_REPLACE_COULDNT_DELETE") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_BAD_MANIFEST") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_MANIFEST_EMPTY") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_MANIFEST_MALFORMED")) {
                return InstallHelper.INSTALL_FAILED_PACKAGE_CONTENT_ERROR;
            }
            if (contains(result.error, "INSTALL_FAILED_MISSING_SHARED_LIBRARY")) {
                return InstallHelper.INSTALL_FAILED_MISSING_SHARED_LIBRARY;
            }
            if (contains(result.error, "INSTALL_FAILED_INVALID_URI") ||
                    contains(result.error, "INSTALL_FAILED_INVALID_APK") ||
                    contains(result.error, "INSTALL_PARSE_FAILED_NOT_APK")) {
                return InstallHelper.INSTALL_FAILED_PACKAGE_INVALID;
            }
            if (contains(result.error, "INSTALL_FAILED_UPDATE_INCOMPATIBLE") ||
                    contains(result.error, "INSTALL_FAILED_DUPLICATE_PACKAGE") ||
                    contains(result.error, "INSTALL_FAILED_VERSION_DOWNGRADE")) {
                return InstallHelper.INSTALL_FAILED_PACKAGE_UPDATE_ERROR;
            }
            if (contains(result.error, "INSTALL_FAILED_CONTAINER_ERROR")) {
                return InstallHelper.INSTALL_FAILED_CONTAINER_ERROR;
            }
            if (contains(result.error, "INSTALL_FAILED_INSUFFICIENT_STORAGE")) {
                return InstallHelper.INSTALL_FAILED_INSUFFICIENT_STORAGE;
            }
            if (containsIgnoreCase(result.error, "operation not permitted") ||
                    containsIgnoreCase(result.error, "permisson denied")) {
                return InstallHelper.INSTALL_PERMISSION_INVALID;
            }
            Logger.d(TAG, "[AS.Nucleus] Install " + packageName + ", errcode: " + result.error);

            return InstallHelper.INSTALL_FAILED_UNEXPECTED_EXCEPTION;
        }

        public ConsoleOutput executeCommand(String command) {
            return execCommand(command);
        }

        protected abstract boolean loadPermission(Context context);

        protected abstract ConsoleOutput quietInstallPackage(Context context, String command);

        protected abstract ConsoleOutput execCommand(String command);

        protected String getInstallCmd(String path, boolean replaceOrNot) {
            if (replaceOrNot) {
                return "pm install -r \"" + path + "\"";
            } else {
                return "pm install \"" + path + "\"";
            }
        }

        protected String getUnInstallCmd(String packageName) {
            return "pm uninstall \"" + packageName + "\"";
        }

        protected boolean contains(List<String> src, String compareStr) {
            if (src.size() == 0)
                return false;
            for (String str : src) {
                if (str.contains(compareStr))
                    return true;
            }
            return false;
        }

        protected boolean containsIgnoreCase(List<String> src, String compareStr) {
            if (src.size() == 0)
                return false;

            for (String str : src) {
                if (LocaleUtils.toLowerCaseIgnoreLocale(str).contains(compareStr))
                    return true;
            }
            return false;
        }

        protected void stringToFile(String str, String FileName) {
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(FileName);
                os.write(str.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Utils.close(os);
            }
        }

    }

    // Root permission process when the device is already root.
    private static class RootProcess extends CommandProcess {
        private String mSuPath;

        @Override
        public boolean loadPermission(Context context) {
            mSuPath = scanRootProcess();
            return !mSuPath.equals("");
        }

        @Override
        protected ConsoleOutput quietInstallPackage(Context context, String command) {
            return execCommand(command);
        }

        @Override
        protected ConsoleOutput execCommand(String command) {
            Logger.d(TAG, "[AS.Root] " + command);

            ConsoleOutput result = new ConsoleOutput(); // Save the pipe content
            BufferedReader in = null;
            BufferedReader er = null;
            try {
                // Shell: >su -c mount -o remount,rw <DevicePath>
                Process process = Runtime.getRuntime().exec(mSuPath);
                OutputStream os = process.getOutputStream();
                os.write((command + "\n").getBytes());
                os.flush();
                os.close();
                in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                er = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                // superuser maybe not return, need monitor timeout
                // startTimeoutDestroy(process);

                String line = null;
                while ((line = er.readLine()) != null) {
                    result.error.add(line);
                }
                while ((line = in.readLine()) != null) {
                    result.contents.add(line);
                }
                result.isSuccess = isSuccess(result);

                // process.destroy();
            } catch (Exception e) {
                Logger.w(TAG, TAG + " root " + e.toString());
                result.error.add(e.toString());
            } finally {
                Utils.close(in);
                Utils.close(er);
            }

            return result;
        }

        public boolean executeCommands(List<String> commands) {
            for (String cmd : commands) {
                execCommand(cmd);
            }
            return true;
        }

        @SuppressWarnings("unused")
        private void startTimeoutDestroy(final Process process) {
            // after 30 sec force destory
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(45000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    process.destroy();
                }
            }).start();
        }

        private String scanRootProcess() {
            mSuPath = "";
            for (String path : System.getenv("PATH").split(":")) {
                File f = new File(path, "su");
                if (f.exists()) {
                    if (suCanExecute(f)) {
                        mSuPath = f.getAbsolutePath();
                        return f.getAbsolutePath();
                    }
                }
            }
            return "";
        }

        private boolean suCanExecute(File f) {
            String cache = mSuPath;
            mSuPath = f.getAbsolutePath();
            ConsoleOutput co = executeCommand("pm install /system/.NOT_EXIST_APPLICATION");
            if (contains(co.error, "INSTALL_FAILED_INVALID_URI") ||
                    contains(co.error, "INSTALL_FAILED_INSUFFICIENT_STORAGE")) {
                return true;
            }
            mSuPath = cache;
            return false;
        }

    }

    /*
     * LeSafe API
     * Socket("127.0.0.1", 30001)
     * LocalSocketAddress("nac_server")
     */
    // LeSafe NAC server permission process when the device pre_install LeSafe.
    private static class NACProcess extends CommandProcess {
        private static final String LENOVO_NAC_SERVER = "nac_server";
        private static final String LENOVO_NAC_SAFE_SERVER = "nac_safe_server";
        private static final String LENOVO_SUPER_CMD = "supercmdlocalsocket";

        private String mCMDPath = "";
        private int sLeMask = -1;

        @Override
        public boolean loadPermission(Context context) {
            mCMDPath = context.getFilesDir().getAbsolutePath() + "/cmd/";
            File cmdPath = new File(mCMDPath);
            if (!cmdPath.exists())
                cmdPath.mkdirs();

            String destPath = mCMDPath + "tmpFile";
            String cmdFile = mCMDPath + "permission_" + SystemClock.elapsedRealtime();
            try {
                File cmd = new File(cmdFile);
                cmd.createNewFile();
                stringToFile("echo 'End' > " + destPath, cmdFile);
            } catch (IOException e) {
                Logger.d(TAG, "loadPermission createNewFile(): " + e.toString());
            }

            doRetryNacCommand(cmdFile, 3, 2000);

            File cmdfile = new File(cmdFile);
            if (cmdfile.exists())
                cmdfile.delete();

            File destFile = new File(destPath);
            if (destFile.exists()) {
                destFile.delete();
                return true;
            }
            return false;
        }

        @Override
        protected ConsoleOutput quietInstallPackage(Context context, String command) {
            return execCommand(command);
        }

        @Override
        protected ConsoleOutput execCommand(String cmd) {
            ConsoleOutput console = execCommand(cmd, LEROOT_MASK_NAC);
            // if (console.isSuccess)
            // return console;
            // if ((sLenovoRootMask & LEROOT_MASK_NACSAFE) != 0) {
            // ConsoleOutput superConsole = execCommand(cmd, LEROOT_MASK_NACSAFE);
            // if (superConsole.isSuccess)
            // return superConsole;
            // }
            // if ((sLenovoRootMask & LEROOT_MASK_SUPERCMD) != 0) {
            // ConsoleOutput superConsole = execCommand(cmd, LEROOT_MASK_SUPERCMD);
            // if (superConsole.isSuccess)
            // return superConsole;
            // }
            return console;
        }

        private boolean executeCommands(List<String> cmds) {
            boolean success = executeCommands(cmds, LEROOT_MASK_NAC);
            // if (!success && (sLenovoRootMask & LEROOT_MASK_NACSAFE) != 0) {
            // success = executeCommands(cmds, LEROOT_MASK_NACSAFE);
            // }
            // if (!success && (sLenovoRootMask & LEROOT_MASK_SUPERCMD) != 0) {
            // success = executeCommands(cmds, LEROOT_MASK_SUPERCMD);
            // }
            return success;
        }

        private boolean executeCommands(List<String> commands, int type) {
            boolean success = false;
            long time = SystemClock.elapsedRealtime();
            String cmdFile = mCMDPath + "cmd_" + time;

            StringBuilder sb = new StringBuilder();
            for (String cmd : commands) {
                sb.append(cmd);
            }
            stringToFile(sb.toString(), cmdFile);

            String socketBuf = "";
            if (type == LEROOT_MASK_SUPERCMD)
                socketBuf = writeLeCommand(cmdFile, LENOVO_SUPER_CMD);
            else if (type == LEROOT_MASK_NACSAFE)
                socketBuf = writeLeCommand(cmdFile, LENOVO_NAC_SAFE_SERVER);
            else
                socketBuf = doRetryNacCommand(cmdFile, 5, 3000);

            if (isSimilar("success", socketBuf))
                success = true;

            clearCmdFiles(cmdFile, null, null);
            return success;
        }

        private ConsoleOutput execCommand(String command, int type) {
            ConsoleOutput result = new ConsoleOutput();
            long time = SystemClock.elapsedRealtime();
            String cmdFile = mCMDPath + "cmd_" + time;
            String outFile = mCMDPath + "out_" + time;
            String errFile = mCMDPath + "err_" + time;

            try {
                File cmd = new File(cmdFile);
                cmd.createNewFile();
                File out = new File(outFile);
                out.createNewFile();
                File err = new File(errFile);
                err.createNewFile();

                String cmdInner = command + " 1> " + outFile + " 2> " + errFile + " \n";
                stringToFile(cmdInner, cmdFile);
            } catch (IOException e) {
                Logger.d(TAG, TAG + " createNewFile() failed: " + e.toString());
            }

            String socketBuf = "";
            if (type == LEROOT_MASK_SUPERCMD)
                socketBuf = writeLeCommand(cmdFile, LENOVO_SUPER_CMD);
            else if (type == LEROOT_MASK_NACSAFE)
                socketBuf = writeLeCommand(cmdFile, LENOVO_NAC_SAFE_SERVER);
            else
                socketBuf = doRetryNacCommand(cmdFile, 5, 3000);

            if (isSimilar("success", socketBuf)) {
                result.contents = readOutFile(outFile);
                result.error = readOutFile(errFile);
                result.isSuccess = isSuccess(result);
            } else {
                result.isSuccess = false;
                result.error.add(socketBuf);
            }
            clearCmdFiles(cmdFile, outFile, errFile);

            return result;
        }

        // because NAC socket may be can't connect, try 3 times
        private String doRetryNacCommand(String cmdFile, int tryCount, long time) {
            String socketBuf = "";
            for (int i = 0; i < tryCount; i++) {
                socketBuf = writeNacCommand(cmdFile);
                if (isSimilar("success", socketBuf)) {
                    break;
                } else {
                    Logger.d(TAG, TAG + " doRetryNacCommand failed:(" + i + "):" + socketBuf);
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        Logger.d(TAG, TAG + " doRetryNacCommand sleep() failed: " + e);
                    }
                }
            }
            return socketBuf;
        }

        private String writeNacCommand(String cmdFile) {
            String socketBuf = "";
            PrintWriter socketWriter = null;
            BufferedReader socketReader = null;

            try {
                LocalSocketAddress address = new LocalSocketAddress(LENOVO_NAC_SERVER);
                LocalSocket localSocket = new LocalSocket();
                localSocket.connect(address);
                socketWriter = new PrintWriter(localSocket.getOutputStream(), true);
                socketReader = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
                socketWriter.write(cmdFile);
                socketWriter.flush();

                String line = "";
                StringBuffer buf = new StringBuffer();
                while (null != (line = socketReader.readLine()))
                    buf.append(line).append("\n");
                socketBuf = buf.toString();
                localSocket.close();

                return socketBuf;
            } catch (IOException e) {
                socketBuf = e.toString();
                Logger.d(TAG, TAG + " nac_server Socket() failed: " + e);
            } finally {
                Utils.close(socketWriter);
                Utils.close(socketReader);
            }

            try {
                Socket client = new Socket("127.0.0.1", 30001);
                socketWriter = new PrintWriter(client.getOutputStream(), true);
                socketReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                socketWriter.write(cmdFile);
                socketWriter.flush();

                String line = "";
                StringBuffer buf = new StringBuffer();
                while (null != (line = socketReader.readLine())) {
                    buf.append(line).append("\n");
                }
                socketBuf = buf.toString();
                client.close();
            } catch (IOException e) {
                socketBuf = e.toString();
                Logger.d(TAG, TAG + " nac_ip Socket() failed: " + e);
            } finally {
                Utils.close(socketWriter);
                Utils.close(socketReader);
            }

            return socketBuf;
        }

        private int loadLePermission(Context context) {
            int mask = LEROOT_MASK_NONE;
            String cmdFile = mCMDPath + "LeMaskTemp_" + SystemClock.elapsedRealtime();
            try {
                File cmd = new File(cmdFile);
                cmd.createNewFile();
                stringToFile("echo 'lenovo.cloneit'", cmdFile);
            } catch (IOException e) {
                Logger.d(TAG, "loadLePermission createNewFile(): " + e.toString());
            }

            if (writeLeCommand(cmdFile, LENOVO_NAC_SERVER).startsWith("success"))
                mask |= LEROOT_MASK_NAC;
            if (writeLeCommand(cmdFile, LENOVO_SUPER_CMD).startsWith("success"))
                mask |= LEROOT_MASK_SUPERCMD;
            if (writeLeCommand(cmdFile, LENOVO_NAC_SAFE_SERVER).startsWith("success"))
                mask |= LEROOT_MASK_NACSAFE;

            File cmdfile = new File(cmdFile);
            if (cmdfile.exists())
                cmdfile.delete();

            return mask;
        }

        private String writeLeCommand(String cmdFile, String sockType) {
            String result = "";
            DataInputStream din = null;
            PrintWriter socketWriter = null;
            LocalSocket localSocket = null;
            LocalSocketAddress address = null;
            try {
                if (LENOVO_NAC_SERVER.equals(sockType))
                    address = new LocalSocketAddress(LENOVO_NAC_SERVER);
                else if (LENOVO_SUPER_CMD.equals(sockType))
                    address = new LocalSocketAddress(LENOVO_SUPER_CMD);
                else if (LENOVO_NAC_SAFE_SERVER.equals(sockType))
                    address = new LocalSocketAddress(LENOVO_NAC_SAFE_SERVER);
                else
                    address = new LocalSocketAddress(LENOVO_NAC_SERVER);

                localSocket = new LocalSocket();
                localSocket.connect(address);
                localSocket.getOutputStream();
                socketWriter = new PrintWriter(localSocket.getOutputStream(), true);
                din = new DataInputStream((localSocket.getInputStream()));
                socketWriter.write(cmdFile);
                socketWriter.flush();

                byte[] buffer = new byte[8];
                int read_count = din.read(buffer);
                if (read_count > 0) {
                    result = new String(buffer);
                    Logger.i(TAG, sockType + " result: " + result);
                }
            } catch (Exception e) {
                Logger.e(TAG, sockType + " error: " + e.toString());
            } finally {
                try {
                    if (din != null) {
                        din.close();
                        din = null;
                    }
                    if (socketWriter != null) {
                        socketWriter.close();
                        socketWriter = null;
                    }
                    if (localSocket != null) {
                        localSocket.close();
                        localSocket = null;
                    }
                } catch (Exception e) {}
            }
            return result;
        }

        private void clearCmdFiles(String cmd, String out, String err) {
            if (cmd != null) {
                File cmdFile = new File(cmd);
                if (cmdFile.exists())
                    cmdFile.delete();
            }
            if (out != null) {
                File outFile = new File(out);
                if (outFile.exists())
                    outFile.delete();
            }
            if (err != null) {
                File errFile = new File(err);
                if (errFile.exists())
                    errFile.delete();
            }
        }

        private List<String> readOutFile(String fileName) {
            List<String> contents = new ArrayList<String>();
            LineNumberReader lnr = null;
            try {
                lnr = new LineNumberReader(new FileReader(fileName));
                String line;
                while ((line = lnr.readLine()) != null) {
                    contents.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Utils.close(lnr);
            }
            return contents;
        }
    }

    private static class SecurityProcess extends CommandProcess {
        private static final String AUTHORITY = "com.lenovo.security.packageinstall.SilentInstallProvider";
        private static final int RESULT_FAILED = -1;
        private static final int RESULT_SUCCESS = 1;

        @Override
        protected boolean loadPermission(Context context) {
            Uri uri = getUri();
            int result = RESULT_FAILED;
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    result = cursor.getInt(0);
                }
            } catch (Exception e) {
                Logger.d(TAG, TAG + ", Security loadPermission: " + e.toString());
            } finally {
                Utils.close(cursor);
            }

            Logger.d(TAG, TAG + ", Security loadPermission: " + result);
            boolean success = (result == RESULT_SUCCESS) ? true : false;
            return success;
        }

        @Override
        public ConsoleOutput quietInstallPackage(Context context, String path) {
            Uri uri = getUri();
            ContentValues value = new ContentValues();
            value.put("PATH", path);

            int result = RESULT_FAILED;
            try {
                result = context.getContentResolver().update(uri, value, null, null);
            } catch (Exception e) {
                Logger.d(TAG, TAG + ", Security: " + e.toString());
            }

            Logger.d(TAG, TAG + ", Security: " + result + ", " + FileUtils.getFileName(path));
            ConsoleOutput console = new ConsoleOutput();
            console.isSuccess = (result == RESULT_SUCCESS) ? true : false;

            return console;
        }

        private Uri getUri() {
            Uri uri = Uri.parse("content://" + AUTHORITY + "/install");
            return uri;
        }

        @Override
        protected ConsoleOutput execCommand(String command) {
            return new ConsoleOutput();
        }
    }

    // System permission process when the application has system flag "FLAG_SYSTEM".
    protected static class SystemProcess extends CommandProcess {

        @Override
        public boolean loadPermission(Context context) {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return (appInfo != null && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        }

        @Override
        protected ConsoleOutput quietInstallPackage(Context context, String command) {
            return execCommand(command);
        }

        @Override
        public ConsoleOutput execCommand(String command) {
            ConsoleOutput result = new ConsoleOutput();
            String[] args = command.split(" ");
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].replaceAll("\"", "");
            }
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            Process process = null;
            BufferedReader in = null;
            BufferedReader er = null;
            try {
                process = processBuilder.start();
                process.waitFor();
                in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                er = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line = null;
                while ((line = er.readLine()) != null) {
                    result.error.add(line);
                }
                while ((line = in.readLine()) != null) {
                    result.contents.add(line);
                }
                process.destroy();
            } catch (InterruptedException ie) {
                Logger.e(TAG, TAG + " system " + ie.getMessage());
            } catch (IOException ioe) {
                Logger.e(TAG, TAG + " system " + ioe.getMessage());
            } catch (RuntimeException e) {
                Logger.e(TAG, TAG + " system " + e.getMessage());
            } finally {
                Utils.close(in);
                Utils.close(er);
            }

            result.isSuccess = isSuccess(result);

            Logger.d(TAG, TAG + " system error:" + result.error);
            return result;
        }
    }

    public static ConsoleOutput execConsoleCommand(String command) {
        ConsoleOutput result = new ConsoleOutput();
        String[] args = command.split(" ");
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].replaceAll("\"", "");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        BufferedReader in = null;
        BufferedReader er = null;
        try {
            process = processBuilder.start();
            process.waitFor();
            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            er = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            while ((line = er.readLine()) != null) {
                result.error.add(line);
            }
            while ((line = in.readLine()) != null) {
                result.contents.add(line);
            }
            process.destroy();
        } catch (InterruptedException ie) {
            result.error.add(ie.getMessage());
            Logger.e(TAG, ie.getMessage());
        } catch (IOException ioe) {
            result.error.add(ioe.getMessage());
            Logger.e(TAG, ioe.getMessage());
        } catch (RuntimeException e) {
            result.error.add(e.getMessage());
            Logger.e(TAG, e.getMessage());
        } finally {
            Utils.close(in);
            Utils.close(er);
        }
        result.isSuccess = isSuccess(result);

        return result;
    }

}
