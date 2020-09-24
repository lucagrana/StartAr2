package com.example.startar2;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.example.startar2.helpers.TapHelper;
import com.example.startar2.rendering.BackgroundRenderer;
import com.example.startar2.rendering.ObjectRenderer;
import com.example.startar2.rendering.PlaneRenderer;
import com.example.startar2.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;
import uk.co.appoly.arcorelocation.utils.Utils2D;

public class ArNav extends AppCompatActivity implements GLSurfaceView.Renderer {

    private static final String TAG = ArNav.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;
    private AugmentedImageDatabase imageDatabase;
    private Session mSession;
    private GestureDetector mGestureDetector;
    private Snackbar mMessageSnackbar;
    private DisplayRotationHelper mDisplayRotationHelper;
    private Frame frame = null;
    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    int t=0;
    private final PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private final PointCloudRenderer mPointCloud = new PointCloudRenderer();
    private TapHelper tapHelper;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    //private final ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private final ArrayList<Anchor> mAnchors = new ArrayList<>();

    private LocationScene locationScene;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_nav);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        tapHelper = new TapHelper(/*context=*/ this);
        mSurfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Exception exception = null;
        String message = null;
        try {
            mSession = new Session(/* context= */ this);
        } catch (UnavailableArcoreNotInstalledException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            showSnackbarMessage(message, true);
            Log.e(TAG, "Exception creating session", exception);
            return;
        }

        // Create default config and check if supported.
        Config config = new Config(mSession);
        setUpDataBase(config, mSession);
        if (!mSession.isSupported(config)) {
            showSnackbarMessage("This device does not support AR", true);
        }
        mSession.configure(config);


        // Set up our location scene
        locationScene = new LocationScene(this, this, mSession);

        for (int i = 0; i<MyModel.getLatitudini().size();i++) {
            locationScene.mLocationMarkers.add(
                    new LocationMarker(
                            MyModel.getLongitudini().get(i),
                            MyModel.getLatitudini().get(i),

                            new ObjectRenderer("andy.obj", "andy.png")

                    )
            );
           /* locationScene.mLocationMarkers.add(
                    new LocationMarker(
                            MyModel.getLongitudini().get(i),
                            MyModel.getLatitudini().get(i),
                            new AnnotationRenderer("")));*/
        }


        // Correct heading with touching side of screen
        /*mSurfaceView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent e) {


                        if(e.getX() < mSurfaceView.getWidth() / 2) {
                            locationScene.setBearingAdjustment( locationScene.getBearingAdjustment() - 1 );
                        } else {
                            locationScene.setBearingAdjustment( locationScene.getBearingAdjustment() + 1 );
                        }
                        Toast.makeText(ArNav.this.findViewById(android.R.id.content).getContext(),
                                "Bearing adjustment: " + locationScene.getBearingAdjustment(), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
        );
        */

    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (ARLocationPermissionHelper.hasPermission(this)) {
            if(locationScene != null)
                locationScene.resume();
            if (mSession != null) {
                showLoadingMessage();
                // Note that order matters - see the note in onPause(), the reverse applies here.
                try {
                    mSession.resume();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }
            }
            mSurfaceView.onResume();
            mDisplayRotationHelper.onResume();
        } else {
            ARLocationPermissionHelper.requestPermission(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationScene != null)
            locationScene.pause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mDisplayRotationHelper.onPause();
        mSurfaceView.onPause();
        if (mSession != null) {
            mSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

   /* private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }*/

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/ this);
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        // Prepare the other rendering objects.
        /*try {
            mVirtualObject.createOnGlThread(*//*context=*//*this, "andy.obj", "andy.png");
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualObjectShadow.createOnGlThread(*//*context=*//*this,
                "andy_shadow.obj", "andy_shadow.png");
            mVirtualObjectShadow.setBlendMode(BlendMode.Shadow);
            mVirtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }*/
        try {
            mPlaneRenderer.createOnGlThread(this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            frame = mSession.update();
            Camera camera = frame.getCamera();
            //RICONOSCO QRCODE
            Collection<AugmentedImage> images = frame.getUpdatedTrackables(AugmentedImage.class);
            for (AugmentedImage image:images) {
                if(image.getTrackingState() == TrackingState.TRACKING) {
                    for (int i =1; i<4; i++){
                        if(image.getName().equals("" + i)) {
                            //do something
                            if (t<10){
                                t++;
                            }
                            Pose pose = image.getCenterPose();
                            Pose camPose = frame.getCamera().getPose();
                            toast(pose,camPose, i);
                            //showSnackbarMessage("QRCode riconosciuto", false);
                        }
                    }

                }
            }

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = tapHelper.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                Log.i(TAG, "HITTEST: Got a tap and tracking");
                Utils2D.handleTap(this, locationScene, frame, tap);
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // Draw location markers
            locationScene.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            mPointCloud.update(pointCloud);
            mPointCloud.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mMessageSnackbar != null) {
                for (Plane plane : mSession.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes.
            mPlaneRenderer.drawPlanes(
                    mSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            // Visualize anchors created by touch.
            float scaleFactor = 1.0f;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

            }



        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }


    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        mMessageSnackbar = Snackbar.make(
                ArNav.this.findViewById(android.R.id.content),
                message, Snackbar.LENGTH_INDEFINITE);
        mMessageSnackbar.getView().setBackgroundColor(0xbf323232);

        if (finishOnDismiss) {
            mMessageSnackbar.setAction(
                    "Dismiss",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mMessageSnackbar.dismiss();
                        }
                    });
            mMessageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            finish();
                        }
                    });
        }
        mMessageSnackbar.show();
    }

    private void showLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSnackbarMessage("Searching for surfaces...", false);
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMessageSnackbar != null) {
                    mMessageSnackbar.dismiss();
                }
                mMessageSnackbar = null;
            }
        });
    }
    public void setUpDataBase(Config config, Session session) {
        imageDatabase = new AugmentedImageDatabase(session);
        Bitmap bitmap =  null;
        for(int i = 1; i<4; i++){
            try (InputStream inputStream = getAssets().open(i +".png")) {
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                Log.e("no immagine", "I/O exception loading augmented image bitmap.", e);
            }
            imageDatabase.addImage("" + i, bitmap,0.165f);
        }

        config.setAugmentedImageDatabase(imageDatabase);
        Toast.makeText(this,
                "db setted", Toast.LENGTH_LONG).show();
    }
    public void toast(Pose pose, Pose camPose, int i) {
        if(t==1) {
            double qrLon = 0.;
            double qrLat = 0.;
            int degree = 0;
            float initialBearing = 0f;
            Log.d("posetoast", pose.toString());
            Log.d("posetoast", pose.ty() + " " + pose.tz() + " " + pose.tx());
            double distance = Math.sqrt(Math.pow((pose.tx() - camPose.tx()), 2)/ 2.0 +
                    Math.pow((pose.ty() - camPose.ty()),2) / 2.0 +
                    Math.pow((pose.tz() - camPose.tz()),2) / 2.0);
            float azimuth = getAzimuth(pose, camPose);
            float pitch = getPitch(pose, camPose);
            for (int j = 0; j < MyModel.getQrId().size(); j++){
                if (MyModel.getQrId().get(j) == i) {
                    qrLat = MyModel.getQrLat().get(j);
                    qrLon = MyModel.getQrLon().get(j);
                    degree = MyModel.getQrOrientamento().get(j);
                }
            }
            /*if (degree + pitch > 360){
                pitch = degree + pitch - 360;
            } else {
                pitch = degree + pitch;
            }*/
            if (degree + azimuth > 360){
                initialBearing = degree + azimuth - 360;
            } else {
                initialBearing = degree + azimuth;
            }
            double[] coordinateCamera = travel( qrLat,  qrLon ,  initialBearing,  distance);
            Log.d("posetoastLat", coordinateCamera[0] + "");
            Log.d("posetoastLon", coordinateCamera[1] + "");
            LocationScene.updateAnchors(coordinateCamera, pitch, initialBearing);
        }
    }

    public float getAzimuth(Pose pose, Pose camPose) {

        float angle = (float) Math.toDegrees(Math.atan2(pose.qx()- camPose.qx(), pose.qy() - camPose.qy()));
        Log.d("posetoastAngle", angle + "");
        // the range of ± 90.0° must be corrected...
        return angle;
    }
    public float getPitch(Pose pose, Pose camPose) {
        float pitch = (float) Math.toDegrees((Math.atan2(Math.sqrt(Math.pow((pose.qz() - camPose.qz()),2) + Math.pow((pose.qx() - camPose.qx()),2)),(pose.qy() - camPose.qy())) + Math.PI));
        return pitch;
    }

    public double[] travel(double qrLat, double qrLon , double initialBearing, double distance) {
        double bR = Math.toRadians(initialBearing);
        distance = distance/1000;
        double lat1R = Math.toRadians(qrLat);
        double lon1R = Math.toRadians(qrLon);
        double dR = distance/6371.01; //Mettere diviso radius earth

        double a = Math.sin(dR) * Math.cos(lat1R);
        double lat2 = Math.asin(Math.sin(lat1R) * Math.cos(dR) + a * Math.cos(bR));
        double lon2 = lon1R
                + Math.atan2(Math.sin(bR) * a, Math.cos(dR) - Math.sin(lat1R) * Math.sin(lat2));
        double[] latlon = new double[2];
        latlon[0] = Math.toDegrees(lat2);
        latlon[1] = Math.toDegrees(lon2);
        return latlon;
    }
}
