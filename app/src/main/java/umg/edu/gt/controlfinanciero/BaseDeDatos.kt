
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Movimiento::class,
        Categoria::class,
        Presupuesto::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Convertidores::class)
abstract class BaseDeDatos : RoomDatabase() {

    abstract fun movimientoDAO(): MovimientoDAO

    abstract fun categoriaDAO(): CategoriaDAO

    abstract fun presupuestoDAO(): PresupuestoDAO
}
