use std::{io, thread};
use std::fs::File;
use std::io::{BufRead, BufReader};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_FALSE, JNI_TRUE};
use log::{error, info, LevelFilter};

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_Native_init(
    _env: JNIEnv,
    _class: JClass,
) {
    info!("Initializing Rust code");

    let file = {
        let (read, write) = rustix::pipe::pipe().unwrap();
        rustix::stdio::dup2_stdout(&write).unwrap();
        rustix::stdio::dup2_stderr(&write).unwrap();

        File::from(read)
    };

    thread::spawn(move || -> io::Result<()> {
        let mut reader = BufReader::new(file);
        let mut buffer = String::new();
        loop {
            buffer.clear();
            let len = reader.read_line(&mut buffer)?;
            if len == 0 {
                break Ok(());
            } else {
                info!(target: "RustStdoutStderr", "{}", buffer);
            }
        }
    });
}

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_Native_runLeaf(
    mut env: JNIEnv,
    _class: JClass,
    config: JString,
) -> jint {
    let config: String = env.get_string(&config).unwrap().into();
    let opts  = leaf::StartOptions {
        config: leaf::Config::Str(config),
        runtime_opt: leaf::RuntimeOption::SingleThread
    };
    if let Err(e) = leaf::start(0, opts) {
        error!(target: "Leaf", "{:?}", e);
        1 as jint
    } else {
        0 as jint
    }
}

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_Native_reloadLeaf(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    if let Err(e) = leaf::reload(0) {
        error!(target: "Leaf", "{:?}", e);
        1 as jint
    } else {
        0 as jint
    }
}

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_Native_stopLeaf(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if leaf::shutdown(0) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}