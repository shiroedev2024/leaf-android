package com.github.shiroedev2024.leaf.android

import android.app.Application
import com.github.shiroedev2024.leaf.android.library.Native
import kotlin.concurrent.thread

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Native.init()

        val config = """
            [General]

            loglevel = info

            #logoutput = leaf.log



            dns-server = 1.1.1.1,8.8.8.8

            routing-domain-resolve = true

            always-fake-ip = *



            socks-interface = 127.0.0.1

            socks-port = 7891



            #tun = auto



            doh-interface = 127.0.0.1

            doh-port = 5123



            [Env]

            #OUTBOUND_INTERFACE=192.168.74.22

            BYPASS_ADDRS=104.21.233.179,104.21.233.180

            ENABLE_IPV6=true



            [Doh]

            CF = 104.21.233.179, 443, domain=cloudflare-dns.com, fragment=true, fragment-packets=0-1, fragment-length=6-19, fragment-interval=8-12, path=/dns-query, get=false

            CF2 = 104.21.233.180, 443, domain=cloudflare-dns.com, fragment=true, fragment-packets=0-1, fragment-length=6-19, fragment-interval=8-12, path=/dns-query, get=false



            [Proxy]

            GB1 = trojan, 104.21.233.179, 443, password=notpublicyet, tls=true, fragment=true, fragment-packets=0-1, fragment-length=6-19, fragment-interval=8-12, sni=new.myfakefirstdomaincard.top, ws=true, ws-host=new.myfakefirstdomaincard.top, ws-path=/chat, amux=true, amux-max=16, amux-con=4

            GB2 = trojan, 104.21.233.180, 443, password=notpublicyet, tls=true, fragment=true, fragment-packets=0-1, fragment-length=6-19, fragment-interval=8-12, sni=new.myfakefirstdomaincard.top, ws=true, ws-host=new.myfakefirstdomaincard.top, ws-path=/chat, amux=true, amux-max=16, amux-con=4



            [Proxy Group]

            Proxy = url-test, GB2, GB2



            [Rule]

            FINAL, Proxy
        """.trimIndent()

        thread {
            val result = Native.runLeaf(config)
            println("Leaf run result: $result")
        }
    }

}