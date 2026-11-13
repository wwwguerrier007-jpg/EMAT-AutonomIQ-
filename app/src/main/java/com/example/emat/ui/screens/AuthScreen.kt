package com.example.emat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.emat.domain.service.BiometricCapability
import com.example.emat.ui.components.StatusBadge
import com.example.emat.ui.theme.ElectricBlue
import com.example.emat.ui.theme.ElectricBlueBorder
import com.example.emat.ui.theme.ElectricBlueBright
import com.example.emat.ui.theme.ElectricBlueVibrant
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatShortRedBorder
import com.example.emat.ui.theme.EmatShortRedDim
import com.example.emat.ui.theme.EmatSurfaceActive
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.SleekCharcoalSurface
import com.example.emat.ui.theme.SleekTextMuted
import com.example.emat.domain.model.AccountCategory
import com.example.emat.domain.model.EmatAccount
import com.example.emat.ui.theme.SleekTextOffset
import com.example.emat.ui.theme.SleekTextPrimary
import com.example.emat.ui.theme.SleekTextSubtle
import com.example.emat.ui.theme.SleekTextWhite

@Composable
fun AuthScreen(
    passwordInput: String,
    errorMessage: String?,
    biometricCapability: BiometricCapability,
    faceSupported: Boolean,
    onPasswordChanged: (String) -> Unit,
    onLoginPassword: () -> Unit,
    onLoginBiometric: () -> Unit,
    selectedAccount: EmatAccount = EmatAccount(
        id = "EMAT-INST-001",
        name = "EMAT Master Institutional",
        role = "Chief Strategist / Lead Operator",
        tier = "Tier 1 Master Institutional",
        dailyLossLimit = "2.0% ($20,000)",
        maxPositionSize = "1.00 Lot",
        isLiveExecutionAllowed = true,
        permissions = listOf("LIVE_EXECUTION", "RISK_CALIBRATION", "FULL_SCANNER", "AUDIT_EXPORT"),
        category = AccountCategory.INSTITUTIONAL
    ),
    availableAccounts: List<EmatAccount> = emptyList(),
    onAccountSelected: (EmatAccount) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("auth_screen")
    ) {
        // Top Header: Institutional Identity & Logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // EMAT Logo Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmatSurfaceElevated)
                    .border(1.5.dp, ElectricBlueBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_emat_logo),
                    contentDescription = "EMAT AutonomIQ Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "EMAT AutonomIQ",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = SleekTextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SECURE ACCESS",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = ElectricBlueBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // Centralized Area: Operator Authentication card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 80.dp, bottom = 64.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, EmatBorderSubtle, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Operator Authentication",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = SleekTextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Enter your credentials key to unlock verified scan and risk engines.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 18.dp),
                        textAlign = TextAlign.Center,
                        color = SleekTextSubtle,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Error Message Banner
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            color = EmatShortRedDim,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, EmatShortRedBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = EmatShortRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = SleekTextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Account Context Selector (Individual on left, Institutional on right)
                    if (availableAccounts.isNotEmpty()) {
                        Text(
                            text = "ACCOUNT SELECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextSubtle,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val sortedAccounts = availableAccounts.sortedBy { account ->
                            if (account.category == AccountCategory.INDIVIDUAL) 0 else 1
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmatSurfaceElevated)
                                .border(1.dp, EmatBorderSubtle, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            sortedAccounts.forEach { account ->
                                val isSelected = account.id == selectedAccount.id
                                val isInst = account.category == AccountCategory.INSTITUTIONAL
                                val label = if (isInst) "INSTITUTIONAL" else "INDIVIDUAL"

                                Surface(
                                    onClick = { onAccountSelected(account) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("account_selector_${account.id.lowercase()}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ElectricBlue else Color.Transparent,
                                    border = if (isSelected) null else BorderStroke(1.dp, Color.Transparent)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else SleekTextMuted,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = onPasswordChanged,
                        label = {
                            Text(
                                text = "Operator Password / Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onLoginPassword() }),
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.testTag("toggle_password_visibility")
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = SleekTextSubtle
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ElectricBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = EmatBorderSubtle,
                            focusedTextColor = SleekTextWhite,
                            unfocusedTextColor = SleekTextWhite,
                            focusedContainerColor = EmatSurfaceActive,
                            unfocusedContainerColor = EmatSurfaceActive,
                            focusedLabelColor = ElectricBlueBright,
                            unfocusedLabelColor = SleekTextSubtle,
                            cursorColor = ElectricBlueBright
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Password Login Button
                    Button(
                        onClick = onLoginPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTHENTICATE OPERATOR",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Biometrics Section Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(EmatBorderSubtle)
                        )
                        Text(
                            text = "HARDWARE BIOMETRICS",
                            color = SleekTextSubtle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(EmatBorderSubtle)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Biometric Button
                    val isBioAvailable = biometricCapability is BiometricCapability.SupportedAvailable
                    OutlinedButton(
                        onClick = onLoginBiometric,
                        enabled = isBioAvailable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_login_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = EmatSurfaceElevated,
                            contentColor = if (isBioAvailable) SleekTextWhite else SleekTextMuted
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isBioAvailable) ElectricBlueBorder else EmatBorderSubtle
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric",
                            modifier = Modifier.size(20.dp),
                            tint = if (isBioAvailable) ElectricBlueBright else SleekTextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (biometricCapability) {
                                is BiometricCapability.SupportedAvailable -> "BIOMETRIC FINGERPRINT"
                                is BiometricCapability.NoneEnrolled -> "BIOMETRICS: NOT ENROLLED"
                                is BiometricCapability.NoHardware -> "BIOMETRICS: NO HARDWARE"
                                else -> "BIOMETRICS: UNAVAILABLE"
                            },
                            color = if (isBioAvailable) SleekTextWhite else SleekTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Face Authentication Hardware Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Face Unlock",
                                tint = ElectricBlueBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Face Authentication",
                                color = SleekTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        StatusBadge(status = if (faceSupported) "SUPPORTED" else "NOT DETECTED")
                    }
                }
            }
        }

        // Bottom Footer: Immutable security journal notice
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            color = SleekCharcoalSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EmatBorderHairline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Authentication attempts are logged.",
                    color = SleekTextOffset,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

