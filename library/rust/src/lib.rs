use std::{io, thread};
use std::fs::File;
use std::io::{BufRead, BufReader};
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use log::{info, LevelFilter};

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_Native_init(
    _env: JNIEnv,
    _class: JClass,
) {
    android_logger::init_once(android_logger::Config::default().with_max_level(LevelFilter::Trace));

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

// public static native int run_leaf(String config);
#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_Native_runLeaf(
    mut env: JNIEnv,
    _class: JClass,
    config: &JString,
) -> jint {
    let config: String = env.get_string(config).unwrap().try_into().unwrap();

}