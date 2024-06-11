package kr.or.yujin.yj_mms.smt_production;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;

public class SMT_Production_Start_Check extends AppCompatActivity {

    private String TAG = "SMT Production Start Check";

    private TextView tv_OrderIndex, tv_Customer, tv_ItemCode, tv_ItemName, tv_OrderQty, tv_TopBottom, tv_OrderStatus;
    private Button btnBottomStart, btnTopStart, btnProductionEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smt_production_start_check);

        control_Initialize();

        Log.e(TAG, "현재 선택된 주문번호는 : " + getIntent().getStringExtra("Order Index"));
        GetData task = new GetData();
        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_po_information.php"
                , "Load Order Information"
                , getIntent().getStringExtra("Order Index")
        );
    }

    private void control_Initialize(){
        tv_OrderIndex = (TextView) findViewById(R.id.tv_OrderIndex);
        tv_Customer = (TextView) findViewById(R.id.tv_Customer);
        tv_ItemCode = (TextView) findViewById(R.id.tv_ItemCode);
        tv_ItemName = (TextView) findViewById(R.id.tv_ItemName);
        tv_OrderQty = (TextView) findViewById(R.id.tv_OrderQty);
        tv_TopBottom = (TextView) findViewById(R.id.tv_TopBottom);
        tv_OrderStatus = (TextView) findViewById(R.id.tv_OrderStatus);

        btnBottomStart = (Button) findViewById(R.id.btnBottomStart);
        btnTopStart = (Button) findViewById(R.id.btnTopStart);
        btnProductionEnd = (Button) findViewById(R.id.btnProductionEnd);
    }

    private class GetData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
            ComponentName componentName= info.get(0).topActivity;
            String ActivityName = componentName.getShortClassName().substring(1);

            if (ActivityName.equals("App.Activity.AllPartsCheck"))
                progressDialog = ProgressDialog.show(SMT_Production_Start_Check.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("ver")) {
                postParameters = "";
            } else if (secondString.equals("Load Order Information")) {
                postParameters = "Order_Index=" + params[2];
            } else if (secondString.equals("Load Work Side Information")) {
                postParameters = "Model_Code=" + params[2];
            }

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString().trim();
            } catch (Exception e) {
                //Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();
                return null;
            }
        }

        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(SMT_Production_Start_Check.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "서버 응답 내용 - " + result);
                showResult(result);
            }
        }

        private void showResult (String mJsonString){
            try {
                JSONObject jsonObject = new JSONObject(mJsonString);

                String header = jsonObject.names().toString();
                header = header.replace("[", "");
                header = header.replace("\"", "");
                header = header.replace("]", "");

                if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:" + BuildConfig.VERSION_NAME)) {
                        appVerAlarm();
                    }
                } else if (header.equals("Order_Information")){
                    JSONArray jsonArray = jsonObject.getJSONArray("Order_Information");
                    JSONObject item = jsonArray.getJSONObject(0);

                    tv_OrderIndex.setText(item.getString("Order_Index"));
                    tv_Customer.setText(item.getString("Customer_Name"));
                    tv_ItemCode.setText(item.getString("Item_Code"));
                    tv_ItemName.setText(item.getString("Item_Name"));
                    tv_OrderQty.setText(item.getString("Order_Qty"));
                    tv_OrderStatus.setText(item.getString("Order_Status"));

                    GetData task2 = new GetData();
                    task2.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_work_side_information.php"
                            , "Load Work Side Information"
                            , item.getString("Model_Code")
                    );
                } else if (header.equals("Order_Information!")){
                    tv_OrderIndex.setText("");
                    tv_Customer.setText("");
                    tv_ItemCode.setText("");
                    tv_ItemName.setText("");
                    tv_OrderQty.setText("");
                    tv_OrderStatus.setText("");
                    Toast.makeText(SMT_Production_Start_Check.this, "주문 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("Work_Side_Information")){
                    JSONArray jsonArray = jsonObject.getJSONArray("Work_Side_Information");
                    JSONObject item = jsonArray.getJSONObject(0);
                    tv_TopBottom.setText(item.getString("Work_Side"));
                } else if (header.equals("Work_Side_Information!")){
                    tv_TopBottom.setText("");
                    Toast.makeText(SMT_Production_Start_Check.this, "주문 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SMT_Production_Start_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
    }

    private void verCheck(){
        GetData task_VerLoad = new GetData();
        task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/yj_mms_ver.php", "ver");
    }

    private void appVerAlarm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사용 경고");
        builder.setMessage("프로그램 업데이트가 필요합니다.\n담당자에게 요청하십시오.");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //확인 눌렀을때의 이벤트 처리
                        dialog.dismiss();
                        //android.os.Process.killProcess(android.os.Process.myPid());
                        finish();
                    }
                });
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verCheck();
    }
}