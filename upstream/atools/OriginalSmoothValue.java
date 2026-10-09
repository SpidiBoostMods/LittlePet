package com.rwstafftool.modules.visuals.taksa;

public class SmoothValue {
    private float current;
    private float target;
    private float startValue;
    private long startTime;
    private final int speedMs;

    public SmoothValue(float value, int speedMs) {
        this.current = value;
        this.target = value;
        this.startValue = value;
        this.startTime = System.currentTimeMillis();
        this.speedMs = speedMs;
    }

    public void set(float value) {
        if (value != this.target) {
            this.startValue = this.get();
            this.target = value;
            this.startTime = System.currentTimeMillis();
        }
    }

    public float get() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed >= (long)this.speedMs) {
            this.current = this.target;
            return this.current;
        }
        float t = (float)elapsed / (float)this.speedMs;
        this.current = this.startValue + (this.target - this.startValue) * t;
        return this.current;
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - this.startTime >= (long)this.speedMs;
    }
}
