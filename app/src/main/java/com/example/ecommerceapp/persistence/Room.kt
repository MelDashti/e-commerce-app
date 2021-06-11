package com.example.ecommerceapp.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Room.databaseBuilder
import com.example.ecommerceapp.api.main.responses.CartProduct
import com.example.ecommerceapp.api.main.responses.NetworkProduct
import com.example.ecommerceapp.domain.Product


@Entity(tableName = "product_table")
data class DatabaseProduct constructor(
    @PrimaryKey
    val productId: String,
    val imgSrcUrl: String,
    val price: Double,
    val name: String,
    val category: String,
    val description: String,
    var favorite: Int?
)

@Dao
interface CartProductDao {
    @Query("select * from cart_table")
    fun getProducts(): LiveData<List<CartProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCartProducts(products: List<CartProduct>?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCartProduct(product: CartProduct)

    @Query("delete from cart_table where id = :productId")
    fun delete(productId: String)

    @Query("update cart_table set quantity = quantity - 1 where id = :productId")
    fun updateQuantity(productId: String)

    @Query("delete from cart_table ")
    fun clear()

}

@Dao
interface ProductDao {

    @Query("select*from product_table where name like '%' || :value || '%' ")
    fun getSearchResult(value: String?): LiveData<List<DatabaseProduct>>

    @Query("select  * from product_table where productId = :productId")
    fun getSpecificProduct(productId: String): DatabaseProduct

    @Query("select * from product_table")
    fun getProducts(): LiveData<List<DatabaseProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProducts(products: List<DatabaseProduct>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProduct(product: DatabaseProduct)

    // for favorite products
    @Query("select * from product_table where favorite=1")
    fun getFavProducts(): LiveData<List<DatabaseProduct>>

    @Query("update product_table set favorite = 1 where productId = :productId")
    fun insertFavProduct(productId: String)

    @Query("UPDATE product_table SET favorite = 1 WHERE productId IN (:ids)")
    fun updateItemPlaces(ids: List<Int>)
//    But keep in mind if your list of ids contains more than 999 items SQLite will throw an exception:

    @Query("update product_table set favorite = 0 where productId = :productId")
    fun removeFavProduct(productId: String)

}

@Database(
    entities = [DatabaseProduct::class, CartProduct::class],
    version = 11,
    exportSchema = true
)
abstract class ProductDatabase : RoomDatabase() {
    abstract val productDao: ProductDao
    abstract val cartProductDao: CartProductDao

    companion object {
        @Volatile
        private var INSTANCE: ProductDatabase? = null
        fun getInstance(context: Context): ProductDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = databaseBuilder(
                        context.applicationContext,
                        ProductDatabase::class.java,
                        "shopfiesta_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

}


fun List<NetworkProduct>.asDomainModel2(): List<Product> {
    return map {
        Product(
            id = it.id,
            imgSrcUrl = it.imgSrcUrl,
            price = it.price,
            name = it.name,
            category = it.category,
            description = it.description

        )
    }
}


// here we create an extension function to convert database object to domain object. Domain object is used to update or change the ui.
fun List<DatabaseProduct>.asDomainModel(): List<Product> {
    return map {
        Product(
            id = it.productId,
            imgSrcUrl = it.imgSrcUrl,
            price = it.price,
            name = it.name,
            category = it.category,
            description = it.description
        )
    }
}


fun DatabaseProduct.asDomainModel() = Product(
    imgSrcUrl = imgSrcUrl,
    id = productId,
    price = price,
    name = name,
    category = category,
    description = description
)



