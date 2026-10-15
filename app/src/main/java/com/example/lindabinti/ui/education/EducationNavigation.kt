package com.example.lindabinti.ui.education

sealed class EducationDest {
    data object Hub : EducationDest()
    data class Topic(val id: String) : EducationDest()
}

object EducationNavigation {
    fun openTopic(from: EducationDest, topicId: String): EducationDest {
        if (topicId.isBlank()) return from
        return EducationDest.Topic(topicId)
    }

    /** Null means leave Education and return to Home. */
    fun back(current: EducationDest): EducationDest? = when (current) {
        EducationDest.Hub -> null
        is EducationDest.Topic -> EducationDest.Hub
    }
}
