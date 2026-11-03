from pymongo import MongoClient
from datetime import datetime
import pandas as pd
import statistics
from collections import defaultdict

# ==========================================================
# CONEXIÓN
# ==========================================================

client = MongoClient("mongodb://localhost:27017/")
db = client["ironfit"]

planes = list(db.planes_entrenamiento.find())
asistencias = list(db.asistencias.find())
entrenamientos = list(db.entrenamientos_realizados.find())
evaluaciones = list(db.evaluaciones_fisicas.find())
rutinas = list(db.rutinas.find())

# ==========================================================
# FUNCIONES AUXILIARES
# ==========================================================

def promedio(lista):
    lista = [x for x in lista if x is not None]
    return round(statistics.mean(lista), 2) if lista else 0


def porcentaje(parte, total):
    if total == 0:
        return 0
    return round((parte / total) * 100, 2)


def obtener_medida(evaluacion, medida):
    medidas = evaluacion.get("medidasCorporales", {})
    return medidas.get(medida, 0) or 0


def safe_fecha(valor):
    return valor if valor else datetime.min


# ==========================================================
# DATASET 1: ADHERENCIA
# ==========================================================

filas_adherencia = []

for plan in planes:

    plan_id = str(plan["_id"])
    cliente_id = plan.get("clienteId")

    if not cliente_id:
        continue

    fecha_inicio = plan.get("fechaInicio")
    fecha_fin = plan.get("fechaFin")

    dias_en_plan = 0
    if fecha_inicio and fecha_fin:
        dias_en_plan = max((fecha_fin - fecha_inicio).days, 1)

    # ------------------------------------------------------
    # RUTINAS
    # ------------------------------------------------------

    rutinas_plan = [
        r for r in rutinas
        if r.get("planId") == plan_id
    ]

    rutinas_asignadas = len(rutinas_plan)

    # ------------------------------------------------------
    # ASISTENCIAS
    # ------------------------------------------------------

    asistencias_plan = [
        a for a in asistencias
        if a.get("planId") == plan_id
        and a.get("clienteId") == cliente_id
    ]

    sesiones_registradas = len(asistencias_plan)

    sesiones_asistidas = sum(
        1 for a in asistencias_plan
        if a.get("asistio") is True
    )

    rutinas_cumplidas = sum(
        1 for a in asistencias_plan
        if a.get("cumplioRutina") is True
    )

    porcentaje_asistencia = porcentaje(
        sesiones_asistidas,
        sesiones_registradas
    )

    porcentaje_cumplimiento = porcentaje(
        rutinas_cumplidas,
        sesiones_registradas
    )

    # ------------------------------------------------------
    # ENTRENAMIENTOS
    # ------------------------------------------------------

    entrenamientos_plan = [
        e for e in entrenamientos
        if e.get("planId") == plan_id
        and e.get("clienteId") == cliente_id
    ]

    entrenamientos_realizados = len(entrenamientos_plan)

    semanas_plan = dias_en_plan / 7 if dias_en_plan > 0 else 1

    frecuencia_semanal = round(
        entrenamientos_realizados / semanas_plan,
        2
    ) if semanas_plan > 0 else 0

    porcentajes_sesion = [
        e.get("porcentajeSesion")
        for e in entrenamientos_plan
        if e.get("porcentajeSesion") is not None
    ]

    promedio_porcentaje_sesion = promedio(
        porcentajes_sesion
    )

    total_ejercicios = sum(
        e.get("totalEjercicios", 0)
        for e in entrenamientos_plan
    )

    ejercicios_completados = sum(
        e.get("ejerciciosCompletados", 0)
        for e in entrenamientos_plan
    )

    pesos_usados = []

    for entrenamiento in entrenamientos_plan:
        for ejercicio in entrenamiento.get(
            "ejerciciosRealizados", []
        ):
            peso = ejercicio.get("pesoUsado")
            if peso is not None:
                pesos_usados.append(peso)

    promedio_peso_usado = promedio(pesos_usados)

    # ------------------------------------------------------
    # EVALUACIONES
    # ------------------------------------------------------

    evaluaciones_cliente = sorted(
        [
            e for e in evaluaciones
            if e.get("clienteId") == cliente_id
        ],
        key=lambda x: safe_fecha(x.get("fecha"))
    )

    cantidad_evaluaciones = len(evaluaciones_cliente)

    peso_inicial = 0
    peso_final = 0
    variacion_peso = 0

    imc_inicial = 0
    imc_final = 0
    variacion_imc = 0

    if evaluaciones_cliente:

        primera = evaluaciones_cliente[0]
        ultima = evaluaciones_cliente[-1]

        peso_inicial = primera.get("pesoCorporal", 0) or 0
        peso_final = ultima.get("pesoCorporal", 0) or 0
        variacion_peso = round(
            peso_final - peso_inicial,
            2
        )

        imc_inicial = primera.get("imc", 0) or 0
        imc_final = ultima.get("imc", 0) or 0
        variacion_imc = round(
            imc_final - imc_inicial,
            2
        )

    # ------------------------------------------------------
    # OBJETIVO PROVISIONAL
    # ------------------------------------------------------

    indice_adherencia = (
        porcentaje_asistencia * 0.50
        + porcentaje_cumplimiento * 0.30
        + promedio_porcentaje_sesion * 0.20
    )

    if indice_adherencia < 50:
        nivel_adherencia = "BAJA"
    elif indice_adherencia < 80:
        nivel_adherencia = "MEDIA"
    else:
        nivel_adherencia = "ALTA"

    filas_adherencia.append({
        "cliente_id": cliente_id,
        "plan_id": plan_id,
        "objetivo": plan.get("objetivo", ""),
        "dias_en_plan": dias_en_plan,
        "rutinas_asignadas": rutinas_asignadas,
        "sesiones_registradas": sesiones_registradas,
        "sesiones_asistidas": sesiones_asistidas,
        "porcentaje_asistencia": porcentaje_asistencia,
        "rutinas_cumplidas": rutinas_cumplidas,
        "porcentaje_cumplimiento":
            porcentaje_cumplimiento,
        "entrenamientos_realizados":
            entrenamientos_realizados,
        "frecuencia_semanal":
            frecuencia_semanal,
        "promedio_porcentaje_sesion":
            promedio_porcentaje_sesion,
        "ejercicios_realizados":
            total_ejercicios,
        "ejercicios_completados":
            ejercicios_completados,
        "promedio_peso_usado":
            promedio_peso_usado,
        "cantidad_evaluaciones":
            cantidad_evaluaciones,
        "peso_inicial":
            peso_inicial,
        "peso_final":
            peso_final,
        "variacion_peso":
            variacion_peso,
        "imc_inicial":
            imc_inicial,
        "imc_final":
            imc_final,
        "variacion_imc":
            variacion_imc,
        "nivel_adherencia":
            nivel_adherencia
    })


df_adherencia = pd.DataFrame(filas_adherencia)

df_adherencia.to_excel(
    "dataset_ironfit_adherencia.xlsx",
    index=False
)

# ==========================================================
# DATASET 2: RENDIMIENTO
# ==========================================================

filas_rendimiento = []

for plan in planes:

    plan_id = str(plan["_id"])
    cliente_id = plan.get("clienteId")

    if not cliente_id:
        continue

    objetivo = plan.get("objetivo", "")

    fecha_inicio = plan.get("fechaInicio")
    fecha_fin = plan.get("fechaFin")

    dias_en_plan = 0
    if fecha_inicio and fecha_fin:
        dias_en_plan = max(
            (fecha_fin - fecha_inicio).days,
            1
        )

    # ------------------------------------------------------
    # ENTRENAMIENTOS DEL PLAN
    # ------------------------------------------------------

    entrenamientos_plan = sorted(
        [
            e for e in entrenamientos
            if e.get("planId") == plan_id
            and e.get("clienteId") == cliente_id
        ],
        key=lambda x: safe_fecha(x.get("fecha"))
    )

    total_entrenamientos = len(
        entrenamientos_plan
    )

    semanas_plan = dias_en_plan / 7 if dias_en_plan > 0 else 1

    frecuencia_semanal = round(
        total_entrenamientos / semanas_plan,
        2
    ) if semanas_plan > 0 else 0

    porcentajes_sesion = [
        e.get("porcentajeSesion")
        for e in entrenamientos_plan
        if e.get("porcentajeSesion") is not None
    ]

    promedio_sesion = promedio(
        porcentajes_sesion
    )

    # ------------------------------------------------------
    # SERIES / REPETICIONES / CARGAS
    # ------------------------------------------------------

    series = []
    repeticiones = []

    pesos_iniciales = []
    pesos_finales = []

    # agrupamos ejercicios por ID para seguir la misma carga
    historial_ejercicios = defaultdict(list)

    for entrenamiento in entrenamientos_plan:

        fecha = entrenamiento.get("fecha")

        for ejercicio in entrenamiento.get(
            "ejerciciosRealizados", []
        ):

            ejercicio_id = ejercicio.get(
                "ejercicioId"
            )

            peso = ejercicio.get("pesoUsado")
            serie = ejercicio.get(
                "seriesRealizadas"
            )
            reps = ejercicio.get(
                "repeticionesRealizadas"
            )

            if serie is not None:
                series.append(serie)

            if reps is not None:
                repeticiones.append(reps)

            if ejercicio_id and peso is not None:
                historial_ejercicios[
                    ejercicio_id
                ].append(
                    (fecha, peso)
                )

    # ------------------------------------------------------
    # PROGRESIÓN DE CARGAS
    # ------------------------------------------------------

    progresiones = []

    for ejercicio_id, registros in \
            historial_ejercicios.items():

        registros = sorted(
            registros,
            key=lambda x: safe_fecha(x[0])
        )

        if len(registros) >= 2:

            peso_inicial = registros[0][1]
            peso_final = registros[-1][1]

            pesos_iniciales.append(
                peso_inicial
            )

            pesos_finales.append(
                peso_final
            )

            progresiones.append(
                peso_final - peso_inicial
            )

    promedio_peso_inicial = promedio(
        pesos_iniciales
    )

    promedio_peso_final = promedio(
        pesos_finales
    )

    variacion_carga = promedio(
        progresiones
    )

    if promedio_peso_inicial > 0:
        porcentaje_progresion_carga = round(
            (
                (
                    promedio_peso_final
                    - promedio_peso_inicial
                )
                / promedio_peso_inicial
            )
            * 100,
            2
        )
    else:
        porcentaje_progresion_carga = 0

    # ------------------------------------------------------
    # EVALUACIONES FÍSICAS
    # ------------------------------------------------------

    evaluaciones_cliente = sorted(
        [
            e for e in evaluaciones
            if e.get("clienteId") == cliente_id
        ],
        key=lambda x: safe_fecha(x.get("fecha"))
    )

    cantidad_evaluaciones = len(
        evaluaciones_cliente
    )

    peso_corporal_inicial = 0
    peso_corporal_final = 0
    variacion_peso = 0

    grasa_inicial = 0
    grasa_final = 0
    variacion_grasa = 0

    cintura_inicial = 0
    cintura_final = 0
    variacion_cintura = 0

    brazo_inicial = 0
    brazo_final = 0
    variacion_brazo = 0

    pecho_inicial = 0
    pecho_final = 0
    variacion_pecho = 0

    pierna_inicial = 0
    pierna_final = 0
    variacion_pierna = 0

    if evaluaciones_cliente:

        primera = evaluaciones_cliente[0]
        ultima = evaluaciones_cliente[-1]

        peso_corporal_inicial = (
            primera.get("pesoCorporal", 0)
            or 0
        )

        peso_corporal_final = (
            ultima.get("pesoCorporal", 0)
            or 0
        )

        variacion_peso = round(
            peso_corporal_final
            - peso_corporal_inicial,
            2
        )

        grasa_inicial = (
            primera.get(
                "porcentajeGraso", 0
            )
            or 0
        )

        grasa_final = (
            ultima.get(
                "porcentajeGraso", 0
            )
            or 0
        )

        variacion_grasa = round(
            grasa_final - grasa_inicial,
            2
        )

        cintura_inicial = obtener_medida(
            primera,
            "cintura"
        )

        cintura_final = obtener_medida(
            ultima,
            "cintura"
        )

        variacion_cintura = round(
            cintura_final
            - cintura_inicial,
            2
        )

        brazo_inicial = obtener_medida(
            primera,
            "brazo"
        )

        brazo_final = obtener_medida(
            ultima,
            "brazo"
        )

        variacion_brazo = round(
            brazo_final
            - brazo_inicial,
            2
        )

        pecho_inicial = obtener_medida(
            primera,
            "pecho"
        )

        pecho_final = obtener_medida(
            ultima,
            "pecho"
        )

        variacion_pecho = round(
            pecho_final
            - pecho_inicial,
            2
        )

        pierna_inicial = obtener_medida(
            primera,
            "pierna"
        )

        pierna_final = obtener_medida(
            ultima,
            "pierna"
        )

        variacion_pierna = round(
            pierna_final
            - pierna_inicial,
            2
        )

    # ------------------------------------------------------
    # CLASE PROVISIONAL DE RENDIMIENTO
    # ------------------------------------------------------

    puntos = 0

    objetivo_lower = objetivo.lower()

    if "bajar grasa" in objetivo_lower:

        if variacion_grasa < 0:
            puntos += 2

        if variacion_cintura < 0:
            puntos += 2

        if variacion_peso < 0:
            puntos += 1

    elif "masa muscular" in objetivo_lower:

        if variacion_brazo > 0:
            puntos += 2

        if variacion_pecho > 0:
            puntos += 1

        if variacion_pierna > 0:
            puntos += 1

        if variacion_carga > 0:
            puntos += 2

    elif "fuerza" in objetivo_lower:

        if variacion_carga > 0:
            puntos += 3

        if porcentaje_progresion_carga > 5:
            puntos += 2

        if promedio_sesion >= 80:
            puntos += 1

    elif "resistencia" in objetivo_lower:

        if promedio(repeticiones) >= 10:
            puntos += 2

        if promedio(series) >= 3:
            puntos += 2

        if frecuencia_semanal >= 2:
            puntos += 2

    else:

        if variacion_carga > 0:
            puntos += 2

        if promedio_sesion >= 80:
            puntos += 2

    if puntos <= 2:
        nivel_rendimiento = "BAJO"

    elif puntos <= 4:
        nivel_rendimiento = "MEDIO"

    else:
        nivel_rendimiento = "ALTO"

    # ------------------------------------------------------
    # GUARDAR FILA
    # ------------------------------------------------------

    filas_rendimiento.append({

        "cliente_id":
            cliente_id,

        "plan_id":
            plan_id,

        "objetivo":
            objetivo,

        "dias_en_plan":
            dias_en_plan,

        "entrenamientos_realizados":
            total_entrenamientos,

        "frecuencia_semanal":
            frecuencia_semanal,

        "promedio_porcentaje_sesion":
            promedio_sesion,

        "promedio_series":
            promedio(series),

        "promedio_repeticiones":
            promedio(repeticiones),

        "promedio_peso_inicial":
            promedio_peso_inicial,

        "promedio_peso_final":
            promedio_peso_final,

        "variacion_carga":
            variacion_carga,

        "porcentaje_progresion_carga":
            porcentaje_progresion_carga,

        "cantidad_evaluaciones":
            cantidad_evaluaciones,

        "peso_corporal_inicial":
            peso_corporal_inicial,

        "peso_corporal_final":
            peso_corporal_final,

        "variacion_peso":
            variacion_peso,

        "grasa_inicial":
            grasa_inicial,

        "grasa_final":
            grasa_final,

        "variacion_grasa":
            variacion_grasa,

        "cintura_inicial":
            cintura_inicial,

        "cintura_final":
            cintura_final,

        "variacion_cintura":
            variacion_cintura,

        "brazo_inicial":
            brazo_inicial,

        "brazo_final":
            brazo_final,

        "variacion_brazo":
            variacion_brazo,

        "pecho_inicial":
            pecho_inicial,

        "pecho_final":
            pecho_final,

        "variacion_pecho":
            variacion_pecho,

        "pierna_inicial":
            pierna_inicial,

        "pierna_final":
            pierna_final,

        "variacion_pierna":
            variacion_pierna,

        "nivel_rendimiento":
            nivel_rendimiento
    })


df_rendimiento = pd.DataFrame(
    filas_rendimiento
)

df_rendimiento.to_excel(
    "dataset_ironfit_rendimiento.xlsx",
    index=False
)

# ==========================================================
# RESUMEN
# ==========================================================

print()
print("==========================================")
print(" DATASETS IRONFIT GENERADOS")
print("==========================================")

print()
print("ADHERENCIA")
print("---------------------------")
print(
    "Registros:",
    len(df_adherencia)
)
print(
    "Columnas:",
    len(df_adherencia.columns)
)

print()
print(
    df_adherencia[
        "nivel_adherencia"
    ].value_counts()
)

print()
print("RENDIMIENTO")
print("---------------------------")
print(
    "Registros:",
    len(df_rendimiento)
)
print(
    "Columnas:",
    len(df_rendimiento.columns)
)

print()
print(
    df_rendimiento[
        "nivel_rendimiento"
    ].value_counts()
)

print()
print("Archivos:")
print(
    "- dataset_ironfit_adherencia.xlsx"
)
print(
    "- dataset_ironfit_rendimiento.xlsx"
)
