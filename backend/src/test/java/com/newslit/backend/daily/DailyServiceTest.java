package com.newslit.backend.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.daily.dto.DailyResponseDto;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyServiceTest {

    private static final Long ARTICLE_ID = 1L;

    @Mock
    private DailyRespository dailyRespository;
    @Mock
    private SentenceRepository sentenceRepository;
    @Mock
    private ArticleRepository articleRepository;
    @InjectMocks
    private DailyService dailyService;

    @Test
    void 청크_인덱스는_문장의_orderIndex로_저장되고_모든_문장을_빠짐없이_덮는다() {
        // 30단어 문장 8개(240단어) → 120단어씩 두 청크로 나뉜다
        List<Sentence> shuffled = IntStream.of(5, 2, 8, 1, 7, 3, 6, 4)
                .mapToObj(this::sentence)
                .toList();
        when(articleRepository.findById(ARTICLE_ID)).thenReturn(Optional.of(Article.builder().Id(ARTICLE_ID).build()));
        when(sentenceRepository.findAllByArticleId(ARTICLE_ID)).thenReturn(shuffled);
        when(dailyRespository.findTopByOrderByDisplayDateDesc()).thenReturn(Optional.empty());
        when(dailyRespository.findByArticleIdAndStartIndexAndEndIndex(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(dailyRespository.save(any(Daily.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<DailyResponseDto> chunks = dailyService.createChunks(ARTICLE_ID);

        assertThat(chunks).extracting(DailyResponseDto::getStartIndex, DailyResponseDto::getEndIndex)
                .containsExactly(
                        Tuple.tuple(1, 4),
                        Tuple.tuple(5, 8));
    }

    private Sentence sentence(int orderIndex) {
        return Sentence.builder()
                .orderIndex(orderIndex)
                .englishText("word ".repeat(30).trim())
                .build();
    }
}
