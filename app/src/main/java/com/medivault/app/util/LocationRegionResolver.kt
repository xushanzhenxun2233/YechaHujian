package com.medivault.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.medivault.app.data.model.InsuranceRegionPresets
import com.medivault.app.data.model.RegionInsurancePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class GpsRegionResult(
    val preset: RegionInsurancePreset,
    val detectedAddressLabel: String,
    val latitude: Double,
    val longitude: Double
)

class LocationRegionResolver(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentRegion(): Result<GpsRegionResult> = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = requestFreshOrLastLocation(locationManager)
                ?: return@withContext Result.failure(
                    IllegalStateException("未能获取到 GPS 或网络定位，请确认手机定位服务已开启")
                )

            val lat = location.latitude
            val lng = location.longitude

            // 优先尝试系统 Geocoder 解析省市名称
            val geocodedName = resolveAddressName(lat, lng)
            val preset = if (!geocodedName.isNullOrBlank()) {
                val matched = InsuranceRegionPresets.findByName(geocodedName)
                if (matched.regionName == "全国通用参考") {
                    InsuranceRegionPresets.findNearestByCoordinates(lat, lng)
                } else {
                    matched
                }
            } else {
                InsuranceRegionPresets.findNearestByCoordinates(lat, lng)
            }

            val label = geocodedName ?: "${preset.regionName} (坐标推算)"
            Result.success(
                GpsRegionResult(
                    preset = preset,
                    detectedAddressLabel = label,
                    latitude = lat,
                    longitude = lng
                )
            )
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("未授予定位权限，请在系统设置中允许访问位置信息"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshOrLastLocation(locationManager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        // 1. 先检查是否有较新的缓存定位（10分钟内）
        val bestLast = providers
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (bestLast != null && System.currentTimeMillis() - bestLast.time < 10 * 60 * 1000L) {
            return bestLast
        }

        // 2. 请求一次实时定位（最多等待 8 秒）
        val activeProvider = when {
            runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
                LocationManager.NETWORK_PROVIDER
            runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
                LocationManager.GPS_PROVIDER
            else -> null
        }

        val fresh = if (activeProvider != null) {
            withTimeoutOrNull(8_000L) {
                suspendCancellableCoroutine<Location?> { cont ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        locationManager.getCurrentLocation(
                            activeProvider,
                            null,
                            context.mainExecutor
                        ) { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                    } else {
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                locationManager.removeUpdates(this)
                                if (cont.isActive) cont.resume(location)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                        }
                        @Suppress("DEPRECATION")
                        locationManager.requestSingleUpdate(activeProvider, listener, Looper.getMainLooper())
                        cont.invokeOnCancellation {
                            locationManager.removeUpdates(listener)
                        }
                    }
                }
            }
        } else null

        return fresh ?: bestLast
    }

    @Suppress("DEPRECATION")
    private fun resolveAddressName(lat: Double, lng: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            val list = geocoder.getFromLocation(lat, lng, 1)
            val addr = list?.firstOrNull() ?: return null
            listOfNotNull(addr.adminArea, addr.locality)
                .distinct()
                .joinToString(" ")
                .ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
