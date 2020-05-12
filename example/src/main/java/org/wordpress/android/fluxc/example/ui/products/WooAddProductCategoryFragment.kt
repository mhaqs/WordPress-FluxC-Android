package org.wordpress.android.fluxc.example.ui.products

import android.R
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_add_product_category.*
import kotlinx.android.synthetic.main.view_floating_edittext.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT_CATEGORY
import org.wordpress.android.fluxc.action.WCProductAction.FETCHED_PRODUCT_CATEGORIES
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter.OnProductCategoryClickListener
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter.ProductCategoryViewHolderModel
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.AddProductCategoryPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchAllProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooAddProductCategoryFragment : Fragment(), OnProductCategoryClickListener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var productStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var productCategories: List<WCProductCategoryModel>? = null
    private var selectedParentId: Long = 0L
    private var productCategoriesAdapter: ArrayAdapter<WCProductCategoryModel>? = null

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"

        fun newInstance(selectedSitePosition: Int): WooAddProductCategoryFragment {
            val fragment = WooAddProductCategoryFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selectedSitePosition = it.getInt(ARG_SELECTED_SITE_POS, 0)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_add_product_category, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productCategoriesAdapter = ArrayAdapter<String>(view.context, R.layout.simple_spinner_dropdown_item, emptyList())

        with(parent_list) {
            adapter = productCategoriesAdapter
        }

        savedInstanceState?.let { bundle ->
            selectedSitePosition = bundle.getInt(ARG_SELECTED_SITE_POS)
        }

        updateProductCategories()

        add_product_category.setOnClickListener {
            getWCSite()?.let { site ->
                if (edit_text.text.isNullOrEmpty()) return@let

                val categoryToAdd = WCProductCategoryModel()
                categoryToAdd.name = edit_text.text.toString()
                categoryToAdd.parent = selectedParentId

                val payload = AddProductCategoryPayload(site, categoryToAdd)
                dispatcher.dispatch(WCProductActionBuilder.newAddProductCategoryAction(payload))
            } ?: prependToLog("No site found...doing nothing")
        }
    }

    private fun showProductCategories() {
        getWCSite()?.let { site ->
            val allCategories =
                    productStore.getProductCategoriesForSite(site).map {
                        ProductCategoryViewHolderModel(it)
                    }

            productCategoriesAdapter.setProductCategories(allCategories.toList())
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun updateProductCategories() {
        getWCSite()?.let { siteModel ->
            prependToLog("Submitting request to fetch product categories for site ${siteModel.id}")
            val payload = FetchAllProductCategoriesPayload(siteModel)
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
        } ?: prependToLog("No valid site found...doing nothing")
    }

    override fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel) {
        selectedParentId = productCategoryViewHolderModel.category.remoteCategoryId
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesFetched(event: OnProductCategoryChanged) {
        if (event.isError) {
            prependToLog("Error fetching product categories - error: " + event.error.type)
        } else {
            when(event.causeOfChange) {
                FETCHED_PRODUCT_CATEGORIES -> {
                    productCategories = productStore.getProductCategoriesForSite(getWCSite()!!)
                    prependToLog("Fetched ${event.rowsAffected} product categories")
                    showProductCategories()
                }
                ADDED_PRODUCT_CATEGORY -> {
                    prependToLog("Added Product Category")
                    showProductCategories()
                }
                else -> prependToLog("Wow dunno what happened here")
            }
        }
    }
}
