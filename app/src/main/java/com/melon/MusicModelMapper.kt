package com.melon

import com.melon.service.MusicEntity

// MusicEntity 에서 MusicModel 로 바꿔주기 위함

fun MusicEntity.mapper(id: Long): MusicModel =
    MusicModel(
        id = id,
        track = this.track,  //this 빼도 무방
        streamUrl = streamUrl,
        artist = artist,
        coverUrl = coverUrl
    )