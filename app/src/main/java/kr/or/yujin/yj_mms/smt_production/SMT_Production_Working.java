package kr.or.yujin.yj_mms.smt_production;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import kr.or.yujin.yj_mms.mmps.All_Parts_Check;
import kr.or.yujin.yj_mms.mmps.MMPS_All_Parts_Check_List;
import kr.or.yujin.yj_mms.mmps.Parts_Change;

public class SMT_Production_Working extends AppCompatActivity {

    private String TAG = "SMT Production Working";
    private String nowModelCode = "";

    private EditText et_Worker;
    private TextView tv_OrderIndex, tv_Customer, tv_ItemCode, tv_ItemName, tv_OrderQty, tv_WorkSide, tv_DeviceData, tv_Department, tv_WorkLine;
    private Button btnAllPartsCheck, btnPartsChange, btnWorkingStart, btnWorkingEnd;
    private CheckBox cb_AllPartsCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smt_production_working);

        control_Initialize();

        tv_OrderIndex.setText(getIntent().getStringExtra("Order Index"));
        tv_Customer.setText(getIntent().getStringExtra("Customer Name"));
        tv_ItemCode.setText(getIntent().getStringExtra("Item Code"));
        tv_ItemName.setText(getIntent().getStringExtra("Item Name"));
        tv_OrderQty.setText(getIntent().getStringExtra("Order Qty"));
        tv_WorkSide.setText(getIntent().getStringExtra("Work Side"));
        tv_Department.setText(getIntent().getStringExtra("Department"));
        tv_WorkLine.setText(getIntent().getStringExtra("Work Line"));
        nowModelCode = getIntent().getStringExtra("Model Code");

        // Device Data DD Main No를 불러온다.
        GetData task = new GetData();
        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_device_data_no.php"
                , "Load_DD_Main_No"
                , nowModelCode
                , tv_WorkSide.getText().toString()
        );

        GetData task2 = new GetData();
        task2.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_smd_operator.php"
                , "Load_Operator"
                , tv_OrderIndex.getText().toString()
                , tv_WorkSide.getText().toString()
        );

        btnAllPartsCheck.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_Worker.getText().toString().equals("")){
                    Toast.makeText(SMT_Production_Working.this, "작업자를 먼저 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(SMT_Production_Working.this, MMPS_All_Parts_Check_List.class);
                intent.putExtra("DD Main No", tv_DeviceData.getText().toString());
                intent.putExtra("Order_Index", tv_OrderIndex.getText().toString());
                intent.putExtra("Worker", et_Worker.getText().toString());
                intent.putExtra("Work_Side", tv_WorkSide.getText().toString());
                if (cb_AllPartsCheck.isChecked()){
                    //Check가 되어 있지 않으므로 뒤로가기키를 누를수 없도록 변경
                    intent.putExtra("First_Check", "False");
                } else {
                    intent.putExtra("First_Check", "True");
                }
                startActivityForResult(intent, 1);
            }
        }));

        btnPartsChange.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_Worker.getText().toString().equals("")){
                    Toast.makeText(SMT_Production_Working.this, "작업자를 먼저 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(SMT_Production_Working.this, Parts_Change.class);
                intent.putExtra("DD_Main_No", tv_DeviceData.getText().toString());
                intent.putExtra("Order_Index", tv_OrderIndex.getText().toString());
                intent.putExtra("Worker", et_Worker.getText().toString());
                startActivityForResult(intent, 2);
            }
        }));

        btnWorkingStart.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_Worker.getText().toString().equals("")){
                    Toast.makeText(SMT_Production_Working.this, "작업자를 먼저 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                    return;
                }
                question_WorkStart();
            }
        }));

        btnWorkingEnd.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_Worker.getText().toString().equals("")){
                    Toast.makeText(SMT_Production_Working.this, "작업자를 먼저 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                    return;
                }
                question_WorkEnd();
            }
        }));
    }

    private void control_Initialize(){
        et_Worker = (EditText) findViewById(R.id.et_Worker);

        tv_OrderIndex = (TextView) findViewById(R.id.tv_OrderIndex);
        tv_Customer = (TextView) findViewById(R.id.tv_Customer);
        tv_ItemCode = (TextView) findViewById(R.id.tv_ItemCode);
        tv_ItemName = (TextView) findViewById(R.id.tv_ItemName);
        tv_OrderQty = (TextView) findViewById(R.id.tv_OrderQty);
        tv_WorkSide = (TextView) findViewById(R.id.tv_WorkSide);
        tv_DeviceData = (TextView) findViewById(R.id.tv_DeviceData);
        tv_Department = (TextView) findViewById(R.id.tv_Department);
        tv_WorkLine = (TextView) findViewById(R.id.tv_WorkLine);

        btnAllPartsCheck = (Button) findViewById(R.id.btnAllPartsCheck);
        btnPartsChange = (Button) findViewById(R.id.btnPartsChange);
        btnWorkingStart = (Button) findViewById(R.id.btnWorkingStart);
        btnWorkingEnd = (Button) findViewById(R.id.btnWorkingEnd);
        btnAllPartsCheck.setEnabled(false);
        btnPartsChange.setEnabled(false);
        btnWorkingStart.setEnabled(false);
        btnWorkingEnd.setEnabled(false);

        cb_AllPartsCheck = (CheckBox) findViewById(R.id.cb_AllPartsCheck);
        cb_AllPartsCheck.setEnabled(false);
    }

    private void question_WorkStart() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("작업시작");
        //타이틀설정
        String showText = "품번 : " + tv_ItemCode.getText().toString();
        showText += "\n품명 : " + tv_ItemName.getText().toString();
        showText += "\n작업면 : " + tv_WorkSide.getText().toString();
        showText += "\n\n생산시작 등록을 하시겠습니까?";
        builder.setMessage(showText);
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        long now = System.currentTimeMillis();
                        Date date = new Date(now);
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String nowTime = df.format(date);

                        String strSQL;
                        strSQL = "insert into tb_mms_smd_production_history(";
                        strSQL += "order_index, smd_start_date, smd_operater, start_quantity, work_side, working_status";
                        strSQL += ") values(";
                        strSQL += "'" + tv_OrderIndex.getText().toString() + "'";
                        strSQL += ",'" + nowTime + "'";
                        strSQL += ",'" + et_Worker.getText().toString() + "'";
                        strSQL += "," + 0 + "";
                        strSQL += ",'" + tv_WorkSide.getText().toString() + "'";
                        strSQL += ",'" + tv_WorkSide.getText().toString() + " Run'";
                        strSQL += ");";
                        //주문상태 변경
                        strSQL += "update tb_mms_order_register_list set order_status = 'Production in SMD'";
                        strSQL += " where order_index = '" + tv_OrderIndex.getText().toString() + "';";
                        //생산계획 SMD시작일 등록
                        strSQL += "update tb_mms_production_plan set smd_" + tv_WorkSide.getText().toString().toLowerCase() + "_start = '" + nowTime + "'";
                        strSQL += ", smd_status = '" + tv_WorkSide.getText().toString() + " Run'";
                        strSQL += " where order_index = '" + tv_OrderIndex.getText().toString() + "';";
                        Log.e(TAG, "작업시작 전송 SQL : " + strSQL);
                        GetData task = new GetData();
                        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/save_work_start.php"
                                , "Save_Work_Start"
                                , strSQL
                        );
                    }
                });

        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    private void question_WorkEnd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("작업시작");
        //타이틀설정
        String showText = "품번 : " + tv_ItemCode.getText().toString();
        showText += "\n품명 : " + tv_ItemName.getText().toString();
        showText += "\n작업면 : " + tv_WorkSide.getText().toString();
        showText += "\n\n생산종료 등록을 하시겠습니까?";
        builder.setMessage(showText);
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        long now = System.currentTimeMillis();
                        Date date = new Date(now);
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String nowTime = df.format(date);

                        String strSQL;
                        Boolean lastCompleted = true;
                        if (getIntent().getStringExtra("Item_TB").equals("Bottom / Top")){
                            if (tv_WorkSide.getText().toString().equals("Bottom")) {
                                lastCompleted = false;
                            }
                        }
                        String completedString = tv_WorkSide.getText().toString() + " Completed";
                        if (lastCompleted) {
                            completedString = "SMD Completed";
                        }
                        strSQL = "update tb_mms_production_plan set smd_" + tv_WorkSide.getText().toString().toLowerCase() + "_end = '" + nowTime + "'";
                        strSQL += ", smd_status = '" + completedString + "'";
                        strSQL += " where order_index = '" + tv_OrderIndex.getText().toString() + "';";
                        Log.e(TAG, "작업종료 전송 SQL : " + strSQL);
                        GetData task = new GetData();
                        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/save_work_end.php"
                                , "Save_Work_End"
                                , strSQL
                        );
                    }
                });

        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    private void notFind_DeviceData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("오삽방지 시스템");
        //타이틀설정
        builder.setMessage("오삽방지 시스템 자료를\n찾을 수 없습니다. 등록하여 주십시오.\n\n자동으로 닫힙니다.");
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent resultIntent = new Intent();
                        setResult(1, resultIntent); //resultCode :1 <- DeviceData를 못찾았다.
                        finish();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "RequestCode : " + requestCode + ", ResultCode : " + resultCode);
        if (requestCode == 1) {
            // MMPS All Parts Check List에서 온 Data
            switch (resultCode) {
                case 1:
                    //정상 종료(체크) 되었을 경우.
                    enabled_All_Parts_Check();
                    break;
            }
        }
    }

    private void enabled_All_Parts_Check(){
        cb_AllPartsCheck.setChecked(true);
        btnAllPartsCheck.setEnabled(true);

        if (getIntent().getStringExtra("Order_Status").contains("작업중")){
            btnWorkingStart.setEnabled(false);
            btnWorkingEnd.setEnabled(true);
            btnPartsChange.setEnabled(true);
        } else {
            btnWorkingStart.setEnabled(true);
            btnWorkingEnd.setEnabled(false);
            btnPartsChange.setEnabled(false);
        }
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
                progressDialog = ProgressDialog.show(SMT_Production_Working.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("ver")) {
                postParameters = "";
            } else if (secondString.equals("Load_DD_Main_No")) {
                postParameters = "Model_Code=" + params[2];
                postParameters += "&Work_Side=" + params[3];
            } else if (secondString.equals("Load_All_Parts_Check_Result")) {
                postParameters = "Order_Index=" + params[2];
                postParameters += "&DD_Main_No=" + params[3];
            } else if (secondString.equals("Load_Operator")) {
                postParameters = "Order_Index=" + params[2];
                postParameters += "&Work_Side=" + params[3];
            } else if (secondString.equals("Save_Work_Start")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("Save_Work_End")) {
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
                Toast.makeText(SMT_Production_Working.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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
                } else if (header.equals("Load_DD_Main_No")) {
                    // 오삽방지 시스템이 등록되어 있다면 해당 주문의 All Parts Check가 진행 되었는지 확인한다.
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_DD_Main_No");
                    JSONObject item = jsonArray.getJSONObject(0);
                    tv_DeviceData.setText(item.getString("DD_Main_No"));

                    String phpDocumentName = "load_all_parts_check_bottom_result.php";
                    if (tv_WorkSide.getText().toString().equals("Top")){
                        phpDocumentName = "load_all_parts_check_top_result.php";
                    }
                    GetData task = new GetData();
                    task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/" + phpDocumentName
                            , "Load_All_Parts_Check_Result"
                            , tv_OrderIndex.getText().toString()
                            , tv_DeviceData.getText().toString()
                    );
                } else if (header.equals("Load_DD_Main_No!")) {
                    notFind_DeviceData();
                } else if (header.equals("Load_All_Parts_Check_Result")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_All_Parts_Check_Result");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (item.getString("Check_Result").equals("No")) {
                        // All Parts Check가 진행 되지 않았으므로 All Parts Check로 넘어간다.
                        Toast.makeText(SMT_Production_Working.this, "All Parts Check를 먼저 진행하여 주십시오.", Toast.LENGTH_LONG).show();
                        btnAllPartsCheck.setEnabled(true);
                        btnPartsChange.setEnabled(false);
                        btnWorkingStart.setEnabled(false);
                        btnWorkingEnd.setEnabled(false);
                        cb_AllPartsCheck.setChecked(false);
                    } else {
                        enabled_All_Parts_Check();
                    }
                } else if (header.equals("Load_Operator")){
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_Operator");
                    JSONObject item = jsonArray.getJSONObject(0);
                    et_Worker.setText(item.getString("SMD_Operator"));
                } else if (header.equals("Load_Operator!")){
                } else if (header.equals("Save_Work_Start")){
                    JSONArray jsonArray = jsonObject.getJSONArray("Save_Work_Start");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(SMT_Production_Working.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        btnWorkingEnd.setEnabled(true);
                        btnWorkingStart.setEnabled(false);
                        btnPartsChange.setEnabled(true);
                    }
                } else if (header.equals("Save_Work_End")){
                    JSONArray jsonArray = jsonObject.getJSONArray("Save_Work_End");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(SMT_Production_Working.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Intent resultIntent = new Intent();
                        setResult(2, resultIntent); //resultCode :2 <- 작업이 종료되었다.
                        finish();
                    }
                } else {
                    Toast.makeText(SMT_Production_Working.this, mJsonString, Toast.LENGTH_SHORT).show();
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