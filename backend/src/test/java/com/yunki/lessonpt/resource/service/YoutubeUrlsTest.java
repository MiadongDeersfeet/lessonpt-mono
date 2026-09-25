package com.yunki.lessonpt.resource.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.yunki.lessonpt.common.exception.BusinessException;
import com.yunki.lessonpt.common.exception.ErrorCode;

class YoutubeUrlsTest {

    @Test
    void acceptsWatchEmbedShortsAndShortLink() {
        assertThat(YoutubeUrls.hasVideoId("https://www.youtube.com/watch?v=abcdefghijk")).isTrue();
        assertThat(YoutubeUrls.hasVideoId("https://youtube.com/watch?v=abcdefghijk&t=3")).isTrue();
        assertThat(YoutubeUrls.hasVideoId("https://m.youtube.com/embed/abcdefghijk")).isTrue();
        assertThat(YoutubeUrls.hasVideoId("https://www.youtube.com/shorts/abcdefghijk")).isTrue();
        assertThat(YoutubeUrls.hasVideoId("https://youtu.be/abcdefghijk")).isTrue();
        YoutubeUrls.requireValid(null);
    }

    @Test
    void rejectsHttpOtherHostsAndShortIds() {
        assertThat(YoutubeUrls.hasVideoId("http://youtu.be/abcdefghijk")).isFalse();
        assertThat(YoutubeUrls.hasVideoId("https://vimeo.com/abcdefghijk")).isFalse();
        assertThat(YoutubeUrls.hasVideoId("https://youtu.be/scale")).isFalse();
        assertThatThrownBy(() -> YoutubeUrls.requireValid("https://youtu.be/scale"))
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
    }
}
