package kr.or.yujin.mmps.App.Activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import androidx.appcompat.app.AppCompatActivity;
import kr.or.yujin.mmps.R;

public class ScannerAllActivity extends AppCompatActivity implements  DecoratedBarcodeView.TorchListener {

    private CaptureManager manager;
    private boolean isFlashOn = false;// 플래시가 켜져 있는지

    private Button btFlash;
    private DecoratedBarcodeView barcodeView;

    private TextView textView7;
    public static String showText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_all);

        barcodeView = findViewById(R.id.db_qr);
        textView7 = (TextView) findViewById(R.id.textView7);
        textView7.setText(showText + "\n(Barcode : ALL Type)");

        manager = new CaptureManager(this,barcodeView);
        manager.initializeFromIntent(getIntent(),savedInstanceState);
        manager.decode();

        /*
        btFlash = findViewById(R.id.bt_flash);
        btFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Scan Activity", "Flash On/Off : " + isFlashOn);
                if(isFlashOn){
                    barcodeView.setTorchOff();
                }else{
                    barcodeView.setTorchOn();
                }
            }
        });
        */
    }

    @Override
    public void onTorchOn() {
        //btFlash.setText("플래시끄기");
        //isFlashOn = true;
    }

    @Override
    public void onTorchOff() {
        //btFlash.setText("플래시켜기");
        //isFlashOn = false;
    }
    @Override
    protected void onResume() {
        super.onResume();
        manager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        manager.onSaveInstanceState(outState);
    }

}
