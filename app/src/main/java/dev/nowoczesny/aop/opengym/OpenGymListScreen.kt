@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nowoczesny.aop.opengym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import dev.nowoczesny.aop.opengym.domain.LocationEntity
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp

import timber.log.Timber


@Composable
fun PlacesListScreen(
    viewModel: PlaceListViewModel = koinViewModel(),
    navigateToDetailScreen: (String) -> Unit
) {

    val state: State<PlaceListState> = viewModel.stateFlow.collectAsState()

    val placeListState = state.value
    Box {
        PlacesMapScreenContent(placeListState = placeListState) {
            navigateToDetailScreen(it.id)
        }
        PlacesSearchBar(
            searchQuery = placeListState.searchQuery,
            hints = placeListState.searchHints
        ) {
            viewModel.search(it)
        }
    }
}

@Composable
fun PlacesSearchBar(searchQuery: String, hints: List<String>, search: (String) -> Unit) {

    var searchText: String by remember {
        mutableStateOf(searchQuery)
    }
    var active: Boolean by remember {
        mutableStateOf(false)
    }

    SearchBar(
        query = searchText,
        onQueryChange = { searchText = it },
        onSearch = {
            search(it)
            active = false
        },
        active = active,
        onActiveChange = { active = it },
        modifier = Modifier
            .fillMaxWidth()
    ) {
        hints.forEach {
            ListItem(headlineContent = { Text(it) }, modifier = Modifier.clickable {
                searchText = it
                search(it)
                active = false
            })
        }
    }
}

@Composable
private fun PlacesMapScreenContent(
    placeListState: PlaceListState,
    clicked: (PlaceElementDisplayable) -> Unit
) {

    val mapProperties by remember {
        mutableStateOf(
            MapProperties(latLngBoundsForCameraTarget = placeListState.latLngBounds)
        )
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(mapToolbarEnabled = false)
        )
    }

    var shortDetailsState by remember {
        mutableStateOf(ShortDetailsState())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(properties = mapProperties, uiSettings = mapUiSettings) {

            MapEffect(key1 = Unit) {
                it.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        placeListState.latLngBounds,
                        20
                    )
                )
            }

            placeListState.gymList.forEach { displayable ->
                Marker(
                    state = MarkerState(position = displayable.toLatLng()),
                    onClick = {
                        shortDetailsState = ShortDetailsState(visible = true, content = displayable)
                        false
                    }
                )
            }
        }

        if (shortDetailsState.visible) {
            ModalBottomSheet(onDismissRequest = {
                shortDetailsState = shortDetailsState.copy(visible = false)
            }) {
                shortDetailsState.content?.let {
                    PlaceListElement(element = it) {
                        clicked(it)
                    }
                }
            }
        }
    }
}

data class ShortDetailsState(
    val visible: Boolean = false,
    val content: PlaceElementDisplayable? = null
)


@Composable
private fun PlacesListScreenContent(
    placeListState: PlaceListState,
    clicked: (PlaceElementDisplayable) -> Unit
) {
    Column() {
        if (placeListState.loading) {
            CircularProgressIndicator()
        }
        if (placeListState.error != null) {
            Text(text = "Error: ${placeListState.error}")
        }
        LazyColumn() {
            items(placeListState.gymList) { gymElement ->
                PlaceListElement(element = gymElement, clicked = {
                    clicked(gymElement)
                    Timber.d("Clicked place list element: $gymElement")
                })
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PlacesListScreenPreview() {

    val element = PlaceElementDisplayable(
        id = "123",
        name = "boisko do siatkówki plażowej",
        shortDescription = "zlokalizowane przy basenie Wandzianka w Nowej Hucie w Krakowie",
        imageUrl =
        "https://lh5.googleusercontent.com/p/AF1QipPIb4AB9J2pII0elSMIW1E1R-lU7XNlAmcHU_1q=w408-h306-k-no",
        location = LocationEntity(0.0, 0.0)
    )

    PlacesListScreenContent(
        placeListState = PlaceListState(
            gymList = listOf(element, element, element),
            loading = false,
            error = "wystąpił błąd"
        ),
        clicked = {}
    )
}

@Composable
fun CreatePlaceScreen() {

    val viewModel: CreatePlaceViewModel = koinViewModel()

    val state = viewModel.state.collectAsState().value

    EditPlaceScreen(initialPlace = state) {
        viewModel.save(it)
    }

}

@Preview(showSystemUi = true)
@Composable
fun EditPlaceScreenPreview(){
    EditPlaceScreen(initialPlace = CreatePlaceState.EMPTY) {

    }
}

@Composable
fun EditPlaceScreen(
    initialPlace: CreatePlaceState,
    onSave: (CreatePlaceState) -> Unit
) {
    var name by remember { mutableStateOf(initialPlace.name) }
    var description by remember { mutableStateOf(initialPlace.description) }
    var address by remember { mutableStateOf(initialPlace.address) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Edit Place", style = MaterialTheme.typography.labelMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val updatedPlace = CreatePlaceState(name, description, address)
                onSave(updatedPlace)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
    }
}
