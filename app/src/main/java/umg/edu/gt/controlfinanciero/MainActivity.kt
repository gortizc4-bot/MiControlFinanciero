
package umg.edu.gt.controlfinanciero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import umg.edu.gt.controlfinanciero.ui.theme.MiControlFinancieroTheme
import umg.edu.gt.progra2.controlfinanciero.AplicacionBaseDatos
import umg.edu.gt.progra2.controlfinanciero.Categoria
import umg.edu.gt.progra2.controlfinanciero.ControlFinanciero
import umg.edu.gt.progra2.controlfinanciero.Movimiento
import umg.edu.gt.progra2.controlfinanciero.Presupuesto
import umg.edu.gt.progra2.controlfinanciero.TipoMovimiento
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch

// CONFIGURACIÓN VISUAL
private var colorAplicacion by mutableStateOf(Color(0xFF9575CD))
private var modoOscuroAplicacion by mutableStateOf(false)
private var monedaAplicacion by mutableStateOf("Q")

private val Lila: Color
    get() = colorAplicacion

private val LilaClaro: Color
    get() = if (modoOscuroAplicacion) Color(0xFF342B45) else Color(0xFFF3EDFF)

private val Blanco: Color
    get() = if (modoOscuroAplicacion) Color(0xFF18151F) else Color.White

private val TextoOscuro: Color
    get() = if (modoOscuroAplicacion) Color.White else Color(0xFF302746)

private val Verde = Color(0xFF388E3C)
private val Rojo = Color(0xFFD9534F)

private fun obtenerColorSeleccionado(nombre: String): Color {
    return when (nombre) {
        "rosa" -> Color(0xFFE88EAD)
        "azul" -> Color(0xFF64B5F6)
        "verde" -> Color(0xFF81C784)
        else -> Color(0xFF9575CD)
    }
}

data class MovimientoUI(
    val id: Long,
    val tipo: String,
    val monto: Double,
    val descripcion: String,
    val categoria: String,
    val fecha: String
)

// ACTIVIDAD PRINCIPAL
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configuracion = Configuracion(this)

        modoOscuroAplicacion = configuracion.obtenerModoOscuro()
        monedaAplicacion = configuracion.obtenerMoneda()
        colorAplicacion = obtenerColorSeleccionado(configuracion.obtenerColor())

        setContent {
            MiControlFinancieroTheme(
                darkTheme = modoOscuroAplicacion,
                primaryColor = colorAplicacion
            ) {
                PantallaPrincipal(configuracion)
            }
        }
    }
}

// PANTALLA PRINCIPAL
@Composable
fun PantallaPrincipal(configuracion: Configuracion) {
    val scope = rememberCoroutineScope()
    val contexto = androidx.compose.ui.platform.LocalContext.current

    val baseDatos = remember(contexto) {
        AplicacionBaseDatos.obtener(contexto)
    }

    val categoriaDAO = remember { baseDatos.categoriaDAO() }
    val presupuestoDAO = remember { baseDatos.presupuestoDAO() }

    val controlFinanciero = remember {
        ControlFinanciero(baseDatos.movimientoDAO(), categoriaDAO)
    }

    val movimientos = remember { mutableStateListOf<MovimientoUI>() }

    var categoriasIngreso by remember { mutableStateOf<List<String>>(emptyList()) }
    var categoriasGasto by remember { mutableStateOf<List<String>>(emptyList()) }
    var presupuestoActual by remember { mutableStateOf<Double?>(null) }

    var nombreUsuario by remember {
        mutableStateOf(configuracion.obtenerNombreUsuario())
    }

    var metaAhorro by remember {
        mutableStateOf(configuracion.obtenerMetaAhorro())
    }

    var pantalla by remember { mutableStateOf("inicio") }
    var mensajeError by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }

    var movimientoEditar by remember { mutableStateOf<Movimiento?>(null) }
    var mostrarConfirmacionEliminar by remember {
        mutableStateOf<Movimiento?>(null)
    }

    fun actualizarLista(lista: List<Movimiento>) {
        movimientos.clear()
        movimientos.addAll(lista.map {
            MovimientoUI(
                id = it.id,
                tipo = if (it.tipo == TipoMovimiento.INGRESO) "Ingreso" else "Gasto",
                monto = it.monto.toDouble(),
                descripcion = it.descripcion,
                categoria = it.categoria,
                fecha = it.fecha.toString()
            )
        })
    }

    // CARGAR DATOS
    LaunchedEffect(Unit) {
        try {
            actualizarLista(controlFinanciero.listarTodos())

            val categoriasIngresosIniciales = listOf(
                "Salario", "Ventas", "Bonos", "Otros"
            )

            val categoriasGastosIniciales = listOf(
                "Alimentación", "Transporte", "Estudios",
                "Entretenimiento", "Salud", "Otros"
            )

            val ingresosExistentes = categoriaDAO
                .listarPorTipo(TipoMovimiento.INGRESO)
                .map { it.nombre }
                .toSet()

            categoriasIngresosIniciales
                .filterNot { it in ingresosExistentes }
                .forEach { nombre ->
                    categoriaDAO.guardar(
                        Categoria(nombre = nombre, tipo = TipoMovimiento.INGRESO)
                    )
                }

            val gastosExistentes = categoriaDAO
                .listarPorTipo(TipoMovimiento.GASTO)
                .map { it.nombre }
                .toSet()

            categoriasGastosIniciales
                .filterNot { it in gastosExistentes }
                .forEach { nombre ->
                    categoriaDAO.guardar(
                        Categoria(nombre = nombre, tipo = TipoMovimiento.GASTO)
                    )
                }

            categoriasIngreso = categoriaDAO
                .listarPorTipo(TipoMovimiento.INGRESO)
                .map { it.nombre }

            categoriasGasto = categoriaDAO
                .listarPorTipo(TipoMovimiento.GASTO)
                .map { it.nombre }

            val hoy = LocalDate.now()
            val presupuestoGuardado = presupuestoDAO.buscarPorMes(
                anio = hoy.year,
                mes = hoy.monthValue
            )

            presupuestoActual = presupuestoGuardado?.monto?.toDouble()
            mensajeError = ""
        } catch (e: Exception) {
            mensajeError = e.message ?: "No se pudieron cargar los datos."
        } finally {
            cargando = false
        }
    }

    // REGISTRAR MOVIMIENTO
    fun guardarMovimiento(
        tipo: TipoMovimiento,
        monto: Double,
        descripcion: String,
        categoria: String,
        fecha: LocalDate
    ) {
        scope.launch {
            try {
                controlFinanciero.registrar(
                    Movimiento(
                        tipo = tipo,
                        descripcion = descripcion,
                        monto = BigDecimal.valueOf(monto),
                        categoria = categoria,
                        fecha = fecha
                    )
                )
                actualizarLista(controlFinanciero.listarTodos())
                mensajeError = ""
                pantalla = "inicio"
            } catch (e: Exception) {
                mensajeError = e.message ?: "No se pudo guardar el movimiento."
            }
        }
    }

    // EDITAR MOVIMIENTO
    fun editarMovimiento(movimiento: Movimiento) {
        scope.launch {
            try {
                controlFinanciero.actualizar(movimiento)
                actualizarLista(controlFinanciero.listarTodos())
                movimientoEditar = null
                mensajeError = ""
                pantalla = "historial"
            } catch (e: Exception) {
                mensajeError = e.message ?: "No se pudo actualizar."
            }
        }
    }

    // ELIMINAR MOVIMIENTO
    fun eliminarMovimiento(movimiento: Movimiento) {
        scope.launch {
            try {
                controlFinanciero.eliminar(movimiento)
                actualizarLista(controlFinanciero.listarTodos())
                mostrarConfirmacionEliminar = null
                mensajeError = ""
            } catch (e: Exception) {
                mensajeError = e.message ?: "No se pudo eliminar."
            }
        }
    }

    // CÁLCULOS GENERALES
    val ingresos = movimientos
        .filter { it.tipo == "Ingreso" }
        .sumOf { it.monto }

    val gastos = movimientos
        .filter { it.tipo == "Gasto" }
        .sumOf { it.monto }

    val hoy = LocalDate.now()

    val ingresosDelMes = movimientos
        .filter {
            it.tipo == "Ingreso" &&
                    LocalDate.parse(it.fecha).year == hoy.year &&
                    LocalDate.parse(it.fecha).monthValue == hoy.monthValue
        }
        .sumOf { it.monto }

    val gastosDelMes = movimientos
        .filter {
            it.tipo == "Gasto" &&
                    LocalDate.parse(it.fecha).year == hoy.year &&
                    LocalDate.parse(it.fecha).monthValue == hoy.monthValue
        }
        .sumOf { it.monto }

    val ahorroDelMes = (ingresosDelMes - gastosDelMes).coerceAtLeast(0.0)

    val progresoAhorro = if (metaAhorro > 0) {
        (ahorroDelMes / metaAhorro).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    // NAVEGACIÓN
    when (pantalla) {
        "ingreso" -> FormularioMovimiento(
            tipo = "Ingreso",
            color = Verde,
            categoriasDisponibles = categoriasIngreso,
            alVolver = {
                mensajeError = ""
                pantalla = "inicio"
            },
            alGuardar = { monto, descripcion, categoria, fecha ->
                guardarMovimiento(
                    TipoMovimiento.INGRESO,
                    monto,
                    descripcion,
                    categoria,
                    fecha
                )
            }
        )

        "gasto" -> FormularioMovimiento(
            tipo = "Gasto",
            color = Rojo,
            categoriasDisponibles = categoriasGasto,
            alVolver = {
                mensajeError = ""
                pantalla = "inicio"
            },
            alGuardar = { monto, descripcion, categoria, fecha ->
                guardarMovimiento(
                    TipoMovimiento.GASTO,
                    monto,
                    descripcion,
                    categoria,
                    fecha
                )
            }
        )

        "historial" -> PantallaHistorial(
            movimientos = movimientos,
            alVolver = { pantalla = "inicio" },
            alEditar = { movimiento ->
                movimientoEditar = Movimiento(
                    id = movimiento.id,
                    tipo = if (movimiento.tipo == "Ingreso")
                        TipoMovimiento.INGRESO else TipoMovimiento.GASTO,
                    descripcion = movimiento.descripcion,
                    monto = BigDecimal.valueOf(movimiento.monto),
                    categoria = movimiento.categoria,
                    fecha = LocalDate.parse(movimiento.fecha)
                )
                pantalla = "editar"
            },
            alEliminar = { movimiento ->
                mostrarConfirmacionEliminar = Movimiento(
                    id = movimiento.id,
                    tipo = if (movimiento.tipo == "Ingreso")
                        TipoMovimiento.INGRESO else TipoMovimiento.GASTO,
                    descripcion = movimiento.descripcion,
                    monto = BigDecimal.valueOf(movimiento.monto),
                    categoria = movimiento.categoria,
                    fecha = LocalDate.parse(movimiento.fecha)
                )
            }
        )

        "editar" -> {
            val movimiento = movimientoEditar
            if (movimiento != null) {
                FormularioMovimiento(
                    tipo = if (movimiento.tipo == TipoMovimiento.INGRESO)
                        "Ingreso" else "Gasto",
                    color = if (movimiento.tipo == TipoMovimiento.INGRESO)
                        Verde else Rojo,
                    categoriasDisponibles = if (
                        movimiento.tipo == TipoMovimiento.INGRESO
                    ) categoriasIngreso else categoriasGasto,
                    movimientoExistente = movimiento,
                    alVolver = {
                        movimientoEditar = null
                        pantalla = "historial"
                    },
                    alGuardar = { monto, descripcion, categoria, fecha ->
                        editarMovimiento(
                            movimiento.copy(
                                monto = BigDecimal.valueOf(monto),
                                descripcion = descripcion,
                                categoria = categoria,
                                fecha = fecha
                            )
                        )
                    }
                )
            } else {
                pantalla = "historial"
            }
        }

        "presupuesto" -> PantallaPresupuesto(
            presupuestoActual = presupuestoActual,
            gastosDelMes = gastosDelMes,
            alVolver = { pantalla = "inicio" },
            alGuardar = { monto ->
                scope.launch {
                    try {
                        val fechaActual = LocalDate.now()
                        presupuestoDAO.guardar(
                            Presupuesto(
                                anio = fechaActual.year,
                                mes = fechaActual.monthValue,
                                monto = BigDecimal.valueOf(monto)
                            )
                        )
                        presupuestoActual = monto
                        mensajeError = ""
                        pantalla = "inicio"
                    } catch (e: Exception) {
                        mensajeError = e.message ?: "No se pudo guardar el presupuesto."
                    }
                }
            }
        )

        "configuracion" -> PantallaConfiguracion(
            modoOscuro = modoOscuroAplicacion,
            colorSeleccionado = configuracion.obtenerColor(),
            moneda = monedaAplicacion,
            nombreUsuario = nombreUsuario,
            metaAhorro = metaAhorro,
            alVolver = { pantalla = "inicio" },
            alCambiarModo = { activado ->
                configuracion.guardarModoOscuro(activado)
                modoOscuroAplicacion = activado
            },
            alCambiarColor = { color ->
                configuracion.guardarColor(color)
                colorAplicacion = obtenerColorSeleccionado(color)
            },
            alCambiarMoneda = { moneda ->
                configuracion.guardarMoneda(moneda)
                monedaAplicacion = moneda
            },
            alGuardarNombre = { nombre ->
                configuracion.guardarNombreUsuario(nombre)
                nombreUsuario = nombre
            },
            alGuardarMeta = { meta ->
                configuracion.guardarMetaAhorro(meta)
                metaAhorro = meta
            }
        )

        // PANTALLA DE INICIO
        else -> Scaffold(
            containerColor = Blanco
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Blanco)
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column {
                    Text(
                        text = "Mi Control",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lila
                    )
                    Text(
                        text = "Financiero",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextoOscuro
                    )
                    Text(
                        text = if (nombreUsuario.isNotBlank())
                            "¡Hola, $nombreUsuario! 💜"
                        else
                            "Organiza tu dinero con tranquilidad 💜",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // BALANCE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Lila)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "BALANCE DISPONIBLE",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            formatoDinero(ingresos - gastos),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tu resumen financiero",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }

                // INGRESOS Y GASTOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TarjetaResumen(
                        titulo = "Ingresos",
                        monto = formatoDinero(ingresos),
                        color = Verde,
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaResumen(
                        titulo = "Gastos",
                        monto = formatoDinero(gastos),
                        color = Rojo,
                        modifier = Modifier.weight(1f)
                    )
                }

                // META DE AHORRO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LilaClaro)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Mi meta de ahorro 💰",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )

                        Text(
                            text = if (metaAhorro > 0) {
                                "${formatoDinero(ahorroDelMes)} de ${formatoDinero(metaAhorro)}"
                            } else {
                                "Todavía no has establecido una meta."
                            },
                            color = TextoOscuro
                        )

                        if (metaAhorro > 0) {
                            LinearProgressIndicator(
                                progress = { progresoAhorro },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                                color = Verde,
                                trackColor = Blanco
                            )
                            Text(
                                "${(progresoAhorro * 100).toInt()}% de tu meta alcanzada",
                                color = TextoOscuro
                            )
                        }
                    }
                }

                if (mensajeError.isNotBlank()) {
                    Text(mensajeError, color = Rojo, fontSize = 14.sp)
                }

                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Lila
                    )
                }

                Text(
                    "¿Qué deseas hacer?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )

                BotonInicio("＋ Registrar ingreso", Lila) {
                    mensajeError = ""
                    pantalla = "ingreso"
                }

                BotonInicio("− Registrar gasto", LilaClaro, TextoOscuro) {
                    mensajeError = ""
                    pantalla = "gasto"
                }

                BotonInicio("▤ Ver historial", LilaClaro, Lila) {
                    mensajeError = ""
                    pantalla = "historial"
                }

                BotonInicio("♡ Presupuesto mensual", Lila) {
                    mensajeError = ""
                    pantalla = "presupuesto"
                }

                BotonInicio("⚙ Configuración", LilaClaro, TextoOscuro) {
                    pantalla = "configuracion"
                }

                Text(
                    "Pequeños pasos, grandes metas ✨",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Lila,
                    fontSize = 13.sp
                )
            }
        }
    }

    // CONFIRMAR ELIMINACIÓN
    if (mostrarConfirmacionEliminar != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = null },
            title = { Text("Eliminar movimiento") },
            text = {
                Text("¿Estás segura de que deseas eliminar este movimiento? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacionEliminar?.let {
                            eliminarMovimiento(it)
                        }
                    }
                ) {
                    Text("Eliminar", color = Rojo)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarConfirmacionEliminar = null }
                ) {
                    Text("Cancelar", color = Lila)
                }
            }
        )
    }
}

// BOTONES DE LA PANTALLA PRINCIPAL
@Composable
fun BotonInicio(
    texto: String,
    color: Color,
    colorTexto: Color = Color.White,
    alPresionar: () -> Unit
) {
    Button(
        onClick = alPresionar,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = colorTexto
        )
    ) {
        Text(texto)
    }
}

// FORMULARIO DE MOVIMIENTOS
@Composable
fun FormularioMovimiento(
    tipo: String,
    color: Color,
    categoriasDisponibles: List<String>,
    alVolver: () -> Unit,
    alGuardar: (Double, String, String, LocalDate) -> Unit,
    movimientoExistente: Movimiento? = null
) {
    val categorias = categoriasDisponibles

    var montoTexto by remember(tipo, movimientoExistente?.id) {
        mutableStateOf(movimientoExistente?.monto?.toPlainString() ?: "")
    }

    var descripcion by remember(tipo, movimientoExistente?.id) {
        mutableStateOf(movimientoExistente?.descripcion ?: "")
    }

    var categoriaSeleccionada by remember(
        tipo, movimientoExistente?.id, categoriasDisponibles
    ) {
        mutableStateOf(
            movimientoExistente?.categoria ?: categorias.firstOrNull().orEmpty()
        )
    }

    var fechaTexto by remember(tipo, movimientoExistente?.id) {
        mutableStateOf(
            movimientoExistente?.fecha?.toString() ?: LocalDate.now().toString()
        )
    }

    var mostrarCategorias by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Scaffold(containerColor = Blanco) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TextButton(onClick = alVolver) {
                Text("← Volver", color = Lila)
            }

            Text(
                if (movimientoExistente == null) "Registrar $tipo" else "Editar $tipo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Lila
            )

            Text("Completa los datos de tu $tipo.", color = TextoOscuro)

            OutlinedTextField(
                value = montoTexto,
                onValueChange = {
                    montoTexto = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monto ($monedaAplicacion)") },
                placeholder = { Text("Ej. 150.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    descripcion = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                placeholder = { Text("Ej. Transporte, salario...") },
                singleLine = true
            )

            Column {
                Text("Categoría", color = TextoOscuro, fontWeight = FontWeight.Medium)

                Box {
                    OutlinedButton(
                        onClick = { mostrarCategorias = true },
                        enabled = categorias.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            categoriaSeleccionada.ifBlank {
                                "Selecciona una categoría"
                            },
                            color = TextoOscuro
                        )
                    }

                    DropdownMenu(
                        expanded = mostrarCategorias,
                        onDismissRequest = { mostrarCategorias = false }
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria) },
                                onClick = {
                                    categoriaSeleccionada = categoria
                                    mostrarCategorias = false
                                }
                            )
                        }
                    }
                }

                if (categorias.isEmpty()) {
                    Text("No hay categorías disponibles.", color = Rojo, fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = fechaTexto,
                onValueChange = {
                    fechaTexto = it
                    error = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Fecha (AAAA-MM-DD)") },
                placeholder = { Text("2026-10-29") },
                singleLine = true
            )

            Text(
                "Utiliza el formato año-mes-día.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (error.isNotBlank()) {
                Text(error, color = Rojo, fontSize = 14.sp)
            }

            Button(
                onClick = {
                    val monto = montoTexto.trim().replace(",", ".").toDoubleOrNull()
                    val fecha = try {
                        LocalDate.parse(fechaTexto.trim())
                    } catch (e: Exception) {
                        null
                    }

                    when {
                        monto == null || !monto.isFinite() || monto <= 0.0 ->
                            error = "Ingresa un monto válido mayor que cero."

                        descripcion.isBlank() ->
                            error = "Escribe una descripción."

                        categorias.isEmpty() ->
                            error = "No hay categorías registradas."

                        categoriaSeleccionada.isBlank() ->
                            error = "Selecciona una categoría."

                        fecha == null ->
                            error = "La fecha no es válida. Usa AAAA-MM-DD."

                        else -> alGuardar(
                            monto,
                            descripcion.trim(),
                            categoriaSeleccionada,
                            fecha
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text(
                    if (movimientoExistente == null) "Guardar $tipo"
                    else "Actualizar $tipo"
                )
            }
        }
    }
}

// PRESUPUESTO MENSUAL
@Composable
fun PantallaPresupuesto(
    presupuestoActual: Double?,
    gastosDelMes: Double,
    alVolver: () -> Unit,
    alGuardar: (Double) -> Unit
) {
    var montoTexto by remember(presupuestoActual) {
        mutableStateOf(presupuestoActual?.toString() ?: "")
    }

    var error by remember { mutableStateOf("") }

    val presupuesto = presupuestoActual ?: 0.0
    val restante = presupuesto - gastosDelMes

    val porcentaje = if (presupuesto > 0) {
        (gastosDelMes / presupuesto).toFloat().coerceIn(0f, 1f)
    } else 0f

    val porcentajeReal = if (presupuesto > 0) {
        gastosDelMes / presupuesto * 100
    } else 0.0

    val hoy = LocalDate.now()

    Scaffold(containerColor = Blanco) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextButton(onClick = alVolver) {
                Text("← Volver", color = Lila)
            }

            Text(
                "Presupuesto mensual",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Lila
            )

            Text("Mes: ${hoy.monthValue}/${hoy.year}", color = Color.Gray)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Tu límite de gastos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    Text(
                        if (presupuestoActual != null) formatoDinero(presupuesto)
                        else "Aún no establecido",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lila
                    )

                    OutlinedTextField(
                        value = montoTexto,
                        onValueChange = {
                            montoTexto = it
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Presupuesto en $monedaAplicacion") },
                        prefix = { Text(monedaAplicacion) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            val monto = montoTexto.trim()
                                .replace(",", ".").toDoubleOrNull()

                            if (monto == null || !monto.isFinite() || monto <= 0.0) {
                                error = "Ingresa un presupuesto válido."
                            } else {
                                alGuardar(monto)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Lila)
                    ) {
                        Text("Guardar presupuesto")
                    }

                    if (error.isNotBlank()) {
                        Text(error, color = Rojo)
                    }
                }
            }

            Text(
                "Resumen del mes",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )

            TarjetaResumen(
                "Total gastado",
                formatoDinero(gastosDelMes),
                Rojo,
                Modifier.fillMaxWidth()
            )

            TarjetaResumen(
                "Dinero restante",
                if (presupuestoActual != null) formatoDinero(restante)
                else "Establece un presupuesto",
                if (restante >= 0) Verde else Rojo,
                Modifier.fillMaxWidth()
            )

            if (presupuestoActual != null) {
                Text(
                    "Uso de tu presupuesto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )

                LinearProgressIndicator(
                    progress = { porcentaje },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (porcentajeReal >= 100) Rojo else Lila,
                    trackColor = LilaClaro
                )

                Text(
                    String.format(Locale.US, "%.1f%% utilizado", porcentajeReal),
                    color = TextoOscuro
                )

                when {
                    porcentajeReal >= 100 -> Text(
                        "Has alcanzado o superado tu presupuesto.",
                        color = Rojo,
                        fontWeight = FontWeight.Bold
                    )

                    porcentajeReal >= 80 -> Text(
                        "¡Atención! Estás cerca de alcanzar tu límite.",
                        color = Color(0xFFB7791F),
                        fontWeight = FontWeight.Bold
                    )

                    else -> Text(
                        "¡Vas bien! Todavía tienes presupuesto disponible.",
                        color = Verde
                    )
                }
            }
        }
    }
}

// CONFIGURACIÓN Y PERSONALIZACIÓN
@Composable
fun PantallaConfiguracion(
    modoOscuro: Boolean,
    colorSeleccionado: String,
    moneda: String,
    nombreUsuario: String,
    metaAhorro: Double,
    alVolver: () -> Unit,
    alCambiarModo: (Boolean) -> Unit,
    alCambiarColor: (String) -> Unit,
    alCambiarMoneda: (String) -> Unit,
    alGuardarNombre: (String) -> Unit,
    alGuardarMeta: (Double) -> Unit
) {
    var nombreTexto by remember(nombreUsuario) {
        mutableStateOf(nombreUsuario)
    }

    var metaTexto by remember(metaAhorro) {
        mutableStateOf(if (metaAhorro > 0) metaAhorro.toString() else "")
    }

    var errorMeta by remember { mutableStateOf("") }
    var mensajeNombre by remember { mutableStateOf("") }

    Scaffold(containerColor = Blanco) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TextButton(onClick = alVolver) {
                Text("← Volver", color = Lila)
            }

            Text(
                "Configuración",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Lila
            )

            Text("Personaliza tu aplicación", color = TextoOscuro)

            // PERFIL
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Mi perfil",
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    OutlinedTextField(
                        value = nombreTexto,
                        onValueChange = {
                            nombreTexto = it
                            mensajeNombre = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("¿Cómo te llamas?")
                        },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            alGuardarNombre(nombreTexto.trim())
                            mensajeNombre = "Nombre guardado correctamente."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Lila)
                    ) {
                        Text("Guardar nombre")
                    }

                    if (mensajeNombre.isNotBlank()) {
                        Text(mensajeNombre, color = Verde)
                    }
                }
            }

            // META DE AHORRO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Mi meta de ahorro",
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    OutlinedTextField(
                        value = metaTexto,
                        onValueChange = {
                            metaTexto = it
                            errorMeta = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Meta de ahorro ($moneda)") },
                        placeholder = { Text("Ej. 1000.00") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val meta = metaTexto.trim()
                                .replace(",", ".").toDoubleOrNull()

                            if (meta == null || !meta.isFinite() || meta <= 0.0) {
                                errorMeta = "Ingresa una meta válida mayor que cero."
                            } else {
                                alGuardarMeta(meta)
                                errorMeta = "Meta de ahorro guardada correctamente."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Lila)
                    ) {
                        Text("Guardar meta de ahorro")
                    }

                    if (errorMeta.isNotBlank()) {
                        Text(
                            errorMeta,
                            color = if (errorMeta.startsWith("Meta")) Verde else Rojo
                        )
                    }
                }
            }

            // APARIENCIA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Apariencia",
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (modoOscuro) "Modo oscuro" else "Modo claro",
                            color = TextoOscuro
                        )
                        Switch(
                            checked = modoOscuro,
                            onCheckedChange = alCambiarModo
                        )
                    }
                }
            }

            // COLORES
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Color de la aplicación",
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    listOf(
                        "lila" to "Lila",
                        "rosa" to "Rosa",
                        "azul" to "Azul",
                        "verde" to "Verde"
                    ).forEach { (valor, nombre) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = colorSeleccionado == valor,
                                onClick = { alCambiarColor(valor) }
                            )
                            Text(nombre, color = TextoOscuro)
                        }
                    }
                }
            }

            // MONEDA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Moneda",
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    listOf(
                        "Q" to "Quetzales (Q)",
                        "$" to "Dólares ($)",
                        "€" to "Euros (€)"
                    ).forEach { (valor, nombre) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = moneda == valor,
                                onClick = { alCambiarMoneda(valor) }
                            )
                            Text(nombre, color = TextoOscuro)
                        }
                    }
                }
            }

            // INFORMACIÓN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LilaClaro)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Mi Control Financiero",
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )
                    Text(
                        "Aplicación para organizar ingresos, gastos y presupuestos.",
                        color = TextoOscuro
                    )
                    Text("Versión 1.0", color = Color.Gray)
                }
            }
        }
    }
}

// HISTORIAL DE MOVIMIENTOS
@Composable
fun PantallaHistorial(
    movimientos: List<MovimientoUI>,
    alVolver: () -> Unit,
    alEditar: (MovimientoUI) -> Unit,
    alEliminar: (MovimientoUI) -> Unit
) {
    Scaffold(containerColor = Blanco) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TextButton(onClick = alVolver) {
                Text("← Volver", color = Lila)
            }

            Text(
                "Historial",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Lila
            )

            if (movimientos.isEmpty()) {
                Text(
                    "Todavía no tienes movimientos registrados.",
                    color = TextoOscuro
                )
            } else {
                movimientos.asReversed().forEach { movimiento ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LilaClaro)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                movimiento.descripcion,
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )

                            Text(
                                movimiento.tipo,
                                color = if (movimiento.tipo == "Ingreso") Verde else Rojo
                            )

                            Text(
                                "Categoría: ${movimiento.categoria}",
                                color = TextoOscuro
                            )

                            Text("Fecha: ${movimiento.fecha}", color = TextoOscuro)

                            Text(
                                formatoDinero(movimiento.monto),
                                fontWeight = FontWeight.Bold,
                                color = TextoOscuro
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { alEditar(movimiento) }
                                ) {
                                    Text("Editar", color = Lila)
                                }

                                OutlinedButton(
                                    onClick = { alEliminar(movimiento) }
                                ) {
                                    Text("Eliminar", color = Rojo)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TARJETAS DE RESUMEN
@Composable
fun TarjetaResumen(
    titulo: String,
    monto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LilaClaro)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(titulo, fontSize = 14.sp, color = TextoOscuro)
            Text(
                monto,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// FORMATO DE DINERO
private fun formatoDinero(monto: Double): String {
    return String.format(
        Locale.US,
        "%s %.2f",
        monedaAplicacion,
        monto
    )
}
