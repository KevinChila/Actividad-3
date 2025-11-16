# 🚀 Sistema de Gestión de Contactos - Concurrencia en Java

## 📋 Descripción del Proyecto
Sistema completo de gestión de contactos desarrollado en Java Swing que implementa **técnicas avanzadas de programación concurrente y sincronización** como parte de la actividad de la Unidad 3. La aplicación demuestra el manejo eficiente de múltiples hilos y la prevención de condiciones de carrera en operaciones críticas.

## 🎯 Objetivos Cumplidos
Implementar técnicas de multitarea y sincronización para asegurar que la aplicación sea:
- ✅ **Más eficiente** mediante el uso de hilos para operaciones bloqueantes
- ✅ **Más fluida** con interfaces que no se congelan durante procesamiento
- ✅ **Capaz de manejar múltiples procesos simultáneamente** de forma segura

## 🏆 Requisitos Funcionales Implementados

### 🔄 1. Validación de Contactos en Segundo Plano
**✅ COMPLETADO - Implementación Exitosa**
- **Thread dedicado** que valida si el contacto ya existe antes del guardado
- **Evita datos duplicados** verificando email y teléfono concurrentemente
- **Interfaz responsive** que no se bloquea durante la validación
- **Implementación:** `SwingWorker` con sincronización en lista de contactos

### 🔍 2. Búsqueda de Contactos en Segundo Plano  
**✅ COMPLETADO - Implementación Exitosa**
- **Thread independiente** para búsquedas sin bloquear la UI
- **Timer inteligente** de 300ms que espera a que el usuario deje de escribir
- **Búsqueda concurrente** en grandes volúmenes de datos usando `SwingWorker`
- **Resultados en tiempo real** con actualización segura de la interfaz

### 📤 3. Exportación de Contactos con Hilos Múltiples
**✅ COMPLETADO - Implementación Exitosa**
- **Proceso en segundo plano** para exportación a CSV
- **Sincronización robusta** de archivos con `synchronized(exportLock)`
- **Barra de progreso** en tiempo real durante la exportación
- **Prevención de corrupción** cuando múltiples exportaciones ocurren simultáneamente

### 💬 4. Notificaciones en la Interfaz Gráfica
**✅ COMPLETADO - Implementación Exitosa**
- **Threads dedicados** para notificaciones en tiempo real
- **Actualizaciones seguras** usando `SwingUtilities.invokeLater()`
- **Notificaciones transitorias** sin interrumpir el flujo de trabajo
- **Mensajes contextuales** para "Contacto guardado", "Exportación completada", etc.

### 🔒 5. Sincronización y Seguridad en Modificación
**✅ COMPLETADO - Implementación Exitosa**
- **Mecanismo de bloqueo exclusivo** por contacto usando `ReentrantLock`
- **Timeout de 5 segundos** para evitar bloqueos permanentes
- **Synchronized blocks** para operaciones críticas en la lista de contactos
- **Prevención de condiciones de carrera** cuando múltiples hilos acceden al mismo contacto

## 🛠️ Tecnologías y Patrones Implementados

### 🔧 Tecnologías Principales
- **Java SE** con Java Swing para la interfaz gráfica
- **SwingWorker** para operaciones en segundo plano
- **ExecutorService** para gestión eficiente de hilos
- **ReentrantLock** para sincronización granular
- **Synchronized** para secciones críticas

### 🎨 Patrones de Diseño
- **MVC (Modelo-Vista-Controlador)** - Arquitectura separada
- **Worker Thread** - Para operaciones largas
- **Lock Pattern** - Para acceso exclusivo a recursos
- **Observer Pattern** - Para actualizaciones de UI

## 📁 Estructura del Código
