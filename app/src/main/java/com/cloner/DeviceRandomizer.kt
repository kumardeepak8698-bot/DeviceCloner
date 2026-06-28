package com.cloner

import kotlin.random.Random

data class DeviceProfile(
    val model: String, val manufacturer: String, val brand: String,
    val device: String, val product: String, val fingerprint: String,
    val serial: String, val androidId: String, val macAddress: String,
    val bluetoothMac: String, val imei1: String, val imei2: String,
    val bluetoothName: String
)

object DeviceRandomizer {
    fun getRandomProfile(): DeviceProfile {
        val models = listOf("SM-S928B","Pixel 9 Pro","Galaxy S25","OnePlus 13","Xiaomi 14","Nothing 3")
        val mfrs = listOf("samsung","google","oneplus","xiaomi","nothing")
        return DeviceProfile(
            model = models[Random.nextInt(models.size)],
            manufacturer = mfrs[Random.nextInt(mfrs.size)],
            brand = mfrs[Random.nextInt(mfrs.size)],
            device = randomAlpha(8).lowercase(),
            product = randomAlpha(8).lowercase(),
            fingerprint = randomAlpha(8) + "/" + randomAlpha(6) + "/" + randomHex(8) + ":user/release-keys",
            serial = randomHex(8).uppercase(),
            androidId = randomHex(16),
            macAddress = randomMac(),
            bluetoothMac = randomMac().uppercase(),
            imei1 = "35" + randomNum(13),
            imei2 = "35" + randomNum(13),
            bluetoothName = "Galaxy " + randomAlpha(4)
        )
    }
    private fun randomHex(l: Int) = (1..l).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
    private fun randomAlpha(l: Int) = (1..l).map { ('a'..'z').random() }.joinToString("")
    private fun randomNum(l: Int) = (1..l).map { Random.nextInt(10).toString() }.joinToString("")
    private fun randomMac() = (1..6).joinToString(":") { String.format("%02x", Random.nextInt(256)) }
}
