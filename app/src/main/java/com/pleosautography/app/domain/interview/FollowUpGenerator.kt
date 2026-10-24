package com.pleosautography.app.domain.interview

import com.pleosautography.app.data.db.QuestionEntity
import com.pleosautography.app.data.speech.DemoScript

/**
 * 방금 들은 이야기를 받아 되묻는 오토의 후속 질문.
 * 후속 질문은 다음 질문으로 이어지고, 그 답은 다음 질문의 답으로 저장한다(진행률 = 답한 질문 수 / 질문 수).
 * - [ScriptedFollowUps]: 시연용, 답변마다 준비된 문장.
 * - 2단계: 답변 맥락으로 AI가 만든다(같은 인터페이스로 바꿔 끼운다).
 */
interface FollowUpGenerator {
    /**
     * @param answered 방금 답한 질문.
     * @param transcript 방금 받아 적은 답변.
     * @param next 이어서 물을 질문.
     * @return 오토가 말할 후속 질문. 다음 질문을 그대로 물으려면 null.
     */
    suspend fun followUp(answered: QuestionEntity, transcript: String, next: QuestionEntity): String?
}

class ScriptedFollowUps(private val script: DemoScript) : FollowUpGenerator {
    override suspend fun followUp(answered: QuestionEntity, transcript: String, next: QuestionEntity): String? =
        script.followUpFor(answered.id)
}
