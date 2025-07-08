# Proyecto IoT - Infraestructura con Docker

Este proyecto forma parte de una solución IoT desarrollada para la cursada de la materia **Sistemas Operativos Avanzados (SOA)** en la Universidad Nacional de La Matanza. El repositorio contiene todos los elementos necesarios para desplegar e integrar una infraestructura completa que recibe datos de sensores, los procesa y los visualiza en tiempo real.

Está dividido en tres partes principales:
- 📱 **ANDROID**: Código fuente de la aplicación Android para interactuar con el sistema.
- 🤖 **EMBEBIDO**: Código del microcontrolador ESP32 que lee sensores y publica valores.
- 🐳 **DockerInfra**: Entorno de infraestructura para backend, procesamiento y visualización.

---

## 🧱 Proyecto DockerInfra

Este proyecto implementa una arquitectura IoT modular basada en Docker Compose. Incluye los siguientes servicios:

- **Mosquitto** (Broker MQTT)
- **Node-RED** (Automatización y lógica de negocio)
- **InfluxDB** (Base de datos de series temporales)
- **Grafana** (Visualización de datos)

La infraestructura fue diseñada para ser fácilmente replicable. Este README documenta todos los pasos necesarios para poner el entorno en funcionamiento.

---

## 📁 Estructura del proyecto

```
DockerInfra/
├── docker-compose.yml
├── grafana/
├── grafana.ini
├── influxdb/
├── mosquitto/
│   ├── config/
│   ├── data/
│   └── log/
└── node-red/
```

---

## 🔧 Requisitos

Sistema operativo: Linux Debian/Ubuntu recomendado

- RAM: 1 GB mínimo (2 GB recomendado para uso fluido con Grafana)
- Espacio en disco: 1 GB mínimo (depende de retención de datos en InfluxDB)

Software requerido:

- Docker
- Docker Compose
- Git
- Acceso a un dominio público (opcional, pero necesario si se quiere usar HTTPS desde fuera de la red local)
- NGINX (para servir Grafana y Node-RED vía HTTPS si se desea)
- Certbot (para SSL gratuito con Let’s Encrypt)

---

## ⚙️ Configuración inicial

### 1. Clonar el repositorio

```bash
git clone https://github.com/UNLAM-SOA/2025_SOA_Q1_L2.git
cd 2025_SOA_Q1_L2/DockerInfra
```

### 2. Crear el archivo `.env`

Este archivo define el dominio donde se accederá a Grafana.

```bash
touch .env
echo "GF_SERVER_DOMAIN=localhost" >> .env
```

> Cambiá `localhost` por tu dominio o IP pública, si vas a exponer Grafana externamente.

### 3. Verificar puertos utilizados

- Mosquitto: `1885`
- Node-RED: `1880`
- InfluxDB: `8086`
- Grafana: `3000`

Asegurate de que no estén en uso.

---

## ▶️ Levantar los servicios

```bash
docker-compose up -d
```

Esto descargará las imágenes necesarias y montará los volúmenes con los datos persistentes.

---

## 🔐 Seguridad

- Mosquitto usa autenticación por usuario/contraseña (configurable en `mosquitto/config/passwd`)
- Grafana requiere configuración de dominio para que funcione con cookies seguras (`SameSite=None`)
- Node-RED puede ser protegido manualmente desde su interfaz (opcional)

### 🔑 Credenciales de prueba

A continuación dejamos los usuarios y contraseñas utilizados en este repositorio (para pruebas). Se recomienda cambiarlos para un entorno real:

- **Mosquitto**: `usuario / usuarioadmin`
- **Node-RED**: `usuario / usuarioadmin`
- **Grafana**: `admin / usuarioadmin`

---

## 🌐 HTTPS con NGINX

Para habilitar HTTPS en Grafana y Node-RED usando NGINX como proxy inverso:

1. Instalar NGINX en el servidor (NAS o PC)
2. Crear un archivo de configuración con redirección HTTPS (puerto 4444 por ejemplo)
3. Obtener un certificado SSL con Certbot:

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx
```

4. Redirigir el tráfico entrante de `https://tu_dominio:4444/grafana` a `localhost:3000`

Este paso permite acceder a Grafana de forma segura desde dispositivos móviles o fuera de la red local.

---

## 📱 Tópicos MQTT utilizados

| Tópico             | Función                                     |
|--------------------|----------------------------------------------|
| `sensor/humedad`   | Publicación del valor actual de humedad      |
| `sensor/luz`       | Publicación del valor de luminosidad         |
| `estado/riego`     | Estado del sistema de riego (1 o 0)          |
| `app/riego`        | Comando de la app Android para activar riego |
| `control/riego`    | Tópico que escucha el ESP32 para activar riego |

---

## 📊 Dashboard en Grafana

Grafana incluye dashboards preconfigurados en la carpeta `grafana/`.

Podés acceder a Grafana en:  
👉 `http://<tu_dominio_o_ip>:3000`

Si configuraste NGINX con HTTPS:  
👉 `https://<tu_dominio>:4444/grafana`

---

## 📱 Aplicación Android

La app Android permite:

- Ver el estado actual del sistema
- Consultar históricos (humedad, riego)
- Programar riego automático
- Activar riego manual

La app se comunica con Node-RED vía HTTPS y con Mosquitto como broker de MQTT.

---

## 🛠 Mantenimiento y Debug

- El ESP32 permite debug por `Serial.print()` si se activa el modo `MODO_DEBUG`
- Node-RED guarda su estado y flows en `node-red/`
- Los contenedores se reinician automáticamente ante fallos (`restart: unless-stopped`)

---

## 📝 Consideraciones

- **NO exponer el puerto 1885 (Mosquitto) públicamente sin TLS y autenticación en una implementación real**
- En `docker-compose.yml` se debe editar el dominio en `.env` para reflejar el entorno real
- Si se detecta que el servidor deja de responder, se recomienda configurar watchdog (no incluido)

---

## ✅ ToDo

- Agregar TLS a Mosquitto
- Añadir alertas con Telegram desde Node-RED
- Automatizar backups desde InfluxDB

---

## 📩 Contacto

Cualquier consulta o problema al clonar este repositorio, contactar a:

- alejandroruiz@alumno.unlam.edu.ar
- mrodriguezbustos@alumno.unlam.edu.ar
- ernojeda@alumno.unlam.edu.ar
- agiardelli@alumno.unlam.edu.ar