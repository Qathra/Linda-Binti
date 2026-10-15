package com.example.lindabinti.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lindabinti.R
import com.example.lindabinti.data.repository.AccessPinResult
import com.example.lindabinti.domain.CalculatorEngine
import com.example.lindabinti.domain.CalculatorSecretGate
import com.example.lindabinti.domain.SecretAction
import com.example.lindabinti.ui.theme.CalcBackground
import com.example.lindabinti.ui.theme.CalcKeyDark
import com.example.lindabinti.ui.theme.CalcKeyLight
import com.example.lindabinti.ui.theme.CalcKeyOrange
import com.example.lindabinti.ui.theme.CalcOnKey
import com.example.lindabinti.ui.theme.CalcOnLightKey

enum class CalculatorAction {
    Digit, Decimal, Clear, Delete, Percent,
    Add, Subtract, Multiply, Divide, Equals
}

@Composable
fun CalculatorScreen(
    setupComplete: Boolean,
    onRequestSetup: () -> Unit,
    onAccessGranted: () -> Unit,
    onSosTriggered: () -> Unit,
    attemptAccessPin: (String) -> AccessPinResult,
    verifySosCode: (String) -> Boolean
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val engine = remember { CalculatorEngine() }
    var display by remember { mutableStateOf(engine.currentDisplay()) }
    var lockHint by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        display = engine.currentDisplay()
    }

    fun onKey(action: CalculatorAction, digit: Char? = null) {
        lockHint = null
        when (action) {
            CalculatorAction.Digit -> digit?.let { engine.inputDigit(it) }
            CalculatorAction.Decimal -> engine.inputDecimal()
            CalculatorAction.Clear -> engine.clear()
            CalculatorAction.Delete -> engine.deleteLast()
            CalculatorAction.Percent -> engine.percent()
            CalculatorAction.Add -> engine.setOperation('+')
            CalculatorAction.Subtract -> engine.setOperation('−')
            CalculatorAction.Multiply -> engine.setOperation('×')
            CalculatorAction.Divide -> engine.setOperation('÷')
            CalculatorAction.Equals -> {
                // LOCKED DUAL-CODE BEHAVIOR — do not reorder or weaken:
                // 1) SOS code → SMS alert then protected Home
                // 2) Access Code (4–6) → Home only, no SMS
                val candidate = engine.digitsCandidate()
                when (
                    val action = CalculatorSecretGate.resolve(
                        candidate = candidate,
                        setupComplete = setupComplete,
                        verifySosCode = verifySosCode,
                        attemptAccessPin = attemptAccessPin
                    )
                ) {
                    SecretAction.TriggerSos -> {
                        engine.clear()
                        refresh()
                        onSosTriggered()
                        return
                    }
                    SecretAction.OpenAccess -> {
                        engine.clear()
                        refresh()
                        onAccessGranted()
                        return
                    }
                    is SecretAction.Locked -> {
                        lockHint = context.getString(
                            R.string.calc_locked,
                            action.remainingSeconds.toInt()
                        )
                        engine.clear()
                        refresh()
                        return
                    }
                    SecretAction.CalculatorEquals -> {
                        engine.equals()
                    }
                }
            }
        }
        refresh()
    }

    Column(
        modifier = mod
            .fillMaxSize()
            .background(CalcBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Calculator",
            color = CalcOnKey.copy(alpha = 0.55f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = mod
                .padding(start = 10.dp, top = 4.dp, bottom = 8.dp)
                .pointerInput(setupComplete) {
                    detectTapGestures(
                        onLongPress = {
                            if (!setupComplete) onRequestSetup()
                        }
                    )
                }
        )

        Box(
            modifier = mod
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = mod.fillMaxWidth()) {
                lockHint?.let {
                    Text(
                        it,
                        color = CalcKeyOrange,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = mod.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = display,
                    color = CalcOnKey,
                    fontSize = when {
                        display.length > 10 -> 36.sp
                        display.length > 7 -> 48.sp
                        else -> 64.sp
                    },
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = mod.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = mod.height(16.dp))

        val rows = listOf(
            listOf(
                KeySpec("C", CalculatorAction.Clear, null, func = true),
                KeySpec("⌫", CalculatorAction.Delete, null, func = true),
                KeySpec("%", CalculatorAction.Percent, null, func = true),
                KeySpec("÷", CalculatorAction.Divide, null, op = true)
            ),
            listOf(
                KeySpec("7", CalculatorAction.Digit, '7'),
                KeySpec("8", CalculatorAction.Digit, '8'),
                KeySpec("9", CalculatorAction.Digit, '9'),
                KeySpec("×", CalculatorAction.Multiply, null, op = true)
            ),
            listOf(
                KeySpec("4", CalculatorAction.Digit, '4'),
                KeySpec("5", CalculatorAction.Digit, '5'),
                KeySpec("6", CalculatorAction.Digit, '6'),
                KeySpec("−", CalculatorAction.Subtract, null, op = true)
            ),
            listOf(
                KeySpec("1", CalculatorAction.Digit, '1'),
                KeySpec("2", CalculatorAction.Digit, '2'),
                KeySpec("3", CalculatorAction.Digit, '3'),
                KeySpec("+", CalculatorAction.Add, null, op = true)
            ),
            listOf(
                KeySpec("0", CalculatorAction.Digit, '0', wide = true),
                KeySpec(".", CalculatorAction.Decimal, null),
                KeySpec("=", CalculatorAction.Equals, null, op = true)
            )
        )

        rows.forEach { row ->
            Row(
                modifier = mod.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    val bg = when {
                        key.op -> CalcKeyOrange
                        key.func -> CalcKeyLight
                        else -> CalcKeyDark
                    }
                    val fg = when {
                        key.func -> CalcOnLightKey
                        else -> CalcOnKey
                    }
                    CalcKey(
                        label = key.label,
                        background = bg,
                        foreground = fg,
                        wide = key.wide,
                        modifier = mod.weight(if (key.wide) 2f else 1f),
                        onClick = { onKey(key.action, key.digit) }
                    )
                }
            }
        }
        Spacer(modifier = mod.height(8.dp))
    }
}

private data class KeySpec(
    val label: String,
    val action: CalculatorAction,
    val digit: Char?,
    val wide: Boolean = false,
    val op: Boolean = false,
    val func: Boolean = false
)

@Composable
private fun CalcKey(
    label: String,
    background: Color,
    foreground: Color,
    wide: Boolean,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(if (wide) 2.12f else 1f)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = CalcOnKey),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = foreground,
            fontSize = if (label == "⌫") 26.sp else 30.sp,
            fontWeight = FontWeight.Normal
        )
    }
}