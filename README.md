## Versión VI Semestre
# IronFit

IronFit es una aplicación web orientada a la gestión de entrenadores personalizados y sus clientes afiliados dentro de un gimnasio.

El sistema permite administrar usuarios, entrenadores, clientes, ejercicios, rutinas, planes de entrenamiento, asistencias, evaluaciones físicas y seguimiento del progreso.

# Objetivo del proyecto

Facilitar la gestión y seguimiento de los procesos de entrenamiento personalizados, permitiendo a entrenadores y administradores centralizar la información de los clientes y apoyar la toma de decisiones mediante datos históricos.

# Roles del sistema

• Administrador
• Entrenador
• Cliente

Cada rol dispone de funcionalidades específicas de acuerdo con sus permisos dentro del sistema.

# Funcionalidades principales

• Inicio de sesión con autenticación JWT.
• Gestión de usuarios.
• Gestión de roles.
• Gestión de ejercicios.
• Creación de rutinas.
• Creación y asignación de planes de entrenamiento.
• Asignación de entrenadores a clientes.
• Registro y consulta de asistencias.
• Registro de entrenamientos realizados.
• Evaluaciones físicas.
• Seguimiento del progreso de los clientes.
• Dashboard administrativo.
• Reportes e indicadores.

# Tecnologías utilizadas

# Frontend
• Angular
• TypeScript
• HTML
• CSS

# Backend
• Java
• Spring Boot
• Spring Security
• JWT

# Base de datos
• MongoDB

# Herramientas
• Git y GitHub
• Docker
• MongoDB Compass
• Postman
• Weka
• Google Colab

# Arquitectura general

Angular >
Spring Boot >
MongoDB

El sistema utiliza una arquitectura cliente-servidor, separando el frontend, backend y base de datos.

# Módulo predictivo

Durante el VI semestre se propone incorporar un modelo predictivo que permita clasificar el nivel de adherencia de los clientes a sus planes de entrenamiento.

# Variable objetivo:
nivel_adherencia

# Posibles valores:
• BAJA
• MEDIA
• ALTA

# Herramientas propuestas:
• Weka
• Algoritmo J48
• Google Colab como herramienta de apoyo para preparación y exploración de datos.

El resultado del modelo podrá utilizarse posteriormente para generar alertas y apoyar al entrenador en el seguimiento de sus clientes.

# Ejecución local

# Requisitos
• Java 17
• Node.js
• npm
• Docker
• MongoDB Compass (opcional)

# Base de datos
MongoDB se ejecuta localmente mediante Docker en:
mongodb://localhost:27017/ironfit

# Backend
Desde:
backend-mongo/backendmongo

# Ejecutar:
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/ironfit"
./mvnw spring-boot:run

El backend se ejecutará en:
http://localhost:8080

# Frontend
Desde:
frontend/ironfit

# Ejecutar:
npm install
npm start

El frontend se ejecutará en:
http://localhost:4200

La pantalla de inicio de sesión se encuentra en:
http://localhost:4200/login

# Integrantes
 • Mariana Avendaño Aguas
 • Diego Solis Jiménez
 • Roberto De Oro Saltarín
 • Dean Ardila Serna

# Proyecto de Aula

# Fundación Universitaria Tecnológico Comfenalco

Programa de Tecnología en Desarrollo de Software

VI Semestre

2026

