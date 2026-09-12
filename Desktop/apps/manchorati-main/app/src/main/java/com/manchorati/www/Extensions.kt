package com.manchorati.www

import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation

fun ImageView.loadUserAvatar(photoUrl: String?) {
    // السطر السحري: إزالة الفلتر اللوني (Tint) الذي يجعل الصورة بيضاء
    this.imageTintList = null 

    if (photoUrl.isNullOrEmpty()) {
        setImageResource(R.drawable.ic_profile)
        return
    }
    if (photoUrl.startsWith("avatar_")) {
        val resId = context.resources.getIdentifier(photoUrl, "drawable", context.packageName)
        if (resId != 0) {
            load(resId) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            setImageResource(R.drawable.ic_profile)
        }
    } else {
        load(photoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
            transformations(CircleCropTransformation())
        }
    }
}