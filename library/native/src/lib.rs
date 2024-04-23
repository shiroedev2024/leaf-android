use std::{io, thread};
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::net::{IpAddr, SocketAddr};
use jni::{JavaVM, JNIEnv};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_FALSE, JNI_TRUE, JNI_VERSION_1_6};
use log::{error, info, LevelFilter};

#[allow(non_snake_case)]
#[no_mangle]
pub unsafe extern "system" fn JNI_OnLoad(vm: JavaVM, _: *mut std::os::raw::c_void) -> jint {
    // leaf
    leaf::mobile::callback::android::set_jvm(vm);
    // doh
    doh::mobile::callback::android::set_jvm(vm_ptr);

    JNI_VERSION_1_6
}

#[allow(non_snake_case)]
#[no_mangle]
pub unsafe extern "system" fn JNI_OnUnload(_vm: JavaVM, _: *mut std::os::raw::c_void) {
    leaf::mobile::callback::android::unset_protect_socket_callback();
    leaf::mobile::callback::android::unset_jvm();

    doh::mobile::callback::android::unset_protect_socket_callback();
    doh::mobile::callback::android::unset_jvm();
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
        // leaf
        leaf::mobile::callback::android::set_protect_socket_callback(class_g.clone(), name.clone());
        // doh
        doh::mobile::callback::android::set_protect_socket_callback(class_g, name);
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

#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_doh_android_library_DohVPNService_runDoh(
    mut env: JNIEnv,
    _class: JClass,
    listen: JString,
    server: JString,
    domain: JString,
    path: JString,
    post: bool,
    fragment: bool,
    fragment_packets: JString,
    fragment_lengths: JString,
    fragment_intervals: JString
) -> jint {
    let listen: String = env.get_string(&listen).unwrap().into();
    let server: String = env.get_string(&server).unwrap().into();
    let domain: String = env.get_string(&domain).unwrap().into();
    let path: String = env.get_string(&path).unwrap().into();

    let listen_config = doh::ListenConfig::Addr(listen.parse().unwrap());

    let server: SocketAddr = server.parse().unwrap();
    let ip = match server.ip() {
        IpAddr::V4(v4) => v4.to_string(),
        IpAddr::V6(v6) => v6.to_string(),
    };
    let port = server.port();

    let remote_config = if fragment {
        let fragment_packets = env.get_string(&fragment_packets).unwrap().into();
        let fragment_lengths = env.get_string(&fragment_lengths).unwrap().into();
        let fragment_intervals = env.get_string(&fragment_intervals).unwrap().into();

        let (packets_from, packets_to) = parse_fragment_option(fragment_packets).unwrap();
        let (length_min, length_max) = parse_fragment_option(fragment_lengths).unwrap();
        let (interval_min, interval_max) = parse_fragment_option(fragment_intervals).unwrap();

        doh::RemoteHost::Fragment(
            ip,
            port,
            packets_from,
            packets_to,
            length_min,
            length_max,
            interval_min,
            interval_max,
        )
    } else {
        doh::RemoteHost::Direct(ip, port)
    };

    if let Ok(doh_config) = doh::Config::new(
        listen_config,
        remote_config,
        domain.as_str(),
        None,
        None,
        path.as_str(),
        1,
        10,
        post,
        0,
        false
    ) {
        if let Err(e) = doh::run_doh(doh_config) {
            error!(target: "Doh", "{:?}", e);
            1 as jint
        } else {
            0 as jint
        }
    } else {
        error!(target: "Doh", "Failed to parse doh config");
        1 as jint
    }
}

// stop doh
#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_doh_android_library_DohVPNService_stopDoh(
    _env: JNIEnv,
    _class: JClass
) -> jboolean {
    match doh::shutdown_doh() {
        Ok(_) => JNI_TRUE,
        Err(_) => JNI_FALSE
    }
}

// is doh running
#[no_mangle]
pub extern "system" fn Java_com_github_shiroedev2024_doh_android_library_DohVPNService_isDohRunning(
    _env: JNIEnv,
    _class: JClass
) -> jboolean {
    if doh::is_doh_running() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

fn parse_fragment_option(option: String) -> Option<(u64, u64)> {
    let parts: Vec<&str> = option.split('-').map(str::trim).collect();
    if parts.len() == 2 {
        let start = parts[0].parse::<u64>().ok()?;
        let end = parts[1].parse::<u64>().ok()?;
        Some((start, end))
    } else {
        None
    }
}
