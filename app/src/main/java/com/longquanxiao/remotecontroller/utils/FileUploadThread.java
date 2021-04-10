package com.longquanxiao.remotecontroller.utils;

import android.util.Log;

import com.longquanxiao.remotecontroller.core.RCTLCore;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

/**
 * 文件上传线程,每个上传线程有一个ID,完成/失败/进度都会回调回去
 */
public class FileUploadThread extends Thread {

    public static final int UPLOAD_FINISH = 1;
    public static final int UPLOAD_FAILED = 2;
    public static final int UPLOAD_DOING = 3;

    String uploadFilename;
    int uploadId;
    int uploadStatus;
    FileUploadThreadCallBackInterface callBack = null;

    public FileUploadThread(String uploadFilePath, int uploadId, FileUploadThreadCallBackInterface callBack) {
        this.uploadFilename = uploadFilePath;
        this.uploadId = uploadId;
        this.callBack = callBack;
    }

    @Override
    public void run() {
        super.run();
        // 创建Http连接,发送文件
        try {
            if (RCTLCore.RCTLCORE_STATUS_RUNNING == RCTLCore.getInstance().getRCTLCoreStatus()){
                OkHttpClient client = new OkHttpClient();
                String url = RCTLCore.getInstance().getFileUploadURL();
                if (null == url) {
                    if (callBack != null) {
                        callBack.reportStatus(uploadId, UPLOAD_FAILED, "Cannot get UploadURL");
                    }
                    return;
                }
                File file = new File(uploadFilename);
                RequestBody originalBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
                RequestBody fileBody = new ProgressRequestBody(originalBody, (byteWritten, contentLength, done) -> {
                    // 进度回调
                    Log.d(TAG, "upload size "+ byteWritten + "total size = " + contentLength);
                    if (null != callBack) { callBack.reportProgress(uploadId, byteWritten, contentLength);}
                });
                MultipartBody body = new MultipartBody.Builder() .setType(MediaType.parse("multipart/form-data")) .addFormDataPart("file", file.getName(), fileBody) .build();
                Request request= new Request.Builder() .url(url) .post(body) .build();
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "onFailure: 上传失败");
                        if (null != call) { callBack.reportStatus(uploadId, UPLOAD_FAILED, e.getMessage());}
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "onResponse: 上传成功");
                        if (null != callBack) { callBack.reportStatus(uploadId, UPLOAD_FINISH, "上传完成");}
                    }
                });
            }else{
                if (null != callBack) { callBack.reportStatus(uploadId, UPLOAD_FAILED, "RCTCore is not running");}
            }
        }catch (Exception e) {
            if (null != callBack) {callBack.reportStatus(uploadId, UPLOAD_FAILED, e.getMessage());}
        }
    }
}