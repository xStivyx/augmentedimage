package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AugmentedImageActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private Frame frame;
    private ModelRenderable pikachu, eevee, pokeball;
    private ImageView fitToScanView;
    private Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    private Collection<AugmentedImage> updatedAugmentedImages;
    private List<AnchorNode> anchorNodeList = new ArrayList<>();
    private AnchorNode aNode;
    private Anchor anchor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.button);
        Button cameraBtn = findViewById(R.id.cameraButton);

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.v("AugmentedImage size", "" + updatedAugmentedImages.size());

                for (int i = 0; i < anchorNodeList.size(); i++) {
                    removeNode(anchorNodeList.get(i));
                }
            }
        });

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Toast.makeText(AugmentedImageActivity.this, "camera button clicked", Toast.LENGTH_SHORT).show();
            }
        });

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);


        mInit();


        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (pikachu == null || eevee == null || pokeball == null) {
                        return;
                    }
                });
    }

    private void removeNode(AnchorNode node) {
        if (node != null) {
            arFragment.getArSceneView().getScene().removeChild(node);
            node.getAnchor().detach();
//            node.setParent(null);
//            node = null;
            Toast.makeText(getApplicationContext(), "노드 삭제 완료", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), "노드가 없습니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }

    private void onUpdateFrame(FrameTime frameTime) {
        frame = arFragment.getArSceneView().getArFrame();

        if (frame == null) {
            return;
        }

        updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);

        for (AugmentedImage augmentedImage : updatedAugmentedImages) {

            switch (augmentedImage.getTrackingState()) {

                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    //String text = "Detected Image " + augmentedImage.getIndex();
                    //SnackbarHelper.getInstance().showMessage(this, text);
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    fitToScanView.setVisibility(View.GONE);
                    // Create a new anchor for newly found images.

                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        if (augmentedImage.getAnchors().size() == 0) {
                            anchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
                            aNode = new AnchorNode(anchor);
                            aNode.setParent(arFragment.getArSceneView().getScene());
                            anchorNodeList.add(aNode);

                            augmentedImageMap.put(augmentedImage, new AugmentedImageNode(this));

                            TransformableNode tNode = new TransformableNode(arFragment.getTransformationSystem());
                            tNode.getScaleController().setMinScale(0.01f);
                            tNode.getScaleController().setMaxScale(0.05f);
                            tNode.getTranslationController().setEnabled(false);
                            tNode.setParent(aNode);

                            switch (augmentedImage.getIndex()) {
                                case 0:
                                    tNode.setRenderable(pikachu);
                                    break;

                                case 1:
                                    tNode.setRenderable(eevee);
                                    break;

                                case 2:
                                    tNode.setRenderable(pokeball);
                                    break;
                            }

                            tNode.select();
                        }
                    }

                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    break;
            }
        }
    }

    private void mInit() {
        //model 1
        ModelRenderable.builder()
                .setSource(this, R.raw.pikachu)
                .build()
                .thenAccept(renderable -> pikachu = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        //model 2
        ModelRenderable.builder()
                .setSource(this, R.raw.eevee)
                .build()
                .thenAccept(renderable -> eevee = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        //model 3
        ModelRenderable.builder()
                .setSource(this, R.raw.pokeball)
                .build()
                .thenAccept(renderable -> pokeball = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }
}