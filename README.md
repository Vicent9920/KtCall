# Koler 通话管理架构流程

## 1. 核心类关系图
```puml
classDiagram
class InCallService {
<<Android Framework>>
}

    class CallService {
        -_calls: MutableStateFlow
        +onCallAdded(call)
        +onCallRemoved(call)
        +onCallAudioStateChanged()
    }
    
    class CallRepositoryImpl {
        -callService: CallService
        +calls: StateFlow
        +mainCall: StateFlow
        +answerMainCall()
        +hangup(call)
        +toggleMute()
    }
    
    class CallViewModelImpl {
        -callRepository: CallRepository
        +uiState: StateFlow
        +onCallActionClick(action)
    }
    
    InCallService <|-- CallService : Inherits
    CallService <-- CallRepositoryImpl : Observes & Controls
    CallRepositoryImpl <-- CallViewModelImpl : Observes & Delegates
```



## 2. 场景一：来电流程 (System -> UI)

 当手机收到来电时，数据是如何流向 UI 的：

- Android System: 检测到入呼叫，绑定 CallService。

- CallService:

    - 触发 onCallAdded(call)。

    - 注册 call.registerCallback(...) 监听状态。

    - 更新 _calls StateFlow。

- CallRepositoryImpl:

    - 观察到 calls 变化。

    - filterPrimaryCall() 识别出当前有一个状态为 STATE_RINGING 的通话。

    - getCallsState() 将总体状态判定为 INCOMING。

    - 更新 mainCall 和 callsState。

- CallViewModelImpl:

    - 观察到 Repository 的数据变化。

    - 更新 UiState，将 call 包含的数据暴露给 UI。

- CallActivity / CallView:

    - Compose UI 重组。

    - 检测到 callsState 为 INCOMING，显示来电接听界面。

## 3. 场景二：用户挂断电话 (UI -> System)

- 当用户点击红色的挂断按钮时：

    - CallView (UI):

        用户点击挂断按钮。

        调用 viewModel.onCallActionClick(CallActionType.DENY)。

    - CallViewModelImpl:

    - 接收到 DENY 动作。

    - 获取当前的主通话对象 (uiState.value.call)。

    - 调用 call.decline() (这是 CallData 的扩展方法) 或者调用 repository.hangup(call)。

    - CallRepositoryImpl:

    - hangup(call) 方法被执行。

- 判断逻辑:

    - 如果是 Android 10+ (API 29) 且是拒接，调用 call.reject(Call.REJECT_REASON_DECLINED)。

    - 否则调用 call.disconnect()。

    - Android System:

    - 底层执行挂断操作。

    - 硬件挂断成功后，回调 CallService.onCallRemoved(call)。

    - 闭环:

    - Service 更新列表 -> Repository 更新为空 -> ViewModel 更新 UI -> Activity 关闭。

## 4. 特殊功能：音频路由 (扬声器/蓝牙)

- Koler 如何处理扬声器切换：

    - 监听: CallService 在 onCallAudioStateChanged 中获取系统音频状态。

    - 转换: CallRepositoryImpl 将系统的 CallAudioState 转换为应用内的枚举 AudioRoute (SPEAKER, BLUETOOTH, WIRED, EARPIECE)。

    - 控制: 当用户点击“扬声器”按钮：

    - ViewModel 调用 callRepository.toggleSpeaker(true)。

    - Repository 调用 callService?.setAudioRoute(AudioRoute.SPEAKER.route)。

    - Service 调用系统的 setAudioRoute(route)。