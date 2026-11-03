#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
IronFit - Generador de historial sintético para Minería de Datos
----------------------------------------------------------------
Objetivo:
- Enriquecer MongoDB con historial sintético coherente para los clientes/planes ya existentes.
- Generar asistencias, entrenamientos y evaluaciones físicas.
- NO genera directamente las etiquetas BAJA/MEDIA/ALTA ni BAJO/MEDIO/ALTO.
- Todos los documentos insertados quedan identificados y pueden eliminarse con --limpiar.

Uso:
    python3 generar_historial_mineria.py
    python3 generar_historial_mineria.py --resumen
    python3 generar_historial_mineria.py --limpiar

Requisitos:
    python3 -m pip install pymongo

IMPORTANTE:
- Por seguridad, el lote es fijo. Si ya existe, el script NO vuelve a insertarlo.
- Cambia LOTE_SINTETICO solo si deliberadamente quieres crear un lote adicional.
"""

import argparse
import math
import random
from datetime import datetime, timedelta
from statistics import mean

from pymongo import MongoClient

# =========================================================
# CONFIGURACIÓN
# =========================================================

MONGO_URI = "mongodb://localhost:27017/"
DB_NAME = "ironfit"

LOTE_SINTETICO = "MINERIA_V1_2026"
ORIGEN_DATO = "SIMULADO_MINERIA"

SEMANAS_HISTORIAL = 12

# Si un plan tiene una frecuencia textual reconocible, intentamos respetarla.
# Si no, se usa una frecuencia base aleatoria entre 2 y 5 sesiones/semana.
FRECUENCIA_MIN = 2
FRECUENCIA_MAX = 5

SEMILLA = 20261025
random.seed(SEMILLA)


# =========================================================
# UTILIDADES
# =========================================================

def clamp(valor, minimo, maximo):
    return max(minimo, min(maximo, valor))


def redondear(valor, decimales=2):
    return round(float(valor), decimales)


def fecha_segura(valor):
    return valor if isinstance(valor, datetime) else None


def promedio(lista, default=0.0):
    datos = [x for x in lista if isinstance(x, (int, float))]
    return mean(datos) if datos else default


def obtener_id_texto(valor):
    if valor is None:
        return None
    return str(valor)


def obtener_objetivo(plan):
    return str(plan.get("objetivo", "") or "").strip()


def normalizar_objetivo(texto):
    t = texto.lower()

    if any(x in t for x in ["grasa", "adelgazar", "perder peso", "bajar peso", "defin"]):
        return "BAJAR_GRASA"

    if any(x in t for x in ["masa", "hipertrof", "muscular"]):
        return "MASA_MUSCULAR"

    if any(x in t for x in ["fuerza", "potencia"]):
        return "FUERZA"

    if any(x in t for x in ["resistencia", "cardio"]):
        return "RESISTENCIA"

    return "GENERAL"


def obtener_frecuencia_plan(plan):
    candidatos = [
        plan.get("frecuenciaSemanal"),
        plan.get("frecuencia"),
        plan.get("diasSemana"),
        plan.get("diasPorSemana"),
    ]

    for valor in candidatos:
        if isinstance(valor, int) and FRECUENCIA_MIN <= valor <= 7:
            return valor

        if isinstance(valor, float) and FRECUENCIA_MIN <= valor <= 7:
            return int(round(valor))

        if isinstance(valor, list) and len(valor) > 0:
            return clamp(len(valor), FRECUENCIA_MIN, 6)

        if isinstance(valor, str):
            digitos = [int(x) for x in valor.split() if x.isdigit()]
            if digitos:
                return clamp(digitos[0], FRECUENCIA_MIN, 6)

    return random.randint(FRECUENCIA_MIN, FRECUENCIA_MAX)


def primera_fecha_plan(plan):
    inicio = fecha_segura(plan.get("fechaInicio"))
    fin = fecha_segura(plan.get("fechaFin"))

    if inicio:
        return inicio

    # Si el plan no tiene inicio usable, lo colocamos 13 semanas atrás.
    # Así queda historial suficiente para minería sin usar fechas futuras.
    return datetime.now() - timedelta(weeks=13)


def obtener_rutinas_plan(db, plan):
    plan_id_str = obtener_id_texto(plan.get("_id"))

    consultas = [
        {"planId": plan_id_str},
        {"planId": plan.get("_id")},
    ]

    rutinas = []
    vistos = set()

    for q in consultas:
        for r in db.rutinas.find(q):
            rid = str(r.get("_id"))
            if rid not in vistos:
                vistos.add(rid)
                rutinas.append(r)

    return rutinas


def obtener_ejercicios_rutina(db, rutina):
    """
    Intenta recuperar ejercicios desde distintas estructuras posibles.
    Si no existen, usa el catálogo general de ejercicios.
    """
    ids = []

    for campo in ["ejercicios", "ejercicioIds", "ejerciciosIds"]:
        valor = rutina.get(campo)

        if isinstance(valor, list):
            for item in valor:
                if isinstance(item, dict):
                    eid = (
                        item.get("ejercicioId")
                        or item.get("id")
                        or item.get("_id")
                    )
                else:
                    eid = item

                if eid is not None:
                    ids.append(eid)

    if not ids:
        for campo in ["rutinaId", "idRutina"]:
            if campo in db.rutina_ejercicios.find_one({}) if db.rutina_ejercicios.estimated_document_count() else {}:
                docs = list(
                    db.rutina_ejercicios.find({
                        campo: {"$in": [rutina.get("_id"), str(rutina.get("_id"))]}
                    })
                )
                for d in docs:
                    eid = d.get("ejercicioId") or d.get("idEjercicio")
                    if eid is not None:
                        ids.append(eid)

    # Tomar máximo 8 para que las sesiones sean razonables.
    ids = ids[:8]

    if ids:
        return [str(x) for x in ids]

    # Fallback: ejercicios reales del catálogo
    catalogo = list(db.ejercicios.find({}, {"_id": 1}).limit(120))

    if not catalogo:
        # IDs sintéticos solo si la BD no tiene catálogo.
        return [f"EJ-SIM-{i}" for i in range(1, 7)]

    muestra = random.sample(catalogo, k=min(random.randint(4, 7), len(catalogo)))
    return [str(e["_id"]) for e in muestra]


# =========================================================
# PERFIL LATENTE
# =========================================================

def crear_perfil_latente(objetivo):
    """
    Perfil oculto usado únicamente para generar comportamiento consistente.
    NO se guarda como etiqueta predictiva.

    constancia -> probabilidad de asistir / completar
    respuesta -> progresión física esperada
    """
    tipo = random.choices(
        ["BAJO", "MEDIO", "ALTO"],
        weights=[0.28, 0.44, 0.28],
        k=1
    )[0]

    if tipo == "BAJO":
        constancia = random.uniform(0.38, 0.60)
        respuesta = random.uniform(0.25, 0.55)
    elif tipo == "MEDIO":
        constancia = random.uniform(0.60, 0.80)
        respuesta = random.uniform(0.48, 0.78)
    else:
        constancia = random.uniform(0.78, 0.94)
        respuesta = random.uniform(0.68, 0.96)

    # Introduce desacople para evitar que adherencia y rendimiento
    # sean exactamente la misma cosa.
    respuesta += random.gauss(0, 0.10)
    respuesta = clamp(respuesta, 0.15, 1.0)

    return {
        "tipo": tipo,              # solo en memoria
        "constancia": constancia,  # solo en memoria
        "respuesta": respuesta,    # solo en memoria
        "objetivo": objetivo,
    }


# =========================================================
# BASE FÍSICA
# =========================================================

def obtener_base_fisica_real(db, cliente_id):
    consultas = [
        {"clienteId": cliente_id},
        {"clienteId": str(cliente_id)},
    ]

    docs = []
    vistos = set()

    for q in consultas:
        for e in db.evaluaciones_fisicas.find(q).sort("fecha", 1):
            eid = str(e.get("_id"))
            if eid not in vistos:
                vistos.add(eid)
                docs.append(e)

    if docs:
        e = docs[0]

        medidas = e.get("medidasCorporales") or {}

        peso = e.get("pesoCorporal") or e.get("peso") or random.uniform(55, 95)
        talla = e.get("talla") or random.uniform(1.55, 1.88)

        # Algunas bases guardan talla en centímetros
        if talla > 3:
            talla = talla / 100

        grasa = (
            e.get("porcentajeGraso")
            or e.get("porcentajeGrasa")
            or random.uniform(16, 32)
        )

        return {
            "peso": float(peso),
            "talla": float(talla),
            "grasa": float(grasa),
            "cintura": float(medidas.get("cintura") or random.uniform(70, 105)),
            "brazo": float(medidas.get("brazo") or random.uniform(25, 40)),
            "pecho": float(medidas.get("pecho") or random.uniform(80, 115)),
            "pierna": float(medidas.get("pierna") or random.uniform(45, 68)),
        }

    # Fallback razonable si el cliente nunca tuvo evaluación.
    sexo_ruido = random.uniform(-1, 1)

    talla = random.uniform(1.55, 1.88)
    peso = random.uniform(58, 95)

    return {
        "peso": peso,
        "talla": talla,
        "grasa": random.uniform(17, 31),
        "cintura": random.uniform(72, 103),
        "brazo": random.uniform(27, 39),
        "pecho": random.uniform(82, 112),
        "pierna": random.uniform(47, 66),
    }


def evolucion_fisica(base, objetivo, perfil, semana):
    progreso = semana / SEMANAS_HISTORIAL
    respuesta = perfil["respuesta"]

    # Ruido moderado: no todos evolucionan igual.
    ruido_peso = random.gauss(0, 0.35)
    ruido_medida = random.gauss(0, 0.30)
    ruido_grasa = random.gauss(0, 0.20)

    peso = base["peso"]
    grasa = base["grasa"]
    cintura = base["cintura"]
    brazo = base["brazo"]
    pecho = base["pecho"]
    pierna = base["pierna"]

    if objetivo == "BAJAR_GRASA":
        peso += (-base["peso"] * random.uniform(0.025, 0.075) * respuesta * progreso)
        grasa += (-random.uniform(1.2, 5.0) * respuesta * progreso)
        cintura += (-random.uniform(2.0, 8.0) * respuesta * progreso)
        brazo += random.uniform(-0.4, 0.5) * progreso
        pecho += random.uniform(-1.0, 0.5) * progreso
        pierna += random.uniform(-0.8, 0.6) * progreso

    elif objetivo == "MASA_MUSCULAR":
        peso += (base["peso"] * random.uniform(0.015, 0.055) * respuesta * progreso)
        grasa += random.uniform(-1.0, 1.3) * progreso
        brazo += random.uniform(0.8, 3.2) * respuesta * progreso
        pecho += random.uniform(1.0, 4.5) * respuesta * progreso
        pierna += random.uniform(1.0, 4.5) * respuesta * progreso
        cintura += random.uniform(-1.5, 1.2) * progreso

    elif objetivo == "FUERZA":
        peso += random.uniform(-1.0, 2.5) * respuesta * progreso
        grasa += random.uniform(-1.2, 0.7) * progreso
        brazo += random.uniform(0.3, 1.8) * respuesta * progreso
        pecho += random.uniform(0.4, 2.2) * respuesta * progreso
        pierna += random.uniform(0.5, 2.5) * respuesta * progreso
        cintura += random.uniform(-1.5, 0.8) * progreso

    elif objetivo == "RESISTENCIA":
        peso += random.uniform(-3.0, 0.8) * respuesta * progreso
        grasa += (-random.uniform(0.4, 2.8) * respuesta * progreso)
        cintura += (-random.uniform(0.8, 4.0) * respuesta * progreso)
        brazo += random.uniform(-0.2, 0.9) * progreso
        pecho += random.uniform(-0.5, 1.0) * progreso
        pierna += random.uniform(-0.3, 1.3) * progreso

    else:
        peso += random.uniform(-2.0, 2.0) * respuesta * progreso
        grasa += random.uniform(-1.8, 0.8) * respuesta * progreso
        cintura += random.uniform(-2.5, 1.0) * respuesta * progreso
        brazo += random.uniform(-0.3, 1.5) * respuesta * progreso
        pecho += random.uniform(-0.5, 2.0) * respuesta * progreso
        pierna += random.uniform(-0.5, 2.0) * respuesta * progreso

    peso += ruido_peso
    grasa += ruido_grasa
    cintura += ruido_medida
    brazo += ruido_medida / 2
    pecho += ruido_medida
    pierna += ruido_medida

    peso = clamp(peso, 42, 145)
    grasa = clamp(grasa, 6, 48)
    cintura = clamp(cintura, 52, 145)
    brazo = clamp(brazo, 18, 58)
    pecho = clamp(pecho, 60, 150)
    pierna = clamp(pierna, 30, 95)

    talla = base["talla"]
    imc = peso / (talla ** 2)

    return {
        "peso": redondear(peso),
        "talla": redondear(talla, 2),
        "imc": redondear(imc),
        "grasa": redondear(grasa),
        "cintura": redondear(cintura),
        "brazo": redondear(brazo),
        "pecho": redondear(pecho),
        "pierna": redondear(pierna),
    }


# =========================================================
# SESIONES
# =========================================================

def peso_base_ejercicio(ejercicio_id):
    # Determinístico por ejercicio para que la misma persona no cambie
    # de 10 kg a 90 kg al azar entre sesiones.
    semilla_local = sum(ord(c) for c in str(ejercicio_id))
    rng = random.Random(semilla_local + SEMILLA)
    return rng.uniform(8, 45)


def crear_ejercicios_sesion(ejercicio_ids, objetivo, perfil, semana, asistio):
    if not asistio:
        return []

    resultado = []

    for eid in ejercicio_ids:
        base = peso_base_ejercicio(eid)
        respuesta = perfil["respuesta"]
        constancia = perfil["constancia"]

        if objetivo == "FUERZA":
            incremento_total = random.uniform(0.12, 0.38) * respuesta
            series_obj = random.choice([3, 4, 5])
            reps_obj = random.choice([4, 5, 6, 8])

        elif objetivo == "MASA_MUSCULAR":
            incremento_total = random.uniform(0.08, 0.28) * respuesta
            series_obj = random.choice([3, 4])
            reps_obj = random.choice([8, 10, 12])

        elif objetivo == "RESISTENCIA":
            incremento_total = random.uniform(0.02, 0.15) * respuesta
            series_obj = random.choice([3, 4])
            reps_obj = random.choice([12, 15, 18, 20])

        else:
            incremento_total = random.uniform(0.04, 0.20) * respuesta
            series_obj = random.choice([3, 4])
            reps_obj = random.choice([8, 10, 12, 15])

        progreso_temporal = (semana - 1) / max(SEMANAS_HISTORIAL - 1, 1)

        peso = base * (1 + incremento_total * progreso_temporal)
        peso *= random.uniform(0.94, 1.06)

        # La constancia influye en completar lo programado.
        factor_cumplimiento = clamp(
            random.gauss(constancia + 0.12, 0.12),
            0.45,
            1.05
        )

        series_realizadas = max(1, int(round(series_obj * factor_cumplimiento)))
        reps_realizadas = max(1, int(round(reps_obj * factor_cumplimiento)))

        completado = random.random() < clamp(constancia + 0.08, 0.15, 0.98)

        resultado.append({
            "ejercicioId": str(eid),
            "seriesRealizadas": series_realizadas,
            "repeticionesRealizadas": reps_realizadas,
            "pesoUsado": redondear(peso, 1),
            "completado": completado,
        })

    return resultado


# =========================================================
# GENERACIÓN
# =========================================================

def existe_lote(db):
    colecciones = [
        db.asistencias,
        db.entrenamientos_realizados,
        db.evaluaciones_fisicas,
    ]

    return any(
        c.count_documents({
            "origenDato": ORIGEN_DATO,
            "loteSintetico": LOTE_SINTETICO
        }) > 0
        for c in colecciones
    )


def limpiar_lote(db):
    print(f"\nEliminando únicamente el lote {LOTE_SINTETICO}...")

    total = 0

    for nombre in [
        "asistencias",
        "entrenamientos_realizados",
        "evaluaciones_fisicas",
    ]:
        col = db[nombre]
        r = col.delete_many({
            "origenDato": ORIGEN_DATO,
            "loteSintetico": LOTE_SINTETICO
        })

        total += r.deleted_count
        print(f"  {nombre}: {r.deleted_count} eliminados")

    print(f"\nTotal eliminado: {total}")
    print("Los registros originales de IronFit no fueron tocados.")


def insertar_por_lotes(coleccion, documentos, tamano=1000):
    if not documentos:
        return 0

    total = 0

    for i in range(0, len(documentos), tamano):
        lote = documentos[i:i + tamano]
        r = coleccion.insert_many(lote, ordered=False)
        total += len(r.inserted_ids)

    return total


def generar(db):
    if existe_lote(db):
        print("\nERROR: Este lote sintético ya existe.")
        print(f"Lote: {LOTE_SINTETICO}")
        print("No se insertó nada para evitar duplicados.")
        print("\nSi quieres empezar de nuevo:")
        print("  python3 generar_historial_mineria.py --limpiar")
        print("  python3 generar_historial_mineria.py")
        return

    planes = list(db.planes_entrenamiento.find())

    if not planes:
        print("No se encontraron planes_entrenamiento. No se generó nada.")
        return

    docs_asistencias = []
    docs_entrenamientos = []
    docs_evaluaciones = []

    planes_procesados = 0
    planes_sin_cliente = 0

    print(f"\nPlanes encontrados: {len(planes)}")
    print(f"Generando hasta {SEMANAS_HISTORIAL} semanas por plan...\n")

    for plan in planes:
        plan_id = obtener_id_texto(plan.get("_id"))
        cliente_id = plan.get("clienteId")

        if cliente_id is None:
            planes_sin_cliente += 1
            continue

        cliente_id = obtener_id_texto(cliente_id)
        entrenador_id = obtener_id_texto(
            plan.get("entrenadorId")
            or plan.get("trainerId")
            or plan.get("usuarioEntrenadorId")
        )

        objetivo_texto = obtener_objetivo(plan)
        objetivo = normalizar_objetivo(objetivo_texto)
        frecuencia = obtener_frecuencia_plan(plan)
        inicio = primera_fecha_plan(plan)

        # Evitar crear datos posteriores a hoy.
        hoy = datetime.now()
        fin_generable = min(
            inicio + timedelta(weeks=SEMANAS_HISTORIAL),
            hoy
        )

        semanas_disponibles = max(
            1,
            min(
                SEMANAS_HISTORIAL,
                int((fin_generable - inicio).days / 7)
            )
        )

        rutinas = obtener_rutinas_plan(db, plan)

        if rutinas:
            rutinas_info = []
            for r in rutinas:
                rutinas_info.append({
                    "rutina_id": str(r.get("_id")),
                    "ejercicios": obtener_ejercicios_rutina(db, r)
                })
        else:
            # Si por estructura no se encuentra rutina, no inventamos una
            # nueva colección: usamos el plan como vínculo técnico y
            # ejercicios existentes del catálogo.
            catalogo = list(db.ejercicios.find({}, {"_id": 1}).limit(100))
            ejercicios = (
                [str(x["_id"]) for x in random.sample(
                    catalogo,
                    k=min(random.randint(4, 7), len(catalogo))
                )]
                if catalogo
                else [f"EJ-SIM-{i}" for i in range(1, 7)]
            )

            rutinas_info = [{
                "rutina_id": None,
                "ejercicios": ejercicios
            }]

        perfil = crear_perfil_latente(objetivo)
        base_fisica = obtener_base_fisica_real(db, cliente_id)

        # ---------------------------------------------
        # Evaluación inicial sintética complementaria
        # ---------------------------------------------
        evaluacion_semanas = sorted(set([
            0,
            min(4, semanas_disponibles),
            min(8, semanas_disponibles),
            semanas_disponibles,
        ]))

        for semana_eval in evaluacion_semanas:
            estado = evolucion_fisica(
                base_fisica,
                objetivo,
                perfil,
                max(0, semana_eval)
            )

            fecha_eval = inicio + timedelta(
                weeks=semana_eval,
                hours=random.randint(7, 18)
            )

            if fecha_eval > hoy:
                continue

            evaluacion = {
                "clienteId": cliente_id,
                "fecha": fecha_eval,
                "pesoCorporal": estado["peso"],
                "talla": estado["talla"],
                "imc": estado["imc"],
                "porcentajeGraso": estado["grasa"],
                "medidasCorporales": {
                    "cintura": estado["cintura"],
                    "brazo": estado["brazo"],
                    "pecho": estado["pecho"],
                    "pierna": estado["pierna"],
                },
                "origenDato": ORIGEN_DATO,
                "loteSintetico": LOTE_SINTETICO,
                "fechaGeneracion": datetime.now(),
            }

            if entrenador_id:
                evaluacion["entrenadorId"] = entrenador_id

            docs_evaluaciones.append(evaluacion)

        # ---------------------------------------------
        # Historial semanal
        # ---------------------------------------------
        dias_preferidos = random.sample(
            [0, 1, 2, 3, 4, 5, 6],
            k=min(frecuencia, 7)
        )
        dias_preferidos.sort()

        for semana in range(1, semanas_disponibles + 1):
            # Fluctuación semanal para que no sea un patrón perfecto.
            fatiga_semana = random.gauss(0, 0.08)

            for n_sesion, dia in enumerate(dias_preferidos, start=1):
                fecha = inicio + timedelta(
                    weeks=semana - 1,
                    days=dia,
                    hours=random.randint(6, 19),
                    minutes=random.choice([0, 10, 20, 30, 40, 50])
                )

                if fecha > hoy:
                    continue

                info_rutina = rutinas_info[
                    (semana + n_sesion - 2) % len(rutinas_info)
                ]

                rutina_id = info_rutina["rutina_id"]
                ejercicio_ids = info_rutina["ejercicios"]

                prob_asistencia = clamp(
                    perfil["constancia"] - fatiga_semana,
                    0.15,
                    0.97
                )

                asistio = random.random() < prob_asistencia

                prob_cumplimiento = clamp(
                    perfil["constancia"] + 0.05 + random.gauss(0, 0.08),
                    0.15,
                    0.98
                )

                cumplio = asistio and random.random() < prob_cumplimiento

                asistencia_doc = {
                    "clienteId": cliente_id,
                    "planId": plan_id,
                    "fecha": fecha,
                    "asistio": asistio,
                    "cumplioRutina": cumplio,
                    "origenDato": ORIGEN_DATO,
                    "loteSintetico": LOTE_SINTETICO,
                    "fechaGeneracion": datetime.now(),
                }

                if rutina_id:
                    asistencia_doc["rutinaId"] = rutina_id

                if entrenador_id:
                    asistencia_doc["entrenadorId"] = entrenador_id

                docs_asistencias.append(asistencia_doc)

                if not asistio:
                    continue

                ejercicios = crear_ejercicios_sesion(
                    ejercicio_ids,
                    objetivo,
                    perfil,
                    semana,
                    asistio
                )

                total_ejercicios = len(ejercicios)
                completados = sum(
                    1 for e in ejercicios
                    if e.get("completado") is True
                )

                if total_ejercicios:
                    porcentaje_sesion = (
                        completados / total_ejercicios
                    ) * 100
                else:
                    porcentaje_sesion = 0

                # Agrega variación adicional: una sesión puede estar
                # parcialmente hecha aunque sus ejercicios individuales
                # se vean razonables.
                porcentaje_sesion = clamp(
                    porcentaje_sesion + random.gauss(0, 6),
                    20,
                    100
                )

                series_sesion = [
                    e["seriesRealizadas"]
                    for e in ejercicios
                ]

                reps_sesion = [
                    e["repeticionesRealizadas"]
                    for e in ejercicios
                ]

                pesos_sesion = [
                    e["pesoUsado"]
                    for e in ejercicios
                ]

                entrenamiento_doc = {
                    "clienteId": cliente_id,
                    "planId": plan_id,
                    "fecha": fecha,
                    "estado": (
                        "COMPLETADO"
                        if porcentaje_sesion >= 80
                        else "PARCIAL"
                    ),
                    "ejerciciosRealizados": ejercicios,
                    "totalEjercicios": total_ejercicios,
                    "ejerciciosCompletados": completados,
                    "porcentajeSesion": redondear(porcentaje_sesion),
                    # Campos agregados por compatibilidad con versiones
                    # que los guardaban a nivel de sesión.
                    "seriesRealizadas": redondear(promedio(series_sesion), 1),
                    "repeticionesRealizadas": redondear(promedio(reps_sesion), 1),
                    "pesoUsado": redondear(promedio(pesos_sesion), 1),
                    "completado": porcentaje_sesion >= 80,
                    "origenDato": ORIGEN_DATO,
                    "loteSintetico": LOTE_SINTETICO,
                    "fechaGeneracion": datetime.now(),
                }

                if rutina_id:
                    entrenamiento_doc["rutinaId"] = rutina_id

                if entrenador_id:
                    entrenamiento_doc["entrenadorId"] = entrenador_id

                docs_entrenamientos.append(entrenamiento_doc)

        planes_procesados += 1

    print("Resumen antes de insertar:")
    print(f"  Planes procesados: {planes_procesados}")
    print(f"  Planes sin cliente: {planes_sin_cliente}")
    print(f"  Asistencias nuevas: {len(docs_asistencias)}")
    print(f"  Entrenamientos nuevos: {len(docs_entrenamientos)}")
    print(f"  Evaluaciones nuevas: {len(docs_evaluaciones)}")

    if not docs_asistencias and not docs_entrenamientos and not docs_evaluaciones:
        print("\nNo hay documentos para insertar.")
        return

    print("\nInsertando...")

    n_a = insertar_por_lotes(
        db.asistencias,
        docs_asistencias
    )

    n_e = insertar_por_lotes(
        db.entrenamientos_realizados,
        docs_entrenamientos
    )

    n_ev = insertar_por_lotes(
        db.evaluaciones_fisicas,
        docs_evaluaciones
    )

    print("\n=======================================")
    print(" HISTORIAL SINTÉTICO GENERADO")
    print("=======================================")
    print(f"Lote: {LOTE_SINTETICO}")
    print(f"Asistencias insertadas: {n_a}")
    print(f"Entrenamientos insertados: {n_e}")
    print(f"Evaluaciones insertadas: {n_ev}")
    print(f"TOTAL: {n_a + n_e + n_ev}")
    print()
    print("Todos los registros nuevos tienen:")
    print(f'  origenDato = "{ORIGEN_DATO}"')
    print(f'  loteSintetico = "{LOTE_SINTETICO}"')
    print()
    print("Siguiente paso:")
    print("  python3 generar_dataset.py")


def resumen(db):
    print("\n=======================================")
    print(" RESUMEN DEL LOTE DE MINERÍA")
    print("=======================================")

    for nombre in [
        "asistencias",
        "entrenamientos_realizados",
        "evaluaciones_fisicas",
    ]:
        col = db[nombre]

        reales = col.count_documents({
            "$or": [
                {"origenDato": {"$exists": False}},
                {"origenDato": {"$ne": ORIGEN_DATO}},
            ]
        })

        sinteticos = col.count_documents({
            "origenDato": ORIGEN_DATO,
            "loteSintetico": LOTE_SINTETICO,
        })

        print(
            f"{nombre:28} "
            f"reales/aparentes={reales:6} | "
            f"sintéticos={sinteticos:6}"
        )


# =========================================================
# MAIN
# =========================================================

def main():
    parser = argparse.ArgumentParser(
        description="Generador de historial sintético IronFit"
    )

    parser.add_argument(
        "--limpiar",
        action="store_true",
        help="Elimina únicamente el lote sintético generado por este script."
    )

    parser.add_argument(
        "--resumen",
        action="store_true",
        help="Muestra cuántos registros reales/sintéticos existen."
    )

    args = parser.parse_args()

    client = MongoClient(
        MONGO_URI,
        serverSelectionTimeoutMS=5000
    )

    try:
        client.admin.command("ping")
    except Exception as exc:
        print("No fue posible conectar con MongoDB.")
        print(f"URI: {MONGO_URI}")
        print("Detalle:", exc)
        return

    db = client[DB_NAME]

    if args.limpiar:
        limpiar_lote(db)
    elif args.resumen:
        resumen(db)
    else:
        generar(db)


if __name__ == "__main__":
    main()
