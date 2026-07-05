package com.vidyarthi.lalkitab.ui.panchang

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.data.PanchangDayInfo
import com.vidyarthi.lalkitab.utils.PanchangLabelResolver
import com.vidyarthi.lalkitab.utils.PanchangUiState
import com.vidyarthi.lalkitab.utils.PlanetNames
import com.vidyarthi.lalkitab.utils.WeekdayNames
import kotlinx.coroutines.launch

private val Maroon = Color(0xFF7A1010)
private val MaroonDeep = Color(0xFF5E0C0C)
private val Saffron = Color(0xFFF4A623)
private val Gold = Color(0xFFE8C85C)
private val GoldLight = Color(0xFFF5DFA0)
private val CreamCard = Color(0xFFFAF8F4)
private val CreamCardSoft = Color(0xF2FFFDF8)
private val LabelMuted = Color(0xFF9A5A4A)
private val TabInactive = Color(0xFF8A6A60)

private enum class PanchangSection(@StringRes val labelRes: Int, val scrollIndex: Int) {
    SAMAY(R.string.panchang_tab_samay, 2),
    VIGAT(R.string.panchang_elements, 4),
    GRAHA(R.string.panchang_tab_lalkitab_graha, 8)
}

@Composable
fun PanchangScreen(viewModel: PanchangViewModel) {
    val uiState: PanchangUiState by viewModel.uiState.collectAsState()
    PanchangScreenBody(uiState)
}

@Composable
fun PanchangScreen(uiState: PanchangUiState) {
    PanchangScreenBody(uiState)
}

@Composable
private fun PanchangScreenBody(uiState: PanchangUiState) {
    when {
        uiState.isLoading -> LoadingView()
        uiState.error != null -> ErrorView(uiState.error!!)
        uiState.dayInfo == null -> LoadingView()
        else -> PanchangContent(uiState)
    }
}

@Composable
private fun PanchangContent(state: PanchangUiState) {
    val day = state.dayInfo ?: return
    var selectedSection by remember { mutableIntStateOf(PanchangSection.SAMAY.ordinal) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { PanchangHeroHeader(state) }

        item {
            PanchangSectionTabRow(
                selectedIndex = selectedSection,
                onSelected = { index ->
                    selectedSection = index
                    val target = PanchangSection.entries[index].scrollIndex
                    scope.launch {
                        listState.animateScrollToItem(target)
                    }
                }
            )
        }

        item { CelestialTimesGrid(day) }
        item { DayNightDurationCard(day) }

        item { SectionTitle(stringResource(R.string.panchang_elements)) }

        item { TithiPakshaCard(state) }
        item { NakshatraCard(state) }
        item { YogaKaranaCard(state) }

        item { LalKitabGrahaCard(state) }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PanchangSectionTabRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    PremiumSurfaceCard(accentTop = false, contentPadding = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PanchangSection.entries.forEachIndexed { index, section ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelected(index) }
                        .background(
                            if (selected) Saffron.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(section.labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = if (selected) 14.sp else 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 17.sp
                        ),
                        color = if (selected) MaroonDeep else TabInactive,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (selected) 0.72f else 0f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(listOf(Gold, Saffron, Gold))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PanchangHeroHeader(state: PanchangUiState) {
    val ctx = LocalContext.current
    PremiumSurfaceCard(accentTop = true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.panchang_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 28.sp,
                    letterSpacing = 0.5.sp,
                    lineHeight = 32.sp
                ),
                color = Maroon
            )

            Spacer(Modifier.height(6.dp))

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .padding(vertical = 4.dp),
                thickness = 2.dp,
                color = Gold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val summaryRows = buildList {
                    state.dayInfo?.let { day ->
                        add(
                            stringResource(R.string.weekday_label) to
                                WeekdayNames.localizedFromGujarati(ctx, day.weekday)
                        )
                    }
                    state.monthInfo?.let { m ->
                        add(
                            stringResource(R.string.month_label) to
                                PanchangLabelResolver.maas(ctx, m.maasIndex)
                        )
                        add(
                            stringResource(R.string.adhik_month) to
                                stringResource(if (m.isAdhik) R.string.yes else R.string.no)
                        )
                        m.amavasyaDate?.let { date ->
                            add(stringResource(R.string.amavasya) to date.toString())
                        }
                    }
                    state.vikramSamvat?.let { vs ->
                        add(stringResource(R.string.vikram_samvat) to vs.toString())
                    }
                }
                summaryRows.forEachIndexed { index, (label, value) ->
                    HeroChip(label = label, value = value)
                    if (index < summaryRows.lastIndex) {
                        SummaryRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 5.dp),
        color = Saffron.copy(alpha = 0.32f),
        thickness = 1.dp
    )
}

@Composable
private fun HeroChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CreamCardSoft)
            .border(1.dp, Saffron.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp),
            color = LabelMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            ),
            color = Maroon,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CelestialTimesGrid(day: PanchangDayInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CelestialTimeCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.sunrise),
                time = day.sunrise,
                symbol = "☀",
                watermark = "☉"
            )
            CelestialTimeCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.sunset),
                time = day.sunset,
                symbol = "🌇",
                watermark = "♌"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CelestialTimeCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.moonrise),
                time = day.moonrise ?: "--",
                symbol = "🌙",
                watermark = "☽"
            )
            CelestialTimeCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.moonset),
                time = day.moonset ?: "--",
                symbol = "🌑",
                watermark = "✦"
            )
        }
    }
}

@Composable
private fun CelestialTimeCard(
    modifier: Modifier = Modifier,
    label: String,
    time: String,
    symbol: String,
    watermark: String
) {
    Card(
        modifier = modifier
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(Saffron, Gold, Saffron)),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = traditionalCardElevation()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp)
        ) {
            AstrologyWatermark(watermark, Modifier.align(Alignment.BottomEnd))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldLight.copy(alpha = 0.9f),
                                    Saffron.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .border(1.dp, Gold.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = symbol, fontSize = 26.sp)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(lineHeight = 17.sp),
                    color = Saffron,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = time,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    color = Maroon,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DayNightDurationCard(day: PanchangDayInfo) {
    PremiumSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DurationTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.day_length),
                value = day.dayLength,
                symbol = "☀",
                watermark = "☉"
            )
            DurationTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.night_length),
                value = day.nightLength,
                symbol = "🌙",
                watermark = "☽"
            )
        }
    }
}

@Composable
private fun DurationTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    symbol: String,
    watermark: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CreamCardSoft)
            .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        AstrologyWatermark(watermark, Modifier.align(Alignment.BottomEnd).padding(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GoldLight.copy(alpha = 0.85f),
                                Saffron.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(1.2.dp, Gold.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = symbol, fontSize = 32.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp),
                color = LabelMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp
                ),
                color = Maroon,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AstrologyWatermark(symbol: String, modifier: Modifier = Modifier) {
    Text(
        text = symbol,
        fontSize = 64.sp,
        color = Gold.copy(alpha = 0.1f),
        modifier = modifier
    )
}

@Composable
private fun TithiPakshaCard(state: PanchangUiState) {
    val ctx = LocalContext.current
    val tithi = state.tithiInfo
    val paksha = state.pakshaInfo
    if (tithi == null && paksha == null) return

    DetailGroupCard(title = stringResource(R.string.tithi_and_paksha)) {
        val rows = buildList {
            tithi?.let {
                add(
                    stringResource(R.string.tithi) to
                        PanchangLabelResolver.tithi(ctx, it.uditaTithiIndex)
                )
                if (it.endTime.isNotBlank()) {
                    add(stringResource(R.string.tithi_end) to it.endTime)
                }
                if (it.janmaTithi.isNotBlank()) {
                    add(
                        stringResource(R.string.tithi_janma) to
                            PanchangLabelResolver.tithi(ctx, it.janmaTithiIndex)
                    )
                }
            }
            paksha?.let {
                add(
                    stringResource(R.string.paksha) to
                        PanchangLabelResolver.paksha(ctx, it.paksha)
                )
            }
        }
        CompactDetailRows(rows)
    }
}

@Composable
private fun NakshatraCard(state: PanchangUiState) {
    val ctx = LocalContext.current
    val n = state.nakshatraInfo ?: return
    DetailGroupCard(title = stringResource(R.string.nakshatra)) {
        val rows = buildList {
            add(
                stringResource(R.string.nakshatra) to
                    stringResource(
                        R.string.nakshatra_with_pada,
                        PanchangLabelResolver.nakshatra(ctx, n.nakshatraIndex),
                        n.pada
                    )
            )
            if (n.endTime.isNotBlank()) {
                add(stringResource(R.string.nakshatra_end) to n.endTime)
            }
        }
        CompactDetailRows(rows)
    }
}

@Composable
private fun YogaKaranaCard(state: PanchangUiState) {
    val ctx = LocalContext.current
    val yk = state.yogaKaranaInfo ?: return
    DetailGroupCard(title = stringResource(R.string.yoga_and_karana)) {
        val rows = buildList {
            add(
                stringResource(R.string.yoga) to
                    PanchangLabelResolver.yoga(ctx, yk.yogaIndex)
            )
            if (yk.yogaEndTime.isNotBlank()) {
                add(stringResource(R.string.yoga_end) to yk.yogaEndTime)
            }
            add(
                stringResource(R.string.karana) to
                    PanchangLabelResolver.karana(ctx, yk.karanaIndex)
            )
            if (yk.karanaEndTime.isNotBlank()) {
                add(stringResource(R.string.karana_end) to yk.karanaEndTime)
            }
        }
        CompactDetailRows(rows)
    }
}

@Composable
private fun LalKitabGrahaCard(state: PanchangUiState) {
    val ctx = LocalContext.current
    val missing = stringResource(R.string.mukhya_grah_missing)
    DetailGroupCard(
        title = stringResource(R.string.lalkitab_birth_planets_title),
        accentSaffron = true
    ) {
        GrahaPremiumRow(
            label = stringResource(R.string.mukhya_birth_weekday_planet),
            planetName = state.birthdayPlanetKey?.let { PlanetNames.localizedName(ctx, it) } ?: missing
        )
        Spacer(Modifier.height(8.dp))
        GrahaPremiumRow(
            label = stringResource(R.string.mukhya_birth_time_planet_label),
            planetName = state.birthtimePlanetKey?.let { PlanetNames.localizedName(ctx, it) } ?: missing
        )
    }
}

@Composable
private fun GrahaPremiumRow(label: String, planetName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CreamCardSoft)
            .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GoldLight.copy(alpha = 0.88f),
                            Saffron.copy(alpha = 0.22f)
                        )
                    )
                )
                .border(1.5.dp, Saffron.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = planetGlyph(planetName),
                fontSize = 24.sp,
                color = Maroon
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(lineHeight = 15.sp),
                color = LabelMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = planetName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                ),
                color = Maroon
            )
        }
    }
}

private fun planetGlyph(planetName: String): String {
    val n = planetName.trim()
    return when {
        n.contains("Sun", ignoreCase = true) || n.contains("સૂર્ય") -> "☉"
        n.contains("Moon", ignoreCase = true) || n.contains("ચંદ્ર") -> "☽"
        n.contains("Mars", ignoreCase = true) || n.contains("મંગળ") -> "♂"
        n.contains("Mercury", ignoreCase = true) || n.contains("બુધ") -> "☿"
        n.contains("Jupiter", ignoreCase = true) || n.contains("ગુરુ") -> "♃"
        n.contains("Venus", ignoreCase = true) || n.contains("શુક્ર") -> "♀"
        n.contains("Saturn", ignoreCase = true) || n.contains("શનિ") -> "♄"
        n.contains("Rahu", ignoreCase = true) || n.contains("રાહુ") -> "☊"
        n.contains("Ketu", ignoreCase = true) || n.contains("કેતુ") -> "☋"
        else -> "✦"
    }
}

@Composable
private fun traditionalCardElevation() = CardDefaults.cardElevation(
    defaultElevation = 4.dp,
    pressedElevation = 6.dp,
    focusedElevation = 4.dp,
    hoveredElevation = 4.dp,
    draggedElevation = 6.dp,
    disabledElevation = 0.dp
)

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Saffron.copy(alpha = 0.5f),
            thickness = 1.dp
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                letterSpacing = 0.3.sp,
                lineHeight = 24.sp
            ),
            color = Maroon
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Saffron.copy(alpha = 0.5f),
            thickness = 1.dp
        )
    }
}

@Composable
private fun PremiumSurfaceCard(
    accentTop: Boolean = false,
    contentPadding: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (accentTop) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(listOf(Gold, Saffron, Gold)),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier.border(
                        1.dp,
                        Saffron.copy(alpha = 0.55f),
                        RoundedCornerShape(20.dp)
                    )
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = traditionalCardElevation()
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun DetailGroupCard(
    title: String,
    accentSaffron: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (accentSaffron) 1.5.dp else 1.dp,
                color = if (accentSaffron) Saffron else Gold.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = traditionalCardElevation()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            DetailCardHeading(title = title, accentSaffron = accentSaffron)
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                color = Gold.copy(alpha = 0.45f),
                thickness = 1.dp
            )
            content()
        }
    }
}

@Composable
private fun DetailCardHeading(title: String, accentSaffron: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "◆",
            fontSize = 13.sp,
            color = Gold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp
            ),
            color = if (accentSaffron) Saffron else Maroon
        )
    }
}

@Composable
private fun CompactDetailRows(rows: List<Pair<String, String>>) {
    rows.forEachIndexed { index, (label, value) ->
        DetailFieldRow(label = label, value = value)
        if (index < rows.lastIndex) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 3.dp),
                color = Saffron.copy(alpha = 0.28f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun DetailFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.width(108.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                lineHeight = 17.sp
            ),
            color = LabelMuted
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp
            ),
            color = Maroon,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Saffron)
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.toast_error_prefix, message),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Maroon,
            textAlign = TextAlign.Center
        )
    }
}
