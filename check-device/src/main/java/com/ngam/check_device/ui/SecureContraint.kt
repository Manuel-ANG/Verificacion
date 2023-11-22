package com.ngam.check_device.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout

@Keep
class SecureContraint : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        this.filterTouchesWhenObscured = true
    }

    override fun onFilterTouchEventForSecurity(event: MotionEvent?): Boolean {
        val flags = event?.flags
        val bad = (((flags?.and(MotionEvent.FLAG_WINDOW_IS_OBSCURED)) != 0) or ((flags?.and(MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0)))
        return if (bad) {
            false
        } else {
            return super.onFilterTouchEventForSecurity(event)
        }
    }
}