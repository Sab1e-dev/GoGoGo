package com.zcshou.utils;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.zcshou.gogogo.R;

import java.util.ArrayList;
import java.util.List;

public class RouteManager {
    private List<LatLng> mPoints = new ArrayList<>();
    private Polyline mPolyline;
    private BaiduMap mBaiduMap;
    private Handler mRouteHandler = new Handler(Looper.getMainLooper());
    private boolean isRouteRunning = false;
    private int mRouteIndex = 0;
    private RouteListener mRouteListener;
    private double mMoveSpeed = 1.0; // 默认速度 1米/秒

    public static final BitmapDescriptor MAP_INDICATOR =
            BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);

    public interface RouteListener {
        void onPositionUpdate(double wgsLng, double wgsLat);
        void onRouteStarted();
        void onRouteStopped();
        void onRouteFinished();
    }

    public RouteManager(BaiduMap baiduMap) {
        this.mBaiduMap = baiduMap;
    }

    public void setRouteListener(RouteListener listener) {
        this.mRouteListener = listener;
    }

    public void setMoveSpeed(double speedMeterPerSecond) {
        this.mMoveSpeed = speedMeterPerSecond;
    }

    public void addPoint(LatLng point) {
        mPoints.add(point);
        drawLine();
        addMarker(point);
    }

    public void clearPoints() {
        mPoints.clear();
        clearRoute();
    }

    public void undoLastPoint() {
        if (mPoints.isEmpty()) return;
        mPoints.remove(mPoints.size() - 1);
        redrawRoute();
    }

    public List<LatLng> getPoints() {
        return new ArrayList<>(mPoints);
    }

    public boolean isRouteRunning() {
        return isRouteRunning;
    }

    private void drawLine() {
        if (mPoints.size() < 2) return;

        if (mPolyline != null) {
            mPolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .points(mPoints)
                .width(8)
                .color(Color.RED);
        mPolyline = (Polyline) mBaiduMap.addOverlay(polylineOptions);
    }

    private void addMarker(LatLng point) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(point)
                .icon(MAP_INDICATOR);
        mBaiduMap.addOverlay(markerOptions);
    }

    private void redrawRoute() {
        mBaiduMap.clear();
        if (mPoints.size() >= 2) {
            drawLine();
        }
        for (LatLng point : mPoints) {
            addMarker(point);
        }
    }

    private void clearRoute() {
        if (mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }
        mBaiduMap.clear();
    }

    public void startRoute() {
        if (mPoints.size() < 2) return;

        // 停止之前的运动（如果正在运行）
        stopRoute();

        // 重置到起点
        mRouteIndex = 0;
        isRouteRunning = true;

        if (mRouteListener != null) {
            mRouteListener.onRouteStarted();
        }

        moveToNextPoint();
    }

    public void stopRoute() {
        isRouteRunning = false;
        mRouteHandler.removeCallbacksAndMessages(null);

        if (mRouteListener != null) {
            mRouteListener.onRouteStopped();
        }
    }

    private void moveToNextPoint() {
        if (!isRouteRunning || mRouteIndex >= mPoints.size() - 1) {
            if (isRouteRunning && mRouteListener != null) {
                mRouteListener.onRouteFinished();
            }
            isRouteRunning = false;
            return;
        }

        LatLng currentPoint = mPoints.get(mRouteIndex);
        LatLng nextPoint = mPoints.get(mRouteIndex + 1);

        // 计算两点之间的距离（米）
        double distance = MapUtils.getDistance(currentPoint, nextPoint);
        // 计算移动时间（毫秒）
        long moveTime = (long) (distance / mMoveSpeed * 1000);

        // 转换坐标到WGS84（GPS坐标系）
        double[] wgsCurrent = MapUtils.bd2wgs(currentPoint.longitude, currentPoint.latitude);
        double[] wgsNext = MapUtils.bd2wgs(nextPoint.longitude, nextPoint.latitude);

        // 开始平滑移动
        smoothMove(wgsCurrent[0], wgsCurrent[1], wgsNext[0], wgsNext[1], moveTime, 0);
    }

    private void smoothMove(double startLng, double startLat, double endLng, double endLat,
                            long totalTime, long elapsedTime) {
        if (!isRouteRunning) return;

        float progress = (float) elapsedTime / totalTime;
        if (progress > 1.0f) progress = 1.0f;

        // 线性插值
        double currentLng = startLng + (endLng - startLng) * progress;
        double currentLat = startLat + (endLat - startLat) * progress;

        // 通知位置更新
        if (mRouteListener != null) {
            mRouteListener.onPositionUpdate(currentLng, currentLat);
        }

        if (progress < 1.0f) {
            // 继续移动
            final long newElapsedTime = elapsedTime + 100; // 每100ms更新一次
            mRouteHandler.postDelayed(() ->
                    smoothMove(startLng, startLat, endLng, endLat, totalTime, newElapsedTime), 100);
        } else {
            // 移动到下一个点
            mRouteIndex++;
            if (mRouteIndex >= mPoints.size() - 1) {
                // 到达终点，运动完成
                if (mRouteListener != null) {
                    mRouteListener.onRouteFinished();
                }
                isRouteRunning = false;
            } else {
                // 继续移动到下一个点
                moveToNextPoint();
            }
        }
    }
}