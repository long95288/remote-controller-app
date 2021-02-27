package com.longquanxiao.remotecontroller.cmd;

import android.util.JsonWriter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.longquanxiao.remotecontroller.core.RCTLCore;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 远程控制指令
 *
 */

class BaseRequestDTO {

    @JSONField(name = "CMD")
    public int cmd;

    public BaseRequestDTO(int cmd) {
        this.cmd = cmd;
    }

    public int getCmd() {
        return cmd;
    }
    public void setCmd(int cmd) {
        this.cmd = cmd;
    }
}
class BaseResponseDTO {

    @JSONField(name = "OptionStatus")
    public int optionStatus;

    @JSONField(name = "Message")
    public String message;

    @JSONField(name = "Data")
    public Object data;

    public BaseResponseDTO() {
    }
    public BaseResponseDTO(int optionStatus, String message, Object data) {
        this.optionStatus = optionStatus;
        this.message = message;
        this.data = data;
    }
    public int getOptionStatus() {
        return optionStatus;
    }
    public void setOptionStatus(int optionStatus) {
        this.optionStatus = optionStatus;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
}

class SetMasterVolumeRequestDTO extends BaseRequestDTO {

    @JSONField(name = "Volume")
    private int volume;

    public SetMasterVolumeRequestDTO(int cmd, int volume) {
        super(cmd);
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}


class SetMasterVolumeResponseDTO {

    @JSONField(name = "Volume")
    private int volume;

    public SetMasterVolumeResponseDTO(int volume) {
        this.volume = volume;
    }

    public SetMasterVolumeResponseDTO() {
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
public class RemoteControlCMD {
    public static final int  SETSHUTDOWNPLAN_CMD = 1;
    public static final int CANCELSHUTDOWNPLAN_CMD = 2;
    public static final int GETMASTERVOLUME_CMD = 3;
    public static final int SETMASTERVOLUME_CMD = 4;


    public static Integer getMasterVolume() throws Exception {
        return null;
    }
    public static Integer setMasterVolume(int voloume) throws Exception {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = "http://"+ RCTLCore.getInstance().getServerIP() +":"+ RCTLCore.getInstance().getServerPort() + "/cmd";
                System.out.println("请求IP : " + url);
                String requestBody = JSON.toJSONString(new SetMasterVolumeRequestDTO(SETMASTERVOLUME_CMD, voloume));

                System.out.println("SetMasterVolume request body: " + requestBody);
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(MediaType.get("application/json"), requestBody))
                        .build();
                Call call = client.newCall(request);
                Response response = call.execute();
                if (response.isSuccessful()) {
                    String requestBodyStr =  response.body().string();
                    System.out.printf("Response %s%n",requestBodyStr);
                    BaseResponseDTO baseResponseDTO = JSON.parseObject(requestBodyStr, BaseResponseDTO.class);
                    if (null != baseResponseDTO) {
                        SetMasterVolumeResponseDTO setMasterVolumeResponseDTO = JSON.parseObject(baseResponseDTO.data.toString(), SetMasterVolumeResponseDTO.class);
                        System.out.println("setMasterVolumeResponseDTO volume "+ setMasterVolumeResponseDTO.getVolume());
                        return setMasterVolumeResponseDTO.getVolume();
                    }
                }else{
                    System.out.println("请求失败");
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
    }

    public static Boolean cancelShutdownPlan() throws Exception {
        OkHttpClient client = new OkHttpClient();
        String ip = RCTLCore.getInstance().getServerIP();
        String url = "http://"+ip+":9999/cmd?id=2";
        System.out.println("请求ip "+url);
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                try {
                    throw e;
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
            }
        });

        return null;
    }

    public static Boolean setShutdownPlan(int planTime) throws Exception {
        return null;
    }
}
