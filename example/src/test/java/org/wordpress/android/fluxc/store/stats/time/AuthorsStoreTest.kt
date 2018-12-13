package org.wordpress.android.fluxc.store.stats.time

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val PAGE_SIZE = 8

@RunWith(MockitoJUnitRunner::class)
class AuthorsStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: AuthorsRestClient
    @Mock lateinit var sqlUtils: TimeStatsSqlUtils
    @Mock lateinit var mapper: TimeStatsMapper
    private lateinit var store: AuthorsStore
    @Before
    fun setUp() {
        store = AuthorsStore(
                restClient,
                sqlUtils,
                mapper,
                Unconfined
        )
    }

    @Test
    fun `returns data per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                AUTHORS_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchAuthors(site, DAYS, PAGE_SIZE + 1, forced)).thenReturn(
                fetchInsightsPayload
        )
        val model = mock<AuthorsModel>()
        whenever(mapper.map(AUTHORS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val responseModel = store.fetchAuthors(site, PAGE_SIZE, DAYS, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, AUTHORS_RESPONSE, DAYS)
    }

    @Test
    fun `returns error when data call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<AuthorsResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchAuthors(site, DAYS, PAGE_SIZE + 1, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchAuthors(site, PAGE_SIZE, DAYS, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns data from db`() {
        whenever(sqlUtils.selectAuthors(site, DAYS)).thenReturn(AUTHORS_RESPONSE)
        val model = mock<AuthorsModel>()
        whenever(mapper.map(AUTHORS_RESPONSE, PAGE_SIZE)).thenReturn(model)

        val result = store.getAuthors(site, DAYS, PAGE_SIZE)

        assertThat(result).isEqualTo(model)
    }
}