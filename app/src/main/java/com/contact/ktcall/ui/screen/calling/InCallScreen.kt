package com.contact.ktcall.ui.screen.calling

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.ui.unit.sp
import com.contact.ktcall.R
import com.contact.ktcall.core.data.CallActionType
import com.contact.ktcall.core.data.CallsState
import com.contact.ktcall.core.data.CallData
import com.contact.ktcall.core.data.AudioRoute
import com.contact.ktcall.core.data.CallState
import com.contact.ktcall.core.viewmodel.CallUiState
import com.contact.ktcall.core.viewmodel.CallViewModelImpl
import com.contact.ktcall.di.CallModule.provideCallViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun InCallScreen(
    viewModel: CallViewModelImpl? = null,
    onCallEnded: (() -> Unit)? = null
) {
    val actualViewModel = viewModel ?: provideCallViewModel()
    val uiState by actualViewModel.uiState.collectAsState()

    // 当通话结束时自动退出
    if (uiState.callsState == CallsState.NO_CALL) {
        onCallEnded?.invoke()
    }

    // Back press during call should hang up
    BackHandler(enabled = uiState.callsState != CallsState.NO_CALL) {
        actualViewModel.onCallActionClick(CallActionType.HANGUP)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C3E50), Color(0xFF7B4F63))
                )
            )
    ) {
        when (uiState.callsState) {
            CallsState.INCOMING -> IncomingCallScreen(actualViewModel)
            CallsState.ACTIVE, CallsState.OUTGOING -> ActiveCallScreen(actualViewModel)
            CallsState.NO_CALL -> EndedCallScreen(actualViewModel)
            else -> NoCallScreen()
        }
    }
}

@Composable
private fun IncomingCallScreen(viewModel: CallViewModelImpl) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 56.dp)) {
            Text(
                "正在呼叫...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                uiState.call?.displayName ?: uiState.call?.number ?: "未知号码",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                uiState.call?.number ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // Bottom action row: accept / hangup (incoming style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Red).clickable(onClick = {
                    viewModel.onCallActionClick(CallActionType.DENY)
                }),
            ){
                Image(painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_hangup), contentDescription = "拒接")
            }


            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1EB980)).clickable(onClick = {
                    viewModel.onCallActionClick(CallActionType.DENY)
                }),
            ){
                Icon(painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_answer), contentDescription = "接听", tint = Color.White)            }
        }
    }
}

@Composable
private fun ActiveCallScreen(viewModel: CallViewModelImpl) {
    val uiState by viewModel.uiState.collectAsState()

    // Main content: name, status, and bottom action panel
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            when (uiState.callsState) {
                CallsState.OUTGOING -> "正在呼叫..."
                CallsState.ACTIVE -> "通话中 ${uiState.callDuration}"
                else -> "通话中 ${uiState.callDuration}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(8.dp))
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            uiState.call?.displayName ?: uiState.call?.number ?: "未知号码",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            uiState.call?.number ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        // spacer pushes bottom panel to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Bottom translucent action panel overlay
        BottomActionPanel(uiState.call?.number ?: "", viewModel, uiState)
    }
}

@Composable
private fun EndedCallScreen(viewModel: CallViewModelImpl) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            "通话已结束。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            uiState.call?.displayName ?: uiState.call?.number ?: "未知号码",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            uiState.call?.number ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotInterested, contentDescription = "拦截", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("拦截", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = "标记", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("标记", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAddAlt, contentDescription = "添加联系人", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("添加至联系人", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddChart, contentDescription = "添加标签", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("添加标签", color = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF2DBA6E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "回拨", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF3AA7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "短信", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF2DBA6E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "视频", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun BottomActionPanel(number: String, viewModel: CallViewModelImpl, uiState: CallUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Rounded translucent card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // First row of actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* record toggle */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "录音", tint = Color.White)
                        }
                        Text("录音", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* video */ }) {
                            Icon(Icons.Default.Videocam, contentDescription = "视频通话", tint = Color.White)
                        }
                        Text("视频通话", color = Color.White)
                    }

//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        IconButton(onClick = { viewModel.onCallActionClick(CallActionType.) }) {
//                            Icon(Icons.Default.Bluetooth, contentDescription = "蓝牙", tint = Color.White)
//                        }
//                        Text("蓝牙", color = Color.White)
//                    }
                }

                Spacer(Modifier.height(8.dp))

                // Second row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { viewModel.onCallActionClick(CallActionType.TOGGLE_SPEAKER) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "扬声器", tint = if (uiState.audioRoute.name == "SPEAKER") Color.Yellow else Color.White)
                        }
                        Text("扬声器", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { viewModel.onCallActionClick(CallActionType.TOGGLE_MUTE) }) {
                            Icon(Icons.Default.MicOff, contentDescription = "静音", tint = if (uiState.isMuted) Color.Yellow else Color.White)
                        }
                        Text("静音", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* dialpad */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "拨号盘", tint = Color.White)
                        }
                        Text("拨号盘", color = Color.White)
                    }
                }
            }
        }

        // Centered hangup button overlapping the card
        FloatingActionButton(
            onClick = { viewModel.onCallActionClick(CallActionType.HANGUP) },
            containerColor = Color.Red,
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        ) {
            Image(painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_hangup), contentDescription = "挂断")
        }
    }
}

@Composable
private fun NoCallScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C3E50)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "无通话",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

// Preview functions for different call states

@Preview(name = "来电预览", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun IncomingCallPreview() {
    val mockCall = CallData(
        id = "mock-incoming",
        state = CallState.RINGING,
        number = "13800138000",
        displayName = "张三"
    )
    val mockUiState = CallUiState(
        call = mockCall,
        callsState = CallsState.INCOMING,
        isMuted = false,
        audioRoute = AudioRoute.EARPIECE,
        callDuration = "00:00"
    )

    InCallScreenPreview(mockUiState)
}

@Preview(name = "呼出电话预览", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun OutgoingCallPreview() {
    val mockCall = CallData(
        id = "mock-outgoing",
        state = CallState.DIALING,
        number = "13900139000",
        displayName = "李四"
    )
    val mockUiState = CallUiState(
        call = mockCall,
        callsState = CallsState.OUTGOING,
        isMuted = false,
        audioRoute = AudioRoute.EARPIECE,
        callDuration = "00:00"
    )

    InCallScreenPreview(mockUiState)
}

@Preview(name = "通话中预览", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ActiveCallPreview() {
    val mockCall = CallData(
        id = "mock-active",
        state = CallState.ACTIVE,
        number = "13700137000",
        displayName = "王五"
    )
    val mockUiState = CallUiState(
        call = mockCall,
        callsState = CallsState.ACTIVE,
        isMuted = false,
        isOnHold = false,
        audioRoute = AudioRoute.SPEAKER,
        callDuration = "05:23",
        callStartTime = System.currentTimeMillis() - 5 * 60 * 1000 - 23 * 1000
    )

    InCallScreenPreview(mockUiState)
}

@Preview(name = "挂断电话预览", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun EndedCallPreview() {
    val mockCall = CallData(
        id = "mock-ended",
        state = CallState.DISCONNECTED,
        number = "13600136000",
        displayName = "赵六"
    )
    val mockUiState = CallUiState(
        call = mockCall,
        callsState = CallsState.NO_CALL,
        isMuted = false,
        audioRoute = AudioRoute.EARPIECE,
        callDuration = "00:00"
    )

    InCallScreenPreview(mockUiState)
}

@Composable
private fun InCallScreenPreview(uiState: CallUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C3E50), Color(0xFF7B4F63))
                )
            )
    ) {
        when (uiState.callsState) {
            CallsState.INCOMING -> IncomingCallScreenPreview(uiState)
            CallsState.OUTGOING, CallsState.ACTIVE -> ActiveCallScreenPreview(uiState)
            CallsState.NO_CALL -> EndedCallScreenPreview(uiState)
            else -> NoCallScreen()
        }
    }
}

@Composable
private fun IncomingCallScreenPreview(uiState: CallUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 56.dp)) {
            Text(
                "正在呼叫...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                uiState.call?.displayName ?: uiState.call?.number ?: "未知号码",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                uiState.call?.number ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // Bottom action row: accept / hangup (incoming style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.CallEnd, contentDescription = "拒接", tint = Color.White)
            }

            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1EB980)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Call, contentDescription = "接听", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ActiveCallScreenPreview(uiState: CallUiState) {
    // Main content: name, status, and bottom action panel
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            when (uiState.callsState) {
                CallsState.OUTGOING -> "正在呼叫..."
                CallsState.ACTIVE -> "通话中 ${uiState.callDuration}"
                else -> "通话中 ${uiState.callDuration}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(8.dp))
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            uiState.call?.displayName ?: uiState.call?.number ?: "未知号码",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            uiState.call?.number ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        // spacer pushes bottom panel to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Bottom translucent action panel overlay
//        BottomActionPanelPreview(uiState.call?.number ?: "", uiState)

        CallMenuPanel(modifier = Modifier.fillMaxSize(), onHangUp = {})
    }
}

@Composable
private fun EndedCallScreenPreview(uiState: CallUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            "通话已结束。",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            uiState.call?.displayName ?: uiState.call?.number ?: "未知号码",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            uiState.call?.number ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Post-call quick actions (block, mark, add contact, tag)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.Transparent
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotInterested, contentDescription = "拦截", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("拦截", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = "标记", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("标记", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAddAlt, contentDescription = "添加联系人", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("添加至联系人", color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddChart, contentDescription = "添加标签", tint = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("添加标签", color = Color.White)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF2DBA6E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "回拨", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF3AA7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "短信", tint = Color.White)
                    }

                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF2DBA6E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "视频", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun BottomActionPanelPreview(number: String, uiState: CallUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Rounded translucent card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // First row of actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* record toggle */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "录音", tint = Color.White)
                        }
                        Text("录音", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* video */ }) {
                            Icon(Icons.Default.Videocam, contentDescription = "视频通话", tint = Color.White)
                        }
                        Text("视频通话", color = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Second row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* speaker */ }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "扬声器", tint = if (uiState.audioRoute.name == "SPEAKER") Color.Yellow else Color.White)
                        }
                        Text("扬声器", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* mute */ }) {
                            Icon(Icons.Default.MicOff, contentDescription = "静音", tint = if (uiState.isMuted) Color.Yellow else Color.White)
                        }
                        Text("静音", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { /* dialpad */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "拨号盘", tint = Color.White)
                        }
                        Text("拨号盘", color = Color.White)
                    }
                }
            }
        }

        // Centered hangup button overlapping the card
        FloatingActionButton(
            onClick = { /* hangup */ },
            containerColor = Color.Red,
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        ) {
            Image(painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_hangup), contentDescription = "挂断")
        }
    }
}

@Composable
fun CallMenuPanel(
    modifier: Modifier = Modifier,
    onHangUp: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xCCFFFFFF),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            MenuRow(
                MenuItem(Icons.Default.Videocam, "录制"),
                MenuItem(Icons.Default.Videocam, "视频通话"),
                MenuItem(Icons.Default.Bluetooth, "蓝牙"),
            )

            Spacer(modifier = Modifier.height(20.dp))

            MenuRow(
                MenuItem(Icons.Default.VolumeUp, "扬声器"),
                MenuItem(Icons.Default.MicOff, "静音"),
                MenuItem(Icons.Default.Dialpad, "拨号盘"),
            )

            Spacer(modifier = Modifier.height(28.dp))

            HangUpButton(onHangUp)
        }
    }
}

@Composable
private fun MenuRow(vararg items: MenuItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach {
            CallMenuItem(it)
        }
    }
}

data class MenuItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@Composable
private fun CallMenuItem(item: MenuItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { }
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = Color(0xFF222222),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            fontSize = 12.sp,
            color = Color(0xFF222222)
        )
    }
}

@Composable
private fun HangUpButton(onHangUp: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFD93025), CircleShape)
            .clickable { onHangUp() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = "挂断",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}