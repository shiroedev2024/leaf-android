package com.github.shiroedev2024.leaf.android.library;

public class Native {

    static {
        System.loadLibrary("rust");
    }

    public static native void init();

    public static native int run_leaf(String config);
}
