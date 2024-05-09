package kr.or.yujin.mmps.App.Activity;

import androidx.appcompat.app.AppCompatActivity;
import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;
import kr.or.yujin.mmps.App.Class.BarcodeSplit;
import kr.or.yujin.mmps.App.Class.StringUtil;
import kr.or.yujin.mmps.R;

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
import android.util.Log;
import android.view.View;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FeederChangeActivity extends AppCompatActivity {

    private EditText etWorker, etMainDDCode, etBefFeederSN, etAftFeederSN;
    private TextView tvWorker, tvMainDDCode, tvBefFeederSN, tvAftFeederSN, tvStatus;
    private Button btnChange;

    //Server 접속주소
    private static String server_ip = MainActivity.server_ip;
    private static int server_port = MainActivity.server_port;

    // Scanner Setting
    private static final String TAG = "Feeder Change";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeder_change);

        server_ip = MainActivity.server_ip;
        server_port = MainActivity.server_port;

        etWorker = (EditText) findViewById(R.id.etWorker);
        etMainDDCode = (EditText) findViewById(R.id.etMainDDCode);
        etBefFeederSN = (EditText) findViewById(R.id.etBefFeederSN);
        etAftFeederSN = (EditText) findViewById(R.id.etAftFeederSN);

        tvWorker = (TextView) findViewById(R.id.tvWorker);
        tvMainDDCode = (TextView) findViewById(R.id.tvMainDDCode);
        tvBefFeederSN = (TextView) findViewById(R.id.tvBefFeederSN);
        tvAftFeederSN = (TextView) findViewById(R.id.tvAftPartNo);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnChange = (Button) findViewById(R.id.btnChange);

        tvStatus.setText("작업자를 입력하여 주십시오.");

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

        btnChange.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etWorker.length() != 0 &&
                    etMainDDCode.length() != 0 &&
                    etBefFeederSN.length() != 0 &&
                    etAftFeederSN.length() != 0) {
                    String insertText = "";
                    long now = System.currentTimeMillis();
                    Date date = new Date(now);
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String getTime = df.format(date);

                    insertText = "update TB_DEVICE_DATA set FEEDER_SN = '" + etAftFeederSN.getText().toString() + "'";
                    insertText += ", FEEDER_DATE = '" + getTime + "'";
                    insertText += " where DD_MAIN_NO = '" + etMainDDCode.getText().toString() + "'";
                    insertText += " and FEEDER_SN = '" + etBefFeederSN.getText().toString() + "'";

                    //서버로 전송한다.
                    Log.d(TAG, "Feeder Change SQL : " + insertText);
                    getData taskSave = new getData();
                    taskSave.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/FeederChange/FeederChange.php"
                            , "feederChange"
                            , insertText);
                } else {
                    Toast.makeText(FeederChangeActivity.this,"모든 항목이 입력되지 않았습니다.",Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }));
    }

    private class getData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
            ComponentName componentName= info.get(0).topActivity;
            String ActivityName = componentName.getShortClassName().substring(1);

            if (ActivityName.equals("App.Activity.FeederChangeActivity"))
                progressDialog = ProgressDialog.show(FeederChangeActivity.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("feederChange")){
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
                Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();
                return null;
            }
        }

        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result != null){
                Log.d(TAG, "서버 응답 내용 - " + result);
                showResult(result);
            } else {
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(FeederChangeActivity.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
            }
        }

        private void showResult (String mJsonString){
            try {
                JSONObject jsonObject = new JSONObject(mJsonString);

                String header = jsonObject.names().toString();
                header = header.replace("[", "");
                header = header.replace("\"", "");
                header = header.replace("]", "");

                if (header.equals("saveResult")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(FeederChangeActivity.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FeederChangeActivity.this, "저장 완료.", Toast.LENGTH_SHORT).show();
                        //저장완료시 컨트롤 초기화
                        etMainDDCode.setText("");
                        etWorker.setText("");
                        etAftFeederSN.setText("");
                        etBefFeederSN.setText("");
                        tvStatus.setText("작업자를 입력하여 주십시오.");
                    }
                }
                else {
                    Toast.makeText(FeederChangeActivity.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
    }

    public class ScanResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScanner != null) {
                try {
                    if (ScanConst.INTENT_USERMSG.equals(intent.getAction())) {
                        mScanner.aDecodeGetResult(mDecodeResult.recycle());
                        Log.d(TAG, "Scan Result : " + mDecodeResult.toString());
                        Log.d(TAG, "Main DD Code : " + etMainDDCode.getText().toString());
                        if (mDecodeResult.toString().equals("READ_FAIL")) {
                            return;
                        }
                        if(etWorker.length() == 0){
                            etWorker.setText(mDecodeResult.toString());
                            tvStatus.setText("작업지시번호를 스캔하여 주십시오.");
                            return;
                        } else if (etMainDDCode.length() == 0){
                            try{
                                String[] scanResultSplit = mDecodeResult.toString().split("-");
                                etMainDDCode.setText(scanResultSplit[0] + "-" + scanResultSplit[1]); // 작업지시번호를 기록
                                tvStatus.setText("교환 하려는 (전)Feeder의 Serial No를 스캔하여 주십시오.");
                            } catch (Exception e){
                                Toast.makeText(FeederChangeActivity.this, "작업지시번호를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        } else if (etBefFeederSN.length() == 0){
                            if (mDecodeResult.toString().substring(0, 3).equals("FN-")) {
                                etBefFeederSN.setText(mDecodeResult.toString().replace("FN-", ""));
                                tvStatus.setText("교환 하려는 (후)Feeder의 Serial No를 스캔하여 주십시오.");
                            } else {
                                Toast.makeText(FeederChangeActivity.this, "Feeder Serial No.를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        } else if (etAftFeederSN.length() == 0){
                            if (mDecodeResult.toString().substring(0, 3).equals("FN-")) {
                                etAftFeederSN.setText(mDecodeResult.toString().replace("FN-", ""));
                                tvStatus.setText("교환 하려는 (후)Feeder의 Serial No를 스캔하여 주십시오.");
                            } else {
                                Toast.makeText(FeederChangeActivity.this, "Feeder Serial No.를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
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

    @Override
    protected void onResume() {
        super.onResume();
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
