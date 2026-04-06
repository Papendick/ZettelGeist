package de.zettelgeist.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(
    tag: String,
    count: Int? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = if (count != null) "#$tag ($count)" else "#$tag",
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
    )
}
