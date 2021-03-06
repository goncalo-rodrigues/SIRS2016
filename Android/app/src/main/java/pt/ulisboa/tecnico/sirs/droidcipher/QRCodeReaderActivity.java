package pt.ulisboa.tecnico.sirs.droidcipher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.List;

public class QRCodeReaderActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String LOG_TAG = QRCodeReaderActivity.class.getSimpleName();
    private SurfaceView cameraView;
    private CameraSource cameraSource;
    private BarcodeDetector detector;
    private boolean cameraStarted = false;
    public static final String RESULT = "result";

    private Button closeBt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_reader);
        cameraView = (SurfaceView) findViewById(R.id.cameraView);
        closeBt = (Button) findViewById(R.id.new_connection_close_bt);

        closeBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                setResult(RESULT_CANCELED, data);
                finish();
            }
        });
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Requesting camera permission.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0);
        } else {
            cameraView.getHolder().addCallback(this);
        }
    }

    @Override
    protected void onResume() {


        super.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public synchronized void tryStartCamera() {
        if (!cameraStarted) {
            try {
                cameraSource.start(cameraView.getHolder());
                cameraStarted = true;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    public Camera.Size getBestSize() {
        try {
            Camera.Size bestSize = null;
            double bestRatio = 10;
            List<Camera.Size> result = Camera.open().getParameters().getSupportedPreviewSizes();
            for (Camera.Size size : result) {
                double ratio = Math.max((double)size.height / size.width, (double)size.width/size.height);
                if (ratio < bestRatio) {
                    bestRatio = ratio;
                    bestSize = size;
                }
            }
            return bestSize;
        } catch (Exception e) {
            return null;
        }

    }

    public void init() {
        detector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        Camera.Size size = getBestSize();
        int width = 480;
        int height = 480;
        if (size != null) {
            width = size.width;
            height = size.height;
        }
        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(width,height)
                .build();

        tryStartCamera();

        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    Intent data = new Intent();
                    data.putExtra(RESULT, barcodes.valueAt(0).rawValue);
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    init();

                } else {
                    //Toast.makeText(this, "The camera is needed to read the QR Code", Toast.LENGTH_LONG);
                    Intent data = new Intent();
                    setResult(RESULT_CANCELED, data);
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        init();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        cameraSource.stop();
        cameraStarted = false;
    }
}
