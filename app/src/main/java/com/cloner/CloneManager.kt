package com.cloner

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CloneManager(private val ctx: Context) {
    
    private val workDir = File(ctx.filesDir, "clones")
    init { workDir.mkdirs() }
    
    data class Result(val success: Boolean, val apkFile: File? = null, val newPackage: String? = null, val message: String = "")
    
    fun getInstalledApps(): List<AppInfo> {
        val pm = ctx.packageManager
        val apps = mutableListOf<AppInfo>()
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        for (ri in pm.queryIntentActivities(intent, 0)) {
            try {
                val ai = pm.getApplicationInfo(ri.activityInfo.packageName, 0)
                apps.add(AppInfo(
                    name = pm.getApplicationLabel(ai).toString(),
                    packageName = ai.packageName,
                    version = pm.getPackageInfo(ai.packageName, 0)?.versionName ?: "1.0",
                    apkPath = ai.sourceDir
                ))
            } catch (_: Exception) {}
        }
        return apps.sortedBy { it.name }
    }
    
    suspend fun cloneApp(pkg: String, progress: (String) -> Unit): Result = withContext(Dispatchers.IO) {
        try {
            progress("Extracting APK...")
            val pm = ctx.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            val original = File(ai.sourceDir)
            val dir = File(workDir, pkg.replace(".", "_"))
            dir.mkdirs()
            val temp = File(dir, "original.apk")
            original.copyTo(temp, overwrite = true)
            
            val newPkg = "com.clone.${pkg.replace(".", "_")}"
            
            progress("Decompiling...")
            val out = File(dir, "decompiled")
            val d = Runtime.getRuntime().exec(arrayOf("java","-jar","${ctx.filesDir}/apktool.jar","d",temp.absolutePath,"-o",out.absolutePath,"-f"))
            if (d.waitFor() != 0) return@withContext Result(false, message = "APKTool decompile failed")
            
            progress("Patching manifest...")
            val mf = File(out, "AndroidManifest.xml")
            var mc = mf.readText()
            mc = mc.replace(Regex("""package="[^"]+""""), """package="$newPkg"""")
            mc = mc.replace("<application", 
                "<application\n        <meta-data android:name=\"frida:scripts\" android:value=\"spoof.js\"/>\n        <meta-data android:name=\"frida:gadget\" android:value=\"true\"/>")
            mf.writeText(mc)
            
            progress("Adding spoofer...")
            val ad = File(out, "assets"); ad.mkdirs()
            val profile = DeviceRandomizer.getRandomProfile()
            File(ad, "spoof.js").writeText("""
Java.perform(function(){
function rh(l){return Array.from({length:l},()=>'0123456789abcdef'[Math.floor(Math.random()*16)]).join('')}
function rm(){return Array.from({length:6},()=>Math.floor(Math.random()*256).toString(16).padStart(2,'0')).join(':')}
var B=Java.use('android.os.Build');B.MODEL.value='${profile.model}';B.MANUFACTURER.value='${profile.manufacturer}';B.FINGERPRINT.value='${profile.fingerprint}';B.SERIAL.value='${profile.serial}';
var S=Java.use('android.provider.Settings$Secure');S.getString.overload('android.content.ContentResolver','java.lang.String').implementation=function(cr,n){if(n==='android_id')return rh(16);if(n==='bluetooth_address'||n==='wlan_mac')return rm();return this.getString(cr,n)};
var T=Java.use('android.telephony.TelephonyManager');T.getDeviceId.overload().implementation=function(){return '${profile.imei1}'};T.getImei.overload().implementation=function(){return '${profile.imei1}'};
var W=Java.use('android.net.wifi.WifiInfo');W.getMacAddress.implementation=function(){return rm()};
console.log('[Cloner] Device spoofed!');
});
""")
            
            progress("Adding Frida gadget...")
            val ld = File(out, "lib/arm64-v8a"); ld.mkdirs()
            
            progress("Recompiling...")
            val ua = File(dir, "unsigned.apk")
            val r = Runtime.getRuntime().exec(arrayOf("java","-jar","${ctx.filesDir}/apktool.jar","b",out.absolutePath,"-o",ua.absolutePath))
            if (r.waitFor() != 0) return@withContext Result(false, message = "Recompile failed")
            
            progress("Signing...")
            val sa = File(dir, "cloned.apk")
            val s = Runtime.getRuntime().exec(arrayOf("java","-jar","${ctx.filesDir}/uber-apk-signer.jar","--apk",ua.absolutePath,"--out",sa.absolutePath))
            s.waitFor()
            
            progress("Installing...")
            val dest = File(ctx.cacheDir, "${newPkg}.apk")
            sa.copyTo(dest, overwrite=true)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", dest)
            ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
            
            Result(true, dest, newPkg, "✅ Clone ready!\nNew package: $newPkg")
        } catch(e: Exception) {
            Result(false, message = "ERROR: ${e.message ?: e}")
        }
    }
}
