package com.oldee.imageviewer

import android.os.Bundle
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

    private var mScaleGestureDetector: ScaleGestureDetector? = null

    private var component = listOf<View>()

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

        binding.vpImage.setOnClickListener {
            visibilityComponent(binding.llLegend.visibility == View.GONE)
        }

//        mScaleGestureDetector =

//        binding.vpImage.getChildAt(0)?.setOnTouchListener { v, event ->
//
//            return@setOnTouchListener true
//        }




        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mode == SINGLE) {
            binding.ivPrev.visibility = View.GONE
            binding.ivNext.visibility = View.GONE
        } else {
            binding.ivPrev.visibility = View.VISIBLE
            binding.ivNext.visibility = View.VISIBLE
        }

        binding.tvMax.text = bitmapList.size.toString()
        binding.tvCurrent.text = "1"

        binding.vpImage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val childView = binding.vpImage.getChildAt(binding.vpImage.currentItem)?.findViewById<ImageView>(R.id.iv_image)
                childView?.let{
                    mScaleGestureDetector = ScaleGestureDetector(requireContext(),
                        ImageViewerActivity.ScaleListener(it)
                    )
                }

                binding.tvCurrent.text = (position + 1).toString()
                if (position == 0 && mode == MULTIPLE) {
                    binding.ivPrev.visibility = View.GONE
                } else if (position == bitmapList.size - 1 && mode == MULTIPLE) {
                    binding.ivNext.visibility = View.GONE
                } else if (mode == MULTIPLE) {
                    binding.ivPrev.visibility = View.VISIBLE
                    binding.ivNext.visibility = View.VISIBLE
                }
            }
        })

        adapter.init()
    }

    private fun visibilityComponent(v:Boolean){
        if(v){
            binding.llLeft.visibility = View.VISIBLE
            binding.llRight.visibility = View.VISIBLE
            binding.llClose.visibility = View.VISIBLE
            binding.llLegend.visibility = View.VISIBLE
        }else{
            binding.llLeft.visibility = View.GONE
            binding.llRight.visibility = View.GONE
            binding.llClose.visibility = View.GONE
            binding.llLegend.visibility = View.GONE
        }
    }
}