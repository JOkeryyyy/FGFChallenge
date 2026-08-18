package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme

/**
 * Search-as-you-type field for the caller's free-text query.
 *
 * The placeholder names `message` and log ID because those are the only fields the product searches
 * — structured conditions such as tag and severity are the filter sheet's, not this field's — and a
 * placeholder that promised more would be the one part of the screen telling the user otherwise.
 *
 * [enabled] lets a caller lock the field while it has nothing to search yet.
 */
@Composable
fun LogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = { Text(stringResource(R.string.log_search_field_placeholder)) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.log_search_field_clear_action),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogSearchFieldPreview() {
    FGFChallengeTheme {
        var query by remember { mutableStateOf("network") }
        LogSearchField(
            query = query,
            onQueryChange = { query = it },
            enabled = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogSearchFieldDisabledPreview() {
    FGFChallengeTheme {
        LogSearchField(
            query = "",
            onQueryChange = {},
            enabled = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}
