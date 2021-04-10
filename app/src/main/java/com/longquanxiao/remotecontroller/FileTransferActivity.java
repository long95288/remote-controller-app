package com.longquanxiao.remotecontroller;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.longquanxiao.remotecontroller.utils.FileUploadThreadCallBackInterface;
import com.longquanxiao.remotecontroller.utils.SendMsgCallbackInterface;
import com.vincent.filepicker.Constant;
import com.vincent.filepicker.activity.NormalFilePickActivity;
import com.vincent.filepicker.filter.entity.NormalFile;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;

public class FileTransferActivity extends AppCompatActivity {

    TextView fileTransferStatusView = null;
    EditText sendMsgInputText = null;
    List<String> statusViewContentList = null;
    // 保存50条日志
    private final int MAX_STATUS_VIEW_SIZE = 20;

    synchronized private void addStatusViewString(String s) {
        if (null != statusViewContentList ) {
            if (MAX_STATUS_VIEW_SIZE <= statusViewContentList.size()) {
                statusViewContentList.remove(0);
            }
            statusViewContentList.add(s);
        }
        updateStatusViewString();
    }

    private void updateStatusViewString() {
        StringBuffer buffer = new StringBuffer();
        for (String s : statusViewContentList) {
            buffer.append(s).append("\n");
        }
        if (null != fileTransferStatusView ) {
            fileTransferStatusView.post(()->{fileTransferStatusView.setText(buffer.toString());});
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);

        Button selectFileBtn =  findViewById(R.id.selectFileBtn);
        selectFileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, NormalFilePickActivity.class);
            intent.putExtra(Constant.MAX_NUMBER, 9);
            intent.putExtra(NormalFilePickActivity.SUFFIX, new String[]{"xlsx", "xls", "doc", "docx", "ppt", "pptx", "pdf", "rar", "zip"});
            startActivityForResult(intent, Constant.REQUEST_CODE_PICK_FILE);
        });
        fileTransferStatusView = findViewById(R.id.fileTransferStatusView);
        statusViewContentList = new ArrayList<>(MAX_STATUS_VIEW_SIZE);
        Button sendMsgBtn = findViewById(R.id.sendMsgBtn);
        sendMsgInputText = findViewById(R.id.sendMsgInputText);
        sendMsgBtn.setOnClickListener(view->{
            String msg = sendMsgInputText.getText().toString();
            // 点击发送的时候隐藏输入法
            InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive()) {
                imm.hideSoftInputFromWindow(sendMsgInputText.getWindowToken(), 0);
            }
            sendMsgInputText.post(()->{sendMsgInputText.setText("");});
            RCTLCore.getInstance().sendMsg(msg, (status,msg2) -> {
                if (status == 1){
                    addStatusViewString(String.format("Success Send: %s", msg));
                }
            });
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_CODE_PICK_FILE:
            {
                if (resultCode == RESULT_OK) {
                    // 获得文件成功
                    ArrayList<NormalFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE);
                    if (null != list && !list.isEmpty()) {
                        for (NormalFile file : list) {
                            String path = file.getPath();
                            System.out.println("file path " + path);
                            int taskId = new Random().nextInt(99999);
                            RCTLCore.getInstance().addFileUploadTask(taskId, path, new FileUploadThreadCallBackInterface() {
                                @SuppressLint("DefaultLocale")
                                @Override
                                public void reportStatus(int id, int status, String msg) {
                                    addStatusViewString(String.format("id:%d status: %d msg:%s", id, status, msg));
                                }

                                @SuppressLint("DefaultLocale")
                                @Override
                                public void reportProgress(int id, long uploadSize, long totalSize) {
                                    addStatusViewString(String.format("id:%d uploadSize:%d totalSize:%d percent:%f", id, uploadSize, totalSize, (double)(uploadSize)/(double)totalSize));
                                }
                            });
                            System.out.println("开启上传线程....");
                        }
                    }
                }
            }break;
        }
    }
}