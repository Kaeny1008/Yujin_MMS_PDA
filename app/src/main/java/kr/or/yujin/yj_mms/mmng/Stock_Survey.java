package kr.or.yujin.yj_mms.mmng;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;
import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;
import kr.or.yujin.yj_mms.common.NoConvet;
import kr.or.yujin.yj_mms.mmps.All_Parts_Check;
import kr.or.yujin.yj_mms.mmps.Device_Data;
import kr.or.yujin.yj_mms.mmps.MMPS_Main;

public class Stock_Survey extends AppCompatActivity {

    private static final String TAG = "재고조사 실행";

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

    private Button btnResultSave;
    private TextView tvStatus, tvWorker, tvSurveyNo, tvPartNo, tvLotNo, tvQty, tvPartCode, tvVendor;
    private EditText etWorker, etPartNo, etLotNo, etQty, etTotalQty, etAccumulateQty, etPartCode, etVendor;
    private Spinner spnSurveyNo;
    private CheckBox checkBox;
    private ArrayList<String> surveyNoList; // 스피너의 네임 리스트
    private ArrayAdapter<String> surveyNoListADT; // 스피너에 사용되는 ArrayAdapter
    private int firstRun_spnSurveyNo = 0;

    private String planContentNo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mmng_stock_survey);

        initControl();

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

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        //데이터 준비
        surveyNoList = new ArrayList<String>();
        // 어댑터 생성
        surveyNoListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, surveyNoList);

        // 어댑터 설정
        spnSurveyNo = (Spinner) findViewById(R.id.spnSurveyNo);
        spnSurveyNo.setAdapter(surveyNoListADT);

        spnSurveyNo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_spnSurveyNo == 0) { //자동실행 방지
                    firstRun_spnSurveyNo += 1;
                } else {
                    tvStatus.setText("자재를 스캔한 후\n수량을 입력하여 주십시오.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });

        etQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        // 검색 동작
                        break;
                    default:
                        // 기본 엔터키 동작
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        //actionWrite();
                        return false;
                }
                return true;
            }
        });
    }

    private void checkInsert(){
        if (etPartCode.getText().toString().length() == 0
                || etWorker.getText().toString().length() == 0){
            Toast.makeText(Stock_Survey.this, "필수 입력항목을 모두 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        String insertText = "";
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = df.format(date);

        insertText = "insert into tb_mms_material_stock_survey_action_content(";
        insertText += "action_content_no, plan_content_no, part_code";
        insertText += ", vendor, part_no, lot_no, stock_qty, checker, check_date";
        insertText += ") ";
        insertText += "select f_mms_stock_survey_action_no(date_format(now(), '%Y-%m-%d'))";
        insertText += ", '" + planContentNo + "'";
        insertText += ", '" + etPartCode.getText().toString() + "'";
        insertText += ", '" + etVendor.getText().toString() + "'";
        insertText += ", '" + etPartNo.getText().toString().replace("'", "\\'") + "'";
        insertText += ", '" + etLotNo.getText().toString().replace("'", "\\'") + "'";
        insertText += ", '" + etQty.getText().toString() + "'";
        insertText += ", '" + etWorker.getText().toString() + "'";
        insertText += ", '" + getTime + "'";
        insertText += ";";

        /*
        // 자재창고를 현재 수량으로 업데이트 한다.
        insertText += "update tb_mms_material_warehousing set available_qty = " + etQty.getText().toString();
        insertText += " where customer_code = f_mms_stock_survey_customer_code('" + planContentNo + "')";
        insertText += " and part_code = '" + etPartCode.getText().toString() + "'";
        insertText += " and part_no = '" + etPartNo.getText().toString().replace("'", "\\'") + "'";
        insertText += " and part_lot_no = '" + etLotNo.getText().toString().replace("'", "\\'") + "'";
        insertText += ";";
         */

        Log.d(TAG, "전송할 SQL : " + insertText);

        GetData taskSave = new GetData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Stock_Survey/check_insert.php"
                , "checkInsert"
                , insertText);
    }

    private void autoCheckQuestion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("스캔결과 자동저장");
        //타이틀설정
        builder.setMessage("자동저장을 활성화 하시겠습니까?");
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        checkBox.setChecked((true));
                    }
                });

        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        checkBox.setChecked((false));
                        //dialog.dismiss();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    private void buttonClick(View v) {
        switch(v.getId()){
            case R.id.checkBox:
                if (checkBox.isChecked()){
                    autoCheckQuestion();
                }
                break;
            case R.id.btnResultSave:
                checkInsert();
                break;
        }
    }

    private void tvClick(View v) {
        switch(v.getId()){
            case R.id.tvWorker:
                tvStatus.setText("작업자를 입력(스캔)하여 주십시오.");
                etWorker.setText("");
                break;
            case R.id.tvPartCode:
            case R.id.tvVendor:
            case R.id.tvPartNo:
            case R.id.tvLotNo:
            case R.id.tvQty:
                tvStatus.setText("재고조사번호를 선택하여 주십시오.");
                etPartCode.setText("");
                etVendor.setText("");
                etPartNo.setText("");
                etLotNo.setText("");
                etQty.setText("");
                etTotalQty.setText("");
                etAccumulateQty.setText("");
                break;
        }
    }

    private void initControl(){
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvStatus.setText("작업자를 입력(스캔)하여 주십시오.");

        tvWorker = (TextView) findViewById(R.id.tvWorker);
        tvSurveyNo = (TextView) findViewById(R.id.tvSurveyNo);
        tvPartCode = (TextView) findViewById(R.id.tvPartCode);
        tvVendor = (TextView) findViewById(R.id.tvVendor);
        tvPartNo = (TextView) findViewById(R.id.tvPartNo);
        tvLotNo = (TextView) findViewById(R.id.tvLotNo);
        tvQty = (TextView) findViewById(R.id.tvQty);
        //연결
        tvWorker.setOnClickListener(this::tvClick);
        tvPartCode.setOnClickListener(this::tvClick);
        tvVendor.setOnClickListener(this::tvClick);
        tvPartNo.setOnClickListener(this::tvClick);
        tvLotNo.setOnClickListener(this::tvClick);
        tvQty.setOnClickListener(this::tvClick);

        checkBox = (CheckBox) findViewById(R.id.checkBox);
        //연결
        checkBox.setOnClickListener(this::buttonClick);

        etWorker = (EditText) findViewById(R.id.etWorker);
        etPartNo = (EditText) findViewById(R.id.etPartNo);
        etLotNo = (EditText) findViewById(R.id.etLotNo);
        etQty = (EditText) findViewById(R.id.etQty);
        etTotalQty = (EditText) findViewById(R.id.etTotalQty);
        etAccumulateQty = (EditText) findViewById(R.id.etAccumulateQty);
        etPartCode = (EditText) findViewById(R.id.etPartCode);
        etVendor = (EditText) findViewById(R.id.etVendor);

        spnSurveyNo = (Spinner) findViewById(R.id.spnSurveyNo);

        btnResultSave = (Button) findViewById(R.id.btnResultSave);
        //연결
        btnResultSave.setOnClickListener(this::buttonClick);
    }

    private void verCheck(){
        GetData task_VerLoad = new GetData();
        task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/yj_mms_ver.php", "ver");
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

                        if (etWorker.length() == 0){
                            etWorker.setText(mDecodeResult.toString());
                            tvStatus.setText("재고조사번호를 선택하여 주십시오.");
                            //재고조사번호를 불러온다.
                            GetData getData = new GetData();
                            getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Stock_Survey/load_stock_survey_no.php"
                                    , "load_stock_survey_no");
                            return;
                        }

                        if (!spnSurveyNo.getSelectedItem().toString().equals("재고조사번호를 선택하여 주십시오.")){
                            String barcode[] = mDecodeResult.toString().split("!");
                            if (barcode.length < 5){
                                tvStatus.setText("바코드를 확인 할 수 없습니다.");
                                Toast.makeText(Stock_Survey.this, "바코드를 확인 할 수 없습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                etPartCode.setText(barcode[0]);
                                etPartNo.setText(barcode[1]);
                                etLotNo.setText(barcode[2]);
                                etQty.setText(barcode[3]);
                                etVendor.setText(barcode[4]);
                                Log.d(TAG, "변환 : " + etPartNo.getText().toString().replace("'", ""));

                                /*
                                GetData getData = new GetData();
                                getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Stock_Survey/load_stock_qty.php"
                                        , "load_stock_qty"
                                        , spnSurveyNo.getSelectedItem().toString()
                                        , etPartCode.getText().toString());
                                 */

                                GetData getData = new GetData();
                                getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Stock_Survey/load_exist_part.php"
                                        , "load_exist_part"
                                        , spnSurveyNo.getSelectedItem().toString()
                                        , etPartCode.getText().toString()
                                        , etPartNo.getText().toString()
                                        , etLotNo.getText().toString());
                            }
                        } else {
                            tvStatus.setText("재고조사번호를 선택하여 주십시오.");
                            Toast.makeText(Stock_Survey.this, "재고조사번호를 선택하여 주십시오.", Toast.LENGTH_SHORT).show();
                            return;
                        }
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

            if (ActivityName.equals("App.Activity.AllPartsCheck"))
                progressDialog = ProgressDialog.show(Stock_Survey.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("load_stock_survey_no")) {
                //postParameters = "ddMainNo=" + params[2];
                //postParameters += "&machineNo=" + params[3];
                postParameters = "";
            } else if (secondString.equals("load_exist_part")) {
                postParameters = "PlanNo=" + params[2];
                postParameters += "&PartCode=" + params[3];
                postParameters += "&PartNo=" + params[4];
                postParameters += "&LotNo=" + params[5];
            } else if (secondString.equals("load_stock_qty")) {
                postParameters = "PlanNo=" + params[2];
                postParameters += "&PartCode=" + params[3];
            } else if (secondString.equals("ver")) {
                postParameters = "";
            } else if (secondString.equals("checkInsert")) {
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
                Toast.makeText(Stock_Survey.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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

                if (header.equals("surveyNo")) {
                    surveyNoList.clear();
                    surveyNoList.add("재고조사번호를 선택하여 주십시오.");

                    JSONArray jsonArray = jsonObject.getJSONArray("surveyNo");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        surveyNoList.add(item.getString("Plan_No"));
                    }
                    surveyNoListADT.notifyDataSetChanged();
                } else if (header.equals("Exist_Part")){
                    long[] pattern = {500,1000,500,1000};
                    vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                    Toast.makeText(Stock_Survey.this, "이미 등록된 자재 입니다.", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("이미 등록된 자재 입니다.");
                    etPartCode.setText("");
                    etPartNo.setText("");
                    etLotNo.setText("");
                    etQty.setText("");
                    etTotalQty.setText("");
                    etVendor.setText("");
                    etAccumulateQty.setText("");
                } else if (header.equals("Exist_Part!")){
                    GetData getData = new GetData();
                    getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Stock_Survey/load_stock_qty.php"
                            , "load_stock_qty"
                            , spnSurveyNo.getSelectedItem().toString()
                            , etPartCode.getText().toString());
                } else if (header.equals("stock_Qty")){
                    JSONArray jsonArray = jsonObject.getJSONArray("stock_Qty");
                    JSONObject item = jsonArray.getJSONObject(0);
                    etTotalQty.setText(item.getString("Stock_Qty"));
                    planContentNo = item.getString("Plan_Content_No");
                    etAccumulateQty.setText(item.getString("Accumulated_Qty"));
                    if (checkBox.isChecked()){
                        checkInsert();
                    }
                } else if (header.equals("stock_Qty!")){
                    Toast.makeText(Stock_Survey.this, "해당 자재를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("해당 자재를 찾을 수 없습니다.\n 다시 스캔하여 주십시오.");
                    etPartCode.setText("");
                    etPartNo.setText("");
                    etLotNo.setText("");
                    etQty.setText("");
                    etTotalQty.setText("");
                    etAccumulateQty.setText("");
                } else if (header.equals("surveyNo!")) {
                    Toast.makeText(Stock_Survey.this, "등록된 재고조사항목이 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("insert")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("insert");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(Stock_Survey.this, mJsonString, Toast.LENGTH_SHORT).show();
                        long[] pattern = {500,1000,500,1000};
                        vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                        return;
                    }
                    vibrator.vibrate(100); // miliSecond, 지정한 시간동안 진동
                    etPartCode.setText("");
                    etVendor.setText("");
                    etPartNo.setText("");
                    etLotNo.setText("");
                    etQty.setText("");
                    etTotalQty.setText("");
                    etAccumulateQty.setText("");
                    tvStatus.setText("저장완료.\n다음 자재를 스캔하여 주십시오.");
                    Toast.makeText(Stock_Survey.this, "저장완료.\n다음 자재를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:"+ BuildConfig.VERSION_NAME)){
                        appVerAlarm();
                    }
                } else {
                    Toast.makeText(Stock_Survey.this, mJsonString, Toast.LENGTH_SHORT).show();
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