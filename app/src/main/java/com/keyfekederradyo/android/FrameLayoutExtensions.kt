package com.keyfekederradyo.android

import android.widget.FrameLayout

// FrameLayout itself has no gravity property; keep the existing player builder source compatible.
var FrameLayout.gravity: Int
    get() = foregroundGravity
    set(value) { foregroundGravity = value }
