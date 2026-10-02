package com.yunok.walzi.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** LocalContext in Compose is often a ContextWrapper (theme/Hilt wrappers) - unwrap to the Activity. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
