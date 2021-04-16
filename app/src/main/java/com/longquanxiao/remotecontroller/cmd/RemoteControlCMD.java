package com.longquanxiao.remotecontroller.cmd;

import com.alibaba.fastjson.JSON;
import com.longquanxiao.remotecontroller.cmd.DTO.BaseRequestDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.BaseResponseDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.GetMasterVolumeResponseDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.SendMsgRequestDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.SetMasterVolumeRequestDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.SetMasterVolumeResponseDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.SetShutdownPlanRequestDTO;
import com.longquanxiao.remotecontroller.cmd.DTO.SetShutdownPlanResponseDTO;
import com.longquanxiao.remotecontroller.core.RCTLCore;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class RemoteControlCMD {
    public static final int  SETSHUTDOWNPLAN_CMD = 1;
    public static final int CANCELSHUTDOWNPLAN_CMD = 2;
    public static final int GETMASTERVOLUME_CMD = 3;
    public static final int SETMASTERVOLUME_CMD = 4;
    public static final int SENDMSG_CMD = 5;

    
    public static Integer getMasterVolume() throws Exception {
        try {
            OkHttpClient client = new OkHttpClient();
            String url = "http://"+ RCTLCore.getInstance().getServerIP() +":"+ RCTLCore.getInstance().getServerPort() + "/cmd";
            System.out.println("请求IP : " + url);
            String requestBody = JSON.toJSONString(new BaseRequestDTO(GETMASTERVOLUME_CMD));
            System.out.println("GetMasterVolume request body: " + requestBody);
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
                    GetMasterVolumeResponseDTO getMasterVolumeResponseDTO = JSON.parseObject(baseResponseDTO.data.toString(), GetMasterVolumeResponseDTO.class);
                    return getMasterVolumeResponseDTO.getVolume();
                }
            }else{
                System.out.println("请求失败");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
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

    public static Boolean cancelShutdownPlan() {
        try {
            OkHttpClient client = new OkHttpClient();
            String ip = RCTLCore.getInstance().getServerIP();
            String url = "http://"+ip+":" +RCTLCore.getInstance().getServerPort()+"/cmd";
            System.out.println("请求ip "+url);
            String requestBody = JSON.toJSONString(new BaseRequestDTO(CANCELSHUTDOWNPLAN_CMD));
            Request request = new Request
                    .Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.get("application/json"), requestBody))
                    .build();
            Call call = client.newCall(request);
            Response response = call.execute();
            if (response.isSuccessful()) {
                String requestBodyStr =  response.body().string();
                System.out.printf("Response %s%n",requestBodyStr);
                BaseResponseDTO baseResponseDTO = JSON.parseObject(requestBodyStr, BaseResponseDTO.class);
                if (null != baseResponseDTO && 2000000 == baseResponseDTO.optionStatus) {
                    return true;
                }
            }else{
                System.out.println("请求失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static Boolean setShutdownPlan(int planTime) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://"+ RCTLCore.getInstance().getServerIP() +":"+ RCTLCore.getInstance().getServerPort() + "/cmd";
        System.out.println("请求IP : " + url);
        String requestBody = JSON.toJSONString(new SetShutdownPlanRequestDTO(SETSHUTDOWNPLAN_CMD, planTime));

        System.out.println("SetShutdownPlanRequestDTO request body: " + requestBody);
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
            if (null != baseResponseDTO && 2000000 == baseResponseDTO.optionStatus) {
                SetShutdownPlanResponseDTO setShutdownPlanResponseDTO = JSON.parseObject(baseResponseDTO.data.toString(), SetShutdownPlanResponseDTO.class);
                System.out.println("setShutdownPlanResponseDTO shutdownTime"+ setShutdownPlanResponseDTO.getShutdownTime());
                return true;
            }
        }else{
            System.out.println("请求失败");
        }
        return false;
    }

    public static Boolean sendMsg(String msg) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String url = "http://"+ RCTLCore.getInstance().getServerIP() +":"+ RCTLCore.getInstance().getServerPort() + "/cmd";
        System.out.println("请求IP : " + url);

        String requestBody = JSON.toJSONString(new SendMsgRequestDTO(SENDMSG_CMD, msg));

        System.out.println("SendMsgRequestDTO request body: " + requestBody);
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
            return null != baseResponseDTO && 2000000 == baseResponseDTO.optionStatus;
        }else{
            System.out.println("请求失败");
        }
        return false;
    }
}
