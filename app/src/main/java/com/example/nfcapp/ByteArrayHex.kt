package com.example.nfcapp

fun ByteArray.toDisplayHex(): String =
    toHexString(
        HexFormat {
            upperCase = true
            bytes {
                byteSeparator = " "
            }
        }
    )
