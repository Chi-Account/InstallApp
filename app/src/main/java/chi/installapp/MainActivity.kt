package chi.installapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.FileProvider
import java.io.*
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE_ACTION_MANAGE_UNKNOWN_APP_SOURCES = 2
    }

    private val file: File get() = File(getExternalFilesDir(null), "test.apk")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        thread {
            try {
                prepareFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_ACTION_MANAGE_UNKNOWN_APP_SOURCES -> {
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
                        install()
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onClick(view: View) {
        beforeInstall()
    }

    private fun prepareFile() {
        if (file.exists()) {
            return
        }
        file.createNewFile()
        val inputStream = assets.open("test.apk")
        FileOutputStream(file).use {
            it.write(inputStream.readBytes())
        }
        inputStream.close()
    }

    private fun beforeInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0 申请 REQUEST_INSTALL_PACKAGES 权限
            if (packageManager.canRequestPackageInstalls()) {
                install()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${packageName}")
                startActivityForResult(
                    intent,
                    PERMISSION_REQUEST_CODE_ACTION_MANAGE_UNKNOWN_APP_SOURCES
                )
            }
        } else {
            install()
        }
    }

    private fun install() {
        val intent = Intent(Intent.ACTION_VIEW)
        val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
        intent.setDataAndType(
            fileUri,
            "application/vnd.android.package-archive"
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}