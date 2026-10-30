
package umg.edu.gt.controlfinanciero

import android.content.Context

class Configuracion(context: Context) {

    private val preferencias = context.getSharedPreferences(
        "configuracion_app",
        Context.MODE_PRIVATE
    )

    // MODO OSCURO
    fun guardarModoOscuro(activado: Boolean) {
        preferencias.edit()
            .putBoolean("modo_oscuro", activado)
            .apply()
    }

    fun obtenerModoOscuro(): Boolean {
        return preferencias.getBoolean("modo_oscuro", false)
    }

    // COLOR PRINCIPAL
    fun guardarColor(color: String) {
        preferencias.edit()
            .putString("color_principal", color)
            .apply()
    }

    fun obtenerColor(): String {
        return preferencias.getString("color_principal", "lila")
            ?: "lila"
    }

    // MONEDA
    fun guardarMoneda(moneda: String) {
        preferencias.edit()
            .putString("moneda", moneda)
            .apply()
    }

    fun obtenerMoneda(): String {
        return preferencias.getString("moneda", "Q")
            ?: "Q"
    }

    // NOMBRE DEL USUARIO
    fun guardarNombreUsuario(nombre: String) {
        preferencias.edit()
            .putString("nombre_usuario", nombre)
            .apply()
    }

    fun obtenerNombreUsuario(): String {
        return preferencias.getString("nombre_usuario", "")
            ?: ""
    }

    // META DE AHORRO
    fun guardarMetaAhorro(meta: Double) {
        preferencias.edit()
            .putFloat("meta_ahorro", meta.toFloat())
            .apply()
    }

    fun obtenerMetaAhorro(): Double {
        return preferencias.getFloat("meta_ahorro", 0f).toDouble()
    }
}
