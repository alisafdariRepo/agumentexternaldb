package ir.azimuthnegar.myapplication

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.util.*

class MainActivity : AppCompatActivity() {

    private val MIN_OPENGL_VERSION = 3.0
    private lateinit var arFragment: ArFragment
    private lateinit var fitToScanView: ImageView
    // Augmented image and its center pose anchor
    private val augmentedImageMap = HashMap<AugmentedImage, AnchorNode>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkIsSupportedDeviceOrFinish())
            return


        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        fitToScanView = findViewById(R.id.hint_prompt)
        arFragment.arSceneView.scene.addOnUpdateListener { updateFrame() }

    }

    override fun onResume() {
        super.onResume()
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.visibility = View.VISIBLE
        }
    }

    private fun updateFrame() {
        val frame = arFragment.arSceneView.arFrame

        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.camera.trackingState != TrackingState.TRACKING) {
            return
        }

        //get back some trackables that we can use for anchors
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    Log.d("**Paused", augmentedImage.name + " " + augmentedImage.index)
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                }

                TrackingState.TRACKING -> {

                    fitToScanView.visibility = View.GONE
                    // Create an anchor for the image that is tracking
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        val node = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
                        augmentedImageMap[augmentedImage] = node
                        arFragment.arSceneView.scene.addChild(node)
                        addTextLabel(augmentedImage, node)
                        Log.d("**Tracking", augmentedImage.name + " " + augmentedImage.index)
                    }
                }

                TrackingState.STOPPED -> {
                    Log.d("**Stopped", augmentedImage.name + " " + augmentedImage.index)
                    augmentedImageMap.remove(augmentedImage)
                }


            }
        }
    }

    private fun addTextLabel(augmentedImage: AugmentedImage, anchorNode: AnchorNode) {
        ViewRenderable.builder().setView(this, R.layout.template_layout).build()
            .thenAccept { viewRenderable ->
                val noteText = Node()
                noteText.renderable = viewRenderable
                //scale the text to the size we need it.
                val localScale = Vector3()
                localScale.set(0.15f, 0.15f, 0.15f)
                noteText.localScale = localScale
                noteText.setParent(anchorNode)
                val textView = viewRenderable.view.findViewById<TextView>(R.id.label)
                //change the type of car based on index in database
                when (augmentedImage.index) {
                    0 -> textView.text = "The Earth!"
                    1 -> textView.text = "The Lamp!"
                    2 -> textView.text = "The Fire!"
                }
            }

    }


    private fun checkIsSupportedDeviceOrFinish(): Boolean {
        if (ArCoreApk.getInstance().checkAvailability(this) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            finish()
            return false
        }
        val openGlVersionString = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            finish()
            return false
        }
        return true
    }




}