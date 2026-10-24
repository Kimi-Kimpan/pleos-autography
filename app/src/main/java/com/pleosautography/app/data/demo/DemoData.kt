package com.pleosautography.app.data.demo

/** 챕터 상태(Room 초안의 Chapter.status와 같다). */
enum class ChapterStatus { NOT_STARTED, WRITING, DONE }

data class ChapterSummary(
    val number: Int,
    val title: String,
    val status: ChapterStatus,
    val storyCount: Int = 0,
    val progress: Float = 0f,
    val lastStory: String? = null,
)

/** 시연용 고정 데이터(챕터 제목, 주행 화면 기본값, 라디오 목록, 예시 원고). 책·답변은 Room 저장소에서 읽는다. */
object DemoData {
    // 07~12장 제목은 질문 은행 파일에서 정한다(마일스톤 4).
    val chapters = listOf(
        ChapterSummary(1, "나의 뿌리", ChapterStatus.NOT_STARTED),
        ChapterSummary(2, "첫 차", ChapterStatus.NOT_STARTED),
        ChapterSummary(3, "일과 생계", ChapterStatus.NOT_STARTED),
        ChapterSummary(4, "가족", ChapterStatus.NOT_STARTED),
        ChapterSummary(5, "길 위에서 만난 사람들", ChapterStatus.NOT_STARTED),
        ChapterSummary(6, "가장 길었던 밤", ChapterStatus.NOT_STARTED),
    )

    /** 주행 화면 데모 값(마일스톤 6에서 NaviState·VehicleState가 정한다). */
    val drive = DriveDemo()

    /** 오토그래피 라디오: 지금 재생 중인 이야기와 다음으로 들을 이야기(주행 중에는 2편 고정). */
    val radio = RadioDemo()

    /**
     * 예시 원고(시안 Script·ListenDrive). 답변을 원고 문장으로 정리하는 일은 2단계(AI)라,
     * MVP는 이 예시 문단 뒤에 저장된 답변을 그대로 문단으로 잇는다.
     */
    val manuscript = listOf(
        ManuscriptParagraph("스물아홉에 처음 운전대를 잡았습니다. 새벽 네 시가 이렇게 조용한 줄 몰랐어요."),
        ManuscriptParagraph("시동을 걸자마자 손이 떨렸죠. 1988년 겨울, 아내가 보온병에 커피를 타 줬습니다.", highlight = "1988년 겨울"),
        ManuscriptParagraph("첫 손님은 시장에 가던 아주머니였어요. “기사 양반, 첫날이지?” 하시더군요."),
    )
}

/** 주행 흐름(주행 홈 → 제안 → 인터뷰 → 도착 전 마무리)의 데모 값. */
data class DriveDemo(
    val minutesToArrival: Int = 24,
    val congestionMinutes: Int = 15,
    val questionNumber: Int = 4,
    val question: String = "처음 운전대를 잡은 날,\n기억나세요?",
    val followUpHint: String = "그날 아내분 이야기를\n조금 더 들려주시면 좋겠어요.",
    val pauseMinutesToArrival: Int = 9,
    val pauseQuestionNumber: Int = 6,
)

/**
 * 오토그래피 라디오 이야기 한 편.
 * @param volume 표지에 쓰는 권·챕터 표시.
 */
data class RadioEpisode(
    val title: String,
    val narrator: String,
    val tag: String,
    val volume: String = "",
    val durationSec: Int = 0,
)

/**
 * 오토그래피 라디오 시연 데이터. 재생 목록을 차례로 돌고, 처음에는 첫 편 [startSec]부터 재생한다.
 * 다음으로 들을 이야기는 지금 편 뒤 2편이다(주행 중에도 2편 고정).
 */
data class RadioDemo(
    val nowPlayingLabel: String = "지금 재생 중 · 오토 목소리 낭독",
    val playlist: List<RadioEpisode> = listOf(
        RadioEpisode("짐칸 가득, 하루치의 삶", "이○○ 기사님 · 1톤 화물차 18년 차", "화물 · 택배", "VOL. 01 · 일과 생계", 18 * 60 + 5),
        RadioEpisode("새벽 4시, 첫 손님", "김○○ 기사님 · 개인택시 22년 차", "택시", "VOL. 02 · 첫 차", 14 * 60 + 30),
        RadioEpisode("국경을 세 번 넘은 밤", "Marek K. 기사님 · 엑시언트 장거리 · 폴란드", "해외 · 번역", "VOL. 06 · 가장 길었던 밤", 21 * 60 + 12),
        RadioEpisode("시장 골목 첫 배달", "박○○ 사장님 · 다마스 12년 차", "자영업", "VOL. 03 · 일과 생계", 11 * 60 + 48),
    ),
    val startSec: Int = 12 * 60 + 40,
    val genres: List<String> = listOf("전체", "택시", "화물 · 택배", "자영업", "장거리"),
    /** 정차 중 플레이어는 칸이 넓어 장르를 하나 더 보인다. */
    val moreGenres: List<String> = listOf("해외 · 번역"),
    val favoriteCount: Int = 12,
)

/**
 * 원고 한 문단. [highlight]는 길게 눌러 고를 때 강조할 구절(시안).
 * @param answerId 저장된 답변이면 그 id(예시 원고는 null).
 */
data class ManuscriptParagraph(val text: String, val highlight: String? = null, val answerId: Long? = null)
