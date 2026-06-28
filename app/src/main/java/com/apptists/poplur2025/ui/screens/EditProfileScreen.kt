package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.components.PoplurText

@Composable
fun EditProfileScreen(
    profile: UserProfile,
    onSave: (UserProfile, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                PoplurText("CANCEL", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Gray)
            }
            TextButton(
                onClick = {
                    onSave(profile.copy(displayName = displayName, username = username, bio = bio), null)
                }
            ) {
                PoplurText("SAVE", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF7B78FF))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PoplurText(
                "Edit Profile",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            EditField("Display Name", displayName) { displayName = it }
            EditField("Username", username) { username = it }
            EditField("Bio", bio, isMultiLine = true) { bio = it }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    isMultiLine: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        PoplurText(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp))
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                ),
                minLines = if (isMultiLine) 3 else 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    MaterialTheme {
        EditProfileScreen(
            profile = UserProfile(),
            onSave = { _, _ -> },
            onDismiss = {}
        )
    }
}
