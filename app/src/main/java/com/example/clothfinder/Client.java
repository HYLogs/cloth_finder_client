package com.example.clothfinder;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Client {
    Socket client;
    InetSocketAddress ipep;
    OutputStream sender;
    InputStream receiver;

    String[] categories = null;
    JSONArray products = null;

    public void connection(){
        try{
            client = new Socket();
            ipep = new InetSocketAddress("<Your IP>", 5000);
            client.connect(ipep);
            sender = client.getOutputStream();
            receiver = client.getInputStream();
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        String sendMsg = "연결 종료";
        byte[] data = sendMsg.getBytes();
        sendMsg(3, data);

        System.out.println(client.getInetAddress() + "의 포트 " + client.getPort());
        if (receiver != null)
            receiver.close();

        if (sender != null)
            sender.close();

        if (client != null)
            client.close();
    }

    public boolean checkStatus(){
        return client.isConnected();
    }

    //1. 이미지 송신
    public void sendImage(Intent intent, ContentResolver Resolver) {
        String msg;
        Bitmap bitmap = CreateBitmap(intent, Resolver); //비트맵 만들기
        byte[] imageByte = bitmapToByteArray(bitmap); // BitMap to ByteArray
        bitmap.recycle();
        bitmap = null;

        try {
            // 1.1. 이미지 전송
            byte[] data = imageByte;
            sendMsg(1, data);

            // 1.2. 카테고리 받기
            msg = receiveMsg();

            // 1.3. Parser to JSON
            JSONObject resultJSON = new JSONObject(msg);

            //1.4. 카테고리 배열 생성
            JSONArray categoryJSONArray = resultJSON.getJSONArray("categories");
            String[] categoryArray = JSONArrayToStringArray(categoryJSONArray);

            //1.5. 카테고리 설정
            setCategories(categoryArray);
            return;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    //1-1. 비트맵 만들기
    private Bitmap CreateBitmap(Intent intent, ContentResolver Resolver){
        Bitmap bitmap = null;
        Uri uri = intent.getData();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(Resolver, uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(Resolver, uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    //1-2. 비트맵을 바이트배열로 변경
    private byte[] bitmapToByteArray( Bitmap bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        bitmap.compress( Bitmap.CompressFormat.PNG, 100, stream) ;
        byte[] byteArray = stream.toByteArray() ;
        return byteArray;
    }

    //1-3. JSONArray를 String[]로 변환
    private String[] JSONArrayToStringArray(JSONArray JsonArray) throws JSONException {
        String[] stringsArray = new String[JsonArray.length()];
        for (int i = 0; i < JsonArray.length(); i++) {
            stringsArray[i] = JsonArray.getString(i);
        }
        return stringsArray;
    }

    //2. 선택 카테고리 전송
    public void sendCategory(String Category) {
        String msg;
        try {
            // 2.1 카테고리 전송
            String sendMsg = Category;
            // string을 byte배열 형식으로 변환한다.
            byte[] data = sendMsg.getBytes();
            // 전송
            sendMsg(2, data);

            // 2. 카테고리 받기
            msg = receiveMsg();

            // 1.3. Parser to JSON
            JSONObject resultJSON = new JSONObject(msg);

            //1.4. 품목 배열 생성
            JSONArray categoryJSONArray = resultJSON.getJSONArray("products");

            setProducts(categoryJSONArray);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(int transNum, byte[] data) throws IOException {
        // 1. 카테고리를 전송하는지 알리는 값
        ByteBuffer a = ByteBuffer.allocate(4);
        // byte포멧은 little 엔디언이다.
        a.order(ByteOrder.LITTLE_ENDIAN);
        a.putInt(transNum);
        // 값 전송
        sender.write(a.array(), 0, 4);

        // ByteBuffer를 통해 데이터 길이를 byte형식으로 변환한다.
        ByteBuffer b = ByteBuffer.allocate(4);
        // byte포멧은 little 엔디언이다.
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(data.length);
        // 데이터 길이 전송
        sender.write(b.array(), 0, 4);
        // 데이터 전송
        sender.write(data);
    }

    private String receiveMsg() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] data = new byte[4];
        String msg = "";

        // 데이터 길이를 받는다.
        receiver.read(data, 0, 4);
        // ByteBuffer를 통해 little 엔디언 형식으로 데이터 길이를 구한다.
        b = ByteBuffer.wrap(data);
        b.order(ByteOrder.LITTLE_ENDIAN);
        int length = b.getInt();
        // 데이터를 받을 버퍼를 선언한다.
        char[] chars = new char[length];
        InputStreamReader reader = new InputStreamReader(receiver);
        // 데이터가 너무 크면 분할하여 수신
        int max = 25;
        if (length > max){
            int count = length/max;
            int remainder = length%max;
            for(int i = 0; i < count; i++){
                chars = new char[max];
                reader.read(chars, 0, max);
                msg += String.valueOf(chars);
            }
            chars = new char[remainder];
            reader.read(chars, 0, remainder);
            msg += String.valueOf(chars);
        }else{
            reader.read(chars, 0, length);
            msg = String.valueOf(chars);
        }
        return msg;
    }


    public void setCategories(String[] categories) {
        this.categories = categories;
    }
    public String[] getCategories(){
        return this.categories;
    }
    public void setProducts(JSONArray products){
        this.products = products;
    }
    public JSONArray getProducts(){
        return this.products;
    }
}

