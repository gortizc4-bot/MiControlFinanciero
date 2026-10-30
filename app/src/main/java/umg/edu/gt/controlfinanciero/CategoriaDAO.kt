
package umg.edu.gt.progra2.controlfinanciero

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoriaDAO {

    @Insert
    suspend fun guardar(categoria: Categoria): Long

    @Query("SELECT * FROM categorias ORDER BY nombre")
    suspend fun listarTodos(): List<Categoria>

    @Query(
        """
        SELECT * FROM categorias
        WHERE tipo = :tipo
        ORDER BY nombre
        """
    )
    suspend fun listarPorTipo(tipo: TipoMovimiento): List<Categoria>
}
