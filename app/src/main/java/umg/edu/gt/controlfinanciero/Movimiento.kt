
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

enum class TipoMovimiento {
    INGRESO,
    GASTO
}

@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: TipoMovimiento,
    val descripcion: String,
    val monto: BigDecimal,
    val categoria: String,
    val fecha: LocalDate = LocalDate.now()
)
