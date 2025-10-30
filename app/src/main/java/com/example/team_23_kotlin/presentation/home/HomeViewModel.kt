import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase
import com.example.team_23_kotlin.presentation.home.HomePopupState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log


class HomeViewModel(
    private val checkInCampusUseCase: CheckInCampusUseCase
) : ViewModel() {

    private val _isInCampus = MutableStateFlow<Boolean?>(null)
    val isInCampus: StateFlow<Boolean?> = _isInCampus.asStateFlow()

    private val _popupState = MutableStateFlow(HomePopupState())
    val popupState: StateFlow<HomePopupState> = _popupState.asStateFlow()

    fun refreshCampusStatus() {
        viewModelScope.launch {
            val inside = checkInCampusUseCase()
            _isInCampus.value = inside
        }
    }

    // 🔹 Nueva función para traer la categoría más visitada
    fun loadMostVisitedCategory() {
        viewModelScope.launch {
            _popupState.value = HomePopupState(isLoading = true)
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val firestore = FirebaseFirestore.getInstance()
                val weekAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))

                val snapshot = firestore.collection("product_click_events")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("timestamp", weekAgo)
                    .get()
                    .await()

                val categoryCount = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val category = doc.getString("category") ?: continue
                    categoryCount[category] = (categoryCount[category] ?: 0) + 1
                }

                val mostVisited = categoryCount.maxByOrNull { it.value }?.key
                _popupState.value = HomePopupState(favoriteCategory = mostVisited)

                if (mostVisited != null) {
                    firestore.collection("users")
                        .document(userId)
                        .set(mapOf("favoriteCategory" to mostVisited), SetOptions.merge())
                }

                subscribeToFavoriteTopic(mostVisited)



            } catch (e: Exception) {
                _popupState.value = HomePopupState(error = e.message)
            }
        }
    }

    private fun subscribeToFavoriteTopic(favoriteCategory: String?) {
        val topic = favoriteCategory!!.lowercase()
        FirebaseMessaging.getInstance()
            .subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "✅ Subscribed to topic: $topic")
                } else {
                    Log.e("FCM", "❌ Subscription failed", task.exception)
                }
            }
    }
}
