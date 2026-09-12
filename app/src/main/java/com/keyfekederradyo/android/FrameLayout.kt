package com.keyfekederradyo.android

import android.content.Context

class FrameLayout(context: Context) : android.widget.FrameLayout(context) {
    var gravity: Int = android.view.Gravity.NO_GRAVITY

    class LayoutParams(width: Int, height: Int) : android.widget.FrameLayout.LayoutParams(width, height)
}
