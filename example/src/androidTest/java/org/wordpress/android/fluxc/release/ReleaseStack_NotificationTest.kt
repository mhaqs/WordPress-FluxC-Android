package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsSeenPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import java.lang.Exception
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.AssertionError

class ReleaseStack_NotificationTest : ReleaseStack_WPComBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_NOTIFS,
        MARKED_NOTIFS_SEEN,
        MARKED_NOTIFS_READ
    }

    @Inject internal lateinit var notificationStore: NotificationStore

    private var nextEvent: TestEvent = TestEvent.NONE
    private var lastEvent: OnNotificationChanged? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)

        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchNotifications() {
        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val fetchedNotifs = notificationStore.getNotifications().size
        assertTrue(fetchedNotifs > 0 && fetchedNotifs <= NotificationRestClient.NOTIFICATION_DEFAULT_NUMBER)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testMarkNotificationsSeen() {
        nextEvent = TestEvent.MARKED_NOTIFS_SEEN
        mCountDownLatch = CountDownLatch(1)

        val lastSeenTime = Date().time
        mDispatcher.dispatch(NotificationActionBuilder
                .newMarkNotificationsSeenAction(MarkNotificationsSeenPayload(lastSeenTime)))

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        assertNotNull(lastEvent?.lastSeenTime)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testMarkNotificationsRead() {
        // First, fetch notifications and store in database.
        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val fetchedNotifs = notificationStore.getNotifications()

        // Second, request up to 3 notifications from the db to set to read
        if (fetchedNotifs.isNotEmpty()) {
            val requestList = fetchedNotifs.take(3)
            val requestListSize = requestList.size

            nextEvent = TestEvent.MARKED_NOTIFS_READ
            mCountDownLatch = CountDownLatch(1)

            mDispatcher.dispatch(NotificationActionBuilder
                    .newMarkNotificationsReadAction(MarkNotificationsReadPayload(requestList)))
            Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            // Verify
            Assert.assertNotNull(lastEvent)
            Assert.assertTrue(lastEvent!!.success)
            Assert.assertEquals(lastEvent!!.changedNotificationLocalIds.size, requestListSize)
            with(lastEvent!!.changedNotificationLocalIds) {
                requestList.forEach { Assert.assertTrue(contains(it.noteId)) }
            }
        } else {
            throw AssertionError("No notifications fetched to run test with!")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        event.error?.let {
            throw AssertionError("OnNotificationChanged as error: ${it.type}")
        }

        lastEvent = event
        when (event.causeOfChange) {
            NotificationAction.FETCH_NOTIFICATIONS -> {
                assertEquals(TestEvent.FETCHED_NOTIFS, nextEvent)
                mCountDownLatch.countDown()
            }
            NotificationAction.MARK_NOTIFICATIONS_SEEN -> {
                assertEquals(TestEvent.MARKED_NOTIFS_SEEN, nextEvent)
                mCountDownLatch.countDown()
            }
            NotificationAction.MARK_NOTIFICATIONS_READ -> {
                assertEquals(TestEvent.MARKED_NOTIFS_READ, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: ${event.causeOfChange}")
        }
    }
}
