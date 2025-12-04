package com.github.shiroedev2024.leaf.android.model

import com.github.shiroedev2024.leaf.android.Utils
import com.github.shiroedev2024.leaf.android.library.model.LeafPreferences
import com.github.shiroedev2024.leaf.android.library.model.LogLevel
import com.github.shiroedev2024.leaf.android.library.model.UpdateLeafPreferences

data class AppPreferences(
    val enableIpv6: Boolean = true,
    val preferIpv6: Boolean = false,
    val memoryLogger: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val apiPort: Int = 10001,
    val autoReload: Boolean = false,
    val customUserAgent: String = Utils.createCustomUserAgent(),
    val bypassLan: Boolean = true,
    val bypassLanInCore: Boolean = true,
    val fakeIp: Boolean = false,
    val forceResolveDomain: Boolean = false,
    val bypassGeoipList: List<String> = emptyList(),
    val bypassGeositeList: List<String> = emptyList(),
    val rejectGeoipList: List<String> = emptyList(),
    val rejectGeositeList: List<String> = emptyList(),
)

fun LeafPreferences.toAppPreferences(): AppPreferences {
    return AppPreferences(
        enableIpv6 = this.isEnableIpv6,
        preferIpv6 = this.isPreferIpv6,
        memoryLogger = this.isMemoryLogger,
        logLevel = this.logLevel,
        apiPort = this.apiPort,
        autoReload = this.isAutoReload,
        customUserAgent = this.userAgent ?: "",
        bypassLan = this.isBypassLan,
        bypassLanInCore = this.isBypassLanInCore,
        fakeIp = this.isFakeIp,
        forceResolveDomain = this.isForceResolveDomain,
        bypassGeoipList = this.bypassGeoipList ?: emptyList(),
        bypassGeositeList = this.bypassGeositeList ?: emptyList(),
        rejectGeoipList = this.rejectGeoipList ?: emptyList(),
        rejectGeositeList = this.rejectGeositeList ?: emptyList(),
    )
}

fun AppPreferences.toUpdateLeafPreferences(): UpdateLeafPreferences {
    return UpdateLeafPreferences(
        this.enableIpv6,
        this.preferIpv6,
        this.memoryLogger,
        this.logLevel,
        this.apiPort,
        this.autoReload,
        this.customUserAgent,
        this.bypassLan,
        this.bypassLanInCore,
        this.fakeIp,
        this.forceResolveDomain,
        this.bypassGeoipList,
        this.bypassGeositeList,
        this.rejectGeoipList,
        this.rejectGeositeList,
    )
}

fun UpdateLeafPreferences.toAppPreferences(): AppPreferences {
    return AppPreferences(
        enableIpv6 = this.isEnableIpv6,
        preferIpv6 = this.isPreferIpv6,
        memoryLogger = this.isMemoryLogger,
        logLevel = this.logLevel,
        apiPort = this.apiPort,
        autoReload = this.isAutoReload,
        customUserAgent = this.customUserAgent ?: "",
        bypassLan = this.isBypassLan,
        bypassLanInCore = this.isBypassLanInCore,
        fakeIp = this.isFakeIp,
        forceResolveDomain = this.isForceResolveDomain,
        bypassGeoipList = this.bypassGeoipList ?: emptyList(),
        bypassGeositeList = this.bypassGeositeList ?: emptyList(),
        rejectGeoipList = this.rejectGeoipList ?: emptyList(),
        rejectGeositeList = this.rejectGeositeList ?: emptyList(),
    )
}
