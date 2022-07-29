package com.oldee.imageviewer

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.oldee.imageviewer.databinding.LayoutImageViewerDialogBinding



class ImageViewerDialog(
    val bitmapList: List<String>,
    val imageCallback: (ImageView, String) -> Unit
) : DialogFragment() {

    lateinit var binding: LayoutImageViewerDialogBinding

    private var mode = 0
    private val SINGLE = 0
    private val MULTIPLE = 1

    lateinit var adapter: ImageViewerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialogFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LayoutImageViewerDialogBinding.inflate(inflater, container, false)

        binding.llClose.setOnClickListener {
            dismiss()
        }

        mode = if (bitmapList.size == 1) SINGLE else MULTIPLE
        adapter = ImageViewerAdapter(bitmapList, imageCallback)
        binding.vpImage.adapter = adapter

        //animat

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.tvMax.text = bitmapList.size.toString()
        binding.tvCurrent.text = "1"



        binding.vpImage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvCurrent.text = (position + 1).toString()
            }
        })

        adapter.init()
    }
}