
package umg.edu.gt.progra2.controlfinanciero

import java.math.BigDecimal
import java.time.LocalDate

class ControlFinanciero(
    private val movimientoDAO: MovimientoDAO,
    private val categoriaDAO: CategoriaDAO
) {

    suspend fun registrarCategoria(categoria: Categoria) {
        require(categoria.nombre.isNotBlank()) {
            "El nombre de la categoría no puede estar vacío"
        }

        val nombre = categoria.nombre.trim()

        val existente = categoriaDAO.listarTodos().any {
            it.nombre.equals(nombre, ignoreCase = true) &&
                    it.tipo == categoria.tipo
        }

        require(!existente) {
            "Esta categoría ya existe para ese tipo"
        }

        categoriaDAO.guardar(
            categoria.copy(nombre = nombre)
        )
    }

    suspend fun listarCategorias(): List<Categoria> {
        return categoriaDAO.listarTodos()
    }

    suspend fun listarCategoriasPorTipo(
        tipo: TipoMovimiento
    ): List<Categoria> {
        return categoriaDAO.listarPorTipo(tipo)
    }

    suspend fun registrar(movimiento: Movimiento) {
        require(movimiento.descripcion.isNotBlank()) {
            "La descripción no puede estar vacía"
        }

        require(movimiento.monto > BigDecimal.ZERO) {
            "El monto debe ser mayor que cero"
        }

        require(movimiento.categoria.isNotBlank()) {
            "La categoría no puede estar vacía"
        }

        movimientoDAO.guardar(movimiento)
    }

    suspend fun listarTodos(): List<Movimiento> {
        return movimientoDAO.listarTodos()
    }

    suspend fun listarHistorial(): List<Movimiento> {
        return movimientoDAO.listarTodos()
    }

    suspend fun listarPorTipo(
        tipo: TipoMovimiento
    ): List<Movimiento> {
        return movimientoDAO.listarPorTipo(tipo)
    }

    suspend fun calcularTotal(tipo: TipoMovimiento): BigDecimal {
        val hoy = LocalDate.now()

        return movimientoDAO.listarPorTipo(tipo)
            .filter {
                it.fecha.year == hoy.year &&
                        it.fecha.month == hoy.month
            }
            .fold(BigDecimal.ZERO) { total, movimiento ->
                total.add(movimiento.monto)
            }
    }

    suspend fun calcularBalance(): BigDecimal {
        val ingresos = calcularTotal(TipoMovimiento.INGRESO)
        val gastos = calcularTotal(TipoMovimiento.GASTO)

        return ingresos.subtract(gastos)
    }


    suspend fun actualizar(movimiento: Movimiento) {
        require(movimiento.id > 0) {
            "El movimiento no es válido."
        }
        require(movimiento.descripcion.isNotBlank()) {
            "La descripción no puede estar vacía."
        }
        require(movimiento.monto > BigDecimal.ZERO) {
            "El monto debe ser mayor que cero."
        }
        require(movimiento.categoria.isNotBlank()) {
            "La categoría no puede estar vacía."
        }

        val filasActualizadas = movimientoDAO.actualizar(movimiento)

        require(filasActualizadas > 0) {
            "No se encontró el movimiento para actualizar."
        }
    }

    suspend fun eliminar(movimiento: Movimiento) {
        val filasEliminadas = movimientoDAO.eliminar(movimiento)

        require(filasEliminadas > 0) {
            "No se encontró el movimiento para eliminar."
        }
    }

}
