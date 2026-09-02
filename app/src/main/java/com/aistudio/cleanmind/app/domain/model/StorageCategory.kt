package com.aistudio.cleanmind.app.domain.model

enum class StorageCategory {
    IMAGES,
    VIDEOS,
    AUDIOS,
    DOCUMENTS,
    LARGE_FILES,
    OTHERS;

    val displayName: String
        get() = when (this) {
            IMAGES -> "Imagens"
            VIDEOS -> "Vídeos"
            AUDIOS -> "Áudios"
            DOCUMENTS -> "Documentos"
            LARGE_FILES -> "Arquivos Grandes"
            OTHERS -> "Outros"
        }
}
