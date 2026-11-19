# 🎵 Reproductor de Música Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)

## 📖 Descripción

Reproductor de música para Android hecho con **Kotlin y Jetpack Compose**. Busca y reproduce en streaming música libre y legal del catálogo de **Jamendo**, a través de su API, con reproducción en segundo plano y controles en la notificación. Forma parte de mi portafolio de desarrollo móvil.

**Motivación**: quería una experiencia musical sin depender de las plataformas de streaming comerciales. El proyecto está diseñado para que la **fuente de música sea intercambiable**: la app trabaja contra una interfaz (`MusicSource`) y hoy la implementa Jamendo, de modo que añadir otra fuente (otra API o un servidor propio) no obliga a tocar la interfaz de usuario.

## ✨ Características Principales

- 🔎 **Búsqueda en Jamendo** por texto
- 🏷️ **Exploración por género** (20 géneros predefinidos) y pistas **destacadas**
- ▶️ **Reproducción en streaming** con Media3 (ExoPlayer)
- 🔔 **Reproducción en segundo plano**: servicio en primer plano con `MediaSession` y notificación con play/pausa, anterior y siguiente
- 🎛️ **Mini reproductor** con controles de reproducción, anterior, siguiente y parar
- 🎨 **Interfaz con Material Design 3**, tema claro/oscuro y colores dinámicos
- 🧪 **Modo demo**: si no hay clave de API o falla la red, la app carga canciones de demostración
- 🔌 **Fuente de música intercambiable** mediante la interfaz `MusicSource`

## 🛠️ Tecnologías Utilizadas

| Área | Tecnología |
|------|------------|
| **Lenguaje** | Kotlin 2.0 |
| **Interfaz** | Jetpack Compose, Material 3 |
| **Arquitectura** | MVVM con `ViewModel` y `StateFlow` |
| **Inyección de dependencias** | Hilt |
| **Reproducción** | Media3 (ExoPlayer, MediaSession), servicio en primer plano |
| **Red** | OkHttp |
| **JSON** | kotlinx.serialization |
| **API externa** | API REST de Jamendo |
| **Build** | Gradle (Kotlin DSL), AGP 8.9 |

## 📱 Capturas de Pantalla

<img src="imagenes/reproductor1.jpg" alt="Búsqueda y resultados" width="300"/>

*Búsqueda, géneros y resultados de Jamendo, con el mini reproductor*

<img src="imagenes/reproductor2.jpg" alt="Exploración por género" width="300"/>

*Exploración por género con 15 resultados*

## 🚀 Instalación y Configuración

### Prerrequisitos
- Android Studio Meerkat (2024.3.1) o superior
- JDK 17
- Dispositivo o emulador con Android 7.0 (API 24) o superior y conexión a internet
- Una clave de desarrollador de Jamendo (`client_id`), gratuita

### Pasos

1. **Clonar el repositorio**
```bash
git clone https://github.com/Yucsan/android_musica.git
cd android_musica
```

2. **Configurar la clave de Jamendo**

   Crea una aplicación en el portal de desarrolladores de Jamendo para obtener tu `client_id` y añádelo a `local.properties`, en la raíz del proyecto (este archivo no se sube a Git):
```properties
jamendo.api.key=TU_CLIENT_ID
```
   Sin clave, la app funciona en modo demo con canciones de ejemplo.

3. **Abrir en Android Studio** (`Open an Existing Project`), esperar a que sincronice Gradle y ejecutar en un dispositivo o emulador. También desde terminal:
```bash
./gradlew installDebug
```

> 🔒 La clave nunca se escribe en el código: `build.gradle.kts` la lee de `local.properties` y la expone mediante `BuildConfig`.

## 🏗️ Estructura del Proyecto

```
app/src/main/java/com/yucsan/musica2/
├── config/       # JamendoConfig: URLs, límites, géneros y clave (BuildConfig)
├── di/           # Módulo de Hilt
├── modelo/       # Song: modelo de canción de la app
├── servicio/     # MusicSource (interfaz), JamendoService, MusicService, MusicPlayer
├── ui/           # Pantallas en Compose y tema (Material 3)
└── viewmodel/    # MusicViewModel: estado de la UI y control de reproducción
```

## 🧩 Fuentes de música intercambiables

`MusicSource` define lo que la app necesita de una fuente de música: buscar, listar por género, destacados, géneros disponibles y comprobar la conexión. `JamendoService` la implementa y devuelve el modelo propio `Song`, no el JSON de Jamendo, así que la interfaz no depende de la API concreta. El `ViewModel` recibe la fuente como `MusicSource`.

## 📈 Roadmap

- [x] Streaming desde Jamendo (búsqueda, géneros y destacados)
- [x] Reproducción en segundo plano con notificación
- [x] Interfaz de fuente de música (`MusicSource`)
- [ ] Segunda fuente de música (servidor propio u otra API)
- [ ] Reproducción de archivos locales del dispositivo
- [ ] Modos aleatorio y repetición
- [ ] Listas de reproducción personalizadas
- [ ] Mostrar errores de red en la interfaz en lugar de recurrir al modo demo
- [ ] Widget de pantalla de inicio
- [ ] Caché para uso sin conexión

## 🤝 Contribuciones

Este proyecto es parte de mi portafolio personal, pero estoy abierto a sugerencias y mejoras. Si encuentras algún bug o tienes ideas:

1. Fork del repositorio
2. Crear una rama (`git checkout -b feature/nueva-caracteristica`)
3. Commit de los cambios (`git commit -am 'Añadir nueva característica'`)
4. Push a la rama (`git push origin feature/nueva-caracteristica`)
5. Crear un Pull Request

## 📄 Licencia

Proyecto bajo licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

## 👨‍💻 Autor

**Yucsan**
- GitHub: [@Yucsan](https://github.com/Yucsan)

## 🙏 Agradecimientos

- Comunidad de Android Developers
- [Jamendo](https://www.jamendo.com) por su API y su catálogo de música libre
