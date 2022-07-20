package com.oldee.imageviewer

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.oldee.imageviewer.databinding.ActivityImageViewerBinding


class ImageViewerActivity : AppCompatActivity() {
    lateinit var binding: ActivityImageViewerBinding

    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_image_viewer)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener(binding.ivImage))

        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        val data = intent.extras?.getString("uri")

        data?.let{
            binding.ivImage.setImageURI(Uri.parse(it))
        }
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        mScaleGestureDetector?.onTouchEvent(motionEvent)
        return true
    }

    open class ScaleListener(val imageView:ImageView,) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var mScaleFactor:Float = 1.0f

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.let{ dec->
                mScaleFactor *= dec.scaleFactor;

                // 최대 10배, 최소 10배 줌 한계 설정
                mScaleFactor = Math.max(0.1f,
                    Math.min(mScaleFactor, 10.0f));

                // 이미지뷰 스케일에 적용
                imageView.scaleX = mScaleFactor;
                imageView.scaleY = mScaleFactor;
            }

            return true;
        }
    }
}