package kr.or.yujin.yj_mms.mmps;

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
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;
import kr.or.yujin.yj_mms.mmng.Stock_Survey;
import kr.or.yujin.yj_mms.smt_production.SMT_Production_Start;
import kr.or.yujin.yj_mms.smt_production.SMT_Production_Start_Check;
import kr.or.yujin.yj_mms.smt_production.SMT_Production_Working;

public class MMPS_All_Parts_Check_List extends AppCompatActivity {

    private String TAG = "All Parts Check List";

    private TextView tv_DeviceData;
    private TableLayout tableLayout;
    private String order_index;
    private Button btnResultSave;

    private Vibrator vibrator;
    private long[] pattern1 = {100,100,100,100,100,100,100,100};
    private long[] pattern2 = {500,1000,500,1000};

    private String firstCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mmps_all_parts_check_list);

        control_Initialize();

        Log.e(TAG, "All Parts Check 했다 안했다 : " + getIntent().getBooleanExtra("Previous_CheckResult", false));

        firstCheck = getIntent().getStringExtra("First_Check");
        tv_DeviceData.setText(getIntent().getStringExtra("DD Main No"));
        order_index = getIntent().getStringExtra("Order_Index");
        GetData task = new GetData();
        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_all_parts_check_list.php"
                , "Load Machine List"
                , tv_DeviceData.getText().toString()
        );

        btnResultSave.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allList_Check()) {
                    if (!getIntent().getBooleanExtra("Previous_CheckResult", false)) {
                        vibrator.vibrate(pattern1, -1); // miliSecond, 지정한 시간동안 진동

                        String colName = "smd_all_parts_check_bottom";
                        if (getIntent().getStringExtra("Work_Side").equals("Top")) {
                            colName = "smd_all_parts_check_top";
                        }

                        String strSQL = "update tb_mms_order_register_list set " + colName + " = 'Yes'";
                        strSQL += " where order_index = '" + order_index + "';";

                        GetData taskSave = new GetData();
                        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/check_insert.php"
                                , "Check_Insert"
                                , strSQL);
                    } else {
                        Intent resultIntent = new Intent();
                        setResult(2, resultIntent);
                        finish();
                    }
                } else {
                    vibrator.vibrate(pattern2, -1); // miliSecond, 지정한 시간동안 진동
                    Toast.makeText(MMPS_All_Parts_Check_List.this
                            , "모든 항목이 확인되지 않았습니다."
                            , Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }

    private Boolean allList_Check(){
        Integer checkCount = 0;
        TableLayout tableView = (TableLayout) findViewById(R.id.tlList);
        View myTempView = null;
        int noOfChild = tableView.getChildCount();
        for (int i = 1; i < noOfChild; i++) {
            myTempView = tableView.getChildAt(i);
            View vv = ((TableRow) myTempView).getChildAt(3);
            if (vv instanceof TextView) {
                //Log.d(TAG, "현재 행 : " + ((TextView) vv).getText().toString());
                if (!((TextView) vv).getText().toString().equals("")) {
                    checkCount +=1;
                }
            }
        }
        Log.e(TAG, "Table Row Count : " + (noOfChild-1) + ",   확인 Count : " + checkCount);
        if ((noOfChild-1)==checkCount){
            return true;
        } else {
            return false;
        }
    }

    private void control_Initialize(){
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        tv_DeviceData = (TextView) findViewById(R.id.tv_DeviceData);
        tableLayout = (TableLayout) findViewById(R.id.tlList);
        btnResultSave = (Button) findViewById(R.id.btnResultSave);
    }

    private void verCheck(){
        GetData task_VerLoad = new GetData();
        task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/yj_mms_ver.php", "ver");
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
                progressDialog = ProgressDialog.show(MMPS_All_Parts_Check_List.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("ver")) {
                postParameters = "";
            } else if (secondString.equals("Load Machine List")) {
                postParameters = "DD_Main_No=" + params[2];
            } else if (secondString.equals("Check_Insert")) {
                postParameters = "sql=" + params[2];
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
                Toast.makeText(MMPS_All_Parts_Check_List.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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
                } else if (header.equals("Load_Machine_List")) {
                    //테이블 레이아웃 초기화
                    tableLayout.removeViews(1, tableLayout.getChildCount()-1);

                    JSONArray jsonArray = jsonObject.getJSONArray("Load_Machine_List");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        final TableRow tableRow = new TableRow(MMPS_All_Parts_Check_List.this); //tablerow 생성
                        tableRow.setLayoutParams(new TableRow.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT
                                , ViewGroup.LayoutParams.WRAP_CONTENT
                        ));

                        int selColor = Color.WHITE;
                        if (i % 2 == 1) {
                            selColor = Color.parseColor("#00D8FF");
                        }
                        TextView textView = new TextView(MMPS_All_Parts_Check_List.this);
                        textView.setText(String.valueOf(tableLayout.getChildCount()));
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundColor(selColor);
                        textView.setMinHeight(100);
                        tableRow.addView(textView);
                        TextView textView2 = new TextView(MMPS_All_Parts_Check_List.this);
                        textView2.setText(item.getString("Machine_No"));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setBackgroundColor(selColor);
                        textView2.setMinHeight(100);
                        tableRow.addView(textView2);
                        TextView textView3 = new TextView(MMPS_All_Parts_Check_List.this);
                        textView3.setText(item.getString("Feeder_No"));
                        textView3.setGravity(Gravity.CENTER);
                        textView3.setBackgroundColor(selColor);
                        textView3.setMinHeight(100);
                        tableRow.addView(textView3);
                        TextView textView4 = new TextView(MMPS_All_Parts_Check_List.this);
                        textView4.setText("");
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setBackgroundColor(selColor);
                        textView4.setMinHeight(100);
                        tableRow.addView(textView4);

                        //Tag에 Order Index를 넣어서 클릭할 때 표시
                        tableRow.setTag(tv_DeviceData.getText().toString() + "-" + item.getString("Machine_No"));

                        tableRow.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View arg0) {
                                Log.e(TAG, "선택된 Po No.: " + tableRow.getTag().toString());
                                Intent intent = new Intent(MMPS_All_Parts_Check_List.this, All_Parts_Check.class);
                                intent.putExtra("Device_Data", tableRow.getTag().toString());
                                intent.putExtra("Order_Index", order_index);
                                intent.putExtra("Worker", getIntent().getStringExtra("Worker"));
                                startActivityForResult(intent, 1);
                            }
                        });
                        tableLayout.addView(tableRow);
                    }
                } else if (header.equals("insert")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("insert");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(MMPS_All_Parts_Check_List.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        // 마지막 단계
                        Intent resultIntent = new Intent();
                        setResult(1, resultIntent);
                        finish();
                    }
                } else {
                    Toast.makeText(MMPS_All_Parts_Check_List.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        //Log.e(TAG, "RequestCode : " + requestCode + ", ResultCode : " + resultCode);
        if (requestCode == 1) {
            // All Parts Check에서 온 Data
            switch (resultCode) {
                case 1:
                    //정상 종료 되었을 경우.
                    String checker = data.getStringExtra("Checker");
                    String machine_no = data.getStringExtra("Machine_No");
                    TableLayout tableView = (TableLayout) findViewById(R.id.tlList);
                    View myTempView = null;
                    int noOfChild = tableView.getChildCount();
                    for (int i = 1; i < noOfChild; i++) {
                        myTempView = tableView.getChildAt(i);
                        View vv = ((TableRow) myTempView).getChildAt(1);
                        if (vv instanceof TextView) {
                            //Log.d(TAG, "현재 행 : " + ((TextView) vv).getText().toString());
                            if (((TextView) vv).getText().toString().equals(machine_no)) {
                                View vv2 = ((TableRow) myTempView).getChildAt(3);
                                ((TextView) vv2).setText(checker); //체크표시
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }

    @Override
    public void onBackPressed() {
        if (firstCheck.equals("True")){
            Toast.makeText(this, "뒤로가기 키는 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
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