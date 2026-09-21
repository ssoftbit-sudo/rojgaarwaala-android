package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.utils.VideoListUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoListUtilsTest {

    @Test
    fun hospitalWithMoreThanPreviewShowsHasMore() {
        assertTrue(
            VideoListUtils.inferHasMore(
                previewCount = 20,
                hasMore = true,
                total = 21,
            ),
        )
    }

    @Test
    fun hospitalWithFewerThanPreviewDoesNotInferHasMore() {
        assertFalse(
            VideoListUtils.inferHasMore(
                previewCount = 6,
                hasMore = false,
                total = 6,
            ),
        )
    }
}
