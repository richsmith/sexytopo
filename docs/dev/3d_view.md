# 3D View Implementation Notes

## Current Status

A working 3D survey skeleton view was implemented using raw OpenGL ES 2.0, with:
- ✅ Basic rendering of survey skeleton (stations + legs)
- ✅ Rotation controls (two-finger drag)
- ✅ Zoom controls (pinch)
- ✅ Theme background color
- ✅ Menu integration and navigation
- ✅ Toolbar with standard SexyTopo UI
- ⚠️ **Panning has issues** - needs fixing before merge

## Outstanding Issue: Panning

### Problem
Panning doesn't work intuitively. The survey should move with your finger in screen-space, but currently:
- Pan direction changes based on camera rotation
- Movement is unpredictable/confusing
- User expects: drag finger right → survey moves right (regardless of camera angle)

### Root Cause
The pan implementation moves the camera's look-at point, but the transformation doesn't properly account for the camera's current orientation in screen-space.

### Attempted Solutions
1. **Simple screen-space offset** - Didn't account for camera rotation
2. **Camera right/up vectors** - Calculated but pan still feels wrong
3. **Moving both camera + look-at** - Better but still orientation-dependent

### Recommended Solution for Next Attempt

The issue is that we're panning in world-space, not view-space. The correct approach:

#### Option A: View-Space Translation (Recommended)
Instead of modifying `cameraPanX/Y` in world coordinates, translate in view-space:

```java
public void panBy(float dx, float dy) {
    // Convert screen delta to view-space translation
    // This requires transforming by the INVERSE of the view matrix

    float[] inverseView = new float[16];
    Matrix.invertM(inverseView, 0, viewMatrix, 0);

    // Screen-space movement vector
    float[] screenDelta = {dx * scale, dy * scale, 0, 0};

    // Transform to world-space
    float[] worldDelta = new float[4];
    Matrix.multiplyMV(worldDelta, 0, inverseView, 0, screenDelta, 0);

    cameraPanX += worldDelta[0];
    cameraPanY += worldDelta[1];
}
```

#### Option B: Orbit Camera with Pan Offset
Keep camera orbiting around origin, but add a model matrix transformation:

```java
// In onDrawFrame():
Matrix.setIdentityM(modelMatrix, 0);
Matrix.translateM(modelMatrix, 0, -cameraPanX, -cameraPanY, 0);
Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0);
```

This translates the entire survey model instead of the camera.

#### Option C: Use Established 3D Libraries
For a more robust solution, consider using a proven library:

**Google Filament** (recommended if expanding 3D features):
- Handles view/projection math correctly
- Well-maintained by Google
- More setup, but professional results
- Good for future enhancements (passage models, lighting, etc.)

**Rajawali** (quick solution but unmaintained):
- Higher-level API with built-in camera controls
- Has working pan/rotate/zoom out of the box
- Risk: no longer maintained (last update 2018)

## Control Scheme

Current implementation uses:
- **One finger drag** = Pan (move view)
- **Two finger drag** = Rotate (orbit camera)
- **Pinch** = Zoom

This matches common 3D viewer conventions (Google Earth, CAD tools).

## Files Implemented

### Core 3D Rendering
- `app/src/main/java/org/hwyl/sexytopo/control/activity/ThreeDViewActivity.java` - Activity
- `app/src/main/java/org/hwyl/sexytopo/control/graph/SurveyView3D.java` - GLSurfaceView with touch handling
- `app/src/main/java/org/hwyl/sexytopo/control/graph/SurveyRenderer.java` - OpenGL ES renderer

### UI/Layout
- `app/src/main/res/layout/activity_3d_view.xml` - Activity layout with toolbar
- `app/src/main/res/menu/threed_view_menu.xml` - 3D-specific menu options
- `app/src/main/res/drawable/icon_3d.xml` - Vector icon (cube)

### Integration
- `app/src/main/java/org/hwyl/sexytopo/control/graph/ViewContext.java` - Added THREE_D enum
- `app/src/main/java/org/hwyl/sexytopo/control/graph/ContextMenuManager.java` - Added 3D navigation
- `app/src/main/java/org/hwyl/sexytopo/control/activity/SurveyEditorActivity.java` - Added onJumpTo3D()
- `app/src/main/java/org/hwyl/sexytopo/control/activity/SexyTopoActivity.java` - Added menu handler
- `app/src/main/res/menu/action_bar.xml` - Added View menu entry
- `app/src/main/res/menu/station_context.xml` - Added navigation option
- `app/src/main/res/values/strings.xml` - Added strings
- `app/src/main/AndroidManifest.xml` - Registered activity

## Code Quality Notes

### What Worked Well
- Zero external dependencies (raw OpenGL ES)
- Integrates cleanly with existing architecture
- Reuses existing `Space3DTransformer` for coordinate calculation
- Follows SexyTopo's coding conventions
- ~700 LOC total

### What Needs Work
- Pan math needs proper view-space transformation
- Consider adding station labels (texture rendering)
- Could add grid overlay for reference
- North arrow would be helpful
- Different colors for different survey sections

## Testing Checklist for Next Attempt

Before considering complete:
- [ ] Pan works intuitively in all camera orientations
- [ ] Rotate survey 180°, pan still works correctly
- [ ] Zoom in/out, pan scaling feels natural
- [ ] Reset View returns to sensible default
- [ ] Toggle Splays works
- [ ] Navigation from other views works
- [ ] Status bar doesn't overlap toolbar
- [ ] Background matches theme (light/dark)
- [ ] Memory doesn't leak on rotation
- [ ] Works on different screen sizes

## Performance Considerations

Current implementation is efficient:
- Direct GPU rendering
- Vertex/index buffers created once
- Only redraws on touch (RENDERMODE_WHEN_DIRTY)
- No complex shaders

For large surveys (1000+ stations):
- May need LOD (level of detail)
- Consider culling off-screen geometry
- Could batch draw calls by type

## Future Enhancements

Once panning works:
1. **Station labels** - Render text using texture atlas
2. **Color coding** - Different colors for survey sections
3. **Grid/axes** - Reference grid and axis indicators
4. **Measurement tools** - Measure distances in 3D
5. **Cross-sections** - Show cross-section data if available
6. **Export** - Save 3D view as image/model
7. **VR mode** - Cardboard support for immersive viewing

## References

- [OpenGL ES 2.0 Documentation](https://www.khronos.org/opengles/2_X/)
- [Android GLSurfaceView Guide](https://developer.android.com/develop/ui/views/graphics/opengl)
- [Learn OpenGL ES](https://learnopengl.com/) - Great tutorials (desktop GL but concepts apply)
- [Google Filament Documentation](https://google.github.io/filament/)

## Key Math Concepts

### Camera Orbit
Camera orbits around origin at distance `cameraDistance`:
```
x = distance * sin(angleX) * cos(angleY)
y = distance * cos(angleX)
z = distance * sin(angleX) * sin(angleY)
```

### Look-At Matrix
`Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)`
- Eye: Camera position
- Center: What camera looks at
- Up: Which direction is "up"

### MVP Matrix
`MVP = Projection × View × Model`
- Model: Object position/rotation in world
- View: Camera position/orientation
- Projection: Perspective transformation

## Debugging Tips

To debug pan issues:
1. Log the camera position and look-at point
2. Draw axes at origin to see orientation
3. Test with simple movements (just X, just Y)
4. Check matrix values are finite (not NaN)
5. Verify touch coordinates are in expected range

## Alternative Approaches Not Tried

1. **SceneForm** - Google's ARCore-based 3D library (deprecated, avoid)
2. **Unity** - Overkill for simple skeleton view
3. **Three.js via WebView** - Could work but less performant
4. **Custom software renderer** - Too slow for real-time interaction

## Conclusion

The 3D view is 90% complete. The remaining 10% is getting panning to work intuitively. The most promising fix is implementing proper view-space transformation (Option A above) or using a model matrix translation (Option B).

Once panning works correctly, this will be a valuable addition to SexyTopo for understanding complex 3D cave systems.
