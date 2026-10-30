
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.LocalDate

class Convertidores {

    @TypeConverter
    fun deTipoMovimiento(tipo: TipoMovimiento): String {
        return tipo.name
    }

    @TypeConverter
    fun aTipoMovimiento(tipo: String): TipoMovimiento {
        return TipoMovimiento.valueOf(tipo)
    }

    @TypeConverter
    fun deBigDecimal(monto: BigDecimal): String {
        return monto.toString()
    }

    @TypeConverter
    fun aBigDecimal(monto: String): BigDecimal {
        return BigDecimal(monto)
    }

    @TypeConverter
    fun deLocalDate(fecha: LocalDate): String {
        return fecha.toString()
    }

    @TypeConverter
    fun aLocalDate(fecha: String): LocalDate {
        return LocalDate.parse(fecha)
    }
}
