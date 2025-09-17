package net.flamgop.physics;

import com.github.stephengold.joltjni.*;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;

public class Physics {

    private final TempAllocator tempAllocator;
    private final JobSystem jobSystem;
    private final PhysicsSystem physicsSystem;

    public Physics() throws Exception {
        LibraryInfo info = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(info);

        NativeDynamicLibrary[] libraries = {
//                new NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
//                new NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
                new NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);

        JoltPhysicsObject.startCleaner();
        Jolt.registerDefaultAllocator();
        Jolt.installDefaultAssertCallback();
        Jolt.installDefaultTraceCallback();
        if (!Jolt.newFactory()) throw new RuntimeException("Failed to install Jolt factory");
        Jolt.registerTypes();

        tempAllocator = new TempAllocatorMalloc();
        int numWorkerThreads = Runtime.getRuntime().availableProcessors();
        jobSystem = new JobSystemThreadPool(
                Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numWorkerThreads);

        final int numBpLayers = 3;

        ObjectLayerPairFilterTable ovoFilter
                = new ObjectLayerPairFilterTable(Layers.NUM_LAYERS);
        ovoFilter.enableCollision(Layers.MOVING, Layers.MOVING);
        ovoFilter.enableCollision(Layers.MOVING, Layers.NON_MOVING);
        ovoFilter.enableCollision(Layers.MOVING, Layers.PLAYER);
        ovoFilter.enableCollision(Layers.PLAYER, Layers.NON_MOVING);
        ovoFilter.enableCollision(Layers.PLAYER, Layers.PLAYER);
        ovoFilter.disableCollision(Layers.NON_MOVING, Layers.NON_MOVING);

        BroadPhaseLayerInterfaceTable layerMap
                = new BroadPhaseLayerInterfaceTable(Layers.NUM_LAYERS, numBpLayers);
        layerMap.mapObjectToBroadPhaseLayer(Layers.MOVING, 0);
        layerMap.mapObjectToBroadPhaseLayer(Layers.NON_MOVING, 1);
        layerMap.mapObjectToBroadPhaseLayer(Layers.PLAYER, 2);
        ObjectVsBroadPhaseLayerFilterTable ovbFilter
                = new ObjectVsBroadPhaseLayerFilterTable(
                layerMap, numBpLayers, ovoFilter, Layers.NUM_LAYERS);

        physicsSystem = new PhysicsSystem();
        int maxBodies = 5_000;
        int numBodyMutexes = 0; // default
        int maxBodyPairs = 65_536;
        int maxContacts = 20_480;
        physicsSystem.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts, layerMap, ovbFilter, ovoFilter);
    }

    public PhysicsSystem system() {
        return physicsSystem;
    }

    public TempAllocator tempAllocator() {
        return tempAllocator;
    }

    public BodyFilter allBodies() {
        return new BodyFilter();
    }

    public ShapeFilter allShapes() {
        return new ShapeFilter();
    }

    public BodyInterface bodyInterface() {
        return physicsSystem.getBodyInterface();
    }

    public void update(float delta, int steps) {
        physicsSystem.update(delta, steps, tempAllocator, jobSystem);
    }

    public void destroy() {
        jobSystem.close();
    }
}
