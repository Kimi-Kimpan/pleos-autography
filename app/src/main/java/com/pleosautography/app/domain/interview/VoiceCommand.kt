package com.pleosautography.app.domain.interview

/** MVP 음성 명령(CLAUDE.md ‘음성 명령’ 표). */
enum class VoiceCommand {
    /** “오토야, 뭐해?”: 지금 챕터를 알리고 인터뷰를 제안·시작한다. */
    WHATS_UP,

    /** “응” / “이어서 하자”: 제안을 받아들인다. */
    ACCEPT,

    /** “오토야, 잠깐만”·“여기까지”: 인터뷰를 멈추고 저장한다. 챕터를 다 채운 뒤라면 챕터를 마무리한다. */
    STOP,

    /** “들려줘”: 내 원고 낭독(마일스톤 7). */
    LISTEN,

    /** “방금 거기 고치자”: 낭독 중 문장 수정(마일스톤 7). */
    FIX,
    ;

    companion object {
        private val acceptWords = setOf("응", "어", "그래", "좋아", "네", "예")

        /** 받아 적은 말에서 명령을 찾는다. 명령이 아니면 null(답변으로 저장한다). */
        fun parse(text: String): VoiceCommand? {
            val t = text.replace(Regex("[\\s,.!?·“”\"']"), "")
            return when {
                "잠깐만" in t || "여기까지" in t -> STOP
                "뭐해" in t -> WHATS_UP
                "고치자" in t -> FIX
                "들려줘" in t -> LISTEN
                t in acceptWords || "이어서하자" in t || "이어서해" in t -> ACCEPT
                else -> null
            }
        }
    }
}
