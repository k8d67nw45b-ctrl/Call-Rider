package com.example.callrecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var switchAutoRecord: SwitchMaterial
    private lateinit var layoutPermissions: LinearLayout
    private lateinit var btnGrantPermissions: Button
    private lateinit var tvEmpty: TextView
    private lateinit var recyclerRecordings: RecyclerView
    private lateinit var adapter: RecordingsAdapter

    private val requiredPermissions: Array<String>
        get() {
            val perms = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_PHONE_STATE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return perms.toTypedArray()
        }

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        updatePermissionsBanner()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchAutoRecord = findViewById(R.id.switchAutoRecord)
        layoutPermissions = findViewById(R.id.layoutPermissions)
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        tvEmpty = findViewById(R.id.tvEmpty)
        recyclerRecordings = findViewById(R.id.recyclerRecordings)

        adapter = RecordingsAdapter(
            mutableListOf(),
            onPlay = { item -> playRecording(item.file) },
            onDelete = { item, position -> deleteRecording(item, position) }
        )
        recyclerRecordings.layoutManager = LinearLayoutManager(this)
        recyclerRecordings.adapter = adapter

        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        switchAutoRecord.isChecked = prefs.getBoolean(Prefs.KEY_AUTO_RECORD, false)
        switchAutoRecord.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Prefs.KEY_AUTO_RECORD, isChecked).apply()
            if (isChecked && !hasAllPermissions()) {
                Toast.makeText(this, getString(R.string.permissions_needed), Toast.LENGTH_LONG).show()
            }
        }

        btnGrantPermissions.setOnClickListener {
            permissionLauncher.launch(requiredPermissions)
        }

        updatePermissionsBanner()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionsBanner()
        loadRecordings()
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updatePermissionsBanner() {
        layoutPermissions.visibility = if (hasAllPermissions()) View.GONE else View.VISIBLE
    }

    private fun loadRecordings() {
        val dir = File(getExternalFilesDir(null), "CallRecordings")
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        val items = files.map { RecordingItem(it) }
        adapter.updateItems(items)
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recyclerRecordings.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun playRecording(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق لتشغيل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRecording(item: RecordingItem, position: Int) {
        if (item.file.delete()) {
            adapter.removeAt(position)
            tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            recyclerRecordings.visibility = if (adapter.itemCount == 0) View.GONE else View.VISIBLE
        } else {
            Toast.makeText(this, "تعذر حذف الملف", Toast.LENGTH_SHORT).show()
        }
    }
}
