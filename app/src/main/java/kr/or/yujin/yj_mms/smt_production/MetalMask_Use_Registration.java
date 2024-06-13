package kr.or.yujin.yj_mms.smt_production;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;
import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;
import kr.or.yujin.yj_mms.mmng.Stock_Survey;
import kr.or.yujin.yj_mms.mmps.MMPS_All_Parts_Check_List;

public class MetalMask_Use_Registration extends AppCompatActivity {

    private String TAG = "MetalMask Use Registration";

    // Scanner Setting

    private static ScanManager mScanner;
    private static DecodeResult mDecodeResult;
    private boolean mKeyLock = false;

    private AlertDialog mDialog = null;
    private int mBackupResultType = ScanConst.ResultType.DCD_RESULT_COPYPASTE;
    private Context mContext;
    private ProgressDialog mWaitDialog = null;
    private final Handler mHandler = new Handler();
    private static ScanResultReceiver mScanResultReceiver = null;
    // Scanner Setting

    private Vibrator vibrator;

    private TextView tv_Customer, tv_ItemCode, tv_ItemName, tv_WorkSide, tv_StorageBox, tv_StorageNo, tv_LoadMaskSN, tv_Worker, etMaskSN, tv_ModelCode;
    private Button btn_WorkingStart;

    private String useDepartment, useWorkLine;
    private int dailyUseCount, totalUseCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.metal_mask_use_registration);

        control_Initialize();

        tv_Customer.setText(getIntent().getStringExtra("Customer_Name"));
        tv_ItemCode.setText(getIntent().getStringExtra("Item_Code"));
        tv_ItemName.setText(getIntent().getStringExtra("Item_Name"));
        tv_WorkSide.setText(getIntent().getStringExtra("Work_Side"));
        tv_Worker.setText(getIntent().getStringExtra("Worker"));
        tv_ModelCode.setText(getIntent().getStringExtra("Model_Code"));
        useDepartment = getIntent().getStringExtra("Department");
        useWorkLine = getIntent().getStringExtra("Work_Line");

        GetData task = new GetData();
        task.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_mask_serial.php"
                , "Load_Mask_Serial"
                , tv_ModelCode.getText().toString()
                , tv_WorkSide.getText().toString()
        );

        mContext = this;
        mScanner = new ScanManager();
        mDecodeResult = new DecodeResult();
        mScanResultReceiver = new ScanResultReceiver();

        if (mScanner != null) {
            mScanner.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT);
            mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
            mScanner.aDecodeSetBeepEnable(1);

            int symID = ScanConst.SymbologyID.DCD_SYM_UPCA;
            int propCnt = mScanner.aDecodeSymGetLocalPropCount(symID);
            int propIndex = 0;

            for (int i = 0; i < propCnt; i++) {
                String propName = mScanner.aDecodeSymGetLocalPropName(symID, i);
                if (propName.equals("Send Check Character")) {
                    propIndex = i;
                    break;
                }
            }

            if (mKeyLock == false) {
                mKeyLock = true;
                mScanner.aDecodeSymSetLocalPropEnable(symID, propIndex, 0);
            } else {
                mKeyLock = false;
                mScanner.aDecodeSymSetLocalPropEnable(symID, propIndex, 1);
            }
        }

        btn_WorkingStart.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tv_LoadMaskSN.getText().toString().equals(etMaskSN.getText().toString())){
                    Toast.makeText(MetalMask_Use_Registration.this
                            , "MetalMask Serial No.가 일치 하지 않습니다."
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                String strSQL = "";
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String getTime = df.format(date);

                strSQL = "insert into tb_mmms_metal_mask_history(";
                strSQL += "write_option, mask_sn, gubun, unique_note, write_id";
                strSQL += ", write_date, mask_note, daily_use_count, total_use_count, work_factory, work_line";
                strSQL += ") values(";
                strSQL += "'START'";
                strSQL += ",'" + etMaskSN.getText().toString() + "'";
                strSQL += ",'생산시작 등록'";
                strSQL += ",'이상 무'";
                strSQL += ",'" + tv_Worker.getText().toString() + "'";
                strSQL += ",'" + getTime + "'";
                strSQL += ",''";
                strSQL += ",'" + dailyUseCount + "'";
                strSQL += ",'" + totalUseCount + "'";
                strSQL += ",'" + useDepartment + "'";
                strSQL += ",'" + useWorkLine + "');";

                //서버로 전송한다.
                //Log.d(TAG, "Insert Text : " + strSQL);
                GetData taskSave = new GetData();
                taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/save_mask_use.php"
                        , "Save_Mask_Use"
                        , strSQL
                );
            }
        }));
    }

    private void control_Initialize(){
        tv_Customer = (TextView) findViewById(R.id.tv_Customer);
        tv_ItemCode = (TextView) findViewById(R.id.tv_ItemCode);
        tv_ItemName = (TextView) findViewById(R.id.tv_ItemName);
        tv_WorkSide = (TextView) findViewById(R.id.tv_WorkSide);
        tv_StorageBox = (TextView) findViewById(R.id.tv_StorageBox);
        tv_StorageNo = (TextView) findViewById(R.id.tv_StorageNo);
        tv_LoadMaskSN = (TextView) findViewById(R.id.tv_LoadMaskSN);
        tv_Worker = (TextView) findViewById(R.id.tv_Worker);
        etMaskSN = (TextView) findViewById(R.id.etMaskSN);
        tv_ModelCode = (TextView) findViewById(R.id.tv_ModelCode);

        btn_WorkingStart = (Button) findViewById(R.id.btn_WorkingStart);
    }

    private void verCheck(){
        GetData task_VerLoad = new GetData();
        task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/yj_mms_ver.php", "ver");
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
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    public class ScanResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScanner != null) {
                try {
                    if (ScanConst.INTENT_USERMSG.equals(intent.getAction())) {
                        mScanner.aDecodeGetResult(mDecodeResult.recycle());
                        Log.d(TAG, "Scan Result : " + mDecodeResult.toString());
                        if (mDecodeResult.toString().equals("READ_FAIL")) {
                            return;
                        }
                        etMaskSN.setText(mDecodeResult.toString());
                        // 메탈마스크 상태(폐기)를 확인 한후 사용가능 상태이면 마스크 정보를 불러온다.
                        GetData taskMaskUsable = new GetData();
                        taskMaskUsable.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_mask_info.php"
                                , "Load_Mask_Info"
                                , etMaskSN.getText().toString()
                        );
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
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

            if (ActivityName.equals("smt_production.MetalMask_Use_Registration"))
                progressDialog = ProgressDialog.show(MetalMask_Use_Registration.this,
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
            } else if (secondString.equals("Load_Mask_Info")) {
                postParameters = "MaskSN=" + params[2];
            } else if (secondString.equals("Save_Mask_Use")) {
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

            //progressDialog.dismiss();

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(MetalMask_Use_Registration.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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
                    tv_LoadMaskSN.setText(item.getString("Mask_SN"));
                    tv_StorageBox.setText(item.getString("Storage_Box"));
                    tv_StorageNo.setText(item.getString("Storage_No"));
                } else if (header.equals("Load_Mask_Serial!")) {
                    Toast.makeText(MetalMask_Use_Registration.this, "사용할 수 있는 메탈마스크가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("Load_Mask_Info")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_Mask_Info");
                    JSONObject item = jsonArray.getJSONObject(0);

                    if (item.getString("Mask_Usable").equals("Yes")) {
                        if (item.getString("Last_Write_Option").equals("START")) {
                            etMaskSN.setText("");
                            String maskStatus = "사용 등록되어 있는 상태의 메탈마스크 입니다.\n확인후 다시 진행하여 주십시오.";
                            Toast.makeText(MetalMask_Use_Registration.this, maskStatus, Toast.LENGTH_SHORT).show();
                        } else {
                            dailyUseCount = Integer.parseInt(item.getString("Daily_Use_Count"));
                            totalUseCount = Integer.parseInt(item.getString("Using_Count"));
                            if (totalUseCount > 99999){
                                userOverAlarm("Over Warning");
                                etMaskSN.setText("");
                            } else if (totalUseCount > 99000) {
                                userOverAlarm("Warning");
                            }
                        }
                    } else {
                        etMaskSN.setText("");
                        String maskStatus = "폐기 등록된 마스크이므로 사용할 수 없습니다.\n 메탈마스크 Serial No.를 재확인하여 주십시오.";
                        Toast.makeText(MetalMask_Use_Registration.this, maskStatus, Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("Load_Mask_Info!")) {
                    Toast.makeText(MetalMask_Use_Registration.this, "메탈마스크 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("Save_Mask_Use")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Save_Mask_Use");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(MetalMask_Use_Registration.this, mJsonString, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MetalMask_Use_Registration.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
    }

    private DecodeStateCallback mStateCallback = new DecodeStateCallback(mHandler) {
        public void onChangedState(int state) {
            switch (state) {
                case ScanConst.STATE_ON:
                case ScanConst.STATE_TURNING_ON:
                    if (getEnableDialog().isShowing()) {
                        getEnableDialog().dismiss();
                    }
                    break;
                case ScanConst.STATE_OFF:
                case ScanConst.STATE_TURNING_OFF:
                    if (!getEnableDialog().isShowing()) {
                        getEnableDialog().show();
                    }
                    break;
            }
        };
    };

    private void initScanner() {
        if (mScanner != null) {
            mScanner.aRegisterDecodeStateCallback(mStateCallback);
            mBackupResultType = mScanner.aDecodeGetResultType();
            mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
        }
    }

    private Runnable mStartOnResume = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initScanner();
                    if (mWaitDialog != null && mWaitDialog.isShowing()) {
                        mWaitDialog.dismiss();
                    }
                }
            });
        }
    };

    private AlertDialog getEnableDialog() {
        if (mDialog == null) {
            AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle(R.string.app_name);
            dialog.setMessage("Your scanner is disabled. Do you want to enable it?");

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ScanConst.LAUNCH_SCAN_SETTING_ACITON);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    });
            dialog.setCancelable(false);
            mDialog = dialog;
        }
        return mDialog;
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
        mWaitDialog = ProgressDialog.show(mContext, "", "Scanner Running...", true);
        mHandler.postDelayed(mStartOnResume, 1000);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanConst.INTENT_USERMSG);
        filter.addAction(ScanConst.INTENT_EVENT);
        mContext.registerReceiver(mScanResultReceiver, filter);
    }

    @Override
    protected void onPause() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
            mScanner.aUnregisterDecodeStateCallback(mStateCallback);
        }
        mContext.unregisterReceiver(mScanResultReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
        }
        mScanner = null;

        super.onDestroy();
    }
}