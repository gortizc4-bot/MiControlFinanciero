
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MovimientoDAO {

    @Insert
    suspend fun guardar(movimiento: Movimiento): Long

    @Update
    suspend fun actualizar(movimiento: Movimiento): Int

    @Delete
    suspend fun eliminar(movimiento: Movimiento): Int

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    suspend fun listarTodos(): List<Movimiento>

    @Query(
        "SELECT * FROM movimientos WHERE tipo = :tipo ORDER BY fecha DESC, id DESC"
    )
    suspend fun listarPorTipo(tipo: TipoMovimiento): List<Movimiento>

    @Query("SELECT * FROM movimientos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): Movimiento?
}
