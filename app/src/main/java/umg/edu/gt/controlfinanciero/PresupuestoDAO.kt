
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PresupuestoDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(presupuesto: Presupuesto)

    @Query("""
        SELECT * FROM presupuestos
        WHERE anio = :anio AND mes = :mes
    """)
    suspend fun buscarPorMes(
        anio: Int,
        mes: Int
    ): Presupuesto?

    @Query("SELECT * FROM presupuestos ORDER BY anio DESC, mes DESC")
    suspend fun listarTodos(): List<Presupuesto>
}
