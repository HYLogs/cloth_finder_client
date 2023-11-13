package com.example.clothfinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Client connect = new Client(); //통신 객체
    MenuItem logoutItem;
    private static final int REQ_ONE_TAP = 2;
    private boolean showOneTapUI = true;
    private FirebaseAuth mAuth;
    private static GoogleSignInClient mGoogleSignInClient;
    private long backKeyPressedTime = 0; // 뒤로가기 버튼을 눌렀던 시간 저장

    //로딩창을 위한 객체들
    ProgressDialog dialog;
    private Handler handler = new Handler();

    ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            loadImage(result.getData());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar tb = (Toolbar) findViewById(R.id.tb_main);
        setSupportActionBar(tb);

        TextView tipView = (TextView) findViewById(R.id.tip);
        tipView.setText("Tip: 그림자로 인해 정확도가 떨어질 수 있습니다");

        //백그라운드에서 종료되도 onDestroy() 실행되도록 하는 것
        startService(new Intent(this, ForecdTerminationService.class));

        //통신 객체 초기화 및 연결
        Thread conn = new Thread(() -> {
            connect.connection();
        });
        conn.start();

        //로딩창 setup
        dialog = new ProgressDialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        // 다이얼로그 영역밖을 터치했을때와 백키를 눌렀을때 dismiss 되지 않게 하기
        dialog.setCancelable(false);

        ImageView imageView = (ImageView) findViewById(R.id.main_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageViewClickHandler();
            }
        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!connect.checkStatus()){
            showDialog("ERROR", "서버와 연결이 끊겼어요. \n인터넷 연결을 확인해주세요!");
            System.out.println("연결이 끊김");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog.isShowing()) dialog.dismiss();
        if (connect != null && connect.checkStatus()){
            Thread test = new Thread(() -> {
                try {
                    connect.close();
                } catch (IOException e) {
                    System.out.println("서버 종료 실패! - from MainActivity");
                    e.printStackTrace();
                }
            });
            test.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.tb_main, menu);
        return true;
    }

    private void imageViewClickHandler(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityResult.launch(intent);
    }

    private void loadImage(Intent intent) throws InterruptedException {
        ImageView imageView = findViewById(R.id.main_image);

        Uri targetImageUri = intent.getData();
        Glide.with(getApplicationContext()).load(targetImageUri).override(500).into(imageView);

        LinearLayout view = (LinearLayout) findViewById(R.id.main_layout);

        view.removeAllViews();

        //로딩창 생성
        dialog.show();

        //이미지 전송
        Thread sendImage = new Thread(() -> {
            connect.sendImage(intent, getContentResolver());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //이미지에 대한 카테고리 반환 값 가져오기
                    String[] classes = connect.getCategories();
                    connect.setCategories(null);

                    for (int i = 0; i < classes.length; i++) {
                        makeDetectedClassButton(classes[i], targetImageUri);
                    }
                    dialog.dismiss();
                }
            });
        });
        sendImage.start();
    }

//    private void signOut() {
//        FirebaseAuth.getInstance().signOut();
//        Intent intent = new Intent(this, LoginActivity.class);
//        startActivity();
//        finish();
//    }

    private void revokeAccess() {
        mAuth.getCurrentUser().delete();
    }

    public void makeDetectedClassButton(String detectedClass, Uri targetImageUri) {
        Button button = new Button(this);

        button.setTextColor(Color.parseColor("#ffffff"));
        button.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent productSelect = new Intent(MainActivity.this, ProductSelectActivity.class);
                String selectedClass = (String) ((Button) view).getText();

                // 로딩창
                dialog.show();

                //카테고리 전송
                Thread sendCategory = new Thread(() -> {
                    connect.sendCategory(selectedClass);
                    handler.post(new Runnable() {
                        public void run () {
                            //카테고리와 유사한 의상 가져오기
                            JSONArray products = connect.getProducts();
                            connect.setProducts(null); // 다시 초기화

                            ArrayList<Product> productArrayList = new ArrayList<>();

                            for (int i = 0; i < products.length(); i++) {
                                try {
                                    JSONObject product = products.getJSONObject(i);
                                    Product temp = new Product();
                                    temp.setName(product.getString("title"));
                                    temp.setLinkUri(product.getString("link"));
                                    temp.setAccuracy(Math.round(Float.parseFloat(product.getString("similarity"))));
                                    temp.setImageUri(product.getString("image"));
                                    temp.setPrice(Integer.parseInt(product.getString("lprice")));
                                    productArrayList.add(temp);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            productSelect.putExtra("products", productArrayList);
                            productSelect.putExtra("selectedClass", selectedClass);
                            productSelect.putExtra("targetImageUri", targetImageUri);
                            dialog.dismiss();
                            startActivity(productSelect);
                        }
                    }); //end handler
                }); //end Thread
                sendCategory.start();
            }
        });
        LinearLayout view = (LinearLayout) findViewById(R.id.main_layout);
        button.setText(detectedClass);

        button.setBackgroundResource(R.drawable.class_button);

        TextView btn_margin = new TextView(this);
        btn_margin.setHeight(20);

        view.addView(button);
        view.addView(btn_margin);
    }

    public void showDialog(String title, String text){
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        dlg.setTitle(title); //제목
        dlg.setMessage(text); // 메시지
        dlg.setIcon(R.drawable.ic_dialog_24); // 아이콘 설정
        // 버튼 클릭시 동작
        dlg.setPositiveButton("확인",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        dlg.show();
    }

    @Override
    public void onBackPressed() {
        // 기존의 뒤로가기 버튼의 기능 제거
        // super.onBackPressed();

        // 2000 milliseconds = 2 seconds
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2초 이내에 뒤로가기 버튼을 한번 더 클릭시 finish()(앱 종료)
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            finish();
        }
    }
}