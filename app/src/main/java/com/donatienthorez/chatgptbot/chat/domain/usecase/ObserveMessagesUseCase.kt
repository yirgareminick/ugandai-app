package com.donatienthorez.chatgptbot.chat.domain.usecase

import com.ugandai.chatgptbot.chat.data.ConversationRepository

class ObserveMessagesUseCase(
    private val conversationRepository: ConversationRepository
) {

    operator fun invoke() = conversationRepository.conversationFlow

}