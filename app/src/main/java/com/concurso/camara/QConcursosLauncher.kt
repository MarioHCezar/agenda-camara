package com.concurso.camara

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object QConcursosLauncher {
    private const val PACKAGE_NAME = "com.qconcursos.QCX"
    private const val PLAY_STORE_URI = "market://details?id=$PACKAGE_NAME"
    private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"

    fun open(context: Context, url: String) {
        val appLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(appLinkIntent)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall back to the app home screen if it does not handle the supplied URL.
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchIntent != null) {
            try {
                context.startActivity(launchIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // Continue to the store if the installed app cannot be launched.
            }
        }

        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URI)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Não foi possível abrir a Play Store.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}