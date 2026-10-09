package com.yunok.walzi.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.BlurMaskFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd
import com.yunok.walzi.ads.NativeAdCard
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.components.ThumbImage
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.GradientSignature
import com.yunok.walzi.util.GRID_THUMBNAIL_SIZE
import com.yunok.walzi.util.thumbMemoryKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Cards in the deck; the middle one is slot 2. */
private const val DECK_SIZE = 5
private const val MIDDLE_SLOT = DECK_SIZE / 2
/** Deck cards are drawn at this fraction of the revealed card's size. */
private const val DECK_SCALE = 0.62f

private const val SHUFFLE_MS = 1200
private const val FAN_HOLD_MS = 350L
private const val LIFT_MS = 450
private const val FLIP_MS = 520

/** Card size on tall screens; shrinks on short ones so the native ad never nears the buttons. */
private val CARD_MAX_HEIGHT = 370.dp
private val CARD_MIN_HEIGHT = 260.dp
private const val CARD_ASPECT = 220f / 370f

/** The native ad appears this long after the reveal, so a quick follow-up tap can't hit it. */
private const val AD_REVEAL_DELAY_MS = 600L

/**
 * "Surprise me" floating button in the app's card style: a signature-gradient pill with a soft
 * pulsing glow, a mini fanned deck as the icon and a light shine sweeping across every few
 * seconds (with a card wiggle). Shows the label when [expanded], just the deck otherwise (the
 * caller collapses it while the user scrolls down). All motion is read in the draw/layer phase.
 */
@Composable
fun SurpriseFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val wiggle = remember { Animatable(0f) }
    val shine = remember { Animatable(-1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            launch {
                shine.snapTo(-1f)
                shine.animateTo(2f, tween(900, easing = LinearEasing))
            }
            repeat(2) {
                wiggle.animateTo(12f, tween(90))
                wiggle.animateTo(-12f, tween(120))
            }
            wiggle.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
        }
    }
    val glow by rememberInfiniteTransition(label = "fabGlow").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press"
    )
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // Soft gradient glow behind the pill. A blur mask on the paint (not a blurred layer),
            // so it fades out smoothly instead of being cut off at the layer's rectangle.
            .drawWithCache {
                val radius = 20.dp.toPx()
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    shader = android.graphics.LinearGradient(
                        0f, 0f, size.width, size.height,
                        GradientSignature.map { it.toArgb() }.toIntArray(), null,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
                val top = 4.dp.toPx()
                onDrawBehind {
                    paint.alpha = (glow * 255).toInt()
                    drawIntoCanvas {
                        it.nativeCanvas.drawRoundRect(0f, top, size.width, size.height + top, radius, radius, paint)
                    }
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(shape)
                .background(Brush.linearGradient(GradientSignature))
                .drawWithContent {
                    drawContent()
                    // Diagonal shine band sweeping left to right.
                    val x = shine.value * size.width
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                            start = Offset(x, 0f),
                            end = Offset(x + size.width * 0.35f, size.height)
                        )
                    )
                }
                .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .semantics(mergeDescendants = true) { contentDescription = "Surprise me" }
                .animateContentSize(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            MiniDeckIcon(wiggle = { wiggle.value })
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandHorizontally(),
                exit = fadeOut(tween(120)) + shrinkHorizontally()
            ) {
                Text(
                    "Surprise me",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                )
            }
        }
    }
}

/** Two little fanned cards - the back one translucent, the front one with a sparkle. */
@Composable
private fun MiniDeckIcon(wiggle: () -> Float) {
    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 14.dp, height = 19.dp)
                .graphicsLayer {
                    rotationZ = -16f - wiggle() * 0.5f
                    translationX = -3.5.dp.toPx()
                }
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.45f))
        )
        Box(
            Modifier
                .size(width = 14.dp, height = 19.dp)
                .graphicsLayer {
                    rotationZ = 10f + wiggle()
                    translationX = 3.5.dp.toPx()
                }
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Accent1, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun mix(from: Float, to: Float, t: Float) = from + (to - from) * t

/**
 * Full-screen card-shuffle picker, in the app's playing-card style: 5 face-down Walzi cards
 * riffle in the centre, fan out, one card lifts to the middle and flips in 3D to reveal a random
 * wallpaper, with confetti and a vibration. Tap during the shuffle to skip to the flip. Then:
 * Open (full preview) / Shuffle again / ✕. Wording stays a fun picker - no "win"/odds language.
 *
 * All per-frame motion is read inside graphicsLayer, so the shuffle doesn't recompose.
 *
 * @param pool wallpapers to pick from (already cached on the phone - instant and offline).
 * @param recentPicks ids picked lately; avoided so it feels truly random.
 */
@Composable
fun SurpriseOverlay(
    pool: List<Wallpaper>,
    recentPicks: List<String>,
    onPicked: (Wallpaper) -> Unit,
    onSpin: () -> Unit,
    onOpen: (Wallpaper) -> Unit,
    onClose: () -> Unit,
    nativeAd: NativeAd? = null
) {
    if (pool.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val cardHeight = (LocalConfiguration.current.screenHeightDp * 0.42f).dp.coerceIn(CARD_MIN_HEIGHT, CARD_MAX_HEIGHT)
    val cardWidth = cardHeight * CARD_ASPECT

    fun choosePick(): Wallpaper = pool.filter { it.id !in recentPicks }.ifEmpty { pool }.random()

    var spinKey by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(choosePick()) }
    var chosenSlot by remember { mutableIntStateOf(Random.nextInt(DECK_SIZE)) }

    val shuffle = remember { Animatable(0f) } // riffle: 0..1
    val fan = remember { Animatable(0f) }     // fan out: 0..1
    val lift = remember { Animatable(0f) }    // chosen card to centre, others away: 0..1
    val flip = remember { Animatable(0f) }    // 3D flip: 0..180 degrees
    val landScale = remember { Animatable(1f) }
    var confettiKey by remember { mutableIntStateOf(0) }

    // Back or face of the chosen card - flips only twice per shuffle, not every frame.
    val showFace by remember { derivedStateOf { flip.value >= 90f } }

    // A gentle float while the cards are fanned out.
    val bob by rememberInfiniteTransition(label = "deck").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    fun reveal() {
        if (revealed) return
        revealed = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        confettiKey++
        onPicked(pick)
        scope.launch {
            landScale.snapTo(0.92f)
            landScale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
        }
    }

    /** Skip straight to the revealed card. Snapping cancels the running sequence below. */
    fun skip() {
        scope.launch {
            shuffle.snapTo(1f)
            fan.snapTo(1f)
            lift.snapTo(1f)
            flip.snapTo(180f)
        }
        reveal()
    }

    LaunchedEffect(spinKey) {
        revealed = false
        shuffle.snapTo(0f)
        fan.snapTo(0f)
        lift.snapTo(0f)
        flip.snapTo(0f)
        // New pick only once the card is face-down again, so it never flashes early.
        if (spinKey > 0) {
            pick = choosePick()
            chosenSlot = Random.nextInt(DECK_SIZE)
        }
        onSpin()
        shuffle.animateTo(1f, tween(SHUFFLE_MS, easing = LinearEasing))
        fan.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(FAN_HOLD_MS)
        lift.animateTo(1f, tween(LIFT_MS, easing = FastOutSlowInEasing))
        flip.animateTo(180f, tween(FLIP_MS, easing = FastOutSlowInEasing))
        reveal()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                // Tap: skip the shuffle, or close once revealed.
                if (!revealed) skip() else onClose()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (revealed) "Surprise! ✨" else "Shuffling the deck…",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                if (revealed) "Here's your pick" else "Tap to skip",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // The deck. Cards move outside this box freely (no clip); the chosen one is drawn
            // last so it sits on top as it lifts.
            Box(modifier = Modifier.size(cardWidth, cardHeight)) {
                val drawOrder = (0 until DECK_SIZE).filter { it != chosenSlot } + chosenSlot
                drawOrder.forEach { i ->
                    key(i) {
                        val chosen = i == chosenSlot
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val w = cardWidth.toPx()
                                    val h = cardHeight.toPx()
                                    val slot = (i - MIDDLE_SLOT).toFloat()

                                    // Riffle: cards cross left/right, starting and ending stacked.
                                    val t = shuffle.value
                                    val envelope = sin(PI * t).toFloat()
                                    val phase = t * 4f * PI.toFloat() + i * 1.3f
                                    val side = if (i % 2 == 0) 1f else -1f
                                    var x = side * sin(phase) * w * 0.5f * envelope
                                    var y = cos(phase) * 10.dp.toPx() * envelope
                                    var rot = side * sin(phase) * 12f * envelope

                                    // Fan: an arc, floating gently.
                                    val f = fan.value
                                    x += slot * w * 0.42f * f
                                    y += slot * slot * 10.dp.toPx() * f - bob * 6.dp.toPx() * f
                                    rot += slot * 10f * f

                                    val l = lift.value
                                    var scale = DECK_SCALE
                                    if (chosen) {
                                        // Lift up in a small arc and fly to the centre, growing.
                                        x = mix(x, 0f, l)
                                        y = mix(y, 0f, l) - sin(PI * l).toFloat() * 40.dp.toPx()
                                        rot = mix(rot, 0f, l)
                                        scale = mix(DECK_SCALE, 1f, l) * landScale.value
                                        rotationY = flip.value
                                        cameraDistance = 14f * density
                                    } else {
                                        // The others slide away and fade.
                                        x += slot * w * 0.4f * l
                                        y += l * h * 0.5f
                                        alpha = 1f - l
                                    }
                                    translationX = x
                                    translationY = y
                                    rotationZ = rot
                                    scaleX = scale
                                    scaleY = scale
                                }
                        ) {
                            if (chosen && showFace) {
                                // Counter-rotate so the face isn't mirrored after the flip.
                                ThumbImage(
                                    url = pick.gridImageUrl,
                                    placeholderKey = pick.id,
                                    size = GRID_THUMBNAIL_SIZE,
                                    contentDescription = pick.title,
                                    memoryCacheKey = thumbMemoryKey(pick.imageUrl),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                        .clip(RoundedCornerShape(26.dp))
                                        .border(2.dp, Brush.linearGradient(GradientSignature), RoundedCornerShape(26.dp))
                                )
                            } else {
                                CardBack()
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        pick.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    if (pick.categoryName.isNotBlank()) {
                        Text(pick.categoryName, color = Accent3, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        OverlayButton("Shuffle again 🃏", primary = false) { spinKey++ }
                        OverlayButton("Open", primary = true) { onOpen(pick) }
                    }
                }
            }
        }

        if (confettiKey > 0) ConfettiBurst(key = confettiKey)

        // Close (✕) in the top-right corner, below the status bar. Available during the shuffle too.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Small native ad - policy-safe placement: only after the pick is revealed (never during
        // the shuffle, when users tap to skip), faded in after a short delay, at the bottom edge far
        // from the Open / Shuffle again / Close buttons. "Ad" label + AdChoices come with the card.
        if (nativeAd != null) {
            var showAd by remember { mutableStateOf(false) }
            LaunchedEffect(revealed) {
                showAd = false
                if (revealed) {
                    delay(AD_REVEAL_DELAY_MS)
                    showAd = true
                }
            }
            AnimatedVisibility(
                visible = showAd,
                enter = fadeIn(tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                NativeAdCard(nativeAd = nativeAd)
            }
        }
    }
}

@Composable
private fun OverlayButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (primary) Modifier.background(Brush.linearGradient(GradientSignature))
                else Modifier.background(Color.White.copy(alpha = 0.12f))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Face-down Walzi card: the signature gradient, an inset frame and the "W" mark. */
@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(GradientSignature))
            .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.12f))
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = 34.sp)
        }
    }
}

private data class Particle(val angle: Float, val speed: Float, val size: Float, val color: Color, val spin: Float)

private val CONFETTI_COLORS = listOf(Accent1, Accent2, Accent3, Color(0xFFFFC94D), Color.White)

/** A one-shot confetti burst from the centre, falling with gravity. Drawn on a Canvas. */
@Composable
private fun ConfettiBurst(key: Int) {
    val particles = remember(key) {
        List(60) {
            Particle(
                angle = Random.nextFloat() * 360f,
                speed = 0.35f + Random.nextFloat() * 0.65f,
                size = 6f + Random.nextFloat() * 8f,
                color = CONFETTI_COLORS.random(),
                spin = Random.nextFloat() * 720f - 360f
            )
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(1400, easing = LinearEasing)) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val origin = center
        val reach = size.minDimension * 0.55f
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val distance = reach * p.speed * (1f - (1f - t) * (1f - t)) // ease-out burst
            val gravity = size.height * 0.35f * t * t
            val x = origin.x + (cos(rad) * distance).toFloat()
            val y = origin.y + (sin(rad) * distance).toFloat() + gravity
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotateAround(Offset(x, y), p.spin * t) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2, y - p.size / 4),
                    size = Size(p.size, p.size / 2)
                )
            }
        }
    }
}

/** Small helper so each confetti piece spins around its own centre. */
private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.rotateAround(
    pivot: Offset,
    degrees: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    drawContext.transform.rotate(degrees, pivot)
    block()
    drawContext.transform.rotate(-degrees, pivot)
}
