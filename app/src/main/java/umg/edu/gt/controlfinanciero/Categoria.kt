
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "categorias",
    indices = [
        androidx.room.Index(
            value = ["nombre", "tipo"],
            unique = true
        )
    ]
)
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: TipoMovimiento
) {
    init {
        require(nombre.isNotBlank()) {
            "El nombre de la categoría no puede estar vacío"
        }
    }
}
