package com.example.moviecatalog.feature.create

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.moviecatalog.R
import com.example.moviecatalog.core.designsystem.movieCardContainer
import com.example.moviecatalog.core.designsystem.posterFrame
import com.example.moviecatalog.domain.model.MovieCreateDto
import com.example.moviecatalog.ui.theme.MovieCatalogTheme

@Composable
fun CreateMovieScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateMovieViewModel = viewModel(factory = CreateMovieViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val readImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.onPosterSelected(uri.toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val onSelectPosterClick = {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            readImagesPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.onPermissionResult(true)
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            permissionLauncher.launch(readImagesPermission)
        }
    }

    CreateMovieContent(
        state = state,
        onSelectPoster = onSelectPosterClick,
        onTitleChange = viewModel::onTitleChange,
        onOriginalTitleChange = viewModel::onOriginalTitleChange,
        onYearChange = viewModel::onYearChange,
        onRatingChange = viewModel::onRatingChange,
        onRuntimeChange = viewModel::onRuntimeChange,
        onGenresChange = viewModel::onGenresChange,
        onDirectorChange = viewModel::onDirectorChange,
        onCastChange = viewModel::onCastChange,
        onCountryChange = viewModel::onCountryChange,
        onPlotChange = viewModel::onPlotChange,
        onSaveClick = viewModel::onSaveClick,
        onCancelClick = viewModel::onCancelClick,
        modifier = modifier
    )
}

@Composable
fun CreateMovieContent(
    state: CreateMovieUiState,
    onSelectPoster: () -> Unit,
    onTitleChange: (String) -> Unit,
    onOriginalTitleChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onRuntimeChange: (String) -> Unit,
    onGenresChange: (String) -> Unit,
    onDirectorChange: (String) -> Unit,
    onCastChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onPlotChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dto = state.dto

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .movieCardContainer()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(140.dp)
                    .posterFrame(),
                contentAlignment = Alignment.Center
            ) {
                if (dto.posterUri != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(dto.posterUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = dto.title.ifBlank { "Poster preview" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            EmptyPosterBox()
                        }
                    )
                } else {
                    EmptyPosterBox()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onSelectPoster,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (dto.posterUri == null) R.string.select_poster else R.string.change_poster
                        )
                    )
                }

                Text(
                    text = stringResource(R.string.gallery_permission_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.saved_in_appdata_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .movieCardContainer()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dto.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.field_title_label)) },
                isError = state.titleError != null,
                supportingText = state.titleError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dto.originalTitle,
                onValueChange = onOriginalTitleChange,
                label = { Text(stringResource(R.string.field_original_title_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dto.yearText,
                    onValueChange = onYearChange,
                    label = { Text(stringResource(R.string.field_year_label)) },
                    isError = state.yearError != null,
                    supportingText = state.yearError?.let { error -> { Text(error) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = dto.ratingText,
                    onValueChange = onRatingChange,
                    label = { Text(stringResource(R.string.field_rating_label)) },
                    isError = state.ratingError != null,
                    supportingText = state.ratingError?.let { error -> { Text(error) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = dto.runtimeText,
                onValueChange = onRuntimeChange,
                label = { Text(stringResource(R.string.field_runtime_label)) },
                isError = state.runtimeError != null,
                supportingText = state.runtimeError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dto.genresText,
                onValueChange = onGenresChange,
                label = { Text(stringResource(R.string.field_genres_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dto.director,
                onValueChange = onDirectorChange,
                label = { Text(stringResource(R.string.field_director_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dto.castText,
                onValueChange = onCastChange,
                label = { Text(stringResource(R.string.field_cast_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dto.country,
                onValueChange = onCountryChange,
                label = { Text(stringResource(R.string.field_country_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dto.plot,
                onValueChange = onPlotChange,
                label = { Text(stringResource(R.string.field_plot_label)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = onSaveClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.save_movie))
            }
        }
    }
}

@Composable
private fun EmptyPosterBox() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.poster_preview_empty),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateMovieScreenPreview() {
    MovieCatalogTheme {
        CreateMovieContent(
            state = CreateMovieUiState(
                dto = MovieCreateDto(
                    title = "Тіні забутих предків",
                    yearText = "1965",
                    ratingText = "8.8",
                    genresText = "Drama, Romance"
                )
            ),
            onSelectPoster = {},
            onTitleChange = {},
            onOriginalTitleChange = {},
            onYearChange = {},
            onRatingChange = {},
            onRuntimeChange = {},
            onGenresChange = {},
            onDirectorChange = {},
            onCastChange = {},
            onCountryChange = {},
            onPlotChange = {},
            onSaveClick = {},
            onCancelClick = {}
        )
    }
}
