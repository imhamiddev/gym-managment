package dev.imhamid.gymyar.ui.coach

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.data.CheckinInfo
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.Qr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrScreen(vm: CoachViewModel) {
    val g = MaterialTheme.gym
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<CheckinInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var stale by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf("bw") }
    var confirm by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var poster by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(reload) {
        when (val r = Repo.checkinInfo()) {
            is Res.Ok -> { info = r.value; stale = r.stale; error = null }
            is Res.Fail -> error = r.message
        }
    }
    LaunchedEffect(info, color) {
        val i = info ?: return@LaunchedEffect
        val payload = Qr.payload(i)
        withContext(Dispatchers.Default) {
            poster = Qr.poster(context, i.gymName, payload, color == "color")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (stale) Banner("حالت آفلاین: آخرین QR ذخیره‌شده نمایش داده می‌شود.", g.warning)
        ErrorText(error)
        val i = info
        if (i == null) {
            if (error == null) SkeletonBox(Modifier.size(280.dp), 280.dp)
        } else {
            PageHeader("QR ثبت حضور", i.gymName, Icons.Rounded.QrCode2, g.attendance)
            AppCard {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Live preview of exactly what gets printed / shared.
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1240f / 1754f)
                            .clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .border(1.dp, g.border, RoundedCornerShape(12.dp)),
                    ) {
                        AnimatedContent(targetState = poster, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "poster") { bmp ->
                            if (bmp != null) {
                                Image(bmp.asImageBitmap(), contentDescription = "پوستر QR حضور", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            } else {
                                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
                            }
                        }
                    }
                    Text(
                        "این QR را چاپ و در باشگاه نصب کنید. ورزشکارها با اپ باشگاه‌یار آن را اسکن می‌کنند تا حضورشان ثبت شود (از نیم‌ساعت قبل تا نیم‌ساعت بعد از ساعت کاری).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                }
            }
            Text("نسخه‌ی چاپ", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            PillTabs(listOf("bw" to "سیاه و سفید", "color" to "رنگی"), color, { color = it }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton("چاپ QR", modifier = Modifier.weight(1f), icon = Icons.Rounded.Print, enabled = poster != null, onClick = {
                    poster?.let { Qr.print(context, it, color == "color") }
                })
                SecondaryButton("اشتراک‌گذاری", modifier = Modifier.weight(1f), icon = Icons.Rounded.Share, enabled = poster != null, onClick = {
                    poster?.let { Qr.share(context, it) }
                })
            }
            if (i.isOwner) {
                SecondaryButton("تعویض QR (قبلی از کار می‌افتد)", onClick = { confirm = true }, icon = Icons.Rounded.Autorenew, tone = g.danger, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = "تعویض QR",
            text = "QR جدید ساخته می‌شود و QR چاپ‌شده‌ی فعلی از کار می‌افتد. باید دوباره چاپش کنید. ادامه می‌دهید؟",
            confirmLabel = "بله، تعویض شود",
            onConfirm = {
                confirm = false
                scope.launch {
                    val err = vm.act("regenerate_checkin_token")
                    if (err != null) UiEvents.toast(err) else { UiEvents.toast("QR جدید ساخته شد"); reload++ }
                }
            },
            onDismiss = { confirm = false },
        )
    }
}
