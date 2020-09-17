package com.ushareit.core.utils.device;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.ushareit.core.Logger;

public class BatteryUtils {
    public static String TAG = "BatteryUtils";
    public static class BatteryInfo {
        private int batteryPercent;
        private boolean isUsbCharge;
        private boolean isAcCharge;

        public int getBatteryPercent() {
            return batteryPercent;
        }

        public void setBatteryPercent(int batteryPercent) {
            this.batteryPercent = batteryPercent;
        }

        public boolean isUsbCharge() {
            return isUsbCharge;
        }

        public void setUsbCharge(boolean usbCharge) {
            isUsbCharge = usbCharge;
        }

        public boolean isAcCharge() {
            return isAcCharge;
        }

        public void setAcCharge(boolean acCharge) {
            isAcCharge = acCharge;
        }

        @Override
        public String toString() {
            return "BatteryInfo{" +
                    "batteryPercent=" + batteryPercent +
                    ", isUsbCharge=" + isUsbCharge +
                    ", isAcCharge=" + isAcCharge +
                    '}';
        }
    }

    /**
     * 获取电池相关信息
     * @param context
     * @return 返回电池信息对象
     */
    public static BatteryInfo getSystemBattery(Context context){
        BatteryInfo batteryInfo =new BatteryInfo();
        int level = 0;
        Intent batteryInfoIntent = context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        level = batteryInfoIntent.getIntExtra("level",0);
        int batterySum = batteryInfoIntent.getIntExtra("scale", 100);
        int percentBattery= 100 *  level / batterySum;
        int chargePlug = batteryInfoIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
        batteryInfo.setBatteryPercent(percentBattery);
        batteryInfo.setAcCharge(acCharge);
        batteryInfo.setUsbCharge(usbCharge);
        Logger.d(TAG, batteryInfo.toString());
        return batteryInfo;
    }
}
