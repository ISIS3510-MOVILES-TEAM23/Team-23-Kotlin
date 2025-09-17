data class EditProfileState(
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
