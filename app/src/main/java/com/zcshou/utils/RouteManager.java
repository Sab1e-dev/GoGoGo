package com.zcshou.utils;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.zcshou.gogogo.R;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RouteManager {
    // 单例实例
    private static volatile RouteManager sInstance;
    private List<LatLng> mPoints = new ArrayList<>();
    private Polyline mPolyline;
    private BaiduMap mBaiduMap;
    private Handler mRouteHandler = new Handler(Looper.getMainLooper());
    private int mRouteIndex = 0;
    private RouteListener mRouteListener;
    private boolean isLoopMode = true;
    private boolean isSpeedChanged = false;
    private double mMoveSpeed = 1.0;

    // 状态机定义
    public enum RouteState {
        IDLE,       // 空闲状态
        RUNNING,    // 运行中
        PAUSED      // 已暂停
    }

    private RouteState mCurrentState = RouteState.IDLE;

    // 私有构造函数
    private RouteManager() {
        // 防止外部实例化
    }

    /**
     * 获取单例实例
     */
    public static RouteManager getInstance() {
        if (sInstance == null) {
            synchronized (RouteManager.class) {
                if (sInstance == null) {
                    sInstance = new RouteManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化方法，必须在使用前调用
     */
    public void initialize(BaiduMap baiduMap) {
        if (baiduMap == null) {
            throw new IllegalArgumentException("BaiduMap cannot be null");
        }
        this.mBaiduMap = baiduMap;

        // 重置状态
        reset();
    }

    /**
     * 重置管理器状态
     */
    public void reset() {
        stopRoute();
        mPoints.clear();
        clearRoute();
        mRouteIndex = 0;
        mRouteListener = null;
        isLoopMode = true;
        isSpeedChanged = false;
        mCurrentState = RouteState.IDLE;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return mBaiduMap != null;
    }

    /**
     * 销毁单例实例，释放资源
     */
    public static void destroyInstance() {
        synchronized (RouteManager.class) {
            if (sInstance != null) {
                sInstance.stopRoute();
                sInstance.mRouteHandler.removeCallbacksAndMessages(null);
                sInstance.mPoints.clear();
                sInstance.clearRoute();
                sInstance.mRouteListener = null;
                sInstance = null;
            }
        }
    }

    public double getmMoveSpeed() {
        return mMoveSpeed;
    }

    public void setmMoveSpeed(double mMoveSpeed) {
        this.mMoveSpeed = mMoveSpeed;
    }

    public void setLoopMode(boolean loopMode) {
        this.isLoopMode = loopMode;
    }

    public boolean isLoopMode() {
        return isLoopMode;
    }

    public RouteState getCurrentState() {
        return mCurrentState;
    }

    public static final BitmapDescriptor MAP_INDICATOR =
            BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);

    public interface RouteListener {
        void onPositionUpdate(double wgsLng, double wgsLat);

        void onRouteStarted();

        void onRouteStopped();

        void onRouteFinished();

        void onRoutePaused();  // 新增暂停回调

        void onRouteResumed(); // 新增恢复回调
    }

    public void setRouteListener(RouteListener listener) {
        this.mRouteListener = listener;
    }

    public void addPoint(LatLng point) {
        checkInitialized();
        mPoints.add(point);
        drawLine();
        addMarker(point);
    }

    public void clearPoints() {
        checkInitialized();
        mPoints.clear();
        clearRoute();
    }

    public void undoLastPoint() {
        checkInitialized();
        if (mPoints.isEmpty()) return;
        mPoints.remove(mPoints.size() - 1);
        redrawRoute();
    }

    public List<LatLng> getPoints() {
        return new ArrayList<>(mPoints);
    }

    public boolean isRouteRunning() {
        return mCurrentState == RouteState.RUNNING;
    }

    public boolean isRoutePaused() {
        return mCurrentState == RouteState.PAUSED;
    }

    public boolean isRouteIdle() {
        return mCurrentState == RouteState.IDLE;
    }

    /**
     * 保存路径到JSON文件
     */
    public boolean saveRouteToFile(String filePath) {
        if (mPoints.isEmpty()) {
            return false;
        }

        try {
            JSONObject routeJson = new JSONObject();
            JSONArray pointsArray = new JSONArray();

            for (LatLng point : mPoints) {
                JSONObject pointJson = new JSONObject();
                pointJson.put("longitude", point.longitude);
                pointJson.put("latitude", point.latitude);
                pointsArray.put(pointJson);
            }

            routeJson.put("points", pointsArray);
            routeJson.put("loopMode", isLoopMode);
            routeJson.put("speed", mMoveSpeed);
            routeJson.put("timestamp", System.currentTimeMillis());

            // 写入文件
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(routeJson.toString());
            fileWriter.flush();
            fileWriter.close();

            return true;
        } catch (JSONException | IOException e) {
            Log.e("RouteManager", "保存路径失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从JSON文件加载路径
     */
    public boolean loadRouteFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();

            JSONObject routeJson = new JSONObject(stringBuilder.toString());
            JSONArray pointsArray = routeJson.getJSONArray("points");

            List<LatLng> loadedPoints = new ArrayList<>();
            for (int i = 0; i < pointsArray.length(); i++) {
                JSONObject pointJson = pointsArray.getJSONObject(i);
                double longitude = pointJson.getDouble("longitude");
                double latitude = pointJson.getDouble("latitude");
                loadedPoints.add(new LatLng(latitude, longitude));
            }

            // 停止当前路径
            stopRoute();

            // 清除现有点并加载新点
            mPoints.clear();
            mPoints.addAll(loadedPoints);

            // 设置其他参数
            if (routeJson.has("loopMode")) {
                isLoopMode = routeJson.getBoolean("loopMode");
            }
            if (routeJson.has("speed")) {
                mMoveSpeed = routeJson.getDouble("speed");
            }

            // 重绘路径
            redrawRoute();

            return true;
        } catch (JSONException | IOException e) {
            Log.e("RouteManager", "加载路径失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取保存的路径列表
     */
    public List<String> getSavedRoutes(String directoryPath) {
        List<String> routeFiles = new ArrayList<>();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    routeFiles.add(file.getName());
                }
            }
        }

        return routeFiles;
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
        checkInitialized();
        mBaiduMap.clear();
        if (mPoints.size() >= 2) {
            drawLine();
        }
        for (LatLng point : mPoints) {
            addMarker(point);
        }
    }

    private void clearRoute() {
        if (mBaiduMap != null) {
            mBaiduMap.clear();
        }
        if (mPolyline != null) {
            mPolyline.remove();
            mPolyline = null;
        }
    }

    public void startRoute(double speed) {
        checkInitialized();
        if (mPoints.size() < 2) return;

        // 只有在空闲或暂停状态下才能开始/重新开始
        if (mCurrentState != RouteState.IDLE && mCurrentState != RouteState.PAUSED) {
            return;
        }

        mMoveSpeed = speed;
        Log.d("RM", "Speed=" + mMoveSpeed);

        // 如果是从暂停状态恢复，不需要重置索引
        if (mCurrentState == RouteState.IDLE) {
            mRouteIndex = 0;
        }

        // 更新状态为运行中
        setState(RouteState.RUNNING);

        if (mRouteListener != null) {
            if (mCurrentState == RouteState.IDLE) {
                mRouteListener.onRouteStarted();
            } else {
                mRouteListener.onRouteResumed();
            }
        }

        moveToNextPoint();
    }
    public void startRoute() {
        checkInitialized();
        if (mPoints.size() < 2) return;

        // 只有在空闲或暂停状态下才能开始/重新开始
        if (mCurrentState != RouteState.IDLE && mCurrentState != RouteState.PAUSED) {
            return;
        }

        Log.d("RM", "Speed=" + mMoveSpeed);

        // 如果是从暂停状态恢复，不需要重置索引
        if (mCurrentState == RouteState.IDLE) {
            mRouteIndex = 0;
        }

        // 更新状态为运行中
        setState(RouteState.RUNNING);

        if (mRouteListener != null) {
            if (mCurrentState == RouteState.IDLE) {
                mRouteListener.onRouteStarted();
            } else {
                mRouteListener.onRouteResumed();
            }
        }

        moveToNextPoint();
    }
    public void pauseRoute() {
        checkInitialized();
        // 只有在运行状态下才能暂停
        if (mCurrentState != RouteState.RUNNING) {
            return;
        }

        // 移除所有待执行的回调
        mRouteHandler.removeCallbacksAndMessages(null);

        // 更新状态为暂停
        setState(RouteState.PAUSED);

        if (mRouteListener != null) {
            mRouteListener.onRoutePaused();
        }
    }

    public void stopRoute() {
        // 只有在运行或暂停状态下才能停止
        if (mCurrentState != RouteState.RUNNING && mCurrentState != RouteState.PAUSED) {
            return;
        }

        mRouteHandler.removeCallbacksAndMessages(null);
        mRouteIndex = 0;
        isSpeedChanged = false;

        // 更新状态为空闲
        setState(RouteState.IDLE);

        if (mRouteListener != null) {
            mRouteListener.onRouteStopped();
        }
    }

    public void deleteAllPoints() {
        checkInitialized();
        // 停止路线
        stopRoute();

        // 清空路径点列表
        mPoints.clear();

        // 清除地图上的路线和标记
        clearRoute();

        // 重置路线索引
        mRouteIndex = 0;
    }

    /**
     * 检查是否已初始化
     */
    private void checkInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException("RouteManager must be initialized with BaiduMap before use");
        }
    }

    /**
     * 设置状态并记录日志（便于调试）
     */
    private void setState(RouteState newState) {
        Log.d("RM", "State changed: " + mCurrentState + " -> " + newState);
        mCurrentState = newState;
        EventBus.getDefault().post(new RouteStateEvent(newState));
    }

    private void moveToNextPoint() {
        // 检查状态是否允许继续移动
        if (mCurrentState != RouteState.RUNNING) {
            return;
        }

        // 检查是否到达终点
        if (mRouteIndex >= mPoints.size() - 1) {
            if (isLoopMode && mPoints.size() >= 2) {
                // 闭合模式：从终点移动到起点
                LatLng currentPoint = mPoints.get(mRouteIndex);
                LatLng nextPoint = mPoints.get(0); // 回到起点

                double distance = MapUtils.getDistance(currentPoint, nextPoint);
                long moveTime = (long) (distance / mMoveSpeed * 1000);

                double[] wgsCurrent = MapUtils.bd2wgs(currentPoint.longitude, currentPoint.latitude);
                double[] wgsNext = MapUtils.bd2wgs(nextPoint.longitude, nextPoint.latitude);
                // 重置索引为-1，因为 smoothMove 完成后会执行 mRouteIndex++
                mRouteIndex = -1;
                smoothMove(wgsCurrent[0], wgsCurrent[1], wgsNext[0], wgsNext[1], moveTime, 0);
            } else {
                // 非闭合模式：正常结束
                if (mRouteListener != null) {
                    mRouteListener.onRouteFinished();
                    EventBus.getDefault().post(new RouteStateEvent(mCurrentState));
                }
                setState(RouteState.IDLE);
            }
            return;
        }

        LatLng currentPoint = mPoints.get(mRouteIndex);
        LatLng nextPoint = mPoints.get(mRouteIndex + 1);

        double distance = MapUtils.getDistance(currentPoint, nextPoint);
        long moveTime = (long) (distance / mMoveSpeed * 1000);

        double[] wgsCurrent = MapUtils.bd2wgs(currentPoint.longitude, currentPoint.latitude);
        double[] wgsNext = MapUtils.bd2wgs(nextPoint.longitude, nextPoint.latitude);

        smoothMove(wgsCurrent[0], wgsCurrent[1], wgsNext[0], wgsNext[1], moveTime, 0);
    }

    private void smoothMove(double startLng, double startLat, double endLng, double endLat,
                            long totalTime, long elapsedTime) {
        // 检查状态是否允许继续移动
        if (mCurrentState != RouteState.RUNNING) {
            return;
        }

        // 检查速度是否已更改，如果是则重新计算移动时间
        if (isSpeedChanged) {
            isSpeedChanged = false;

            // 重新计算剩余距离所需的时间
            double remainingDistance = MapUtils.getDistance(
                    new LatLng(startLat, startLng),
                    new LatLng(endLat, endLng)
            ) * (1 - (float) elapsedTime / totalTime);

            totalTime = (long) (remainingDistance / mMoveSpeed * 1000);
            elapsedTime = 0; // 重置已用时间
        }

        float progress = (float) elapsedTime / totalTime;
        if (progress > 1.0f) progress = 1.0f;

        double currentLng = startLng + (endLng - startLng) * progress;
        double currentLat = startLat + (endLat - startLat) * progress;

        if (mRouteListener != null) {
            mRouteListener.onPositionUpdate(currentLng, currentLat);
        }

        if (progress < 1.0f) {
            final long newElapsedTime = elapsedTime + 100;
            final long newTotalTime = totalTime;
            mRouteHandler.postDelayed(() ->
                    smoothMove(startLng, startLat, endLng, endLat, newTotalTime, newElapsedTime), 100);
        } else {
            // 移动到下一个点
            mRouteIndex++;

            if (isLoopMode && mRouteIndex >= mPoints.size()) {
                mRouteIndex = 0;
            }

            if (mRouteIndex >= mPoints.size() - 1 && !isLoopMode) {
                if (mRouteListener != null) {
                    mRouteListener.onRouteFinished();
                }
                setState(RouteState.IDLE);
            } else {
                moveToNextPoint();
            }
        }
    }
}