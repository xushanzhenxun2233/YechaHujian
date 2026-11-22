package com.medivault.app.data.model

import com.medivault.app.data.db.InsuranceConfigEntity
import kotlin.math.max
import kotlin.math.min

data class RegionInsurancePreset(
    val regionName: String,
    val keywords: List<String>,
    val centerLat: Double,
    val centerLng: Double,
    val empOutpatientDeductible: Double,
    val empOutpatientRateL1: Double,
    val empOutpatientRateL2: Double,
    val empOutpatientRateL3: Double,
    val empInpatientDeductibleL1: Double,
    val empInpatientDeductibleL2: Double,
    val empInpatientDeductibleL3: Double,
    val empInpatientRateL1: Double,
    val empInpatientRateL2: Double,
    val empInpatientRateL3: Double,
    val empAnnualCap: Double,
    val resOutpatientDeductible: Double,
    val resOutpatientRate: Double,
    val resInpatientDeductibleL3: Double,
    val resInpatientRateL1: Double,
    val resInpatientRateL2: Double,
    val resInpatientRateL3: Double,
    val resAnnualCap: Double,
    val defaultCategoryBCopay: Double = 10.0
)

object InsuranceRegionPresets {

    val allRegions: List<RegionInsurancePreset> = listOf(
        RegionInsurancePreset(
            regionName = "北京市",
            keywords = listOf("北京", "Beijing"),
            centerLat = 39.9042, centerLng = 116.4074,
            empOutpatientDeductible = 1800.0,
            empOutpatientRateL1 = 90.0, empOutpatientRateL2 = 70.0, empOutpatientRateL3 = 70.0,
            empInpatientDeductibleL1 = 1300.0, empInpatientDeductibleL2 = 1300.0, empInpatientDeductibleL3 = 1300.0,
            empInpatientRateL1 = 90.0, empInpatientRateL2 = 87.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 500000.0,
            resOutpatientDeductible = 550.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 1300.0,
            resInpatientRateL1 = 80.0, resInpatientRateL2 = 78.0, resInpatientRateL3 = 75.0,
            resAnnualCap = 250000.0
        ),
        RegionInsurancePreset(
            regionName = "上海市",
            keywords = listOf("上海", "Shanghai"),
            centerLat = 31.2304, centerLng = 121.4737,
            empOutpatientDeductible = 1500.0,
            empOutpatientRateL1 = 80.0, empOutpatientRateL2 = 75.0, empOutpatientRateL3 = 70.0,
            empInpatientDeductibleL1 = 1500.0, empInpatientDeductibleL2 = 1500.0, empInpatientDeductibleL3 = 1500.0,
            empInpatientRateL1 = 85.0, empInpatientRateL2 = 85.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 610000.0,
            resOutpatientDeductible = 300.0, resOutpatientRate = 70.0,
            resInpatientDeductibleL3 = 300.0,
            resInpatientRateL1 = 80.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 300000.0
        ),
        RegionInsurancePreset(
            regionName = "广东省",
            keywords = listOf("广东", "广州", "深圳", "佛山", "东莞", "珠海", "Guangdong", "Guangzhou", "Shenzhen"),
            centerLat = 23.1291, centerLng = 113.2644,
            empOutpatientDeductible = 0.0,
            empOutpatientRateL1 = 80.0, empOutpatientRateL2 = 70.0, empOutpatientRateL3 = 65.0,
            empInpatientDeductibleL1 = 400.0, empInpatientDeductibleL2 = 800.0, empInpatientDeductibleL3 = 1600.0,
            empInpatientRateL1 = 90.0, empInpatientRateL2 = 85.0, empInpatientRateL3 = 80.0,
            empAnnualCap = 500000.0,
            resOutpatientDeductible = 0.0, resOutpatientRate = 60.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 65.0,
            resAnnualCap = 300000.0
        ),
        RegionInsurancePreset(
            regionName = "浙江省",
            keywords = listOf("浙江", "杭州", "宁波", "温州", "嘉兴", "Zhejiang", "Hangzhou"),
            centerLat = 30.2741, centerLng = 120.1551,
            empOutpatientDeductible = 1000.0,
            empOutpatientRateL1 = 86.0, empOutpatientRateL2 = 82.0, empOutpatientRateL3 = 76.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 600.0, empInpatientDeductibleL3 = 800.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 82.0,
            empAnnualCap = 450000.0,
            resOutpatientDeductible = 300.0, resOutpatientRate = 60.0,
            resInpatientDeductibleL3 = 800.0,
            resInpatientRateL1 = 80.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 70.0,
            resAnnualCap = 250000.0
        ),
        RegionInsurancePreset(
            regionName = "江苏省",
            keywords = listOf("江苏", "南京", "苏州", "无锡", "常州", "Jiangsu", "Nanjing", "Suzhou"),
            centerLat = 32.0603, centerLng = 118.7969,
            empOutpatientDeductible = 600.0,
            empOutpatientRateL1 = 80.0, empOutpatientRateL2 = 70.0, empOutpatientRateL3 = 60.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 500.0, empInpatientDeductibleL3 = 900.0,
            empInpatientRateL1 = 95.0, empInpatientRateL2 = 90.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 400000.0,
            resOutpatientDeductible = 200.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 65.0,
            resAnnualCap = 250000.0
        ),
        RegionInsurancePreset(
            regionName = "四川省",
            keywords = listOf("四川", "成都", "绵阳", "Sichuan", "Chengdu"),
            centerLat = 30.5728, centerLng = 104.0668,
            empOutpatientDeductible = 200.0,
            empOutpatientRateL1 = 60.0, empOutpatientRateL2 = 55.0, empOutpatientRateL3 = 50.0,
            empInpatientDeductibleL1 = 200.0, empInpatientDeductibleL2 = 400.0, empInpatientDeductibleL3 = 800.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 90.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 400000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 60.0,
            resInpatientDeductibleL3 = 800.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 200000.0
        ),
        RegionInsurancePreset(
            regionName = "湖北省",
            keywords = listOf("湖北", "武汉", "宜昌", "Hubei", "Wuhan"),
            centerLat = 30.5928, centerLng = 114.3055,
            empOutpatientDeductible = 500.0,
            empOutpatientRateL1 = 80.0, empOutpatientRateL2 = 60.0, empOutpatientRateL3 = 50.0,
            empInpatientDeductibleL1 = 200.0, empInpatientDeductibleL2 = 400.0, empInpatientDeductibleL3 = 800.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 89.0, empInpatientRateL3 = 86.0,
            empAnnualCap = 350000.0,
            resOutpatientDeductible = 200.0, resOutpatientRate = 50.0,
            resInpatientDeductibleL3 = 800.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 70.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 180000.0
        ),
        RegionInsurancePreset(
            regionName = "山东省",
            keywords = listOf("山东", "济南", "青岛", "烟台", "Shandong", "Jinan", "Qingdao"),
            centerLat = 36.6512, centerLng = 117.1201,
            empOutpatientDeductible = 400.0,
            empOutpatientRateL1 = 80.0, empOutpatientRateL2 = 70.0, empOutpatientRateL3 = 60.0,
            empInpatientDeductibleL1 = 200.0, empInpatientDeductibleL2 = 500.0, empInpatientDeductibleL3 = 1000.0,
            empInpatientRateL1 = 94.0, empInpatientRateL2 = 90.0, empInpatientRateL3 = 86.0,
            empAnnualCap = 400000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 60.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 65.0,
            resAnnualCap = 200000.0
        ),
        RegionInsurancePreset(
            regionName = "陕西省",
            keywords = listOf("陕西", "西安", "Shaanxi", "Xian"),
            centerLat = 34.3416, centerLng = 108.9398,
            empOutpatientDeductible = 200.0,
            empOutpatientRateL1 = 70.0, empOutpatientRateL2 = 60.0, empOutpatientRateL3 = 50.0,
            empInpatientDeductibleL1 = 200.0, empInpatientDeductibleL2 = 500.0, empInpatientDeductibleL3 = 850.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 350000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 180000.0
        ),
        RegionInsurancePreset(
            regionName = "重庆市",
            keywords = listOf("重庆", "Chongqing"),
            centerLat = 29.5630, centerLng = 106.5516,
            empOutpatientDeductible = 200.0,
            empOutpatientRateL1 = 60.0, empOutpatientRateL2 = 55.0, empOutpatientRateL3 = 50.0,
            empInpatientDeductibleL1 = 200.0, empInpatientDeductibleL2 = 440.0, empInpatientDeductibleL3 = 880.0,
            empInpatientRateL1 = 90.0, empInpatientRateL2 = 87.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 350000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 800.0,
            resInpatientRateL1 = 80.0, resInpatientRateL2 = 70.0, resInpatientRateL3 = 55.0,
            resAnnualCap = 180000.0
        ),
        RegionInsurancePreset(
            regionName = "福建省",
            keywords = listOf("福建", "福州", "厦门", "泉州", "Fujian", "Fuzhou", "Xiamen"),
            centerLat = 26.0745, centerLng = 119.2965,
            empOutpatientDeductible = 800.0,
            empOutpatientRateL1 = 85.0, empOutpatientRateL2 = 80.0, empOutpatientRateL3 = 75.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 500.0, empInpatientDeductibleL3 = 800.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 84.0,
            empAnnualCap = 400000.0,
            resOutpatientDeductible = 200.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 800.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 65.0,
            resAnnualCap = 200000.0
        ),
        RegionInsurancePreset(
            regionName = "天津市",
            keywords = listOf("天津", "Tianjin"),
            centerLat = 39.0842, centerLng = 117.2009,
            empOutpatientDeductible = 800.0,
            empOutpatientRateL1 = 75.0, empOutpatientRateL2 = 65.0, empOutpatientRateL3 = 55.0,
            empInpatientDeductibleL1 = 500.0, empInpatientDeductibleL2 = 1100.0, empInpatientDeductibleL3 = 1700.0,
            empInpatientRateL1 = 90.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 450000.0,
            resOutpatientDeductible = 500.0, resOutpatientRate = 50.0,
            resInpatientDeductibleL3 = 1700.0,
            resInpatientRateL1 = 80.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 65.0,
            resAnnualCap = 250000.0
        ),
        RegionInsurancePreset(
            regionName = "河南省",
            keywords = listOf("河南", "郑州", "洛阳", "Henan", "Zhengzhou"),
            centerLat = 34.7466, centerLng = 113.6253,
            empOutpatientDeductible = 300.0,
            empOutpatientRateL1 = 65.0, empOutpatientRateL2 = 55.0, empOutpatientRateL3 = 50.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 600.0, empInpatientDeductibleL3 = 900.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 350000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 1200.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 150000.0
        ),
        RegionInsurancePreset(
            regionName = "湖南省",
            keywords = listOf("湖南", "长沙", "Hunan", "Changsha"),
            centerLat = 28.2282, centerLng = 112.9388,
            empOutpatientDeductible = 200.0,
            empOutpatientRateL1 = 70.0, empOutpatientRateL2 = 60.0, empOutpatientRateL3 = 55.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 600.0, empInpatientDeductibleL3 = 900.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 84.0,
            empAnnualCap = 350000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 60.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 150000.0
        ),
        RegionInsurancePreset(
            regionName = "辽宁省",
            keywords = listOf("辽宁", "沈阳", "大连", "Liaoning", "Shenyang", "Dalian"),
            centerLat = 41.8057, centerLng = 123.4315,
            empOutpatientDeductible = 400.0,
            empOutpatientRateL1 = 70.0, empOutpatientRateL2 = 60.0, empOutpatientRateL3 = 50.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 600.0, empInpatientDeductibleL3 = 1000.0,
            empInpatientRateL1 = 92.0, empInpatientRateL2 = 88.0, empInpatientRateL3 = 85.0,
            empAnnualCap = 350000.0,
            resOutpatientDeductible = 100.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 85.0, resInpatientRateL2 = 75.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 150000.0
        ),
        RegionInsurancePreset(
            regionName = "全国通用参考",
            keywords = listOf("全国", "通用", "默认"),
            centerLat = 35.8617, centerLng = 104.1954,
            empOutpatientDeductible = 500.0,
            empOutpatientRateL1 = 75.0, empOutpatientRateL2 = 65.0, empOutpatientRateL3 = 55.0,
            empInpatientDeductibleL1 = 300.0, empInpatientDeductibleL2 = 600.0, empInpatientDeductibleL3 = 1000.0,
            empInpatientRateL1 = 90.0, empInpatientRateL2 = 85.0, empInpatientRateL3 = 80.0,
            empAnnualCap = 300000.0,
            resOutpatientDeductible = 200.0, resOutpatientRate = 55.0,
            resInpatientDeductibleL3 = 1000.0,
            resInpatientRateL1 = 80.0, resInpatientRateL2 = 70.0, resInpatientRateL3 = 60.0,
            resAnnualCap = 150000.0
        )
    )

    fun findByName(name: String): RegionInsurancePreset {
        return allRegions.firstOrNull { preset ->
            preset.regionName == name || preset.keywords.any { name.contains(it, ignoreCase = true) }
        } ?: allRegions.last()
    }

    /**
     * 根据经纬度通过最近城市中心距离推断所在地区（离线 GPS 兜底算法）。
     */
    fun findNearestByCoordinates(lat: Double, lng: Double): RegionInsurancePreset {
        val candidates = allRegions.filter { it.regionName != "全国通用参考" }
        var best = allRegions.last()
        var bestDistSq = Double.MAX_VALUE
        for (preset in candidates) {
            val dLat = lat - preset.centerLat
            val dLng = lng - preset.centerLng
            val distSq = dLat * dLat + dLng * dLng
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                best = preset
            }
        }
        // 如果距离超过约 8 度（不在中国大陆附近），回退全国通用参考
        return if (bestDistSq <= 64.0) best else allRegions.last()
    }

    /**
     * 根据地区预设 + 医保类型 + 就诊类型 + 医院等级生成具体医保配置参数。
     */
    fun buildConfig(
        preset: RegionInsurancePreset,
        insuranceType: String,
        visitType: String,
        hospitalLevel: String,
        lat: Double? = null,
        lng: Double? = null
    ): InsuranceConfigEntity {
        val isEmployee = insuranceType == "EMPLOYEE"
        val isOutpatient = visitType == "OUTPATIENT"

        val deductible: Double
        val rate: Double
        val cap: Double

        if (isEmployee) {
            cap = preset.empAnnualCap
            if (isOutpatient) {
                deductible = preset.empOutpatientDeductible
                rate = when (hospitalLevel) {
                    "LEVEL_1" -> preset.empOutpatientRateL1
                    "LEVEL_2" -> preset.empOutpatientRateL2
                    else -> preset.empOutpatientRateL3
                }
            } else {
                deductible = when (hospitalLevel) {
                    "LEVEL_1" -> preset.empInpatientDeductibleL1
                    "LEVEL_2" -> preset.empInpatientDeductibleL2
                    else -> preset.empInpatientDeductibleL3
                }
                rate = when (hospitalLevel) {
                    "LEVEL_1" -> preset.empInpatientRateL1
                    "LEVEL_2" -> preset.empInpatientRateL2
                    else -> preset.empInpatientRateL3
                }
            }
        } else {
            cap = preset.resAnnualCap
            if (isOutpatient) {
                deductible = preset.resOutpatientDeductible
                rate = preset.resOutpatientRate
            } else {
                deductible = when (hospitalLevel) {
                    "LEVEL_1" -> preset.resInpatientDeductibleL3 * 0.3
                    "LEVEL_2" -> preset.resInpatientDeductibleL3 * 0.6
                    else -> preset.resInpatientDeductibleL3
                }
                rate = when (hospitalLevel) {
                    "LEVEL_1" -> preset.resInpatientRateL1
                    "LEVEL_2" -> preset.resInpatientRateL2
                    else -> preset.resInpatientRateL3
                }
            }
        }

        return InsuranceConfigEntity(
            id = 1,
            regionName = preset.regionName,
            insuranceType = insuranceType,
            visitType = visitType,
            hospitalLevel = hospitalLevel,
            deductible = deductible,
            reimbursementRate = rate,
            annualCap = cap,
            categoryBCopayRate = preset.defaultCategoryBCopay,
            isCustomOverride = false,
            lastGpsLatitude = lat,
            lastGpsLongitude = lng,
            updatedAt = System.currentTimeMillis()
        )
    }
}

data class ReimbursementCalculationResult(
    val totalCost: Double,
    val eligibleBase: Double,
    val categoryBCopay: Double,
    val categoryCSelfPay: Double,
    val deductibleUsed: Double,
    val reimbursedAmount: Double,
    val overCapSelfPay: Double,
    val personalTotalPay: Double,
    val effectiveRatioPercent: Double
)

object InsuranceCalculator {
    fun calculate(
        config: InsuranceConfigEntity,
        categoryAAmount: Double,
        categoryBAmount: Double,
        categoryCAmount: Double,
        alreadyReimbursedThisYear: Double = 0.0
    ): ReimbursementCalculationResult {
        val catA = max(0.0, categoryAAmount)
        val catB = max(0.0, categoryBAmount)
        val catC = max(0.0, categoryCAmount)
        val total = catA + catB + catC

        val catBCopay = catB * (config.categoryBCopayRate.coerceIn(0.0, 100.0) / 100.0)
        val eligibleBase = catA + (catB - catBCopay)

        val deductibleUsed = min(eligibleBase, max(0.0, config.deductible))
        val reimbursableExcess = max(0.0, eligibleBase - deductibleUsed)

        val rate = config.reimbursementRate.coerceIn(0.0, 100.0) / 100.0
        val rawReimbursed = reimbursableExcess * rate

        val remainingCap = max(0.0, config.annualCap - max(0.0, alreadyReimbursedThisYear))
        val finalReimbursed = min(rawReimbursed, remainingCap)
        val overCapSelfPay = max(0.0, rawReimbursed - finalReimbursed)

        val personalPay = max(0.0, total - finalReimbursed)
        val effectiveRatio = if (total > 0.0) (finalReimbursed / total) * 100.0 else 0.0

        return ReimbursementCalculationResult(
            totalCost = total,
            eligibleBase = eligibleBase,
            categoryBCopay = catBCopay,
            categoryCSelfPay = catC,
            deductibleUsed = deductibleUsed,
            reimbursedAmount = finalReimbursed,
            overCapSelfPay = overCapSelfPay,
            personalTotalPay = personalPay,
            effectiveRatioPercent = effectiveRatio
        )
    }
}
