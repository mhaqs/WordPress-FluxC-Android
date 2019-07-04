package org.wordpress.android.fluxc.persistance.room

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.LATEST_POST_DETAIL_INSIGHTS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType.INSIGHTS
import org.wordpress.android.fluxc.persistence.room.StatsDao
import org.wordpress.android.fluxc.persistence.room.StatsDao.StatsBlock

class StatsDaoTest : StatsDatabaseTest() {
    private lateinit var statsDao: StatsDao
    @Before
    fun setUp() {
        statsDao = database.statsDao()
    }

    @Test
    fun insertStatsBlockTriggersLiveData() {
        val localSiteId = 1
        val blockType = LATEST_POST_DETAIL_INSIGHTS
        val statsType = INSIGHTS
        var statsBlock: String? = null
        statsDao.liveSelect(localSiteId, blockType, statsType).observeForever { statsBlock = it }

        assertThat(statsBlock).isNull()

        val postId: Long = 10
        val json = "{test: value}"
        val insertedStatsBlock = StatsBlock(
                localSiteId = localSiteId,
                blockType = blockType,
                statsType = statsType,
                json = json,
                postId = postId,
                date = null,
                id = null
        )
        statsDao.insertOrReplace(insertedStatsBlock)

        assertThat(statsBlock).isNotNull()
        assertThat(statsBlock!!).isEqualTo(json)
    }
}