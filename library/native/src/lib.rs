use std::{io, thread};
use std::fs::File;
use std::io::{BufRead, BufReader};
use jni::{JavaVM, JNIEnv};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_FALSE, JNI_TRUE, JNI_VERSION_1_6};
use log::{error, info, LevelFilter};

#[allow(non_snake_case)]
#[no_mangle]
pub unsafe extern "system" fn JNI_OnLoad(vm: JavaVM, _: *mut std::os::raw::c_void) -> jint {
    leaf::mobile::callback::android::set_jvm(vm);
    JNI_VERSION_1_6
}

#[allow(non_snake_case)]
#[no_mangle]
pub unsafe extern "system" fn JNI_OnUnload(vm: JavaVM, _: *mut std::os::raw::c_void) {
    leaf::mobile::callback::android::unset_protect_socket_callback();
    leaf::mobile::callback::android::unset_jvm();
}

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_LeafVPNService_init(
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

// Sets a callback method to protect sockets.
//
// Expects a method with the given name and signature `(I)Z`.
#[allow(non_snake_case)]
#[no_mangle]
pub unsafe extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_LeafVPNService_setProtectSocketCallback(
    mut env: JNIEnv,
    class: JClass,
    name: JString,
) {
    let Ok(name) = env.get_string(&name) else {
        return;
    };
    let name: String = name.into();
    if let Ok(class_g) = env.new_global_ref(class) {
        leaf::mobile::callback::android::set_protect_socket_callback(class_g, name);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_LeafVPNService_runLeaf(
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
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_LeafVPNService_isLeafRunning(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if leaf::is_running(0) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_LeafVPNService_reloadLeaf(
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
pub extern "system" fn Java_com_github_shiroedev2024_leaf_android_library_LeafVPNService_stopLeaf(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if leaf::shutdown(0) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}