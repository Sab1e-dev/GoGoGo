package com.zcshou.utils;

public class RouteStateEvent {
    private RouteManager.RouteState state;
    private String message;

    public RouteStateEvent(RouteManager.RouteState state) {
        this.state = state;
        this.message = "";
    }

    public RouteStateEvent(RouteManager.RouteState state, String message) {
        this.state = state;
        this.message = message;
    }

    public RouteManager.RouteState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }
}