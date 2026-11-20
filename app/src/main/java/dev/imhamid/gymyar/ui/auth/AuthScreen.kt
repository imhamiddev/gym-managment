package dev.imhamid.gymyar.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.imhamid.gymyar.R
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.ui.components.AppField
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.ui.components.ErrorText
import dev.imhamid.gymyar.ui.components.PrimaryButton
import dev.imhamid.gymyar.util.en
import kotlinx.coroutines.launch

private val PHONE_RE = Regex("^09\\d{9}$")

@Composable
fun BrandMark(size: Int = 64) {
    Box(
        modifier = Modifier.size(size.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.gym.lime),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = MaterialTheme.gym.onLime,
            modifier = Modifier.size((size * 1.25f).dp),
        )
    }
}

@Composable
private fun RoleChoice(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = MaterialTheme.gym
    val shape = RoundedCornerShape(50)
    val fg = if (selected) g.onLime else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) g.lime else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) g.lime else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, color = if (selected) g.onLime else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AuthScreen() {
    var register by rememberSaveable { mutableStateOf(false) }
    var role by rememberSaveable { mutableStateOf("ATHLETE") }
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (loading) return
        val p = phone.en().replace(Regex("[\\s-]"), "")
        val f = first.trim().replace(Regex("\\s+"), " ")
        val l = last.trim().replace(Regex("\\s+"), " ")
        error = when {
            register && (f.isEmpty() || l.isEmpty()) -> "نام و نام خانوادگی را وارد کنید."
            register && (f.length > 40 || l.length > 60) -> "نام یا نام خانوادگی بیش از حد طولانی است."
            !PHONE_RE.matches(p) -> "شماره تلفن باید مثل ۰۹۱۲۱۲۳۴۵۶۷ باشد."
            register && password.length < 6 -> "رمز عبور باید حداقل ۶ کاراکتر باشد."
            password.isEmpty() -> "رمز عبور را وارد کنید."
            else -> null
        }
        if (error != null) return
        loading = true
        scope.launch {
            val err = if (register) Api.signUp(f, l, p, password, role) else Api.signIn(p, password)
            // On success the session flow switches the screen; only a failure stays here.
            if (err != null) {
                error = err
                loading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            BrandMark()
            Text("باشگاه‌یار", style = MaterialTheme.typography.headlineSmall)
            Text(
                "شهریه، اعضا و حضور و غیاب، همه در یک اپ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (register) "ساخت حساب جدید" else "خوش برگشتید",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (register) "حساب خود را بسازید و نقش‌تان را انتخاب کنید." else "با شماره تلفن و رمز عبور وارد شوید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            if (register) {
                Text("نقش شما", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    RoleChoice("ورزشکار", Icons.Rounded.Person, role == "ATHLETE", { role = "ATHLETE" }, Modifier.weight(1f))
                    RoleChoice("مربی", Icons.Rounded.Star, role == "COACH", { role = "COACH" }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppField(first, { first = it }, "نام", Modifier.weight(1f))
                    AppField(last, { last = it }, "نام خانوادگی", Modifier.weight(1f))
                }
            }

            AppField(
                phone, { phone = it }, "شماره تلفن",
                keyboardType = KeyboardType.Phone, ltr = true, placeholder = "09121234567",
                leading = { Icon(Icons.Rounded.Phone, contentDescription = null) },
            )
            AppField(
                password, { password = it }, "رمز عبور",
                keyboardType = KeyboardType.Password, ltr = true, password = !show,
                imeAction = ImeAction.Done, onDone = { submit() },
                leading = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                trailing = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = if (show) "پنهان کردن رمز" else "نمایش رمز")
                    }
                },
            )
            ErrorText(error)
            PrimaryButton(if (register) "ساخت حساب" else "ورود", onClick = { submit() }, loading = loading)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (register) "حساب دارید؟" else "حساب ندارید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { register = !register; error = null }) {
                    Text(if (register) "وارد شوید" else "ثبت‌نام کنید", color = MaterialTheme.gym.accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}
