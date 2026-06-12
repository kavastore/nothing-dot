package tech.dotlab.dot.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import tech.dotlab.dot.designsystem.DotColors
import tech.dotlab.dot.designsystem.DotMatrixPreview
import tech.dotlab.dot.designsystem.SectionLabel
import tech.dotlab.dot.device.ShapeMask
import tech.dotlab.dot.feature.editor.domain.EditorDocument

/** Grid of saved drawings; tap to open, long-press to set as AOD toy or delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.factory(LocalContext.current)),
) {
    val items by viewModel.gallery.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SectionLabel("(02) DRAW — GALLERY") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew, containerColor = DotColors.Red) {
                Text("+", fontSize = 24.sp, color = DotColors.White)
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = "Пока пусто.\nНажми + чтобы нарисовать.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(items, key = { it.id }) { doc ->
                    GalleryCell(doc = doc, onClick = { onOpen(doc.id) })
                }
            }
        }
    }
}

@Composable
private fun GalleryCell(doc: EditorDocument, onClick: () -> Unit) {
    val mask = rememberMask(doc.matrixSize)
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            DotMatrixPreview(
                mask = mask,
                frame = doc.frames.first().frame,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = doc.name + if (doc.isAodToy) "  ●" else "",
            color = if (doc.isAodToy) DotColors.Red else MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun rememberMask(size: Int): ShapeMask =
    remember(size) { ShapeMask.circle(size) }
