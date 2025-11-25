package com.example.team_23_kotlin.presentation.feedback

import android.net.Uri

sealed class FeedbackEvent {
    data class RatingChanged(val rating: Int) : FeedbackEvent()
    data class CommentChanged(val comment: String) : FeedbackEvent()
    data class PhotoAdded(val uri: Uri) : FeedbackEvent()
    data class PhotoRemovedAt(val index: Int) : FeedbackEvent()
    object SubmitClicked : FeedbackEvent()
}
