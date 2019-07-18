package com.yl.pay.otcgate;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by lu on 2019/1/3.
 */

public class OkhttpManager {

    private OkHttpClient client;
    private static OkhttpManager okHttpManager;
    private Handler mHandler;
//    Gson gson;
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    /**
     * 单例模式 OKhttpManager2实例
     */
    private static OkhttpManager getInstance() {
        if (okHttpManager == null) {
            okHttpManager = new OkhttpManager();
        }
        return okHttpManager;
    }

    private OkhttpManager() {
        client = new OkHttpClient();
        client.newBuilder().proxy(Proxy.NO_PROXY);
        mHandler = new Handler(Looper.getMainLooper());
//        gson = new Gson();
    }


    //******************  内部逻辑处理方法  ******************/
    private Response p_getSync(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        return response;
    }

    private String p_getSyncAsString(String url) throws IOException {
        return p_getSync(url).body().string();
    }

    private void p_getAsync(String url, final DataCallBack callBack) {
        final Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverDataFailure(request, e, callBack);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String result = response.body().string();
                    deliverDataSuccess(result, callBack);
                } catch (IOException e) {
                    e.printStackTrace();
                    deliverDataFailure(request, e, callBack);
                }
            }
        });
    }

    private void p_postAsync(String url, Map<String, Object> params,
                             final DataCallBack callBack) {
        RequestBody requestBody = null;
        if (params == null) {
            params = new HashMap<String, Object>();
        }
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey().toString();
            String value = null;
            if (entry.getValue() == null) {
                value = "";
            } else {
                value = entry.getValue().toString();
            }
            builder.add(key, value);
        }
        requestBody = builder.build();
        // TODO: 2019/1/3 测试下！！ 加入header
        Request.Builder builders = new Request.Builder()
                .url(url).post(requestBody);
//        if (!MainActivity.header.isEmpty() && MainActivity.header != null) {
//            Set<String> keySet = MainActivity.header.keySet();
//            for (String key : keySet) {
//                builders.addHeader(key, (String) MainActivity.header.get(key));
//            }
//        }
        final Request request = builders.build();
        client.proxy();//没有代理

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverDataFailure(request, e, callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String result = response.body().string();
                    deliverDataSuccess(result, callBack);
                } catch (IOException e) {
                    e.printStackTrace();
                    deliverDataFailure(request, e, callBack);
                }
            }
        });
    }


    //******************  数据分发的方法  ******************/
    private void deliverDataFailure(final Request request, final IOException e, final DataCallBack callBack) {
        mHandler.post(new Runnable() {//发送到主线程
            @Override
            public void run() {
                if (e != null) {
                    callBack.requestFailure("网络链接失败，请检查网络状态", e);
                    return;
                }
                if (callBack != null) {
                    if (TextUtils.isEmpty(request.body().toString())) {
//                       BaseBean errorBean= gson.fromJson(request.body(), BaseBean.class);
//                        if (ResponseCode.TOKEN_NOT_EXIST.getCode().equals()) {
//                        }
                    }
                }
                callBack.requestFailure(request.toString(), e);
            }
        });
    }

    private void deliverDataSuccess(final String result, final DataCallBack callBack) {
        mHandler.post(new Runnable() {//同样 发送到主线程
            @Override
            public void run() {
                if (callBack != null) {
//                    Log.e("netWorkBack", result);
                    BaseBean<?> bean = JSON.parseObject(result, BaseBean.class);
//                    BaseBean bean = gson.fromJson(result, BaseBean.class);
                    //失败的情况？
//                    if (bean.getStatus().equalsIgnoreCase("error")) {
//                        callBack.requestFailure(bean.getMessage() , null);//+ ",错误码：" + bean.getCode()
//                        if (ResponseCode.TOKEN_NOT_EXIST.getCode().equals(bean.getCode())
//                                || ResponseCode.TOKEN_ERROR.getCode().equals(bean.getCode())) {
//                            // TODO: 2019/1/21 如何直接跳转到登录页面？
//                            PreferencesService.setPreferences("token", "");//重新登录
//                        }
//                        return;
//                    }
                    callBack.requestSuccess(bean);
                }
            }
        });
    }


    //******************  对外公布的方法  ******************/
    public static Response getSync(String url) throws IOException {
        return getInstance().p_getSync(url);//同步GET，返回Response类型数据
    }

    public static String getSyncAsString(String url) throws IOException {
        return getInstance().p_getSyncAsString(url);//同步GET，返回String类型数据（和上面getSync方法只是返回的数据类型不同而已）
    }

    public static void getAsync(String url, DataCallBack callBack) {
        getInstance().p_getAsync(url, callBack);//异步GET 调用方法
    }

    public static void postAsync(String url, Map<String, Object> params, DataCallBack callBack) {
        getInstance().p_postAsync(url, params, callBack);//POST提交表单 调用方法
    }


    //******************  数据回调接口  ******************/
    public interface DataCallBack {
        void requestFailure(String request, IOException e);

        void requestSuccess(BaseBean baseBean);
    }


    public static void postImageAsync(String reqUrl, Map<String, File> files, final DataCallBack callBack) {
        getInstance().postFile(reqUrl, null, files, callBack);
    }

    private void postFile(String url, Map<String, String> params, Map<String, File> files, final DataCallBack callBack) {
        OkHttpClient okHttpClient = new OkHttpClient();
      okHttpClient.newBuilder().proxy(Proxy.NO_PROXY);
        Request.Builder builder = new Request.Builder();
        MediaType MEDIA_TYPE_IMG = MediaType.parse("image/*");
//        if (!MainActivity.header.isEmpty() && MainActivity.header != null) {
//            Set<String> keySet = MainActivity.header.keySet();
//            for (String key : keySet) {
//                builder.addHeader(key, (String) MainActivity.header.get(key));
//            }
//        }
        MultipartBody.Builder multiPartBodyBuilder = new MultipartBody.Builder();
        //需要添加的参数
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                multiPartBodyBuilder.addFormDataPart(key, value);
            }
        }
        //需要上传的文件
        for (Map.Entry<String, File> entry : files.entrySet()) {
            String key = entry.getKey();
            File value = entry.getValue();
            multiPartBodyBuilder.addFormDataPart("file", key, RequestBody.create
                    (MEDIA_TYPE_IMG, value));
        }
        Request request = builder.url(url).post(multiPartBodyBuilder.build()).build();
//        Call call = okHttpClient.newCall(request);
        try {
            client.newBuilder().proxy(Proxy.NO_PROXY);
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String result = response.body().string();
                deliverDataSuccess(result, callBack);
            } else {
                deliverDataFailure(request, new IOException(), callBack);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                deliverDataFailure(request, e, callBack);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                try {
//                    String result = response.body().string();
//                    deliverDataSuccess(result, callBack);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    deliverDataFailure(request, e, callBack);
//                }
//            }
//        });
    }

}
