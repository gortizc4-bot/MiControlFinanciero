
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Entity

@Entity(
    tableName = "presupuestos",
    primaryKeys = ["anio", "mes"]
)
data class Presupuesto(
    val anio: Int,
    val mes: Int,
    val monto: java.math.BigDecimal
)
