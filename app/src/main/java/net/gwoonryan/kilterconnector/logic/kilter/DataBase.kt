package net.gwoonryan.kilterconnector.logic.kilter

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.FileOutputStream

enum class HoldType(val id: Int){
    BOLT_ON(1),
    SCREW_ON(20);

    companion object {
        fun fromId(id: Int): HoldType? =
            entries.firstOrNull { it.id == id }
    }
}

data class HoldLocation(
    val x: Int,
    val y: Int,
    val ledPosition: Int,
    val placementId: Long,
    val holdType: HoldType?
)

class DataBase(private val context: Context) {

    private val db: SQLiteDatabase

    init {
        ensureBundledDbCopied()
        db = SQLiteDatabase.openDatabase(
            context.getDatabasePath("kilter_data.db").path,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }

    private fun ensureBundledDbCopied(
        assetName: String = "kilter_data.db",
        dbName: String = "kilter_data.db"
    ) {
        val dbFile = context.getDatabasePath(dbName)

        if (dbFile.exists() && dbFile.length() > 0) return

        dbFile.parentFile?.mkdirs()

        context.assets.open(assetName).use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Runs:
     * SELECT holes.x, holes.y, leds.position, placements.id, placements.set_id FROM leds
     *   INNER JOIN holes ON leds.hole_id = holes.id
     *   INNER JOIN placements ON placements.hole_id = holes.id
     * WHERE placements.layout_id = 1                                = kilter board original
     *   AND leds.product_size_id = ?                                = 28 (16*12 superwide)
     *   AND holes.product_id = 1                                    = kilter board original
     */
    fun getHoldLocations(productSizeId: Int = 28): List<HoldLocation> {
        val sql = """
            SELECT holes.x, holes.y, leds.position, placements.id, placements.set_id
            FROM leds
            INNER JOIN holes ON leds.hole_id = holes.id
            INNER JOIN placements ON placements.hole_id = holes.id
            WHERE placements.layout_id = 1 
              AND leds.product_size_id = ?
              AND holes.product_id = 1
        """.trimIndent()

        val args = arrayOf(productSizeId.toString())
        val results = ArrayList<HoldLocation>()

        db.rawQuery(sql, args).use { cursor ->
            val xIdx = cursor.getColumnIndexOrThrow("x")
            val yIdx = cursor.getColumnIndexOrThrow("y")
            val posIdx = cursor.getColumnIndexOrThrow("position")
            val setIdx = cursor.getColumnIndexOrThrow("set_id")
            // column name will be "id" unless you alias it. We'll handle both.
            val idIdx = cursor.getColumnIndex("id").takeIf { it >= 0 }
                ?: cursor.getColumnIndexOrThrow("placement_id")

            while (cursor.moveToNext()) {
                results += HoldLocation(
                    x = cursor.getInt(xIdx),
                    y = cursor.getInt(yIdx),
                    ledPosition = cursor.getInt(posIdx),
                    placementId = cursor.getLong(idIdx),
                    holdType = HoldType.fromId(cursor.getInt(setIdx))
                )
            }
        }

        return results
    }

    /**
     * Optional: call when you're done with the DB (e.g., in onDestroy of a singleton owner).
     * If you keep this as a long-lived singleton, you may not need to call it often.
     */
    fun close() {
        if (db.isOpen) db.close()
    }
}
