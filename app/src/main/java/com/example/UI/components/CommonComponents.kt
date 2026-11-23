package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer

@Composable
fun SectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = InkPrimary,
      fontWeight = FontWeight.SemiBold,
    )
    if (actionLabel != null && onActionClick != null) {
      Row(
        modifier =
          Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onActionClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = EmeraldPrimary,
          fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = null,
          tint = EmeraldPrimary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun CleanCard(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
  borderWidth: Dp = 0.75.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = MaterialTheme.shapes.medium
  Surface(
    modifier =
      modifier.fillMaxWidth().let {
        if (onClick != null) it.clip(shape).clickable(onClick = onClick) else it
      },
    shape = shape,
    color = containerColor,
    border = BorderStroke(borderWidth, borderColor),
    tonalElevation = 0.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp), content = content)
  }
}

@Composable
fun SampleDataBanner(
  modifier: Modifier = Modifier,
  text: String = "Database Lokal Room • Data tersimpan permanen di perangkat",
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = EmeraldContainer.copy(alpha = 0.65f),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        tint = EmeraldPrimary,
        modifier = Modifier.size(17.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = OnEmeraldContainer,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
fun IconBadge(
  icon: ImageVector,
  containerColor: Color,
  iconColor: Color,
  modifier: Modifier = Modifier,
  size: Int = 42,
) {
  Box(
    modifier = modifier.size(size.dp).clip(CircleShape).background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconColor,
      modifier = Modifier.size((size * 0.52f).dp),
    )
  }
}

@Composable
fun StatusPill(
  text: String,
  backgroundColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier,
  fontWeight: FontWeight = FontWeight.SemiBold,
) {
  Box(
    modifier =
      modifier
        .semantics(mergeDescendants = true) {}
        .clip(RoundedCornerShape(999.dp))
        .background(backgroundColor)
        .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      fontWeight = fontWeight,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun MetricMiniTile(
  label: String,
  value: String,
  accentColor: Color,
  containerColor: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  onClick: (() -> Unit)? = null,
) {
  CleanCard(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surface,
    onClick = onClick,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconBadge(
        icon = icon,
        containerColor = containerColor,
        iconColor = accentColor,
        size = 34,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = InkSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = InkPrimary,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (!subtitle.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = InkSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun UiPreviewNoticeDialog(
  title: String,
  message: String,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = InkSecondary,
      )
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(
          text = stringResource(R.string.tutup),
          color = EmeraldPrimary,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
  )
}
