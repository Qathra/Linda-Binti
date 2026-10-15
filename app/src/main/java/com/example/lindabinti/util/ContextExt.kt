package com.example.lindabinti.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

fun Context.findActivity(): ComponentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

fun ComponentActivity.canShowWindow(): Boolean = !isFinishing && !isDestroyed
