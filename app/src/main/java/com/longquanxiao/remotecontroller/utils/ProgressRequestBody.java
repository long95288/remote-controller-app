package com.longquanxiao.remotecontroller.utils;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * HTTP请求的进度回调接口
 * 用于监听上传文件的进度
 */


/**
 * 含有上传进度的监听
 */
public class ProgressRequestBody extends RequestBody {

    public final RequestBody requestBody;
    private final ProgressRequestListener progressRequestListener;
    private BufferedSink bufferedSink;

    public ProgressRequestBody(RequestBody requestBody, ProgressRequestListener progressRequestListener) {
        this.requestBody = requestBody;
        this.progressRequestListener = progressRequestListener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (null == bufferedSink) {
            Sink sk = sink(sink);
            bufferedSink = Okio.buffer(sk);
        }
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            long bytesWritten = 0L;
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (0 == contentLength) {
                    contentLength = contentLength();
                }
                bytesWritten += byteCount;
                progressRequestListener.onRequestProgress(bytesWritten, contentLength, bytesWritten == contentLength);
            }
        };
    }
}
