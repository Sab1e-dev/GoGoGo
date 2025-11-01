package com.zcshou.utils;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;

public class CoordinateValidator {
    private static final String TAG = "CV";
    public static void validateCoordinateConversion(LatLng baiduPoint) {
        // 正向转换：百度 -> WGS84
        double[] wgsPoint = MapUtils.bd2wgs(baiduPoint.longitude, baiduPoint.latitude);

        // 反向转换：WGS84 -> 百度
        double[] baiduPointReverse = MapUtils.wgs2bd09(wgsPoint[0], wgsPoint[1]); // 注意：wgs2bd09参数是(纬度, 经度)

        // 计算偏差 - 修正顺序
        double distance = MapUtils.getDistance(baiduPoint,
                new LatLng(baiduPointReverse[0], baiduPointReverse[1])); // 修正：纬度,经度

        Log.i(TAG,"=== 坐标转换验证 ===");
        Log.i(TAG,"原始百度坐标: (" + baiduPoint.longitude + ", " + baiduPoint.latitude + ")");
        Log.i(TAG,"转换WGS84坐标: (" + wgsPoint[0] + ", " + wgsPoint[1] + ")");
        Log.i(TAG,"反向百度坐标: (" + baiduPointReverse[0] + ", " + baiduPointReverse[1] + ")"); // 修正顺序
        Log.i(TAG,"转换偏差: " + distance + "米");
        Log.i(TAG,"转换是否精确: " + (distance < 1.0 ? "是" : "否"));
    }

    // 添加更详细的验证方法
    public static void detailedValidation(LatLng baiduPoint) {
        Log.i(TAG,"=== 详细坐标验证 ===");

        // 百度坐标
        double bdLng = baiduPoint.longitude;
        double bdLat = baiduPoint.latitude;
        Log.i(TAG,"百度坐标: 经度=" + bdLng + ", 纬度=" + bdLat);

        // 百度 → WGS84
        double[] wgs84 = MapUtils.bd2wgs(bdLng, bdLat);
        Log.i(TAG,"百度→WGS84: 经度=" + wgs84[0] + ", 纬度=" + wgs84[1]);

        // WGS84 → 百度（验证反向）
        double[] bdReverse = MapUtils.wgs2bd09(wgs84[0], wgs84[1]); // 注意参数顺序：纬度,经度
        Log.i(TAG,"WGS84→百度: 经度=" + bdReverse[0] + ", 纬度=" + bdReverse[1]);

        // 计算偏差
        double distance = MapUtils.getDistance(
                new LatLng(bdLat, bdLng),
                new LatLng(bdReverse[1], bdReverse[0])
        );

        Log.i(TAG,"坐标转换圆环偏差: " + distance + "米");
        Log.i(TAG,"转换质量: " + (distance < 10 ? "优秀" : distance < 50 ? "良好" : "需要改进"));

        // 验证MapUtils.wgs2bd09的参数顺序
        Log.i(TAG,"=== 参数顺序验证 ===");
        double[] test1 = MapUtils.wgs2bd09(39.907, 116.391); // 纬度,经度
        Log.i(TAG,"输入(纬度39.907,经度116.391) → 百度(" + test1[0] + "," + test1[1] + ")");
    }

    public static void testKnownPoints() {
        // 天安门广场
        LatLng tiananmenBaidu = new LatLng(39.915, 116.404);
        Log.i(TAG,"=== 天安门坐标测试 ===");
        detailedValidation(tiananmenBaidu);

        // 上海外滩
        LatLng shanghaiBaidu = new LatLng(31.245, 121.501);
        Log.i(TAG,"=== 上海外滩坐标测试 ===");
        detailedValidation(shanghaiBaidu);
    }
}