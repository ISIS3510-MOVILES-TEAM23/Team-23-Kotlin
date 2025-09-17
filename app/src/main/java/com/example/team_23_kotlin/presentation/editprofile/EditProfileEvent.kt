sealed class EditProfileEvent {
    data class OnNameChanged(val name: String) : EditProfileEvent()
    data class OnEmailChanged(val email: String) : EditProfileEvent()
    data class OnPhoneChanged(val phone: String) : EditProfileEvent()
    object OnSaveClicked : EditProfileEvent()
}
