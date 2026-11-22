package com.medivault.app

import com.medivault.app.data.model.InsuranceCalculator
import com.medivault.app.data.model.InsuranceRegionPresets
import org.junit.Assert.assertEquals
import org.junit.Test

class InsuranceCalculatorTest {

    @Test
    fun testGpsCoordinateInference() {
        val beijing = InsuranceRegionPresets.findNearestByCoordinates(39.91, 116.39)
        assertEquals("北京市", beijing.regionName)

        val shanghai = InsuranceRegionPresets.findNearestByCoordinates(31.22, 121.48)
        assertEquals("上海市", shanghai.regionName)

        val guangdong = InsuranceRegionPresets.findNearestByCoordinates(22.54, 114.06) // Shenzhen
        assertEquals("广东省", guangdong.regionName)

        val sichuan = InsuranceRegionPresets.findNearestByCoordinates(30.65, 104.07) // Chengdu
        assertEquals("四川省", sichuan.regionName)

        val fallback = InsuranceRegionPresets.findNearestByCoordinates(51.50, -0.12) // London
        assertEquals("全国通用参考", fallback.regionName)
    }

    @Test
    fun testReimbursementCalculationWithCategoriesAndCap() {
        val preset = InsuranceRegionPresets.findByName("北京市")
        val config = InsuranceRegionPresets.buildConfig(
            preset = preset,
            insuranceType = "EMPLOYEE",
            visitType = "OUTPATIENT",
            hospitalLevel = "LEVEL_3"
        )
        // 北京职工三级门诊: 起付线 1800, 报销比例 70%, 乙类先行自付 10%
        // 甲类 2000 + 乙类 1000 (先行自付 100, 纳入 900) + 丙类 500 = 总计 3500
        // 统筹基数 = 2900, 减起付线 1800 = 1100, 报销 = 1100 * 0.7 = 770
        // 个人自付 = 3500 - 770 = 2730
        val res = InsuranceCalculator.calculate(
            config = config,
            categoryAAmount = 2000.0,
            categoryBAmount = 1000.0,
            categoryCAmount = 500.0
        )

        assertEquals(3500.0, res.totalCost, 0.01)
        assertEquals(100.0, res.categoryBCopay, 0.01)
        assertEquals(2900.0, res.eligibleBase, 0.01)
        assertEquals(1800.0, res.deductibleUsed, 0.01)
        assertEquals(770.0, res.reimbursedAmount, 0.01)
        assertEquals(2730.0, res.personalTotalPay, 0.01)
    }

    @Test
    fun testAnnualCapLimit() {
        val config = InsuranceRegionPresets.buildConfig(
            preset = InsuranceRegionPresets.findByName("全国通用参考"),
            insuranceType = "EMPLOYEE",
            visitType = "INPATIENT",
            hospitalLevel = "LEVEL_3"
        ).copy(deductible = 1000.0, reimbursementRate = 80.0, annualCap = 10000.0)

        // 本年已报销 9500，剩余额度仅 500
        val res = InsuranceCalculator.calculate(
            config = config,
            categoryAAmount = 5000.0,
            categoryBAmount = 0.0,
            categoryCAmount = 0.0,
            alreadyReimbursedThisYear = 9500.0
        )
        assertEquals(500.0, res.reimbursedAmount, 0.01)
        assertEquals(4500.0, res.personalTotalPay, 0.01)
        assertEquals(2700.0, res.overCapSelfPay, 0.01)
    }
}
