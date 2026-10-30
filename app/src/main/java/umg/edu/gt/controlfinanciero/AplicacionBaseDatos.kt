
package umg.edu.gt.progra2.controlfinanciero

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AplicacionBaseDatos {

    @Volatile
    private var INSTANCIA: BaseDeDatos? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS presupuestos (
                    anio INTEGER NOT NULL,
                    mes INTEGER NOT NULL,
                    monto TEXT NOT NULL,
                    PRIMARY KEY(anio, mes)
                )
                """.trimIndent()
            )
        }
    }

    fun obtener(context: Context): BaseDeDatos {

        return INSTANCIA ?: synchronized(this) {

            INSTANCIA ?: Room.databaseBuilder(
                context.applicationContext,
                BaseDeDatos::class.java,
                "mi_control_financiero.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also {
                    INSTANCIA = it
                }
        }
    }
}
