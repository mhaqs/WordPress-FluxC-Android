package org.wordpress.android.fluxc.release

import com.google.gson.JsonArray
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductModel.ProductTriplet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductVisibility
import org.wordpress.android.fluxc.persistence.MediaSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchAllProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductPasswordChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_WCProductTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_SINGLE_PRODUCT,
        FETCHED_PRODUCTS,
        FETCHED_PRODUCT_VARIATIONS,
        FETCHED_PRODUCT_REVIEWS,
        FETCHED_PRODUCT_CATEGORIES,
        FETCHED_SINGLE_PRODUCT_REVIEW,
        FETCHED_PRODUCT_SHIPPING_CLASS_LIST,
        FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS,
        FETCHED_PRODUCT_PASSWORD,
        UPDATED_PRODUCT,
        UPDATED_PRODUCT_REVIEW_STATUS,
        UPDATED_PRODUCT_IMAGES,
        UPDATED_PRODUCT_PASSWORD,
    }

    @Inject internal lateinit var productStore: WCProductStore
    @Inject internal lateinit var mediaStore: MediaStore // must be injected for onMediaListFetched()

    private var nextEvent: TestEvent = TestEvent.NONE
    private val productModel = WCProductModel(8).apply {
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
        taxStatus = "taxable"
        stockStatus = "instock"
        backorders = "yes"
        images = "[]"
        categories = "[]"
    }
    private val productModelWithVariations = WCProductModel(8).apply {
        remoteProductId = BuildConfig.TEST_WC_PRODUCT_WITH_VARIATIONS_ID.toLong()
        dateCreated = "2018-04-20T15:45:14Z"
    }
    private val remoteProductReviewId = BuildConfig.TEST_WC_PRODUCT_REVIEW_ID.toLong()

    private val updatedPassword = "password"

    private var lastEvent: OnProductChanged? = null
    private var lastShippingClassEvent: OnProductShippingClassesChanged? = null
    private var lastReviewEvent: OnProductReviewChanged? = null
    private var lastProductCategoryEvent: OnProductCategoryChanged? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp(false)
        mReleaseStackAppComponent.inject(this)
        init()
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleProduct() {
        // remove all products for this site and verify there are none
        ProductSqlUtils.deleteProductsForSite(sSite)
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 0)

        nextEvent = TestEvent.FETCHED_SINGLE_PRODUCT
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newFetchSingleProductAction(FetchSingleProductPayload(sSite, productModel.remoteProductId))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(fetchedProduct)
        assertEquals(fetchedProduct!!.remoteProductId, productModel.remoteProductId)

        // Verify there's only one product for this site
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 1)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProducts() {
        // remove all products for this site and verify there are none
        ProductSqlUtils.deleteProductsForSite(sSite)
        assertEquals(ProductSqlUtils.getProductCountForSite(sSite), 0)

        nextEvent = TestEvent.FETCHED_PRODUCTS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newFetchProductsAction(FetchProductsPayload(sSite))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedProducts = productStore.getProductsForSite(sSite)
        assertNotEquals(fetchedProducts.size, 0)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductVariations() {
        // remove all variations for this product and verify there are none
        ProductSqlUtils.deleteVariationsForProduct(sSite, productModelWithVariations.remoteProductId)
        assertEquals(ProductSqlUtils.getVariationsForProduct(sSite, productModelWithVariations.remoteProductId).size, 0)

        nextEvent = TestEvent.FETCHED_PRODUCT_VARIATIONS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newFetchProductVariationsAction(
                                FetchProductVariationsPayload(
                                        sSite,
                                        productModelWithVariations.remoteProductId
                                )
                        )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedVariations = productStore.getVariationsForProduct(sSite, productModelWithVariations.remoteProductId)
        assertNotEquals(fetchedVariations.size, 0)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductShippingClassesForSite() {
        /*
         * TEST 1: Fetch product shipping classes for site
         */
        // Remove all product shipping classes from the database
        ProductSqlUtils.deleteProductShippingClassListForSite(sSite)
        assertEquals(0, ProductSqlUtils.getProductShippingClassListForSite(sSite.id).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_SHIPPING_CLASS_LIST
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(WCProductActionBuilder.newFetchProductShippingClassListAction(
                        FetchProductShippingClassListPayload(sSite)
                ))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedShippingClasses = productStore.getShippingClassListForSite(sSite)
        assertTrue(fetchedShippingClasses.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductShippingClassByRemoteIdForSite() {
        /*
         * TEST 1: Fetch product shipping class for site
         */
        // Remove all product shipping classes from the database
        val remoteShippingClassId = 31L
        ProductSqlUtils.deleteProductShippingClassListForSite(sSite)
        assertEquals(0, ProductSqlUtils.getProductShippingClassListForSite(sSite.id).size)

        nextEvent = TestEvent.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductShippingClassAction(
                FetchSingleProductShippingClassPayload(sSite, remoteShippingClassId)
        ))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedShippingClasses = productStore.getShippingClassByRemoteId(
                sSite, remoteShippingClassId
        )
        assertNotNull(fetchedShippingClasses)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductReviews() {
        /*
         * TEST 1: Fetch product reviews for site
         */
        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_REVIEWS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchProductReviewsAction(FetchProductReviewsPayload(sSite, offset = 0)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedReviewsAll = productStore.getProductReviewsForSite(sSite)
        assertTrue(fetchedReviewsAll.isNotEmpty())

        /*
         * TEST 2: Fetch product reviews matching a list of review ID's
         */
        // Store a couple of the IDs from the previous test
        val idsToFetch = fetchedReviewsAll.take(3).map { it.remoteProductReviewId }

        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_REVIEWS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchProductReviewsAction(
                        FetchProductReviewsPayload(sSite, reviewIds = idsToFetch, offset = 0)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchReviewsId = productStore.getProductReviewsForSite(sSite)
        assertEquals(idsToFetch.size, fetchReviewsId.size)

        /*
         * TEST 3: Fetch product reviews for a list of product
         */
        // Store a couple of the IDs from the previous test
        val productIdsToFetch = fetchedReviewsAll.take(3).map { it.remoteProductId }

        // Check to see how many reviews currently exist for these product IDs before deleting
        // from the database
        val reviewsByProduct = productIdsToFetch.map { productStore.getProductReviewsForProductAndSiteId(sSite.id, it) }

        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_REVIEWS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchProductReviewsAction(
                        FetchProductReviewsPayload(sSite, productIds = productIdsToFetch, offset = 0)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchedReviewsForProduct = productStore.getProductReviewsForSite(sSite)
        assertEquals(reviewsByProduct.size, fetchedReviewsForProduct.size)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateProductPassword() {
        // first dispatch a request to update the password - note that this will fail for private products
        nextEvent = TestEvent.UPDATED_PRODUCT_PASSWORD
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newUpdateProductPasswordAction(
                                UpdateProductPasswordPayload(
                                        sSite,
                                        productModel.remoteProductId,
                                        updatedPassword
                                )
                        )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // then dispatch a request to fetch it so we can make sure it's the same we just updated to
        nextEvent = TestEvent.FETCHED_PRODUCT_PASSWORD
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder
                        .newFetchProductPasswordAction(FetchProductPasswordPayload(sSite, productModel.remoteProductId))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleProductAndUpdateReview() {
        // Remove all product reviews from the database
        productStore.deleteAllProductReviews()
        assertEquals(0, ProductSqlUtils.getProductReviewsForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_SINGLE_PRODUCT_REVIEW
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchSingleProductReviewAction(
                        FetchSingleProductReviewPayload(sSite, remoteProductReviewId)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val review = productStore
                .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
        assertNotNull(review)

        // Update review status to spam - should get deleted from db
        review?.let {
            val newStatus = "spam"
            nextEvent = TestEvent.UPDATED_PRODUCT_REVIEW_STATUS
            mCountDownLatch = CountDownLatch(1)
            mDispatcher.dispatch(
                    WCProductActionBuilder.newUpdateProductReviewStatusAction(
                            UpdateProductReviewStatusPayload(sSite, review.remoteProductReviewId, newStatus)))
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            // Verify results - review should be deleted from db
            val savedReview = productStore
                    .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNull(savedReview)
        }

        // Update review status to approved - should get added to db
        review?.let {
            val newStatus = "approved"
            nextEvent = TestEvent.UPDATED_PRODUCT_REVIEW_STATUS
            mCountDownLatch = CountDownLatch(1)
            mDispatcher.dispatch(
                    WCProductActionBuilder.newUpdateProductReviewStatusAction(
                            UpdateProductReviewStatusPayload(sSite, review.remoteProductReviewId, newStatus)))
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            // Verify results
            val savedReview = productStore
                    .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNotNull(savedReview)
            assertEquals(newStatus, savedReview!!.status)
        }

        // Update review status to trash - should get deleted from db
        review?.let {
            val newStatus = "trash"
            nextEvent = TestEvent.UPDATED_PRODUCT_REVIEW_STATUS
            mCountDownLatch = CountDownLatch(1)
            mDispatcher.dispatch(
                    WCProductActionBuilder.newUpdateProductReviewStatusAction(
                            UpdateProductReviewStatusPayload(sSite, review.remoteProductReviewId, newStatus)))
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            // Verify results - review should be deleted from db
            val savedReview = productStore
                    .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNull(savedReview)
        }

        // Update review status to hold - should get added to db
        review?.let {
            val newStatus = "hold"
            nextEvent = TestEvent.UPDATED_PRODUCT_REVIEW_STATUS
            mCountDownLatch = CountDownLatch(1)
            mDispatcher.dispatch(
                    WCProductActionBuilder.newUpdateProductReviewStatusAction(
                            UpdateProductReviewStatusPayload(sSite, review.remoteProductReviewId, newStatus)))
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            // Verify results
            val savedReview = productStore
                    .getProductReviewByRemoteId(sSite.id, remoteProductReviewId)
            assertNotNull(savedReview)
            assertEquals(newStatus, savedReview!!.status)
        }
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateProductImages() {
        // first get the list of this site's media, and if it's empty fetch a single media model
        var siteMedia = MediaSqlUtils.getAllSiteMedia(sSite)
        if (siteMedia.isEmpty()) {
            fetchFirstMedia()
            siteMedia = MediaSqlUtils.getAllSiteMedia(sSite)
            assertTrue(siteMedia.isNotEmpty())
        }

        val mediaModelForProduct = siteMedia[0]

        nextEvent = TestEvent.UPDATED_PRODUCT_IMAGES
        mCountDownLatch = CountDownLatch(1)
        val imageList = ArrayList<WCProductImageModel>().also {
            it.add(WCProductImageModel.fromMediaModel(mediaModelForProduct))
        }
        mDispatcher.dispatch(
                WCProductActionBuilder.newUpdateProductImagesAction(
                        UpdateProductImagesPayload(sSite, productModel.remoteProductId, imageList)
                )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val updatedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(updatedProduct)

        val updatedImageList = updatedProduct!!.getImages()
        assertNotNull(updatedImageList)
        assertEquals(updatedImageList.size, 1)

        val updatedImage = updatedImageList[0]
        assertEquals(updatedImage.id, mediaModelForProduct.mediaId)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testUpdateProduct() {
        val updatedProductDesc = "Testing updating product description"
        productModel.description = updatedProductDesc

        val updatedProductName = "Product I"
        productModel.name = updatedProductName

        val updatedProductStatus = CoreProductStatus.PRIVATE.value
        productModel.status = updatedProductStatus

        val updatedProductVisibility = CoreProductVisibility.HIDDEN.value
        productModel.catalogVisibility = updatedProductVisibility

        val updatedProductFeatured = false
        productModel.featured = updatedProductFeatured

        val updatedProductSlug = "product-slug"
        productModel.slug = updatedProductSlug

        val updatedProductReviewsAllowed = true
        productModel.reviewsAllowed = updatedProductReviewsAllowed

        val updateProductPurchaseNote = "Test purchase note"
        productModel.purchaseNote = updateProductPurchaseNote

        val updatedProductMenuOrder = 5
        productModel.menuOrder = updatedProductMenuOrder

        val updatedProductCategories = JsonArray().also {
            it.add(ProductTriplet(1374, "Uncategorized", "uncategorized").toJson())
        }.toString()
        productModel.categories = updatedProductCategories

        nextEvent = TestEvent.UPDATED_PRODUCT
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newUpdateProductAction(UpdateProductPayload(sSite, productModel))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val updatedProduct = productStore.getProductByRemoteId(sSite, productModel.remoteProductId)
        assertNotNull(updatedProduct)
        assertEquals(updatedProductDesc, updatedProduct?.description)
        assertEquals(productModel.remoteProductId, updatedProduct?.remoteProductId)
        assertEquals(updatedProductName, updatedProduct?.name)
        assertEquals(updatedProductStatus, updatedProduct?.status)
        assertEquals(updatedProductVisibility, updatedProduct?.catalogVisibility)
        assertEquals(updatedProductFeatured, updatedProduct?.featured)
        assertEquals(updatedProductSlug, updatedProduct?.slug)
        assertEquals(updatedProductReviewsAllowed, updatedProduct?.reviewsAllowed)
        assertEquals(updateProductPurchaseNote, updatedProduct?.purchaseNote)
        assertEquals(updatedProductMenuOrder, updatedProduct?.menuOrder)
        assertEquals(updatedProductCategories, updatedProduct?.categories)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchProductCategories() {
        // Remove all product categories from the database
        ProductSqlUtils.deleteAllProductCategories()
        assertEquals(0, ProductSqlUtils.getProductCategoriesForSite(sSite).size)

        nextEvent = TestEvent.FETCHED_PRODUCT_CATEGORIES
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(
                WCProductActionBuilder.newFetchProductCategoriesAction(
                        FetchAllProductCategoriesPayload(sSite)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify results
        val fetchAllCategories = productStore.getProductCategoriesForSite(sSite)
        assertTrue(fetchAllCategories.isNotEmpty())
    }

    /**
     * Used by the update images test to fetch a single media model for this site
     */
    @Throws(InterruptedException::class)
    private fun fetchFirstMedia() {
        mCountDownLatch = CountDownLatch(1)
        val payload = MediaStore.FetchMediaListPayload(sSite, 1, false)
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload))
        mCountDownLatch.await()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        event.error?.let {
            throw AssertionError("OnProductChanged has unexpected error: " + it.type)
        }

        lastEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_SINGLE_PRODUCT -> {
                assertEquals(TestEvent.FETCHED_SINGLE_PRODUCT, nextEvent)
                assertEquals(event.remoteProductId, productModel.remoteProductId)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_PRODUCTS -> {
                assertEquals(TestEvent.FETCHED_PRODUCTS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_PRODUCT_VARIATIONS -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_VARIATIONS, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        event.error?.let {
            throw AssertionError("OnProductReviewChanged has unexpected error: " + it.type)
        }

        lastReviewEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW -> {
                assertEquals(TestEvent.FETCHED_SINGLE_PRODUCT_REVIEW, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_PRODUCT_REVIEWS -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_REVIEWS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS -> {
                assertEquals(TestEvent.UPDATED_PRODUCT_REVIEW_STATUS, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched) {
        event.error?.let {
            throw AssertionError("WCProductTest.onMediaListFetched has unexpected error: ${it.type}, ${it.message}")
        }
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        event.error?.let {
            throw AssertionError("OnProductImagesChanged has unexpected error: ${it.type}, ${it.message}")
        }

        assertEquals(TestEvent.UPDATED_PRODUCT_IMAGES, nextEvent)
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        event.error?.let {
            throw AssertionError("OnProductUpdated has unexpected error: ${it.type}, ${it.message}")
        }

        assertEquals(TestEvent.UPDATED_PRODUCT, nextEvent)
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductPasswordChanged(event: OnProductPasswordChanged) {
        if (event.isError) {
            event.error?.let {
                throw AssertionError("onProductPasswordChanged has unexpected error: ${it.type}, ${it.message}")
            }
        } else if (event.causeOfChange == WCProductAction.FETCH_PRODUCT_PASSWORD) {
            assertEquals(TestEvent.FETCHED_PRODUCT_PASSWORD, nextEvent)
            assertEquals(updatedPassword, event.password)
        } else if (event.causeOfChange == WCProductAction.UPDATE_PRODUCT_PASSWORD) {
            assertEquals(TestEvent.UPDATED_PRODUCT_PASSWORD, nextEvent)
            assertEquals(updatedPassword, event.password)
        }

        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductShippingClassesChanged(event: OnProductShippingClassesChanged) {
        event.error?.let {
            throw AssertionError(
                    "OnProductShippingClassesChanged has unexpected error: ${it.type}, ${it.message}"
            )
        }

        lastShippingClassEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS -> {
                assertEquals(TestEvent.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_SHIPPING_CLASS_LIST, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesChanged(event: OnProductCategoryChanged) {
        event.error?.let {
            throw AssertionError("OnProductCategoryChanged has unexpected error: " + it.type)
        }

        lastProductCategoryEvent = event

        when (event.causeOfChange) {
            WCProductAction.FETCHED_PRODUCT_CATEGORIES -> {
                assertEquals(TestEvent.FETCHED_PRODUCT_CATEGORIES, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }
}
