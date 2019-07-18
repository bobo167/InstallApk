package com.yl.pay.otcgate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String myVersionCode;
    AlertDialog.Builder builder;

    String URL = "https://api.otcofmall.com/api/offsite/version/update";
    String appDownloadUrl = "";
    public ProgressBar downloadProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadProgressBar = (ProgressBar) findViewById(R.id.bar);
        downloadProgressBar.setVisibility(View.VISIBLE);
        final TextView text = findViewById(R.id.tt1);
        Button button = findViewById(R.id.btn_checkUp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                text.setText(myVersionCode);
                //dialog
                if (true) {
                    showTwo();
                }
            }
        });
        myVersionCode = getVersionCode(this);
        initData();
    }

    public void initData() {
        OkhttpManager.getAsync(URL, new OkhttpManager.DataCallBack() {
            @Override
            public void requestFailure(String request, IOException e) {
                Toast.makeText(MainActivity.this, "requestFailure" + request, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void requestSuccess(BaseBean baseBean) {
                if (baseBean.code.equals("0")) {
                    Log.e("success", baseBean.getData().toString());
                    JSONObject jsonObject = JSONObject.parseObject(baseBean.getData().toString());
                    appDownloadUrl = jsonObject.getString("url");
//                    appDownloadUrl = "";
                    Log.e("appDownloadUrl", appDownloadUrl);
                }
            }
        });
        InstallApkUtil.installProcess(MainActivity.this);
    }


    /**
     * 两个按钮的 dialog
     */
    private void showTwo() {
        builder = new AlertDialog.Builder(this).setTitle("最普通dialog") //.setIcon(R.mipmap.ic_launcher)
//                .setView(EditText )
                .setMessage("有新版本可以更新！").setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!isHasPermission()) {
                            requestPermission();
                            return;
                        }
                        if (!InstallApkUtil.installProcess(MainActivity.this)) {
                            return;
                        }
                        //ToDo: download and install
                        DownloadUtil.get().download(appDownloadUrl,
                                Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/",
                                "tt.apk", new DownloadUtil.OnDownloadListener() {
                                    @Override
                                    public void onDownloadSuccess(File file) {
//            handler 去更新...操作
                                        Log.e("onDownloadSuccess", file.getPath());

                                        new InstallApkUtil(MainActivity.this, file);
                                    }

                                    @Override
                                    public void onDownloading(int progress) {
                                        downloadProgressBar.setProgress(progress);
                                    }

                                    @Override
                                    public void onDownloadFailed(Exception e) {
                                        Log.e("onDownloadSuccess", e.toString());

                                    }
                                });
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //ToDo: 你想做的事情
                        Toast.makeText(MainActivity.this, "关闭按钮", Toast.LENGTH_LONG).show();
                        dialogInterface.dismiss();
                    }
                });
        builder.create().show();
    }


    public boolean isHasPermission() {
        if (XXPermissions.isHasPermission(MainActivity.this, Permission.Group.STORAGE)) {
//            showToast(LoginActivity.this, "已经获取到权限，不需要再次申请了");
            return true;
        } else {
//            showToast(LoginActivity.this, "还没有获取到权限或者部分权限未授予");
            return false;
        }
    }

    public void requestPermission() {
        XXPermissions.with(this)
                //.constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                //.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
                .permission(Permission.Group.STORAGE) //不指定权限则自动获取清单中的危险权限
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {
//                        if (isAll) {
//                            showToast(TaskDetails.this, "获取权限成功");
//                        } else {
//                            showToast(TaskDetails.this, "获取权限成功，部分权限未正常授予");
//                        }
                        showTwo();
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        if (quick) {
//                            showToast(TaskDetails.this, "需授权后才能使用，请手动授予权限");
                            //如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.gotoPermissionSettings(MainActivity.this);
                        } else {
//                            showToast(TaskDetails.this, "获取权限失败");
                        }
                    }
                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    public void onActivityResult(int rcode, int resultCode, Intent data) {
        super.onActivityResult(rcode, resultCode, data);
        if (InstallApkUtil.installProcess(MainActivity.this)) {
            //todo  直接做下载操作
//            InstallApkUtil.installApk();
//            new InstallApkUtil(MainActivity.this, file);
        }
    }

    public static String getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        String versionCode = "0";
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = String.valueOf(packageInfo.versionCode) + " vName:" + packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
}
