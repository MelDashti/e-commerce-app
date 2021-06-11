package com.example.ecommerceapp.repository.main

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.ecommerceapp.api.main.EcomApiService
import com.example.ecommerceapp.api.main.asDatabaseModel
import com.example.ecommerceapp.api.main.responses.CartProduct
import com.example.ecommerceapp.api.main.responses.PostCartItemResponse
import com.example.ecommerceapp.api.main.responses.PostFavoriteItemResponse
import com.example.ecommerceapp.domain.Product
import com.example.ecommerceapp.persistence.CartProductDao
import com.example.ecommerceapp.persistence.ProductDao
import com.example.ecommerceapp.persistence.asDomainModel
import com.example.ecommerceapp.persistence.asDomainModel2
import com.example.ecommerceapp.util.FilterType
import com.example.ecommerceapp.util.NetworkConnectionInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val ecomApiService: EcomApiService,
    private val productDao: ProductDao, private val cartProductDao: CartProductDao,
    private val sharedPreferences: SharedPreferences
) :
    ProductRepository {
    //ProductDatabase returns a live data so that our database is up to date. Now when we fetch this live data from room database
    // we wanna convert it to domain objects. Now we could have directly used asDomainObject extension function on a list of database products
    // but because it is contained in a live data object we can't.By using transformation maps we convert the DatabaseProduct Live data into Domain Model Live data.
    override val product: LiveData<List<Product>> =
        Transformations.map(productDao.getProducts()) { it.asDomainModel() }

    override val favProduct: LiveData<List<Product>> =
        Transformations.map(productDao.getFavProducts()) { it.asDomainModel() }


    override val cartProducts: LiveData<List<CartProduct>> = cartProductDao.getProducts()

    override fun applyFiltering(filterType: FilterType): List<Product> {
        var list1 = listOf<Product>()
        list1 = product.value!!
        val list2 = mutableListOf<Product>()
        if (filterType == FilterType.ACCESSORIES)
            list1.forEach {
                if (it.category == "electronics")
                    list2 += it
            }
        if (filterType == FilterType.LAPTOPS)
            list1.forEach {
                if (it.price < 800.0)
                    list2 += it
            }
        if (filterType == FilterType.PHONES)
            list1.forEach {
                if (it.price > 700)
                    list2 += it
            }
        if (filterType == FilterType.RECENTLY_VIEWED)
            list1.forEach {
                if (it.price < 700.0)
                    list2 += it
            }
        if (filterType == FilterType.POPULAR)
            list1.forEach {
                if (it.price >= 700.0)
                    list2 += it
            }
        return list2
    }

    override suspend fun refreshFavProducts() {
        withContext(Dispatchers.IO) {
            try {
                var favProducts = ecomApiService.fetchFavoriteItems()
                productDao.insertProducts(favProducts.product.asDatabaseModel())
            } catch (e: IOException) {
                if (e is NetworkConnectionInterceptor.NoConnectionException) {
                }
            }
        }
    }

    //this will be the api used to refresh the offline cache
    override suspend fun refreshProducts() {
        //get back to this one **
        withContext(Dispatchers.IO) {
            try {
                val response = ecomApiService.getProperties()
                val products = response.product.asDatabaseModel()
                if (response.error == false) {
                    productDao.insertProducts(products)
                }

            } catch (e: IOException) {
                if (e is NetworkConnectionInterceptor.NoConnectionException) {
                    // show No Connectivity message to user or do whatever you want.
                    Log.d("dasfasf", "fdasf")
                }
            }
        }
    }


    override fun checkIfFav(productId: Int): Boolean {
        var flag = false
        favProduct.value?.forEach {
            Log.d("fav", it.id)
            Log.d("fav", productId.toString())
            if (it.id.toInt() == productId)
                flag = true
        }
        return flag
    }


    override suspend fun refreshCartProducts() {
        //get back to this one **
        withContext(Dispatchers.IO) {
            try {
                val cartproducts = ecomApiService.fetchCartItems()
                cartProductDao.insertCartProducts(cartproducts.product)

            } catch (e: IOException) {
                if (e is NetworkConnectionInterceptor.NoConnectionException) {
                }
            }
        }
    }

    override suspend fun updateCartProductQuantity(productId: String) {
        withContext(Dispatchers.IO) {
            try {
                //we have to make a api call to update the server first, after it is updated successfully we can update the local database!
                val response = ecomApiService.updateQuantity(productId)
                if (response.error == false) {
                    cartProductDao.updateQuantity(productId)
                } else Log.d("message", response.message.toString())

            } catch (e: IOException) {
                Log.d("exception", "fdsafsa")
            }
        }
    }

    override suspend fun addToFavorite(productId: String): PostFavoriteItemResponse {
        withContext(Dispatchers.IO) {
            try {
                val response = ecomApiService.postFavoriteItem(productId)
                if (response.error == false) {
                    productDao.insertFavProduct(productId)
                } else Log.d("e", response.message.toString())
            } catch (e: IOException) {
                Log.d("r", e.message.toString())
            }
        }
        val result = ecomApiService.postFavoriteItem(productId)
        Log.d("favorite", result.message.toString())
        return result
    }

    override suspend fun removeFavProduct(productId: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = ecomApiService.removeFavItem(productId)
                if (response.error == false) {
                    productDao.removeFavProduct(productId)
                } else Log.d("e", response.message.toString())
            } catch (e: IOException) {
                Log.d("r", e.message.toString())
            }
        }
    }


    override suspend fun removeCartProduct(productId: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = ecomApiService.removeCartItem(productId)
                if (response.error == false) {
                    cartProductDao.delete(productId)
                } else Log.d("message", response.message.toString())
            } catch (e: IOException) {
                Log.d("exception", "fsdfsdf")
            }
        }
    }


    override suspend fun fetchProductInfo(productId: String): Product {
        val productInfo: Product
        return withContext(Dispatchers.IO) {
            val cachedProductInfo = productDao.getSpecificProduct(productId)
            if (cachedProductInfo != null) {
                productInfo = cachedProductInfo.asDomainModel()
            } else {
                val product = productDao.getSpecificProduct(productId)
                productInfo = product.asDomainModel()
                productDao.insertProduct(product)
            }
            productInfo
        }
    }

    override suspend fun fetchFavoriteItems(): List<Product> {
        val result = ecomApiService.fetchFavoriteItems()
        return result.product!!.asDomainModel2()
    }


    override suspend fun fetchCartItems(): List<CartProduct> {
        val result = ecomApiService.fetchCartItems()
        return if (result.product.isNullOrEmpty()) emptyList()
        else result.product!!
    }

    override suspend fun fetchNoOfCartItems(): Int {
        return if (cartProducts.value.isNullOrEmpty()) 0 else {
            var quantity: Int = 0
            cartProducts.value!!.forEach {
                quantity += it.quantity
            }
            quantity
        }

    }


    override suspend fun addToCart(productId: String): PostCartItemResponse {
        val result = ecomApiService.postCartItem(productId)
        refreshCartProducts()
        Log.d("cart", result.message.toString())
        return result
    }

    override fun searchProduct(query: String?): LiveData<List<Product>> {
        return Transformations.map(productDao.getSearchResult(query)) { it.asDomainModel() }
    }
}