package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCProductStore.AddProductCategoryPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchAllProductCategoriesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsResponsePayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductShippingClassPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductCategoryResponsePayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductCategoryListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductImagesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdatedProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPasswordPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload;

@ActionEnum
public enum WCProductAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchSingleProductPayload.class)
    FETCH_SINGLE_PRODUCT,
    @Action(payloadType = FetchProductsPayload.class)
    FETCH_PRODUCTS,
    @Action(payloadType = SearchProductsPayload.class)
    SEARCH_PRODUCTS,
    @Action(payloadType = FetchProductVariationsPayload.class)
    FETCH_PRODUCT_VARIATIONS,
    @Action(payloadType = FetchProductShippingClassListPayload.class)
    FETCH_PRODUCT_SHIPPING_CLASS_LIST,
    @Action(payloadType = FetchSingleProductShippingClassPayload.class)
    FETCH_SINGLE_PRODUCT_SHIPPING_CLASS,
    @Action(payloadType = FetchProductReviewsPayload.class)
    FETCH_PRODUCT_REVIEWS,
    @Action(payloadType = FetchSingleProductReviewPayload.class)
    FETCH_SINGLE_PRODUCT_REVIEW,
    @Action(payloadType = UpdateProductReviewStatusPayload.class)
    UPDATE_PRODUCT_REVIEW_STATUS,
    @Action(payloadType = UpdateProductImagesPayload.class)
    UPDATE_PRODUCT_IMAGES,
    @Action(payloadType = UpdateProductPayload.class)
    UPDATE_PRODUCT,
    @Action(payloadType = FetchProductSkuAvailabilityPayload.class)
    FETCH_PRODUCT_SKU_AVAILABILITY,
    @Action(payloadType = FetchProductPasswordPayload.class)
    FETCH_PRODUCT_PASSWORD,
    @Action(payloadType = UpdateProductPasswordPayload.class)
    UPDATE_PRODUCT_PASSWORD,
    @Action(payloadType = FetchAllProductCategoriesPayload.class)
    FETCH_PRODUCT_CATEGORIES,
    @Action(payloadType = AddProductCategoryPayload.class)
    ADD_PRODUCT_CATEGORY,


    // Remote responses
    @Action(payloadType = RemoteProductPayload.class)
    FETCHED_SINGLE_PRODUCT,
    @Action(payloadType = RemoteProductListPayload.class)
    FETCHED_PRODUCTS,
    @Action(payloadType = RemoteSearchProductsPayload.class)
    SEARCHED_PRODUCTS,
    @Action(payloadType = RemoteProductVariationsPayload.class)
    FETCHED_PRODUCT_VARIATIONS,
    @Action(payloadType = RemoteProductShippingClassListPayload.class)
    FETCHED_PRODUCT_SHIPPING_CLASS_LIST,
    @Action(payloadType = RemoteProductShippingClassPayload.class)
    FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS,
    @Action(payloadType = FetchProductReviewsResponsePayload.class)
    FETCHED_PRODUCT_REVIEWS,
    @Action(payloadType = RemoteProductReviewPayload.class)
    FETCHED_SINGLE_PRODUCT_REVIEW,
    @Action(payloadType = RemoteProductReviewPayload.class)
    UPDATED_PRODUCT_REVIEW_STATUS,
    @Action(payloadType = RemoteUpdateProductImagesPayload.class)
    UPDATED_PRODUCT_IMAGES,
    @Action(payloadType = RemoteUpdateProductPayload.class)
    UPDATED_PRODUCT,
    @Action(payloadType = RemoteProductSkuAvailabilityPayload.class)
    FETCHED_PRODUCT_SKU_AVAILABILITY,
    @Action(payloadType = RemoteProductPasswordPayload.class)
    FETCHED_PRODUCT_PASSWORD,
    @Action(payloadType = RemoteUpdatedProductPasswordPayload.class)
    UPDATED_PRODUCT_PASSWORD,
    @Action(payloadType = RemoteProductCategoryListPayload.class)
    FETCHED_PRODUCT_CATEGORIES,
    @Action(payloadType = RemoteAddProductCategoryResponsePayload.class)
    ADDED_PRODUCT_CATEGORY
}
