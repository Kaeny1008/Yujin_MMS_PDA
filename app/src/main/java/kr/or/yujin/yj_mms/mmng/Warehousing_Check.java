package kr.or.yujin.yj_mms.mmng;

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

import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;
import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;

public class Warehousing_Check extends AppCompatActivity {

    private static final String TAG = "자재입고 확인";

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
    private TextView tvStatus, tvWorker, tvPartNo, tvLotNo, tvQty, tvPartCode, tvVendor, tvSupplier, tvDocumentNo;
    private EditText etWorker, etPartNo, etLotNo, etQty, etPartCode, etVendor;
    private Spinner spnSupplier, spnDocumentNo;
    private CheckBox checkBox;
    private ArrayList<String> documentNoList, supplierList; // 스피너의 네임 리스트
    private ArrayAdapter<String> documentNoListADT, supplierListADT; // 스피너에 사용되는 ArrayAdapter
    private int firstRun_spnDocumentNo, firstRun_spnSupplier = 0;

    private String inNo = "";
    private String customerCode = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mmng_warehousing_check);

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

        loadSupplier();

        spnSupplier.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_spnSupplier == 0) { //자동실행 방지
                    firstRun_spnSupplier += 1;
                } else {
                    loadDocument();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });

        spnDocumentNo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_spnDocumentNo == 0) { //자동실행 방지
                    firstRun_spnDocumentNo += 1;
                } else {
                    makingInNo();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });
    }

    private void initControl(){
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvStatus.setText("작업자를 입력(스캔)하여 주십시오.");

        tvWorker = (TextView) findViewById(R.id.tvWorker);
        tvPartCode = (TextView) findViewById(R.id.tvPartCode);
        tvVendor = (TextView) findViewById(R.id.tvVendor);
        tvPartNo = (TextView) findViewById(R.id.tvPartNo);
        tvLotNo = (TextView) findViewById(R.id.tvLotNo);
        tvQty = (TextView) findViewById(R.id.tvQty);
        tvSupplier = (TextView) findViewById(R.id.tvSupplier);
        tvDocumentNo = (TextView) findViewById(R.id.tvDocumentNo);
        //연결
        tvWorker.setOnClickListener(this::tvClick);
        tvPartCode.setOnClickListener(this::tvClick);
        tvVendor.setOnClickListener(this::tvClick);
        tvPartNo.setOnClickListener(this::tvClick);
        tvLotNo.setOnClickListener(this::tvClick);
        tvQty.setOnClickListener(this::tvClick);
        tvSupplier.setOnClickListener(this::tvClick);
        tvDocumentNo.setOnClickListener(this::tvClick);

        checkBox = (CheckBox) findViewById(R.id.checkBox);
        //연결
        checkBox.setOnClickListener(this::buttonClick);

        etWorker = (EditText) findViewById(R.id.etWorker);
        etPartNo = (EditText) findViewById(R.id.etPartNo);
        etLotNo = (EditText) findViewById(R.id.etLotNo);
        etQty = (EditText) findViewById(R.id.etQty);
        etPartCode = (EditText) findViewById(R.id.etPartCode);
        etVendor = (EditText) findViewById(R.id.etVendor);

        btnResultSave = (Button) findViewById(R.id.btnResultSave);
        //연결
        btnResultSave.setOnClickListener(this::buttonClick);

        //Spinner 관련 부분
        //데이터 준비
        documentNoList = new ArrayList<String>();
        supplierList = new ArrayList<String>();
        // 어댑터 생성
        documentNoListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, documentNoList);
        supplierListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, supplierList);

        // 어댑터 설정
        spnSupplier = (Spinner) findViewById(R.id.spnSupplier);
        spnSupplier.setAdapter(supplierListADT);
        spnDocumentNo = (Spinner) findViewById(R.id.spnDocumentNo);
        spnDocumentNo.setAdapter(documentNoListADT);
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
                break;
            case R.id.tvSupplier:
                loadSupplier();
                break;
            case R.id.tvDocumentNo:
                loadDocument();
                break;
        }
    }

    private void checkInsert(){
        if (etPartCode.getText().toString().length() == 0
                || etWorker.getText().toString().length() == 0){
            Toast.makeText(Warehousing_Check.this, "필수 입력항목을 모두 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        String insertText = "";
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = df.format(date);

        insertText = "insert into tb_mms_material_warehousing(";
        insertText += "mw_no, in_no, document_no, customer_code, part_code, part_vendor, part_no, part_lot_no";
        insertText += ", part_qty, barcode1,barcode2, barcode3, write_date, write_id";
        insertText += ") ";
        insertText += "select f_mms_new_mw_no(f_mms_new_in_no(date_format(now(), '%Y-%m-%d'), '" + spnDocumentNo.getSelectedItem().toString() + "'))";
        insertText += ", f_mms_new_in_no(date_format(now(), '%Y-%m-%d'), '" + spnDocumentNo.getSelectedItem().toString() + "')";
        insertText += ", '" + spnDocumentNo.getSelectedItem().toString() + "'";
        insertText += ", '" + customerCode + "'";
        insertText += ", '" + etPartCode.getText().toString() + "'";
        insertText += ", '" + etVendor.getText().toString() + "'";
        insertText += ", '" + etPartNo.getText().toString() + "'";
        insertText += ", '" + etLotNo.getText().toString() + "'";
        insertText += ", '" + etQty.getText().toString() + "'";
        insertText += ", null";
        insertText += ", null";
        insertText += ", null";
        insertText += ", '" + getTime + "'";
        insertText += ", 'PDA'";
        insertText += ";";

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

    private void makingInNo(){
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String getTime = df.format(date);
        GetData taskSave = new GetData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Warehouse_Check/load_new_in_no.php"
                , "Make_New_In_No"
                , getTime
                , spnDocumentNo.getSelectedItem().toString());
    }

    private void loadInformation(){
        GetData getData = new GetData();
        getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Warehouse_Check/load_information.php"
                , "Load_Information"
                , spnDocumentNo.getSelectedItem().toString()
                , etPartCode.getText().toString());
    }

    private void loadDocument(){
        GetData getData = new GetData();
        getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Warehouse_Check/load_document.php"
                , "Load_Document"
                , spnSupplier.getSelectedItem().toString());
    }

    private void loadSupplier(){
        GetData getData = new GetData();
        getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Warehouse_Check/load_supplier.php"
                , "Load_Supplier");
    }

    private void loadSamePartsCheck(){
        GetData getData = new GetData();
        getData.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMNG/Warehouse_Check/load_same_parts_check.php"
                , "Load_Same_Parts_check"
                , spnDocumentNo.getSelectedItem().toString()
                , etPartCode.getText().toString()
                , etLotNo.getText().toString());
    }

    private void controlReset(){
        etPartCode.setText("");
        etPartNo.setText("");
        etLotNo.setText("");
        etVendor.setText("");
        etQty.setText("");
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
                            tvStatus.setText("공급사를 선택하여 주십시오.");
                            return;
                        }

                        if (spnDocumentNo.getSelectedItem().toString().equals("") ||
                                spnDocumentNo.getSelectedItem().toString().equals("선택")){
                            tvStatus.setText("문서번호를 먼저 선택하여 주십시오.");
                            Toast.makeText(Warehousing_Check.this
                                    , "문서번호를 먼저 선택하여 주십시오."
                                    , Toast.LENGTH_SHORT).show();
                        } else {
                            if (inNo.equals("")){
                                makingInNo();
                                return;
                            }
                            String barcode[] = mDecodeResult.toString().split("!");
                            if (barcode.length < 5){
                                long[] pattern = {500,1000,500,1000};
                                vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                                tvStatus.setText("바코드를 확인 할 수 없습니다.");
                                Toast.makeText(Warehousing_Check.this, "바코드를 확인 할 수 없습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                etPartCode.setText(barcode[0]);
                                etPartNo.setText(barcode[1]);
                                etLotNo.setText(barcode[2]);
                                etQty.setText(barcode[3]);
                                etVendor.setText(barcode[4]);
                                //loadInformation();
                                loadSamePartsCheck();
                            }
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

            if (ActivityName.equals("mmng.Warehousing_Check"))
                progressDialog = ProgressDialog.show(Warehousing_Check.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("ver")) {
                postParameters = "";
            } else if (secondString.equals("Load_Supplier")) {
                postParameters = "";
            } else if (secondString.equals("Load_Document")) {
                postParameters = "Supplier=" + params[2];
            } else if (secondString.equals("Load_Information")) {
                postParameters = "DocumentNo=" + params[2];
                postParameters += "&PartCode=" + params[3];
            } else if (secondString.equals("Make_New_In_No")) {
                postParameters = "InDate=" + params[2];
                postParameters += "&DocumentNo=" + params[3];
            } else if (secondString.equals("Load_Same_Parts_check")) {
                postParameters = "DocumentNo=" + params[2];
                postParameters += "&PartCode=" + params[3];
                postParameters += "&PartLotNo=" + params[4];
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
                Toast.makeText(Warehousing_Check.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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

                if (header.equals("Supplier_List")) {
                    documentNoList.clear();
                    documentNoListADT.notifyDataSetChanged();
                    supplierList.clear();
                    supplierList.add("선택");
                    tvStatus.setText("공급사를 선택하여 주십시오.");

                    JSONArray jsonArray = jsonObject.getJSONArray("Supplier_List");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        supplierList.add(item.getString("Supplier"));
                    }
                    supplierListADT.notifyDataSetChanged();
                    controlReset();
                    spnSupplier.setSelection(0);
                } else if (header.equals("Supplier_List!")) {
                    tvStatus.setText("등록된 리스트가 없습니다.");
                    Toast.makeText(Warehousing_Check.this, "등록된 리스트가 없습니다.", Toast.LENGTH_SHORT).show();
                    controlReset();
                    supplierList.clear();
                    supplierListADT.notifyDataSetChanged();
                } else if (header.equals("Document_List")) {
                    documentNoList.clear();
                    documentNoList.add("선택");
                    tvStatus.setText("문서번호를 선택하여 주십시오.");

                    JSONArray jsonArray = jsonObject.getJSONArray("Document_List");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        documentNoList.add(item.getString("Document"));
                    }
                    documentNoListADT.notifyDataSetChanged();
                    controlReset();
                    spnDocumentNo.setSelection(0);
                } else if (header.equals("Document_List!")) {
                    tvStatus.setText("등록된 리스트가 없습니다.");
                    Toast.makeText(Warehousing_Check.this, "등록된 리스트가 없습니다.", Toast.LENGTH_SHORT).show();
                    controlReset();
                    documentNoList.clear();
                    documentNoListADT.notifyDataSetChanged();
                } else if (header.equals("Information")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Information");
                    JSONObject item = jsonArray.getJSONObject(0);
                    int documentQty = item.getInt("Part_Qty");
                    int inQty = item.getInt("In_Qty");

                    customerCode = item.getString("CustomerCode");

                    if (inQty >= documentQty){
                        long[] pattern = {500,1000,500,1000};
                        vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                        String showText = "누적 입고수량이 등록된\n문서 수량보다 크거나 같습니다.";
                        tvStatus.setText(showText);
                        Toast.makeText(Warehousing_Check.this, showText, Toast.LENGTH_SHORT).show();
                    } else {
                        if (checkBox.isChecked()){
                            checkInsert();
                        }
                    }
                } else if (header.equals("Information!")) {
                    tvStatus.setText("등록된 자재가 아닙니다");
                    Toast.makeText(Warehousing_Check.this, "등록된 자재가 아닙니다.", Toast.LENGTH_SHORT).show();
                    controlReset();
                } else if (header.equals("Make_In_No")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Make_In_No");
                    JSONObject item = jsonArray.getJSONObject(0);

                    inNo = item.getString("New_In_No");
                } else if (header.equals("Make_In_No!")) {
                    Toast.makeText(Warehousing_Check.this, "입고 순번을 만들지 못했습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("insert")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("insert");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(Warehousing_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                        long[] pattern = {500,1000,500,1000};
                        vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                        return;
                    }

                    controlReset();
                    tvStatus.setText("저장완료.\n다음 자재를 스캔하여 주십시오.");
                    Toast.makeText(Warehousing_Check.this, "저장완료.\n다음 자재를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("Load_Same_Parts_check")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("Load_Same_Parts_check");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("SamePart").equals("Exist")) {
                        loadInformation();
                    } else {
                        long[] pattern = {500,1000,500,1000};
                        vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                        tvStatus.setText("중복된 Lot No.입니다.");
                        Toast.makeText(Warehousing_Check.this, "중복된 Lot No.입니다.", Toast.LENGTH_SHORT).show();
                        controlReset();
                    }
                } else if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:"+ BuildConfig.VERSION_NAME)){
                        appVerAlarm();
                    }
                } else {
                    Toast.makeText(Warehousing_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
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