package com.github.shiroedev2024.leaf.android.library;

public class Native {

    static {
        System.loadLibrary("native");
    }

    public static native void init();

    public static native int runLeaf(String config);
    public static native int reloadLeaf();
    public static native boolean stopLeaf();
}
