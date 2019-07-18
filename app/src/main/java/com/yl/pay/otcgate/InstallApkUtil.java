package com.yl.pay.otcgate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;

/**
 * Created by lu on 2019/7/18.
 */

public class InstallApkUtil {
    public Activity mActivity;
    private File apk;

    public InstallApkUtil(Activity mActivity, File apk) {
        this.mActivity = mActivity;
        this.apk = apk;
        start();
    }

    //异步下载app file
    public void start() {
        if (installProcess(mActivity)) {
            //有权限，开始安装应用程序
            installApk(apk);
        }else {
            Log.e("permission","noPermission");
        }
    }

    //安装应用的流程
    public static boolean installProcess(Activity mActivity) {
        boolean haveInstallPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //先获取是否有安装未知来源应用的权限
            haveInstallPermission = mActivity.getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {//没有权限
                startInstallPermissionSettingActivity(mActivity);
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static void startInstallPermissionSettingActivity(Activity mActivity) {
        Uri packageURI = Uri.parse("package:" + mActivity.getPackageName());
        //注意这个是8.0新API
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        mActivity.startActivityForResult(intent, 110);
    }

    //安装应用
    public void installApk(File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        } else {//Android7.0之后获取uri要用contentProvider
            Uri uri = getUriFromFile(apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);//这个权限加完后可以
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.getBaseContext().startActivity(intent);
        mActivity.finish();
    }

    public Uri getUriFromFile(File file) {
        Uri uri = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            uri = FileProvider.getUriForFile(mActivity,
                    mActivity.getPackageName() + ".fileProvider",
                    file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }


}
