package tokyo.isseikuzumaki.signalinglib.demo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.outlined.ArrowCircleLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DPadComponent(
    onDPadCenterClick: () -> Unit = {},
    onDPadUpClick: () -> Unit = {},
    onDPadDownClick: () -> Unit = {},
    onDPadLeftClick: () -> Unit = {},
    onDPadRightClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DPAD Section
        DPadCross(
            onCenterClick = onDPadCenterClick,
            onUpClick = onDPadUpClick,
            onDownClick = onDPadDownClick,
            onLeftClick = onDPadLeftClick,
            onRightClick = onDPadRightClick
        )

        // Navigation Buttons Section
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationButton(
                text = "Back",
                onClick = onBackClick
            )

            NavigationButton(
                text = "Home",
                onClick = onHomeClick
            )
        }
    }
}

@Composable
private fun DPadCross(
    onCenterClick: () -> Unit,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DPadCrossRow {
                // Up button
                DPadButton(
                    onClick = onUpClick,
                    modifier = Modifier
                        .size(50.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleLeft,
                        contentDescription = "Up",
                        modifier = Modifier.fillMaxSize().rotate(90f),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            DPadCrossRow {
                // Left button
                DPadButton(
                    onClick = onLeftClick,
                    modifier = Modifier
                        .size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleLeft,
                        contentDescription = "Left",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }

                // Center button
                DPadButton(
                    onClick = onCenterClick,
                    modifier = Modifier
                        .size(60.dp),
                    isCenter = true
                ) {
                    Icon(
                        imageVector = Icons.Filled.Adjust,
                        contentDescription = "Right",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }

                // Right button
                DPadButton(
                    onClick = onRightClick,
                    modifier = Modifier
                        .size(50.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleRight,
                        contentDescription = "Right",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            DPadCrossRow {
                Spacer(modifier = Modifier.weight(1f))
                // Down button
                DPadButton(
                    onClick = onDownClick,
                    modifier = Modifier
                        .size(50.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowCircleRight,
                        contentDescription = "Down",
                        modifier = Modifier.fillMaxSize().rotate(90f),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DPadCrossRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun DPadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCenter: Boolean = false,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape),
//        colors = ButtonDefaults.buttonColors(
//            containerColor = if (isCenter) MaterialTheme.colorScheme.primary
//                           else MaterialTheme.colorScheme.secondary
//        ),
//        contentPadding = PaddingValues(8.dp)
    ) {
        content()
    }
}

@Composable
private fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
fun DPadComponentPreview() {
    MaterialTheme {
        Surface {
            DPadComponent()
        }
    }
}
