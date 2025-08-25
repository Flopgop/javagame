package org.renderdoc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("UnusedReturnValue")
public class RenderDoc implements AutoCloseable {

    @Override
    public void close() {
        removeHooks();
    }

    // API version changelog:
    //
    // 1.0.0 - initial release
    // 1.0.1 - Bugfix: IsFrameCapturing() was returning false for captures that were triggered
    //         by keypress or TriggerCapture, instead of Start/EndFrameCapture.
    // 1.0.2 - Refactor: Renamed eRENDERDOC_Option_DebugDeviceMode to eRENDERDOC_Option_APIValidation
    // 1.1.0 - Add feature: TriggerMultiFrameCapture(). Backwards compatible with 1.0.x since the new
    //         function pointer is added to the end of the struct, the original layout is identical
    // 1.1.1 - Refactor: Renamed remote access to target control (to better disambiguate from remote
    //         replay/remote server concept in replay UI)
    // 1.1.2 - Refactor: Renamed "log file" in function names to just capture, to clarify that these
    //         are captures and not debug logging files. This is the first API version in the v1.0
    //         branch.
    // 1.2.0 - Added feature: SetCaptureFileComments() to add comments to a capture file that will be
    //         displayed in the UI program on load.
    // 1.3.0 - Added feature: New capture option eRENDERDOC_Option_AllowUnsupportedVendorExtensions
    //         which allows users to opt-in to allowing unsupported vendor extensions to function.
    //         Should be used at the user's own risk.
    //         Refactor: Renamed eRENDERDOC_Option_VerifyMapWrites to
    //         eRENDERDOC_Option_VerifyBufferAccess, which now also controls initialisation to
    //         0xdddddddd of uninitialised buffer contents.
    // 1.4.0 - Added feature: DiscardFrameCapture() to discard a frame capture in progress and stop
    //         capturing without saving anything to disk.
    // 1.4.1 - Refactor: Renamed Shutdown to RemoveHooks to better clarify what is happening
    // 1.4.2 - Refactor: Renamed 'draws' to 'actions' in callstack capture option.
    // 1.5.0 - Added feature: ShowReplayUI() to request that the replay UI show itself if connected
    // 1.6.0 - Added feature: SetCaptureTitle() which can be used to set a title for a
    //         capture made with StartFrameCapture() or EndFrameCapture()
    public enum KnownVersion {
        API_1_0_0(10000),
        API_1_0_1(10001),
        API_1_0_2(10002),
        API_1_1_0(10100),
        API_1_1_1(10101),
        API_1_1_2(10102),
        API_1_2_0(10200),
        API_1_3_0(10300),
        API_1_4_0(10400),
        API_1_4_1(10401),
        API_1_4_2(10402),
        API_1_5_0(10500),
        API_1_6_0(10600),
        ;
        final int value;
        KnownVersion(int value) {
            this.value = value;
        }
    }

    public enum CaptureOption {
        // Allow the application to enable vsync
        //
        // Default - enabled
        //
        // 1 - The application can enable or disable vsync at will
        // 0 - vsync is force disabled
        ALLOW_VSYNC(0),

        // Allow the application to enable fullscreen
        //
        // Default - enabled
        //
        // 1 - The application can enable or disable fullscreen at will
        // 0 - fullscreen is force disabled
        ALLOW_FULLSCREEN(1),

        // Record API debugging events and messages
        //
        // Default - disabled
        //
        // 1 - Enable built-in API debugging features and records the results into
        //     the capture, which is matched up with events on replay
        // 0 - no API debugging is forcibly enabled
        API_VALIDATION(2),
        @Deprecated DEBUG_DEVICE_MODE(2),

        // Capture CPU callstacks for API events
        //
        // Default - disabled
        //
        // 1 - Enables capturing of callstacks
        // 0 - no callstacks are captured
        CAPTURE_CALLSTACKS(3),

        // When capturing CPU callstacks, only capture them from actions.
        // This option does nothing without the above option being enabled
        //
        // Default - disabled
        //
        // 1 - Only captures callstacks for actions.
        //     Ignored if CaptureCallstacks is disabled
        // 0 - Callstacks, if enabled, are captured for every event.
        CAPTURE_CALLSTACKS_ONLY_DRAWS(4),
        CAPTURE_CALLSTACKS_ONLY_ACTIONS(4),

        // Specify a delay in seconds to wait for a debugger to attach, after
        // creating or injecting into a process, before continuing to allow it to run.
        //
        // 0 indicates no delay, and the process will run immediately after injection
        //
        // Default - 0 seconds
        //
        DELAY_FOR_DEBUGGER(5),

        // Verify buffer access. This includes checking the memory returned by a Map() call to
        // detect any out-of-bounds modification, as well as initialising buffers with undefined contents
        // to a marker value to catch use of uninitialised memory.
        //
        // NOTE: This option is only valid for OpenGL and D3D11. Explicit APIs such as D3D12 and Vulkan do
        // not do the same kind of interception & checking and undefined contents are really undefined.
        //
        // Default - disabled
        //
        // 1 - Verify buffer access
        // 0 - No verification is performed, and overwriting bounds may cause crashes or corruption in
        //     RenderDoc.
        VERIFY_BUFFER_ACCESS(6),
        // The old name for eRENDERDOC_Option_VerifyBufferAccess was eRENDERDOC_Option_VerifyMapWrites.
        // This option now controls the filling of uninitialised buffers with 0xdddddddd which was
        // previously always enabled
        @Deprecated VERIFY_MAP_WRITES(6),

        // Hooks any system API calls that create child processes, and injects
        // RenderDoc into them recursively with the same options.
        //
        // Default - disabled
        //
        // 1 - Hooks into spawned child processes
        // 0 - Child processes are not hooked by RenderDoc
        HOOK_INTO_CHILDREN(7),

        // By default, RenderDoc only includes resources in the final capture necessary
        // for that frame, this allows you to override that behaviour.
        //
        // Default - disabled
        //
        // 1 - all live resources at the time of capture are included in the capture
        //     and available for inspection
        // 0 - only the resources referenced by the captured frame are included
        REF_ALL_RESOURCES(8),

        // **NOTE**: As of RenderDoc v1.1 this option has been deprecated. Setting or
        // getting it will be ignored, to allow compatibility with older versions.
        // In v1.1 the option acts as if it's always enabled.
        //
        // By default, RenderDoc skips saving initial states for resources where the
        // previous contents don't appear to be used, assuming that writes before
        // reads indicate previous contents aren't used.
        //
        // Default - disabled
        //
        // 1 - initial contents at the start of each captured frame are saved, even if
        //     they are later overwritten or cleared before being used.
        // 0 - unless a read is detected, initial contents will not be saved and will
        //     appear as black or empty data.
        @Deprecated SAVE_ALL_INITIALS(9),

        // In APIs that allow for the recording of command lists to be replayed later,
        // RenderDoc may choose to not capture command lists before a frame capture is
        // triggered, to reduce overheads. This means any command lists recorded once
        // and replayed many times will not be available and may cause a failure to
        // capture.
        //
        // NOTE: This is only true for APIs where multithreading is difficult or
        // discouraged. Newer APIs like Vulkan and D3D12 will ignore this option
        // and always capture all command lists since the API is heavily oriented
        // around it and the overheads have been reduced by API design.
        //
        // 1 - All command lists are captured from the start of the application
        // 0 - Command lists are only captured if their recording begins during
        //     the period when a frame capture is in progress.
        CAPTURE_ALL_CMD_LISTS(10),

        // Mute API debugging output when the API validation mode option is enabled
        //
        // Default - enabled
        //
        // 1 - Mute any API debug messages from being displayed or passed through
        // 0 - API debugging is displayed as normal
        DEBUG_OUTPUT_MUTE(11),

        // Option to allow vendor extensions to be used even when they may be
        // incompatible with RenderDoc and cause corrupted replays or crashes.
        //
        // Default - inactive
        //
        // No values are documented, this option should only be used when absolutely
        // necessary as directed by a RenderDoc developer.
        ALLOW_UNSUPPORTED_VENDOR_EXTENSIONS(12),

        // Define a soft memory limit which some APIs may aim to keep overhead under where
        // possible. Anything above this limit will where possible be saved directly to disk during
        // capture.
        // This will cause increased disk space use (which may cause a capture to fail if disk space is
        // exhausted) as well as slower capture times.
        //
        // Not all memory allocations may be deferred like this so it is not a guarantee of a memory
        // limit.
        //
        // Units are in MBs, suggested values would range from 200MB to 1000MB.
        //
        // Default - 0 Megabytes
        SOFT_MEMORY_LIMIT(13)

        ;

        final int value;
        CaptureOption(int value) {
            this.value = value;
        }
    }

    public enum OverlayBits {
        ENABLED(0x1),
        FRAMERATE(0x2),
        FRAME_NUMBER(0x4),
        CAPTURE_LIST(0x8),

        DEFAULT(ENABLED.value | FRAMERATE.value | FRAME_NUMBER.value | CAPTURE_LIST.value),
        ALL(0x7ffffff),
        NONE(0)
        ;

        final int value;
        OverlayBits(int value) {
            this.value = value;
        }

        public int bits() {
            return value;
        }
    }

    public enum InputButton {
        KEY_0(0x30),
        KEY_1(0x31),
        KEY_2(0x32),
        KEY_3(0x33),
        KEY_4(0x34),
        KEY_5(0x35),
        KEY_6(0x36),
        KEY_7(0x37),
        KEY_8(0x38),
        KEY_9(0x39),

        KEY_A(0x41),
        KEY_B(0x42),
        KEY_C(0x43),
        KEY_D(0x44),
        KEY_E(0x45),
        KEY_F(0x46),
        KEY_G(0x47),
        KEY_H(0x48),
        KEY_I(0x49),
        KEY_J(0x4a),
        KEY_K(0x4b),
        KEY_L(0x4c),
        KEY_M(0x4d),
        KEY_N(0x4e),
        KEY_O(0x4f),
        KEY_P(0x50),
        KEY_Q(0x51),
        KEY_R(0x52),
        KEY_S(0x53),
        KEY_T(0x54),
        KEY_U(0x55),
        KEY_V(0x56),
        KEY_W(0x57),
        KEY_X(0x58),
        KEY_Y(0x59),
        KEY_Z(0x5a),

        KEY_NONPRINTABLE(0x100),

        KEY_DIVIDE(0x101),
        KEY_MULTIPLY(0x102),
        KEY_SUBTRACT(0x103),
        KEY_PLUS(0x104),

        KEY_F1(0x105),
        KEY_F2(0x106),
        KEY_F3(0x107),
        KEY_F4(0x108),
        KEY_F5(0x109),
        KEY_F6(0x110),
        KEY_F7(0x111),
        KEY_F8(0x112),
        KEY_F9(0x113),
        KEY_F10(0x114),
        KEY_F11(0x115),
        KEY_F12(0x116),

        KEY_HOME(0x117),
        KEY_END(0x118),
        KEY_INSERT(0x119),
        KEY_DELETE(0x120),
        KEY_PAGEUP(0x121),
        KEY_PAGEDN(0x122),

        KEY_BACKSPACE(0x123),
        KEY_TAB(0x124),
        KEY_PRTSCRN(0x125),
        KEY_PAUSE(0x126),

        KEY_MAX(0x127),
        ;
        final int value;
        InputButton(int value) {
            this.value = value;
        }
    }

    public record Version(int major, int minor, int patch) {
        @NotNull
        @Override
        public String toString() {
            return major + "." +  minor + "." + patch;
        }
    }

    private static final Linker LINKER;
    private static final SymbolLookup DEFAULT_LOOKUP;

    static {
        System.loadLibrary("renderdoc");
        LINKER = Linker.nativeLinker();
        DEFAULT_LOOKUP = SymbolLookup.loaderLookup();
    }

    private static final int API_FUNCTION_COUNT = 27;

    private final MemorySegment apiStructPtr;

    private final MethodHandle hGetAPIVersion; // 0; addr, addr, addr -> void
    private final MethodHandle hSetCaptureOptionU32; // 1; int, int -> int
    private final MethodHandle hSetCaptureOptionF32; // 2; int, float -> int
    private final MethodHandle hGetCaptureOptionU32; // 3; int -> int
    private final MethodHandle hGetCaptureOptionF32; // 4; int -> float
    private final MethodHandle hSetFocusToggleKeys; // 5; addr, int -> void
    private final MethodHandle hSetCaptureKeys; // 6; addr, int -> void
    private final MethodHandle hGetOverlayBits; // 7; void -> int
    private final MethodHandle hMaskOverlayBits; // 8; int, int -> void
    private final MethodHandle hRemoveHooks; // 9; void -> void
    private final MethodHandle hUnloadCrashHandler; // 10; void -> void
    private final MethodHandle hSetCaptureFilePathTemplate; // 11; String -> void
    private final MethodHandle hGetCaptureFilePathTemplate; // 12; void -> String
    private final MethodHandle hGetNumCaptures; // 13; void -> int
    private final MethodHandle hGetCapture; // 14; int, addr, addr, addr -> int
    private final MethodHandle hTriggerCapture; // 15; void -> void
    private final MethodHandle hIsTargetControlConnected; // 16; void -> int
    private final MethodHandle hLaunchReplayUI; // 17; int, addr, addr -> int
    private final MethodHandle hSetActiveWindow; // 18; addr, addr -> void
    private final MethodHandle hStartFrameCapture; // 19; addr, addr -> void
    private final MethodHandle hIsFrameCapturing; // 20; void -> int
    private final MethodHandle hEndFrameCapture; // 21; addr, addr -> int

    // since 1.1.0
    private @Nullable MethodHandle hTriggerMultiFrameCapture; // 22; int -> void
    // since 1.2.0
    private @Nullable MethodHandle hSetCaptureFileComments; // 23; addr, addr -> void
    // since 1.4.0
    private @Nullable MethodHandle hDiscardFrameCapture; // 24; addr, addr -> void
    // since 1.5.0
    private @Nullable MethodHandle hShowReplayUI; // 25; void -> int
    // since 1.6.0
    private @Nullable MethodHandle hSetCaptureTitle; // 26; addr -> void

    private final int version;

    private RenderDoc(KnownVersion version, MemorySegment apiStructPtr) {
        this.version = version.value;
        this.apiStructPtr = apiStructPtr;

        this.hGetAPIVersion = downcallIndex(0, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        this.hSetCaptureOptionU32 = downcallIndex(1, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.hSetCaptureOptionF32 = downcallIndex(2, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_FLOAT));
        this.hGetCaptureOptionU32 = downcallIndex(3, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.hGetCaptureOptionF32 = downcallIndex(4, FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_INT));
        this.hSetFocusToggleKeys = downcallIndex(5, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.hSetCaptureKeys = downcallIndex(6, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        this.hGetOverlayBits = downcallIndex(7, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.hMaskOverlayBits = downcallIndex(8, FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        this.hRemoveHooks = downcallIndex(9, FunctionDescriptor.ofVoid());
        this.hUnloadCrashHandler = downcallIndex(10, FunctionDescriptor.ofVoid());
        this.hSetCaptureFilePathTemplate = downcallIndex(11, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        this.hGetCaptureFilePathTemplate = downcallIndex(12, FunctionDescriptor.of(ValueLayout.ADDRESS));
        this.hGetNumCaptures = downcallIndex(13, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.hGetCapture = downcallIndex(14, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        this.hTriggerCapture = downcallIndex(15, FunctionDescriptor.ofVoid());
        this.hIsTargetControlConnected = downcallIndex(16, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.hLaunchReplayUI = downcallIndex(17, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        this.hSetActiveWindow = downcallIndex(18, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        this.hStartFrameCapture = downcallIndex(19, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        this.hIsFrameCapturing = downcallIndex(20, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        this.hEndFrameCapture = downcallIndex(21, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        if (this.version < KnownVersion.API_1_1_0.value) return;
        this.hTriggerMultiFrameCapture = downcallIndex(22, FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
        if (this.version < KnownVersion.API_1_2_0.value) return;
        this.hSetCaptureFileComments = downcallIndex(23, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        if (this.version < KnownVersion.API_1_4_0.value) return;
        this.hDiscardFrameCapture = downcallIndex(24, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        if (this.version < KnownVersion.API_1_5_0.value) return;
        this.hShowReplayUI = downcallIndex(25, FunctionDescriptor.of(ValueLayout.JAVA_INT));
        if (this.version < KnownVersion.API_1_6_0.value) return;
        this.hSetCaptureTitle = downcallIndex(26, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    private MemorySegment functionPointerAt(int idx) {
        if (idx < 0 || idx >= API_FUNCTION_COUNT) throw new IndexOutOfBoundsException(idx);
        long offset = idx * ValueLayout.ADDRESS.byteSize();
        return apiStructPtr.get(ValueLayout.ADDRESS, offset);
    }

    private MethodHandle downcallIndex(int idx, FunctionDescriptor fd) {
        MemorySegment fnAddr = functionPointerAt(idx);
        if (fnAddr == null || fnAddr.equals(MemorySegment.NULL))
            throw new UnsatisfiedLinkError("API function pointer at index " + idx + " is null! (Did you load the right version?)");

        return LINKER.downcallHandle(fnAddr, fd);
    }

    public static RenderDoc load(KnownVersion version) {
        MemorySegment getApiSym = DEFAULT_LOOKUP.find("RENDERDOC_GetAPI")
                .orElseThrow(() -> new UnsatisfiedLinkError("RENDERDOC_GetAPI symbol not found"));

        MethodHandle getApiMH = LINKER.downcallHandle(getApiSym, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(ValueLayout.ADDRESS.byteSize());

            int rc = (int) getApiMH.invoke(version.value, out);
            if (rc != 1) {
                throw new UnsatisfiedLinkError("RENDERDOC_GetAPI rejected version " + version);
            }

            MemorySegment apiPtr = out.get(ValueLayout.ADDRESS, 0);
            if (apiPtr == null || apiPtr.equals(MemorySegment.NULL))
                throw new UnsatisfiedLinkError("RENDERDOC_GetAPI returned null pointer");

            long ptrSize = ValueLayout.ADDRESS.byteSize();
            long structBytes = API_FUNCTION_COUNT * ptrSize;

            MemorySegment apiStruct = apiPtr.reinterpret(structBytes, Arena.global(), _ -> {});
            return new RenderDoc(version, apiStruct);
        } catch (Throwable e) {
            if (e instanceof UnsatisfiedLinkError u) throw u;
            throw new RuntimeException(e);
        }
    }

    public Version getApiVersion() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment major = arena.allocate(ValueLayout.JAVA_INT.byteSize()), minor = arena.allocate(ValueLayout.JAVA_INT.byteSize()), patch = arena.allocate(ValueLayout.JAVA_INT.byteSize());
            this.hGetAPIVersion.invoke(major, minor, patch);
            return new Version(major.get(ValueLayout.JAVA_INT, 0), minor.get(ValueLayout.JAVA_INT, 0), patch.get(ValueLayout.JAVA_INT, 0));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public int setCaptureOptionU32(CaptureOption option, int val) {
        try {
            return (int)this.hSetCaptureOptionU32.invoke(option.value, val);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public int setCaptureOptionF32(CaptureOption option, float val) {
        try {
            return (int)this.hSetCaptureOptionF32.invoke(option.value, val);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public int getCaptureOptionU32(CaptureOption option) {
        try {
            return (int)this.hGetCaptureOptionU32.invoke(option.value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public float getCaptureOptionF32(CaptureOption option) {
        try {
            return (float)this.hGetCaptureOptionF32.invoke(option.value);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setFocusToggleKeys(InputButton[] keys) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment array = arena.allocate(ValueLayout.JAVA_INT, keys.length);
            for (int i = 0; i < keys.length ; i++) {
                array.setAtIndex(ValueLayout.JAVA_INT, i, keys[i].value);
            }

            this.hSetFocusToggleKeys.invoke(array, keys.length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setCaptureKeys(InputButton[] keys) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment array = arena.allocate(ValueLayout.JAVA_INT, keys.length);
            for (int i = 0; i < keys.length ; i++) {
                array.setAtIndex(ValueLayout.JAVA_INT, i, keys[i].value);
            }

            this.hSetCaptureKeys.invoke(array, keys.length);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public int getOverlayBits() {
        try {
            return (int)this.hGetOverlayBits.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void maskOverlayBits(int and, int or) {
        try {
            this.hMaskOverlayBits.invoke(and, or);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void removeHooks() {
        try {
            this.hRemoveHooks.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public void shutdown() {
        removeHooks();
    }

    public void unloadCrashHandler() {
        try {
            this.hUnloadCrashHandler.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setCaptureFilePathTemplate(String template) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment str = arena.allocateFrom(template, StandardCharsets.UTF_8);
            this.hSetCaptureFilePathTemplate.invoke(str);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public void setLogFilePathTemplate(String template) {
        setCaptureFilePathTemplate(template);
    }

    public String getCaptureFilePathTemplate() {
        try {
            MemorySegment segment = (MemorySegment) this.hGetCaptureFilePathTemplate.invoke();
            return segment.getString(0, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public String getLogFilePathTemplate() {
        return getCaptureFilePathTemplate();
    }

    public int getNumCaptures() {
        try {
            return (int) this.hGetNumCaptures.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public record Capture(boolean valid, String filename, long timestamp) {}

    public Capture getCapture(int idx) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lengthPtr = arena.allocate(ValueLayout.JAVA_INT);

            this.hGetCapture.invoke(idx, MemorySegment.NULL, lengthPtr, MemorySegment.NULL);
            int length = lengthPtr.get(ValueLayout.JAVA_INT, 0);

            MemorySegment strPtr = arena.allocate(ValueLayout.JAVA_BYTE, length + 1);
            MemorySegment timestampPtr = arena.allocate(ValueLayout.JAVA_LONG);

            int valid = (int) this.hGetCapture.invoke(idx, strPtr, lengthPtr, timestampPtr);
            return new Capture(valid == 1, strPtr.getString(0, StandardCharsets.UTF_8), timestampPtr.get(ValueLayout.JAVA_LONG, 0));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void triggerCapture() {
        try {
            this.hTriggerCapture.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTargetControlConnected() {
        try {
            return ((int)this.hIsTargetControlConnected.invoke()) == 1;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public boolean isRemoteAccessConnected() {
        return isTargetControlConnected();
    }

    public int launchReplayUI(boolean connectTargetControl, @Nullable String cmdLineParams) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cmdLine = cmdLineParams == null ? MemorySegment.NULL : arena.allocateFrom(cmdLineParams, StandardCharsets.UTF_8);
            return (int) this.hLaunchReplayUI.invoke(connectTargetControl ? 1 : 0, cmdLine);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setActiveWindow(long devicePtr, long nativeWindowHandle) {
        try {
            this.hSetActiveWindow.invoke(MemorySegment.ofAddress(devicePtr), MemorySegment.ofAddress(nativeWindowHandle));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void startFrameCapture(long devicePtr, long nativeWindowHandle) {
        try {
            this.hStartFrameCapture.invoke(MemorySegment.ofAddress(devicePtr), MemorySegment.ofAddress(nativeWindowHandle));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isFrameCapturing() {
        try {
            return ((int)this.hIsFrameCapturing.invoke()) == 1;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean endFrameCapture(long devicePtr, long nativeWindowHandle) {
        try {
            return ((int)this.hEndFrameCapture.invoke(MemorySegment.ofAddress(devicePtr), MemorySegment.ofAddress(nativeWindowHandle)) == 1);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void triggerMultiFrameCapture(int numFrames) {
        if (this.version < KnownVersion.API_1_1_0.value || hTriggerMultiFrameCapture == null) throw new UnsupportedOperationException("TriggerMultiFrameCapture requires RenderDoc >=1.1.0!");
        try {
            this.hTriggerMultiFrameCapture.invoke(numFrames);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setCaptureFileComments(String filePath, String comments) {
        if (this.version < KnownVersion.API_1_2_0.value || hSetCaptureFileComments == null) throw new UnsupportedOperationException("SetCaptureFileComments requires RenderDoc >=1.2.0!");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pathPtr = arena.allocateFrom(filePath, StandardCharsets.UTF_8);
            MemorySegment commentPtr = arena.allocateFrom(comments, StandardCharsets.UTF_8);
            this.hSetCaptureFileComments.invoke(pathPtr, commentPtr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean discardFrameCapture(long devicePtr, long nativeWindowHandle) {
        if (this.version < KnownVersion.API_1_4_0.value || hDiscardFrameCapture == null) throw new UnsupportedOperationException("DiscardFrameCapture requires RenderDoc >=1.4.0!");
        try {
            return ((int)this.hDiscardFrameCapture.invoke(MemorySegment.ofAddress(devicePtr), MemorySegment.ofAddress(nativeWindowHandle))) == 1;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean showReplayUI() {
        if (this.version < KnownVersion.API_1_5_0.value || hShowReplayUI == null) throw new UnsupportedOperationException("ShowReplayUI requires RenderDoc >=1.5.0!");
        try {
            return ((int)this.hShowReplayUI.invoke()) == 1;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void setCaptureTitle(String title) {
        if (this.version < KnownVersion.API_1_6_0.value || hSetCaptureTitle == null) throw new UnsupportedOperationException("SetCaptureTitle requires RenderDoc >=1.6.0!");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment titlePtr = arena.allocateFrom(title, StandardCharsets.UTF_8);
            this.hSetCaptureTitle.invoke(titlePtr);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
