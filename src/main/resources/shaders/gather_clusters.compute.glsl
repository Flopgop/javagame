#version 430 core
layout(local_size_x = 1, local_size_y = 1, local_size_z = 1) in;

struct Cluster
{
    vec4 minPoint;
    vec4 maxPoint;
    uint count;
    uint lightIndices[128];
};

layout(std430, binding = 1) restrict buffer ClusterBuffer {
    Cluster clusters[];
};

uniform float zNear;
uniform float zFar;

uniform mat4 inverseProjection;
uniform uvec3 gridSize;
uniform uvec2 screenDimensions;

vec3 screenToView(vec2 screenCoord);
vec3 lineIntersectionWithZPlane(vec3 startPoint, vec3 endPoint, float zDistance);

/*
 context: glViewport is referred to as the "screen"
 clusters are built based on a 2d screen-space grid and depth slices.
 Later when shading, it is easy to figure what cluster a fragment is in based on
 gl_FragCoord.xy and the fragment's z depth from camera
*/
void main()
{
    uint tileIndex = gl_WorkGroupID.x + (gl_WorkGroupID.y * gridSize.x) +
    (gl_WorkGroupID.z * gridSize.x * gridSize.y);
    vec2 tileSize = screenDimensions / gridSize.xy;

    // tile in screen-space
    vec2 minTile_screenspace = gl_WorkGroupID.xy * tileSize;
    vec2 maxTile_screenspace = (gl_WorkGroupID.xy + 1) * tileSize;

    // convert tile to view space sitting on the near plane
    vec3 minTile = screenToView(minTile_screenspace);
    vec3 maxTile = screenToView(maxTile_screenspace);

    float planeNear =
    zNear * pow(zFar / zNear, gl_WorkGroupID.z / float(gridSize.z));
    float planeFar =
    zNear * pow(zFar / zNear, (gl_WorkGroupID.z + 1) / float(gridSize.z));

    // the line goes from the eye position in view space (0, 0, 0)
    // through the min/max points of a tile to intersect with a given cluster's near-far planes
    vec3 minPointNear =
    lineIntersectionWithZPlane(vec3(0, 0, 0), minTile, planeNear);
    vec3 minPointFar =
    lineIntersectionWithZPlane(vec3(0, 0, 0), minTile, planeFar);
    vec3 maxPointNear =
    lineIntersectionWithZPlane(vec3(0, 0, 0), maxTile, planeNear);
    vec3 maxPointFar =
    lineIntersectionWithZPlane(vec3(0, 0, 0), maxTile, planeFar);

    vec3 aabbMin = min(min(minPointNear, minPointFar), min(maxPointNear, maxPointFar));
    vec3 aabbMax = max(max(minPointNear, minPointFar), max(maxPointNear, maxPointFar));

    clusters[tileIndex].minPoint = vec4(aabbMin, 0.0);
    clusters[tileIndex].maxPoint = vec4(aabbMax, 0.0);
}

// Returns the intersection point of an infinite line and a
// plane perpendicular to the Z-axis
vec3 lineIntersectionWithZPlane(vec3 startPoint, vec3 endPoint, float zDistance)
{
    vec3 direction = endPoint - startPoint;
    vec3 normal = vec3(0.0, 0.0, -1.0); // plane normal

    // skip check if the line is parallel to the plane.

    float t = (zDistance - dot(normal, startPoint)) / dot(normal, direction);
    return startPoint + t * direction; // the parametric form of the line equation
}
vec3 screenToView(vec2 screenCoord)
{
    // normalize screenCoord to [-1, 1] and
    // set the NDC depth of the coordinate to be on the near plane. This is -1 by
    // default in OpenGL
    vec4 ndc = vec4(screenCoord / screenDimensions * 2.0 - 1.0, -1.0, 1.0);

    vec4 viewCoord = inverseProjection * ndc;
    viewCoord /= viewCoord.w;
    return viewCoord.xyz;
}