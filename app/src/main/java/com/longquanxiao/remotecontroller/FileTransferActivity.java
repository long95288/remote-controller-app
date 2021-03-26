package com.longquanxiao.remotecontroller;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.longquanxiao.remotecontroller.core.RCTLCore;
import com.vincent.filepicker.Constant;
import com.vincent.filepicker.activity.NormalFilePickActivity;
import com.vincent.filepicker.filter.entity.NormalFile;

import java.util.ArrayList;

public class FileTransferActivity extends AppCompatActivity {

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
                        for (NormalFile file :
                                list) {
                            String path = file.getPath();
                            System.out.println("file path " + path);
                            RCTLCore.getInstance().addFileUploadTask(1, path);
                            System.out.println("开启上传线程....");
                        }
                    }
                }
            }break;
        }
    }
}