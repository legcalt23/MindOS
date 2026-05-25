package com.example.neuv3.features.wallet.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuv3.features.wallet.domain.model.SandboxPurchaseOffer
import com.example.neuv3.features.wallet.domain.model.WalletBalance
import com.example.neuv3.features.wallet.domain.model.WalletError

/**
 * MoneyScreen
 *
 * Displays the user's virtual currency balance, the sandbox mode badge,
 * and buy buttons for the three sandbox coin packs.
 *
 * Security notes:
 *  - Balance displayed is always the value received from the server — never computed locally.
 *  - The sandbox badge is always visible whenever SANDBOX_PAYMENTS_ENABLED is in effect.
 *    This screen must NEVER be shown without the badge in production.
 *  - Buy buttons are disabled while [WalletUiState.Loaded.isBuying] is true.
 */
@Composable
fun MoneyScreen(
    stateHolder: WalletStateHolder,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by stateHolder.uiState.collectAsState()

    // Trigger balance load on first composition.
    LaunchedEffect(stateHolder) {
        stateHolder.load()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoneyBlack),
    ) {
        MoneyAtmosphere(modifier = Modifier.fillMaxSize())

        when (val state = uiState) {
            WalletUiState.Loading -> MoneyLoadingContent()

            is WalletUiState.Loaded -> MoneyLoadedContent(
                state         = state,
                onBuyPack     = stateHolder::buyPack,
                onDismissError = stateHolder::dismissBuyError,
                onRefresh     = stateHolder::refresh,
                onBack        = onBack,
            )

            is WalletUiState.Error -> MoneyErrorContent(
                error   = state.error,
                onRetry = stateHolder::load,
                onBack  = onBack,
            )
        }
    }
}

// ── Loaded content ────────────────────────────────────────────────────────────

@Composable
private fun MoneyLoadedContent(
    state: WalletUiState.Loaded,
    onBuyPack: (String) -> Unit,
    onDismissError: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 42.dp),
    ) {
        item {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NEU",
                    color = MoneyWhite,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 29.sp),
                )
                MoneyChip(text = "VOLVER", onClick = onBack)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Sandbox badge (always visible in sandbox builds) ───────────────
            SandboxBadge(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(36.dp))

            // ── Balance display ───────────────────────────────────────────────
            BalanceCard(
                balance = state.balance,
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Section label ─────────────────────────────────────────────────
            Text(
                text = "ADQUIRIR MONEDAS",
                color = MoneyMist,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Buy error banner ──────────────────────────────────────────────
            if (state.buyError != null) {
                BuyErrorBanner(
                    error   = state.buyError,
                    onDismiss = onDismissError,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Purchase offer tiles ──────────────────────────────────────────
            state.offers.forEach { offer ->
                PurchaseOfferTile(
                    offer    = offer,
                    isBuying = state.isBuying,
                    onBuy    = { onBuyPack(offer.packId) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Las monedas se acreditan instantáneamente en modo sandbox.",
                color = MoneyMist.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Loading content ───────────────────────────────────────────────────────────

@Composable
private fun MoneyLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MoneyGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando billetera.",
                color = MoneyMist,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── Error content ─────────────────────────────────────────────────────────────

@Composable
private fun MoneyErrorContent(
    error: WalletError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No se pudo cargar la billetera.",
            color = MoneyWhite,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error.message,
            color = MoneyMist,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyChip(text = "REINTENTAR", onClick = onRetry)
            MoneyChip(text = "VOLVER", onClick = onBack)
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

/**
 * Sandbox mode badge — must be visually prominent.
 * Signals to the user that this is not a real-money environment.
 */
@Composable
private fun SandboxBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SandboxAmber.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, SandboxAmber.copy(alpha = 0.55f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = SandboxAmber, radius = size.minDimension / 2f)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "MODO SANDBOX — monedas sin valor real",
            color = SandboxAmber,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Balance card. Shows available coins from the server.
 * Refresh button triggers a background balance refresh.
 */
@Composable
private fun BalanceCard(
    balance: WalletBalance,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.065f),
                        MoneyGreen.copy(alpha = 0.045f),
                    ),
                ),
            )
            .border(BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.30f)), RoundedCornerShape(10.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SALDO DISPONIBLE",
            color = MoneyMist,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.8.sp,
                fontSize = 10.sp,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isRefreshing) {
            CircularProgressIndicator(
                color = MoneyGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Text(
                text = balance.availableBalance.toString(),
                color = MoneyGreen,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 64.sp,
                ),
            )
        }

        Text(
            text = "monedas",
            color = MoneyMist,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (!isRefreshing) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BalanceBucket(label = "Comprado", amount = balance.purchasedBalance)
                BalanceBucket(label = "Promo",    amount = balance.promotionalBalance)
                BalanceBucket(label = "Ganado",   amount = balance.earnedBalance)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Actualizar",
                color = MoneyGreen.copy(alpha = 0.70f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun BalanceBucket(label: String, amount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = amount.toString(),
            color = MoneyWhite,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = label,
            color = MoneyMist,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        )
    }
}

/**
 * Purchase offer tile — one per coin pack.
 * Disabled while a purchase is in progress.
 */
@Composable
private fun PurchaseOfferTile(
    offer: SandboxPurchaseOffer,
    isBuying: Boolean,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (isBuying) 0.45f else 1f

    Row(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.060f * alpha),
                        MoneyGreen.copy(alpha = 0.040f * alpha),
                    ),
                ),
            )
            .border(
                BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.28f * alpha)),
                RoundedCornerShape(8.dp),
            )
            .clickable(enabled = !isBuying, onClick = onBuy)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(modifier = Modifier.size(54.dp), alpha = alpha)
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = offer.displayLabel.uppercase(),
                color = MoneyWhite.copy(alpha = alpha),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                maxLines = 1,
            )
            Text(
                text = "GRATIS · SANDBOX",
                color = SandboxAmber.copy(alpha = 0.80f * alpha),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                ),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        if (isBuying) {
            CircularProgressIndicator(
                color = MoneyGreen,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Coin icon drawn via Canvas to avoid bitmap dependencies. */
@Composable
private fun CoinIcon(modifier: Modifier = Modifier, alpha: Float = 1f) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MoneyGreen.copy(alpha = 0.10f * alpha))
            .border(
                BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.40f * alpha)),
                androidx.compose.foundation.shape.CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            // Outer ring
            drawCircle(
                color = MoneyGreen.copy(alpha = alpha),
                radius = size.minDimension * 0.40f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.8f),
            )
            // Inner dot
            drawCircle(
                color = MoneyGreen.copy(alpha = 0.35f * alpha),
                radius = size.minDimension * 0.18f,
                center = Offset(cx, cy),
            )
        }
    }
}

/** Dismissable error banner for buy failures. */
@Composable
private fun BuyErrorBanner(
    error: WalletError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ErrorRed.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, ErrorRed.copy(alpha = 0.40f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = error.message,
            color = ErrorRed.copy(alpha = 0.90f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "×",
            color = ErrorRed.copy(alpha = 0.70f),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Small chip button — matches HomeScreen's HomeOverlayButton. */
@Composable
private fun MoneyChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MoneyWhite,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.055f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

/** Background atmosphere — mirrors HomeAtmosphere with a coin-tinted accent. */
@Composable
private fun MoneyAtmosphere(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(MoneyBlack)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MoneyGreen.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.14f),
                radius = size.minDimension * 0.68f,
            ),
        )
        repeat(6) { index ->
            val y = size.height * (0.28f + index * 0.09f)
            drawLine(
                color = MoneyGreen.copy(alpha = 0.038f),
                start = Offset(size.width * 0.06f, y),
                end = Offset(size.width * 0.94f, y - size.height * 0.035f),
                strokeWidth = 1f,
            )
        }
    }
}

// ── Color palette ─────────────────────────────────────────────────────────────

// Matches HomeScreen palette for visual consistency
private val MoneyBlack  = Color(0xFF020504)
private val MoneyWhite  = Color(0xFFF4FFF9)
private val MoneyGreen  = Color(0xFF13ECA4)
private val MoneyMist   = Color(0xFFA7CFC1)

// Sandbox-specific — amber draws attention without alarming
private val SandboxAmber = Color(0xFFFFB038)

// Error state
private val ErrorRed = Color(0xFFFF5252)
