package com.ngam.check_device.tap

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout

@Keep
class SecureLayout : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        this.filterTouchesWhenObscured = true
    }

    override fun onFilterTouchEventForSecurity(event: MotionEvent?): Boolean {
        val flags = event?.flags
        val bad = if (Build.VERSION.SDK_INT <= 29) {
            flags?.and(MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0
        } else {
            flags?.and(MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0 || flags.and(MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0
        }
        return if (bad) {
            false
        } else {
            return super.onFilterTouchEventForSecurity(event)
        }
    }
}