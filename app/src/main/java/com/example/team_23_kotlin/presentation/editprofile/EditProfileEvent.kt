package com.example.team_23_kotlin.presentation.editprofile

sealed class EditProfileEvent {
    data class OnNameChanged(val value: String) : EditProfileEvent()
    data class OnEmailChanged(val value: String) : EditProfileEvent()
    data class OnPhoneChanged(val value: String) : EditProfileEvent()
    object OnSaveClicked : EditProfileEvent()

    data class OnContactPrefsChanged(val value: List<String>) : EditProfileEvent()

}
