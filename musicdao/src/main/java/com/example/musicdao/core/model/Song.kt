package com.example.musicdao.core.model

import java.io.File

data class Song(
    val title: String,
    val name: String,
    val artist: String,
    val file: File?
)