// Arquivo de build de nível superior onde você pode adicionar opções de configuração comuns a todos os sub-projetos/módulos.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
}