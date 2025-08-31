#version 430 core

#define LOCAL_SIZE 128
#define LIGHT_COUNT 128

layout(local_size_x = LOCAL_SIZE, local_size_y = 1, local_size_z = 1) in;

struct Light {
    vec3 position;
    float radius;

    vec3 color;
    float constant;

    float linear;
    float quadratic;
    vec2 _pad0;
};

struct Cluster
{
    vec4 minPoint;
    vec4 maxPoint;
    uint count;
    uint lightIndices[LIGHT_COUNT];
};

layout(std430, binding = 1) restrict buffer ClusterBuffer
{
    Cluster clusters[];
};

layout(std430, binding = 2) restrict buffer LightBuffer
{
    Light light[];
};

uniform mat4 viewMatrix;

bool testSphereAABB(uint i, Cluster c);

// each invocation of main() is a thread processing a cluster
void main()
{
    uint lightCount = light.length();
    uint index = gl_WorkGroupID.x * LOCAL_SIZE + gl_LocalInvocationID.x;
    Cluster cluster = clusters[index];

    // we need to reset count because culling runs every frame.
    // otherwise it would accumulate.
    cluster.count = 0;

    for (uint i = 0; i < lightCount; ++i)
    {
        if (testSphereAABB(i, cluster) && cluster.count < LIGHT_COUNT)
        {
            cluster.lightIndices[cluster.count] = i;
            cluster.count++;
        }
    }
    clusters[index] = cluster;
}

bool sphereAABBIntersection(vec3 center, float radius, vec3 aabbMin, vec3 aabbMax)
{
    // closest point on the AABB to the sphere center
    vec3 closestPoint = clamp(center, aabbMin, aabbMax);
    // squared distance between the sphere center and closest point
    float distanceSquared = dot(closestPoint - center, closestPoint - center);
    return distanceSquared <= radius * radius;
}

// this just unpacks data for sphereAABBIntersection
bool testSphereAABB(uint i, Cluster cluster)
{
    vec3 center = vec3(viewMatrix * vec4(light[i].position, 1.0));
    float radius = light[i].radius;

    vec3 aabbMin = cluster.minPoint.xyz;
    vec3 aabbMax = cluster.maxPoint.xyz;

    return sphereAABBIntersection(center, radius, aabbMin, aabbMax);
}