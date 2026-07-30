# Aplicación Sordo (Reconocimiento de Lenguaje de Señas)

Aplicación móvil Android para la captura, entrenamiento y reconocimiento en tiempo real de gestos en lenguaje de señas mediante la cámara del dispositivo.

## Características

- **Autenticación de Usuarios:** Registro e inicio de sesión con almacenamiento remoto en PostgreSQL y hash SHA-256.
- **Captura en Tiempo Real:** Visualización y procesamiento de video usando Android CameraX (soporte para cámara frontal y trasera).
- **Modelo de Señas Local:** Registro de muestras vectoriales de gestos y clasificación local basada en similitud de características.
- **Gestión de Sesión:** Persistencia de estado de usuario con `SharedPreferences`.

## Requisitos del Sistema

- **Android Studio:** Jellyfish / Ladybug o posterior
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 36
- **JDK:** Version 11

## Estructura del Proyecto

```text
app/src/main/java/com/example/aplicacionsordo/
├── HomeActivity.java      # Vista principal, captura de cámara y pruebas de reconocimiento
├── LoginActivity.java     # Pantalla de inicio de sesión
├── RegisterActivity.java  # Pantalla de registro de nuevos usuarios
├── DatabaseHelper.java    # Conexión asíncrona a PostgreSQL remoto (JDBC)
├── SignModelHelper.java   # Módulo de entrenamiento local y clasificación de gestos
├── SessionManager.java    # Manejo de sesión local (SharedPreferences)
└── User.java              # Modelo de datos de usuario
```

## Compilación y Ejecución

1. Abrir el proyecto en Android Studio.
2. Sincronizar las dependencias de Gradle.
3. Ejecutar en un dispositivo físico o emulador con soporte para cámara:

```bash
./gradlew assembleDebug
```

> **Nota:** La aplicación requiere permisos de `CAMERA` e `INTERNET`.
