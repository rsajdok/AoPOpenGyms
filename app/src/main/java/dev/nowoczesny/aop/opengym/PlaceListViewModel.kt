package dev.nowoczesny.aop.opengym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nowoczesny.aop.opengym.domain.FetchAllGymData
import dev.nowoczesny.aop.opengym.domain.GymEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

class PlaceListViewModel(
    private val fetchAllGymData: FetchAllGymData,
    private val searchGymData: SearchGymData,
    private val searchHistory: SearchHistory
) : ViewModel() {


    fun search(searchQuery: String) {
        searchHistory.saveSearch(searchQuery)

        mutableStateFlow.value =
            mutableStateFlow.value.copy(searchHints = searchHistory.getAllSearches(), searchQuery = searchQuery)

        viewModelScope.launch(Dispatchers.IO) {
            val entityList = searchGymData.execute(searchQuery)

            mutableStateFlow.value = mutableStateFlow.value.copy(gymList = entityList.map { it.toDisplayable() })
        }
    }

    private val mutableStateFlow: MutableStateFlow<PlaceListState> = MutableStateFlow(
        PlaceListState(
            gymList = emptyList(),
            searchHints = searchHistory.getAllSearches(),
            loading = true,
            error = null
        )
    )

    val stateFlow: StateFlow<PlaceListState>
        get() = mutableStateFlow.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                val data: List<GymEntity> = fetchAllGymData.execute()

                val gymList: List<PlaceElementDisplayable> = data.map {
                    it.toDisplayable()
                }

                mutableStateFlow.value = mutableStateFlow.value.copy(
                    gymList = gymList,
                    loading = false
                )
            } catch (exception: HttpException) {
                Timber.e(exception)
                mutableStateFlow.value = mutableStateFlow.value.copy(
                    loading = false,
                    error = exception.code().toString()
                )
            }
        }
    }
}