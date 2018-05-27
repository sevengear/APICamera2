package com.example.preview_basico;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "PreviewBasico";
    private TextureView textureview;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Size dimensionesImagen;
    //* Thread adicional para ejecutar tareas que no bloqueen Int usuario.
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "En onCreate !!!!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureview = (TextureView) findViewById(R.id.textureView);
        assert textureview != null;
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { //open your camera here
            Log.i(TAG, "Abriendo camara desde onSurfaceTextureAvailable");
            abrirCamara();
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        Log.i(TAG, "Setting textureListener a textureview");
        textureview.setSurfaceTextureListener(textureListener);
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void abrirCamara() {
        Log.i(TAG, "En abrir Camara");
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            mCameraId = manager.getCameraIdList()[0]; //La primera cámara
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            Size tamanos[] = map.getOutputSizes(SurfaceTexture.class);
            for (Size tam : tamanos) dimensionesImagen = tam;
            Log.i(TAG, "Dimensiones Imagen =" + String.valueOf(dimensionesImagen));
            manager.openCamera(mCameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            crearPreviewCamara();
        }

        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private void crearPreviewCamara() {
        try {
            SurfaceTexture texture = textureview.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(dimensionesImagen.getWidth(),
                    dimensionesImagen.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            CameraCaptureSession.StateCallback statecallback =
                    new CameraCaptureSession.StateCallback() {

                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            Log.i(TAG, "Sesión de captura configurada para preview");
                            //The camera is closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            // Cuando la sesion este lista empezamos a visualizer imags.
                            mCaptureSession = cameraCaptureSession;
                            comenzarPreview();
                        }

                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(MainActivity.this, "Configuration change failed", Toast.LENGTH_SHORT).show();
                        }
                    };
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    statecallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void comenzarPreview() {
        if (null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                    null, mBackgroundHandler);
            Log.v(TAG, "*****setRepeatingRequest");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
