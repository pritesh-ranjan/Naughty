package com.example.naughty.binding

import com.example.naughty.data.local.AppBinding
import com.example.naughty.data.local.BindingType

data class BindingEvent(
    val binding: AppBinding? = null,
    val noteId: String = "",
    val packageName: String = "",
    val type: BindingType = BindingType.PERSISTENT,
    val isDismiss: Boolean = false
)
