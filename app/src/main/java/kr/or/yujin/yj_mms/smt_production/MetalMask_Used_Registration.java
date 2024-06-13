package kr.or.yujin.yj_mms.smt_production;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import device.common.DecodeStateCallback;
import device.common.ScanConst;
import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;

public class MetalMask_Used_Registration extends AppCompatActivity {

    private String TAG = "MetalMask Used Registration";

    private EditText et_UsingCount, et_Check;
    private TextView tv_DailyCount, tv_TotalCount, tv_Worker, tv_MaskSN, tvWarningInfo;
    private Button btnWorkingEnd;

    private String nowModelCode, nowWorkSide, workFactory, workLine;

    private String preText;
    private Integer editUsingCount, orgDailyCount, orgTotalCount;
    DecimalFormat myFormatter = new DecimalFormat("###,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metal_mask_used_registration);

        control_Initialize();

        tv_Worker.setText(getIntent().getStringExtra("Worker"));
        nowModelCode = getIntent().getStringExtra("Model_Code");
        nowWorkSide = getIntent().getStringExtra("Work_Side");
        workFactory = getIntent().getStringExtra("Department");
        workLine = getIntent().getStringExtra("Work_Line");

        GetData task = new GetData();
        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_mask_serial.php"
                , "Load_Mask_Serial"
                , nowModelCode
                , nowWorkSide
        );

        et_UsingCount.addTextChangedListener(new TextWatcher() {
            //변경되기전 문자열을 담고있다.
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                preText = s.toString();
            }

            //텍스트가 변경될때 마다 호출된다. 보통은 이 함수안에 이벤트를 많이 사용하는것 같다.
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(et_UsingCount.isFocusable() && !s.toString().equals("")) {
                    try{
                        editUsingCount = Integer.parseInt(et_UsingCount.getText().toString());
                    } catch (NumberFormatException e){
                        e.printStackTrace();
                        return;
                    }
                }
            }

            //텍스트가 변경된 이후에 호출.
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    tv_DailyCount.setText(String.valueOf(editUsingCount + orgDailyCount));
                    tv_TotalCount.setText(String.valueOf(editUsingCount + orgTotalCount));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        btnWorkingEnd.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_UsingCount.getText().length() != 0) {

                    if (Integer.parseInt(tv_TotalCount.getText().toString()) > 99999){
                        userOverAlarm("Over Warning");
                    } else if (Integer.parseInt(tv_TotalCount.getText().toString()) > 99000) {
                        userOverAlarm("Warning");
                    } else {
                        maskInspectionCheck();
                    }
                } else {
                    String showAlarm = "사용횟수를 입력하여 주십시오.";
                    Toast.makeText(MetalMask_Used_Registration.this, showAlarm,Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }));
    }

    private void control_Initialize(){
        et_UsingCount = (EditText) findViewById(R.id.et_UsingCount);
        et_Check = (EditText) findViewById(R.id.et_Check);

        tv_DailyCount = (TextView) findViewById(R.id.tv_DailyCount);
        tv_TotalCount = (TextView) findViewById(R.id.tv_TotalCount);
        tv_Worker = (TextView) findViewById(R.id.tv_Worker);
        tv_MaskSN = (TextView) findViewById(R.id.tv_MaskSN);

        btnWorkingEnd = (Button) findViewById(R.id.btnWorkingEnd);

        tvWarningInfo = findViewById(R.id.tvWarningInfo);
        // 기본화면 셋팅
        tvWarningInfo.setVisibility(View.INVISIBLE);
        // etCheck.setText("이상 무");
        // etUsingCount.setText(String.valueOf(0));
        tv_DailyCount.setText(String.valueOf(0));
        tv_TotalCount.setText(String.valueOf(0));
        orgDailyCount = 0;
        orgTotalCount = 0;
    }

    private void maskInspectionCheck() {
        // 검사결과가 비었다면 자동으로 '이상 무'를 입력할지 물어보는 코딩이 필요하다.
        if (et_Check.getText().toString().length() == 0){
            getWriteDialog();
        } else {
            workingStartWrite(et_Check.getText().toString());
        }
    }

    private void getWriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("저장 확인");
        builder.setMessage("검사결과 입력란이 비었습니다.\n'이상 무'로 자동입력 하시겠습니까?");
        builder.setNegativeButton("예",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //예 눌렀을때의 이벤트 처리
                        workingStartWrite("이상 무");
                    }
                });
        builder.setPositiveButton("아니오",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //아니오 눌렀을때의 이벤트 처리
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void userOverAlarm(String alarm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사용 경고");
        String alarmMessage = "";
        if (alarm.equals("Over Warning")) {
            alarmMessage = getString(R.string.mask_use_over_warning);
        } else if (alarm.equals("Warning")) {
            alarmMessage = getString(R.string.mask_use_warning);
        }
        builder.setMessage(alarmMessage);
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //확인 눌렀을때의 이벤트 처리
                        maskInspectionCheck();
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void workingStartWrite(String checkResult) {
        String strSQL = "";
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = df.format(date);

        strSQL = "insert into tb_mmms_metal_mask_history(";
        strSQL += "write_option, mask_sn, gubun, unique_note, write_id";
        strSQL += ", write_date, mask_note, use_count, daily_use_count, total_use_count, work_factory, work_line";
        strSQL += ") values(";
        strSQL += "'END'";
        strSQL += ",'" + tv_MaskSN.getText().toString() + "'";
        strSQL += ",'생산종료 등록'";
        strSQL += ",'" + checkResult + "'";
        strSQL += ",'" + tv_Worker.getText().toString() + "'";
        strSQL += ",'" + getTime + "'";
        strSQL += ",''";
        strSQL += ",'" + et_UsingCount.getText().toString() + "'";
        strSQL += ",'" + tv_DailyCount.getText().toString() + "'";
        strSQL += ",'" + tv_TotalCount.getText().toString() + "'";
        strSQL += ",'" + workFactory + "'";
        strSQL += ",'" + workLine + "'";
        strSQL += ");";

        strSQL += "update tb_mmms_metal_mask_list set using_count = '" + tv_TotalCount.getText().toString() + "'";
        strSQL += " where mask_sn = '" + tv_MaskSN.getText().toString() + "';";

        //서버로 전송한다.
        //Log.d(TAG, "Insert Text : " + strSQL);
        GetData taskSave = new GetData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/save_mask_used.php"
                , "Save_Mask_Used"
                , strSQL
        );
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

            if (ActivityName.equals("smt_production.MetalMask_Used_Registration"))
                progressDialog = ProgressDialog.show(MetalMask_Used_Registration.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("ver")) {
                postParameters = "";
            } else if(secondString.equals("Load_Mask_Serial")) {
                postParameters = "Model_Code=" + params[2];
                postParameters += "&Work_Side=" + params[3];
            } else if (secondString.equals("Save_Mask_Used")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("Load_Mask_Info")) {
                postParameters = "MaskSN=" + params[2];
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

            //progressDialog.dismiss();

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(MetalMask_Used_Registration.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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

                if (header.equals("Load_Mask_Serial")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_Mask_Serial");
                    JSONObject item = jsonArray.getJSONObject(0);
                    tv_MaskSN.setText(item.getString("Mask_SN"));
                    GetData taskMaskUsable = new GetData();
                    taskMaskUsable.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_mask_info.php"
                            , "Load_Mask_Info"
                            , tv_MaskSN.getText().toString()
                    );
                } else if (header.equals("Load_Mask_Serial!")) {
                    Toast.makeText(MetalMask_Used_Registration.this, "사용할 수 있는 메탈마스크가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("Load_Mask_Info")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_Mask_Info");
                    JSONObject item = jsonArray.getJSONObject(0);

                    if (item.getString("Mask_Usable").equals("Yes")) {
                        if (!item.getString("Last_Write_Option").equals("START")) {
                            tv_MaskSN.setText("");
                            String maskStatus = "사용 등록되어 있지 않는 상태입니다.\n확인후 다시 진행하여 주십시오.";
                            Toast.makeText(MetalMask_Used_Registration.this, maskStatus, Toast.LENGTH_SHORT).show();
                        } else {
                            tv_DailyCount.setText(item.getString("Daily_Use_Count"));
                            orgDailyCount = Integer.parseInt(item.getString("Daily_Use_Count"));
                            tv_TotalCount.setText(item.getString("Using_Count"));
                            orgTotalCount = Integer.parseInt(item.getString("Using_Count"));

                            et_UsingCount.setText(getIntent().getStringExtra("Order_Count")); //이거 작동이 안됨.

                            String maskStatus = "사용횟수, 작업자, 검사결과를 입력한 후\n'사용종료 등록' 버튼을 눌러 저장하십시오.";
                            Toast.makeText(MetalMask_Used_Registration.this, maskStatus, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        tv_MaskSN.setText("");
                        String maskStatus = "폐기 등록된 마스크이므로 사용할 수 없습니다.\n 메탈마스크 Serial No.를 재확인하여 주십시오.";
                        Toast.makeText(MetalMask_Used_Registration.this, maskStatus, Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("Save_Mask_Used")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Save_Mask_Used");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(MetalMask_Used_Registration.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Intent resultIntent = new Intent();
                        setResult(1, resultIntent);
                        finish();
                    }
                } else if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:"+ BuildConfig.VERSION_NAME)){
                        appVerAlarm();
                    }
                } else {
                    Toast.makeText(MetalMask_Used_Registration.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
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