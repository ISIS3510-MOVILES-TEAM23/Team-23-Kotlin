import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val checkInCampusUseCase: CheckInCampusUseCase
) : ViewModel() {

    private val _isInCampus = MutableStateFlow<Boolean?>(null)
    val isInCampus: StateFlow<Boolean?> = _isInCampus.asStateFlow()

    fun refreshCampusStatus() {
        viewModelScope.launch {
            val inside = checkInCampusUseCase()
            _isInCampus.value = inside
        }
    }
}
