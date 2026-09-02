package com.aistudio.cleanmind.app.domain.model

enum class BackgroundWorkStatus {
    IDLE,        // Análise não iniciada
    ENQUEUED,    // Aguardando execução no WorkManager
    RUNNING,     // Executando em segundo plano
    SUCCEEDED,   // Concluída com sucesso
    CANCELLED,   // Cancelada pelo usuário/sistema
    FAILED       // Falha durante a execução
}
