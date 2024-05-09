package kr.or.yujin.mmps.App.Activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothClass;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
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

import androidx.appcompat.app.AppCompatActivity;
import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;
import kr.or.yujin.mmps.App.Class.BarcodeSplit;
import kr.or.yujin.mmps.App.Class.StringUtil;
import kr.or.yujin.mmps.BuildConfig;
import kr.or.yujin.mmps.R;

public class DeviceDataActivity extends AppCompatActivity {

    // Scanner Setting
    private static final String TAG = "All Parts Check_New";

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

    private ArrayList<String> customerNameList, customerCodeList; // 스피너의 네임 리스트
    private ArrayAdapter<String> customerNameListADT; // 스피너에 사용되는 ArrayAdapter
    private ArrayList<String> modelNameList, modelCodeList; // 스피너의 네임 리스트
    private ArrayAdapter<String> modelNameListADT; // 스피너에 사용되는 ArrayAdapter
    private ArrayList<String> factoryNameList, factoryCodeList; // 스피너의 네임 리스트
    private ArrayAdapter<String> factoryNameListADT; // 스피너에 사용되는 ArrayAdapter
    private ArrayList<String> workLineNameList; // 스피너의 네임 리스트
    private ArrayAdapter<String> workLineNameListADT; // 스피너에 사용되는 ArrayAdapter
    private ArrayList<String> makerNameList; // 스피너의 네임 리스트
    private ArrayAdapter<String> makerNameListADT; // 스피너에 사용되는 ArrayAdapter


    //Server 접속주소
    private static String server_ip = MainActivity.server_ip;
    private static int server_port = MainActivity.server_port;

    private Spinner spnCustomer, spnModel, spnFactory, spnWorkLine, spnWorkSide, spnMaker;

    private int firstRun_spnCustomer = 0;
    private int firstRun_spnModel = 0;
    private int firstRun_spnFactory = 0;

    private EditText etMachineNo, etFeederNo, etOrgMaker, etOrgPartNo;
    private TextView tvPartNo, tvPartNo2, tvInformation2;
    private Button btnFindFeeder, btnResultSave;
    private RadioButton rbAdd, rbReplace, rbNew;

    private String selModelCode, selDDCode, selDDMainNo;

    private Boolean saveOK = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_data);

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

        server_ip = MainActivity.server_ip;
        server_port = MainActivity.server_port;

        etMachineNo = (EditText) findViewById(R.id.etMachineNo);
        etFeederNo = (EditText) findViewById(R.id.etFeederNo);
        etOrgMaker = (EditText) findViewById(R.id.etOrgMaker);
        etOrgPartNo = (EditText) findViewById(R.id.etOrgPartNo);

        tvPartNo = (TextView) findViewById(R.id.tvPartNo);
        tvPartNo2 = (TextView) findViewById(R.id.tvPartNo2);
        tvInformation2 = (TextView) findViewById(R.id.tvInformation2);

        btnFindFeeder = (Button) findViewById(R.id.btnFindFeeder);
        btnResultSave = (Button) findViewById(R.id.btnResultSave);

        rbAdd = (RadioButton) findViewById(R.id. rbAdd);
        rbReplace = (RadioButton) findViewById(R.id.rbReplace);
        rbNew = (RadioButton) findViewById(R.id.rbNew);

        //데이터 준비
        customerNameList = new ArrayList<String>();
        customerCodeList = new ArrayList<String>();
        modelNameList = new ArrayList<String>();
        modelCodeList = new ArrayList<String>();
        factoryNameList = new ArrayList<String>();
        factoryCodeList = new ArrayList<String>();
        workLineNameList = new ArrayList<String>();
        makerNameList = new ArrayList<String>();

        // 어댑터 생성
        customerNameListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, customerNameList);
        modelNameListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, modelNameList);
        factoryNameListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, factoryNameList);
        workLineNameListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, workLineNameList);
        makerNameListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, makerNameList);

        // 어댑터 설정
        spnCustomer = (Spinner) findViewById(R.id.spnCustomer);
        spnCustomer.setAdapter(customerNameListADT);
        spnModel = (Spinner) findViewById(R.id.spnModel);
        spnModel.setAdapter(modelNameListADT);
        spnFactory = (Spinner) findViewById(R.id.spnFactory);
        spnFactory.setAdapter(factoryNameListADT);
        spnWorkLine = (Spinner) findViewById(R.id.spnWorkLine);
        spnWorkLine.setAdapter(workLineNameListADT);
        spnMaker = (Spinner) findViewById(R.id.spnMaker);
        spnMaker.setAdapter(makerNameListADT);

        getData taskCustomerFind = new getData();
        taskCustomerFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/customerlist.php", "customer Find");

        spnModel.setEnabled(false);

        getData taskFactoryFind = new getData();
        taskFactoryFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/factorylist.php", "factory Find");

        spnWorkLine.setEnabled(false);

        spnWorkSide = (Spinner) findViewById(R.id.spnWorkSide);
        ArrayAdapter workSideADT = ArrayAdapter.createFromResource(this, R.array.work_side, android.R.layout.simple_spinner_dropdown_item);
        spnWorkSide.setAdapter(workSideADT);

        // 현재 프로세스 변경으로 메이커 리스트를 먼저 불러 올 필요가 없다.
        //getData taskMakerFind = new getData();
        //taskMakerFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/makerlist.php", "maker Find");

        spnCustomer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_spnCustomer == 0) { //자동실행 방지
                    firstRun_spnCustomer += 1;
                } else {
                    //Toast.makeText(DeviceDataActivity.this,"선택 된 고객사 코드 : " + customerCodeList.get(position),Toast.LENGTH_SHORT).show();
                    //Log.d(activityTag, "선택 된 고객사 코드 : " + customerCodeList.get(position));
                    getData taskModelFind = new getData();
                    taskModelFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/modellist.php", "model Find", customerCodeList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });

        spnModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_spnModel == 0) { //자동실행 방지
                    firstRun_spnModel += 1;
                } else {
                    //Toast.makeText(DeviceDataActivity.this,"선택 된 모델 코드 : " + modelCodeList.get(position),Toast.LENGTH_SHORT).show();
                    selModelCode = modelCodeList.get((position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });

        spnFactory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_spnFactory == 0) { //자동실행 방지
                    firstRun_spnFactory += 1;
                } else {
                    //Log.d(activityTag, "선택 된 공장 코드 : " + factoryCodeList.get(position));
                    getData taskLineFind = new getData();
                    taskLineFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/worklinelist.php", "line Find", factoryCodeList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });

        // 테스트용
        tvInformation2.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                getData taskLineFind = new getData();
                taskLineFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/BarcodeSplit/barcodesplit.php",
                        "BarcodeSplit",
                        "POCONS",
                        "PTC4944HEX2038V0314K001700");
                 */
            }
        }));

        btnFindFeeder.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputMethodManager imm = (InputMethodManager)getSystemService((Context.INPUT_METHOD_SERVICE));
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                etMachineNo.clearFocus();
                etFeederNo.clearFocus();

                try{
                    if (spnModel.getSelectedItem().toString().length() == 0) {
                        Toast.makeText(DeviceDataActivity.this,"Model을 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }catch (Exception e){
                    Toast.makeText(DeviceDataActivity.this,"Model을 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                    return;
                }

                if (spnFactory.getSelectedItem().toString().length() == 0) {
                    Toast.makeText(DeviceDataActivity.this,"공장을 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                    return;
                }

                try{
                    if (spnWorkLine.getSelectedItem().toString().length() == 0) {
                        Toast.makeText(DeviceDataActivity.this,"작업라인을 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }catch (Exception e){
                    Toast.makeText(DeviceDataActivity.this,"작업라인을 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                    return;
                }

                if (spnWorkSide.getSelectedItem().toString().length() == 0) {
                    Toast.makeText(DeviceDataActivity.this,"작업면을 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                    return;
                }

                if (etMachineNo.getText().toString().length() == 0) {
                    Toast.makeText(DeviceDataActivity.this,"Machine No.를 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                    return;
                }

                if (etFeederNo.getText().toString().length() == 0) {
                    Toast.makeText(DeviceDataActivity.this,"Feeder No.를 먼저 입력하여 주십시오.",Toast.LENGTH_SHORT).show();
                    return;
                }

                /*
                getData taskMakerFind = new getData();
                taskMakerFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/makerlist.php"
                        , "maker Find"
                );
                 */

                getData taskLineFind = new getData();
                taskLineFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/feedercheck.php"
                        , "feederCheck"
                        , selModelCode
                        , spnFactory.getSelectedItem().toString()
                        , spnWorkLine.getSelectedItem().toString()
                        , spnWorkSide.getSelectedItem().toString()
                        , etMachineNo.getText().toString()
                        , etFeederNo.getText().toString()
                );
            }
        }));

        tvPartNo2.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvPartNo.setText("");
            }
        }));

        btnResultSave.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 고객사가 전유산업이라면 메인파트 넘버는 동일하지만(용량값으로 표기) 실제 다른 제조사의
                // 대치자재가 존재하므로 자재 제조사가 지정 되어 있어야 한다.
                /*
                if (spnCustomer.getSelectedItem().toString().equals("J산업") ||
                        spnCustomer.getSelectedItem().toString().equals("전유산업")) {
                    if (etOrgMaker.getText().toString().length() == 0) {
                        Toast.makeText(DeviceDataActivity.this, "PC에서 해당자재의 제조사를 먼저 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                 */
                if (saveOK == true && tvPartNo.getText().toString().length() != 0) {
                    String insertText = "";
                    if (rbAdd.isChecked()) { // 추가가 선택된 경우
                        insertText = "set @abcd = f_mmps_new_ds_code('" + spnCustomer.getSelectedItem().toString() + "');";
                        /*
                        // 모델 상관없이 메인파트에 서브파트를 다 사용하려면 해당 고객사를 등록하여야 한다.
                        if (spnCustomer.getSelectedItem().toString().equals("J산업") ||
                                spnCustomer.getSelectedItem().toString().equals("전유산업") ||
                                spnCustomer.getSelectedItem().toString().equals("Mangoslab") ||
                                spnCustomer.getSelectedItem().toString().equals("L-Tech") ||
                                spnCustomer.getSelectedItem().toString().equals("TopRun") ||
                                spnCustomer.getSelectedItem().toString().equals("덕일전자") ||
                                spnCustomer.getSelectedItem().toString().contains("CI Digital") ||
                                spnCustomer.getSelectedItem().toString().equals("allRadio")){
                            insertText += "Insert Into tb_device_data_SUB2(DS_CODE, CUSTOMER, MAIN_PART_MAKER, MAIN_PART_NO, PART_MAKER, PART_NO, DS_NOTE) values";
                            insertText += "(ifnull(@abcd, 'DS00000001')";
                            insertText += ",'" + spnCustomer.getSelectedItem().toString() + "'";
                            insertText += ",'" + etOrgMaker.getText().toString() + "'";
                            insertText += ",'" + etOrgPartNo.getText().toString() + "'";
                            insertText += ",'" + spnMaker.getSelectedItem().toString() + "'";
                            insertText += ",'" + tvPartNo.getText() + "'";
                            insertText += ",'');";
                        } else {
                            insertText += "Insert Into tb_device_data_SUB(DD_CODE, DS_CODE, PART_MAKER, PART_NO, DS_NOTE)";
                            insertText += " select DD_CODE";
                            insertText += ", ifnull(@abcd, 'DS00000001')"; // 신규 DS Code
                            insertText += ",'" + spnMaker.getSelectedItem().toString() + "'";
                            insertText += ",'" + tvPartNo.getText() + "'";
                            insertText += ",''";
                            insertText += " from tb_device_data";
                            insertText += " where DD_MAIN_NO = '" + selDDMainNo + "'";
                            insertText += " and PART_NO = '" + etOrgPartNo.getText().toString() + "';";
                        }
                        */
                        insertText += "Insert Into tb_mmps_device_data_sub2(ds_code, customer, main_part_maker, main_part_no, part_maker, part_no, ds_note) values";
                        insertText += "(ifnull(@abcd, 'DS00000001')";
                        insertText += ",'" + spnCustomer.getSelectedItem().toString() + "'";
                        insertText += ",'" + etOrgMaker.getText().toString() + "'";
                        insertText += ",'" + etOrgPartNo.getText().toString() + "'";
                        insertText += ",'" + spnMaker.getSelectedItem().toString() + "'";
                        insertText += ",'" + tvPartNo.getText() + "'";
                        insertText += ",'');";
                    } else if (rbNew.isChecked()) { // 신규가 선택된 경우
                        long now = System.currentTimeMillis();
                        Date date = new Date(now);
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String getTime = df.format(date);
                        insertText = "insert into tb_device_data(dd_code, model_code, factory_name, line_name, work_side";
                        insertText += ", machine_no, feeder_no, part_maker, part_no, part_specification, dd_note, write_id, write_date, dd_main_no) values";
                        insertText += "((select f_mmps_new_dd_code())";
                        insertText += ",'" + selModelCode + "'";
                        insertText += ",'" + spnFactory.getSelectedItem().toString() + "'";
                        insertText += ",'" + spnWorkLine.getSelectedItem().toString() + "'";
                        insertText += ",'" + spnWorkSide.getSelectedItem().toString() + "'";
                        insertText += ",'" + etMachineNo.getText().toString() + "'";
                        insertText += ",'" + etFeederNo.getText().toString() + "'";
                        insertText += ",'" + spnMaker.getSelectedItem().toString() + "'";
                        insertText += ",'" + tvPartNo.getText().toString() + "'";
                        insertText += ",(select f_mmps_load_spec('" + tvPartNo.getText().toString() + "'))";
                        insertText += ",''";
                        insertText += ",'APP_USER'";
                        insertText += ",'" + getTime + "'";
                        insertText += ",'" + selDDMainNo + "');"; //이게 없는게 있다 다시 확인해라
                    }
                    //서버로 전송한다.
                    Log.d(TAG, "Insert Text : " + insertText);
                    getData taskSave = new getData();
                    taskSave.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/ddinsert.php"
                            , "ddInsert"
                            , insertText);
                } else {
                    Toast.makeText(DeviceDataActivity.this,"모든 항목이 입력되지 않았습니다.",Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }));
    }

    public class ScanResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScanner != null) {
                try {
                    if (ScanConst.INTENT_USERMSG.equals(intent.getAction())) {
                        mScanner.aDecodeGetResult(mDecodeResult.recycle());
                        Log.d(TAG, "php Split Start and Barcode Scan Result : " + mDecodeResult.toString());

                        // Barcode 분리작업 시작

                        // 자재 제조사별 바코드 해독의 편의를 위해 PHP 서버에서 해독을 실시하도록 변경
                        // 21-01-22 박시현
                        String replaceBarcode = StringUtil.barcodeChange(mDecodeResult.toString());
                        getData php_barcodeSplit = new getData();
                        php_barcodeSplit.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/BarcodeSplit/barcodesplit.php",
                                "BarcodeSplit",
                                spnMaker.getSelectedItem().toString(),
                                replaceBarcode);

                        Log.d("Barcode Split Send", "Send Message : " + spnMaker.getSelectedItem().toString() + " , " + mDecodeResult.toString().replace("&", "//"));
                        // Barcode 분리작업 완료

                        /*
                        String barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), spnMaker.getSelectedItem().toString());
                        if (barcodeReturn.indexOf("!@") > 0){ // 분리가 된 데이터가 들어 온것이라면
                            try {
                                String[] partInfo = barcodeReturn.split("!@");
                                tvPartNo.setText(partInfo[0].replace("P:", ""));
                            } catch (Exception e) {
                                Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                            }
                        } else { // 원래 내용이 다시 돌아 온 것이라면
                            tvPartNo.setText(mDecodeResult.toString());
                        }
                        */
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
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

            if (ActivityName.equals("App.Activity.DeviceDataActivity"))
                progressDialog = ProgressDialog.show(DeviceDataActivity.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("customer Find")) {
                postParameters = ""; //
            } else if (secondString.equals("model Find")) {
                postParameters = "customerCode=" + params[2];
            } else if (secondString.equals("factory Find")) {
                postParameters = ""; //
            } else if (secondString.equals("line Find")) {
                postParameters = "factoryCode=" + params[2];
            } else if (secondString.equals("maker Find")) {
                postParameters = "";
            } else if (secondString.equals("feederCheck")) {
                postParameters = "modelCode=" + params[2];
                postParameters += "&factoryName=" + params[3];
                postParameters += "&lineName=" + params[4];
                postParameters += "&workSide=" + params[5];
                postParameters += "&machineNo=" + params[6];
                postParameters += "&feederNo=" + params[7];
            } else if (secondString.equals("ddInsert")){
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("ddCodeCheck")) {
                postParameters = "modelCode=" + params[2];
                postParameters += "&factoryName=" + params[3];
                postParameters += "&lineName=" + params[4];
                postParameters += "&workSide=" + params[5];
            } else if (secondString.equals("BarcodeSplit")) {
                postParameters = "barcode=" + params[3];
                postParameters += "&maker=" + params[2];
            } else if (secondString.equals("ver")) {
                postParameters = "";
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

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(DeviceDataActivity.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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

                if (header.equals("customerList")) {
                    customerNameList.clear();
                    customerCodeList.clear();

                    customerNameList.add("고객사를 선택 하십시오.");
                    customerCodeList.add("선택");

                    JSONArray jsonArray = jsonObject.getJSONArray("customerList");
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject item = jsonArray.getJSONObject(i);
                        customerNameList.add(item.getString("CUSTOMER_NAME"));
                        customerCodeList.add(item.getString("CUSTOMER_CODE"));
                    }
                    customerNameListADT.notifyDataSetChanged();
                } else if (header.equals("customerList!")) {
                    Toast.makeText(DeviceDataActivity.this, "등록된 고객사가 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("modelList")) {
                    modelNameList.clear();
                    modelCodeList.clear();

                    modelNameList.add("모델을 선택 하십시오.");
                    modelCodeList.add("선택");

                    JSONArray jsonArray = jsonObject.getJSONArray("modelList");
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject item = jsonArray.getJSONObject(i);
                        modelNameList.add(item.getString("MODEL_NAME"));
                        modelCodeList.add(item.getString("MODEL_CODE"));
                    }
                    modelNameListADT.notifyDataSetChanged();
                    spnModel.setEnabled(true);
                } else if (header.equals("modelList!")) {
                    Toast.makeText(DeviceDataActivity.this, "등록된 모델이 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("factoryList")) {
                    factoryNameList.clear();
                    factoryCodeList.clear();

                    factoryNameList.add("공장을 선택 하십시오.");
                    factoryCodeList.add("선택");

                    JSONArray jsonArray = jsonObject.getJSONArray("factoryList");
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject item = jsonArray.getJSONObject(i);
                        factoryNameList.add(item.getString("SUB_CODE_NAME"));
                        factoryCodeList.add(item.getString("SUB_CODE"));
                    }
                    factoryNameListADT.notifyDataSetChanged();
                } else if (header.equals("factoryList!")) {
                    Toast.makeText(DeviceDataActivity.this, "등록된 공장이 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("workLineList")) {
                    workLineNameList.clear();

                    workLineNameList.add("라인을 선택 하십시오.");

                    JSONArray jsonArray = jsonObject.getJSONArray("workLineList");
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject item = jsonArray.getJSONObject(i);
                        workLineNameList.add(item.getString("LAST_CODE_NAME"));
                    }
                    workLineNameListADT.notifyDataSetChanged();
                    spnWorkLine.setEnabled(true);
                } else if (header.equals("workLineList!")) {
                    Toast.makeText(DeviceDataActivity.this, "등록된 라인이 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("makerList")) {
                    makerNameList.clear();

                    makerNameList.add("제조사를 선택 하십시오.");

                    JSONArray jsonArray = jsonObject.getJSONArray("makerList");
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject item = jsonArray.getJSONObject(i);
                        makerNameList.add(item.getString("SUB_CODE_NAME"));
                    }
                    makerNameListADT.notifyDataSetChanged();
                } else if (header.equals("makerList!")) {
                    Toast.makeText(DeviceDataActivity.this, "등록된 제조사가 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("feederCheck")) { // 추가 등록
                    JSONArray jsonArray = jsonObject.getJSONArray("feederCheck");
                    JSONObject item = jsonArray.getJSONObject(0);

                    rbAdd.setChecked(true);
                    rbAdd.setEnabled(true);
                    rbReplace.setEnabled(false);
                    rbNew.setEnabled(false);
                    selDDCode = item.getString("DD_CODE");
                    etOrgMaker.setText(item.getString("PART_MAKER"));
                    etOrgPartNo.setText(item.getString("PART_NO"));
                    selDDMainNo = item.getString("DD_MAIN_NO");
                    saveOK = true;

                    Toast.makeText(DeviceDataActivity.this, "조회완료. 추가 등록으로 진행됩니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("feederCheck!")) { // 신규 등록
                    rbNew.setChecked(true);
                    rbAdd.setEnabled(false);
                    rbReplace.setEnabled(false);
                    rbNew.setEnabled(true);
                    selDDCode = "";
                    selDDMainNo = "";
                    etOrgMaker.setText("");
                    etOrgPartNo.setText("");
                    saveOK = true;

                    Toast.makeText(DeviceDataActivity.this, "등록된 정보가 없습니다. \n 신규 등록으로 진행됩니다.", Toast.LENGTH_SHORT).show();

                    getData taskLineFind = new getData(); // DD_MAIN_CODE가 있는지 확인
                    taskLineFind.execute("http://" + server_ip + ":" + server_port + "/MMPS_V2/DeviceData/ddcodecheck.php"
                            , "ddCodeCheck"
                            , selModelCode
                            , spnFactory.getSelectedItem().toString()
                            , spnWorkLine.getSelectedItem().toString()
                            , spnWorkSide.getSelectedItem().toString()
                    );
                } else if (header.equals("insert")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("insert");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(DeviceDataActivity.this, mJsonString, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        selDDCode = "";
                        selDDMainNo = "";
                        etOrgMaker.setText("");
                        etOrgPartNo.setText("");
                        // etFeederNo.setText(""); //Machine No 와 Feeder No를 초기화 하니까 불편해서.. 필요하면 없애라..
                        // etMachineNo.setText("");
                        tvPartNo.setText("");
                        saveOK = false;
                        // spnMaker.setSelection(0);

                        Toast.makeText(DeviceDataActivity.this, "정상 등록 되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("ddCodeCheck")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("ddCodeCheck");
                    JSONObject item = jsonArray.getJSONObject(0);

                    rbNew.setChecked(true);
                    rbAdd.setEnabled(false);
                    rbReplace.setEnabled(false);
                    rbNew.setEnabled(true);
                    selDDMainNo = item.getString("DD_MAIN_NO");
                    saveOK = true;
                    //Toast.makeText(DeviceDataActivity_New.this, "현재 Main DD Code : " + selDDMainNo, Toast.LENGTH_SHORT).show();
                } else if (header.equals("ddCodeCheck!")) {
                    long now = System.currentTimeMillis();
                    Date date = new Date(now);
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                    String getTime = df.format(date);
                    selDDMainNo = "DD-" + getTime;
                    saveOK = true;
                    //Toast.makeText(DeviceDataActivity.this, "현재 Main DD Code : " + selDDMainNo, Toast.LENGTH_SHORT).show();
                } else if (header.equals("BarcodeSplitResult")) {
                    JSONArray jsonArray = jsonObject.getJSONArray(("BarcodeSplitResult"));
                    JSONObject item = jsonArray.getJSONObject(0);

                    try {
                        String[] partInfo = item.getString("returnStr").split("!@");
                        if (partInfo.length < 3) {
                            Toast.makeText(DeviceDataActivity.this, "바코드 해독에 실패 하였습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            String nowPartNo = partInfo[0].replace("P:", "");
                            String nowORG = partInfo[3].replace("ORG:", "");
                            if (!nowORG.equals("")) {
                                tvPartNo.setText(nowORG);
                            } else {
                                tvPartNo.setText(nowPartNo);
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "PHP에서 돌아온 바코드 정보를 확인 중 오류 발생", e);
                    }
                } else if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:"+ BuildConfig.VERSION_NAME)){
                        appVerAlarm();
                    }
                } else {
                    Toast.makeText(DeviceDataActivity.this, mJsonString, Toast.LENGTH_SHORT).show();
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

    private void verCheck(){
        getData task_VerLoad = new getData();
        task_VerLoad.execute( "http://" + server_ip + ":" + server_port + "/MMPS_V2/app_ver_new.php", "ver");
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
                        android.os.Process.killProcess(android.os.Process.myPid());
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
