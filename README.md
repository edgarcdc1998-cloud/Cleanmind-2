# CleanMind Android

**CleanMind** é um aplicativo Android nativo para gerenciamento e otimização inteligente do armazenamento local do dispositivo, construído com foco rigoroso em **privacidade, processamento 100% on-device, segurança e arquitetura limpa**.

---

## 1. Objetivo do Projeto

O CleanMind analisa o armazenamento local do dispositivo, diagnostica categorias de arquivos (imagens, vídeos, áudios, documentos, arquivos temporários, arquivos grandes e duplicatas) e apresenta recomendações explicáveis e determinísticas de limpeza.

### Princípios Fundamentais:
- **Privacidade Absoluta**: Nenhum arquivo ou metadado é transmitido para a rede ou servidores externos.
- **Processamento On-Device**: Diagnósticos, detecção de duplicatas por hash SHA-256 e OCR são executados localmente.
- **Transparência e Explicabilidade**: Toda recomendação expõe o motivo detalhado (*"Por que revisar este item"*).
- **Sem Exclusão Silenciosa**: Nenhuma alteração destrutiva é realizada sem o consentimento e confirmação explícita do usuário.
- **Eficiência de Recursos**: Amostragem controlada, streams fechados imediatamente e reciclagem de bitmaps para mínimo impacto de bateria e memória.

---

## 2. Arquitetura

O projeto segue rigorosamente a **Clean Architecture** combinada com **MVVM** e **Fluxo Unidirecional de Dados (UDF)**:

```
Presentation Layer (Jetpack Compose, Material 3, Navigation Compose)
       │
   ViewModel (HomeViewModel, StateFlow, Coroutines)
       │
  Domain Layer (Use Cases, Models, Repository Interfaces)
       │
   Data Layer (Repository Implementations, Room Database, MediaStore DataSource, Hash Engine)
       │
Android Framework (StatFs, MediaStore, WorkManager, Google ML Kit on-device)
```

### Estrutura de Pacotes:
* `com.aistudio.cleanmind.app.domain`:
  * `model`: Modelos de domínio imutáveis (`StorageFile`, `CleanupRecommendation`, `DeviceStorageStats`, `DuplicateGroup`, `CategorySummary`, `DocumentOcrResult`, `DocumentClassification`).
  * `repository`: Interfaces de repositório (`StorageRepository`, `AnalysisHistoryRepository`, `FileHashRepository`, `SettingsRepository`).
  * `usecase`: Casos de uso especializados (`AnalyzeStorageUseCase`, `FindDuplicateFilesUseCase`, `FindLargeFilesUseCase`, `FindOldFilesUseCase`, `FindTemporaryFilesUseCase`, `GenerateCleanupRecommendationsUseCase`, `GetDeviceStorageStatsUseCase`, `GetLatestAnalysisUseCase`).
* `com.aistudio.cleanmind.app.data`:
  * `datasource`: Acesso ao `MediaStore` e `StatFs`.
  * `local`: Banco de dados Room (`CleanMindDatabase`), entidades (`AnalysisSummaryEntity`, `CategorySummaryEntity`, `ScannedFileEntity`, `RecommendationEntity`) e DAOs (`AnalysisDao`).
  * `repository`: Implementações concretas de repositórios.
* `com.aistudio.cleanmind.app.ocr`:
  * `OcrImageAnalyzer`: Análise on-device de imagens com ML Kit Text Recognition.
  * `DocumentClassifier`: Heurística e regras determinísticas locais para classificação de documentos (fiscais, bancários, jurídicos, acadêmicos).
* `com.aistudio.cleanmind.app.worker`:
  * `StorageAnalysisWorker`: Execução em segundo plano com WorkManager e restrições de bateria.
  * `WorkManagerScheduler`: Agendamento seguro com `ExistingPeriodicWorkPolicy.KEEP` e `ExistingWorkPolicy.REPLACE`.
* `com.aistudio.cleanmind.app.presentation`:
  * `navigation`: Rotas type-safe com `CleanMindDestination` e `CleanMindMainScreen`.
  * `screens`: Telas de Início, Análise, Recomendações, Armazenamento e Configurações.
  * `components`: Componentes modulares reutilizáveis em Material 3.
  * `theme`: Esquema de cores dark/light moderno, tipografia e espaçamentos padronizados.

---

## 3. Tecnologias Utilizadas

* **Linguagem**: Kotlin 2.0+
* **Interface**: Jetpack Compose com Material Design 3
* **Navegação**: Navigation Compose (rotas isoladas e type-safe)
* **Persistência Local**: Room Database com KSP (Kotlin Symbol Processing)
* **Assincronia e Reatividade**: Coroutines & Kotlin Flow (`StateFlow`, `collectAsStateWithLifecycle`)
* **Processamento em Segundo Plano**: AndroidX WorkManager com `CoroutineWorker`
* **Visão Computacional Local**: Google ML Kit On-Device Text Recognition
* **Build System**: Gradle com Kotlin DSL (`build.gradle.kts`) e Version Catalog (`gradle/libs.versions.toml`)
* **Testes**: JUnit 4, Robolectric, Roborazzi Screenshot Testing, AndroidX Work Testing

---

## 4. Como Compilar e Testar

### Pré-requisitos
* JDK 17
* Android SDK (API 36 / Android 16, suporte mínimo API 24 / Android 7.0)

### Compilação do App
```bash
gradle :app:compileDebugKotlin
```

### Execução de Testes Unitários e Robolectric
```bash
gradle :app:testDebugUnitTest
```

### Geração de APK Debug
```bash
gradle :app:assembleDebug
# Saída: app/build/outputs/apk/debug/app-debug.apk
```

### Geração de APK Release (Unsigned)
```bash
gradle :app:assembleRelease
# Saída: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Geração de Android App Bundle (AAB Release)
```bash
gradle :app:bundleRelease
# Saída: app/build/outputs/bundle/release/app-release.aab
```

---

## 5. Permissões Utilizadas

O CleanMind segue o modelo de permissões granulares e modernas do Android:
1. `READ_MEDIA_IMAGES` (Android 13+): Acesso a metadados de fotos e capturas para categorização.
2. `READ_MEDIA_VIDEO` (Android 13+): Acesso a vídeos locais para cálculo de volumetria e grandes arquivos.
3. `READ_MEDIA_AUDIO` (Android 13+): Acesso a arquivos de áudio locais.
4. `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`): Compatibilidade com versões anteriores ao Android 13.

> **Zero Acesso à Internet**: Nenhuma permissão `INTERNET` ou de rede é declarada ou solicitada no `AndroidManifest.xml`.

---

## 6. Política de Privacidade Técnica

1. **Isolamento de Dados**: Todas as operações de leitura e hash são processadas estritamente em memória volátil e banco local Room.
2. **Sem Telemetria ou Rastreamento**: Não há SDKs de terceiros de analytics, crash reporting remoto ou rastreadores publicitários.
3. **Logs Seguros**: Nomes de arquivos sensíveis e caminhos privados não são registrados em logs de produção.
4. **Armazenamento Mínimo**: Apenas metadados agregados e estatísticas de análise são persistidos localmente.

---

## 7. Limitações Conhecidas

* **Scoping do MediaStore**: Em dispositivos com Android 11+ com Scoped Storage, arquivos fora das coleções públicas do MediaStore ou do sandbox do aplicativo só podem ser acessados via Storage Access Framework (SAF) sob seleção explícita.
* **Assinatura de Produção**: Os builds gerados por padrão em ambiente de CI/desenvolvimento são desprovidos de chave de produção (`upload key`). Para publicação na Google Play Store, deve-se fornecer o arquivo JKS via variáveis de ambiente (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
