package com.cloner

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: com.cloner.databinding.ActivityMainBinding
    private lateinit var manager: CloneManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.cloner.databinding.ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        manager = CloneManager(this)
        
        binding.appList.layoutManager = LinearLayoutManager(this)
        binding.refreshBtn.setOnClickListener { loadApps() }
        
        requestPermissions()
        loadApps()
    }
    
    private fun requestPermissions() {
        val perms = listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.REQUEST_INSTALL_PACKAGES,
            android.Manifest.permission.QUERY_ALL_PACKAGES
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
            !packageManager.canRequestPackageInstalls()) {
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }
    
    private fun loadApps() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        scope.launch {
            val apps = withContext(Dispatchers.IO) { manager.getInstalledApps() }
            binding.appList.adapter = AppListAdapter(apps) { app -> showCloneDialog(app) }
            binding.progressBar.visibility = android.view.View.GONE
            binding.statusText.text = "${apps.size} apps loaded"
        }
    }
    
    private fun showCloneDialog(app: AppInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clone '${app.name}'")
            .setMessage("Create cloned version with randomized device identity?\n\nDifferent device details every launch!")
            .setPositiveButton("CLONE & INSTALL") { _, _ -> startCloning(app) }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startCloning(app: AppInfo) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Cloning...")
            .setMessage("Starting...")
            .setCancelable(false)
            .show()
        
        scope.launch {
            val result = manager.cloneApp(app.packageName) { progress ->
                withContext(Dispatchers.Main) { dialog.setMessage(progress) }
            }
            dialog.dismiss()
            
            if (result.success) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("✅ SUCCESS")
                    .setMessage(result.message)
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("❌ FAILED")
                    .setMessage(result.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
