package org.celimited.manager.feature.distributorDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_filter
import manager.composeapp.generated.resources.ic_icon_next
import manager.composeapp.generated.resources.ic_icon_search
import org.jetbrains.compose.resources.painterResource

private val IssueTitle = Color(0xFF111827)
private val IssueSubtle = Color(0xFF6B7280)
private val IssueBorder = Color(0xFFE5E7EB)
private val IssueAccent = Color(0xFF582FFF)

// Hint and typed text share one style so they sit on the same baseline.
private val IssueSearchStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = IssueTitle)

private fun IssueStatus.color(): Color = when (this) {
    IssueStatus.Pending -> Color(0xFFF97316)
    IssueStatus.InProgress -> Color(0xFF16A34A)
    IssueStatus.Resolved -> Color(0xFF9CA3AF)
}

@Composable
internal fun IssuesTabContent(
    issues: ImmutableList<DistributorIssueUi>,
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onIssueClick: (issueId: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IssueSearchField(query, onQueryChange, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            FilterButton(onFilterClick)
        }

        Text(
            text = "Pending Issues",
            fontSize = 15.sp,
            color = IssueTitle,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (issues.isEmpty()) {
            Text(
                text = "No issues found",
                fontSize = 13.sp,
                color = IssueSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )
        } else {
            issues.forEach { issue ->
                IssueCard(issue, onClick = { onIssueClick(issue.id) })
            }
        }
    }
}

@Composable
private fun IssueSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = IssueSearchStyle,
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_search),
                    contentDescription = null,
                    tint = IssueSubtle,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Search issues...", style = IssueSearchStyle.copy(color = IssueSubtle))
                    }
                    inner()
                }
            }
        }
    )
}

@Composable
private fun FilterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(IssueAccent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_icon_filter),
            contentDescription = "Filter issues",
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .offset(x = (-6).dp)
        )
        // Active-filter dot, as in the design.
        Box(
            Modifier
                .offset(x = 12.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun IssueCard(issue: DistributorIssueUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, IssueBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IssueChip(issue.priority.label, IssueAccent)
                IssueChip(issue.status.label, issue.status.color())
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = issue.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = IssueTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Assigned: ${issue.assignedTo}   Due: ${issue.dueDate}",
                fontSize = 12.sp,
                color = Color(0xFF4B5563),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(Res.drawable.ic_icon_next),
            contentDescription = null,
            tint = IssueSubtle,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun IssueChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}
